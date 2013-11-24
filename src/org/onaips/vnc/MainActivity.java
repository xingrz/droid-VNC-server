package org.onaips.vnc;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String TAG = "GUI";

    private static final String ACTIVITY_UPDATE = "org.onaips.vnc.ACTIVITY_UPDATE";

	private ServerManager server = null;

    private BroadcastReceiver activityUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setStateLabels(ServerManager.isServerRunning());
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
        registerReceiver(activityUpdateReceiver, new IntentFilter(ACTIVITY_UPDATE));
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
        unbindService(mConnection);
        super.onDestroy();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);  

        // setup window
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

        // bind service
        bindService(new Intent(this, ServerManager.class), mConnection, Context.BIND_AUTO_CREATE);

        // check root permission
		if (!hasRootPermission()) {
			Log.v(TAG, "You do not have root permissions...!!!");

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Cannot continue");
            alert.setMessage("You do not have root permissions!");
            alert.setIcon(R.drawable.icon);
            alert.show();
		}

        // update ui
		setStateLabels(ServerManager.isServerRunning());

        // attach button event
        findViewById(R.id.button_start).setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (ServerManager.isServerRunning()) {
                    stopServer();
                } else {
                    startServer();
                }
            }
        });
	}

    // TODO: make this for Glass
	public void setStateLabels(boolean running) {
		TextView stateLabel = (TextView) findViewById(R.id.stateLabel);
		stateLabel.setText(running ? "Running" : "Stopped");
		stateLabel.setTextColor(running ? Color.rgb(114, 182, 43) : Color.rgb(234, 113, 29));

		TextView textView = (TextView) findViewById(R.id.TextView01);
		Button button = (Button) findViewById(R.id.button_start);

		if (running) {
			String ip = getIpAddress();
            textView.setText(ip.equals("") ? "localhost:5901 (via adb)" : ip + ":5901");
            button.setBackgroundDrawable(getResources().getDrawable(R.drawable.btnstop_normal));
		} else {
            textView.setText("");
            button.setBackgroundDrawable(getResources().getDrawable(R.drawable.btnstart_normal));
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
	}
 	public void stopServer() {
        server.killServer();
	}

	public static boolean hasRootPermission() {
		try {
            return new File("/system/bin/su").exists() || new File("/system/xbin/su").exists();
		} catch (Exception e) {
			return false;
		}
	}
}