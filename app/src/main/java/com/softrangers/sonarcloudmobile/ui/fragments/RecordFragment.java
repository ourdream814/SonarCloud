package com.softrangers.sonarcloudmobile.ui.fragments;


import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
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
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lassana.recorder.AudioRecorder;
import com.github.lassana.recorder.AudioRecorderBuilder;
import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.AnnouncementRecAdapter;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.ui.ScheduleActivity;
import com.softrangers.sonarcloudmobile.utils.AudioProcessor;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.widgets.MillisChronometer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements View.OnClickListener,
        AnnouncementRecAdapter.OnAnnouncementRecordInteraction,
        MediaPlayer.OnPreparedListener, RadioGroup.OnCheckedChangeListener, View.OnTouchListener {

    private static final int ADD_SCHEDULE_REQUEST_CODE = 1813;
    private static final int SAMPLE_RATE = 16000;
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
    private AnnouncementRecAdapter mRecAdapter;
    private MediaPlayer mMediaPlayer;
    private MillisChronometer mRecordChronometer;
    private MillisChronometer mStreamingChronometer;
    private MillisChronometer mPTTChronometer;
    private static AudioRecorder audioRecorder;
    private int recordingNumber;

    public RecordFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_record, container, false);
        mActivity = (MainActivity) getActivity();
        mRecorderState = RecorderState.STOPPED;
        mStreamingState = StreamingState.WAITING;
        mPTTState = PTTState.RELEASED;
        mRecordAndSend = (RelativeLayout) view.findViewById(R.id.record_and_send_layoutHolder);
        mStreamLayout = (RelativeLayout) view.findViewById(R.id.stream_layoutHolder);
        mPushToTalkLayout = (RelativeLayout) view.findViewById(R.id.ptt_layoutHolder);
        RadioGroup layoutSelector = (RadioGroup) view.findViewById(R.id.send_audio_type_selector);
        layoutSelector.setOnCheckedChangeListener(this);
        RecyclerView recordingsList = (RecyclerView) view.findViewById(R.id.make_announcement_recordingsList);
        mRecAdapter = new AnnouncementRecAdapter(getRecordedFileList());
        mRecAdapter.setRecordInteraction(this);
        recordingsList.setLayoutManager(new LinearLayoutManager(mActivity));
        recordingsList.setAdapter(mRecAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSimpleCallback);
        itemTouchHelper.attachToRecyclerView(recordingsList);
        TextView recordAndSendText = (TextView) view.findViewById(R.id.record_and_sendTextView);
        recordAndSendText.setTypeface(SonarCloudApp.avenirMedium);
        TextView tapRecordingText = (TextView) view.findViewById(R.id.tap_record_textView);
        tapRecordingText.setTypeface(SonarCloudApp.avenirBook);

        mRecordChronometer = (MillisChronometer) view.findViewById(R.id.make_announcement_chronometer);
        mStreamingChronometer = (MillisChronometer) view.findViewById(R.id.streaming_chronometer);
        mPTTChronometer = (MillisChronometer) view.findViewById(R.id.ptt_chronometer);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mStartRecordingBtn = (ImageButton) view.findViewById(R.id.start_pause_recording_button);
        mStartRecordingBtn.setOnClickListener(this);
        mStopRecordingBtn = (ImageButton) view.findViewById(R.id.stop_recording_button);
        mStopRecordingBtn.setOnClickListener(this);
        mStartStreamingBtn = (ImageButton) view.findViewById(R.id.start_streaming_button);
        mStartStreamingBtn.setOnClickListener(this);
        mPTTButton = (ImageButton) view.findViewById(R.id.push_to_talk_button);
        mPTTButton.setOnTouchListener(this);
        invalidateRecordAndSendViews();
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_pause_recording_button:
                if (mRecorderState == RecorderState.STOPPED) {
                    startRecording();
                } else if (mRecorderState == RecorderState.RECORDING) {
                    pauseRecording();
                } else if (mRecorderState == RecorderState.PAUSED) {
                    resumeRecording();
                }
                break;
            case R.id.stop_recording_button:
                stopRecording();
                break;
            case R.id.start_streaming_button:
                if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
                    Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mStreamingState == StreamingState.WAITING) {
                    // TODO: 3/27/16 start streaming process
                    mStreamingState = StreamingState.STREAMING;
                    invalidateStreamViews();
                } else if (mStreamingState == StreamingState.STREAMING) {
                    // TODO: 3/27/16 stop streaming process
                    mStreamingState = StreamingState.WAITING;
                    invalidateStreamViews();
                }
                break;
        }
    }

    private ArrayList<Recording> getRecordedFileList() {
        File cacheDir = mActivity.getCacheDir();
        File[] recordings = cacheDir.listFiles();
        ArrayList<Recording> recordingList = new ArrayList<>();
        for (File file : recordings) {
            Recording recording = new Recording();
            recording.setFilePath(file.getAbsolutePath());
            recording.setRecordName(file.getName().split("\\.")[0]);
            recordingList.add(recording);
        }
        return recordingList;
    }


    private void startRecording() {
        recordAudio();
//        recordingNumber = SonarCloudApp.getInstance().getLastRecordingNumber() + 1;
//        String recordName = mActivity.getString(R.string.recording) + " " + recordingNumber;
//        String filePath = mActivity.getCacheDir().getAbsolutePath()
//                + File.separator + recordName + ".mp4";
//        audioRecorder = AudioRecorderBuilder.with(mActivity)
//                .fileName(filePath)
//                .config(AudioRecorder.MediaRecorderConfig.DEFAULT)
//                .loggable()
//                .build();
//        audioRecorder.start(new AudioRecorder.OnStartListener() {
//            @Override
//            public void onStarted() {
//                mRecorderState = RecorderState.RECORDING;
//                invalidateRecordAndSendViews();
//                SonarCloudApp.getInstance().addNewRecording(recordingNumber);
//            }
//
//            @Override
//            public void onException(Exception e) {
//                Log.e(this.getClass().getSimpleName(), e.getMessage());
//                Snackbar.make(mStartRecordingBtn, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
//            }
//        });
    }

    private void resumeRecording() {
    }

    private void pauseRecording() {
        audioRecorder.pause(new AudioRecorder.OnPauseListener() {
            @Override
            public void onPaused(String activeRecordFileName) {
                mRecorderState = RecorderState.PAUSED;
                invalidateRecordAndSendViews();
            }

            @Override
            public void onException(Exception e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage());
                Snackbar.make(mStartRecordingBtn, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void stopRecording() {
        audioRecorder.pause(new AudioRecorder.OnPauseListener() {
            @Override
            public void onPaused(String activeRecordFileName) {
                mRecorderState = RecorderState.STOPPED;
                audioRecorder = null;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invalidateRecordAndSendViews();
                        mRecAdapter.refreshList(getRecordedFileList());
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage());
                Snackbar.make(mStartRecordingBtn, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void invalidateRecordAndSendViews() {
        switch (mRecorderState) {
            case RECORDING:
                mRecordChronometer.setVisibility(View.VISIBLE);
                mStartRecordingBtn.setImageResource(R.mipmap.ic_button_pause);
                mStartRecordingBtn.setClickable(false);
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

    @Override
    public void onItemClick(Recording recording, int position, boolean isPlaying) {
        try {
            if (!isPlaying) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                    mRecAdapter.notifyItemChanged(position);
                }
            } else {
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(recording.getFilePath());
                mMediaPlayer.prepareAsync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    @Override
    public void onSendRecordClick(Recording recording, int position) {
        if (MainActivity.selectedReceivers.size() <= 0 && MainActivity.selectedGroup == null) {
            Toast.makeText(mActivity, mActivity.getString(R.string.please_select_pa), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(mActivity, "Not working yet", Toast.LENGTH_SHORT).show();
        // TODO: 3/23/16 send the audio file to server
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

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

    private void deleteRecord(Recording rec) {
        File file = new File(rec.getFilePath());
        if (file.delete()) {
            mRecAdapter.refreshList(getRecordedFileList());
        }
    }

    private void recordAudio() {
        // We'll be throwing stuff here
        String recordName = mActivity.getString(R.string.recording) + " " + recordingNumber;
        String filePath = mActivity.getCacheDir().getAbsolutePath()
                + File.separator + recordName;
        SonarCloudApp.getInstance().addNewRecording(recordingNumber);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }

        final int channel = 1;
        int channelOption;

        // Processed bytes
        int bytesProcessed = 0;
        int stopAfter = SAMPLE_RATE * channel * 2 * 5; // 5 seconds of recording


        if (channel == 1)
            channelOption = AudioFormat.CHANNEL_IN_MONO;
        else
            channelOption = AudioFormat.CHANNEL_IN_STEREO;

        int bufferSizeInBytes = 0;

        // Get our opus recorder
        AudioProcessor audioProcessor = AudioProcessor.encoder(SAMPLE_RATE, channel, AudioProcessor.OPUS_APPLICATION_VOIP);

        // The buffer
        byte[] recordBuffer = new byte[audioProcessor.pcmBytes];
        byte[] encodeBuffer = new byte[audioProcessor.bufferSize];

        Log.i("SonarCloud", "Buffer size got is " + bufferSizeInBytes);
        bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelOption, AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord audioRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                channelOption,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes
        );

        Log.i("SonarCloud", "Buffer size wanted is " + bufferSizeInBytes + " with " + SAMPLE_RATE
                + "Hz - " + audioProcessor.pcmBytes);

        // Now
        audioRecorder.startRecording();

        while (true) {
            audioRecorder.read(recordBuffer, 0, recordBuffer.length);

            Log.i("SonarCloud", "Encoding " + recordBuffer.length + " bytes.");

            // Encode
            int bytesWritten = audioProcessor.encodePCM(
                    recordBuffer,
                    0,
                    encodeBuffer,
                    0
            );

            outputStream.write(encodeBuffer, 0, bytesWritten);
            try {
                fos.write(encodeBuffer, 0, bytesWritten);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Keep track of how much
            bytesProcessed += recordBuffer.length;
            if (bytesProcessed >= stopAfter)
                break;
        }

        // Stop the recorder
        audioRecorder.stop();
        audioRecorder.release();

        // Get rid
        audioProcessor.dealloc();

        // Show the payload
        playAudio(outputStream.toByteArray(), SAMPLE_RATE);
    }

    private void playAudio(byte[] payloadBytes, int sampleRate) {
        final int channel = 1;
        int channelOption;

        if (channel == 1)
            channelOption = AudioFormat.CHANNEL_OUT_MONO;
        else
            channelOption = AudioFormat.CHANNEL_OUT_STEREO;

//                final int application = AudioProcessor.OPUS_APPLICATION_VOIP;

        AudioProcessor audioProcessor;

        try {
            // Get the decoder
            audioProcessor = AudioProcessor.decoder(sampleRate, channel);
        } catch (Error e) {
            e.printStackTrace();
            Toast.makeText(mActivity, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        int minimumBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelOption,
                AudioFormat.ENCODING_PCM_16BIT
        );


        int pcmBufferLength = 0;
        int payloadOffset = 0;

        Log.w("SonarCloud", "Minimum buffer size is " + minimumBufferSize);

        byte[] pcmBytes = new byte[audioProcessor.bufferSize];
        byte[] buffer = null;

        AudioTrack mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelOption,
                AudioFormat.ENCODING_PCM_16BIT,
                minimumBufferSize,
                AudioTrack.MODE_STREAM
        );

        // Now figure our lives
        mAudioTrack.play();

        // Now decode in sequence
        while (true) {
            // Read more
            if (payloadOffset < payloadBytes.length) {
                // Get the length
                ByteBuffer wrapped = ByteBuffer.wrap(payloadBytes); // big-endian by default
                int payloadSize = wrapped.getInt(payloadOffset);

                // Did we run out?
                if (payloadSize < 0 || payloadSize > AudioProcessor.MAX_PACKET) {
                    Log.e("Decoder", "Payload size for opus " + payloadSize + " is invalid.");
                    break;
                }

                payloadOffset += 4;

                // Read more
                // Decode us
                int bytesWritten = audioProcessor.decodePayload(
                        payloadBytes,
                        payloadOffset,
                        payloadSize,
                        pcmBytes,
                        0);

                // Now append
                if (buffer == null)
                    buffer = Arrays.copyOf(pcmBytes, bytesWritten);
                else {
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
                if (buffer.length >= minimumBufferSize || payloadOffset >= payloadBytes.length) {
                    // Write the audio data in full
                    mAudioTrack.write(buffer, 0, buffer.length);

                    // We are done with our buffer
                    buffer = null;
                } // Else fill more
            }
        }

        Log.w("SonarCloud", "Audio track stopped.");
        mAudioTrack.stop();
        mAudioTrack.release();

        // Get rid of this
        audioProcessor.dealloc();
    }

    enum RecorderState {
        RECORDING, STOPPED, PAUSED
    }

    enum StreamingState {
        STREAMING, WAITING
    }

    enum PTTState {
        TAPPED, RELEASED
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaPlayer.release();
    }
}
