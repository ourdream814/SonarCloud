package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Eduard Albu on 19 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class Group implements Parcelable {
    private int groupID;
    private String mCreated;
    private String mModified;
    private String mLoginID;
    private String mName;
    private ArrayList<Receiver> mReceivers;
    private boolean isSelected;
    private int mPin;

    public Group() {
    }

    protected Group(Parcel in) {
        groupID = in.readInt();
        mCreated = in.readString();
        mModified = in.readString();
        mLoginID = in.readString();
        mName = in.readString();
        mReceivers = in.createTypedArrayList(Receiver.CREATOR);
        isSelected = in.readByte() != 0;
        mPin = in.readInt();
    }

    public static final Creator<Group> CREATOR = new Creator<Group>() {
        @Override
        public Group createFromParcel(Parcel in) {
            return new Group(in);
        }

        @Override
        public Group[] newArray(int size) {
            return new Group[size];
        }
    };

    public int getGroupID() {
        return groupID;
    }

    public void setGroupID(int groupID) {
        this.groupID = groupID;
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

    public String getLoginID() {
        return mLoginID;
    }

    public void setLoginID(String loginID) {
        mLoginID = loginID;
    }

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

    public boolean isSelected() {
        return isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public int getPin() {
        return mPin;
    }

    public void setPin(int pin) {
        mPin = pin;
    }

    public static ArrayList<Group> build(JSONObject response) {
        ArrayList<Group> groups = new ArrayList<>();
        try {
            JSONArray receiverGroups = response.getJSONArray("receiverGroups");
            for (int i = 0; i < receiverGroups.length(); i++) {
                Group group = new Group();
                JSONObject object = receiverGroups.getJSONObject(i);
                group.setGroupID(object.getInt("receiverGroupID"));
                group.setCreated(object.getString("created"));
                group.setModified(object.getString("modified"));
                group.setLoginID(object.getString("loginID"));
                group.setName(object.getString("name"));
                group.setReceivers(Receiver.build(object));
                groups.add(group);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }

    public static Group buildSingle(JSONObject response) {
        Group group = new Group();
        try {
            JSONObject object = response.getJSONObject("receiverGroup");
            group.setGroupID(object.getInt("receiverGroupID"));
            group.setCreated(object.getString("created"));
            group.setModified(object.getString("modified"));
            group.setLoginID(object.getString("loginID"));
            group.setName(object.getString("name"));
            group.setReceivers(Receiver.build(object));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return group;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Group) {
            Group group = (Group) o;
            if (this.getGroupID() == group.getGroupID()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(groupID);
        dest.writeString(mCreated);
        dest.writeString(mModified);
        dest.writeString(mLoginID);
        dest.writeString(mName);
        dest.writeTypedList(mReceivers);
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeInt(mPin);
    }
}
