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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

public class MenuActivity extends Activity {

	private static final String TAG = "MenuActivity";

    private ServerManager mServer;

    public BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (info != null) {
                if (info.getType() == ConnectivityManager.TYPE_MOBILE
                        || info.getType() == ConnectivityManager.TYPE_WIFI) {
                    //setStateLabels(VNCService.isServerRunning());
                }
            }
        }
    };

	private ServiceConnection mConnection = new ServiceConnection() {
        @Override
		public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof VNCService.VNCBinder) {
                mServer = ((VNCService.VNCBinder) service).getServer();
                openOptionsMenu();
            }
		}

        @Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	@Override  
	protected void onDestroy() {
        //unbindService(mConnection);
        super.onDestroy();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        bindService(new Intent(this, VNCService.class), mConnection, 0);

        // check root permission
		if (!hasRootPermission()) {
			Log.v(TAG, "You do not have root permissions...!!!");

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Cannot continue");
            alert.setMessage("You do not have root permissions!");
            alert.setIcon(R.drawable.icon);
            alert.show();
		}
	}

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        openOptionsMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(connectivityReceiver);
    }

    @Override
    public void openOptionsMenu() {
        if (mServer != null) {
            super.openOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vnc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stop:
                mServer.stop();
                stopService(new Intent(this, VNCService.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        unbindService(mConnection);
        finish();
    }

    // TODO: make this for Glass
	/*public void setStateLabels(boolean running) {
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
	}*/

	public static boolean hasRootPermission() {
		try {
            return new File("/system/bin/su").exists() || new File("/system/xbin/su").exists();
		} catch (Exception e) {
			return false;
		}
	}
}