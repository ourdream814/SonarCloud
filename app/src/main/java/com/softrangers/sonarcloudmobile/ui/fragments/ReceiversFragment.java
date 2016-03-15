package com.softrangers.sonarcloudmobile.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ReceiverListAdapter;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReceiversFragment extends Fragment implements RadioGroup.OnCheckedChangeListener, OnResponseListener {

    private static final String RECEIVERS_ARG = "receivers_args";
    private AnimatedExpandableListView mListView;
    private RadioButton mReceivers;
    private RadioButton mGroups;
    private ArrayList<Receiver> mReceiversArray;

    public ReceiversFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receivers, container, false);
        mListView = (AnimatedExpandableListView) view.findViewById(R.id.receivers_expandableListView);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.pa_list_selector);
        mReceivers = (RadioButton) view.findViewById(R.id.receivers_button);
        mReceivers.setChecked(true);
        mReceivers.setTextColor(getResources().getColor(R.color.colorPrimary));
        mGroups = (RadioButton) view.findViewById(R.id.groups_button);
        radioGroup.setOnCheckedChangeListener(this);
        ResponseReceiver.getInstance().addOnResponseListener(this);
        return view;
    }

    private void setUpListView(ArrayList<PASystem> paSystems) {
        ReceiverListAdapter adapter = new ReceiverListAdapter(getActivity(), paSystems);
        mListView.setAdapter(adapter);
    }

    private ArrayList<PASystem> getTestPASystems() {
        ArrayList<PASystem> paSystems = new ArrayList<>();
        for (int i = 1; i < mReceiversArray.size(); i++) {
            PASystem paSystem = new PASystem();
            paSystem.setName("PA System " + i);
            paSystem.setReceivers(mReceiversArray);
            paSystems.add(paSystem);
        }
        return paSystems;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.receivers_button:
                mReceivers.setTextColor(getResources().getColor(R.color.colorPrimary));
                mGroups.setTextColor(getResources().getColor(android.R.color.white));
                break;
            case R.id.groups_button:
                mReceivers.setTextColor(getResources().getColor(android.R.color.white));
                mGroups.setTextColor(getResources().getColor(R.color.colorPrimary));
                break;
        }
    }

    @Override
    public void onResponse(JSONObject response) {
        mReceiversArray = Receiver.build(response);
        setUpListView(getTestPASystems());
    }

    @Override
    public void onCommandFailure(String message) {

    }

    @Override
    public void onError() {

    }
}
