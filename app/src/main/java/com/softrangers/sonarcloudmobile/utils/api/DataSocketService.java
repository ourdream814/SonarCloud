package com.softrangers.sonarcloudmobile.utils.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by Eduard Albu on 14 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class DataSocketService extends Service {

    public static SSLSocket dataSocket;
    private static SSLSocketFactory sslSocketFactory;
    public static BufferedReader readIn;
    public static BufferedWriter writeOut;
    public boolean isConnected;
    private ConnectionThread mConnectionThread;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Bind this service to application
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mConnectionThread = new ConnectionThread();
        mConnectionThread.start();
        return mIBinder;
    }

    private final IBinder mIBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public DataSocketService getService() {
            return DataSocketService.this;
        }
    }

    /**
     * Restart socket connection
     */
    public void restartConnection() {
        new Reconnect().start();
    }

    /**
     * Send a request to server if the connection is ok and send back the response through
     *
     * @param request to send
     */
    public void sendRequest(JSONObject request) {
        if (isConnected && SonarCloudApp.getInstance().isConnected()) {
            if (mConnectionThread != null) {
                Message message = mConnectionThread.mHandler.obtainMessage();
                message.obj = request.toString();
                mConnectionThread.mHandler.sendMessage(message);
            }
        } else {
            Intent intent = new Intent(DataSocketService.this, ConnectionReceiver.class);
            intent.setAction(Api.CONNECTION_FAILED);
            sendBroadcast(intent);
        }
    }

    class ConnectionThread extends Thread {
        Handler mHandler;

        @Override
        public void run() {
            Looper.prepare();
            try {
                // check if we have internet connection and if true connect the socket else just return
                if (!SonarCloudApp.getInstance().isConnected()) {
                    Intent intent = new Intent(Api.CONNECTION_LOST);
                    sendBroadcast(intent);
                    isConnected = false;
                    return;
                }

                // create a new instance of socket and connect it to server
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(Api.M_URL, Api.PORT);
                socket.connect(socketAddress, 7000);
                dataSocket = (SSLSocket) sslSocketFactory.createSocket(socket, Api.M_URL, Api.PORT, false);

                dataSocket.setKeepAlive(true);
                dataSocket.setUseClientMode(true);

                // Start socket handshake
                dataSocket.startHandshake();
                // Create a reader and writer from socket output and input streams
                readIn = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
                writeOut = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));

                // create a handler to receive requests from worker thread and send them to server
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        try {
                            String message = (String) msg.obj;
                            writeOut.write(message);
                            writeOut.newLine();
                            writeOut.flush();
                        } catch (Exception e) {
                            Intent intent = new Intent(Api.CONNECTION_LOST);
                            sendBroadcast(intent);
                            e.printStackTrace();
                        }
                    }
                };

                isConnected = dataSocket.isConnected();

                // start reading response thread so it will wait for server responses
                new ResponseThread(readIn).start();

                // send info about connection success to listeners so they can update ui
                Intent intent = new Intent(Api.CONNECTION_SUCCEED);
                sendBroadcast(intent);

                // try to authenticate to server if user is logged in
                if (SonarCloudApp.getInstance().isLoggedIn()) {
                    Request.Builder builder = new Request.Builder();
                    builder.command(Api.Command.AUTHENTICATE);
                    builder.device(Api.Device.CLIENT).method(Api.Method.IDENTIFIER).identifier(SonarCloudApp.getInstance().getIdentifier())
                            .secret(SonarCloudApp.getInstance().getSavedData()).seq(SonarCloudApp.SEQ_VALUE);
                    sendRequest(builder.build().toJSON());
                }
            } catch (Exception e) {
                // inform user about connection fails
                Intent intent = new Intent(Api.CONNECTION_FAILED);
                sendBroadcast(intent);
                isConnected = false;
            } finally {
                Looper.loop();
            }
        }
    }

    class ResponseThread extends Thread {
        BufferedReader mReader;

        public ResponseThread(BufferedReader reader) {
            mReader = reader;
        }

        @Override
        public void run() {
            try {
                // start reading the response in a loop so the thread can wait for further responses
                // it will be paused till a new response will arrive, if the thread will exit the loop
                // this means that socket was disconnected, try to connect again
                String line;
                while ((line = mReader.readLine()) != null) {
                    sendResponseToUI(line);
                }
                // if we got here than socket is disconnected
                // let's try to connect again
                isConnected = false;
            } catch (Exception e) {
                Intent intent = new Intent(Api.CONNECTION_TIME_OUT);
                sendBroadcast(intent);
//                try {
//                    if (dataSocket != null)
//                        dataSocket.close();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                } finally {
//                    isConnected = false;
//                }
            }
        }
    }

    /**
     * Runnable used to restart the connection, it will close the previous connection if the socket
     * were connected and then will establish a new one
     */
    class Reconnect extends Thread {
        @Override
        public void run() {
            try {
                // try to close current socket
                if (dataSocket != null && dataSocket.isConnected()) {
                    dataSocket.close();
                }
                // start a new connection thread and try to connect again
                mConnectionThread = null;
                mConnectionThread = new ConnectionThread();
                mConnectionThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendResponseToUI(String response) throws IOException {
        String command;
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String isCommand = jsonResponse.optString("command", null);
            if (isCommand == null) {
                command = jsonResponse.optString("originalCommand", Api.EXCEPTION);
                // send the response to ui
                Intent responseContainer = new Intent(command);
                responseContainer.putExtra(command, response);
                sendBroadcast(responseContainer);
            }
        } catch (JSONException e) {
            Log.e(this.getClass().getName(), "Finally " + e.getMessage());
            Intent intent = new Intent(Api.CONNECTION_FAILED);
            sendBroadcast(intent);
        }
    }

    /**
     * Close socket connection and set socket to null to free the resources
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    readIn.close();
//                    writeOut.close();
//                    dataSocket.close();
//                    isConnected = false;
//                    Log.i(this.getClass().getSimpleName(), "onDestroy(): Socket closed");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    dataSocket = null;
//                }
//            }
//        }).start();
    }
}
