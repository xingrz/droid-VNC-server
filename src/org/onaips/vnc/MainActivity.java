package org.onaips.vnc;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "GUI";

	private ServerManager server = null;

	private Animation buttonAnimation = null;
	private SharedPreferences preferences;

    private BroadcastReceiver activityUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                setStateLabels(ServerManager.isServerRunning());
            }
        }
    };

    public BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (info != null) {
                if (info.getType() == ConnectivityManager.TYPE_MOBILE
                        || info.getType() == ConnectivityManager.TYPE_WIFI) {
                    setStateLabels(ServerManager.isServerRunning());
                }
            }
        }
    };

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
            server = ((ServerManager.MyBinder) binder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
            server = null;
		}
	};

    @Override
    public void onResume() {
        registerReceiver(activityUpdateReceiver, new IntentFilter("org.onaips.vnc.ACTIVITY_UPDATE"));
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceiver(connectivityReceiver);
        unregisterReceiver(activityUpdateReceiver);
        super.onPause();
    }

	@Override  
	protected void onDestroy() {
        //unregisterReceiver(connectivityReceiver);
		//unregisterReceiver(activityUpdateReceiver);

        unbindService(mConnection);

        super.onDestroy();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);  

        // setup window
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

        // register activity update receiver
        //registerReceiver(activityUpdateReceiver, new IntentFilter("org.onaips.vnc.ACTIVITY_UPDATE"));

        // register wifi event receiver
        //registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // bind service
        bindService(new Intent(this, ServerManager.class), mConnection, Context.BIND_AUTO_CREATE);

		// Initialize preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		boolean root = preferences.getBoolean("asroot",true);
		if (!hasRootPermission() && root) {
			Log.v(TAG, "You do not have root permissions...!!!");

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Cannot continue");
            alert.setMessage("You do not have root permissions.\n" +
                                   "Please root your phone first!\n\n" +
                                   "Do you want to continue anyway?");
            alert.setIcon(R.drawable.icon);

            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    Editor e = preferences.edit();
                    e.putBoolean("asroot", false);
                    e.commit();
                }
            });

            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    System.exit(0);
                }
            });

            alert.show();
		}

		setStateLabels(ServerManager.isServerRunning());

		findViewById(R.id.Button01).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				final Button b=(Button)findViewById(R.id.Button01);
				
				buttonAnimation=AnimationUtils.loadAnimation(MainActivity.this, R.anim.animation);
				buttonAnimation.setAnimationListener(new AnimationListener() {
					public void onAnimationEnd(Animation animation) {
						b.setEnabled(true);
						//b.setVisibility(View.INVISIBLE);
					}

					public void onAnimationRepeat(Animation animation) {
					}

					public void onAnimationStart(Animation animation) {
						b.setEnabled(false);

						if (ServerManager.isServerRunning())
							stopServer();
						else
							startServer();
					}
				});
				b.startAnimation(buttonAnimation);
			}
		}) ;
	}

    // deprecated
	public void showTextOnScreen(final String t) {
		runOnUiThread(new Runnable(){
			public void run() {
				Toast.makeText(MainActivity.this, t, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void setStateLabels(boolean state) {
		TextView stateLabel=(TextView)findViewById(R.id.stateLabel);
		stateLabel.setText(state?"Running":"Stopped");


		stateLabel.setTextColor(state?Color.rgb(114,182,43):Color.rgb(234,113,29));

		TextView t=(TextView)findViewById(R.id.TextView01);
 
		Button b=(Button)findViewById(R.id.Button01);
		b.clearAnimation();
		Button b2=(Button)findViewById(R.id.Button02);
		if (state)
		{
			String port=preferences.getString("port", "5901");
			String httpport;
			try
			{
				int port1=Integer.parseInt(port);
				port=String.valueOf(port1);
				httpport=String.valueOf(port1-100);
			}
			catch(NumberFormatException e)
			{
				port="5901";
				httpport="5801";
			}

			String ip = getIpAddress();
            String text = ip.equals("")
                        ? "Not connected to a network.\n"
                        + "You can connect through USB with:\n"
                        + "  localhost:" + port + "\n"
                        + "or\n"
                        + "  http://localhost:" + httpport + "\n"
                        + "(use adb to forward ports)"
                        : "Connect to:\n"
                        + "  " + ip + ":" + port + "\n"
                        + "  or\n"
                        + "http://" + ip + ":" + httpport;

            t.setText(text);
			b.setBackgroundDrawable(getResources().getDrawable(R.drawable.btnstop_normal));
			b2.setVisibility(View.VISIBLE);

		} 
		else
		{
			t.setText("");
			b.setBackgroundDrawable(getResources().getDrawable(R.drawable.btnstart_normal));
			b2.setVisibility(View.INVISIBLE);
		}  

	} 

	public String getIpAddress() {
		try {
            NetworkInterface intf = NetworkInterface.getByName("wlan0");
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = enumIpAddr.nextElement();

                if (!inetAddress.isLoopbackAddress()) {
                    String ip = inetAddress.getHostAddress();
                    if (InetAddressUtils.isIPv4Address(ip)) {
                        return ip;
                    }
                }
            }
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return "";
	}

	public void startServer() {
        server.startServer();
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (!ServerManager.isServerRunning()) {
					runOnUiThread(new Runnable() {
						public void run() {
							showTextOnScreen("Could not start server :(");
							Log.v(TAG, "Could not start server :(");
							setStateLabels(ServerManager.isServerRunning());
						}
					});
				} 
			}
		}, 2000);
	}
 	public void stopServer() {
        server.killServer();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (ServerManager.isServerRunning()) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            showTextOnScreen("Could not stop server :(");
                            Log.v(TAG, "Could not stop server :(");
                            setStateLabels(ServerManager.isServerRunning());
                        }
                    });
                }
            }
        }, 4000);
	}

	public static boolean hasRootPermission() {
		try {
            return new File("/system/bin/su").exists() || new File("/system/xbin/su").exists();
		} catch (Exception e) {
			return false;
		}
	}
}