package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Eduard Albu on 12 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class Receiver implements Parcelable {

    private String mName;
    private boolean isSelected;

    public Receiver() {

    }

    protected Receiver(Parcel in) {
        mName = in.readString();
        isSelected = in.readByte() != 0;
    }

    public static final Creator<Receiver> CREATOR = new Creator<Receiver>() {
        @Override
        public Receiver createFromParcel(Parcel in) {
            return new Receiver(in);
        }

        @Override
        public Receiver[] newArray(int size) {
            return new Receiver[size];
        }
    };

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }
}
