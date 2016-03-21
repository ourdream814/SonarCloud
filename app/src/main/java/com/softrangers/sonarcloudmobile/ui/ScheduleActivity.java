package com.softrangers.sonarcloudmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ScheduleEditAdapter;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.models.ScheduleListHeader;
import com.softrangers.sonarcloudmobile.utils.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import java.util.ArrayList;
import java.util.Date;

public class ScheduleActivity extends BaseActivity implements ExpandableListView.OnGroupClickListener,
        ScheduleEditAdapter.OnItemInteractionListener {

    public static final String ACTION_ADD_SCHEDULE = "com.softrangers.sonarcloudmobile.ACTION_ADD_SCHEDULE";
    public static final String ACTION_EDIT_SCHEDULE = "com.softrangers.sonarcloudmobile.ACTION_EDIT_SCHEDULE";

    public static final String SCHEDULE_EXTRAS = "key for schedule extras";

    private AnimatedExpandableListView mListView;
    private ScheduleEditAdapter mAdapter;
    private static Schedule schedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        TextView toolbarTitle = (TextView) findViewById(R.id.schedule_toolbarTitle);
        toolbarTitle.setTypeface(SonarCloudApp.avenirMedium);
        mListView = (AnimatedExpandableListView) findViewById(R.id.schedule_activity_expandableListView);
        Intent intent = getIntent();
        if (intent == null) return;
        String action = intent.getAction();
        switch (action) {
            case ACTION_ADD_SCHEDULE:
                schedule = new Schedule();
                Toast.makeText(this, "Add schedule", Toast.LENGTH_SHORT).show();
                break;
            case ACTION_EDIT_SCHEDULE:
                schedule = intent.getExtras().getParcelable(SCHEDULE_EXTRAS);
                mAdapter = new ScheduleEditAdapter(buildAdaptersList(schedule), this);
                intializeList(mAdapter);
                Toast.makeText(this, "Edit schedule", Toast.LENGTH_SHORT).show();
                break;
        }
        mAdapter.setOnItemInteractionListener(this);
    }

    private ArrayList<ScheduleListHeader> buildAdaptersList(final Schedule schedule) {
        ArrayList<ScheduleListHeader> headerArrayList = new ArrayList<>();
        Date date = schedule.getFormattedStartDate();
        // add title for date and time
        ScheduleListHeader headerTitle = new ScheduleListHeader(ScheduleListHeader.RowType.TITLE);
        headerTitle.setTitle(getString(R.string.date_and_time));
        headerTitle.setScheduleListItems(new ArrayList<Schedule>() {{add(schedule);}});
        headerArrayList.add(headerTitle);
        // add date list item
        ScheduleListHeader headerDate = new ScheduleListHeader(ScheduleListHeader.RowType.DATE);
        headerDate.setTitle(getString(R.string.date));
        headerDate.setSubtitle(schedule.getStringDate(date));
        headerDate.setScheduleListItems(new ArrayList<Schedule>() {{add(schedule);}});
        headerArrayList.add(headerDate);
        // add time list item
        ScheduleListHeader headerTime = new ScheduleListHeader(ScheduleListHeader.RowType.TIME);
        headerTime.setTitle(getString(R.string.time));
        headerTime.setSubtitle(schedule.getStringTime(date));
        headerTime.setScheduleListItems(new ArrayList<Schedule>() {{add(schedule);}});
        headerArrayList.add(headerTime);
        // add title for repeating
        ScheduleListHeader repeatTitle = new ScheduleListHeader(ScheduleListHeader.RowType.TITLE);
        repeatTitle.setTitle(getString(R.string.repeating));
        repeatTitle.setScheduleListItems(new ArrayList<Schedule>(){{add(schedule);}});
        headerArrayList.add(repeatTitle);
        return headerArrayList;
    }

    private void intializeList(ScheduleEditAdapter adapter) {
        mListView.setAdapter(adapter);
        mListView.setOnGroupClickListener(this);
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

    @Override
    public void onTimeChanged(TimePicker picker, int hour, int minutes, int groupPos, int childPos) {
        schedule.getFormattedStartDate().setHours(hour);
        schedule.getFormattedStartDate().setMinutes(minutes);
    }

    @Override
    public void onDateChanged(DatePicker picker, int day, int month, int year, int groupPos, int childPos) {
        schedule.getFormattedStartDate().setDate(day);
        schedule.getFormattedStartDate().setMonth(month);
        schedule.getFormattedStartDate().setYear(year);
    }

    public void saveSchedule(View view) {

    }

    public void cancelEdditSchedule(View view) {
        onBackPressed();
    }
}
