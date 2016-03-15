package com.softrangers.sonarcloudmobile.utils.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case Api.CONNECTION_BROADCAST:
                for (OnConnected connected : listeners) {
                    connected.onSocketConnected();
                }
        }
    }

    public interface OnConnected {
        void onSocketConnected();
    }
}
