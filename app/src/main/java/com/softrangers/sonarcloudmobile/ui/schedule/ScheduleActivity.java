package com.softrangers.sonarcloudmobile.ui.schedule;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
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
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.api.ConnectionReceiver;
import com.softrangers.sonarcloudmobile.utils.lock.PatternLockUtils;
import com.softrangers.sonarcloudmobile.utils.api.DataSocketService;
import com.softrangers.sonarcloudmobile.utils.api.SendAudioThread;
import com.softrangers.sonarcloudmobile.utils.ui.BaseActivity;
import com.softrangers.sonarcloudmobile.utils.RepeatingCheck;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleActivity extends BaseActivity implements ScheduleEditAdapter.OnItemClickListener, ConnectionReceiver.OnConnected {

    public static final String ACTION_ADD_SCHEDULE = "com.softrangers.sonarcloudmobile.ACTION_ADD_SCHEDULE";
    public static final String ACTION_EDIT_SCHEDULE = "com.softrangers.sonarcloudmobile.ACTION_EDIT_SCHEDULE";
    public static final String RECORD_BUNDLE = "key for record bundle";
    public static final String READY_FOR_DATA = "Ready for data.";
    private static final String DATE = "Date";
    private static final String TIME = "Time";
    private static final String REPEAT = "Repeat";
    private static final String REPEAT_UNTIL = "Repeat until";
    private static final int SAMPLE_RATE = 48000;
    private static final int BITRATE = 16000;
    private static final int CHANNEL = 1;
    public static final String SCHEDULE_EXTRAS = "key for schedule extras";
    private static SimpleDateFormat serverFormat;
    public static boolean fromScheduleActivity;

    private RecyclerView mRecyclerView;
    private ScheduleEditAdapter mAdapter;
    private static Schedule schedule;
    private static Recording recording;
    private Request.Builder mRequestBuilder;
    private String mAction;
    public static DataSocketService dataSocketService;
    private static int repeatingOption;
    private static String endDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        Intent socketIntent = new Intent(this, DataSocketService.class);
        bindService(socketIntent, mDataServiceConnection, Context.BIND_AUTO_CREATE);
        IntentFilter dataIntentFilter = new IntentFilter(Api.Command.UPDATE_SCHEDULE);
        dataIntentFilter.addAction(Api.Command.CREATE_SCHEDULE);
        dataIntentFilter.addAction(Api.Command.SEND_AUDIO);
        dataIntentFilter.addAction(Api.EXCEPTION);
        ConnectionReceiver.getInstance().addOnConnectedListener(this);
        registerReceiver(mBroadcastReceiver, dataIntentFilter);
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
        serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZZZ", Locale.getDefault());
        serverFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mAction = intent.getAction();
        switch (mAction) {
            // User made a new record and want to schedule playing
            case ACTION_ADD_SCHEDULE:
                schedule = new Schedule();
                Date date = new Date();
                schedule.setTime(serverFormat.format(date));
                schedule.setStartDate(serverFormat.format(date));
                recording = intent.getExtras().getParcelable(RECORD_BUNDLE);
                mAdapter = new ScheduleEditAdapter(buildAdaptersList(schedule));
                mRequestBuilder.command(Api.Command.SEND_AUDIO);
                isUnlocked = true;
                break;
            // User opened an existing schedule
            case ACTION_EDIT_SCHEDULE:
                toolbarTitle.setText(getString(R.string.edit_schedule));
                schedule = intent.getExtras().getParcelable(SCHEDULE_EXTRAS);
                mAdapter = new ScheduleEditAdapter(buildAdaptersList(schedule));
                mRequestBuilder.command(Api.Command.UPDATE_SCHEDULE);
                mRequestBuilder.scheduleId(String.valueOf(schedule.getScheduleID()));
                isUnlocked = true;
                break;
        }
        initializeList(mAdapter);
    }

    // needed to bind DataSocketService to current class
    protected ServiceConnection mDataServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // get the service instance
            dataSocketService = ((DataSocketService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // remove service instance
            dataSocketService = null;
        }
    };

    //---------------------------- Update schedule ----------------------------//
    /**
     * Receives the server response, check the status and then call the appropriate method
     * for that status
     */
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
                    case Api.Command.SEND_AUDIO:
                        onKeyAndIDReceived(jsonResponse);
                        break;
                }
            } catch (Exception e) {
                onErrorOccurred();
            }
        }
    };

    /**
     * Called by {@link ScheduleActivity#mBroadcastReceiver} when the response is successful
     *
     * @param response as JSON which is received from server
     */
    private void onResponseSucceed(JSONObject response) {
        dismissLoading();
        Schedule schedule = Schedule.buildSingle(response);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(mAction);
        intent.putExtra(mAction, schedule);
        setResult(RESULT_OK, intent);
        MainActivity.statusChanged = true;
        finish();
    }

    /**
     * Called by {@link ScheduleActivity#mBroadcastReceiver} when the server can't execute the
     * command for some reasons and the response status is false
     *
     * @param message either message from server or {@link com.softrangers.sonarcloudmobile.R.string#unknown_error}
     */
    private void onCommandFailure(String message) {
        dismissLoading();
        Snackbar.make(mRecyclerView, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Called by {@link ScheduleActivity#mBroadcastReceiver} when an error occurs during receiving
     * response from server, it is used to notify the user if something went wrong
     */
    private void onErrorOccurred() {
        dismissLoading();
        Snackbar.make(mRecyclerView, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
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
        Date date = new Date();
        if (mAction.equals(ACTION_EDIT_SCHEDULE)) {
            if (schedule.getRepeatOption() > 0)
                date = schedule.getScheduleDate();
            else date = schedule.getScheduleTime();
        }
        int repeatOption = schedule.getRepeatOption();
        if (repeatOption > 0) date = schedule.getScheduleDate();
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
            if (!endDate.equals("null")) {
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

    /**
     * Called when user click on save button from the toolbar
     */
    public void saveSchedule(View view) {
        showLoading();
        switch (mAction) {
            case ACTION_EDIT_SCHEDULE: {
                if (schedule.getRepeatOption() == 0) {
                    if (schedule.getTime() == null || schedule.getTime().equals("null")) {
                        schedule.setTime(schedule.getServerFormatDate(new Date()));
                    }
                    mRequestBuilder.deleteAfter(1)
                            .day(null)
                            .hour(null)
                            .minute(null)
                            .month(null)
                            .startDate(schedule.getStartDate())
                            .time(schedule.getTime());
                } else if (schedule.getRepeatOption() > 0) {
                    mRequestBuilder.deleteAfter(0)
                            .startDate(schedule.getStartDate())
                            .day(schedule.getDay())
                            .hour(schedule.getHour())
                            .minute(schedule.getMinute())
                            .month(schedule.getMonth())
                            .time(null)
                            .wday(schedule.getWday());
                    if (schedule.getEndDate() != null && !schedule.getEndDate().equals("null")) {
                        if (endDate != null) {
                            mRequestBuilder.endDate(schedule.getEndDate());
                        }
                    }
                }
                JSONObject request = mRequestBuilder.build().toJSON();
                dataSocketService.sendRequest(request);
                break;
            }
            case ACTION_ADD_SCHEDULE: {
                startSendingAudioProcess();
                break;
            }
        }
    }

    /**
     * Called when user click on cancel button from the toolbar
     */
    public void cancelEditSchedule(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED, new Intent(mAction));
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mDataServiceConnection);
        ConnectionReceiver.getInstance().removeOnResponseListener(this);
    }

    /**
     * Called when and item within the list is clicked
     *
     * @param itemTitle the title for clicked item, used to check which item was clicked
     * @param position  for this item in the adapter, used to update ui when a value was changed
     */
    @Override
    public void onItemClickListener(String itemTitle, final int position) {

        // obtain the date from schedule object
        final Date date = new Date();
        // get a calendar instance and set the time
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        if (mAction.equals(ACTION_EDIT_SCHEDULE)) {
            if (schedule.getRepeatOption() > 0) {
                if (schedule.getScheduleDate() != null)
                    calendar.setTimeInMillis(schedule.getScheduleDate().getTime());
            } else calendar.setTimeInMillis(schedule.getScheduleTime().getTime());
        }
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
                        String dateString = serverFormat.format(date);
                        schedule.setStartDate(dateString);
                        schedule.setTime(dateString);
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
                datePicker.setMinDate(Calendar.getInstance().getTimeInMillis() - 2000);
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
                                String dateString = serverFormat.format(date);
                                schedule.setStartDate(dateString);
                                schedule.setTime(dateString);
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
                numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                final int currentOption = schedule.getRepeatOption();
                numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        repeatingOption = newVal;
                    }
                });
                new AlertDialog.Builder(this).setView(numberPicker).setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    mAdapter.getItem(position).setSubtitle(repeatValues[repeatingOption]);
                                    mAdapter.notifyItemChanged(position);
                                    schedule = RepeatingCheck.setRepeating(schedule, repeatingOption);
                                    schedule.setRepeatOption();
                                    if (schedule.getRepeatOption() == 0)
                                        schedule.setScheduleTime(schedule.getScheduleStartDate());
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
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    mAdapter.getItem(position).setSubtitle(repeatValues[currentOption]);
                                    mAdapter.notifyItemChanged(position);
                                    schedule = RepeatingCheck.setRepeating(schedule, currentOption);
                                    schedule.setRepeatOption();
                                    endDate = null;
                                    if (schedule.getRepeatOption() == 0)
                                        schedule.setTime(schedule.getStartDate());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).show();
                break;
            case REPEAT_UNTIL:
                DatePickerDialog pickerDialog = new DatePickerDialog(
                        this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // set the date user have selected
                        calendar.setTimeInMillis(System.currentTimeMillis());
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        date.setTime(calendar.getTimeInMillis());
                        // update the interface
                        String dateString = serverFormat.format(date);
                        endDate = dateString;
                        schedule.setEndDate(dateString);
                        mAdapter.getItem(position).setSubtitle(schedule.getStringDate(date));
                        mAdapter.notifyItemChanged(position);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                // Obtain the date picker from dialog and set the minimum date
                DatePicker datePicker = pickerDialog.getDatePicker();
                datePicker.setMinDate(Calendar.getInstance().getTimeInMillis() - 2000);
                // show the dialog
                pickerDialog.show();
                break;
        }
    }


    /**
     * Convert the {@link ScheduleActivity#schedule} into a JSON object for adding a new schedule
     *
     * @return a JSON object from schedule object
     */
    private JSONObject buildScheduleObject() {
        JSONObject scheduleJSON = new JSONObject();
        try {
            scheduleJSON.put("startDate", schedule.getStartDate());
            if (schedule.getRepeatOption() > 0) {
                schedule.setTime(null);
                scheduleJSON.put("minute", schedule.getMinute());
                scheduleJSON.put("hour", schedule.getHour());
                scheduleJSON.put("day", schedule.getDay());
                scheduleJSON.put("month", schedule.getMonth());
                scheduleJSON.put("wday", schedule.getWday());
                scheduleJSON.put("deleteAfter", false);
            } else {
                scheduleJSON.put("time", schedule.getTime());
                scheduleJSON.put("deleteAfter", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scheduleJSON;
    }

    /**
     * Start the sending process
     */
    private void startSendingAudioProcess() {
        fromScheduleActivity = true;
        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .command(Api.Command.SEND_AUDIO)
                    .bitrate(BITRATE)
                    .channels(CHANNEL)
                    .format(Api.FORMAT)
                    .samplerate(SAMPLE_RATE)
                    .schedule(buildScheduleObject());
            if (MainActivity.selectedGroup != null) {
                requestBuilder.groupId(String.valueOf(MainActivity.selectedGroup.getGroupID()));
            } else if (MainActivity.selectedReceivers.size() > 0) {
                ArrayList<Integer> selectedReceivers = new ArrayList<>();
                for (Receiver receiver : MainActivity.selectedReceivers) {
                    selectedReceivers.add(receiver.getReceiverId());
                }
                requestBuilder.receiversID(selectedReceivers);
            }
            JSONObject request = requestBuilder.build().toJSON();
            request.put(Api.Options.PLAY_IMMEDIATELY, false).put(Api.Options.KEEP, schedule.getTime() == null);
            dataSocketService.sendRequest(request);
        } catch (Exception e) {
            onErrorOccurred();
            e.printStackTrace();
        }
    }

    /**
     * Called by {@link ScheduleActivity#mBroadcastReceiver} when the key
     * and id for record are received from server
     *
     * @param response which contains "key" and "recordingID"
     * @throws JSONException
     */
    private void onKeyAndIDReceived(JSONObject response) throws JSONException {
        String sendAudioKey = response.getString("key");
        recording.setRecordingId(response.getInt("recordingID"));
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.command(Api.Command.SEND).key(sendAudioKey);
        try {
            File file = new File(recording.getFilePath());
            int size = (int) file.length();
            byte[] bytes = new byte[size];
            FileInputStream fis = new FileInputStream(new File(recording.getFilePath()));
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(bytes, 0, bytes.length);
            bis.close();
            JSONObject request = requestBuilder.build().toJSON();
            request.remove("seq");
            new SendAudioThread(bytes, request, mSendAudioHandler).start();
        } catch (Exception e) {
            onErrorOccurred();
            e.printStackTrace();
        }
    }

    private Handler mSendAudioHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SendAudioThread.SENDING_STARTED: {
                    break;
                }
                case SendAudioThread.SENDING_SUCCEED: {
                    onAudioSent();
                    break;
                }
                case SendAudioThread.SENDING_FAILED: {
                    onErrorOccurred();
                    break;
                }
            }
        }
    };

    /**
     * Called when the data is sent
     */
    private void onAudioSent() {
        dismissLoading();
        MainActivity.statusChanged = true;
        File file = new File(recording.getFilePath());
        file.delete();
        fromScheduleActivity = false;
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(mAction);
        setResult(RESULT_OK, intent);
        MainActivity.statusChanged = true;
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (PatternLockUtils.checkConfirmPatternResult(this, requestCode, resultCode)) {
            finish();
        } else {
            isUnlocked = true;
        }
    }

    @Override
    public void onInternetConnectionRestored() {
        dataSocketService.restartConnection();
        Snackbar.make(mRecyclerView, "Internet connection restored",
                Snackbar.LENGTH_SHORT).show();
        showLoading();
    }

    @Override
    public void onInternetConnectionLost() {
        Snackbar.make(mRecyclerView, "Internet connection lost",
                Snackbar.LENGTH_SHORT).show();
        dismissLoading();
    }

    @Override
    public void onSocketConnected() {
        dismissLoading();
    }

    @Override
    public void onConnectionFailed() {
        dismissLoading();
    }

    @Override
    public void onConnectTimeOut() {
        dismissLoading();
        Snackbar.make(mRecyclerView, "Connection time out.", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onAudioConnectionClosed() {
        dismissLoading();
    }
}