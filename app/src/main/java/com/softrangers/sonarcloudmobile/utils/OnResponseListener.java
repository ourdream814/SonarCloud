package com.softrangers.sonarcloudmobile.utils;

import org.json.JSONObject;

/**
 * Created by eduard on 3/13/16.
 */
public interface OnResponseListener {

    void onResponse(JSONObject response);
    void onCommandFailure(String message);
    void onError();
}
