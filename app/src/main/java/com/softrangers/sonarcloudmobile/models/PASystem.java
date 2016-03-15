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
public class PASystem implements Parcelable {

    private String mName;
    private String mCreated;
    private String mModified;
    private String mOrganisationId;
    private ArrayList<Receiver> mReceivers;

    public PASystem() {

    }

    protected PASystem(Parcel in) {
        mName = in.readString();
        mReceivers = in.createTypedArrayList(Receiver.CREATOR);
    }

    public static final Creator<PASystem> CREATOR = new Creator<PASystem>() {
        @Override
        public PASystem createFromParcel(Parcel in) {
            return new PASystem(in);
        }

        @Override
        public PASystem[] newArray(int size) {
            return new PASystem[size];
        }
    };

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

    public String getOrganisationId() {
        return mOrganisationId;
    }

    public void setOrganisationId(String organisationId) {
        mOrganisationId = organisationId;
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

    public static ArrayList<PASystem> build(JSONObject response) {
        ArrayList<PASystem> systems = new ArrayList<>();
        try {
            JSONArray array = response.getJSONArray("organizations");
            for (int i = 0; i < array.length(); i++) {
                PASystem system = new PASystem();
                JSONObject o = array.getJSONObject(i);
                system.setCreated(o.getString("created"));
                system.setModified(o.getString("modified"));
                system.setOrganisationId("organizationID");
                system.setName(o.getString("name"));
                systems.add(system);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return systems;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeTypedList(mReceivers);
    }
}
