package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by Eduard Albu on 12 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class PASystem implements Parcelable {

    private String mName;
    private ArrayList<Receiver> mReceivers;

    public PASystem() {

    }

    protected PASystem(Parcel in) {
        mName = in.readString();
        mReceivers = in.createTypedArrayList(Receiver.CREATOR);
    }

    public static final Creator<PASystem> CREATOR = new Creator<PASystem>() {
        @Override
        public PASystem createFromParcel(Parcel in) {
            return new PASystem(in);
        }

        @Override
        public PASystem[] newArray(int size) {
            return new PASystem[size];
        }
    };

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public ArrayList<Receiver> getReceivers() {
        return mReceivers;
    }

    public void setReceivers(ArrayList<Receiver> receivers) {
        mReceivers = receivers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeTypedList(mReceivers);
    }
}
