package com.softrangers.sonarcloudmobile.ui.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.ui.MainActivity;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.widgets.ToggleImageButton;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements ToggleImageButton.OnCheckedChangeListener {

    private TextView mRecordAndSendText;
    private TextView mTapRecordingText;
    private ToggleImageButton mStartRecordingBtn;
    private ToggleImageButton mStopRecordingBtn;
    private MainActivity mActivity;

    public RecordFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_record, container, false);
        mActivity = (MainActivity) getActivity();
        mRecordAndSendText = (TextView) view.findViewById(R.id.record_and_sendTextView);
        mRecordAndSendText.setTypeface(SonarCloudApp.avenirMedium);
        mTapRecordingText = (TextView) view.findViewById(R.id.tap_record_textView);
        mTapRecordingText.setTypeface(SonarCloudApp.avenirBook);

        mStartRecordingBtn = (ToggleImageButton) view.findViewById(R.id.start_pause_recording_button);
        mStopRecordingBtn = (ToggleImageButton) view.findViewById(R.id.stop_recording_button);
        return view;
    }

    @Override
    public void onCheckedChanged(ToggleImageButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.start_pause_recording_button:
                if (!isChecked) {
                    Toast.makeText(mActivity, "Start recording", Toast.LENGTH_SHORT).show();
                    mStopRecordingBtn.setChecked(true);
                } else {
                    Toast.makeText(mActivity, "Pause recording", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.stop_recording_button:
                if (isChecked) {
                    Toast.makeText(mActivity, "Stop recording", Toast.LENGTH_SHORT).show();
                    mStopRecordingBtn.setChecked(false);
                    mStartRecordingBtn.setChecked(false);
                }
        }
    }
}
