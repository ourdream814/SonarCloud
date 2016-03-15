package com.softrangers.sonarcloudmobile.utils.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.softrangers.sonarcloudmobile.utils.OnResponseListener;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Eduard Albu on 13 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class ResponseReceiver extends BroadcastReceiver {

    private static ArrayList<OnResponseListener> listeners;
    private static ResponseReceiver instance;

    public static synchronized ResponseReceiver getInstance() {
        listeners = new ArrayList<>();
        if (instance == null) instance = new ResponseReceiver();
        return instance;
    }

    /**
     * Add a new listener
     * @param listener to add to the list
     */
    public void addOnResponseListener(OnResponseListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the given listener
     * @param listener to remove from list
     */
    public void removeOnResponseListener(OnResponseListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // check if the intent have the right action
        switch (action) {
            case Api.RESPONSE_BROADCAST:
                String response = intent.getExtras().getString(Api.RESPONSE_MESSAGE);
                try {
                    JSONObject object = new JSONObject(response);
                    // get the request status from response
                    boolean success = object.getBoolean("success");
                    Api.SEQ_VALUE = object.getInt("seq") + 1;
                    // check status and inform listeners about either response or error
                    if (success) {
                        for (OnResponseListener l : listeners) {
                            if (l != null) {
                                l.onResponse(object);
                            }
                        }
                    } else {
                        for (OnResponseListener l : listeners) {
                            if (l != null) {
                                l.onCommandFailure(object.getString("message"));
                            }
                        }
                    }
                } catch (Exception e) {
                    // Inform listeners about error in case of exception
                    for (OnResponseListener l : listeners) {
                        if (l != null) {
                            l.onError();
                        }
                    }
                    e.printStackTrace();
                }
                break;
        }
    }
}
