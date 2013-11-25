package org.onaips.vnc;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class PanelView extends LinearLayout {

    private static final String TAG = "PanelView";

    public interface ChangeListener {
        public void onChange();
    }

    private final TextView mStateView;
    private final TextView mAddressView;

    private final ServerManager mServer;
    private final ServerManager.ServerListener mServerListener = new ServerManager.ServerListener() {
        @Override
        public void onStart(int port) {
            Log.v(TAG, "Server started at port " + port);
            updateText();
        }

        @Override
        public void onStop() {
            Log.v(TAG, "Server stopped");
            updateText();
        }

        @Override
        public void onConnect(String ip) {
            Log.v(TAG, "Client " + ip + " connected");
        }

        @Override
        public void onDisconnect() {
            Log.v(TAG, "Client disconnected");
        }
    };

    private ChangeListener mChangeListener;

    public PanelView(Context context) {
        this(context, null);
    }

    public PanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanelView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        LayoutInflater.from(context).inflate(R.layout.main, this);

        mStateView = (TextView) findViewById(R.id.state);
        mAddressView = (TextView) findViewById(R.id.address);

        mServer = new ServerManager(context);
        mServer.setListener(mServerListener);
        mServer.start();
    }

    public ServerManager getServer() {
        return mServer;
    }

    public void setListener(ChangeListener listener) {
        mChangeListener = listener;
    }

    private void updateText() {
        Log.v(TAG, "update text");
        mStateView.setText(mServer.isRunning() ? "Running" : "Stopped");
        mAddressView.setText(mServer.isRunning() ? (getIpAddress() + ":" + mServer.getPort()) : "");
        if (mChangeListener != null) {
            mChangeListener.onChange();
        }
    }

    private String getIpAddress() {
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
}
