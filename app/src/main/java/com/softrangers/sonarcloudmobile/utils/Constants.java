package com.softrangers.sonarcloudmobile.utils;

import java.io.File;

/**
 * Created by eduard on 4/4/16.
 */
public class Constants {
    public static final String DB_CACHE_DIR =
            SonarCloudApp.getInstance().getBaseContext().getCacheDir().getPath() + File.separator
                    + "dbcache" + File.separator;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "response_results";

    public static final String RESPONSE_TABLE = "response_table";
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String CREATE_RESPONSE_TABLE = "CREATE TABLE IF NOT EXISTS " + RESPONSE_TABLE
            + " (" + REQUEST + " VARCHAR(255) PRIMARY KEY, " + RESPONSE + " TEXT);";
    public static final String DROP_TABLE_RESPONSE = "DROP TABLE IF EXISTS " + RESPONSE_TABLE;
}
