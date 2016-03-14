package com.softrangers.sonarcloudmobile.utils;

import org.json.JSONObject;

/**
 * Created by Eduard Albu on 13 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public interface OnResponseListener {

    /**
     * Called by ResponseReceiver
     * @param response object from server
     */
    void onResponse(JSONObject response);

    /**
     * Called by ResponseReceiver
     * @param message about server error
     */
    void onCommandFailure(String message);

    /**
     * Called by ResponseReceiver to inform listeners about all unknown errors
     */
    void onError();
}
