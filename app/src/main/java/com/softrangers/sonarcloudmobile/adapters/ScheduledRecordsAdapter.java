package com.softrangers.sonarcloudmobile.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.models.Schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by eduard on 3/20/16.
 */
public class ScheduledRecordsAdapter extends RecyclerView.Adapter<ScheduledRecordsAdapter.ViewHolder> {

    private static final int NOT_SELECTED = -1;
    private static final int WAS_NOT_SELECTED = -2;

    private ArrayList<Schedule> mSchedules;
    private OnScheduleClickListener mOnScheduleClickListener;
    private Context mContext;

    private static int currentSelectedPosition;
    private static int lastSelectedPosition;

    public ScheduledRecordsAdapter(ArrayList<Schedule> schedules, Context context) {
        mSchedules = schedules;
        currentSelectedPosition = NOT_SELECTED;
        lastSelectedPosition = WAS_NOT_SELECTED;
        mContext = context;
    }

    public void setOnScheduleClickListener(OnScheduleClickListener onScheduleClickListener) {
        mOnScheduleClickListener = onScheduleClickListener;
    }

    public void changeList(ArrayList<Schedule> recordings) {
        currentSelectedPosition = NOT_SELECTED;
        lastSelectedPosition = WAS_NOT_SELECTED;
        mSchedules.clear();
        mSchedules.addAll(recordings);
        notifyDataSetChanged();
    }

    public void addItems(ArrayList<Schedule> recordings) {
        mSchedules.addAll(recordings);
        notifyDataSetChanged();
    }

    public void clearList() {
        currentSelectedPosition = NOT_SELECTED;
        lastSelectedPosition = WAS_NOT_SELECTED;
        mSchedules.clear();
        notifyDataSetChanged();
    }

    public void addItem(Schedule recording, int position) {
        if (position == currentSelectedPosition) currentSelectedPosition++;
        mSchedules.add(position, recording);
        notifyItemInserted(position);
    }

    public Schedule removeItem(int position) {
        if (position == currentSelectedPosition) currentSelectedPosition = NOT_SELECTED;
        if (position == lastSelectedPosition) lastSelectedPosition = WAS_NOT_SELECTED;
        Schedule recording = mSchedules.get(position);
        mSchedules.remove(position);
        notifyItemRemoved(position);
        return recording;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.schedule_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mSchedule = mSchedules.get(position);

        Recording recording = holder.mSchedule.getRecording();

        if (recording.isLoading()) {
            holder.mPlayPauseBtn.setVisibility(View.INVISIBLE);
        } else if (recording.isPlaying()) {
            holder.mPlayPauseBtn.setVisibility(View.GONE);
        } else {
            holder.mPlayPauseBtn.setVisibility(View.VISIBLE);
        }

        holder.mLoadingProgress.setVisibility(recording.isLoading() ? View.VISIBLE : View.GONE);

        if (recording.isPlaying()) {
            holder.mSeekBarLayout.setVisibility(View.VISIBLE);
            holder.mSeekBar.setMax(recording.getLength());
            holder.mRecordTitle.setVisibility(View.GONE);
            holder.mRecordLength.setVisibility(View.GONE);
            holder.mHour.setVisibility(View.GONE);
            holder.mMinutesAmPm.setVisibility(View.GONE);
        } else {
            holder.mSeekBarLayout.setVisibility(View.GONE);
            holder.mRecordTitle.setVisibility(View.VISIBLE);
            holder.mRecordLength.setVisibility(View.VISIBLE);
            holder.mHour.setVisibility(View.VISIBLE);
            holder.mMinutesAmPm.setVisibility(View.VISIBLE);

            holder.mRecordTitle.setText(
                    mContext.getString(R.string.recording) + " " + holder.mSchedule.getRecordingID()
            );

            Date date = holder.mSchedule.getScheduleTime() == null ?
                    holder.mSchedule.getScheduleDate() : holder.mSchedule.getScheduleTime();

            holder.mRecordLength.setText(holder.mSchedule.getRecording().getFromatedLength());

            holder.mHour.setText(holder.mSchedule.getStringHour(date));
            holder.mMinutesAmPm.setText(holder.mSchedule.getStringMinute(date));
        }
    }

    @Override
    public int getItemCount() {
        return mSchedules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
        final TextView mRecordTitle;
        final TextView mRecordLength;
        final TextView mHour;
        final TextView mMinutesAmPm;
        final ImageButton mPlayPauseBtn;
        final SeekBar mSeekBar;
        final TextView mSeekBarTime;
        final LinearLayout mSeekBarLayout;
        final ProgressBar mLoadingProgress;
        Schedule mSchedule;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mRecordTitle = (TextView) itemView.findViewById(R.id.schedule_record_item_nameText);
            mRecordLength = (TextView) itemView.findViewById(R.id.schedule_record_timeText);
            mHour = (TextView) itemView.findViewById(R.id.schedule_hour_textView);
            mMinutesAmPm = (TextView) itemView.findViewById(R.id.schedule_minutes_textView);
            mPlayPauseBtn = (ImageButton) itemView.findViewById(R.id.schedule_record_item_playButton);
            mSeekBar = (SeekBar) itemView.findViewById(R.id.schedule_all_record_seekBar);
            mSeekBarTime = (TextView) itemView.findViewById(R.id.schedule_all_record_seekBarTime);
            mSeekBarLayout = (LinearLayout) itemView.findViewById(R.id.schedule_all_record_seekBarLayout);
            mLoadingProgress = (ProgressBar) itemView.findViewById(R.id.schedule_all_record_loadingProgress);
            mPlayPauseBtn.setOnClickListener(this);
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.schedule_record_item_playButton:
                    currentSelectedPosition = getAdapterPosition();
                    mSchedule.setIsSelected(!mSchedule.isSelected());
                    if (lastSelectedPosition != WAS_NOT_SELECTED
                            && lastSelectedPosition != currentSelectedPosition) {
                        mSchedules.get(lastSelectedPosition).setIsSelected(false);
                        mSchedules.get(lastSelectedPosition).getRecording().setLoading(false);
                        notifyItemChanged(lastSelectedPosition);
                    }
                    notifyItemChanged(currentSelectedPosition);
                    lastSelectedPosition = currentSelectedPosition;

                    if (mOnScheduleClickListener != null) {
                        mOnScheduleClickListener.onSchedulePlayClick(mSchedule, mSchedule.getRecording(),
                                mSeekBar, mSeekBarTime, currentSelectedPosition);
                    }
                    break;
                default:
                    if (mOnScheduleClickListener != null) {
                        mOnScheduleClickListener.onScheduleClick(mSchedule, getAdapterPosition());
                    }
                    break;
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                if (mOnScheduleClickListener != null) {
                    mOnScheduleClickListener.onSeekBarChanged(mSchedule.getRecording(), mSeekBar, mSeekBarTime, getAdapterPosition(),  progress);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule, int position);

        void onSchedulePlayClick(Schedule schedule, Recording recording, SeekBar seekBar, TextView seekBarTime, int position);

        void onSeekBarChanged(Recording recording, SeekBar seekBar, TextView seekBarTime, int position,  int progress);
    }
}
