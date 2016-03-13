package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by eduard on 3/13/16.
 *
 */
public class ConnectionManager extends Service {

    private static final String TAG = ConnectionManager.class.getSimpleName();
    private static SecureRandom secureRandom;
    private static SSLSocket sslSocket;
    private static BufferedReader readIn;
    private static BufferedWriter writeOut;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action == null) {
            throw new NullPointerException("Please specify an action for the intent");
        }

        switch (action) {
            case Api.Action.ACTION_CONNECT:
                Thread connection = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            secureRandom = new SecureRandom();
                            // Install the all-trusting trust manager
                            final SSLContext sslContext = SSLContext.getInstance("TLS");
                            sslContext.init(null, getTrustManagers(), secureRandom);
                            // Create an ssl socket factory with our all trusting manager
                            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                            // Create a socket and connect to the server
                            sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                                    new Socket(Api.URL, Api.PORT), Api.URL, Api.PORT, true
                            );
                            sslSocket.startHandshake();
                            writeOut = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
                            readIn = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                connection.start();
                break;
            case Api.Action.ACTION_SEND:
                if (!intent.hasExtra(Api.REQUEST_MESSAGE)) {
                    throw new NullPointerException(
                            "Please put an extras with a string int JSON format"
                    );
                }
                final String request = intent.getExtras().getString(Api.REQUEST_MESSAGE);

                if (request == null) {
                    throw new NullPointerException(
                            "Please put an extras with a string int JSON format"
                    );
                } else {
                    Thread send = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendRequest(request);
                        }
                    });

                    send.start();
                }
                break;
            case Api.Action.ACTION_DISCONECT:
                Thread disconnect = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sslSocket.close();
                            writeOut.close();
                            readIn.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                disconnect.start();
                break;
        }
        return START_NOT_STICKY;
    }

    private void sendRequest(String request) {
        try {
            StringBuilder response = new StringBuilder();
            Log.i(TAG, String.valueOf(sslSocket.isConnected()));
            String s = request + "\n";
            Log.i(TAG, s);

            // Send the JSON to server
            writeOut.write(s);
            writeOut.flush();

            // TODO: 3/13/16 figure out why it does not exit the loop
            char[] chars = new char[1024];
            int read;
            Intent intent = new Intent(Api.RESPONSE_BROADCAST);
            while ((read = readIn.read(chars)) != -1) {
                response.append(new String(chars, 0, read));
                intent.putExtra(Api.RESPONSE_MESSAGE, response.toString());
                sendBroadcast(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

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
