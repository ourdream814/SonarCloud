package com.softrangers.sonarcloudmobile.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ReceiverListAdapter;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.ReceiverObserver;
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
        OnResponseListener, SwipeRefreshLayout.OnRefreshListener,
        ReceiverListAdapter.OnItemClickListener, ReceiverObserver {

    private AnimatedExpandableListView mListView;
    private RadioButton mReceivers;
    private RadioButton mGroups;
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
        ArrayList<PASystem> PASystems = new ArrayList<>();
        setUpListView(PASystems);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // build a request
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.command(Api.Command.ORGANISATIONS);
        requestBuilder.userId(SonarCloudApp.getInstance().userId());
        ResponseReceiver.getInstance().addOnResponseListener(this);
        // send request to server
        SonarCloudApp.socketService.sendRequest(requestBuilder.build().toJSON());
    }

    private void setUpListView(ArrayList<PASystem> paSystems) {
        mReceiverListAdapter = new ReceiverListAdapter(getActivity(), paSystems);
        mReceiverListAdapter.setOnItemClickListener(this);
        mListView.setAdapter(mReceiverListAdapter);
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

        ResponseReceiver.getInstance().removeOnResponseListener(this);
        ArrayList<PASystem> paSystems = PASystem.build(response);

        mReceiverListAdapter.refreshList(paSystems);

        for (PASystem system : paSystems) {
            system.addObserver(this);
            if (system.getReceivers() == null)
                system.loadReceivers();
        }

        mActivity.dismissLoading();
    }

    @Override
    public void onCommandFailure(String message) {
        mActivity.alertUserAboutError(mActivity.getString(R.string.error), message);
        mActivity.dismissLoading();
    }

    @Override
    public void onError() {
        mActivity.alertUserAboutError(mActivity.getString(R.string.error), mActivity.getString(R.string.unknown_error));
        mActivity.dismissLoading();
    }

    @Override
    public void onRefresh() {
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onChildClick(Receiver receiver, int position) {
        if (receiver.isSelected()) {
            receiver.setIsSelected(false);
            Snackbar.make(mListView, receiver.getName() + " deselected, Id = " + receiver.getReceiverId(), Snackbar.LENGTH_SHORT).show();
        } else {
            receiver.setIsSelected(true);
            Snackbar.make(mListView, receiver.getName() + " selected, Id = " + receiver.getReceiverId(), Snackbar.LENGTH_SHORT).show();
        }

        mReceiverListAdapter.notifyDataSetChanged();
    }

    @Override
    public void update(PASystem paSystem, ArrayList<Receiver> receivers) {
//        mReceiverListAdapter.addItem(paSystem);
        mReceiverListAdapter.notifyDataSetChanged();
    }
}
