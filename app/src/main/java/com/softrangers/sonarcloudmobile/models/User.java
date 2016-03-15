package com.softrangers.sonarcloudmobile.models;

import org.json.JSONObject;

/**
 * Created by Eduard Albu on 12 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class User {

    private String mName;
    private String mEmail;
    private String mPassword;
    private String mId;
    private String mIdentifier;
    private String mSecret;
    private boolean isActive;
    private String mCreated;
    private String mModified;

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
}
