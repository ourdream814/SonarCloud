package com.softrangers.sonarcloudmobile.utils.observers;

/**
 * Created by Eduard Albu on 15 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public interface Observable<T> {
    void addObserver(T observer);

    void removeObserver(T observer);

    void notifyObservers();
}
