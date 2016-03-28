package com.softrangers.sonarcloudmobile.utils;

/**
 * Created by mike on 15/12/15.
 */
public class AudioIn extends Thread {
    private boolean stopped = false;

    public AudioIn() {
        start();
    }

    @Override
    public void run() {

    }

    private void close() {
        stopped = true;
    }
}
