package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

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
     * @param request to send
     */
    public void sendRequest(final JSONObject request) {
        new SendRequest(request, sslSocket);
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
        new Connect(sslSocket);
        return START_NOT_STICKY;
    }

    /**
     * Runnable which will establish server connection in a new thread
     */
    class Connect implements Runnable {
        SSLSocket mSocket;
        public Connect(SSLSocket sslSocket) {
            // Give the socket object to current thread
            mSocket = sslSocket;

            // start the thread
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                // create a new instance of socket and connect it to server
                mSocket = (SSLSocket) sslSocketFactory.createSocket(
                        new Socket(Api.URL, Api.PORT), Api.URL, Api.PORT, true
                );

                // set the timeout for the connection
                mSocket.setSoTimeout(2000);
                Log.d(TAG, "Socket connected: " + String.valueOf(mSocket.isConnected()));
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
        SSLSocket mSocket;

        /**
         * Constructor
         * @param message for server
         * @param sslSocket through which to send the request
         */
        public SendRequest(JSONObject message, SSLSocket sslSocket) {
            // give the message for server and socket object to current thread
            this.message = message;
            mSocket = sslSocket;

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
                // Start socket handshake
                mSocket.startHandshake();

                // Create a reader and writer from socket output and input streams
                writeOut = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                readIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                // send the request to server through writer object
                writeOut.write(message.toString() + "\n");
                writeOut.flush();

                // Start reading each response line
                String response;
                while ((response = readIn.readLine()) != null) {
                    // append each line to builder
                    builder.append(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Send the broadcast to receiver with either response or null
                // if it's null all listeners will be informed about an unknown error
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        sslSocket = null;
    }

    /**
     * Create a TrustManager which will trust all certificates
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
