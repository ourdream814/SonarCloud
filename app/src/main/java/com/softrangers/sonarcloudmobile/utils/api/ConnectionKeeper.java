package com.softrangers.sonarcloudmobile.utils.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import org.json.JSONObject;

/**
 * Created by Eduard Albu on 31 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class ConnectionKeeper extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case Api.KEEP_CONNECTION:
                sendKeepConnectionRequest();
                break;
            case Api.Command.NOOP:
                try {
                    JSONObject response = new JSONObject(intent.getExtras().getString(Api.Command.NOOP));
                    boolean success = response.optBoolean("success", false);
                    if (!success) SonarCloudApp.socketService.restartConnection();
                } catch (Exception e) {
                    SonarCloudApp.socketService.restartConnection();
                }
                break;
        }
    }

    private void sendKeepConnectionRequest() {
        try {
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.NOOP);
            if (SonarCloudApp.socketService != null) {
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            }
        } catch (Exception e) {

        }
    }
}