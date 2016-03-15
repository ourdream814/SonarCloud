package com.softrangers.sonarcloudmobile.utils;

/**
 * Created by Eduard Albu on 15 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public interface ReceiversObservable {
    void addObserver(ReceiverObserver observer);

    void removeObserver(ReceiverObserver observer);

    void notifyObservers();
}
