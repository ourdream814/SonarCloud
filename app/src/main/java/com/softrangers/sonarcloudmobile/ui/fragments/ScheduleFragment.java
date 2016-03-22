package com.softrangers.sonarcloudmobile.ui.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ScheduleAllRecordingsAdapter;
import com.softrangers.sonarcloudmobile.adapters.ScheduledRecordsAdapter;
import com.softrangers.sonarcloudmobile.models.Group;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.ui.ScheduleActivity;
import com.softrangers.sonarcloudmobile.utils.BaseFragment;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;

import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleFragment extends BaseFragment implements RadioGroup.OnCheckedChangeListener,
        ScheduledRecordsAdapter.OnScheduleClickListener {

    private static RelativeLayout scheduledLayout;
    private static LinearLayout allScheduleLayout;

    private static TextView unselectedText;
    private static TextView noRecordsText;

    public static boolean isScheduleSelected;

    private static ScheduleAllRecordingsAdapter allRecordingsAdapter;
    private static ScheduledRecordsAdapter scheduledRecordsAdapter;
    private MainActivity mActivity;
    private int clickedPosition;

    public ScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        // get a link to fragment activity
        mActivity = (MainActivity) getActivity();
        IntentFilter intentFilter = new IntentFilter(Api.Command.RECORDINGS);
        intentFilter.addAction(Api.Command.SCHEDULES);
        intentFilter.addAction(Api.EXCEPTION);
        intentFilter.addAction(ScheduleActivity.ACTION_EDIT_SCHEDULE);
        intentFilter.addAction(ScheduleActivity.ACTION_ADD_SCHEDULE);
        mActivity.registerReceiver(mBroadcastReceiver, intentFilter);
        // initialize adapters with empty lists
        ArrayList<Recording> recordings = new ArrayList<>();
        ArrayList<Schedule> schedules = new ArrayList<>();
        allRecordingsAdapter = new ScheduleAllRecordingsAdapter(recordings, mActivity);
        scheduledRecordsAdapter = new ScheduledRecordsAdapter(schedules, mActivity);
        // initialize all fragment views
        initializeViews(view);
        return view;
    }

    /**
     * Initialize all views for this fragment
     *
     * @param view root for this fragment
     */
    private void initializeViews(View view) {
        // The buttons used to switch between layouts in this fragment
        RadioGroup topButtonsGroup = (RadioGroup) view.findViewById(R.id.schedule_list_selector);
        topButtonsGroup.setOnCheckedChangeListener(this);

        // Text views with no data texts, used to show and information if there is no data to display
        unselectedText = (TextView) view.findViewById(R.id.schedule_fragment_unselected);
        unselectedText.setTypeface(SonarCloudApp.avenirBook);
        noRecordsText = (TextView) view.findViewById(R.id.schedule_fragment_no_recordsText);
        noRecordsText.setTypeface(SonarCloudApp.avenirBook);

        // Layouts which are holding all lists, used to hide/show the lists on the screen
        scheduledLayout = (RelativeLayout) view.findViewById(R.id.scheduled_list_layout);
        allScheduleLayout = (LinearLayout) view.findViewById(R.id.schedule_all_layout);

        // initialize horizontal days list
        RecyclerView daysHorizontalList = (RecyclerView) view.findViewById(R.id.schedule_horizontal_list);
        daysHorizontalList.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));

        // initialize scheduled recordings list
        RecyclerView scheduledList = (RecyclerView) view.findViewById(R.id.schedule_vertical_list);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSimpleItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(scheduledList);
        scheduledList.setLayoutManager(new LinearLayoutManager(mActivity));
        scheduledList.setAdapter(scheduledRecordsAdapter);
        scheduledRecordsAdapter.setOnScheduleClickListener(this);

        // initialize all recordings list
        RecyclerView allScheduleList = (RecyclerView) view.findViewById(R.id.schedule_all_recyclerView);
        allScheduleList.setLayoutManager(new LinearLayoutManager(mActivity));
        allScheduleList.setAdapter(allRecordingsAdapter);
    }

    /**
     * Called when user press the top buttons
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.all_schedule_list_button:
                // Change the selected status to know which button was selected
                isScheduleSelected = false;
                // check if the list is not empty
                if (allRecordingsAdapter.getItemCount() > 0) {
                    // if is not empty hide the no data texts
                    unselectedText.setVisibility(View.GONE);
                    noRecordsText.setVisibility(View.GONE);
                } else {
                    // else check if there are any receiver selected and if true get all recordings
                    // for selected receiver
                    if (MainActivity.selectedGroup != null) {
                        getAllRecordingsFromServer(MainActivity.selectedGroup);
                    } else if (MainActivity.selectedReceivers.size() > 0) {
                        getAllRecordingsFromServer(MainActivity.selectedReceivers);
                    }
                }
                // hide the scheduled layout and show the all recordings layout
                scheduledLayout.setVisibility(View.GONE);
                allScheduleLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.scheduled_list_button:
                // Change the selected status to know which button was selected
                isScheduleSelected = true;
                // Hide the all recordings layout and show the scheduled layout
                allScheduleLayout.setVisibility(View.GONE);
                scheduledLayout.setVisibility(View.VISIBLE);
                // check if there are any receiver selected
                // if true then get all scheduled recordings for selected receivers
                if (MainActivity.selectedGroup != null) {
                    getAllScheduledRecords(MainActivity.selectedGroup);
                } else if (MainActivity.selectedReceivers.size() > 0) {
                    getAllScheduledRecords(MainActivity.selectedReceivers);
                }
                break;
        }
    }

    /**
     * Clear all adapters lists
     */
    public void clearList() {
        // clear all recordings list
        allRecordingsAdapter.clearList();
        // clear scheduled recordings list
        scheduledRecordsAdapter.clearList();
        // show the unselected receiver text
        unselectedText.setVisibility(View.VISIBLE);
    }

    /**
     * Get all scheduled recordings from server for given receivers
     *
     * @param receivers for which to get recordings
     */
    public void getAllScheduledRecords(ArrayList<Receiver> receivers) {
        // hide the unselected text
        unselectedText.setVisibility(View.GONE);
        // clear current scheduled list
        scheduledRecordsAdapter.clearList();
        // build a request with provided receivers
        Request.Builder builder = new Request.Builder();
        builder.command(Api.Command.SCHEDULES);
        for (Receiver receiver : receivers) {
            builder.receiverId(receiver.getReceiverId());
            // send request to server
            SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
        }
    }

    /**
     * Get all scheduled recordings from server for given receivers group
     *
     * @param group of receivers for which to get recordings
     */
    public void getAllScheduledRecords(Group group) {
        // hide the unselected text
        unselectedText.setVisibility(View.GONE);
        // clear current scheduled list
        scheduledRecordsAdapter.clearList();
        // build a request with provided receivers
        Request.Builder builder = new Request.Builder();
        builder.command(Api.Command.SCHEDULES);
        for (Receiver receiver : group.getReceivers()) {
            builder.receiverId(receiver.getReceiverId());
            // send request to server
            SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
        }
    }

    /**
     * Get all recordings from server for given receivers
     *
     * @param receivers for which to get recordings
     */
    public void getAllRecordingsFromServer(ArrayList<Receiver> receivers) {
        // check which button is selected
        // if scheduled recordings is selected get scheduled recordings from server
        // else get all recordings from server
        if (isScheduleSelected) {
            getAllScheduledRecords(receivers);
        } else {
            // hide the unselected text
            unselectedText.setVisibility(View.GONE);
            // clear current scheduled list
            allRecordingsAdapter.clearList();
            // build a request with provided receivers
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.RECORDINGS);
            for (Receiver receiver : receivers) {
                builder.receiverId(receiver.getReceiverId());
                // send request to server
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            }
        }
    }

    /**
     * Get all recordings from server for given receivers group
     *
     * @param group of receivers for which to get recordings
     */
    public void getAllRecordingsFromServer(Group group) {
        // check which button is selected
        // if scheduled recordings is selected get scheduled recordings from server
        // else get all recordings from server
        if (isScheduleSelected) {
            getAllScheduledRecords(group);
        } else {
            // hide the unselected text
            unselectedText.setVisibility(View.GONE);
            // clear current scheduled list
            allRecordingsAdapter.clearList();
            // build a request with provided receivers
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.RECORDINGS);
            for (Receiver receiver : group.getReceivers()) {
                builder.receiverId(receiver.getReceiverId());
                // send request to server
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            }
        }
    }

    /**
     * Add recordings to the list with all receiver records
     *
     * @param recordings list to add to adapter
     */
    private void addRecordingsToList(ArrayList<Recording> recordings) {
        // add recordings to adapter's list
        allRecordingsAdapter.addItems(recordings);
        // check if there are any item in the list
        // if false then show the no data text and hide the list layout
        // else hide the no data text and show the list layout
        if (allRecordingsAdapter.getItemCount() <= 0) {
            noRecordsText.setVisibility(View.VISIBLE);
            allScheduleLayout.setVisibility(View.GONE);
        } else {
            noRecordsText.setVisibility(View.GONE);
            allScheduleLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Add scheduled recordings to the list with all receiver records
     *
     * @param schedules list to add to adapter
     */
    private void addSchedulesToList(ArrayList<Schedule> schedules) {
        // add schedules to adapter's list
        scheduledRecordsAdapter.addItems(schedules);
        // check if there are any item in the list
        // if false then show the no data text and hide the list layout
        // else hide the no data text and show the list layout
        if (scheduledRecordsAdapter.getItemCount() <= 0) {
            noRecordsText.setVisibility(View.VISIBLE);
            scheduledLayout.setVisibility(View.GONE);
        } else {
            noRecordsText.setVisibility(View.GONE);
            scheduledLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onScheduleClick(Schedule schedule, int position) {
        clickedPosition = position;
        Intent intent = new Intent(mActivity, ScheduleActivity.class);
        intent.setAction(ScheduleActivity.ACTION_EDIT_SCHEDULE);
        intent.putExtra(ScheduleActivity.SCHEDULE_EXTRAS, schedule);
        mActivity.startActivityForResult(intent, 1911);
    }

    @Override
    public void onSchedulePlayClick(Schedule schedule, Recording recording, int position) {

    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                switch (action) {
                    case ScheduleActivity.ACTION_ADD_SCHEDULE: {
                        Schedule schedule = intent.getExtras().getParcelable(ScheduleActivity.ACTION_ADD_SCHEDULE);
                        scheduledRecordsAdapter.addItem(schedule, scheduledRecordsAdapter.getItemCount() - 1);
                        break;
                    }
                    case ScheduleActivity.ACTION_EDIT_SCHEDULE: {
                        Schedule schedule = intent.getExtras().getParcelable(ScheduleActivity.ACTION_EDIT_SCHEDULE);
                        scheduledRecordsAdapter.removeItem(clickedPosition);
                        scheduledRecordsAdapter.addItem(schedule, clickedPosition);
                        break;
                    }
                    default:
                        JSONObject jsonResponse = new JSONObject(intent.getExtras().getString(action));
                        boolean success = jsonResponse.optBoolean("success", false);
                        if (!success) {
                            String message = jsonResponse.optString("message", mActivity.getString(R.string.unknown_error));
                            onCommandFailure(message);
                            return;
                        }
                        switch (action) {
                            case Api.Command.SCHEDULES:
                                onSchedulesReceived(jsonResponse);
                                break;
                            case Api.Command.RECORDINGS:
                                onRecordingsReceived(jsonResponse);
                                break;
                        }
                        break;
                }
            } catch (Exception e) {
                onErrorOccurred();
            }
        }
    };

    private void onSchedulesReceived(JSONObject response) {
        addSchedulesToList(Schedule.build(response));
    }

    private void onRecordingsReceived(JSONObject response) {
        addRecordingsToList(Recording.build(response));
    }

    @Override
    public void onCommandFailure(String message) {
        Snackbar.make(allScheduleLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onErrorOccurred() {
        Snackbar.make(allScheduleLayout,
                mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Swipe to delete implementation
     */
    private ItemTouchHelper.SimpleCallback mSimpleItemTouchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
            // get the swiped position
            final int position = viewHolder.getAdapterPosition();
            // remove the item for the above position from adapter but store it in an object to
            // be able to restore it in case user clicks "Undo" button
            final Schedule schedule = scheduledRecordsAdapter.removeItem(position);
            // Show the Snackbar with information about deleting and "Undo" button
            Snackbar.make(scheduledLayout,
                    mActivity.getString(R.string.schedule_deleted), Snackbar.LENGTH_LONG)
                    // set the undo action for Snackbar button
                    .setAction(mActivity.getString(R.string.undo), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // restore the saved Schedule object
                            scheduledRecordsAdapter.addItem(schedule, position);
                        }
                    }).setActionTextColor(mActivity.getResources()
                    .getColor(R.color.colorAlertAction))
                    // add a callback to know when the Snackbar goes away
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            // check the event status and delete schedule from server if
                            // the Snackbar was not dismissed by "Undo" button click
                            switch (event) {
                                case DISMISS_EVENT_TIMEOUT:
                                    deleteScheduleFromServer(schedule);
                                    break;
                                case DISMISS_EVENT_CONSECUTIVE:
                                    deleteScheduleFromServer(schedule);
                                    break;
                                case DISMISS_EVENT_MANUAL:
                                    deleteScheduleFromServer(schedule);
                                    break;
                            }
                        }
                    }).show();
        }
    };

    /**
     * Send request to server to delete the given Schedule
     *
     * @param schedule to delete from server
     */
    private void deleteScheduleFromServer(Schedule schedule) {
        // build a request object
        Request.Builder builder = new Request.Builder();
        builder.command(Api.Command.DELETE_SCHEDULE);
        builder.scheduleId(schedule.getScheduleID());
        // send request to server
        SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.unregisterReceiver(mBroadcastReceiver);
    }
}
