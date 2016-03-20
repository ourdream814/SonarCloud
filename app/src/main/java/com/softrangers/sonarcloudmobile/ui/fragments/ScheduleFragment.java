package com.softrangers.sonarcloudmobile.ui.fragments;


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
import com.softrangers.sonarcloudmobile.utils.OnResponseListener;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.ResponseReceiver;

import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {

    private static RelativeLayout scheduledLayout;
    private static LinearLayout allScheduleLayout;

    private RecyclerView mDaysHorizontalList;
    private RecyclerView mScheduledList;
    private RecyclerView mAllScheduleList;
    private static TextView unselectedText;
    private static TextView noRecordsText;

    public static boolean isScheduleSelected;
    public static boolean gotScheduledRecords;

    private static ScheduleAllRecordingsAdapter allRecordingsAdapter;
    private static ScheduledRecordsAdapter scheduledRecordsAdapter;
    private MainActivity mActivity;

    private static ArrayList<Recording> recordings;
    private static ArrayList<Schedule> schedules;

    private Group mGroup;
    private ArrayList<Receiver> mReceivers;

    public ScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        mActivity = (MainActivity) getActivity();
        recordings = new ArrayList<>();
        schedules = new ArrayList<>();
        allRecordingsAdapter = new ScheduleAllRecordingsAdapter(recordings, mActivity);
        scheduledRecordsAdapter = new ScheduledRecordsAdapter(schedules, mActivity);
        initializeViews(view);
        return view;
    }

    private void initializeViews(View view) {
        RadioGroup topButtonsGroup = (RadioGroup) view.findViewById(R.id.schedule_list_selector);
        topButtonsGroup.setOnCheckedChangeListener(this);

        unselectedText = (TextView) view.findViewById(R.id.schedule_fragment_unselected);
        unselectedText.setTypeface(SonarCloudApp.avenirBook);
        noRecordsText = (TextView) view.findViewById(R.id.schedule_fragment_no_recordsText);
        noRecordsText.setTypeface(SonarCloudApp.avenirBook);

        scheduledLayout = (RelativeLayout) view.findViewById(R.id.scheduled_list_layout);
        allScheduleLayout = (LinearLayout) view.findViewById(R.id.schedule_all_layout);

        // initialize horizontal days list
        mDaysHorizontalList = (RecyclerView) view.findViewById(R.id.schedule_horizontal_list);
        mDaysHorizontalList.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));

        // initialize scheduled recordings list
        mScheduledList = (RecyclerView) view.findViewById(R.id.schedule_vertical_list);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSimpleItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(mScheduledList);
        mScheduledList.setLayoutManager(new LinearLayoutManager(mActivity));
        mScheduledList.setAdapter(scheduledRecordsAdapter);

        // initialize all recordings list
        mAllScheduleList = (RecyclerView) view.findViewById(R.id.schedule_all_recyclerView);
        mAllScheduleList.setLayoutManager(new LinearLayoutManager(mActivity));
        mAllScheduleList.setAdapter(allRecordingsAdapter);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.all_schedule_list_button:
                isScheduleSelected = false;
                if (allRecordingsAdapter.getItemCount() > 0) {
                    unselectedText.setVisibility(View.GONE);
                    noRecordsText.setVisibility(View.GONE);
                }
                scheduledLayout.setVisibility(View.GONE);
                allScheduleLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.scheduled_list_button:
                isScheduleSelected = true;
                allScheduleLayout.setVisibility(View.GONE);
                scheduledLayout.setVisibility(View.VISIBLE);
                if (MainActivity.selectedGroup != null) {
                    getAllScheduledRecords(MainActivity.selectedGroup);
                } else if (MainActivity.selectedReceivers.size() > 0) {
                    getAllScheduledRecords(MainActivity.selectedReceivers);
                }
                break;
        }
    }

    public void clearList() {
        allRecordingsAdapter.clearList();
        unselectedText.setVisibility(View.VISIBLE);
    }

    public void getAllScheduledRecords(ArrayList<Receiver> receivers) {
        if (!gotScheduledRecords) {
            unselectedText.setVisibility(View.GONE);
            scheduledRecordsAdapter.clearList();
            ResponseReceiver.getInstance().clearResponseListenersList();
            ResponseReceiver.getInstance().addOnResponseListener(new ScheduledRecordingsListener());
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.SCHEDULES);
            for (Receiver receiver : receivers) {
                builder.receiverId(receiver.getReceiverId());
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            }
            gotScheduledRecords = true;
        }
    }

    public void getAllScheduledRecords(Group group) {
        if (!gotScheduledRecords) {
            unselectedText.setVisibility(View.GONE);
            scheduledRecordsAdapter.clearList();
            ResponseReceiver.getInstance().clearResponseListenersList();
            ResponseReceiver.getInstance().addOnResponseListener(new ScheduledRecordingsListener());
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.SCHEDULES);
            for (Receiver receiver : group.getReceivers()) {
                builder.receiverId(receiver.getReceiverId());
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            }
            gotScheduledRecords = true;
        }
    }

    public void getAllRecordingsFromServer(ArrayList<Receiver> receivers) {
        if (isScheduleSelected) getAllScheduledRecords(receivers);
        else {
            unselectedText.setVisibility(View.GONE);
            allRecordingsAdapter.clearList();
            ResponseReceiver.getInstance().clearResponseListenersList();
            ResponseReceiver.getInstance().addOnResponseListener(new AllRecordingsListener());
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.RECORDINGS);
            for (Receiver receiver : receivers) {
                builder.receiverId(receiver.getReceiverId());
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            }
        }
    }

    public void getAllRecordingsFromServer(Group group) {
        if (isScheduleSelected) getAllScheduledRecords(group);
        else {
            unselectedText.setVisibility(View.GONE);
            allRecordingsAdapter.clearList();
            ResponseReceiver.getInstance().clearResponseListenersList();
            ResponseReceiver.getInstance().addOnResponseListener(new AllRecordingsListener());
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.RECORDINGS);
            for (Receiver receiver : group.getReceivers()) {
                builder.receiverId(receiver.getReceiverId());
                SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
            }
        }
    }

    private void addRecordingsToList(ArrayList<Recording> recordings) {
        allRecordingsAdapter.addItems(recordings);
        if (allRecordingsAdapter.getItemCount() <= 0) {
            noRecordsText.setVisibility(View.VISIBLE);
            allScheduleLayout.setVisibility(View.GONE);
        } else {
            noRecordsText.setVisibility(View.GONE);
            allScheduleLayout.setVisibility(View.VISIBLE);
        }
    }

    private void addSchedulesToList(ArrayList<Schedule> schedules) {
        scheduledRecordsAdapter.addItems(schedules);
        if (scheduledRecordsAdapter.getItemCount() <= 0) {
            noRecordsText.setVisibility(View.VISIBLE);
            scheduledLayout.setVisibility(View.GONE);
        } else {
            noRecordsText.setVisibility(View.GONE);
            scheduledLayout.setVisibility(View.VISIBLE);
        }
    }


    class AllRecordingsListener implements OnResponseListener {

        @Override
        public void onResponse(JSONObject response) {
            addRecordingsToList(Recording.build(response));
        }

        @Override
        public void onCommandFailure(String message) {
            Snackbar.make(allScheduleLayout, message, Snackbar.LENGTH_SHORT).show();
        }

        @Override
        public void onError() {
            Snackbar.make(allScheduleLayout,
                    mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
        }
    }

    class ScheduledRecordingsListener implements OnResponseListener {

        @Override
        public void onResponse(JSONObject response) {
            addSchedulesToList(Schedule.build(response));
        }

        @Override
        public void onCommandFailure(String message) {
            Snackbar.make(scheduledLayout, message, Snackbar.LENGTH_SHORT).show();
        }

        @Override
        public void onError() {
            Snackbar.make(scheduledLayout,
                    mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
        }
    }

    private ItemTouchHelper.SimpleCallback mSimpleItemTouchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
            final int position = viewHolder.getAdapterPosition();
            final Schedule schedule = scheduledRecordsAdapter.removeItem(position);
            scheduledRecordsAdapter.notifyItemRemoved(position);
            Snackbar.make(scheduledLayout,
                    mActivity.getString(R.string.schedule_deleted), Snackbar.LENGTH_LONG)
                    .setAction(mActivity.getString(R.string.undo), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            scheduledRecordsAdapter.addItem(schedule, position);
                        }
                    }).setActionTextColor(mActivity.getResources()
                    .getColor(R.color.colorAlertAction))
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
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

    private void deleteScheduleFromServer(Schedule schedule) {
        Request.Builder builder = new Request.Builder();
        builder.command(Api.Command.DELETE_SCHEDULE);
        builder.scheduleId(schedule.getScheduleID());
        ResponseReceiver.getInstance().clearResponseListenersList();
        SonarCloudApp.socketService.sendRequest(builder.build().toJSON());
    }
}
