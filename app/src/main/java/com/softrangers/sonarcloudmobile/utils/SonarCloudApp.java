package com.softrangers.sonarcloudmobile.utils;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.IBinder;

import com.softrangers.sonarcloudmobile.models.User;
import com.softrangers.sonarcloudmobile.utils.api.SocketService;

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

    public static final String NO_IDENTIFIER = "no identifier";
    public static final String NO_DATA = "no_user_server_data";
    private static final String IS_FIRST_LAUNCH = "is first launch";

    public static int SEQ_VALUE;

    private static SonarCloudApp instance;
    public static Typeface avenirBook;
    public static Typeface avenirMedium;
    private static SharedPreferences preferences;
    public static User user;
    public static SocketService socketService;


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
        Intent socketIntent = new Intent(this, SocketService.class);
        startService(socketIntent);

        // bind current class to the SocketService
        bindService(new Intent(this, SocketService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }



    // needed to bind SocketService to current class
    protected ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // get the service instance
            socketService = ((SocketService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // remove service instance
            socketService = null;
        }
    };

    public void setIsFirstLaunch(boolean isFirstLaunch) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(IS_FIRST_LAUNCH, isFirstLaunch);
        editor.apply();
    }

    public void addNewRecording(int recordingNumber) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("recording_counter", recordingNumber);
        editor.apply();
    }

    public int getLastRecordingNumber() {
        return preferences.getInt("recording_number", 0);
    }

    public boolean isFirstLaunch() {
        return preferences.getBoolean(IS_FIRST_LAUNCH, false);
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

    public void clearUserSession() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(LOGIN_STATUS);
        editor.remove(USER_ID);
        editor.remove(USER_DATA);
        editor.remove(USER_IDENTIFIER);
        editor.apply();
    }
}
