package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Eduard Albu on 14 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class Request implements Parcelable {
    private String command;
    private String device;
    private String method;
    private String email;
    private String password;
    private String identifier;
    private String secret;
    private String groupId;
    private int pin;
    private String action;
    private String userId;
    private String organisationId;
    private String receiverId;
    private int seq = 0;
//    private boolean playImmediately;

    private Request() {

    }

    protected Request(Parcel in) {
        command = in.readString();
        device = in.readString();
        method = in.readString();
        email = in.readString();
        password = in.readString();
        identifier = in.readString();
        secret = in.readString();
        groupId = in.readString();
        pin = in.readInt();
        action = in.readString();
        userId = in.readString();
        organisationId = in.readString();
        receiverId = in.readString();
//        playImmediately = in.readByte() != 0;
    }

    public static final Creator<Request> CREATOR = new Creator<Request>() {
        @Override
        public Request createFromParcel(Parcel in) {
            return new Request(in);
        }

        @Override
        public Request[] newArray(int size) {
            return new Request[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(command);
        dest.writeString(device);
        dest.writeString(method);
        dest.writeString(email);
        dest.writeString(password);
        dest.writeString(identifier);
        dest.writeString(secret);
        dest.writeString(groupId);
        dest.writeInt(pin);
        dest.writeString(action);
        dest.writeString(userId);
        dest.writeString(organisationId);
        dest.writeString(receiverId);
//        dest.writeByte((byte) (playImmediately ? 1 : 0));
    }

    public JSONObject toJSON() {
        try {
            Class cl = this.getClass();
            Field[] fields = cl.getDeclaredFields();
            JSONObject request = new JSONObject();
            HashMap<String, Field> fieldHashMap = new HashMap<>();
            for (Field field : fields) {
                fieldHashMap.put(field.getName(), field);
            }

            for (Map.Entry<String ,Field> set :fieldHashMap.entrySet()) {
                if (set.getValue().get(this) != null &&
                        !set.getValue().getName().equalsIgnoreCase("creator")) {
                    request.put(set.getKey(), set.getValue().get(this));
                }
            }
            return request;
        } catch (Exception e) {
            e.printStackTrace();
            throw new NullPointerException("Cannot get fields");
        }
    }

    public static class Builder {
        private String mCommand;
        private String mDevice;
        private String mMethod;
        private String mEmail;
        private String mPassword;
        private String mIdentifier;
        private String mSecret;
        private String mGroupId;
        private int mPin;
        private String mAction;
        private String mUserId;
        private String mOrganisationId;
        private String mReceiverId;
        private boolean mPlayImmediately;

        public Builder command(String command) {
            mCommand = command;
            return this;
        }

        public Builder device(String device) {
            mDevice = device;
            return this;
        }

        public Builder method(String method) {
            mMethod = method;
            return this;
        }

        public Builder email(String email) {
            mEmail = email;
            return this;
        }

        public Builder password(String password) {
            mPassword = password;
            return this;
        }

        public Builder identifier(String identifier) {
            mIdentifier = identifier;
            return this;
        }

        public Builder secret(String secret) {
            mSecret = secret;
            return this;
        }

        public Builder groupId(String groupId) {
            mGroupId = groupId;
            return this;
        }

        public Builder pin(int pin) {
            mPin = pin;
            return this;
        }

        public Builder action(String action) {
            mAction = action;
            return this;
        }

        public Builder userId(String userId) {
            mUserId = userId;
            return this;
        }

        public Builder organisationId(String organistaionId) {
            mOrganisationId = organistaionId;
            return this;
        }

        public Builder receiverId(String receiverId) {
            mReceiverId = receiverId;
            return this;
        }

        public Builder playImmediately(boolean playImmediately) {
            mPlayImmediately = playImmediately;
            return this;
        }


        public Request build() {
            Request request = new Request();
            request.action = mAction;
            request.command = mCommand;
            request.device = mDevice;
            request.email = mEmail;
            request.groupId = mGroupId;
            request.identifier = mIdentifier;
            request.method = mMethod;
            request.organisationId = mOrganisationId;
            request.password = mPassword;
//            request.playImmediately = mPlayImmediately;
            request.receiverId = mReceiverId;
            request.secret = mSecret;
            request.pin = mPin;
            request.userId = mUserId;
            return request;
        }
    }
}
