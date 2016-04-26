package com.softrangers.sonarcloudmobile.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by eduard on 26.04.16.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class FileLog {

    private static final File FILE = new File(SonarCloudApp.getInstance().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Log.txt");
    private static FileWriter writer;
    private static FileLog instance;

    public static synchronized FileLog getInstance() {
        if (instance == null) instance = new FileLog();
        return instance;
    }

    private FileLog() {
        try {
            writer = new FileWriter(FILE, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String message) {
        try {
            writer.write(message);
            writer.write("\n");
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
