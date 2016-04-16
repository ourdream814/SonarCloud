package com.softrangers.sonarcloudmobile.utils.api;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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
public class AudioSocket {

    public static SSLSocket audioSocket;
    public OutputStream mOutputStream;
    public static BufferedReader readIn;
    public static BufferedWriter writeOut;
    private static SSLSocketFactory sslSocketFactory;
    private static ExecutorService mResponseExecutor;
    private static ExecutorService mRequestExecutor;
    private static AudioSocket instance;
    private static boolean isConnected;

    private AudioSocket() {
        try {
            // initialize socket factory and all-trust TrustyManager
            SecureRandom secureRandom = new SecureRandom();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, SonarCloudApp.getInstance().getTrustManagers(), secureRandom);
            sslSocketFactory = sslContext.getSocketFactory();
            mResponseExecutor = Executors.newFixedThreadPool(5);
            mRequestExecutor = Executors.newFixedThreadPool(5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized AudioSocket getInstance() {
        if (instance == null) instance = new AudioSocket();
        return instance;
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

    public void setAudioConnection() {
        new AudioConnection();
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

    public void closeAudioConnection() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (audioSocket != null)
                        audioSocket.close();
                    audioSocket = null;
                    isConnected = false;
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
                Looper.prepare();
                if (!SonarCloudApp.getInstance().isConnected()) {
                    intent.setAction(Api.CONNECTION_FAILED);
                    SonarCloudApp.getInstance().sendBroadcast(intent);
                    Log.e(this.getClass().getSimpleName(), "run()");
                    return;
                }
                audioSocket = (SSLSocket) sslSocketFactory.createSocket(
                        new Socket(Api.M_URL, Api.AUDIO_PORT), Api.M_URL, Api.AUDIO_PORT, true);
            } catch (Exception e) {
                closeAudioConnection();
            } finally {
                Looper.loop();
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
                    Intent responseContainer = new Intent(Api.AUDIO_DATA_RESULT);
                    SonarCloudApp.getInstance().sendBroadcast(responseContainer);
                }
            } catch (Exception e) {
                closeAudioConnection();
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
                if (audioSocket == null || !audioSocket.isConnected()) {
                    new AudioConnection();
                }

                if (audioSocket != null && audioSocket.isConnected()) {
                    // Start socket handshake
                    audioSocket.startHandshake();
                    if (audioSocket.isClosed()) new AudioConnection();
                    // Create a reader and writer from socket output and input streams
                    writeOut = new BufferedWriter(new OutputStreamWriter(audioSocket.getOutputStream()));
                    // send the request to server through writer object
                    writeOut.write(message.toString() + "\n");
                    writeOut.flush();
                    readIn = new BufferedReader(new InputStreamReader(audioSocket.getInputStream()));
                    mResponseExecutor.execute(new ReceiveMessage(readIn, message));
                }
            } catch (Exception e) {
                closeAudioConnection();
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
            try {
                String line = mReader.readLine();
                String command = Api.EXCEPTION;
                try {
                    JSONObject response = new JSONObject(line);
                    command = response.optString("originalCommand", Api.EXCEPTION);
                } catch (JSONException e) {
                    closeAudioConnection();
                } finally {
                    // send the response to ui
                    Intent responseContainer = new Intent(command);
                    responseContainer.putExtra(command, line);
                    responseContainer.putExtra(Api.REQUEST_MESSAGE, mRequest.toString());
                    SonarCloudApp.getInstance().sendBroadcast(responseContainer);
                }
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage());
            }
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
                byte[] buffer = new byte[1024];
                InputStream inputStream = audioSocket.getInputStream();
                BufferedOutputStream baos = new BufferedOutputStream(new FileOutputStream(mFile));
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                baos.flush();
            } catch (Exception e) {
                closeAudioConnection();
            } finally {
                Intent intent = new Intent(Api.AUDIO_READY_TO_PLAY);
                intent.putExtra(Api.AUDIO_READY_TO_PLAY, mFile.getAbsolutePath());
                SonarCloudApp.getInstance().sendBroadcast(intent);
            }
        }
    }
}
