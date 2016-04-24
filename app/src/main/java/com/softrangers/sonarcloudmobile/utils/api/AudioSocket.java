package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by Eduard Albu on 07 04 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class AudioSocket extends Service {

    public static SSLSocket audioSocket;
    public static OutputStream outputStream;
    public static InputStream inputStream;
    public static BufferedReader readIn;
    public static BufferedWriter writeOut;
    private static SSLSocketFactory sslSocketFactory;
    private static ExecutorService mRequestExecutor;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        new AudioConnection();
        return mIBinder;
    }

    private final IBinder mIBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public AudioSocket getService() {
            return AudioSocket.this;
        }
    }

    /**
     * Send a request to server if the connection is ok and send back the response through
     *
     * @param request to send
     */
    public void sendRequest(JSONObject request) {
        if (isAudioConnectionReady() && SonarCloudApp.getInstance().isConnected()) {
            mRequestExecutor.execute(new SendRequest(request));
        } else {
            Intent intent = new Intent(SonarCloudApp.getInstance().getBaseContext(), ConnectionReceiver.class);
            intent.setAction(Api.CONNECTION_FAILED);
            SonarCloudApp.getInstance().sendBroadcast(intent);
        }
    }

    public boolean isAudioConnectionReady() {
        return audioSocket != null && audioSocket.isConnected() && !audioSocket.isClosed();
    }

    public SSLSocket getAudioSocket() {
        return audioSocket;
    }

    public void startReadingAudioData() {
        if (isAudioConnectionReady() && SonarCloudApp.getInstance().isConnected()) {
            new ReadAudioData();
        } else {
            Intent intent = new Intent(SonarCloudApp.getInstance().getBaseContext(), ConnectionReceiver.class);
            intent.setAction(Api.CONNECTION_FAILED);
            SonarCloudApp.getInstance().sendBroadcast(intent);
        }
    }


    /**
     * Send bytes from audio file encoded with "Opus" to server
     *
     * @param audioBytes which needs to be sent
     */
    public void sendAudio(byte[] audioBytes) {
        if (isAudioConnectionReady() && SonarCloudApp.getInstance().isConnected()) {
            new SendAudio(audioBytes);
        } else {
            Intent intent = new Intent(SonarCloudApp.getInstance().getBaseContext(), ConnectionReceiver.class);
            intent.setAction(Api.CONNECTION_FAILED);
            SonarCloudApp.getInstance().sendBroadcast(intent);
            Log.e(this.getClass().getSimpleName(), "sendAudio()");
        }
    }

    /**
     * Connect a new socket to server data port
     */
    class AudioConnection implements Runnable {

        public AudioConnection() {
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            final Intent intent = new Intent(SonarCloudApp.getInstance().getBaseContext(), ConnectionReceiver.class);
            try {
                if (!SonarCloudApp.getInstance().isConnected()) {
                    intent.setAction(Api.CONNECTION_FAILED);
                    SonarCloudApp.getInstance().sendBroadcast(intent);
                    Log.e(this.getClass().getSimpleName(), "run()");
                    return;
                }
                audioSocket = (SSLSocket) sslSocketFactory.createSocket(
                        new Socket(Api.M_URL, Api.AUDIO_PORT), Api.M_URL, Api.AUDIO_PORT, true);
                audioSocket.setKeepAlive(true);
                audioSocket.setUseClientMode(true);

                // Start socket handshake
                audioSocket.startHandshake();
                outputStream = audioSocket.getOutputStream();
                inputStream = audioSocket.getInputStream();
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
                // Start socket handshake
                audioSocket.startHandshake();
                // Create a reader and writer from socket output and input streams
                outputStream.write(mBytes, 0, mBytes.length);
                outputStream.flush();
                Intent responseContainer = new Intent(Api.AUDIO_DATA_RESULT);
                SonarCloudApp.getInstance().sendBroadcast(responseContainer);
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
                // Start socket handshake

                audioSocket.startHandshake();
                // send the request to server through writer object
                writeOut = new BufferedWriter(new OutputStreamWriter(outputStream));
                writeOut.write(message.toString());
                writeOut.newLine();
                writeOut.flush();
                readIn = new BufferedReader(new InputStreamReader(inputStream));
                String line = readIn.readLine();
                sendResponseToUI(line, message);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponseToUI("", message);
            }
        }
    }

    private void sendResponseToUI(String response, JSONObject message) {
        String command = Api.EXCEPTION;
        try {
            JSONObject jsonResponse = new JSONObject(response);
            command = jsonResponse.optString("message", Api.EXCEPTION);
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

    class ReadAudioData implements Runnable {

        final File mFile;

        public ReadAudioData() {
            File dir = new File(SonarCloudApp.getInstance().getCacheDir().getAbsolutePath() + File.separator + "tmp");
            dir.mkdir();
            mFile = new File(dir, "audioSRV.opus");
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                // Start socket handshake
                audioSocket.startHandshake();
                byte[] buffer = new byte[1024];
                BufferedOutputStream baos = new BufferedOutputStream(new FileOutputStream(mFile));
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                baos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Intent intent = new Intent(Api.AUDIO_READY_TO_PLAY);
                intent.putExtra(Api.AUDIO_READY_TO_PLAY, mFile.getAbsolutePath());
                SonarCloudApp.getInstance().sendBroadcast(intent);
            }
        }
    }
}
