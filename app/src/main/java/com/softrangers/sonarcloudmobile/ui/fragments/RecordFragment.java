package com.softrangers.sonarcloudmobile.ui.fragments;


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.lassana.recorder.AudioRecorder;
import com.github.lassana.recorder.AudioRecorderBuilder;
import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.AnnouncementRecAdapter;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.widgets.MillisChronometer;

import java.io.File;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements View.OnClickListener,
        AnnouncementRecAdapter.OnAnnouncementRecordInteraction,
        MediaPlayer.OnPreparedListener {

    private ImageButton mStartRecordingBtn;
    private ImageButton mStopRecordingBtn;
    private MainActivity mActivity;
    private RecorderState mRecorderState;
    private AnnouncementRecAdapter mRecAdapter;
    private MediaPlayer mMediaPlayer;
    private MillisChronometer mChronometer;
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
        RecyclerView recordingsList = (RecyclerView) view.findViewById(R.id.make_announcement_recordingsList);
        mRecAdapter = new AnnouncementRecAdapter(getRecordedFileList());
        mRecAdapter.setRecordInteraction(this);
        recordingsList.setLayoutManager(new LinearLayoutManager(mActivity));
        recordingsList.setAdapter(mRecAdapter);
        TextView recordAndSendText = (TextView) view.findViewById(R.id.record_and_sendTextView);
        recordAndSendText.setTypeface(SonarCloudApp.avenirMedium);
        TextView tapRecordingText = (TextView) view.findViewById(R.id.tap_record_textView);
        tapRecordingText.setTypeface(SonarCloudApp.avenirBook);

        mChronometer = (MillisChronometer) view.findViewById(R.id.make_announcement_chronometer);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mStartRecordingBtn = (ImageButton) view.findViewById(R.id.start_pause_recording_button);
        mStartRecordingBtn.setOnClickListener(this);
        mStopRecordingBtn = (ImageButton) view.findViewById(R.id.stop_recording_button);
        mStopRecordingBtn.setOnClickListener(this);
        invalidateViews();
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
        recordingNumber = SonarCloudApp.getInstance().getLastRecordingNumber() + 1;
        String recordName = mActivity.getString(R.string.recording) + " " + recordingNumber;
        String filePath = mActivity.getCacheDir().getAbsolutePath()
                + File.separator + recordName + ".mp4";
        audioRecorder = AudioRecorderBuilder.with(mActivity)
                .fileName(filePath)
                .config(AudioRecorder.MediaRecorderConfig.DEFAULT)
                .loggable()
                .build();
        mStartRecording.execute(audioRecorder);
    }

    private void resumeRecording() {
        mStopRecording.execute(audioRecorder);
    }

    private void pauseRecording() {
        mPauseRecording.execute(audioRecorder);
    }

    private void stopRecording() {
        mStopRecording.execute(audioRecorder);
    }

    AsyncTask<AudioRecorder, Integer, Integer> mStartRecording = new AsyncTask<AudioRecorder, Integer, Integer>() {
        @Override
        protected Integer doInBackground(AudioRecorder... params) {
            params[0].start(new AudioRecorder.OnStartListener() {
                @Override
                public void onStarted() {
                    mRecorderState = RecorderState.RECORDING;
                    invalidateViews();
                    SonarCloudApp.getInstance().addNewRecording(recordingNumber);
                }

                @Override
                public void onException(Exception e) {
                    Log.e(this.getClass().getSimpleName(), e.getMessage());
                    Snackbar.make(mStartRecordingBtn, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    };

    AsyncTask<AudioRecorder, Void, Void> mStopRecording = new AsyncTask<AudioRecorder, Void, Void>() {
        @Override
        protected Void doInBackground(AudioRecorder... params) {
            final AudioRecorder recorder = params[0];
            params[0].pause(new AudioRecorder.OnPauseListener() {
                @Override
                public void onPaused(String activeRecordFileName) {
                    mRecorderState = RecorderState.STOPPED;
//                    recorder.cancel();
                    audioRecorder = null;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            invalidateViews();
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
            return null;
        }
    };

    AsyncTask<AudioRecorder, Void, Void> mPauseRecording = new AsyncTask<AudioRecorder, Void, Void>() {
        @Override
        protected Void doInBackground(AudioRecorder... params) {
            params[0].pause(new AudioRecorder.OnPauseListener() {
                @Override
                public void onPaused(String activeRecordFileName) {
                    mRecorderState = RecorderState.PAUSED;
                    invalidateViews();
                }

                @Override
                public void onException(Exception e) {
                    Log.e(this.getClass().getSimpleName(), e.getMessage());
                    Snackbar.make(mStartRecordingBtn, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    };

    AsyncTask<AudioRecorder, Void, Void> mResumeRecording = new AsyncTask<AudioRecorder, Void, Void>() {
        @Override
        protected Void doInBackground(AudioRecorder... params) {
            params[0].start(new AudioRecorder.OnStartListener() {
                @Override
                public void onStarted() {
                    mRecorderState = RecorderState.RECORDING;
                    invalidateViews();
                }

                @Override
                public void onException(Exception e) {
                    Log.e(this.getClass().getSimpleName(), e.getMessage());
                    Snackbar.make(mStartRecordingBtn, mActivity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    };

    private void invalidateViews() {
        switch (mRecorderState) {
            case RECORDING:
                mChronometer.setVisibility(View.VISIBLE);
                mStartRecordingBtn.setImageResource(R.mipmap.ic_button_pause);
                mStartRecordingBtn.setClickable(false);
                mStopRecordingBtn.setImageResource(R.mipmap.ic_button_stop);
                mStopRecordingBtn.setActivated(true);
                mStopRecordingBtn.setClickable(true);
                mChronometer.start();
                break;
            case PAUSED:
                mStartRecordingBtn.setImageResource(R.mipmap.ic_button_record);
                mChronometer.stop();
                break;
            case STOPPED:
                mStartRecordingBtn.setImageResource(R.mipmap.ic_button_record);
                mStartRecordingBtn.setClickable(true);
                mStopRecordingBtn.setImageResource(R.mipmap.ic_button_stop_deactivated);
                mStopRecordingBtn.setActivated(false);
                mStopRecordingBtn.setClickable(false);
                mChronometer.setVisibility(View.INVISIBLE);
                mChronometer.reset();
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
        // TODO: 3/23/16 open schedule activity with add new schedule action
    }

    @Override
    public void onSendRecordClick(Recording recording, int position) {
        // TODO: 3/23/16 send the audio file to server
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    enum RecorderState {
        RECORDING, STOPPED, PAUSED
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaPlayer.release();
    }
}
