package com.softrangers.sonarcloudmobile.ui.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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
import com.softrangers.sonarcloudmobile.utils.AudioProcessor;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.api.Api;
import com.softrangers.sonarcloudmobile.utils.widgets.MillisChronometer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class RecordFragment extends Fragment implements View.OnClickListener,
        AnnouncementRecAdapter.OnAnnouncementRecordInteraction,
        RadioGroup.OnCheckedChangeListener, View.OnTouchListener {

    private static final int ADD_SCHEDULE_REQUEST_CODE = 1813;
    private static final int SAMPLE_RATE = 48000;
    private static final int BITRATE = 16000;
    private static final int CHANNEL = 1;
    private static final String RECORDINGS = "recordings";
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
    private RecorderState mRecorderState;
    private StreamingState mStreamingState;
    private PTTState mPTTState;
    public AnnouncementRecAdapter mRecAdapter;
    private MillisChronometer mRecordChronometer;
    private MillisChronometer mStreamingChronometer;
    private MillisChronometer mPTTChronometer;
    private int recordingNumber;
    private AudioTrack mAudioTrack;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_record, container, false);
        // Obtain a link to activity
        mActivity = (MainActivity) getActivity();
        PATH = mActivity.getCacheDir().getAbsolutePath()
                + File.separator + RECORDINGS + File.separator;
        // Register the receiver with intent filters
        IntentFilter intentFilter = new IntentFilter(Api.Command.SEND_AUDIO);
        intentFilter.addAction(Api.Command.SEND);
        intentFilter.addAction(Api.EXCEPTION);
        mActivity.registerReceiver(mAudioSendingReceiver, intentFilter);
        // set default states for all layouts included in current fragment
        mRecorderState = RecorderState.STOPPED;
        mStreamingState = StreamingState.WAITING;
        mPTTState = PTTState.RELEASED;
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
        // set all views in default state
        invalidateRecordAndSendViews();
        return view;
    }


    //---------------- Common methods ----------------//

    /**
     * Handles clicks from all fragment buttons
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_pause_recording_button:
                if (mRecorderState == RecorderState.STOPPED) {
                    // start recording if it is stopped
                    startRecording();
                } else if (mRecorderState == RecorderState.RECORDING) {
                    // if the device is recording, pause it
                    mRecorderState = RecorderState.PAUSED;
                    invalidateRecordAndSendViews();
                } else if (mRecorderState == RecorderState.PAUSED) {
                    // if is paused then we should release the pause
                    mRecorderState = RecorderState.RECORDING;
                    invalidateRecordAndSendViews();
                }
                break;
            case R.id.stop_recording_button:
                // stop recording
                stopRecording();
                break;
            case R.id.start_streaming_button:
                // if the user did not select any receivers, inform about it and return
                if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
                    Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
                    return;
                }
                // start stream the audio to server
                if (mStreamingState == StreamingState.WAITING) {
                    // TODO: 3/27/16 start streaming process
                    mStreamingState = StreamingState.STREAMING;
                    invalidateStreamViews();
                    // stop streaming process
                } else if (mStreamingState == StreamingState.STREAMING) {
                    // TODO: 3/27/16 stop streaming process
                    mStreamingState = StreamingState.WAITING;
                    invalidateStreamViews();
                }
                break;
        }
    }

    /**
     * Handles chechk state changes for top radio buttons
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.records_selector_button:
                mRecordAndSend.setVisibility(View.VISIBLE);
                mStreamLayout.setVisibility(View.GONE);
                mPushToTalkLayout.setVisibility(View.GONE);
                break;
            case R.id.stream_selector_button:
                mStreamLayout.setVisibility(View.VISIBLE);
                mRecordAndSend.setVisibility(View.GONE);
                mPushToTalkLayout.setVisibility(View.GONE);
                break;
            case R.id.ptt_selector_button:
                mPushToTalkLayout.setVisibility(View.VISIBLE);
                mRecordAndSend.setVisibility(View.GONE);
                mStreamLayout.setVisibility(View.GONE);
                break;
        }
    }


    //---------------- Record and Send layout ----------------//

    /**
     * Called when a record from the list is clicked
     *
     * @param recording which was clicked
     * @param position  of the clicked record in the list
     * @param isPlaying either true or false, depends on record playing state
     */
    @Override
    public void onItemClick(Recording recording, int position, boolean isPlaying) {
        try {
            if (recording.isPlaying()) {
                recording.setIsPlaying(false);
            } else if (mAudioTrack == null) {
                new PlayProcess(recording, position);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when schedule button for a record within the list is clicked
     *
     * @param recording whom schedule button was clicked
     * @param position  of the recording in the list
     */
    @Override
    public void onScheduleClick(Recording recording, int position) {
        if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
            Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: 3/23/16 open schedule activity with add new schedule action
        Intent addSchedule = new Intent(mActivity, ScheduleActivity.class);
        addSchedule.setAction(ScheduleActivity.ACTION_ADD_SCHEDULE);
        addSchedule.putExtra(ScheduleActivity.RECORD_BUNDLE, recording);
        mActivity.startActivityForResult(addSchedule, ADD_SCHEDULE_REQUEST_CODE);
    }

    /**
     * Called when send button for a record within the list is clicked
     *
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
     *
     * @return a list of recordings, each recording will contain a unique path to a file
     */
    public ArrayList<Recording> getRecordedFileList() {
        File cacheDir = new File(PATH);
        if (!cacheDir.exists()) cacheDir.mkdirs();
        File[] recordings = cacheDir.listFiles();
        ArrayList<Recording> recordingList = new ArrayList<>();
        if (recordings != null) {
            for (File file : recordings) {
                Recording recording = new Recording();
                recording.setFilePath(file.getAbsolutePath());
                recording.setRecordName(file.getName().split("\\.")[0]);
                recordingList.add(recording);
            }
        }
        return recordingList;
    }

    /**
     * Fires the recording process
     */
    private void startRecording() {
        recordingNumber = SonarCloudApp.getInstance().getLastRecordingNumber();
        String recordName = mActivity.getString(R.string.recording) + " " + recordingNumber;
        new RecordingProcess(recordName);
    }

    /**
     * Update views to inform user about changes
     */
    private void invalidateRecordAndSendViews() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (mRecorderState) {
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
     *
     * @param rec which needs to be deleted
     */
    private void deleteRecord(Recording rec) {
        File file = new File(rec.getFilePath());
        if (file.delete()) {
            mRecAdapter.refreshList(getRecordedFileList());
        }
    }

    /**
     * Used to track the recording process state
     */
    enum RecorderState {
        RECORDING, STOPPED, PAUSED
    }


    //---------------- Streaming layout ----------------//

    /**
     * Update views to inform user about changes
     */
    private void invalidateStreamViews() {
        switch (mStreamingState) {
            case STREAMING:
                mStartStreamingBtn.setImageResource(R.mipmap.ic_button_pause);
                mStreamingChronometer.setVisibility(View.VISIBLE);
                mStreamingChronometer.start();
                break;
            case WAITING:
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
     * Called when PTT button is either pressed or released
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
            Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // TODO: 3/27/16 start recording
                mPTTChronometer.setVisibility(View.VISIBLE);
                mPTTChronometer.start();
                mPTTState = PTTState.TAPPED;
                invalidatePTTViews();
                break;
            case MotionEvent.ACTION_UP:
                // TODO: 3/27/16 stop recording and send the audio
                Snackbar.make(mPushToTalkLayout, "Sending audio", Snackbar.LENGTH_LONG).setAction("Undo",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(mActivity, "Sending canceled", Toast.LENGTH_SHORT).show();
                            }
                        }).setActionTextColor(mActivity.getResources()
                        .getColor(R.color.colorAlertAction)).show();
                mPTTChronometer.setVisibility(View.GONE);
                mPTTChronometer.stop();
                mPTTChronometer.reset();
                mPTTState = PTTState.RELEASED;
                invalidatePTTViews();
                break;
        }
        return true;
    }

    /**
     * Update PTT layout views to infor user about changes
     */
    private void invalidatePTTViews() {
        switch (mPTTState) {
            case TAPPED:
                mPTTButton.setImageResource(R.mipmap.ic_button_ptt_active);
                break;
            case RELEASED:
                mPTTButton.setImageResource(R.mipmap.ic_button_ptt);
                break;
        }
    }

    /**
     * Used to track ptt process state
     */
    enum PTTState {
        TAPPED, RELEASED
    }


    //------------------ Recording process ---------------------//
    /**
     * Starts a new thread to record the audio
     */
    class RecordingProcess implements Runnable {
        String mFileName;
        public RecordingProcess(String fileName) {
            mFileName = fileName;
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                recordAudio(mFileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Starts a new thread to play a recoded file
     */
    class PlayProcess implements Runnable {

        Recording mRecording;
        int mPosition;

        public PlayProcess(Recording recording, int position) {
            mRecording = recording;
            mPosition = position;
            new Thread(this, this.getClass().getSimpleName()).start();
        }

        @Override
        public void run() {
            try {
                playAudio(mRecording, mPosition);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Start recording the audio from device microphone
     *
     * @param fileName to store the recording in app cache till it will be sent to server
     */
    private void recordAudio(String fileName) throws Exception {
        // We'll be throwing stuff here
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        File path = new File(PATH);
        if (!path.exists()) path.mkdirs();
        File file = new File(path, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        SonarCloudApp.getInstance().addNewRecording(recordingNumber);
        int channelOption = AudioFormat.CHANNEL_IN_MONO;

        int bufferSizeInBytes = 0;

        // Get our opus recorder
        AudioProcessor audioProcessor = AudioProcessor.encoder(SAMPLE_RATE, CHANNEL, AudioProcessor.OPUS_APPLICATION_VOIP);

        // The buffer
        byte[] recordBuffer = new byte[audioProcessor.pcmBytes];
        byte[] encodeBuffer = new byte[audioProcessor.bufferSize];

        bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelOption, AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, channelOption,
                AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
        audioRecorder.startRecording();
        mRecorderState = RecorderState.RECORDING;
        invalidateRecordAndSendViews();
        while (mRecorderState == RecorderState.RECORDING || mRecorderState == RecorderState.PAUSED) {
            if (mRecorderState != RecorderState.PAUSED) {
                audioRecorder.read(recordBuffer, 0, recordBuffer.length);
                // Encode
                int bytesWritten = audioProcessor.encodePCM(recordBuffer, 0, encodeBuffer, 0);
                outputStream.write(encodeBuffer, 0, bytesWritten);
                bos.write(encodeBuffer, 0, bytesWritten);
            }
        }
        // Stop the recorder
        audioRecorder.stop();
        audioRecorder.release();
        bos.flush();
        bos.close();

        // Get rid
        audioProcessor.dealloc();
    }

    /**
     * Stop the recording process if there are any
     */
    private void stopRecording() {
        mRecorderState = RecorderState.STOPPED;
        invalidateRecordAndSendViews();
        mRecAdapter.refreshList(getRecordedFileList());
    }

    /**
     * Plays the given file by reading bytes from it
     *
     * @param recording which contains a file path
     * @param position  of the recording in the list, used to update UI
     */
    private void playAudio(Recording recording, int position) throws Exception {
        int channelOption;

        File file = new File(recording.getFilePath());
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        FileInputStream fis = new FileInputStream(new File(recording.getFilePath()));
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(bytes, 0, bytes.length);
        bis.close();
        channelOption = AudioFormat.CHANNEL_OUT_MONO;
        AudioProcessor audioProcessor;
        try {
            // Get the decoder
            audioProcessor = AudioProcessor.decoder(SAMPLE_RATE, CHANNEL);
        } catch (Error e) {
            e.printStackTrace();
            Toast.makeText(mActivity, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        int minimumBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                channelOption,
                AudioFormat.ENCODING_PCM_16BIT
        );

        int payloadOffset = 0;

        byte[] pcmBytes = new byte[audioProcessor.bufferSize];
        byte[] buffer = null;

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, channelOption,
                AudioFormat.ENCODING_PCM_16BIT, minimumBufferSize, AudioTrack.MODE_STREAM);

        // Now figure our lives
        mAudioTrack.play();
        recording.setIsPlaying(true);
        notifyRecordingsAdapter(position);

        // Now decode in sequence
        while (recording.isPlaying()) {
            // Read more
            if (payloadOffset < bytes.length) {
                // Get the length
                ByteBuffer wrapped = ByteBuffer.wrap(bytes); // big-endian by default
                int payloadSize = wrapped.getInt(payloadOffset);

                // Did we run out?
                if (payloadSize < 0 || payloadSize > AudioProcessor.MAX_PACKET) {
                    break;
                }
                payloadOffset += 4;
                // Decode us
                int bytesWritten = audioProcessor.decodePayload(bytes, payloadOffset, payloadSize, pcmBytes, 0);
                // Now append
                if (buffer == null) {
                    buffer = Arrays.copyOf(pcmBytes, bytesWritten);
                } else {
                    byte[] aBuffer = new byte[buffer.length + bytesWritten];
                    System.arraycopy(buffer, 0, aBuffer, 0, buffer.length);
                    System.arraycopy(pcmBytes, 0, aBuffer, buffer.length, bytesWritten);
                    buffer = aBuffer; // Replace
                }
                // Offset more
                payloadOffset += payloadSize;
            }

            // Shall we dance?
            if (buffer == null || buffer.length == 0) {
                // We have finished apparently
                break;
            } else {
                // Check if we have enough for our buffer or if we are done
                if (buffer.length >= minimumBufferSize || payloadOffset >= bytes.length) {
                    // Write the audio data in full
                    mAudioTrack.write(buffer, 0, buffer.length);
                    // We are done with our buffer
                    buffer = null;
                } // Else fill more
            }
        }

        mAudioTrack.stop();
        mAudioTrack.release();
        mAudioTrack = null;
        recording.setIsPlaying(false);
        notifyRecordingsAdapter(position);
        // Get rid of this
        audioProcessor.dealloc();
    }

    /**
     * Notifies recordings list adapter about playing state of a recording changes
     *
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
    BroadcastReceiver mAudioSendingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                JSONObject jsonResponse = new JSONObject(intent.getExtras().getString(action));
                boolean success = jsonResponse.optBoolean("success", false);
                if (!success && !SonarCloudApp.socketService.isAudioConnectionReady()) {
                    String message = jsonResponse.optString("message", getString(R.string.unknown_error));
                    onCommandFailure(message);
                    return;
                } else if (SonarCloudApp.socketService.isAudioConnectionReady()) {
                    onAudioSent();
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
            } catch (Exception e) {
                isSending = false;
                onErrorOccurred();
                e.printStackTrace();
            }
        }
    };

    /**
     * Start the sending process
     *
     * @param recording which needs to be sent
     */
    private void startSendingAudioProcess(Recording recording, ProgressBar progressBar, ImageButton send) {
        try {
            mRecording = recording;
            mSendingProgress = progressBar;
            mSendButton = send;
            mSendButton.setClickable(false);
            mSendButton.setVisibility(View.INVISIBLE);
            mSendingProgress.setVisibility(View.VISIBLE);
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
            SonarCloudApp.socketService.sendRequest(request);
        } catch (Exception e) {
            isSending = false;
            mSendButton.setVisibility(View.VISIBLE);
            mSendingProgress.setVisibility(View.GONE);
            mSendButton.setClickable(true);
            onErrorOccurred();
            e.printStackTrace();
        }
    }

    /**
     * Called when the key and id for record are received from server
     *
     * @param response which contains "key" and "recordingID"
     * @throws JSONException
     */
    private void onKeyAndIDReceived(JSONObject response) throws JSONException {
        String sendAudioKey = response.getString("key");
        mRecording.setRecordingId(response.getInt("recordingID"));
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.command(Api.Command.SEND).key(sendAudioKey);
        SonarCloudApp.socketService.setAudioConnection();
        while (!SonarCloudApp.socketService.isAudioConnectionReady()) {
        }
        SonarCloudApp.socketService.prepareServerForAudio(requestBuilder.build().toJSON());
    }

    /**
     * Called when server says "Ready for data."
     */
    private void onServerReadyForData() {
        try {
            File file = new File(mRecording.getFilePath());
            int size = (int) file.length();
            byte[] bytes = new byte[size];
            FileInputStream fis = new FileInputStream(new File(mRecording.getFilePath()));
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(bytes, 0, bytes.length);
            bis.close();
            SonarCloudApp.socketService.sendAudio(bytes);
        } catch (Exception e) {
            isSending = false;
            mSendButton.setVisibility(View.VISIBLE);
            mSendingProgress.setVisibility(View.GONE);
            mSendButton.setClickable(true);
            onErrorOccurred();
            e.printStackTrace();
        }
    }

    /**
     * Called when the data is sent
     */
    private void onAudioSent() {
        mSendButton.setVisibility(View.VISIBLE);
        mSendingProgress.setVisibility(View.GONE);
        mSendButton.setClickable(true);
        MainActivity.statusChanged = true;
        SonarCloudApp.socketService.closeAudioConnection();
        Snackbar.make(mRecordAndSend, mActivity.getString(R.string.audio_sent), Snackbar.LENGTH_SHORT).show();
        File file = new File(mRecording.getFilePath());
        if (file.delete()) {
            mRecAdapter.removeItem(mRecording);
        }
    }

    /**
     * called when the command which is sent to server fails
     *
     * @param message with the problem description
     */
    private void onCommandFailure(String message) {
        isSending = false;
        if (mSendButton != null && mSendingProgress != null) {
            mSendButton.setVisibility(View.VISIBLE);
            mSendingProgress.setVisibility(View.GONE);
            mSendButton.setClickable(true);
        }
        Snackbar.make(mRecordAndSend, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Called when an exception occurs while sending the audio
     */
    private void onErrorOccurred() {
        isSending = false;
        if (mSendButton != null && mSendingProgress != null) {
            mSendButton.setVisibility(View.VISIBLE);
            mSendingProgress.setVisibility(View.GONE);
            mSendButton.setClickable(true);
        }
        Snackbar.make(mRecordAndSend, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
    }
}
