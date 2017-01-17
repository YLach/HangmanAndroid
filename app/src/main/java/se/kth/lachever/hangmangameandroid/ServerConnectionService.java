package se.kth.lachever.hangmangameandroid;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class ServerConnectionService extends Service {
    private static final String TAG = "ServerConnectionService";
    private String host;
    private int port;
    private Socket clientSocket;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    public final static String SRV_MESSAGE = "se.kth.lachever.hangmangameandroid.intentService.MESSAGE";
    public final static String SRV_MESSAGE_CONTENT = "se.kth.lachever.hangmangameandroid.intentService.MESSAGE_CONTENT";
    public final static String SRV_CONNECTED = "se.kth.lachever.hangmangameandroid.intentService.SRV_CONNECTED";


    public void sendResult(String messToSend) {
        Intent intent = new Intent(SRV_MESSAGE);
        intent.putExtra(SRV_MESSAGE_CONTENT, messToSend);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void notifyConnection() {
        Intent intent = new Intent(SRV_CONNECTED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void sendMessageToServer(String messageToSend) {
        Runnable send = new callServer(messageToSend);
        new Thread(send).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Bundle extras = intent.getExtras();
        host = (String) extras.get(ServerConnectionActivity.SRV_HOST);
        port = (Integer) extras.get(ServerConnectionActivity.SRV_PORT);

        Runnable connect = new connectSocket();
        new Thread(connect).start();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        clientSocket = null;
    }

    public class LocalBinder extends Binder {
        public ServerConnectionService getService() {
            return ServerConnectionService.this;
        }
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    private class connectSocket implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(host);
                clientSocket = new Socket(serverAddr, port);
                in = new BufferedInputStream(clientSocket.getInputStream());
                out = new BufferedOutputStream(clientSocket.getOutputStream());
                Log.d(TAG, "Connected to server");
                notifyConnection();
            } catch (UnknownHostException e) {
                Log.d(TAG, "Don't know about host: " + host + ".", e);
            } catch (IOException e) {
                Log.d(TAG, "Couldn't get I/O for the connection to: "
                        + host + ".", e);
            }
        }
    }


    private class callServer implements Runnable {
        private String messageToSend;

        public callServer(String messageToSend) {
            this.messageToSend = messageToSend;
        }

        @Override
        public void run() {
            try {
                // Send the message stringToSend
                byte[] msg = this.messageToSend.getBytes();
                out.write(msg, 0, msg.length);
                out.flush();
            } catch (IOException e) {
                Log.d(TAG, "Error", e);
            }

            // Receive the answer
            try {
                byte[] fromServer = new byte[4096];
                int length = in.read(fromServer, 0, fromServer.length);
                sendResult(new String(fromServer, 0, length));
            } catch (IOException e) {
                Log.d(TAG, "Error", e);
            }
        }
    }
}
