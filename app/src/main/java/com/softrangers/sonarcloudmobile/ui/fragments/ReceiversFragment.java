package com.softrangers.sonarcloudmobile.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.GroupsListAdapter;
import com.softrangers.sonarcloudmobile.adapters.ReceiverListAdapter;
import com.softrangers.sonarcloudmobile.models.Group;
import com.softrangers.sonarcloudmobile.models.PASystem;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.ui.AddGroupActivity;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ConnectionReceiver;
import com.softrangers.sonarcloudmobile.utils.observers.GroupObserver;
import com.softrangers.sonarcloudmobile.utils.ui.BaseFragment;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import org.json.JSONObject;

import java.util.ArrayList;


public class ReceiversFragment extends BaseFragment implements RadioGroup.OnCheckedChangeListener,
        ReceiverListAdapter.OnItemClickListener,
        GroupsListAdapter.OnGroupClickListener, GroupObserver, ConnectionReceiver.OnConnected {

    private static final String PA_LIST_STATE = "pa_list_state";
    private static final String GROUPS_LIST_STATE = "groups_list_state";
    public static final int GROUP_REQUEST_CODE = 1331;


    private AnimatedExpandableListView mListView;
    private MainActivity mActivity;
    //    private SwipeRefreshLayout mRefreshLayout;
    private LinearLayout mReceiversLayout;
    private RelativeLayout mGroupsLayout;
    private ReceiverListAdapter mReceiverListAdapter;
    private GroupsListAdapter mGroupsListAdapter;
    private RecyclerView mGroupsRecyclerView;
    private ArrayList<PASystem> mPASystems;
    private ArrayList<Group> mGroups;
    private OnRecordFragmentListener mFragmentListener;
    private ProgressBar mReceiversLoading;

    public ReceiversFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mFragmentListener = (OnRecordFragmentListener) activity;
        } catch (Exception e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnRecordFragmentListener");
        }

        mActivity = (MainActivity) activity;
        mActivity.addObserver(this);
        IntentFilter intentFilter = new IntentFilter(Api.Command.ORGANISATIONS);
        intentFilter.addAction(Api.Command.RECEIVERS);
        intentFilter.addAction(Api.Command.RECEIVER_GROUPS);
        intentFilter.addAction(Api.EXCEPTION);
        mActivity.registerReceiver(mPAReceiver, intentFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receivers, container, false);
        Log.i(this.getClass().getSimpleName(), "onCreateView()");

        // Obtain a link to the lists from layout
        mListView = (AnimatedExpandableListView) view.findViewById(R.id.receivers_expandableListView);
        mGroupsRecyclerView = (RecyclerView) view.findViewById(R.id.groups_recyclerView);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSimpleItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(mGroupsRecyclerView);
        LinearLayout addGroupButton = (LinearLayout) view.findViewById(R.id.add_group_clickableLayout);
        addGroupButton.setOnClickListener(mAddNewGroup);
        // Obtain a link to the SwipeRefreshLayout which holds the list view
        mGroupsLayout = (RelativeLayout) view.findViewById(R.id.groups_relativeLayout);
        mReceiversLayout = (LinearLayout) view.findViewById(R.id.pa_list_linearLayout);

        // Obtain a link to the top buttons (PAs and Groups) and customize them, setting PAs button
        // as the selected one
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.pa_list_selector);

        // Radio buttons group to handle checked state changes
        radioGroup.setOnCheckedChangeListener(this);

        // Initialize PAs list
        mPASystems = new ArrayList<>();
        mReceiversLoading = (ProgressBar) view.findViewById(R.id.receivers_loadingProgress);
        ConnectionReceiver.getInstance().addOnConnectedListener(this);
        if (savedInstanceState != null) {
            mPASystems = savedInstanceState.getParcelableArrayList(PA_LIST_STATE);
            mGroups = savedInstanceState.getParcelableArrayList(GROUPS_LIST_STATE);
            setUpGroupsListView(mGroups);
            setUpReceiversListView(mPASystems);
        }
        return view;
    }

    private void showLoading() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mReceiversLoading.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideLoading() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mReceiversLoading.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(PA_LIST_STATE, mPASystems);
        outState.putParcelableArrayList(GROUPS_LIST_STATE, mGroups);
    }

    /**
     * Initialize receivers adapter with a given list (use adapter methods to add items or to clear, replace
     * the list within adapter)
     *
     * @param paSystems list to instantiate the adapter for list view (now it's an empty array
     */
    private void setUpReceiversListView(ArrayList<PASystem> paSystems) {
        mReceiverListAdapter = new ReceiverListAdapter(getActivity(), paSystems);
        mReceiverListAdapter.setOnItemClickListener(this);
        mListView.setAdapter(mReceiverListAdapter);
//        mListView.setOnGroupClickListener(this);
    }

    /**
     * Initialize the list of groups
     *
     * @param groups you want to show in the list
     */
    private void setUpGroupsListView(ArrayList<Group> groups) {
        if (groups == null) return;
        mGroupsListAdapter = new GroupsListAdapter(groups);
        mGroupsListAdapter.setOnGroupClickListener(this);
        mGroupsRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        mGroupsRecyclerView.setAdapter(mGroupsListAdapter);
    }

    /**
     * Called on top buttons checked state is changed
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.receivers_button:
                showReceiversLayout();
                break;
            case R.id.groups_button:
                showGroupsLayout(false);
                break;
        }
    }

    /**
     * Hide the PAs layout and show the Groups layout
     *
     * @param always make request to server
     */
    private void showGroupsLayout(boolean always) {
        mReceiversLayout.setVisibility(View.GONE);
        mGroupsLayout.setVisibility(View.VISIBLE);
        mGroupsLayout.bringToFront();
        // Build a request for getting groups
        if (always || mGroups == null) {
            showLoading();
            if (SonarCloudApp.user != null) {
                Request.Builder builder = new Request.Builder()
                        .command(Api.Command.RECEIVER_GROUPS)
                        .userId(SonarCloudApp.user.getId());
                MainActivity.dataSocketService.sendRequest(builder.build().toJSON());
            }
        }
    }

    /**
     * Shows the layout with PAs list and hide Groups list layout
     */
    private void showReceiversLayout() {
        mGroupsLayout.setVisibility(View.GONE);
        mReceiversLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Called when a group is clicked
     *
     * @param group    which was clicked
     * @param position in the list
     */
    @Override
    public void onGroupClicked(Group group, int position) {
        mFragmentListener.onGroupClicked(group);
        clearPASystemSelection();
    }

    /**
     * Clear all selected receivers
     */
    private void clearPASystemSelection() {
        for (PASystem system : mReceiverListAdapter.getList()) {
            for (Receiver receiver : system.getReceivers()) {
                receiver.setIsSelected(false);
                MainActivity.selectedReceivers.clear();
            }
        }
        mReceiverListAdapter.notifyDataSetChanged();
    }

    private void clearGroupsSelection() {
        for (Group group : mGroupsListAdapter.getList()) {
            group.setIsSelected(false);
        }
        mGroupsListAdapter.notifyDataSetChanged();
    }

    /**
     * Called when a group edit button was clicked
     *
     * @param group    for which the button was clicked
     * @param position of the group in the list
     */
    @Override
    public void onEditButtonClicked(Group group, int position) {
        Intent intent = new Intent(mActivity, AddGroupActivity.class);
        intent.setAction(Api.ACTION_EDIT_GROUP);
        intent.putExtra(AddGroupActivity.PA_SYSTEMS_BUNDLE, mPASystems);
        intent.putExtra(AddGroupActivity.GROUP_EDIT_BUNDLE, group);
        mActivity.startActivityForResult(intent, GROUP_REQUEST_CODE);
    }

    /**
     * Called when a group where added or edited
     *
     * @param group either new or edited
     */
    @Override
    public void update(Group group) {
        int position = -1;
        clearPASystemSelection();
        for (int i = 0; i < mGroups.size(); i++) {
            Group g = mGroups.get(i);
            if (g.getGroupID() == group.getGroupID()) {
                mGroups.remove(g);
                position = i;
                break;
            }
        }
        clearGroupsSelection();
        group.setIsSelected(true);
        if (position > -1) {
            mGroups.add(position, group);
        } else {
            mGroups.add(group);
        }
        MainActivity.selectedGroup = group;
        MainActivity.statusChanged = true;
        mGroupsListAdapter.notifyDataSetChanged();

    }


    /**
     * Called when server can't understand the command
     *
     * @param message about server error
     */
    @Override
    public void onCommandFailure(String message) {
        mActivity.dismissLoading();
        hideLoading();
    }

    @Override
    public void onErrorOccurred() {
        mActivity.dismissLoading();
        hideLoading();
    }


    /**
     * Called when a receiver is clicked in the PAs list
     *
     * @param receiver which is clicked
     * @param position of the receiver in the list
     */
    @Override
    public void onChildClick(Receiver receiver, int position) {
        receiver.setIsSelected(!receiver.isSelected());
        mFragmentListener.onReceiverClicked(receiver);
        mReceiverListAdapter.notifyDataSetChanged();

        if (mGroups != null)
            clearGroupsSelection();
    }

    /**
     * Called when Add new group button is called
     */
    private View.OnClickListener mAddNewGroup = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mActivity, AddGroupActivity.class);
            intent.setAction(Api.ACTION_ADD_GROUP);
            intent.putExtra(AddGroupActivity.PA_SYSTEMS_BUNDLE, mPASystems);
            mActivity.startActivityForResult(intent, GROUP_REQUEST_CODE);
        }
    };

    private ItemTouchHelper.SimpleCallback mSimpleItemTouchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
            final int position = viewHolder.getAdapterPosition();
            final Group group = mGroupsListAdapter.deleteItem(position);
            mGroupsListAdapter.notifyItemRemoved(position);
            Snackbar.make(mGroupsRecyclerView,
                    mActivity.getString(R.string.group_deleted), Snackbar.LENGTH_LONG)
                    .setAction(mActivity.getString(R.string.undo), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGroupsListAdapter.addItem(group, position);
                        }
                    }).setActionTextColor(mActivity.getResources()
                    .getColor(R.color.colorAlertAction))
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            switch (event) {
                                case DISMISS_EVENT_TIMEOUT:
                                case DISMISS_EVENT_CONSECUTIVE:
                                case DISMISS_EVENT_MANUAL:
                                    if (group.equals(MainActivity.selectedGroup)) {
                                        MainActivity.selectedGroup = null;
                                        MainActivity.statusChanged = true;
                                    }
                                    deleteGroupFromServer(group);
                                    break;
                            }
                        }
                    }).show();
        }
    };

    private void deleteGroupFromServer(Group group) {
        Request.Builder builder = new Request.Builder();
        builder.command(Api.Command.DELETE_GROUP);
        builder.receiverGroupID(group.getGroupID());
        MainActivity.dataSocketService.sendRequest(builder.build().toJSON());
    }

    public BroadcastReceiver mPAReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
//                if (mActivity.mSelectedFragment == MainActivity.SelectedFragment.RECEIVERS) {
                String action = intent.getAction();
                JSONObject jsonResponse = new JSONObject(intent.getExtras().getString(action));
                JSONObject jsonRequest = new JSONObject(intent.getExtras().getString(Api.REQUEST_MESSAGE));
                boolean success = jsonResponse.optBoolean("success", false);
                if (!success) {
                    String message = jsonResponse.optString("message", mActivity.getString(R.string.unknown_error));
                    onCommandFailure(message);
                    return;
                }
                switch (action) {
                    case Api.Command.ORGANISATIONS:
                        onOrganisationsReceived(jsonResponse, jsonRequest);
                        break;
                    case Api.Command.RECEIVERS:
                        onReceiversReceived(jsonResponse, jsonRequest);
                        break;
                    case Api.Command.RECEIVER_GROUPS:
                        onGroupsReceived(jsonResponse, jsonRequest);
                        break;
                }
//                }
            } catch (Exception e) {
                onErrorOccurred();
            }

        }
    };

    private void onOrganisationsReceived(JSONObject response, JSONObject jsonRequest) {
        // Build a list of organisations
        mPASystems = PASystem.build(response);
        // Start getting receivers for each organisation
        String lastRequest = null;
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
            // Send the request to server
            if (lastRequest == null || !lastRequest.equals(request.toJSON().toString())) {
                lastRequest = request.toJSON().toString();
                MainActivity.dataSocketService.sendRequest(request.toJSON());
            }
        }
        response.remove("seq");
        jsonRequest.remove("seq");
        setUpReceiversListView(mPASystems);
    }

    private void onReceiversReceived(JSONObject response, JSONObject jsonRequest) {
        hideLoading();
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
                        break;
                    }
                }
            }
            response.remove("seq");
            jsonRequest.remove("seq");
            setUpReceiversListView(mPASystems);
            mReceiverListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onGroupsReceived(JSONObject response, JSONObject jsonRequest) {
        hideLoading();
        mGroups = Group.build(response);
        setUpGroupsListView(mGroups);
        response.remove("seq");
        jsonRequest.remove("seq");
        setUpReceiversListView(mPASystems);
    }

    public void getPAListFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // build a request
                Request.Builder requestBuilder = new Request.Builder();
                requestBuilder.command(Api.Command.ORGANISATIONS);
                requestBuilder.userId(SonarCloudApp.getInstance().userId());
                requestBuilder.seq(SonarCloudApp.SEQ_VALUE);
                // send request to server
                if (MainActivity.dataSocketService != null) {
                    showLoading();
                    MainActivity.dataSocketService.sendRequest(requestBuilder.build().toJSON());
                }
            }
        }).start();
    }

    @Override
    public void onInternetConnectionRestored() {
        Snackbar.make(mReceiversLayout, "Internet connection restored",
                Snackbar.LENGTH_SHORT).show();
        MainActivity.dataSocketService.restartConnection();
    }

    @Override
    public void onInternetConnectionLost() {
        Snackbar.make(mReceiversLayout, "Internet connection lost",
                Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onSocketConnected() {
        getPAListFromServer();
    }

    @Override
    public void onConnectionFailed() {
//        Snackbar.make(mReceiversLayout, "Can\'t connect to server.", Snackbar.LENGTH_SHORT).show();
        hideLoading();
        mActivity.dismissLoading();
    }

    @Override
    public void onConnectTimeOut() {

    }

    @Override
    public void onAudioConnectionClosed() {
        mActivity.dismissLoading();
    }

    public interface OnRecordFragmentListener {
        void onReceiverClicked(Receiver receiver);

        void onGroupClicked(Group group);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ConnectionReceiver.getInstance().removeOnResponseListener(this);
        mActivity.unregisterReceiver(mPAReceiver);
    }
}
