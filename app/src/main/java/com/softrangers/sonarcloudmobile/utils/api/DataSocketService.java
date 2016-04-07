package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.utils.cache.DBManager;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class DataSocketService extends Service {

    public static SSLSocket dataSocket;
    private static SSLSocketFactory sslSocketFactory;
    public BufferedReader readIn;
    public BufferedWriter writeOut;
    public boolean isConnected;
    private ExecutorService mResponseExecutor;
    private ExecutorService mRequestExecutor;

    // Bind this service to application
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    private final IBinder mIBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public DataSocketService getService() {
            return DataSocketService.this;
        }
    }

    /**
     * Send a request to server if the connection is ok and send back the response through
     *
     * @param request to send
     */
    public void sendRequest(JSONObject request) {
        mRequestExecutor.execute(new SendRequest(request));
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
            mResponseExecutor = Executors.newFixedThreadPool(10);
            mRequestExecutor = Executors.newFixedThreadPool(10);
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
                Looper.prepare();
                // create a new instance of socket and connect it to server
                dataSocket = (SSLSocket) sslSocketFactory.createSocket(
                        new Socket(Api.URL, Api.PORT), Api.M_URL, Api.PORT, true
                );

                if (SonarCloudApp.getInstance().isLoggedIn()) {
                    Request.Builder builder = new Request.Builder();
                    builder.command(Api.Command.AUTHENTICATE);
                    builder.device(Api.Device.CLIENT).method(Api.Method.IDENTIFIER).identifier(SonarCloudApp.getInstance().getIdentifier())
                            .secret(SonarCloudApp.getInstance().getSavedData()).seq(SonarCloudApp.SEQ_VALUE);
                    sendRequest(builder.build().toJSON());
                    SonarCloudApp.getInstance().startKeepingConnection();
                }

                Intent intent = new Intent(DataSocketService.this, ConnectionReceiver.class);
                intent.setAction(Api.CONNECTION_BROADCAST);
                sendBroadcast(intent);
                Log.i(this.getClass().getSimpleName(), "Connection restarted");
                isConnected = true;
            } catch (Exception e) {
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
         *
         * @param message for server
         */
        public SendRequest(JSONObject message) {
            // give the message for server and socket object to current thread
            this.message = message;
        }

        @Override
        public void run() {
            try {
                if (dataSocket == null || !dataSocket.isConnected()) {
                    new Connection();
                }

                if (dataSocket != null && dataSocket.isConnected()) {
                    // Start socket handshake
                    dataSocket.startHandshake();
                    if (dataSocket.isClosed()) new Connection();
                    // Create a reader and writer from socket output and input streams
                    writeOut = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));
                    // send the request to server through writer object
                    writeOut.write(message.toString() + "\n");
                    writeOut.flush();
                    readIn = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
                    mResponseExecutor.execute(new ReceiveMessage(readIn, message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Receives and read message obtained from server
     */
    class ReceiveMessage implements Runnable {

        final BufferedReader mReader;
        JSONObject mRequest;

        public ReceiveMessage(BufferedReader reader, JSONObject request) {
            mReader = reader;
            mRequest = request;
        }

        @Override
        public void run() {
            JSONObject response;
            String stringResponse = null;
            try {
                char[] buffer = new char[1024];
                StringBuilder builder = new StringBuilder();
                builder.append(mReader.readLine());

                while (mReader.ready()) {
                    mReader.read(buffer, 0, buffer.length);
                    builder.append(buffer);
                }

                stringResponse = builder.toString();
                Log.i(this.getClass().getSimpleName(), "Response: " + stringResponse);
                if (stringResponse == null || stringResponse.equals("null")) {
                    stringResponse = DBManager.loadDataFromDB(mRequest);
                    Log.e(this.getClass().getSimpleName(), "Response is: " + stringResponse);
                }

            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage());
            } finally {
                String command = Api.EXCEPTION;
                try {
                    response = new JSONObject(stringResponse);
                    command = response.optString("originalCommand", Api.EXCEPTION);
                } catch (JSONException e) {
                    Log.e(this.getClass().getName(), "Finally " + e.getMessage());
                }
                // send the response to ui
                Intent responseContainer = new Intent(command);
                responseContainer.putExtra(command, stringResponse);
                responseContainer.putExtra(Api.REQUEST_MESSAGE, mRequest.toString());
                sendBroadcast(responseContainer);
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
            dataSocket.close();
            isConnected = false;
            mResponseExecutor.shutdown();
            mRequestExecutor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dataSocket = null;
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
