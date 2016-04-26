package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.softrangers.sonarcloudmobile.utils.FileLog;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.joda.time.DateTime;
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
import java.util.Arrays;
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
    private AudioConnection mAudioConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // initialize socket factory and all-trust TrustyManager
            SecureRandom secureRandom = new SecureRandom();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, SonarCloudApp.getInstance().getTrustManagers(), secureRandom);
            sslSocketFactory = sslContext.getSocketFactory();
            mAudioConnection = new AudioConnection("AudioConnection");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mAudioConnection.start();
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
            Message message = mAudioConnection.mHandler.obtainMessage();
            message.obj = request.toString();
            mAudioConnection.mHandler.sendMessage(message);
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
    class AudioConnection extends Thread {

        Handler mHandler;
        public AudioConnection(String name) {
            super(name);
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
                writeOut = new BufferedWriter(new OutputStreamWriter(outputStream));
                readIn = new BufferedReader(new InputStreamReader(inputStream));
                Looper.prepare();
                ResponseReader responseReader = new ResponseReader(readIn);
                responseReader.start();
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        try {
                            String message = (String) msg.obj;
                            FileLog.getInstance().write(new DateTime().toString() + " Send request: " + message + "; port: " + audioSocket.getPort() + "; url: " + audioSocket.getInetAddress());
                            writeOut.write(msg.obj.toString());
                            writeOut.newLine();
                            writeOut.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                Looper.loop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class ResponseReader extends Thread {
        BufferedReader mReader;

        public ResponseReader(BufferedReader reader) {
            mReader = reader;
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = readIn.readLine()) != null) {
                    sendResponseToUI(line);
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
            FileLog.getInstance().write(new DateTime().toString() + " Send bytes: " + Arrays.toString(bytes) + "; port: " + audioSocket.getPort() + "; url: " + audioSocket.getInetAddress());
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

    private void sendResponseToUI(String response) {
        String command = Api.EXCEPTION;
        try {
            FileLog.getInstance().write(new DateTime().toString() + " Receive message: " + response + "; port: " + audioSocket.getPort() + "; url: " + audioSocket.getInetAddress());
            JSONObject jsonResponse = new JSONObject(response);
            command = jsonResponse.optString("message", Api.EXCEPTION);
        } catch (JSONException e) {
            FileLog.getInstance().write(new DateTime().toString() + " Error: " + e.getMessage() + "; port: " + audioSocket.getPort() + "; url: " + audioSocket.getInetAddress());
        } finally {
            // send the response to ui
            Intent responseContainer = new Intent(command);
            responseContainer.putExtra(command, response);
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
