package com.softrangers.sonarcloudmobile.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
        return recording;
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
        holder.mTitleText.setText(holder.mRecording.getRecordName());
        holder.mPlayButton.setImageResource(
                holder.mRecording.isPlaying() ? R.mipmap.ic_record_play_stop_button : R.mipmap.ic_record_play_play_button
        );
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
        Recording mRecording;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mPlayButton = (ImageButton) itemView.findViewById(R.id.make_announcement_itemPlayBtn);
            mTitleText = (TextView) itemView.findViewById(R.id.make_announcement_itemTitle);
            mScheduleButton = (ImageButton) itemView.findViewById(R.id.make_announcement_scheduleRecording);
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
                        mRecordInteraction.onSendRecordClick(mRecording, position);
                    }
                    break;
                default:
                    currentPosition = position;
                    mRecording.setIsPlaying(!mRecording.isPlaying());
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
        void onSendRecordClick(Recording recording, int position);
    }
}
