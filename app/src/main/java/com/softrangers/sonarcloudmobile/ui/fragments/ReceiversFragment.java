package com.softrangers.sonarcloudmobile.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ReceiverListAdapter;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReceiversFragment extends Fragment implements RadioGroup.OnCheckedChangeListener,
        OnResponseListener, SwipeRefreshLayout.OnRefreshListener, ReceiverListAdapter.OnItemClickListener {

    private static final String RECEIVERS_ARG = "receivers_args";
    private AnimatedExpandableListView mListView;
    private RadioButton mReceivers;
    private RadioButton mGroups;
    private ArrayList<Receiver> mReceiversArray;
    private ArrayList<PASystem> mPASystems;
    private MainActivity mActivity;
    private SwipeRefreshLayout mRefreshLayout;
    private ReceiverListAdapter mReceiverListAdapter;

    public ReceiversFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receivers, container, false);
        mActivity = (MainActivity) getActivity();
        mListView = (AnimatedExpandableListView) view.findViewById(R.id.receivers_expandableListView);
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.pa_list_swipeRefresh);
        mRefreshLayout.setOnRefreshListener(this);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.pa_list_selector);
        mReceivers = (RadioButton) view.findViewById(R.id.receivers_button);
        mReceivers.setChecked(true);
        mReceivers.setTextColor(getResources().getColor(R.color.colorPrimary));
        mGroups = (RadioButton) view.findViewById(R.id.groups_button);
        radioGroup.setOnCheckedChangeListener(this);
        ResponseReceiver.getInstance().addOnResponseListener(this);

        // build a request
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.command(Api.Command.ORGANISATIONS);
        requestBuilder.userId(SonarCloudApp.getInstance().userId());

        // send request to server
        SonarCloudApp.socketService.sendRequest(requestBuilder.build().toJSON());
        return view;
    }

    private void setUpListView(ArrayList<PASystem> paSystems) {
        mReceiverListAdapter = new ReceiverListAdapter(getActivity(), paSystems);
        mReceiverListAdapter.setOnItemClickListener(this);
        mListView.setAdapter(mReceiverListAdapter);
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
        mPASystems = PASystem.build(response);
        ResponseReceiver.getInstance().removeOnResponseListener(this);

        for (final PASystem system : mPASystems) {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.command(Api.Command.RECEIVERS);
            requestBuilder.userId(SonarCloudApp.getInstance().userId());
            requestBuilder.organisationId(system.getOrganisationId());

            // send request to server
            SonarCloudApp.socketService.sendRequest(requestBuilder.build().toJSON());
            ResponseReceiver.getInstance().addOnResponseListener(new OnResponseListener() {
                @Override
                public void onResponse(JSONObject response) {
                    system.setReceivers(Receiver.build(response));
                }

                @Override
                public void onCommandFailure(String message) {

                }

                @Override
                public void onError() {

                }
            });
        }
        setUpListView(mPASystems);
        mActivity.dismissLoading();
    }

    @Override
    public void onCommandFailure(String message) {

    }

    @Override
    public void onError() {

    }

    @Override
    public void onRefresh() {
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onChildClick(Receiver receiver, int position) {
        if (receiver.isSelected()) receiver.setIsSelected(false);
        else receiver.setIsSelected(true);
        mReceiverListAdapter.notifyDataSetChanged();
    }
}
