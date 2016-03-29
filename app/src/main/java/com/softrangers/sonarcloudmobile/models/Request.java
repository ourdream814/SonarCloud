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
    private String time;
    private String minute;
    private String hour;
    private String day;
    private String month;
    private String wday;
    private String endDate;
    private String startDate;
    private String format;
    private int bitrate = UNSPECIFIED_INT;
    private int receiverGroupID = UNSPECIFIED_INT;
    private int loginID = UNSPECIFIED_INT;
    private int pin = UNSPECIFIED_INT;
    private int samplerate = UNSPECIFIED_INT;
    private String action;
    private String userID;
    private String key;
    private int deleteAfter;
    private int organizationID = UNSPECIFIED_INT;
    private int receiverID = UNSPECIFIED_INT;
    private int seq;
    private int scheduleID = UNSPECIFIED_INT;
    private int channels = UNSPECIFIED_INT;
    private JSONArray receivers;
    private JSONArray receiversID;

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
            if (bitrate == UNSPECIFIED_INT) request.remove("bitrate");
            if (samplerate == UNSPECIFIED_INT) request.remove("samplerate");
            if (channels == UNSPECIFIED_INT) request.remove("channels");
            if (!command.equalsIgnoreCase(Api.Command.UPDATE_SCHEDULE)) request.remove("deleteAfter");
            request.remove("UNSPECIFIED_INT");
            request.remove("receiversID");
            if (receiversID != null) {
                request.put("receiverID", receiversID);
            }

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
        private int mSeq;
        private int mReceiverGroupId = UNSPECIFIED_INT;
        private int mScheduleId = UNSPECIFIED_INT;
        private int mSampleRate = UNSPECIFIED_INT;
        private int mChannels = UNSPECIFIED_INT;
        private String time;
        private String minute;
        private String hour;
        private String day;
        private String month;
        private String wday;
        private String endDate;
        private String startDate;
        private String mFormat;
        private String mKey;
        private int deleteAfter;
        private int mBitrate = UNSPECIFIED_INT;
        private JSONArray mReceiversID;


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

        public Builder seq(int seq) {
            mSeq = seq;
            return this;
        }

        public Builder receivers(ArrayList<Integer> receivers) {
            try {
                mReceivers = new JSONArray();
                for (int i : receivers) {
                    mReceivers.put(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return this;
        }

        public Builder receiversID(ArrayList<Integer> receivers) {
            try {
                mReceiversID = new JSONArray();
                for (int i : receivers) {
                    mReceiversID.put(i);
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

        public Builder time(String time) {
            this.time = time;
            return this;
        }

        public Builder minute(String minute) {
            this.minute = minute;
            return this;
        }

        public Builder hour(String hour) {
            this.hour = hour;
            return this;
        }

        public Builder day(String day) {
            this.day = day;
            return this;
        }

        public Builder month(String month) {
            this.month = month;
            return this;
        }

        public Builder wday(String wday) {
            this.wday = wday;
            return this;
        }

        public Builder deleteAfter(int deleteAfter) {
            this.deleteAfter = deleteAfter;
            return this;
        }

        public Builder startDate(String startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(String endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder bitrate(int bitrate) {
            mBitrate = bitrate;
            return this;
        }

        public Builder samplerate(int sampleRate) {
            mSampleRate = sampleRate;
            return this;
        }

        public Builder channels(int channels) {
            mChannels = channels;
            return this;
        }

        public Builder format(String format) {
            mFormat = format;
            return this;
        }

        public Builder key(String sendAudioKey) {
            mKey = sendAudioKey;
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
            request.time = time;
            request.minute = minute;
            request.hour = hour;
            request.day = day;
            request.month = month;
            request.wday = wday;
            request.deleteAfter = deleteAfter;
            request.startDate = startDate;
            request.endDate = endDate;
            request.scheduleID = mScheduleId;
            request.bitrate = mBitrate;
            request.samplerate = mSampleRate;
            if (mSeq != 0)
                request.seq = mSeq;
            request.channels = mChannels;
            request.format = mFormat;
            request.receivers = mReceivers;
            request.receiversID = mReceiversID;
            request.key = mKey;
            return request;
        }
    }
}
