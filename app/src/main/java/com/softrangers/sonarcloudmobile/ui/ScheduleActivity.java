package com.softrangers.sonarcloudmobile.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.ScheduleEditAdapter;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.utils.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.RepeatingCheck;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleActivity extends BaseActivity implements ScheduleEditAdapter.OnItemClickListener {

    public static final String ACTION_ADD_SCHEDULE = "com.softrangers.sonarcloudmobile.ACTION_ADD_SCHEDULE";
    public static final String ACTION_EDIT_SCHEDULE = "com.softrangers.sonarcloudmobile.ACTION_EDIT_SCHEDULE";
    private static final String DATE = "Date";
    private static final String TIME = "Time";
    private static final String REPEAT = "Repeat";
    private static final String REPEAT_UNTIL = "Repeat until";

    public static final String SCHEDULE_EXTRAS = "key for schedule extras";

    private RecyclerView mRecyclerView;
    private ScheduleEditAdapter mAdapter;
    private static Schedule schedule;
    private Request.Builder mRequestBuilder;
    private String mAction;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        IntentFilter intentFilter = new IntentFilter(Api.Command.UPDATE_SCHEDULE);
        intentFilter.addAction(Api.Command.CREATE_SCHEDULE);
        intentFilter.addAction(Api.EXCEPTION);
        registerReceiver(mBroadcastReceiver, intentFilter);
        mRequestBuilder = new Request.Builder();
        // instantiate all views for this activity
        mRecyclerView = (RecyclerView) findViewById(R.id.schedule_activity_recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // get toolbar title text and set typeface
        TextView toolbarTitle = (TextView) findViewById(R.id.schedule_toolbarTitle);
        toolbarTitle.setTypeface(SonarCloudApp.avenirMedium);
        // get the intent and check action
        Intent intent = getIntent();
        if (intent == null) return;
        mAction = intent.getAction();
        switch (mAction) {
            // User made a new record and want to schedule playing
            case ACTION_ADD_SCHEDULE:
                schedule = new Schedule();
                break;
            // User opened an existing schedule
            case ACTION_EDIT_SCHEDULE:
                toolbarTitle.setText(getString(R.string.edit_schedule));
                schedule = intent.getExtras().getParcelable(SCHEDULE_EXTRAS);
                mRequestBuilder.command(Api.Command.UPDATE_SCHEDULE);
                mRequestBuilder.time(schedule.getTime());
                mRequestBuilder.scheduleId(schedule.getScheduleID());
                mRequestBuilder.minute(schedule.getMinute());
                mRequestBuilder.hour(schedule.getHour());
                mRequestBuilder.day(schedule.getDay());
                mRequestBuilder.month(schedule.getMonth());
                mRequestBuilder.wday(schedule.getWday());
                mRequestBuilder.deleteAfter(0); // TODO: 3/22/16 change this later
                mRequestBuilder.startDate(schedule.getServerFormatDate(new Date()));
                mRequestBuilder.endDate(schedule.getEndDate());
                mAdapter = new ScheduleEditAdapter(buildAdaptersList(schedule));
                initializeList(mAdapter);
                break;
        }
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                JSONObject jsonResponse = new JSONObject(intent.getExtras().getString(action));
                boolean success = jsonResponse.optBoolean("success", false);
                if (!success) {
                    String message = jsonResponse.optString("message", getString(R.string.unknown_error));
                    onCommandFailure(message);
                    return;
                }
                switch (action) {
                    case Api.Command.UPDATE_SCHEDULE:
                        onResponseSucceed(jsonResponse);
                        break;
                    case Api.Command.CREATE_SCHEDULE:
                        onResponseSucceed(jsonResponse);
                        break;
                }
            } catch (Exception e) {
                onErrorOccurred();
            }
        }
    };

    private void onResponseSucceed(JSONObject response) {
        Schedule schedule = Schedule.buildSingle(response);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(mAction);
        intent.putExtra(mAction, schedule);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void onCommandFailure(String message) {

    }

    private void onErrorOccurred() {

    }

    /**
     * Build a list for adapter, just to display the most important part for the schedule, date,
     * time, repeating and so on
     *
     * @param schedule to get the data from
     * @return an array list of schedules which contain row type, title and subtitle
     */
    private ArrayList<Schedule> buildAdaptersList(final Schedule schedule) {
        ArrayList<Schedule> headerArrayList = new ArrayList<>();
        Date date = schedule.getFormattedTime();
        int repeatOption = schedule.getRepeatOption();
        // item for Date and Time title
        Schedule dateTimeTitle = new Schedule();
        dateTimeTitle.setRowType(Schedule.RowType.TITLE);
        dateTimeTitle.setTitle(getString(R.string.date_and_time));
        headerArrayList.add(dateTimeTitle);
        // item for Date item with the schedule date
        Schedule dateListItem = new Schedule();
        dateListItem.setRowType(Schedule.RowType.ITEM);
        dateListItem.setTitle(getString(R.string.date));
        dateListItem.setSubtitle(schedule.getStringDate(date));
        headerArrayList.add(dateListItem);
        // item for time item with the schedule time
        Schedule timeListItem = new Schedule();
        timeListItem.setRowType(Schedule.RowType.ITEM);
        timeListItem.setTitle(getString(R.string.time));
        timeListItem.setSubtitle(schedule.getStringTime(date));
        headerArrayList.add(timeListItem);
        // item for Repeating title
        Schedule repeatTitle = new Schedule();
        repeatTitle.setRowType(Schedule.RowType.TITLE);
        repeatTitle.setTitle(getString(R.string.repeating));
        headerArrayList.add(repeatTitle);
        // item for repeating item with schedule repeating mode
        Schedule repeatingListItem = new Schedule();
        repeatingListItem.setRowType(Schedule.RowType.ITEM);
        repeatingListItem.setTitle(getString(R.string.repeat));
        repeatingListItem.setSubtitle(getResources().getStringArray(R.array.repeat_values)[repeatOption]);
        headerArrayList.add(repeatingListItem);

        if (repeatOption > 0) {
            Schedule repeatUntil = new Schedule();
            repeatUntil.setRowType(Schedule.RowType.ITEM);
            repeatUntil.setTitle(getString(R.string.repeat_until));
            String endDate = schedule.getEndDate();
            if (!endDate.equals("null") && endDate != null && !endDate.contains("00:00:00.000Z")) {
                repeatUntil.setSubtitle(schedule.getStringDate(schedule.getFormattedEndDate()));
            }
            headerArrayList.add(repeatUntil);
        }
        // return the list
        return headerArrayList;
    }

    /**
     * Initialize the list and click listener for adapter
     *
     * @param adapter adapter to set for RecyclerView
     */
    private void initializeList(ScheduleEditAdapter adapter) {
        adapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(adapter);
    }

    // Called when user press the save button on the top right
    public void saveSchedule(View view) {
        if (schedule.getRepeatOption() > 0) schedule.setTime(null);
        if (schedule.getStartDate().equals("null")) {
            schedule.setStartDate(schedule.getServerFormatDate(new Date()));
        }
        mRequestBuilder.minute(schedule.getMinute())
                .hour(schedule.getHour())
                .day(schedule.getDay())
                .month(schedule.getMonth())
                .wday(schedule.getWday())
                .time(schedule.getTime())
                .deleteAfter(schedule.getTime() == null ? 0 : 1)
                .endDate(schedule.getEndDate().equals("null") ? null : schedule.getEndDate())
                .startDate(schedule.getTime() == null ? schedule.getStartDate() : null);
        JSONObject request = mRequestBuilder.build().toJSON();
        SonarCloudApp.socketService.sendRequest(request);
    }

    // called when user press the cancel button on the top left
    public void cancelEditSchedule(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED, new Intent(mAction));
        finish();
        super.onBackPressed();
    }

    /**
     * Called when and item within the list is clicked
     *
     * @param itemTitle the title for clicked item, used to check which item was clicked
     * @param position  for this item in the adapter, used to update ui when a value was changed
     */
    @Override
    public void onItemClickListener(String itemTitle, final int position) {
        final SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.getDefault());
        serverFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // obtain the date from schedule object
        final Date date = schedule.getFormattedTime();
        // get a calendar instance and set the time
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        switch (itemTitle) {
            case DATE: {
                // if user selected date, create a date picker dialog set the minimum date for today and
                // show the dialog to the user
                DatePickerDialog pickerDialog = new DatePickerDialog(
                        this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // set the date user have selected
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        date.setTime(calendar.getTimeInMillis());
                        // TODO: 3/22/16 user update schedule date, set it for schedule
                        schedule.setStartDate(serverFormat.format(date));
                        mRequestBuilder.startDate(serverFormat.format(date));
                        // update the interface
                        mAdapter.getItem(position).setSubtitle(schedule.getStringDate(date));
                        mAdapter.notifyItemChanged(position);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                // Obtain the date picker from dialog and set the minimum date
                DatePicker datePicker = pickerDialog.getDatePicker();
                datePicker.setMinDate(Calendar.getInstance().getTimeInMillis() - 10);
                // show the dialog
                pickerDialog.show();
                break;
            }
            case TIME: {
                // if user clicks on time item, create a time picker dialog and show it to user
                TimePickerDialog pickerDialog = new TimePickerDialog(this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                // set the selected values in calendar instance
                                calendar.set(Calendar.HOUR, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                date.setTime(calendar.getTimeInMillis());
                                // TODO: 3/22/16 user update the schedule time, set new time for schedule
                                schedule.setStartDate(serverFormat.format(date));
                                mRequestBuilder.startDate(serverFormat.format(date));
                                // update the interface
                                mAdapter.getItem(position).setSubtitle(schedule.getStringTime(date));
                                mAdapter.notifyItemChanged(position);
                            }
                        }, calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE), false);
                // show the dialog
                pickerDialog.show();
                break;
            }
            case REPEAT:
                final String[] repeatValues = getResources().getStringArray(R.array.repeat_values);
                String currentValue = mAdapter.getItem(position).getSubtitle();
                int currentValuePosition = 0;
                for (int i = 0; i < repeatValues.length; i++) {
                    if (repeatValues[i].equalsIgnoreCase(currentValue)) {
                        currentValuePosition = i;
                        break;
                    }
                }
                NumberPicker numberPicker = new NumberPicker(this);
                numberPicker.setMinValue(0);
                numberPicker.setMaxValue(repeatValues.length - 1);
                numberPicker.setValue(currentValuePosition);
                numberPicker.setDisplayedValues(getResources().getStringArray(R.array.repeat_values));
                numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        mAdapter.getItem(position).setSubtitle(repeatValues[newVal]);
                        mAdapter.notifyItemChanged(position);
                        schedule = RepeatingCheck.setRepeating(schedule, newVal);
                        schedule.setRepeatOption();
                    }
                });
                new AlertDialog.Builder(this).setView(numberPicker).setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!mAdapter.getItem(position).getSubtitle().equalsIgnoreCase(repeatValues[0])) {
                                    Schedule schedule = new Schedule();
                                    schedule.setRowType(Schedule.RowType.ITEM);
                                    schedule.setTitle(getString(R.string.repeat_until));
                                    if (mAdapter.getItemCount() != position + 2) {
                                        mAdapter.addItem(schedule);
                                    }
                                } else if (mAdapter.getItemCount() > (position + 1)) {
                                    mAdapter.removeItem(position + 1);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
                break;
            case REPEAT_UNTIL:
                DatePickerDialog pickerDialog = new DatePickerDialog(
                        this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // set the date user have selected
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        date.setTime(calendar.getTimeInMillis());
                        // update the interface
                        schedule.setEndDate(serverFormat.format(date));
                        mAdapter.getItem(position).setSubtitle(schedule.getStringDate(date));
                        mAdapter.notifyItemChanged(position);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                // Obtain the date picker from dialog and set the minimum date
                DatePicker datePicker = pickerDialog.getDatePicker();
                datePicker.setMinDate(Calendar.getInstance().getTimeInMillis() - 10);
                // show the dialog
                pickerDialog.show();
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
    }
}
