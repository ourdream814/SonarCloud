package com.softrangers.sonarcloudmobile.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReceiversFragment extends Fragment implements RadioGroup.OnCheckedChangeListener,
        OnResponseListener, SwipeRefreshLayout.OnRefreshListener,
        ReceiverListAdapter.OnItemClickListener, ReceiverObserver {

    private static final String PA_LIST_STATE = "pa_list_state";

    private AnimatedExpandableListView mListView;
    private RadioButton mReceivers;
    private RadioButton mGroups;
    private MainActivity mActivity;
    private SwipeRefreshLayout mRefreshLayout;
    private ReceiverListAdapter mReceiverListAdapter;
    private ArrayList<PASystem> mPASystems;
    private static boolean sent;

    public ReceiversFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receivers, container, false);
        mActivity = (MainActivity) getActivity();

        // Obtain a link to the list view from layout
        mListView = (AnimatedExpandableListView) view.findViewById(R.id.receivers_expandableListView);
        // Obtain a link to the SwipeRefreshLayout which holds the list view
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.pa_list_swipeRefresh);
        mRefreshLayout.setOnRefreshListener(this);

        // Obtain a link to the top buttons (PAs and Groups) and customize them, setting PAs button
        // as the selected one
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.pa_list_selector);
        mReceivers = (RadioButton) view.findViewById(R.id.receivers_button);
        mReceivers.setChecked(true);
        mReceivers.setTextColor(getResources().getColor(R.color.colorPrimary));

        // Radio buttons group to handle checked state changes
        mGroups = (RadioButton) view.findViewById(R.id.groups_button);
        radioGroup.setOnCheckedChangeListener(this);

        // Initialize PAs list
        mPASystems = new ArrayList<>();
        PASystem.addObserverToList(this);
        setUpListView(mPASystems);

        if (savedInstanceState != null) {
            mPASystems = savedInstanceState.getParcelableArrayList(PA_LIST_STATE);
            setUpListView(mPASystems);
        } else {
            // build a request
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.command(Api.Command.ORGANISATIONS);
            requestBuilder.userId(SonarCloudApp.getInstance().userId());
            requestBuilder.seq(SonarCloudApp.SEQ_VALUE);
            ResponseReceiver.getInstance().addOnResponseListener(this);
            // send request to server
            SonarCloudApp.socketService.sendRequest(requestBuilder.build().toJSON());
            mActivity.showLoading();
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(PA_LIST_STATE, mPASystems);
    }

    /**
     * Initialize adapter with a given list (use adapter methods to add items or to clear, replace
     * the list within adapter)
     * @param paSystems list to instantiate the adapter for list view (now it's an empty array
     */
    private void setUpListView(ArrayList<PASystem> paSystems) {
        mReceiverListAdapter = new ReceiverListAdapter(getActivity(), paSystems);
        mReceiverListAdapter.setOnItemClickListener(this);
        mListView.setAdapter(mReceiverListAdapter);
    }

    /**
     * Called on top buttons checked state is changed
     */
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


    /**
     * Called when the response with the organisations is ready
     * @param response object from server
     */
    @Override
    public void onResponse(JSONObject response) {

        // Remove the listener because we don't need it any more
        ResponseReceiver.getInstance().removeOnResponseListener(this);

        // Build a list of organisations
        mPASystems = PASystem.build(response);

        // Start getting receivers for each organisation
        for (PASystem system : mPASystems) {

            // increment manually the seq value because the response will return later
            // and it will not increment on each response
            SonarCloudApp.SEQ_VALUE += 1;

            // Set the seq value for the current PA system, it is used when the response is ready
            // to set the receivers in the right PA
            system.setSeqValue(SonarCloudApp.SEQ_VALUE);

            // Build a request for current PA system
            Request request = new Request.Builder().command(Api.Command.RECEIVERS)
                    .organisationId(system.getOrganisationId()).seq(system.getSeqValue()).build();

            // Add a new response listener to handle the response
            ResponseReceiver.getInstance().addOnResponseListener(
                    new ReceiversGet());

            // Send the request to server
            SonarCloudApp.socketService.sendRequest(request.toJSON());
        }
    }


    class ReceiversGet implements OnResponseListener {

        @Override
        public void onResponse(JSONObject response) {
            try {
                // Parse the response and build an receivers array list
                ArrayList<Receiver> receivers = Receiver.build(response);

                // For each receiver
                for (Receiver receiver : receivers) {
                    // Take a PA system
                    for (PASystem sys : mPASystems) {
                        // check if the seq of the receiver and PA are identical
                        if (receiver.getSeqValue() == sys.getSeqValue()) {
                            // set receivers for current system if true
                            sys.setReceivers(receivers);
                            // hide loading dialog
                            mActivity.dismissLoading();
                            // exit the loop because we already have all receivers for current PA
                            return;
                        }
                    }
                }
                // hide loading dialog
                mActivity.dismissLoading();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCommandFailure(String message) {
            mActivity.alertUserAboutError(mActivity.getString(R.string.error), message);
            mActivity.dismissLoading();
        }

        @Override
        public void onError() {
            Snackbar.make(mRefreshLayout, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
            mActivity.dismissLoading();
        }
    }

    @Override
    public void onCommandFailure(String message) {
        mActivity.alertUserAboutError(mActivity.getString(R.string.error), message);
        mActivity.dismissLoading();
    }

    @Override
    public void onError() {
        Snackbar.make(mRefreshLayout, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
        mActivity.dismissLoading();
    }

    @Override
    public void onRefresh() {
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onChildClick(Receiver receiver, int position) {
        // TODO: 3/16/16 Replace with a correct action
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
        mReceiverListAdapter.refreshList(mPASystems);
    }
}
