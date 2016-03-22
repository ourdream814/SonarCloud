package com.softrangers.sonarcloudmobile.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;

import java.util.ArrayList;

/**
 * Created by Eduard Albu on 21 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class ScheduleEditAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Schedule> mScheduleListItems;
    private OnItemClickListener mOnItemClickListener;

    public ScheduleEditAdapter(ArrayList<Schedule> scheduleListItems) {
        mScheduleListItems = scheduleListItems;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public Schedule getItem(int position) {
        return mScheduleListItems.get(position);
    }

    public void addItem(Schedule schedule) {
        mScheduleListItems.add(schedule);
        notifyItemInserted(mScheduleListItems.size() - 1);
    }

    public void removeItem(int position) {
        mScheduleListItems.remove(position);
        notifyItemRemoved(position);
    }

    public ArrayList<Schedule> getList() {
        return mScheduleListItems;
    }

    @Override
    public int getItemViewType(int position) {
        Schedule.RowType rowType = mScheduleListItems.get(position).getRowType();
        return Schedule.RowType.getIntRowType(rowType);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Schedule.RowType rowType = Schedule.RowType.getRowType(viewType);
        switch (rowType) {
            case TITLE: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_title_layout, parent, false);
                return new TitleViewHolder(view);
            }
            case ITEM: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_group_item, parent, false);
                return new ItemViewHolder(view);
            }
            default: {
                return new EmptyViewHolder(null);
            }
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Schedule.RowType rowType = mScheduleListItems.get(position).getRowType();
        Schedule listItem = mScheduleListItems.get(position);
        switch (rowType) {
            case TITLE: {
                TitleViewHolder titleHolder = (TitleViewHolder) holder;
                titleHolder.mListItem = listItem;
                titleHolder.mTitleText.setText(((TitleViewHolder) holder).mListItem.getTitle());
                break;
            }

            case ITEM: {
                ItemViewHolder itemHolder = (ItemViewHolder) holder;
                itemHolder.mListItem = listItem;
                itemHolder.mItemTitle.setText(itemHolder.mListItem.getTitle());
                itemHolder.mItemSubtitle.setText(itemHolder.mListItem.getSubtitle());
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return mScheduleListItems.size();
    }

    public class TitleViewHolder extends RecyclerView.ViewHolder {
        final TextView mTitleText;
        Schedule mListItem;
        public TitleViewHolder(View itemView) {
            super(itemView);
            mTitleText = (TextView) itemView.findViewById(R.id.schedule_group_titleText);
            mTitleText.setTypeface(SonarCloudApp.avenirMedium);
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView mItemTitle;
        final TextView mItemSubtitle;
        Schedule mListItem;
        public ItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mItemTitle = (TextView) itemView.findViewById(R.id.schedule_header_itemTitle);
            mItemSubtitle = (TextView) itemView.findViewById(R.id.schedule_header_itemSubtitle);

            mItemTitle.setTypeface(SonarCloudApp.avenirBook);
            mItemSubtitle.setTypeface(SonarCloudApp.avenirBook);
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClickListener(mListItem.getTitle(), getAdapterPosition());
            }
        }
    }

    public class EmptyViewHolder extends RecyclerView.ViewHolder {

        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface OnItemClickListener {
        void onItemClickListener(String itemTitle, int position);
    }
}