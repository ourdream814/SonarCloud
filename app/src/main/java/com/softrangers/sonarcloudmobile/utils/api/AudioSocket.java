package com.softrangers.sonarcloudmobile.utils.api;

import android.content.Intent;
import android.os.Looper;
import android.util.Log;

import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.cache.DBManager;

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

    private AudioSocket() {
        try {
            // initialize socket factory and all-trust TrustyManager
            SecureRandom secureRandom = new SecureRandom();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, getTrustManagers(), secureRandom);
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
        mRequestExecutor.execute(new SendRequest(request));
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
        new ReadAudioData();
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
     * Connect a new socket to server data port
     */
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
                readIn = new BufferedReader(new InputStreamReader(audioSocket.getInputStream()));
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
                    Log.i(this.getClass().getSimpleName(), "Audio data sent: " + mBytes.length);
                    Intent responseContainer = new Intent(Api.AUDIO_DATA_RESULT);
                    SonarCloudApp.getInstance().sendBroadcast(responseContainer);
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
                SonarCloudApp.getInstance().sendBroadcast(responseContainer);
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
                e.printStackTrace();
            } finally {
                Intent intent = new Intent(Api.AUDIO_READY_TO_PLAY);
                intent.putExtra(Api.AUDIO_READY_TO_PLAY, mFile.getAbsolutePath());
                SonarCloudApp.getInstance().sendBroadcast(intent);
            }
        }
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
