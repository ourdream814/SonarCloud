package com.softrangers.sonarcloudmobile.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Recording;

import java.util.ArrayList;

/**
 * Created by eduard on 3/23/16.
 */
public class AnnouncementRecAdapter extends RecyclerView.Adapter<AnnouncementRecAdapter.ViewHolder> {
    private static final int NOT_SELECTED = -1;
    private static final int WAS_NOT_SELECTED = -2;

    private ArrayList<Recording> mRecordings;
    private OnAnnouncementRecordInteraction mRecordInteraction;
    private static int currentPosition;
    private static int lastPosition;

    public AnnouncementRecAdapter(ArrayList<Recording> recordings) {
        mRecordings = recordings;
        currentPosition = NOT_SELECTED;
        lastPosition = WAS_NOT_SELECTED;
    }

    public void setRecordInteraction(OnAnnouncementRecordInteraction recordInteraction) {
        mRecordInteraction = recordInteraction;
    }

    public void refreshList(ArrayList<Recording> recordings) {
        mRecordings.clear();
        for (Recording recording : recordings) {
            mRecordings.add(recording);
        }
        notifyDataSetChanged();
    }

    public Recording removeItem(int position) {
        Recording recording = mRecordings.get(position);
        mRecordings.remove(position);
        notifyItemRemoved(position);
        if (lastPosition == getItemCount()) {
            lastPosition = WAS_NOT_SELECTED;
        }
        return recording;
    }

    public void removeItem(Recording recording) {
        mRecordings.remove(recording);
        notifyDataSetChanged();
        if (lastPosition == getItemCount()) {
            lastPosition = WAS_NOT_SELECTED;
        }
    }

    public void insertItem(int position, Recording recording) {
        mRecordings.add(position, recording);
        notifyItemInserted(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.announcement_recordings_list_item,
                parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mRecording = mRecordings.get(position);
        boolean isPlaying = holder.mRecording.isPlaying();
        if (isPlaying) {
            holder.mPlayingProgress.setVisibility(View.VISIBLE);
            holder.mPlayingProgress.setMax(holder.mRecording.getLength());
            holder.mPlayingProgress.setProgress(holder.mRecording.getProgress());
            holder.mPlayButton.setVisibility(View.GONE);
            holder.mStopButton.setVisibility(View.VISIBLE);
            holder.mScheduleButton.setVisibility(View.GONE);
            holder.mTitleText.setVisibility(View.GONE);
        } else {
            holder.mPlayingProgress.setVisibility(View.GONE);
            holder.mPlayButton.setVisibility(View.VISIBLE);
            holder.mStopButton.setVisibility(View.GONE);
            holder.mScheduleButton.setVisibility(View.VISIBLE);
            holder.mTitleText.setVisibility(View.VISIBLE);
            holder.mTitleText.setText(holder.mRecording.getRecordName());
            holder.mPlayButton.setImageResource(R.mipmap.ic_record_play_play_button);
        }
    }

    @Override
    public int getItemCount() {
        return mRecordings.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageButton mPlayButton;
        final TextView mTitleText;
        final ImageButton mScheduleButton;
        final ImageButton mSendRecord;
        final ProgressBar mSendingProgress;
        final ProgressBar mPlayingProgress;
        final ImageButton mStopButton;
        Recording mRecording;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mPlayButton = (ImageButton) itemView.findViewById(R.id.make_announcement_itemPlayBtn);
            mTitleText = (TextView) itemView.findViewById(R.id.make_announcement_itemTitle);
            mSendingProgress = (ProgressBar) itemView.findViewById(R.id.sending_record_progressBar);
            mScheduleButton = (ImageButton) itemView.findViewById(R.id.make_announcement_scheduleRecording);
            mPlayingProgress = (ProgressBar) itemView.findViewById(R.id.playing_record_progressBar);
            mStopButton = (ImageButton) itemView.findViewById(R.id.stop_playing_recordButton);
            mStopButton.setOnClickListener(this);
            mScheduleButton.setOnClickListener(this);
            mSendRecord = (ImageButton) itemView.findViewById(R.id.make_announcement_sendRecordingBtn);
            mSendRecord.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            switch (v.getId()) {
                case R.id.make_announcement_scheduleRecording:
                    if (mRecordInteraction != null) {
                        mRecordInteraction.onScheduleClick(mRecording, position);
                    }
                    break;
                case R.id.make_announcement_sendRecordingBtn:
                    if (mRecordInteraction != null) {
                        mRecordInteraction.onSendRecordClick(mRecording, position, mSendingProgress, mSendRecord);
                    }
                    break;
                case R.id.stop_playing_recordButton:
                    if (mRecordInteraction != null) {
                        mRecordInteraction.onStopPlayingClick(mRecording, position);
                    }
                    break;
                default:
                    currentPosition = position;
                    notifyItemChanged(currentPosition);
                    if (lastPosition != WAS_NOT_SELECTED && lastPosition != currentPosition) {
                        mRecordings.get(lastPosition).setIsPlaying(false);
                        notifyItemChanged(lastPosition);
                    }
                    lastPosition = currentPosition;
                    if (mRecordInteraction != null) {
                        mRecordInteraction.onItemClick(mRecording, position, mRecording.isPlaying());
                    }
                    break;
            }
        }
    }

    public interface OnAnnouncementRecordInteraction {
        void onItemClick(Recording recording, int position, boolean isPlaying);
        void onScheduleClick(Recording recording, int position);
        void onStopPlayingClick(Recording recording, int position);
        void onSendRecordClick(Recording recording, int position, ProgressBar progressBar, ImageButton send);
    }
}
