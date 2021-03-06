package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Eduard Albu on 12 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class Receiver implements Parcelable {

    private String mName;
    private boolean isSelected;
    private String mCreated;
    private String mModified;
    private int mReceiverId;
    private int mSeqValue;

    public Receiver() {

    }

    protected Receiver(Parcel in) {
        mName = in.readString();
        isSelected = in.readByte() != 0;
        mCreated = in.readString();
        mModified = in.readString();
        mReceiverId = in.readInt();
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

    public int getReceiverId() {
        return mReceiverId;
    }

    public void setReceiverId(int receiverId) {
        mReceiverId = receiverId;
    }

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

    public int getSeqValue() {
        return mSeqValue;
    }

    public void setSeqValue(int seqValue) {
        mSeqValue = seqValue;
    }

    public static ArrayList<Receiver> build(JSONObject response) {
        ArrayList<Receiver> receivers = new ArrayList<>();
        try {
            JSONArray receiversArray = response.optJSONArray("receivers");
            if (receiversArray != null) {
                for (int i = 0; i < receiversArray.length(); i++) {
                    JSONObject o = receiversArray.getJSONObject(i);
                    Receiver receiver = new Receiver();
                    receiver.setReceiverId(o.optInt("receiverID"));
                    receiver.setName(o.optString("name", ""));
                    receiver.setCreated(o.optString("created", ""));
                    receiver.setModified(o.optString("modified", ""));
                    receiver.setSeqValue(response.optInt("seq"));
                    if (!receiver.getName().equals("null")) {
                        receivers.add(receiver);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return receivers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeString(mCreated);
        dest.writeString(mModified);
        dest.writeInt(mReceiverId);
    }
}
