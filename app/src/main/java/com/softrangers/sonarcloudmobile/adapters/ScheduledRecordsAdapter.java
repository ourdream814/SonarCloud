package com.softrangers.sonarcloudmobile.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.models.Schedule;

import java.util.ArrayList;
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
        mSchedules.clear();
        for (int i = 0; i < recordings.size(); i++) {
            mSchedules.add(recordings.get(i));
            notifyItemInserted(i);
        }
    }

    public void addItems(ArrayList<Schedule> recordings) {
        for (Schedule recording : recordings) {
//            if (recording.getFormattedStartDate() == null) return;
            if (mSchedules.size() == 0) mSchedules.add(recording);
            boolean exists = false;
            for (Schedule rec : mSchedules) {
                if (rec.equals(recording)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) mSchedules.add(recording);
            notifyItemInserted(mSchedules.size() - 1);
        }
    }

    public void clearList() {
        mSchedules.clear();
        notifyDataSetChanged();
    }

    public void addItem( Schedule recording, int position) {
        mSchedules.add(position, recording);
        notifyItemInserted(position);
    }

    public Schedule removeItem(int position) {
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
        holder.mRecordTitle.setText(
                mContext.getString(R.string.recording) + " " + holder.mSchedule.getRecordingID()
        );

        Date date = holder.mSchedule.getFormattedTime();

        holder.mRecordLength.setText(holder.mSchedule.getRecording().getFromatedLength());

        holder.mHour.setText(holder.mSchedule.getStringHour(date));
        holder.mMinutesAmPm.setText(holder.mSchedule.getStringMinute(date));
        // TODO: 3/20/16 change the icon for playing state
        holder.mPlayPauseBtn.setImageResource(
                holder.mSchedule.isSelected() ? R.mipmap.ic_play : R.mipmap.ic_play
        );
    }

    @Override
    public int getItemCount() {
        return mSchedules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView mRecordTitle;
        final TextView mRecordLength;
        final TextView mHour;
        final TextView mMinutesAmPm;
        final ImageButton mPlayPauseBtn;
        Schedule mSchedule;
        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mRecordTitle = (TextView) itemView.findViewById(R.id.schedule_record_item_nameText);
            mRecordLength = (TextView) itemView.findViewById(R.id.schedule_record_timeText);
            mHour = (TextView) itemView.findViewById(R.id.schedule_hour_textView);
            mMinutesAmPm = (TextView) itemView.findViewById(R.id.schedule_minutes_textView);
            mPlayPauseBtn = (ImageButton) itemView.findViewById(R.id.schedule_record_item_playButton);
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
                        notifyItemChanged(lastSelectedPosition);
                    }
                    notifyItemChanged(currentSelectedPosition);
                    lastSelectedPosition = currentSelectedPosition;

                    if (mOnScheduleClickListener != null) {
                        mOnScheduleClickListener.onSchedulePlayClick(mSchedule, mSchedule.getRecording(),
                                currentSelectedPosition);
                    }
                    break;
                default:
                    if (mOnScheduleClickListener != null) {
                        mOnScheduleClickListener.onScheduleClick(mSchedule, getAdapterPosition());
                    }
                    break;
            }
        }
    }

    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule, int position);
        void onSchedulePlayClick(Schedule schedule, Recording recording, int position);
    }
}
