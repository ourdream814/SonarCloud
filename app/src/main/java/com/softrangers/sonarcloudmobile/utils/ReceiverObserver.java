package com.softrangers.sonarcloudmobile.utils;

import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;

import java.util.ArrayList;

/**
 * Created by Eduard Albu on 15 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public interface ReceiverObserver {
    void update(PASystem paSystem, ArrayList<Receiver> receivers);
}
