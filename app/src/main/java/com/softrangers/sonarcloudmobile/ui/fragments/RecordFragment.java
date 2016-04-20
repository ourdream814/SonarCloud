package com.softrangers.sonarcloudmobile.ui.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.AnnouncementRecAdapter;
import com.softrangers.sonarcloudmobile.models.Receiver;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.models.Request;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.ui.ScheduleActivity;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.api.AudioSocket;
import com.softrangers.sonarcloudmobile.utils.opus.OpusPlayer;
import com.softrangers.sonarcloudmobile.utils.opus.OpusRecorder;
import com.softrangers.sonarcloudmobile.utils.opus.OpusRecorder.RecorderState;
import com.softrangers.sonarcloudmobile.utils.widgets.MillisChronometer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class RecordFragment extends Fragment implements View.OnClickListener,
        AnnouncementRecAdapter.OnAnnouncementRecordInteraction,
        RadioGroup.OnCheckedChangeListener, View.OnTouchListener,
        OpusRecorder.OnRecordListener {

    private static final int ADD_SCHEDULE_REQUEST_CODE = 1813;
    private static final int SAMPLE_RATE = 48000;
    private static final int BITRATE = 16000;
    private static final int CHANNEL = 1;
    private static final String RECORDINGS = "recordings";
    private static final String FILENAME = "Recording ";
    private static String PATH;
    private static boolean isSending;
    private RelativeLayout mRecordAndSend;
    private RelativeLayout mStreamLayout;
    private RelativeLayout mPushToTalkLayout;
    private ImageButton mStartStreamingBtn;
    private ImageButton mStartRecordingBtn;
    private ImageButton mStopRecordingBtn;
    private ImageButton mPTTButton;
    private MainActivity mActivity;
    private StreamingState mStreamingState;
    public AnnouncementRecAdapter mRecAdapter;
    private MillisChronometer mRecordChronometer;
    private MillisChronometer mStreamingChronometer;
    private MillisChronometer mPTTChronometer;
    private SelectedLayout mSelectedLayout;
    private static boolean isServerReady;
    private static OpusRecorder opusRecorder;
    private OpusPlayer mOpusPlayer;
    private RecordingLayout mRecordingLayout;


    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_record, container, false);
        setRetainInstance(true);
        // Obtain a link to activity
        mActivity = (MainActivity) getActivity();
        PATH = mActivity.getCacheDir().getAbsolutePath()
                + File.separator + RECORDINGS + File.separator;
        // Register the receiver with intent filters
        mActivity.registerReceiver(mAudioSendingReceiver, getIntentFilter());
        // set default states for all layouts included in current fragment
        mStreamingState = StreamingState.WAITING;
        // Link all views and set listeners
        // Record, Stream and PTT layouts
        mRecordAndSend = (RelativeLayout) view.findViewById(R.id.record_and_send_layoutHolder);
        mStreamLayout = (RelativeLayout) view.findViewById(R.id.stream_layoutHolder);
        mPushToTalkLayout = (RelativeLayout) view.findViewById(R.id.ptt_layoutHolder);
        // Radio group which holds all buttons used to change the layouts visibility within this fragment
        RadioGroup layoutSelector = (RadioGroup) view.findViewById(R.id.send_audio_type_selector);
        layoutSelector.setOnCheckedChangeListener(this);

        // Recordings list
        RecyclerView recordingsList = (RecyclerView) view.findViewById(R.id.make_announcement_recordingsList);
        mRecAdapter = new AnnouncementRecAdapter(getRecordedFileList());
        mRecAdapter.setRecordInteraction(this);
        recordingsList.setLayoutManager(new LinearLayoutManager(mActivity));
        recordingsList.setAdapter(mRecAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSimpleCallback);
        itemTouchHelper.attachToRecyclerView(recordingsList);
        // Record and send text views
        TextView recordAndSendText = (TextView) view.findViewById(R.id.record_and_sendTextView);
        recordAndSendText.setTypeface(SonarCloudApp.avenirMedium);
        TextView tapRecordingText = (TextView) view.findViewById(R.id.tap_record_textView);
        tapRecordingText.setTypeface(SonarCloudApp.avenirBook);

        opusRecorder = new OpusRecorder();
        opusRecorder.setOnRecordListener(this);
        mOpusPlayer = new OpusPlayer();
        // Chronometers for each view
        mRecordChronometer = (MillisChronometer) view.findViewById(R.id.make_announcement_chronometer);
        mStreamingChronometer = (MillisChronometer) view.findViewById(R.id.streaming_chronometer);
        mPTTChronometer = (MillisChronometer) view.findViewById(R.id.ptt_chronometer);
        // Buttons for all views
        mStartRecordingBtn = (ImageButton) view.findViewById(R.id.start_pause_recording_button);
        mStartRecordingBtn.setOnClickListener(this);
        mStopRecordingBtn = (ImageButton) view.findViewById(R.id.stop_recording_button);
        mStopRecordingBtn.setOnClickListener(this);
        mStartStreamingBtn = (ImageButton) view.findViewById(R.id.start_streaming_button);
        mStartStreamingBtn.setOnClickListener(this);
        mPTTButton = (ImageButton) view.findViewById(R.id.push_to_talk_button);
        mPTTButton.setOnTouchListener(this);
        mSelectedLayout = SelectedLayout.RECORDING;
        // set all views in default state
        invalidateRecordAndSendViews(opusRecorder.getRecorderState());
        return view;
    }

    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter(Api.Command.SEND_AUDIO);
        intentFilter.addAction(Api.Command.SEND);
        intentFilter.addAction(Api.EXCEPTION);
        intentFilter.addAction(Api.AUDIO_DATA_RESULT);
        return intentFilter;
    }

    //---------------- Common methods ----------------//

    /**
     * Handles clicks from all fragment buttons
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.start_pause_recording_button:
                // if there aren't any receiver or group selected inform user about this and return
                if (opusRecorder.isRecording() && mRecordingLayout != RecordingLayout.RECORDINGS) {
                    if (mStreamingState == StreamingState.STREAMING) {
                        Snackbar.make(mStreamingChronometer, mActivity.getString(R.string.stop_streaming), Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(mStreamingChronometer, mActivity.getString(R.string.stop_recording), Snackbar.LENGTH_SHORT).show();
                    }
                    return;
                }
                if (!opusRecorder.isRecording() || opusRecorder.isPaused()) {
                    // start recording if it is stopped
                    opusRecorder.startRecording();
                } else {
                    opusRecorder.pauseRecording();
                }
                break;
            case R.id.stop_recording_button:
                // stop recording
                opusRecorder.stopRecording();
                break;
            case R.id.start_streaming_button:
                if (opusRecorder.isRecording() && mRecordingLayout != RecordingLayout.STREAMING) {
                    Snackbar.make(mStreamingChronometer, mActivity.getString(R.string.stop_recording), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
                    Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
                    return;
                }
                // start stream the audio to server
                if (mStreamingState == StreamingState.WAITING && !AudioSocket.getInstance().isAudioConnectionReady()) {
                    startSendingAudioProcess(null, null, null);
                } else if (mStreamingState == StreamingState.WAITING && AudioSocket.getInstance().isAudioConnectionReady()) {
                    opusRecorder.startStreaming(AudioSocket.getInstance().getAudioSocket());
                } else if (mStreamingState == StreamingState.STREAMING) {
                    opusRecorder.stopRecording();
                }
                break;
        }
    }

    /**
     * Handles check state changes for top radio buttons
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.records_selector_button:
                mSelectedLayout = SelectedLayout.RECORDING;
                mRecordAndSend.setVisibility(View.VISIBLE);
                mStreamLayout.setVisibility(View.GONE);
                mPushToTalkLayout.setVisibility(View.GONE);
                break;
            case R.id.stream_selector_button:
                mSelectedLayout = SelectedLayout.STREAMING;
                mStreamLayout.setVisibility(View.VISIBLE);
                mRecordAndSend.setVisibility(View.GONE);
                mPushToTalkLayout.setVisibility(View.GONE);
                break;
            case R.id.ptt_selector_button:
                mSelectedLayout = SelectedLayout.PTT;
                mPushToTalkLayout.setVisibility(View.VISIBLE);
                mRecordAndSend.setVisibility(View.GONE);
                mStreamLayout.setVisibility(View.GONE);

                if (opusRecorder.isRecording() && mRecordingLayout != RecordingLayout.PUSH_TO_TALK) {
                    if (mStreamingState == StreamingState.STREAMING) {
                        Snackbar.make(mStreamingChronometer, mActivity.getString(R.string.stop_streaming), Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(mStreamingChronometer, mActivity.getString(R.string.stop_recording), Snackbar.LENGTH_SHORT).show();
                    }
                    return;
                }

                if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
                    Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
                    return;
                }
                startSendingAudioProcess(null, null, null);
                break;
        }
    }


    //---------------- Record and Send layout ----------------//

    /**
     * Called when a record from the list is clicked
     * @param recording which was clicked
     * @param position  of the clicked record in the list
     * @param isPlaying either true or false, depends on record playing state
     */
    @Override
    public void onItemClick(Recording recording, int position, boolean isPlaying) {
        mOpusPlayer.play(recording, position, mHandler);
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(final Message msg) {
            Recording recording = ((Recording) msg.obj);
            switch (msg.what) {
                case OpusPlayer.STATE_STARTED:
                case OpusPlayer.STATE_STOPPED:
                    recording.setLoading(false);
                    recording.setProgress(0);
                    notifyRecordingsAdapter(msg.arg1);
                    break;
                default:
                    int progress = msg.what;
                    recording.setProgress(progress);
                    notifyRecordingsAdapter(msg.arg1);
                    break;
            }
        }
    };

    /**
     * Called when schedule button for a record within the list is clicked
     * @param recording whom schedule button was clicked
     * @param position  of the recording in the list
     */
    @Override
    public void onScheduleClick(Recording recording, int position) {
        if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
            Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent addSchedule = new Intent(mActivity, ScheduleActivity.class);
        addSchedule.setAction(ScheduleActivity.ACTION_ADD_SCHEDULE);
        addSchedule.putExtra(ScheduleActivity.RECORD_BUNDLE, recording);
        mActivity.startActivityForResult(addSchedule, ADD_SCHEDULE_REQUEST_CODE);
    }

    @Override
    public void onStopPlayingClick(Recording recording, int position) {
        recording.setIsPlaying(false);
    }

    /**
     * Called when send button for a record within the list is clicked
     * @param recording whom send button was clicked
     * @param position  of the record in the list
     */
    @Override
    public void onSendRecordClick(Recording recording, int position, ProgressBar progressBar, ImageButton send) {
        if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
            Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
            return;
        }
        if (isSending) {
            Toast.makeText(mActivity, mActivity.getString(R.string.please_wait), Toast.LENGTH_SHORT).show();
            return;
        }
        startSendingAudioProcess(recording, progressBar, send);
    }

    /**
     * Load recorded files from the application cache
     * @return a list of recordings, each recording will contain a unique path to a file
     */
    public ArrayList<Recording> getRecordedFileList() {
        File cacheDir = new File(PATH);
        if (!cacheDir.exists()) cacheDir.mkdirs();
        File[] recordings = cacheDir.listFiles();
        ArrayList<Recording> recordingList = new ArrayList<>();
        if (recordings != null) {
            for (File file : recordings) {
                if (!file.isDirectory()) {
                    Recording recording = new Recording();
                    recording.setFilePath(file.getAbsolutePath());
                    double duration = (file.length() / (OpusRecorder.SAMPLE_RATE / 8.0)) - 1;
                    String recordName = file.getName().split("\\.")[0] + " (" + recording.stringForTime((int) duration) + ")";
                    recording.setRecordName(recordName);
                    recording.setLength(duration * 1000);
                    recordingList.add(recording);
                }
            }
        }
        return recordingList;
    }

    /**
     * Update views to inform user about changes
     */
    private void invalidateRecordAndSendViews(final RecorderState recorderState) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (recorderState) {
                    case RECORDING:
                        mRecordChronometer.setVisibility(View.VISIBLE);
                        mStartRecordingBtn.setImageResource(R.mipmap.ic_button_pause);
//                        mStartRecordingBtn.setClickable(false);
                        mStopRecordingBtn.setImageResource(R.mipmap.ic_button_stop);
                        mStopRecordingBtn.setActivated(true);
                        mStopRecordingBtn.setClickable(true);
                        mRecordChronometer.start();
                        break;
                    case PAUSED:
                        mStartRecordingBtn.setImageResource(R.mipmap.ic_button_record);
                        mRecordChronometer.stop();
                        break;
                    case STOPPED:
                        mStartRecordingBtn.setImageResource(R.mipmap.ic_button_record);
                        mStartRecordingBtn.setClickable(true);
                        mStopRecordingBtn.setImageResource(R.mipmap.ic_button_stop_deactivated);
                        mStopRecordingBtn.setActivated(false);
                        mStopRecordingBtn.setClickable(false);
                        mRecordChronometer.setVisibility(View.INVISIBLE);
                        mRecordChronometer.reset();
                        break;
                }
            }
        });
    }

    /**
     * Used to implement swipe to delete function
     */
    ItemTouchHelper.SimpleCallback mSimpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            final int position = viewHolder.getAdapterPosition();
            final Recording recording = mRecAdapter.removeItem(position);
            Snackbar.make(mRecordAndSend, mActivity.getString(R.string.record_deleted), Snackbar.LENGTH_SHORT)
                    .setAction(mActivity.getString(R.string.undo), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mRecAdapter.insertItem(position, recording);
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
                                    deleteRecord(recording);
                                    break;
                            }
                        }
                    }).show();
        }
    };

    /**
     * Delete a recorded file from application cache and reload the list
     * @param rec which needs to be deleted
     */
    private void deleteRecord(Recording rec) {
        File file = new File(rec.getFilePath());
        file.delete();
    }


    @Override
    public void onRecordStarted(final RecorderState recorderState) {
        Log.i(this.getClass().getSimpleName(), "Record started");
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (mSelectedLayout) {
                    case RECORDING:
                        invalidateRecordAndSendViews(recorderState);
                        mRecordingLayout = RecordingLayout.RECORDINGS;
                        break;
                    case STREAMING:
                        mStreamingState = StreamingState.STREAMING;
                        mRecordingLayout = RecordingLayout.STREAMING;
                        invalidateStreamViews(recorderState);
                        break;
                    case PTT:
                        invalidatePTTViews(recorderState);
                        mRecordingLayout = RecordingLayout.PUSH_TO_TALK;
                        break;
                }
            }
        });
    }

    @Override
    public void onRecordPaused(byte[] audioData, final RecorderState recorderState) {
        try {
            Log.i(this.getClass().getSimpleName(), "Record paused");
            // get the directory for temp files or create it if it does not exist
            File directory = new File(PATH + File.separator + "tmp" + File.separator);
            directory.mkdirs();
            // create a temp file
            File tempFile = new File(directory, "tmp.opus");
            // write bytes to temp file or append them to the end of the
            // file in case file already exists
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile, true);
            fileOutputStream.write(audioData, 0, audioData.length);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (mSelectedLayout) {
                        case RECORDING:
                            invalidateRecordAndSendViews(recorderState);
                            break;
                    }

                }
            });
        }

    }

    @Override
    public void onRecordFinished(final byte[] audioData, final RecorderState recorderState) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (mSelectedLayout) {
                    case RECORDING:
                        invalidateRecordAndSendViews(recorderState);
                        break;
                    case STREAMING:
                        mStreamingState = StreamingState.WAITING;
                        invalidateStreamViews(recorderState);
                        break;
                    case PTT:
                        invalidatePTTViews(recorderState);
                }
            }
        });
        switch (mRecordingLayout) {
            case PUSH_TO_TALK: {
                Snackbar.make(mPushToTalkLayout, "Sending audio", Snackbar.LENGTH_LONG).setAction("Undo",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(mActivity, "Sending canceled", Toast.LENGTH_SHORT).show();
                            }
                        }).setActionTextColor(mActivity.getResources()
                        .getColor(R.color.colorAlertAction)).setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        switch (event) {
                            case DISMISS_EVENT_TIMEOUT:
                            case DISMISS_EVENT_CONSECUTIVE:
                            case DISMISS_EVENT_MANUAL:
                                AudioSocket.getInstance().sendAudio(audioData);
                                break;
                        }
                    }
                }).show();
                break;
            }
            case RECORDINGS: {
                try {
                    Log.i(this.getClass().getSimpleName(), "Record finished");
                    // get the directory for temp files
                    File directoryTemp = new File(PATH + File.separator + "tmp" + File.separator);
                    directoryTemp.mkdirs();
                    // obtain last temp file if it exists
                    File tempFile = new File(directoryTemp, "tmp.opus");
                    byte[] writtenBytes = new byte[(int) tempFile.length()];
                    // if temp file exists read bytes from it to an array
                    if (tempFile.exists()) {
                        FileInputStream fis = new FileInputStream(tempFile);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        bis.read(writtenBytes, 0, writtenBytes.length);
                        bis.close();
                    }
                    // copy both arrays to allData[]
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(writtenBytes, 0, writtenBytes.length);
                    baos.write(audioData, 0, audioData.length);
                    byte[] allData = baos.toByteArray();

                    // create a new recorded file
                    File directory = new File(PATH);
                    directory.mkdirs();
                    int recordingNumber = SonarCloudApp.getInstance().getLastRecordingNumber();
                    File file = new File(directory, FILENAME + recordingNumber + ".opus");
                    // increment recording numbers
                    SonarCloudApp.getInstance().addNewRecording(recordingNumber);
                    // start writing all audio data to file
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
                    // write recorded bytes at the end of the file
                    bos.write(allData, 0, allData.length);

                    bos.flush();
                    bos.close();

                    baos.flush();
                    baos.close();

                    tempFile.delete();
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mRecAdapter.refreshList(getRecordedFileList());
                            Log.i(this.getClass().getSimpleName(), "Adapter updated");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mRecordingLayout = RecordingLayout.NOT_RECORDING;
                }
            }
        }
    }

    @Override
    public void onRecordFailed(Exception e, final RecorderState recorderState) {
        mRecordingLayout = RecordingLayout.NOT_RECORDING;
        Log.i(this.getClass().getSimpleName(), "Record failed");
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (mSelectedLayout) {
                    case RECORDING:
                        invalidateRecordAndSendViews(recorderState);
                        break;
                    case STREAMING:
                        mStreamingState = StreamingState.WAITING;
                        invalidateStreamViews(recorderState);
                        break;
                    case PTT:
                        invalidatePTTViews(recorderState);
                }
                Snackbar.make(mStreamLayout, mActivity.getString(R.string.unknown_error),
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }


    //---------------- Streaming layout ----------------//

    /**
     * Update views to inform user about changes
     */
    private void invalidateStreamViews(RecorderState recorderState) {
        switch (recorderState) {
            case RECORDING:
                mStartStreamingBtn.setImageResource(R.mipmap.ic_button_pause);
                mStreamingChronometer.setVisibility(View.VISIBLE);
                mStreamingChronometer.start();
                break;
            case STOPPED:
                mStartStreamingBtn.setImageResource(R.mipmap.ic_stream_microphone);
                mStreamingChronometer.setVisibility(View.GONE);
                mStreamingChronometer.stop();
                mStreamingChronometer.reset();
                break;
        }
    }

    /**
     * Used to track the streaming process state
     */
    enum StreamingState {
        STREAMING, WAITING
    }


    //---------------- Push to talk layout ----------------//

    /**
     * Called when PTT button is pressed or released
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (opusRecorder.isRecording() && mRecordingLayout != RecordingLayout.PUSH_TO_TALK) {
            if (mStreamingState == StreamingState.STREAMING) {
                Snackbar.make(mStreamingChronometer, mActivity.getString(R.string.stop_streaming), Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(mStreamingChronometer, mActivity.getString(R.string.stop_recording), Snackbar.LENGTH_SHORT).show();
            }
            return false;
        }
        int action = MotionEventCompat.getActionMasked(event);
        if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
            Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
            return false;
        }
        // do not start recording if server is not ready for data
        if (!AudioSocket.getInstance().isAudioConnectionReady()) {
            startSendingAudioProcess(null, null, null);
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                opusRecorder.startRecording();
                break;
            case MotionEvent.ACTION_UP:
                opusRecorder.stopRecording();
                break;
        }
        return true;
    }

    /**
     * Update PTT layout views to infor user about changes
     */
    private void invalidatePTTViews(RecorderState recorderState) {
        switch (recorderState) {
            case RECORDING:
                mPTTButton.setImageResource(R.mipmap.ic_button_ptt_active);
                mPTTChronometer.setVisibility(View.VISIBLE);
                mPTTChronometer.start();
                break;
            case STOPPED:
                mPTTButton.setImageResource(R.mipmap.ic_button_ptt);
                mPTTChronometer.setVisibility(View.GONE);
                mPTTChronometer.stop();
                mPTTChronometer.reset();
                break;
        }
    }

    /**
     * Notifies recordings list adapter about playing state of a recording changes
     * @param position to notify adapter about
     */
    private void notifyRecordingsAdapter(final int position) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecAdapter.notifyItemChanged(position);
            }
        });
    }


    //------------------ Send recording to server ---------------------//
    // link to selected recording
    private Recording mRecording;
    private ProgressBar mSendingProgress;
    private ImageButton mSendButton;

    // receiver for server responses
    public BroadcastReceiver mAudioSendingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (ScheduleActivity.fromScheduleActivity) return;
                if (mActivity.mSelectedFragment == MainActivity.SelectedFragment.ANNOUNCEMENTS) {
                    String action = intent.getAction();
                    if (action.equals(Api.AUDIO_DATA_RESULT)) {
                        onAudioSent();
                        return;
                    }
                    JSONObject jsonResponse = new JSONObject(intent.getExtras().getString(action));
                    boolean success = jsonResponse.optBoolean("success", false);
                    if (!success && !AudioSocket.getInstance().isAudioConnectionReady()) {
                        String message = jsonResponse.optString("message");
                        onCommandFailure(message);
                        return;
                    }
                    switch (action) {
                        case Api.Command.SEND_AUDIO:
                            onKeyAndIDReceived(jsonResponse);
                            break;
                        case Api.EXCEPTION:
                            String message = jsonResponse.optString("message");
                            if (message.equalsIgnoreCase("Ready for data.")) {
                                onServerReadyForData();
                            } else {
                                onErrorOccurred();
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                isSending = false;
                onErrorOccurred();
                e.printStackTrace();
            }
        }
    };

    /**
     * Start the sending process
     * @param recording which needs to be sent
     */
    public void startSendingAudioProcess(Recording recording, ProgressBar progressBar, ImageButton send) {
        try {
            if (mSelectedLayout == SelectedLayout.RECORDING) {
                mRecording = recording;
                mSendingProgress = progressBar;
                mSendButton = send;
                mSendButton.setClickable(false);
                mSendButton.setVisibility(View.INVISIBLE);
                mSendingProgress.setVisibility(View.VISIBLE);
            } else {
                mActivity.showLoading();
            }

            if (AudioSocket.getInstance().isAudioConnectionReady()) {
                onServerReadyForData();
                return;
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .command(Api.Command.SEND_AUDIO)
                    .bitrate(BITRATE)
                    .channels(CHANNEL)
                    .format(Api.FORMAT)
                    .samplerate(SAMPLE_RATE);
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
            request.put(Api.Options.PLAY_IMMEDIATELY, true).put(Api.Options.KEEP, false);
            MainActivity.dataSocketService.sendRequest(request);
        } catch (Exception e) {
            isSending = false;
            if (mSendButton != null && mSendingProgress != null) {
                mSendButton.setVisibility(View.VISIBLE);
                mSendingProgress.setVisibility(View.GONE);
                mSendButton.setClickable(true);
            }
            onErrorOccurred();
            e.printStackTrace();
        }
    }

    /**
     * Called when the key and id for record are received from server
     * @param response which contains "key" and "recordingID"
     * @throws JSONException
     */
    private void onKeyAndIDReceived(JSONObject response) throws JSONException {
        if (mRecording == null) mRecording = new Recording();
        String sendAudioKey = response.getString("key");
        mRecording.setRecordingId(response.getInt("recordingID"));
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.command(Api.Command.SEND).key(sendAudioKey);
        JSONObject request = requestBuilder.build().toJSON();
        AudioSocket.getInstance().setAudioConnection();
        while (!AudioSocket.getInstance().isAudioConnectionReady()) {
            // wait till connection will be ok to start sending audio data
        }
        AudioSocket.getInstance().sendRequest(request);
    }

    /**
     * Called when server says "Ready for data."
     */
    private void onServerReadyForData() {
        isServerReady = true;
        mActivity.dismissLoading();
        switch (mSelectedLayout) {
            case RECORDING: {
                try {
                    File file = new File(mRecording.getFilePath());
                    int size = (int) file.length();
                    byte[] bytes = new byte[size];
                    FileInputStream fis = new FileInputStream(new File(mRecording.getFilePath()));
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    bis.read(bytes, 0, bytes.length);
                    bis.close();
                    AudioSocket.getInstance().sendAudio(bytes);
                } catch (Exception e) {
                    isSending = false;
                    if (mSendButton != null && mSendingProgress != null) {
                        mSendButton.setVisibility(View.VISIBLE);
                        mSendingProgress.setVisibility(View.GONE);
                        mSendButton.setClickable(true);
                    }
                    onErrorOccurred();
                    e.printStackTrace();
                }
                break;
            }
            case PTT: {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.dismissLoading();
                        Toast.makeText(mActivity, "Press and hold to record your message", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            }
            case STREAMING: {
                mActivity.dismissLoading();
                opusRecorder.startStreaming(AudioSocket.getInstance().getAudioSocket());
                break;
            }
        }

    }

    /**
     * Called when the data is sent
     */
    private void onAudioSent() {
        isServerReady = false;
        MainActivity.statusChanged = true;
        switch (mSelectedLayout) {
            case PTT:
            case RECORDING: {
                MainActivity.statusChanged = true;
                Snackbar.make(mRecordAndSend, mActivity.getString(R.string.audio_sent), Snackbar.LENGTH_SHORT).show();
                if (mSendButton != null && mSendingProgress != null) {
                    mSendButton.setVisibility(View.VISIBLE);
                    mSendingProgress.setVisibility(View.GONE);
                    mSendButton.setClickable(true);
                    File file = new File(mRecording.getFilePath());
                    if (file.delete()) {
                        mRecAdapter.removeItem(mRecording);
                    }
                }
                break;
            }
        }
    }

    /**
     * called when the command which is sent to server fails
     * @param message with the problem description
     */
    private void onCommandFailure(String message) {
        mActivity.dismissLoading();
        isSending = false;
        isServerReady = false;
        if (message != null) {
            Snackbar.make(mStreamLayout, message, Snackbar.LENGTH_SHORT).show();
        }
        if (mSendButton != null && mSendingProgress != null) {
            mSendButton.setVisibility(View.VISIBLE);
            mSendingProgress.setVisibility(View.GONE);
            mSendButton.setClickable(true);
        }
    }

    /**
     * Called when an exception occurs while sending the audio
     */
    private void onErrorOccurred() {
        mActivity.dismissLoading();
        isSending = false;
        isServerReady = false;
        if (mSendButton != null && mSendingProgress != null) {
            mSendButton.setVisibility(View.VISIBLE);
            mSendingProgress.setVisibility(View.GONE);
            mSendButton.setClickable(true);
        }
    }

    enum SelectedLayout {
        RECORDING, STREAMING, PTT
    }

    enum RecordingLayout {
        RECORDINGS, STREAMING, PUSH_TO_TALK, NOT_RECORDING
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.unregisterReceiver(mAudioSendingReceiver);
    }
}
