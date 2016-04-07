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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.DaysAdapter;
import com.softrangers.sonarcloudmobile.adapters.ScheduleAllRecordingsAdapter;
import com.softrangers.sonarcloudmobile.adapters.ScheduledRecordsAdapter;
import com.softrangers.sonarcloudmobile.models.Day;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.ui.ScheduleActivity;
import com.softrangers.sonarcloudmobile.utils.api.AudioSocket;
import com.softrangers.sonarcloudmobile.utils.ui.BaseFragment;
import com.softrangers.sonarcloudmobile.utils.opus.OpusPlayer;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;

import org.json.JSONObject;

import java.util.ArrayList;


public class ScheduleFragment extends BaseFragment implements RadioGroup.OnCheckedChangeListener,
        ScheduledRecordsAdapter.OnScheduleClickListener, DaysAdapter.OnDayClickListener,
        ScheduleAllRecordingsAdapter.OnRecordClickListener, OpusPlayer.OnPlayListener {

    private static RelativeLayout scheduledLayout;
    private static RelativeLayout allScheduleLayout;

    private static TextView unselectedText;
    private TextView mScheduledNoRecords;

    public static boolean isScheduleSelected;
    private DaysAdapter mDaysAdapter;

    private static ScheduleAllRecordingsAdapter allRecordingsAdapter;
    private static ScheduledRecordsAdapter scheduledRecordsAdapter;
    private MainActivity mActivity;
    private int clickedPosition;
    private ProgressBar mLoadingProgress;
    private OpusPlayer mOpusPlayer;
    private View mRootView;

    public ScheduleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = inflater.inflate(R.layout.fragment_schedule, container, false);
        setRetainInstance(true);
        // get a link to fragment activity
        mActivity = (MainActivity) getActivity();
        IntentFilter intentFilter = new IntentFilter(Api.Command.RECORDINGS);
        intentFilter.addAction(Api.Command.SCHEDULES);
        intentFilter.addAction(Api.EXCEPTION);
        intentFilter.addAction(ScheduleActivity.ACTION_EDIT_SCHEDULE);
        intentFilter.addAction(ScheduleActivity.ACTION_ADD_SCHEDULE);
        intentFilter.addAction(Api.Command.GET_AUDIO);
        intentFilter.addAction(Api.AUDIO_READY_TO_PLAY);
        mActivity.registerReceiver(mBroadcastReceiver, intentFilter);
        mDaysAdapter = new DaysAdapter();
        mOpusPlayer = new OpusPlayer();
        mOpusPlayer.setOnPlayListener(this);
        // initialize adapters with empty lists
        ArrayList<Recording> recordings = new ArrayList<>();
        ArrayList<Schedule> schedules = new ArrayList<>();
        allRecordingsAdapter = new ScheduleAllRecordingsAdapter(recordings, mActivity);
        allRecordingsAdapter.setOnRecordClickListener(this);
        scheduledRecordsAdapter = new ScheduledRecordsAdapter(schedules, mActivity);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeViews(mRootView);
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
        mScheduledNoRecords = (TextView) view.findViewById(R.id.scheduled_noRecordsText);
        mScheduledNoRecords.setTypeface(SonarCloudApp.avenirBook);

        // Layouts which are holding all lists, used to hide/show the lists on the screen
        scheduledLayout = (RelativeLayout) view.findViewById(R.id.scheduled_list_layout);
        allScheduleLayout = (RelativeLayout) view.findViewById(R.id.schedule_all_layout);
        mLoadingProgress = (ProgressBar) view.findViewById(R.id.schedules_loadingProgress);

        // initialize horizontal days list
        RecyclerView daysHorizontalList = (RecyclerView) view.findViewById(R.id.schedule_horizontal_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false);
        daysHorizontalList.setLayoutManager(layoutManager);

        mDaysAdapter.setOnDayClickListener(this);
        daysHorizontalList.setAdapter(mDaysAdapter);
        // scroll to selected position and set it in the center of the screen
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, mActivity.getResources().getDisplayMetrics());
        int screenWidth = mActivity.getWindow().getDecorView().getWidth() - margin;
        int selectedPosition = mDaysAdapter.getSelectedPostion();
        layoutManager.scrollToPositionWithOffset(selectedPosition, screenWidth / 2);

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
                // hide the scheduled layout and show the all recordings layout
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scheduledLayout.setVisibility(View.GONE);
                        allScheduleLayout.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case R.id.scheduled_list_button:
                // Change the selected status to know which button was selected
                isScheduleSelected = true;
                // Hide the all recordings layout and show the scheduled layout
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        allScheduleLayout.setVisibility(View.GONE);
                        scheduledLayout.setVisibility(View.VISIBLE);
                        if (scheduledRecordsAdapter.getItemCount() <= 0
                                && MainActivity.selectedReceivers.size() <= 0
                                && MainActivity.selectedGroup == null
                                && unselectedText.getVisibility() != View.VISIBLE) {
                            mScheduledNoRecords.setVisibility(View.VISIBLE);
                        } else {
                            mScheduledNoRecords.setVisibility(View.GONE);
                        }
                    }
                });
                break;
        }
    }

    /**
     * Get all scheduled recordings from server for given receivers
     *
     * @param receivers for which to get recordings
     */
    public void getAllScheduledRecords(ArrayList<Receiver> receivers) {
        if (receivers == null) receivers = new ArrayList<>();
        // hide the unselected text
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                unselectedText.setVisibility(View.GONE);
            }
        });
        // clear current scheduled list
        scheduledRecordsAdapter.clearList();
        // build a request with provided receivers
        Request.Builder builder = new Request.Builder();
        builder.command(Api.Command.SCHEDULES);
        for (Receiver receiver : receivers) {
            builder.receiverId(receiver.getReceiverId());
            // send request to server
            SonarCloudApp.dataSocketService.sendRequest(builder.build().toJSON());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get all recordings from server for given receivers
     *
     * @param receivers for which to get recordings
     */
    public void getAllRecordingsFromServer(ArrayList<Receiver> receivers) {
        showLoading();
        if (receivers == null) receivers = new ArrayList<>();
        getAllScheduledRecords(receivers);
        // hide the unselected text
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                unselectedText.setVisibility(View.GONE);
            }
        });
        // build a request with provided receivers
        Request.Builder builder = new Request.Builder();
        builder.command(Api.Command.RECORDINGS);
        for (Receiver receiver : receivers) {
            builder.receiverId(receiver.getReceiverId());
            // send request to server
            SonarCloudApp.dataSocketService.sendRequest(builder.build().toJSON());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add recordings to the list with all receiver records
     *
     * @param recordings list to add to adapter
     */
    private void addRecordingsToList(final ArrayList<Recording> recordings) {
        // add recordings to adapter's list
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                allRecordingsAdapter.addItems(recordings);
                // check if there are any item in the list
                // if false then show the no data text and hide the list layout
                // else hide the no data text and show the list layout
                hideLoading();
                if (allRecordingsAdapter.getItemCount() <= 0) {
                    mScheduledNoRecords.setVisibility(View.VISIBLE);
                    allScheduleLayout.setVisibility(View.GONE);
                } else {
                    mScheduledNoRecords.setVisibility(View.GONE);
                    allScheduleLayout.setVisibility(View.VISIBLE);
                }
            }
        });
        MainActivity.statusChanged = false;
    }

    /**
     * Add scheduled recordings to the list with all receiver records
     *
     * @param schedules list to add to adapter
     */
    private void addSchedulesToList(final ArrayList<Schedule> schedules) {
        // add schedules to adapter's list
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scheduledRecordsAdapter.changeList(schedules);
                mActivity.dismissLoading();
                // check if there are any item in the list
                // if false then show the no data text and hide the list layout
                // else hide the no data text and show the list layout
                if (scheduledRecordsAdapter.getItemCount() <= 0) {
                    if (scheduledLayout.getVisibility() == View.VISIBLE
                            && unselectedText.getVisibility() != View.VISIBLE)
                        mScheduledNoRecords.setVisibility(View.VISIBLE);
                } else {
                    mScheduledNoRecords.setVisibility(View.GONE);
                }
            }
        });
        MainActivity.statusChanged = false;
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
    public void onSchedulePlayClick(Schedule schedule, Recording recording, SeekBar seekBar, TextView seekBarTime, int position) {
        recording.setLoading(true);
        notifyAllRecordAdapter(position);
        AudioSocket.getInstance().setAudioConnection();
        startGettingAudioData(recording, seekBar, seekBarTime, position);
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
                    case Api.AUDIO_READY_TO_PLAY: {
                        String path = intent.getExtras().getString(action);
                        onAudioReady(path);
                        break;
                    }
                    default:
                        if (mActivity.mSelectedFragment == MainActivity.SelectedFragment.RECORDINGS) {
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
                                case Api.Command.GET_AUDIO: {
                                    onAudioDetailsReceived(jsonResponse);
                                    break;
                                }
                                case Api.EXCEPTION: {
                                    String message = jsonResponse.optString("message");
                                    if (message.equalsIgnoreCase("Ready for data.")) {
                                        AudioSocket.getInstance().startReadingAudioData();
                                    } else {
                                        onErrorOccurred();
                                    }

                                    break;
                                }
                            }
                        }
                        break;
                }
            } catch (Exception e) {
                onErrorOccurred();
            }
        }
    };

    public void clearLists() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scheduledRecordsAdapter.clearList();
                allRecordingsAdapter.clearList();
                allSchedules.clear();
                unselectedText.setVisibility(View.VISIBLE);
                mScheduledNoRecords.setVisibility(View.GONE);
            }
        });
    }

    private static ArrayList<Schedule> allSchedules = new ArrayList<>();

    private void onSchedulesReceived(JSONObject response) {
        allSchedules.addAll(Schedule.build(response));
        Day day = mDaysAdapter.getDays().get(mDaysAdapter.getSelectedPostion());
        sortSchedule(day, allSchedules);
        addSchedulesToList(mDaysAdapter.getDays().get(mDaysAdapter.getSelectedPostion()).getSchedules());
    }

    private void sortSchedule(Day day, ArrayList<Schedule> schedules) {
        day.clearSchedules();
        for (Schedule schedule : schedules) {
            Schedule.sortAllSchedules(day, schedule, schedule.getRepeatOption());
        }
    }

    private void onRecordingsReceived(JSONObject response) {
        addRecordingsToList(Recording.build(response));
    }

    @Override
    public void onCommandFailure(String message) {
        mActivity.dismissLoading();
        Snackbar.make(allScheduleLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onErrorOccurred() {
        hideLoading();
        ArrayList<Receiver> receivers = new ArrayList<>();
        if (MainActivity.selectedReceivers != null && MainActivity.selectedReceivers.size() > 0) {
            receivers = MainActivity.selectedReceivers;
        } else if (MainActivity.selectedGroup != null) {
            receivers = MainActivity.selectedGroup.getReceivers();
        }
        getAllRecordingsFromServer(receivers);
        Snackbar.make(allScheduleLayout,
                mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.unregisterReceiver(mBroadcastReceiver);
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
                                case DISMISS_EVENT_CONSECUTIVE:
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
        SonarCloudApp.dataSocketService.sendRequest(builder.build().toJSON());
    }

    @Override
    public void onDayClick(Day day, int position) {
        sortSchedule(day, allSchedules);
        addSchedulesToList(day.getSchedules());
    }

    private void showLoading() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingProgress.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideLoading() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingProgress.setVisibility(View.GONE);
            }
        });
    }


    static int clickedRecordPosition = -1;
    Recording clickedRecording;
    SeekBar mClickedItemSeekBar;
    TextView mClickedItemSeekBarTime;

    @Override
    public void onItemClick(Recording recording, SeekBar seekBar, TextView seekBarTime, int position) {
        recording.setLoading(true);
        notifyAllRecordAdapter(position);
        AudioSocket.getInstance().setAudioConnection();
        startGettingAudioData(recording, seekBar, seekBarTime, position);
    }

    @Override
    public void onSeekBarChanged(Recording recording, SeekBar seekBar, TextView seekBarTime, int position, int progress) {

    }

    private void startGettingAudioData(Recording recording, SeekBar seekBar, TextView seekBarTime, int position) {
        if (!recording.isPlaying()) {
            clickedRecordPosition = position;
            clickedRecording = recording;
            mClickedItemSeekBar = seekBar;
            mClickedItemSeekBarTime = seekBarTime;
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.GET_AUDIO).recordingID(recording.getRecordingId());
            JSONObject request = builder.build().toJSON();
            Log.i(this.getClass().getSimpleName(), "Get audio: " + request.toString());
            SonarCloudApp.dataSocketService.sendRequest(request);
        }
    }

    private void onAudioDetailsReceived(JSONObject audioDetails) {
        Log.i(this.getClass().getSimpleName(), "Get audio response: " + audioDetails.toString());
        String key = audioDetails.optString("key", null);
        if (key != null) {
            Request.Builder builder = new Request.Builder();
            builder.command(Api.Command.RECEIVE).key(key);
            JSONObject request = builder.build().toJSON();
            request.remove("seq");
            Log.i(this.getClass().getSimpleName(), request.toString());
            AudioSocket.getInstance().sendRequest(request);
        }
    }

    private void onAudioReady(String path) {
        clickedRecording.setFilePath(path);
        mOpusPlayer.play(clickedRecording, clickedRecordPosition);
    }

    @Override
    public void onStartPlayback(Recording recording, int position) {
        Log.i(this.getClass().getSimpleName(), "Start playback");
        recording.setLoading(false);
        notifyAllRecordAdapter(position);
        AudioSocket.getInstance().closeAudioConnection();
    }

    @Override
    public void onStopPlayback(Recording recording, int position) {
        Log.i(this.getClass().getSimpleName(), "Stop playback");
        recording.setLoading(false);
        notifyAllRecordAdapter(position);
        AudioSocket.getInstance().closeAudioConnection();
    }

    @Override
    public void onPlaybackError(Recording recording, int position) {
        Log.i(this.getClass().getSimpleName(), "Error playback");
        recording.setLoading(false);
        notifyAllRecordAdapter(position);
        AudioSocket.getInstance().closeAudioConnection();
    }

    @Override
    public void onPlaying(final Recording recording, int position, final long duration) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int progress = (int) (duration / 1000);
                mClickedItemSeekBar.setProgress(progress);
                mClickedItemSeekBarTime.setText(recording.stringForTime(progress));
            }
        });
    }

    private void notifyAllRecordAdapter(final int position) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                allRecordingsAdapter.notifyItemChanged(position);
                scheduledRecordsAdapter.notifyItemChanged(position);
            }
        });
    }
}
