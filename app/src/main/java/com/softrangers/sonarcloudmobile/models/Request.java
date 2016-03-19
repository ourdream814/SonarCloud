package com.softrangers.sonarcloudmobile.models;

import com.softrangers.sonarcloudmobile.utils.api.Api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Eduard Albu on 14 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class Request {
    private String command;
    private String device;
    private String method;
    private String name;
    private String email;
    private String password;
    private String identifier;
    private String secret;
    private String groupID;
    private int pin = -1;
    private String action;
    private String userID;
    private int organizationID;
    private String receiverID;
    private int seq;
    private JSONArray receivers;

    private Request() {

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

            for (Map.Entry<String, Field> set : fieldHashMap.entrySet()) {
                if (set.getValue().get(this) != null &&
                        !set.getValue().getName().equalsIgnoreCase("creator")) {
                    request.put(set.getKey(), set.getValue().get(this));
                }
            }
            if (pin == -1) request.remove("pin");
            if (request.getInt("organizationID") <= 0) request.remove("organizationID");
            request.put(Api.Options.SEQ_FIELD, seq);
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
        private String mName;
        private String mEmail;
        private String mPassword;
        private String mIdentifier;
        private String mSecret;
        private String mGroupId;
        private int mPin = -1;
        private String mAction;
        private String mUserId;
        private int mOrganisationId;
        private String mReceiverId;
        private JSONArray mReceivers;
        private boolean mPlayImmediately;
        private int mSeq;

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

        public Builder name(String name) {
            mName = name;
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

        public Builder organisationId(int organistaionId) {
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

        public Builder seq(int seq) {
            mSeq = seq;
            return this;
        }

        public Builder receivers(ArrayList<Integer> receivers) {
            try {
                mReceivers = new JSONArray();
                for (Integer i : receivers) {
                    mReceivers.put(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return this;
        }


        public Request build() {
            Request request = new Request();
            request.action = mAction;
            request.command = mCommand;
            request.device = mDevice;
            request.email = mEmail;
            request.name = mName;
            request.groupID = mGroupId;
            request.identifier = mIdentifier;
            request.method = mMethod;
            request.organizationID = mOrganisationId;
            request.password = mPassword;
            request.receiverID = mReceiverId;
            request.secret = mSecret;
            request.pin = mPin;
            request.userID = mUserId;
            request.seq = mSeq;
            request.receivers = mReceivers;
            return request;
        }
    }
}
