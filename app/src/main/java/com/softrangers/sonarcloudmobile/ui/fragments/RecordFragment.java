package com.softrangers.sonarcloudmobile.ui.fragments;


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.rtp.AudioStream;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.adapters.AnnouncementRecAdapter;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import java.io.File;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements View.OnClickListener,
        Chronometer.OnChronometerTickListener, AnnouncementRecAdapter.OnAnnouncementRecordInteraction,
        MediaPlayer.OnPreparedListener {

    private ImageButton mStartRecordingBtn;
    private ImageButton mStopRecordingBtn;
    private MainActivity mActivity;
    private Chronometer mChronometer;
    private MediaRecorder mRecorder;
    private RecorderState mRecorderState;
    private RecyclerView mRecordingsList;
    private AnnouncementRecAdapter mRecAdapter;
    private MediaPlayer mMediaPlayer;

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
        mRecordingsList = (RecyclerView) view.findViewById(R.id.make_announcement_recordingsList);
        mRecAdapter = new AnnouncementRecAdapter(getRecordedFileList());
        mRecAdapter.setRecordInteraction(this);
        mRecordingsList.setLayoutManager(new LinearLayoutManager(mActivity));
        mRecordingsList.setAdapter(mRecAdapter);
        TextView recordAndSendText = (TextView) view.findViewById(R.id.record_and_sendTextView);
        recordAndSendText.setTypeface(SonarCloudApp.avenirMedium);
        TextView tapRecordingText = (TextView) view.findViewById(R.id.tap_record_textView);
        tapRecordingText.setTypeface(SonarCloudApp.avenirBook);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mChronometer = (Chronometer) view.findViewById(R.id.recording_chronometer);
//        mChronometer.setOnChronometerTickListener(this);
        mChronometer.setVisibility(View.INVISIBLE);

        mStartRecordingBtn = (ImageButton) view.findViewById(R.id.start_pause_recording_button);
        mStartRecordingBtn.setOnClickListener(this);
        mStopRecordingBtn = (ImageButton) view.findViewById(R.id.stop_recording_button);
        mStopRecordingBtn.setOnClickListener(this);
        mStopRecordingBtn.setActivated(false);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_pause_recording_button:
                if (mRecorderState == RecorderState.STOPPED ||
                        mRecorderState == RecorderState.PAUSED) {
                    startRecording();
                } else if (mRecorderState == RecorderState.RECORDING){
                    pauseRecording();
                }
                break;
            case R.id.stop_recording_button:
                finishRecording();
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
        int recordingNumber = SonarCloudApp.getInstance().getLastRecordingNumber() + 1;
        String recordName = mActivity.getString(R.string.recording) + " " + recordingNumber;
        String filePath = mActivity.getCacheDir().getAbsolutePath()
                + File.separator + recordName + ".mp4";

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(filePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "prepare() failed");
        }

        mRecorder.start();
        SonarCloudApp.getInstance().addNewRecording(recordingNumber);
        mRecorderState = RecorderState.RECORDING;
        invalidateViews();
    }

    private void pauseRecording() {
        mRecorder.stop();
        mRecorderState = RecorderState.PAUSED;
        invalidateViews();
    }

    private void finishRecording() {
        if (mRecorderState == RecorderState.RECORDING) {
            mRecorder.stop();
        }
        mRecorder.release();
        mRecAdapter.refreshList(getRecordedFileList());
        mRecorderState = RecorderState.STOPPED;
        invalidateViews();
    }

    private void invalidateViews() {
        switch (mRecorderState) {
            case RECORDING:
                mChronometer.setVisibility(View.VISIBLE);
                mChronometer.start();
                mStartRecordingBtn.setImageResource(R.mipmap.ic_button_pause);
                mStopRecordingBtn.setActivated(true);
                break;
            case PAUSED:
                mChronometer.stop();
                mStartRecordingBtn.setImageResource(R.mipmap.ic_button_record);
                break;
            case STOPPED:
                mChronometer.setVisibility(View.INVISIBLE);
                mChronometer.stop();
                mStartRecordingBtn.setImageResource(R.mipmap.ic_button_record);
                mStopRecordingBtn.setActivated(false);
                break;
        }
    }

    @Override
    public void onChronometerTick(Chronometer chronometer) {
        long time = SystemClock.elapsedRealtime() - chronometer.getBase();
        int h   = (int)(time /3600000);
        int m = (int)(time - h*3600000)/60000;
        int s= (int)(time - h*3600000- m*60000)/1000 ;
        String hh = h < 10 ? "0"+h: h+"";
        String mm = m < 10 ? "0"+m: m+"";
        String ss = s < 10 ? "0"+s: s+"";
        chronometer.setText(hh+":"+mm+":"+ss);
    }

    @Override
    public void onItemClick(Recording recording, int position, boolean isPlaying) {
        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            } else {
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

    }

    @Override
    public void onSendRecordClick(Recording recording, int position) {

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
