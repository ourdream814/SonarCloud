package com.softrangers.sonarcloudmobile.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.User;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ConnectionKeeper;
import com.softrangers.sonarcloudmobile.utils.api.DataSocketService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Eduard Albu on 12 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class SonarCloudApp extends Application {

    private static final String LOGIN_RESULT = "com.softrangers.sonarcloudmobile.LOGIN_RESULT";
    private static final String LOGIN_STATUS = "login_status";
    private static final String USER_IDENTIFIER = "identifier";
    private static final String USER_ID = "id";
    private static final String USER_DATA = "user_server_data";
    private static final String USER_EMAIL = "user_email";
    private static final String USER_PASS = "user_password";

    public static final String NO_IDENTIFIER = "no identifier";
    public static final String NO_DATA = "no_user_server_data";

    public static int SEQ_VALUE;

    private static SonarCloudApp instance;
    public static Typeface avenirBook;
    public static Typeface avenirMedium;
    private static SharedPreferences preferences;
    public static User user;
    public static DataSocketService dataSocketService;
    public static Intent dataSocketIntent;

    private static AlarmManager alarmManager;
    private static PendingIntent pendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // initialize type faces for the application from assets
        avenirBook = Typeface.createFromAsset(getAssets(), "fonts/avenir_lt_45_book_0.ttf");
        avenirMedium = Typeface.createFromAsset(getAssets(), "fonts/avenir_lt_65_medium_0.ttf");
        // initialize a preferences object
        preferences = getSharedPreferences(LOGIN_RESULT, MODE_PRIVATE);

        // start socket service and connect to server
        dataSocketIntent = new Intent(this, DataSocketService.class);
        // bind current class to the DataSocketService
        bindService(dataSocketIntent, mDataServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void requestUserIdentifier() {
        String identifier = getIdentifier();
        // Start building a request to either create a new or renew existing identifier
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.command(Api.Command.IDENTIFIER);
        requestBuilder.seq(SonarCloudApp.SEQ_VALUE);
        if (SonarCloudApp.NO_IDENTIFIER.equals(identifier)) {
            requestBuilder.action(Api.Action.NEW);
        } else {
            requestBuilder.action(Api.Action.RENEW);
            requestBuilder.identifier(identifier);
        }
        dataSocketService.sendRequest(requestBuilder.build().toJSON());
    }

    /**
     * Start an AlarmManager which will be fired every 50 seconds to send a noop command for server
     * used to keep connection active while app is started
     */
    public void startKeepingConnection() {
        if (isLoggedIn()) {
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(this, ConnectionKeeper.class);
            intent.setAction(Api.KEEP_CONNECTION);
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 50000, pendingIntent);
        }
    }

    // needed to bind DataSocketService to current class
    protected ServiceConnection mDataServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // get the service instance
            dataSocketService = ((DataSocketService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // remove service instance
            dataSocketService = null;
        }
    };

    /**
     * Check if application has permissions to use microphone
     */
    public boolean canUseMicrophone() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Add a new recording number, called when a new audio file is recorded
     * @param recordingNumber for current file and it is incremented by one
     */
    public void addNewRecording(int recordingNumber) {
        SharedPreferences.Editor editor = preferences.edit();
        Log.i("addNewRecordingNumber", "Record Number: " + recordingNumber);
        editor.putInt("recording_counter", recordingNumber + 1);
        editor.apply();
    }

    /**
     * Get the last recorded file number
     * @return either last record number or 0 if there are no records
     */
    public int getLastRecordingNumber() {
        return preferences.getInt("recording_counter", 1);
    }

    /**
     * @return either old or new instance of current class
     */
    public static SonarCloudApp getInstance() {
        return instance;
    }

    /**
     * Check if the user is already logged in
     * @return either true or false
     */
    public boolean isLoggedIn() {
        return preferences.getBoolean(LOGIN_STATUS, false);
    }

    /**
     * @return logged user id
     */
    public String userId() {
        return preferences.getString(USER_ID, "");
    }

    /**
     * @return either logged user identifier or NO_IDENTIFIER holder
     */
    public String getIdentifier() {
        return preferences.getString(USER_IDENTIFIER, NO_IDENTIFIER);
    }

    /**
     * Save user identifier to preferences
     * @param identifier for current user
     */
    public void saveIdentifier(String identifier) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(USER_IDENTIFIER, identifier);
        editor.apply();
    }

    /**
     * Save user secret to preferences
     * @param secret for current user
     */
    public void saveUserSecret(String secret) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(USER_DATA, secret);
        editor.apply();
    }

    /**
     * Save user login data to login in background
     * @param email user email from login screen
     * @param password user password from login screen
     */
    public void saveUserLoginDate(String email, String password) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(USER_EMAIL, email);
        editor.putString(USER_PASS, password);
        editor.apply();
    }

    BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                JSONObject jsonResponse = new JSONObject(intent.getExtras().getString(action));
                boolean success = jsonResponse.optBoolean("success", false);
                if (!success) {
                    return;
                }
                switch (action) {
                    case Api.Command.AUTHENTICATE:
                        onResponseSucceed(jsonResponse);
                        break;
                    case Api.Command.IDENTIFIER:
                        onIdentifierReady(jsonResponse);
                        break;
                }
            } catch (Exception e) {
            }
        }
    };

    /**
     * Called to notify about server response
     * @param response object which contains desired data
     */
    public void onResponseSucceed(JSONObject response) {
        try {
            // Get user id from the response object
            String id = response.getString("userID");

            user.setId(id);

            // request identifier from server
            requestUserIdentifier();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onIdentifierReady(JSONObject response) {
        try {
            // Save identifier and secret to app preferences
            SonarCloudApp.getInstance().saveIdentifier(response.getString(Api.IDENTIFIER));
            SonarCloudApp.getInstance().saveUserSecret(response.getString(Api.SECRET));

            // save identifier and secret to user object for this session
            user.setIdentifier(SonarCloudApp.getInstance().getIdentifier());
            user.setSecret(SonarCloudApp.getInstance().getSavedData());

            saveUserLoginStatus(true, user.getId());
            startKeepingConnection();
            unregisterReceiver(mLoginReceiver);
            dataSocketService.restartConnection();
        } catch (Exception e) {
        }
    }

    /**
     * Get user password from preferences
     * @return either saved password or an empty string
     */
    public String getUserPass() {
        return preferences.getString(USER_PASS, "");
    }

    /**
     * Get user email from preferences
     * @return either saved email or an empty string
     */
    public String getUserEmail() {
        return preferences.getString(USER_EMAIL, "");
    }

    /**
     * @return current user secret
     */
    public String getSavedData() {
        return preferences.getString(USER_DATA, NO_DATA);
    }

    /**
     * Save current user login status
     * @param status either true or false
     * @param id for current user from server
     */
    public void saveUserLoginStatus(boolean status, String id) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(LOGIN_STATUS, status);
        editor.putString(USER_ID, id);
        editor.apply();
    }

    /**
     * Clear all saved user data from preferences
     */
    public void clearUserSession(boolean clearAll) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(LOGIN_STATUS);
        editor.remove(USER_ID);
        editor.remove(USER_DATA);
        editor.remove(USER_IDENTIFIER);
        if (clearAll) {
            editor.remove(USER_EMAIL);
            editor.remove(USER_PASS);
        }
        editor.apply();
    }

    public void stopKeepingConnection() {
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
        alarmManager = null;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unbindService(mDataServiceConnection);
        stopKeepingConnection();
    }
}
