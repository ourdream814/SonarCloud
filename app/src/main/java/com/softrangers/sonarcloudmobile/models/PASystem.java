package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.ReceiverObserver;
import com.softrangers.sonarcloudmobile.utils.ReceiversObservable;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Eduard Albu on 12 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class PASystem implements Parcelable, ReceiversObservable {

    private String mName;
    private String mCreated;
    private String mModified;
    private int mOrganisationId;
    private ArrayList<Receiver> mReceivers;
    private static ArrayList<ReceiverObserver> observers;

    public PASystem() {
        observers = new ArrayList<>();
    }

    protected PASystem(Parcel in) {
        mName = in.readString();
        mCreated = in.readString();
        mModified = in.readString();
        mOrganisationId = in.readInt();
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
        notifyObservers();
    }

    public int getOrganisationId() {
        return mOrganisationId;
    }

    public void setOrganisationId(int organisationId) {
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
        final ArrayList<PASystem> systems = new ArrayList<>();
        try {
            JSONArray array = response.getJSONArray("organizations");
            for (int i = 0; i < array.length(); i++) {
                PASystem system = new PASystem();
                JSONObject o = array.getJSONObject(i);
                system.setCreated(o.getString("created"));
                system.setModified(o.getString("modified"));
                system.setOrganisationId(o.getInt("organizationID"));
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
        dest.writeString(mCreated);
        dest.writeString(mModified);
        dest.writeInt(mOrganisationId);
        dest.writeTypedList(mReceivers);
    }

    public void loadReceivers() {
        Request request = new Request.Builder().command(Api.Command.RECEIVERS).organisationId(this.getOrganisationId()).build();
        SonarCloudApp.socketService.sendRequest(request.toJSON());
        ResponseReceiver.getInstance().addOnResponseListener(new OnResponseListener() {
            @Override
            public void onResponse(JSONObject response) {
                PASystem.this.setReceivers(Receiver.build(response));
            }

            @Override
            public void onCommandFailure(String message) {

            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void addObserver(ReceiverObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(ReceiverObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (ReceiverObserver o : observers) {
            if (o != null) {
                o.update(this, this.getReceivers());
            }
        }
    }
}
