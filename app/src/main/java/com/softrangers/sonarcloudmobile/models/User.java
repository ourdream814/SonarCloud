package com.softrangers.sonarcloudmobile.models;

/**
 * Created by eduard on 3/12/16.
 */
public class User {

    private String mEmail;
    private String mPassword;
    private int mId;

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String mPassword) {
        this.mPassword = mPassword;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }
}
