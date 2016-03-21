package com.softrangers.sonarcloudmobile.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
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
import com.softrangers.sonarcloudmobile.utils.GroupObserver;
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.ReceiverObserver;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import org.json.JSONObject;

import java.util.ArrayList;


public class ReceiversFragment extends Fragment implements RadioGroup.OnCheckedChangeListener,
        OnResponseListener, SwipeRefreshLayout.OnRefreshListener,
        ReceiverListAdapter.OnItemClickListener, ReceiverObserver,
        GroupsListAdapter.OnGroupClickListener, GroupObserver,
        ExpandableListView.OnGroupClickListener {

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

    public ReceiversFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receivers, container, false);
        mActivity = (MainActivity) getActivity();
        mActivity.addObserver(this);

        // Obtain a link to the lists from layout
        mListView = (AnimatedExpandableListView) view.findViewById(R.id.receivers_expandableListView);
        mGroupsRecyclerView = (RecyclerView) view.findViewById(R.id.groups_recyclerView);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSimpleItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(mGroupsRecyclerView);
        LinearLayout addGroupButton = (LinearLayout) view.findViewById(R.id.add_group_clickableLayout);
        addGroupButton.setOnClickListener(mAddNewGroup);
        // Obtain a link to the SwipeRefreshLayout which holds the list view
        mGroupsLayout = (RelativeLayout) view.findViewById(R.id.groups_relativeLayout);
//        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.receivers_swipe_refreshLayout);
        mReceiversLayout = (LinearLayout) view.findViewById(R.id.pa_list_linearLayout);
//        mRefreshLayout.setOnRefreshListener(this);

        // Obtain a link to the top buttons (PAs and Groups) and customize them, setting PAs button
        // as the selected one
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.pa_list_selector);

        // Radio buttons group to handle checked state changes
        radioGroup.setOnCheckedChangeListener(this);

        // Initialize PAs list
        mPASystems = new ArrayList<>();
        PASystem.addObserverToList(this);
        setUpReceiversListView(mPASystems);

        if (savedInstanceState != null) {
            mPASystems = savedInstanceState.getParcelableArrayList(PA_LIST_STATE);
            mGroups = savedInstanceState.getParcelableArrayList(GROUPS_LIST_STATE);
            setUpGroupsListView(mGroups);
            setUpReceiversListView(mPASystems);
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
        mListView.setOnGroupClickListener(this);
    }

    /**
     * Initialize the list of groups
     *
     * @param groups you want to show in the list
     */
    private void setUpGroupsListView(ArrayList<Group> groups) {
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
            Request.Builder builder = new Request.Builder()
                    .command(Api.Command.RECEIVER_GROUPS)
                    .userId(SonarCloudApp.user.getId());
            ResponseReceiver.getInstance().clearResponseListenersList();
            ResponseReceiver.getInstance().addOnResponseListener(new GroupsGet());
            SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            mActivity.showLoading();
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
     * Called when the response with the organisations is ready
     *
     * @param response object from server
     */
    @Override
    public void onResponse(JSONObject response) {

        // Remove the listener because we don't need it any more
        ResponseReceiver.getInstance().clearResponseListenersList();

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

    /**
     * Called when a group is clicked
     *
     * @param group    which was clicked
     * @param position in the list
     */
    @Override
    public void onGroupClicked(Group group, int position) {
        Log.d(this.getClass().getSimpleName(), String.valueOf(group.isSelected()));
        if (!group.isSelected()) {
            MainActivity.selectedGroup = null;
        } else {
            MainActivity.selectedGroup = group;
        }
        MainActivity.statusChanged = true;
        if (MainActivity.selectedReceivers.size() > 0)
            mReceiverListAdapter.refreshList(clearPASystemSelection(mPASystems));
    }

    /**
     * Clear all selected receivers
     *
     * @param systems list with pa systems for which to clear the selection
     * @return a list with unselected receivers
     */
    private ArrayList<PASystem> clearPASystemSelection(ArrayList<PASystem> systems) {
        for (PASystem system : systems) {
            for (Receiver receiver : system.getReceivers()) {
                receiver.setIsSelected(false);
                MainActivity.selectedReceivers.clear();
            }
        }
        return systems;
    }

    private ArrayList<Group> clearGroupsSelection(ArrayList<Group> groups) {
        for (Group group : groups) {
            group.setIsSelected(false);
        }
        return groups;
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
        intent.putExtra(AddGroupActivity.PA_SUSTEMS_BUNDLE, mPASystems);
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
        for (int i = 0; i < mGroups.size(); i++) {
            Group g = mGroups.get(i);
            if (g.getGroupID() == group.getGroupID()) {
                mGroups.remove(g);
                position = i;
                break;
            }
        }
        clearGroupsSelection(mGroups);
        group.setIsSelected(true);
        if (position > -1) {
            mGroups.add(position, group);
        } else {
            mGroups.add(group);
        }
        MainActivity.selectedGroup = group;
        mGroupsListAdapter.notifyDataSetChanged();
    }

    /**
     * Response listener for receivers
     */
    class ReceiversGet implements OnResponseListener {

        @Override
        public void onResponse(JSONObject response) {
            mActivity.dismissLoading();
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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCommandFailure(String message) {
            mActivity.dismissLoading();
            ResponseReceiver.getInstance().clearResponseListenersList();
            mActivity.alertUserAboutError(mActivity.getString(R.string.error), message);
        }

        @Override
        public void onError() {
            mActivity.dismissLoading();
            ResponseReceiver.getInstance().clearResponseListenersList();
            Snackbar.make(mReceiversLayout, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();

        }
    }

    /**
     * Response listener for groups
     */
    class GroupsGet implements OnResponseListener {

        @Override
        public void onResponse(JSONObject response) {
            mActivity.dismissLoading();
            mGroups = Group.build(response);
            ResponseReceiver.getInstance().clearResponseListenersList();
            setUpGroupsListView(mGroups);

        }

        @Override
        public void onCommandFailure(String message) {
            mActivity.dismissLoading();
            ResponseReceiver.getInstance().clearResponseListenersList();
            Snackbar.make(mGroupsLayout, message, Snackbar.LENGTH_SHORT).show();
        }

        @Override
        public void onError() {
            mActivity.dismissLoading();
            ResponseReceiver.getInstance().clearResponseListenersList();
            Snackbar.make(mGroupsLayout, mActivity.getString(R.string.unknown_error),
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when server can't understand the command
     *
     * @param message about server error
     */
    @Override
    public void onCommandFailure(String message) {
        mActivity.dismissLoading();
        mActivity.alertUserAboutError(mActivity.getString(R.string.error), message);
    }

    /**
     * Called when any error such parsing the response, connection error and so on
     */
    @Override
    public void onError() {
        mActivity.dismissLoading();
        Snackbar.make(mReceiversLayout, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Called when user swipe down to refresh the list
     */
    @Override
    public void onRefresh() {
//        mRefreshLayout.setRefreshing(false);
    }

    /**
     * Called when a receiver is clicked in the PAs list
     *
     * @param receiver which is clicked
     * @param position of the receiver in the list
     */
    @Override
    public void onChildClick(Receiver receiver, int position) {
        if (mGroups != null)
            setUpGroupsListView(clearGroupsSelection(mGroups));
        MainActivity.selectedGroup = null;

        if (receiver.isSelected()) {
            receiver.setIsSelected(false);
            MainActivity.selectedReceivers.remove(receiver);
        } else {
            receiver.setIsSelected(true);
            MainActivity.selectedReceivers.add(receiver);
        }
        MainActivity.statusChanged = true;
        mReceiverListAdapter.notifyDataSetChanged();
    }

    /**
     * Called when a list for one PA is ready
     *
     * @param paSystem  which has the receivers list ready
     * @param receivers list for the PA system
     */
    @Override
    public void update(PASystem paSystem, ArrayList<Receiver> receivers) {
        mReceiverListAdapter.refreshList(mPASystems);
    }

    /**
     * Called when Add new group button is called
     */
    private View.OnClickListener mAddNewGroup = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mActivity, AddGroupActivity.class);
            intent.setAction(Api.ACTION_ADD_GROUP);
            intent.putExtra(AddGroupActivity.PA_SUSTEMS_BUNDLE, mPASystems);
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
                                    if (group.equals(MainActivity.selectedGroup)) {
                                        MainActivity.selectedGroup = null;
                                        MainActivity.statusChanged = true;
                                    }
                                    deleteGroupFromServer(group);
                                    break;
                                case DISMISS_EVENT_CONSECUTIVE:
                                    if (group.equals(MainActivity.selectedGroup)) {
                                        MainActivity.selectedGroup = null;
                                        MainActivity.statusChanged = true;
                                    }
                                    deleteGroupFromServer(group);
                                    break;
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
        ResponseReceiver.getInstance().clearResponseListenersList();
        SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        // We call collapseGroupWithAnimation(int) and
        // expandGroupWithAnimation(int) to animate group
        // expansion/collapse.
        if (mListView.isGroupExpanded(groupPosition)) {
            mListView.collapseGroupWithAnimation(groupPosition);
        } else {
            mListView.expandGroupWithAnimation(groupPosition);
        }
        return true;
    }
}
