package com.softrangers.sonarcloudmobile.utils.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.softrangers.sonarcloudmobile.utils.OnResponseListener;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by eduard on 3/13/16.
 */
public class ResponseReceiver extends BroadcastReceiver {

    private static ArrayList<OnResponseListener> listeners = new ArrayList<>();

    public static void addOnResponseListener(OnResponseListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch (action) {
            case Api.RESPONSE_BROADCAST:
                String response = intent.getExtras().getString(Api.RESPONSE_MESSAGE);
                try {
                    JSONObject object = new JSONObject(response);
                    boolean success = object.getBoolean("success");

                    if (success) {
                        for (OnResponseListener l : listeners) {
                            l.onResponse(object);
                        }
                    } else {
                        for (OnResponseListener l : listeners) {
                            l.onCommandFailure(object.getString("message"));
                        }
                    }
                } catch (Exception e) {
                    for (OnResponseListener l : listeners) {
                        l.onError();
                    }
                    e.printStackTrace();
                }
                break;
        }
    }
}
