package com.softrangers.sonarcloudmobile.utils;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Typeface;

import com.softrangers.sonarcloudmobile.models.User;

/**
 * Created by eduard on 3/12/16.
 *
 */
public class SonarCloudApp extends Application {

    private static final String LOGIN_RESULT = "com.softrangers.sonarcloudmobile.LOGIN_RESULT";
    private static final String LOGIN_STATUS = "login_status";
    private static final String USER_IDENTIFIER = "identifier";
    private static final String USER_ID = "id";

    public static final String NO_IDENTIFIER = "no identifier";

    private static SonarCloudApp instance;
    public static Typeface avenirBook;
    public static Typeface avenirMedium;
    private static SharedPreferences preferences;
    public static User user;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        avenirBook = Typeface.createFromAsset(getAssets(), "fonts/avenir_lt_45_book_0.ttf");
        avenirMedium = Typeface.createFromAsset(getAssets(), "fonts/avenir_lt_65_medium_0.ttf");
        preferences = getSharedPreferences(LOGIN_RESULT, MODE_PRIVATE);
    }

    public static SonarCloudApp getInstance() {
        return instance;
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(LOGIN_STATUS, false);
    }

    public String userId() {
        return preferences.getString(USER_ID, "");
    }

    public String getIdentifier() {
        return preferences.getString(USER_IDENTIFIER, NO_IDENTIFIER);
    }

    public void saveIdentifier(String identifier) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(USER_IDENTIFIER, identifier);
        editor.apply();
    }

    public void saveUserLoginStatus(boolean status, String id) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(LOGIN_STATUS, status);
        editor.putString(USER_ID, id);
        editor.apply();
    }
}
