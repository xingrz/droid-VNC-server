package org.onaips.vnc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ServerManager extends Service {
    private static final String TAG = "ServerManager";

	SocketListener serverConnection = null;

    private final IBinder mBinder = new MyBinder();

	@Override
	public void onCreate() {
		super.onCreate();

		if (serverConnection != null) {
            Log.v(TAG, "ServerConnection was already active!");
		} else {
            Log.v(TAG, "ServerConnection started");
			serverConnection = new SocketListener();
			serverConnection.start();
		}

	}

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

	public void startServer() {
		try {
			String exec = getFilesDir().getParent() + "/lib/libandroidvncserver.so";
			if (!new File(exec).exists()) {
                Log.v(TAG, "Error! Could not find daemon file: " + exec);
				return;
			}

            File dir = new File(getFilesDir().getAbsolutePath());
            Runtime runtime = Runtime.getRuntime();

			String chmod = "chmod 777 " + exec;
			String start = exec + " -P 5901";

			if (MainActivity.hasRootPermission()) {
                Log.v(TAG, "Running as root...");
				Process shell = runtime.exec("su", null, dir);
				OutputStream os = shell.getOutputStream();
                os.write((chmod + "\n").getBytes());
                os.write((start + "\n").getBytes());
			} else {
                Log.v(TAG, "Not running as root...");
                runtime.exec(chmod);
                runtime.exec(start, null, dir);
			}

            Log.v(TAG, "Starting " + start);
		} catch (Exception e) {
            e.printStackTrace();
		}

	}

	public void killServer() {
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress addr = InetAddress.getLocalHost();
			String toSend = "~KILL|";
			byte[] buffer = toSend.getBytes();

			DatagramPacket question = new DatagramPacket(buffer, buffer.length, addr, 13132);
			clientSocket.send(question);
		} catch (Exception e) {
            e.printStackTrace();
		}
	}

	public static boolean isServerRunning() {
		try {
			byte[] receiveData = new byte[1024];
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress addr = InetAddress.getLocalHost();

			clientSocket.setSoTimeout(100);
			String toSend = "~PING|";
			byte[] buffer = toSend.getBytes();

			DatagramPacket question = new DatagramPacket(buffer, buffer.length,
					addr, 13132);
			clientSocket.send(question);

			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			clientSocket.receive(receivePacket);
			String receivedString = new String(receivePacket.getData());
			receivedString = receivedString.substring(0, receivePacket
					.getLength());

			return receivedString.equals("~PONG|");
		} catch (Exception e) {
			return false;
		}
	}

	class SocketListener extends Thread {
		boolean finished = false;

		public void finishThread() {
			finished = true;
		}

		@Override
		public void run() {
			try {
                DatagramSocket server = new DatagramSocket(13131);
                Log.v(TAG, "Listening...");

                while (!finished) {
                    DatagramPacket answer = new DatagramPacket(new byte[1024], 1024);
                    server.receive(answer);

                    String res = new String(answer.getData());
                    res = res.substring(0, answer.getLength());

                    Log.v(TAG, "RECEIVED " + res);

                    int splitter = res.indexOf("|");
                    if (res.startsWith("~") && splitter > 1) {
                        String command = res.substring(1, splitter);
                        String extras = res.substring(splitter + 1);

                        Log.v(TAG, "Command: " + command);

                        if (command.equals("SERVERSTARTED")) {
                            sendBroadcast(new Intent("org.onaips.vnc.ACTIVITY_UPDATE"));
                        } else if (command.equals("SERVERSTOPPED")) {
                            sendBroadcast(new Intent("org.onaips.vnc.ACTIVITY_UPDATE"));
                        }/* else if (command.equals("CONNECTED")) {
                            //showClientConnected(extras);
                        } else if (command.equals("DISCONNECTED")) {
                            //showClientDisconnected();
                        }*/
                    }
                }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    public class MyBinder extends Binder {
        ServerManager getService() {
            return ServerManager.this;
        }
    }
}
