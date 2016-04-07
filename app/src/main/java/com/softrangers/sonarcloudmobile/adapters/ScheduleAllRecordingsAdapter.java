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

import java.util.ArrayList;

/**
 * Created by eduard on 3/20/16.
 */
public class ScheduleAllRecordingsAdapter extends RecyclerView.Adapter<ScheduleAllRecordingsAdapter.ViewHolder> {

    private static final int NOT_SELECTED = -1;
    private static final int WAS_NOT_SELECTED = -2;

    private ArrayList<Recording> mRecordings;
    private OnRecordClickListener mOnRecordClickListener;
    private static int currentSelectedItem;
    private static int lastSelectedItem;
    private Context mContext;

    public ScheduleAllRecordingsAdapter(ArrayList<Recording> recordings, Context context) {
        mRecordings = recordings;
        currentSelectedItem = NOT_SELECTED;
        lastSelectedItem = WAS_NOT_SELECTED;
        mContext = context;
    }

    public void setOnRecordClickListener(OnRecordClickListener onRecordClickListener) {
        mOnRecordClickListener = onRecordClickListener;
    }

    public void changeList(ArrayList<Recording> recordings) {
        mRecordings.clear();
        mRecordings.addAll(recordings);
        notifyDataSetChanged();
    }

    public void addItems(ArrayList<Recording> recordings) {
        for (Recording recording : recordings) {
            if (mRecordings.size() == 0) mRecordings.add(recording);
            boolean exists = false;
            for (Recording rec : mRecordings) {
                if (rec.equals(recording)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) mRecordings.add(recording);
            notifyItemInserted(mRecordings.size() - 1);
        }
    }

    public void clearList() {
        mRecordings.clear();
        notifyDataSetChanged();
    }

    public void addItem(int position, Recording recording) {
        mRecordings.add(position, recording);
        notifyItemInserted(position);
    }

    public Recording removeItem(int position) {
        Recording recording = mRecordings.get(position);
        mRecordings.remove(position);
        notifyItemRemoved(position);
        return recording;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.schedule_all_record_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mRecording = mRecordings.get(position);

        if (holder.mRecording.isLoading()) {
            holder.mPlayStopButton.setVisibility(View.INVISIBLE);
        } else if (holder.mRecording.isPlaying()) {
            holder.mPlayStopButton.setVisibility(View.GONE);
        } else {
            holder.mPlayStopButton.setVisibility(View.VISIBLE);
        }

        holder.mRecordTitle.setVisibility(holder.mRecording.isPlaying() ? View.GONE : View.VISIBLE);
        holder.mRecordLength.setVisibility(holder.mRecording.isPlaying() ? View.GONE : View.VISIBLE);
        holder.mLoadingProgress.setVisibility(holder.mRecording.isLoading() ? View.VISIBLE : View.GONE);
        holder.mSeekBarLayout.setVisibility(holder.mRecording.isPlaying() ? View.VISIBLE : View.GONE);

        if (holder.mSeekBarLayout.getVisibility() == View.VISIBLE) {
            holder.mSeekBar.setMax(holder.mRecording.getLength());
        }

        if (holder.mRecordTitle.getVisibility() == View.VISIBLE) {
            holder.mRecordTitle.setText(
                    mContext.getString(R.string.recording) + " " + holder.mRecording.getRecordingId()
            );
        }

        if (holder.mRecordLength.getVisibility() == View.VISIBLE) {
            holder.mRecordLength.setText(holder.mRecording.getFromatedLength());
        }
    }

    @Override
    public int getItemCount() {
        return mRecordings.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
        final ImageButton mPlayStopButton;
        final TextView mRecordTitle;
        final TextView mRecordLength;
        final SeekBar mSeekBar;
        final TextView mSeekBarTime;
        final LinearLayout mSeekBarLayout;
        final ProgressBar mLoadingProgress;
        Recording mRecording;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mPlayStopButton = (ImageButton) itemView.findViewById(R.id.schedule_all_record_item_playButton);
            mRecordTitle = (TextView) itemView.findViewById(R.id.schedule_all_record_item_nameText);
            mRecordLength = (TextView) itemView.findViewById(R.id.schedule_all_record_timeText);
            mSeekBar = (SeekBar) itemView.findViewById(R.id.schedule_all_record_seekBar);
            mSeekBarTime = (TextView) itemView.findViewById(R.id.schedule_all_record_seekBarTime);
            mSeekBarLayout = (LinearLayout) itemView.findViewById(R.id.schedule_all_record_seekBarLayout);
            mLoadingProgress = (ProgressBar) itemView.findViewById(R.id.schedule_all_record_loadingProgress);

            mPlayStopButton.setOnClickListener(this);
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onClick(View v) {
            currentSelectedItem = getAdapterPosition();
            notifyItemChanged(currentSelectedItem);
            if (lastSelectedItem != WAS_NOT_SELECTED && lastSelectedItem != currentSelectedItem) {
                mRecordings.get(lastSelectedItem).setIsPlaying(false);
                notifyItemChanged(lastSelectedItem);
            }
            lastSelectedItem = currentSelectedItem;
            if (mOnRecordClickListener != null) {
                mOnRecordClickListener.onItemClick(mRecording, mSeekBar, mSeekBarTime, getAdapterPosition());
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                if (mOnRecordClickListener != null) {
                    mOnRecordClickListener.onSeekBarChanged(mRecording, mSeekBar, mSeekBarTime, getAdapterPosition(),  progress);
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

    public interface OnRecordClickListener {
        void onItemClick(Recording recording, SeekBar seekBar, TextView seekBarTime, int position);
        void onSeekBarChanged(Recording recording, SeekBar seekBar, TextView seekBarTime, int position,  int progress);
    }
}
