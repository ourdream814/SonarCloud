package com.softrangers.sonarcloudmobile.utils.api;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by eduard on 29.04.16.
 */
public class GetAudioThread extends Thread {

    public static final int READING_FAILED = -1;
    public static final int READING_SUCCEED = 0;
    public static final int READING_STARTED = 1;
    public static final String READY_FOR_DATA = "Ready for data.";

    File mFile;
    private JSONObject mRequest;
    private SSLSocketFactory mSSLSocketFactory;
    private Handler mHandler;
    private boolean mIsFailed;

    public GetAudioThread(JSONObject request, Handler handler) {
        File dir = new File(SonarCloudApp.getInstance().getCacheDir().getAbsolutePath() + File.separator + "tmp");
        dir.mkdir();
        mFile = new File(dir, "audioSRV.opus");
        mRequest = request;
        mHandler = handler;
        try {
            // initialize socket factory and all-trust TrustyManager
            SecureRandom secureRandom = new SecureRandom();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, SonarCloudApp.getInstance().getTrustManagers(), secureRandom);
            mSSLSocketFactory = sslContext.getSocketFactory();
            mIsFailed = false;
            mHandler.sendEmptyMessage(READING_STARTED);
        } catch (Exception e) {
            mHandler.sendEmptyMessage(READING_FAILED);
            mIsFailed = true;
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        SSLSocket sslSocket = null;
        try {
            if (!mIsFailed) {
                sslSocket = (SSLSocket) mSSLSocketFactory.createSocket(
                        new Socket(Api.M_URL, Api.AUDIO_PORT), Api.M_URL, Api.AUDIO_PORT, true);
                // Start socket handshake
                sslSocket.startHandshake();
                OutputStream outputStream = sslSocket.getOutputStream();
                InputStream inputStream = sslSocket.getInputStream();
                BufferedWriter writeOut = new BufferedWriter(new OutputStreamWriter(outputStream));
                BufferedReader readIn = new BufferedReader(new InputStreamReader(inputStream));
                writeOut.write(mRequest.toString());
                writeOut.newLine();
                writeOut.flush();
                String line = readIn.readLine();
                if (line != null) {
                    JSONObject jsonResponse = new JSONObject(line);
                    String message = jsonResponse.optString("message", Api.EXCEPTION);
                    if (READY_FOR_DATA.equalsIgnoreCase(message)) {
                        byte[] buffer = new byte[1024];
                        BufferedOutputStream baos = new BufferedOutputStream(new FileOutputStream(mFile));
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                    } else {
                        mHandler.sendEmptyMessage(READING_FAILED);
                        return;
                    }
                    Message msg = mHandler.obtainMessage();
                    msg.obj = mFile.getAbsolutePath();
                    msg.what = READING_SUCCEED;
                    mHandler.sendMessage(msg);
                } else {
                    mHandler.sendEmptyMessage(READING_FAILED);
                }
            }
        } catch (Exception e) {
            mHandler.sendEmptyMessage(READING_FAILED);
            e.printStackTrace();
        } finally {
            if (sslSocket != null) {
                try {
                    sslSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
