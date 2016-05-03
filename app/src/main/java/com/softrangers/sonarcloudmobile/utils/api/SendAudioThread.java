package com.softrangers.sonarcloudmobile.utils.api;

import android.os.Handler;

import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by eduard on 29.04.16.
 *
 */
public class SendAudioThread extends Thread {

    public static final int SENDING_FAILED = -1;
    public static final int SENDING_SUCCEED = 0;
    public static final int SENDING_STARTED = 1;
    public static final String READY_FOR_DATA = "Ready for data.";

    private byte[] mAudioData;
    private JSONObject mRequest;
    private SSLSocketFactory mSSLSocketFactory;
    private Handler mHandler;
    private boolean mIsFailed;

    public SendAudioThread(byte[] audioData, JSONObject request, Handler handler) {
        mAudioData = audioData;
        mRequest = request;
        mHandler = handler;
        try {
            // initialize socket factory and all-trust TrustyManager
            SecureRandom secureRandom = new SecureRandom();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, SonarCloudApp.getInstance().getTrustManagers(), secureRandom);
            mSSLSocketFactory = sslContext.getSocketFactory();
            mIsFailed = false;
            mHandler.sendEmptyMessage(SENDING_STARTED);
        } catch (Exception e) {
            mHandler.sendEmptyMessage(SENDING_FAILED);
            mIsFailed = true;
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        SSLSocket sslSocket = null;
        try {
            if (!mIsFailed) {
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(Api.M_URL, Api.AUDIO_PORT);
                socket.connect(socketAddress, 7000);
                sslSocket = (SSLSocket) mSSLSocketFactory.createSocket(socket, Api.M_URL, Api.AUDIO_PORT, true);
                // Start socket handshake
                if (sslSocket.isConnected()) {
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
                            outputStream.write(mAudioData, 0, mAudioData.length);
                            outputStream.flush();
                        } else {
                            mHandler.sendEmptyMessage(SENDING_FAILED);
                            return;
                        }
                        mHandler.sendEmptyMessage(SENDING_SUCCEED);
                    } else {
                        mHandler.sendEmptyMessage(SENDING_FAILED);
                    }
                } else {
                    mHandler.sendEmptyMessage(SENDING_FAILED);
                }
            } else {
                mHandler.sendEmptyMessage(SENDING_FAILED);
            }
        } catch (Exception e) {
            mHandler.sendEmptyMessage(SENDING_FAILED);
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
