package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Eduard Albu on 14 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class SocketService extends Service implements HandshakeCompletedListener {

    private static final String TAG = SocketService.class.getSimpleName();
    public static SSLSocket sslSocket;
    private static SSLSocketFactory sslSocketFactory;
    public BufferedReader readIn;
    public BufferedWriter writeOut;
    public boolean isConnected;

    // Bind this service to application
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    private final IBinder mIBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }


    /**
     * Send a request to server if the connection is ok and send back the response through
     * ResponseReceiver broadcast
     *
     * @param request to send
     */
    public void sendRequest(final JSONObject request) {
        new SendRequest(request);
    }


    /**
     * Called when socket handShsake is finished
     */
    @Override
    public void handshakeCompleted(HandshakeCompletedEvent event) {

    }

    /**
     * Initiate all components needed to build a new SSLSocket object
     */
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // initialize socket factory and all-trust TrustyManager
            SecureRandom secureRandom = new SecureRandom();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, getTrustManagers(), secureRandom);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Connect socket to server through TLS protocol
        new Connection();
        return START_NOT_STICKY;
    }

    /**
     * Runnable which will establish server connection in a new thread
     */
    class Connection implements Runnable {

        public Connection() {
            // start the thread
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "Start connecting with server");
                // create a new instance of socket and connect it to server
                sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                        new Socket(Api.URL, Api.PORT), Api.M_URL, Api.PORT, true
                );

                Intent intent = new Intent(SocketService.this, ConnectionReceiver.class);
                intent.setAction(Api.CONNECTION_BROADCAST);
                sendBroadcast(intent);

                isConnected = true;

                Log.d(TAG, "Socket connected: " + String.valueOf(sslSocket.isConnected()));
            } catch (Exception e) {
                e.printStackTrace();
                isConnected = false;
            }
        }
    }

    public void restartConnection() {
        new Reconnect();
    }

    class Reconnect implements Runnable {

        public Reconnect() {
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                if (sslSocket != null && sslSocket.isConnected()) {
                    sslSocket.close();
                    new Connection();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runnable which will send requests in a new thread
     */
    class SendRequest implements Runnable {
        JSONObject message;

        /**
         * Constructor
         *
         * @param message for server
         */
        public SendRequest(JSONObject message) {
            // give the message for server and socket object to current thread
            this.message = message;

            // send message to server
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            // create an intent for the ResponseReceiver
            Intent intent = new Intent(SocketService.this, ResponseReceiver.class);
            // set the action to RESPONSE_BROADCAST object from Api class
            intent.setAction(Api.RESPONSE_BROADCAST);
            // create a StringBuilder to append each line from response
            StringBuilder builder = new StringBuilder();
            try {
                if (sslSocket == null || !sslSocket.isConnected()) {
                    new Connection();
                }

                if (sslSocket != null && sslSocket.isConnected()) {
                    // Start socket handshake
                    sslSocket.startHandshake();

                    // set the timeout for the connection

                    // Create a reader and writer from socket output and input streams
                    writeOut = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
                    readIn = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                    // send the request to server through writer object
                    writeOut.write(message.toString() + "\n");
                    writeOut.flush();

                    // Start reading each response line\
                    String stringResponse = readIn.readLine();

                    // check if the response it's not null
                    if (stringResponse != null) {
                        try {
                            // get response status
                            JSONObject response = new JSONObject(stringResponse);
                            boolean success = response.getBoolean("success");
                            // check if response is successful
                            if (!success) {
                                // close current connection if not and open a new one
                                if (sslSocket != null) {
                                    sslSocket.close();
                                }
                                sslSocket = null;
                                new Connection();
                            }
                        } catch (Exception e) {
                            // close connection and open new if an Exception occurs
                            if (sslSocket != null) {
                                sslSocket.close();
                            }
                            sslSocket = null;
                            new Connection();
                        }
                    } else {
                        // close connection and open a new one if the response is null
                        if (sslSocket != null) {
                            sslSocket.close();
                        }
                        sslSocket = null;
                        new Connection();
                    }

                    // send the response to ui
                    intent.putExtra(Api.RESPONSE_MESSAGE, stringResponse);
                    sendBroadcast(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // send a null object to ui in case an exception occurs
                intent.putExtra(Api.RESPONSE_MESSAGE, builder.toString());
                sendBroadcast(intent);
            }
        }
    }

    /**
     * Close socket connection and set socket to null to free the resources
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            sslSocket.close();
            isConnected = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        sslSocket = null;
    }

    /**
     * Create a TrustManager which will trust all certificates
     *
     * @return TrustManager[] with a trust-all certificates TrustManager object inside
     */
    private TrustManager[] getTrustManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };
    }
}
