package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
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
public class SocketService extends Service {

    public static SSLSocket dataSocket;
    public static SSLSocket audioSocket;
    private static SSLSocketFactory sslSocketFactory;
    public BufferedReader readIn;
    public BufferedWriter writeOut;
    public OutputStream mOutputStream;
    public boolean isConnected;
    private JSONObject mLastRequest;
    private ExecutorService mExecutorService;

    ServerSocket mServerSocket;

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
     *
     * @param request to send
     */
    public void sendRequest(JSONObject request) {
        new SendRequest(request);
    }

    public void prepareServerForAudio(JSONObject request) {
        new PrepareServer(request);
    }

    public boolean isAudioConnectionReady() {
        return audioSocket != null && audioSocket.isConnected() && !audioSocket.isClosed();
    }

    public void setAudioConnection() {
        new AudioConnection();
    }

    public void closeAudioConnection() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (audioSocket != null)
                        audioSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Send bytes from audio file encoded with "Opus" to server
     *
     * @param audioBytes which needs to be sent
     */
    public void sendAudio(byte[] audioBytes) {
        new SendAudio(audioBytes);
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
            mServerSocket = new ServerSocket(Api.PORT);
            mExecutorService = Executors.newFixedThreadPool(5);
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

    class AudioConnection implements Runnable {

        public AudioConnection() {
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                Looper.prepare();
                audioSocket = (SSLSocket) sslSocketFactory.createSocket(
                        new Socket(Api.URL, Api.AUDIO_PORT), Api.M_URL, Api.AUDIO_PORT, true
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            try {
                Looper.prepare();
                // create a new instance of socket and connect it to server
                dataSocket = (SSLSocket) sslSocketFactory.createSocket(
                        new Socket(Api.URL, Api.PORT), Api.M_URL, Api.PORT, true
                );

                Intent intent = new Intent(SocketService.this, ConnectionReceiver.class);
                intent.setAction(Api.CONNECTION_BROADCAST);
                sendBroadcast(intent);

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
                    new Connection();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runnable used to send the audio file bytes to server
     */
    class SendAudio implements Runnable {

        byte[] mBytes;

        public SendAudio(byte[] bytes) {
            mBytes = bytes;
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            Looper.prepare();
            try {
                if (audioSocket == null || !audioSocket.isConnected()) {
                    new AudioConnection();
                }

                if (audioSocket != null && audioSocket.isConnected()) {
                    // Start socket handshake
                    audioSocket.startHandshake();

                    if (audioSocket.isClosed()) new AudioConnection();
                    // Create a reader and writer from socket output and input streams
                    mOutputStream = audioSocket.getOutputStream();
                    mOutputStream.write(mBytes, 0, mBytes.length);
                    mOutputStream.flush();
                    readIn = new BufferedReader(new InputStreamReader(audioSocket.getInputStream()));
                    // send the request to server through writer object
                    new Thread(new ReceiveMessage(readIn)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runnable which will send requests in a new thread
     */
    class PrepareServer implements Runnable {
        JSONObject message;

        /**
         * Constructor
         *
         * @param message for server
         */
        public PrepareServer(JSONObject message) {
            // give the message for server and socket object to current thread
            this.message = message;
            mLastRequest = message;
            // send message to server
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                Looper.prepare();
                if (audioSocket == null || !audioSocket.isConnected()) {
                    new Connection();
                }

                if (audioSocket != null && audioSocket.isConnected()) {
                    // Start socket handshake
                    audioSocket.startHandshake();

                    if (audioSocket.isClosed()) new Connection();
                    // Create a reader and writer from socket output and input streams
                    writeOut = new BufferedWriter(new OutputStreamWriter(audioSocket.getOutputStream()));
                    readIn = new BufferedReader(new InputStreamReader(audioSocket.getInputStream()));
                    // send the request to server through writer object
                    writeOut.write(message.toString() + "\n");
                    writeOut.flush();
                    new Thread(new ReceiveMessage(readIn)).start();
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
            mLastRequest = message;
            // send message to server
            new Thread(this, this.getClass().getSimpleName()).start();
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
                    Thread.sleep(1000);
                    mExecutorService.execute(new ReceiveMessage(readIn));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    class ReceiveMessage implements Runnable {

        final BufferedReader mReader;

        public ReceiveMessage(BufferedReader reader) {
            mReader = reader;
        }

        @Override
        public void run() {
            try {
                char[] buffer = new char[1024];
                StringBuilder builder = new StringBuilder();
                builder.append(mReader.readLine());
                while (mReader.ready()) {
                    mReader.read(buffer, 0, buffer.length);
                    builder.append(buffer);
                }

                String stringResponse = builder.toString();
                Log.i(this.getClass().getSimpleName(), "Response: " + stringResponse);
                if (stringResponse == null) {
                    stringResponse = "{\"success\":false}";
                    restartConnection();
                    sendRequest(mLastRequest);
                }
                JSONObject response = new JSONObject(stringResponse);
                String command = response.optString("originalCommand", Api.EXCEPTION);
                // send the response to ui
                Intent responseContainer = new Intent();
                responseContainer.setAction(command);
                responseContainer.putExtra(command, stringResponse);
                sendBroadcast(responseContainer);
                mLastRequest = null;
            } catch (Exception e) {
                e.printStackTrace();
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
            mExecutorService.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dataSocket = null;
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
