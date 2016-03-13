package com.softrangers.sonarcloudmobile.utils.api;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.softrangers.sonarcloudmobile.utils.OnResponseListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by eduard on 3/12/16.
 */
public class SonarcloudRequest extends AsyncTask<JSONObject, String, String> {

    private static final String TAG = SonarcloudRequest.class.getSimpleName();
    private static SecureRandom secureRandom;
    private SSLSocket mSSLSocket;
    private OnResponseListener mOnResponseListener;

    public SonarcloudRequest(@NonNull OnResponseListener onResponseListener) {
        secureRandom = new SecureRandom();
        mOnResponseListener = onResponseListener;
    }

    @Override
    protected void onPostExecute(String s) {
        try {
            JSONObject response = new JSONObject(s);
            boolean success = response.getBoolean("success");

            if (success) {
                mOnResponseListener.onResponse(response);
            } else {
                String message = response.getString("message");
                mOnResponseListener.onCommandFailure(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            mOnResponseListener.onError();
        }
    }

    @Override
    protected String doInBackground(JSONObject... params) {
        StringBuilder response = new StringBuilder();
        try {
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, getTrustManagers(), secureRandom);
            // Create an ssl socket factory with our all trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            // Create a socket and connect to the server
            mSSLSocket = (SSLSocket) sslSocketFactory.createSocket(
                    new Socket(Api.URL, Api.PORT), Api.URL, Api.PORT, true
            );
            mSSLSocket.startHandshake();
            mSSLSocket.setSoTimeout(2000);
            Log.i(TAG, String.valueOf(mSSLSocket.isConnected()));

            // Create in and out writers to send request and read responses
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(mSSLSocket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(mSSLSocket.getInputStream()));

            String s = params[0].toString() + "\n";
            Log.i(TAG, s);

            // Send the JSON to server
            out.write(s);
            out.flush();
            out.close();

            // TODO: 3/13/16 figure out why it does not exit the loop
            char[] chars = new char[1024];
            int read;
            while ((read = in.read(chars)) != -1) {
                response.append(new String(chars, 0, read));
            }

            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            mOnResponseListener.onError();
        }
        return response.toString();
    }

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
