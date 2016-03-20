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
    private static final int UNSPECIFIED_INT = -1;

    private String command;
    private String device;
    private String method;
    private String name;
    private String email;
    private String password;
    private String identifier;
    private String secret;
    private String groupID;
    private int receiverGroupID = UNSPECIFIED_INT;
    private int loginID = UNSPECIFIED_INT;
    private int pin = UNSPECIFIED_INT;
    private String action;
    private String userID;
    private int organizationID = UNSPECIFIED_INT;
    private int receiverID = UNSPECIFIED_INT;
    private int seq;
    private int scheduleID = UNSPECIFIED_INT;
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
            if (pin == UNSPECIFIED_INT) request.remove("pin");
            if (receiverGroupID == UNSPECIFIED_INT) request.remove("receiverGroupID");
            if (loginID == UNSPECIFIED_INT) request.remove("loginID");
            if (organizationID == UNSPECIFIED_INT) request.remove("organizationID");
            if (receiverID == UNSPECIFIED_INT) request.remove("receiverID");
            if (scheduleID == UNSPECIFIED_INT) request.remove("scheduleID");
            request.remove("UNSPECIFIED_INT");

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
        private int mPin = UNSPECIFIED_INT;
        private String mAction;
        private String mUserId;
        private int mOrganisationId;
        private int mReceiverId = UNSPECIFIED_INT;
        private JSONArray mReceivers;
        private boolean mPlayImmediately;
        private int mSeq;
        private int mReceiverGroupId = UNSPECIFIED_INT;
        private int mScheduleId = UNSPECIFIED_INT;

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

        public Builder receiverId(int receiverId) {
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

        public Builder receiverGroupID(int receiverGroupId) {
            if (receiverGroupId == 0) receiverGroupId = UNSPECIFIED_INT;
            mReceiverGroupId = receiverGroupId;
            return this;
        }

        public Builder scheduleId(int scheduleId) {
            mScheduleId = scheduleId;
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

            if (mOrganisationId != 0)
                request.organizationID = mOrganisationId;

            request.password = mPassword;
            request.receiverID = mReceiverId;
            request.secret = mSecret;
            request.pin = mPin;
            request.userID = mUserId;
            request.receiverGroupID = mReceiverGroupId;

            request.scheduleID = mScheduleId;

            if (mSeq != 0)
                request.seq = mSeq;

            request.receivers = mReceivers;
            return request;
        }
    }
}
