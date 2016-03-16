package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * Created by Eduard Albu on 12 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class User implements Parcelable {

    private String mName;
    private String mEmail;
    private String mPassword;
    private String mId;
    private String mIdentifier;
    private String mSecret;
    private boolean isActive;
    private String mCreated;
    private String mModified;

    public User() {}

    protected User(Parcel in) {
        mName = in.readString();
        mEmail = in.readString();
        mPassword = in.readString();
        mId = in.readString();
        mIdentifier = in.readString();
        mSecret = in.readString();
        isActive = in.readByte() != 0;
        mCreated = in.readString();
        mModified = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

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

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getIdentifier() {
        return mIdentifier;
    }

    public void setIdentifier(String identifier) {
        mIdentifier = identifier;
    }

    public String getSecret() {
        return mSecret;
    }

    public void setSecret(String secret) {
        mSecret = secret;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreated() {
        return mCreated;
    }

    public void setCreated(String created) {
        mCreated = created;
    }

    public String getModified() {
        return mModified;
    }

    public void setModified(String modified) {
        mModified = modified;
    }

    public static User build(JSONObject response) {
        User user = new User();
        try {
            user.setId(response.getString("userID"));
            user.setName(response.getString("name"));
            user.setActive(response.getBoolean("active"));
            user.setCreated(response.getString("created"));
            user.setModified(response.getString("modified"));
            user.setEmail(response.getString("email"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mEmail);
        dest.writeString(mPassword);
        dest.writeString(mId);
        dest.writeString(mIdentifier);
        dest.writeString(mSecret);
        dest.writeByte((byte) (isActive ? 1 : 0));
        dest.writeString(mCreated);
        dest.writeString(mModified);
    }
}
