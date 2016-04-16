package com.softrangers.sonarcloudmobile.utils.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import java.util.ArrayList;

/**
 * Created by eduard on 3/14/16.
 */
public class ConnectionReceiver extends BroadcastReceiver {

    private static ArrayList<OnConnected> listeners;
    private static ConnectionReceiver instance;

    public static synchronized ConnectionReceiver getInstance() {
        listeners = new ArrayList<>();
        if (instance == null) instance = new ConnectionReceiver();
        return instance;
    }

    /**
     * Add a new listener
     * @param listener to add to the list
     */
    public void addOnConnectedListener(OnConnected listener) {
        listeners.add(listener);
    }

    /**
     * Remove the given listener
     * @param listener to remove from list
     */
    public void removeOnResponseListener(OnConnected listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (listeners == null || listeners.size() <= 0) return;
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                for (OnConnected connected : listeners) {
                    connected.onInternetConnectionRestored();
                }
            } else if (networkInfo == null || !networkInfo.isConnected()) {
                for (OnConnected connected : listeners) {
                    connected.onInternetConnectionLost();
                }
            }
            return;
        }
        String action = intent.getAction();
        switch (action) {
            case Api.CONNECTION_SUCCEED:
                for (OnConnected connected : listeners) {
                    connected.onSocketConnected();
                }
                break;
            case Api.CONNECTION_FAILED:
                for (OnConnected connected : listeners) {
                    connected.onConnectionFailed();
                }
                break;
            case Api.CONNECTION_TIME_OUT:
                for (OnConnected connected : listeners) {
                    connected.onConnectTimeOut();
                }
                break;
            case Api.AUDIO_CONNECTION_CLOSED:
                for (OnConnected connected : listeners) {
                    connected.onAudioConnectionClosed();
                }
                break;

        }
    }

    public interface OnConnected {
        void onInternetConnectionRestored();
        void onInternetConnectionLost();
        void onSocketConnected();
        void onConnectionFailed();
        void onConnectTimeOut();
        void onAudioConnectionClosed();
    }
}
