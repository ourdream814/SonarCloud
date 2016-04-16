package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by eduard on 4/13/16.
 *
 */
public class AuthService extends Service {

    public static SSLSocket dataSocket;
    private static SSLSocketFactory sslSocketFactory;
    public static BufferedReader readIn;
    public static BufferedWriter writeOut;
    public boolean isConnected;
    private ExecutorService mRequestExecutor;

    // Bind this service to application
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        new Connection();
        return mIBinder;
    }

    private final IBinder mIBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public AuthService getService() {
            return AuthService.this;
        }
    }

    /**
     * Send a request to server if the connection is ok and send back the response through
     * @param request to send
     */
    public void sendRequest(JSONObject request) {
        if (isConnected && SonarCloudApp.getInstance().isConnected()) {
            mRequestExecutor.execute(new SendRequest(request));
        } else {
            Intent intent = new Intent(this, ConnectionReceiver.class);
            intent.setAction(Api.CONNECTION_FAILED);
            sendBroadcast(intent);
            new Connection();
        }
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
            sslContext.init(null, SonarCloudApp.getInstance().getTrustManagers(), secureRandom);
            sslSocketFactory = sslContext.getSocketFactory();
            mRequestExecutor = Executors.newFixedThreadPool(10);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            final Intent intent = new Intent(AuthService.this, ConnectionReceiver.class);
            try {
                if (!SonarCloudApp.getInstance().isConnected()) {
                    intent.setAction(Api.CONNECTION_LOST);
                    sendBroadcast(intent);
                    isConnected = false;
                    return;
                }
                Looper.prepare();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isConnected) return;
                        intent.setAction(Api.CONNECTION_TIME_OUT);
                        sendBroadcast(intent);
                        isConnected = false;
                        try {
                            if (dataSocket != null)
                                dataSocket.close();
                            dataSocket = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            new Connection();
                        }
                    }
                }, 10000);
                // create a new instance of socket and connect it to server
                dataSocket = (SSLSocket) sslSocketFactory.createSocket(
                        new Socket(Api.M_URL, Api.PORT), Api.M_URL, Api.PORT, false
                );
                dataSocket.setKeepAlive(true);
                dataSocket.setUseClientMode(true);
                isConnected = true;

                intent.setAction(Api.CONNECTION_SUCCEED);
                sendBroadcast(intent);
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage());
                intent.setAction(Api.CONNECTION_FAILED);
                sendBroadcast(intent);
                isConnected = false;
            }
        }
    }

    /**
     * Restart socket connection
     */
    public void restartConnection() {
        new Reconnect();
    }

    /**
     * Runnable used to restart the connection, it will close the previous connection if the socket
     * were connected and then will establish a new one
     */
    class Reconnect implements Runnable {

        public Reconnect() {
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                if (dataSocket != null && dataSocket.isConnected()) {
                    dataSocket.close();
                }
                new Connection();
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
         * @param message for server
         */
        public SendRequest(JSONObject message) {
            // give the message for server and socket object to current thread
            this.message = message;
        }

        @Override
        public void run() {
            try {
                // Start socket handshake
                dataSocket.startHandshake();
                // Create a reader and writer from socket output and input streams
                writeOut = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));
                // send the request to server through writer object
                writeOut.write(message.toString());
                writeOut.newLine();
                writeOut.flush();
                readIn = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
                String line = readIn.readLine();
                sendResponseToUI(line, message);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(this.getClass().getSimpleName(), e.getMessage());
                // send the response to ui
                Intent responseContainer = new Intent(Api.EXCEPTION);
//                responseContainer.putExtra(command, line);
                responseContainer.putExtra(Api.REQUEST_MESSAGE, message.toString());
                sendBroadcast(responseContainer);
            }
        }
    }

    private void sendResponseToUI(String response, JSONObject message) {
        String command = Api.EXCEPTION;
        try {
            JSONObject jsonResponse = new JSONObject(response);
            command = jsonResponse.optString("originalCommand", Api.EXCEPTION);
        } catch (JSONException e) {
            Log.e(this.getClass().getName(), "Finally " + e.getMessage());
        } finally {
            // send the response to ui
            Intent responseContainer = new Intent(command);
            responseContainer.putExtra(command, response);
            responseContainer.putExtra(Api.REQUEST_MESSAGE, message.toString());
            sendBroadcast(responseContainer);
        }
    }

    /**
     * Close socket connection and set socket to null to free the resources
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dataSocket.close();
                    mRequestExecutor.shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dataSocket = null;
            }
        }).start();
    }
}
