package com.softrangers.sonarcloudmobile;

import android.app.Application;
import android.graphics.Typeface;

/**
 * Created by eduard on 3/12/16.
 */
public class SonarCloudApp extends Application {

    private static SonarCloudApp instance;
    public static Typeface avenirBook;
    public static Typeface avenirMedium;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        avenirBook = Typeface.createFromAsset(getAssets(), "/fonts/Avenir LT 45 Book_0.ttf");
        avenirMedium = Typeface.createFromAsset(getAssets(), "/fonts/Avenir LT 65 Medium_0.ttf");
    }

    public static SonarCloudApp getInstance() {
        return instance;
    }
}
