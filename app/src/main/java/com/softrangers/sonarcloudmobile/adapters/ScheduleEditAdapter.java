package com.softrangers.sonarcloudmobile.adapters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.models.ScheduleListHeader;
import com.softrangers.sonarcloudmobile.utils.SonarCloudApp;
import com.softrangers.sonarcloudmobile.utils.widgets.AnimatedExpandableListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Eduard Albu on 21 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class ScheduleEditAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {

    private static final long TEN_SECONDS = 10000;

    private ArrayList<ScheduleListHeader> mScheduleListItems;
    private LayoutInflater mLayoutInflater;
    private OnItemInteractionListener mOnItemInteractionListener;
    public Context mContext;

    public ScheduleEditAdapter(ArrayList<ScheduleListHeader> scheduleListItems, Context context) {
        mScheduleListItems = scheduleListItems;
        mLayoutInflater = LayoutInflater.from(context);
        mContext = context;
    }

    public void setOnItemInteractionListener(OnItemInteractionListener onItemInteractionListener) {
        mOnItemInteractionListener = onItemInteractionListener;
    }

    public void refreshData(ArrayList<ScheduleListHeader> scheduleListItems) {
        mScheduleListItems.clear();
        for (ScheduleListHeader header : scheduleListItems) {
            mScheduleListItems.add(header);
        }
        notifyDataSetChanged();
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Schedule item = mScheduleListItems.get(groupPosition).getScheduleListItems().get(childPosition);
        ScheduleListHeader.RowType rowType = mScheduleListItems.get(groupPosition).getRowType();
        BaseChild childHolder = new BaseChild();
        if (convertView == null) {
            switch (rowType) {
                case TIME:
                    childHolder = new TimeChild();
                    convertView = mLayoutInflater.inflate(R.layout.schedule_time_picker, parent, false);
                    ((TimeChild)childHolder).mTimePicker = (TimePicker) convertView.findViewById(R.id.schedule_activity_timePicker);
                    break;
                case DATE:
                    childHolder = new DateChild();
                    convertView = mLayoutInflater.inflate(R.layout.schedule_date_picker, parent, false);
                    ((DateChild)childHolder).mDatePicker = (DatePicker) convertView.findViewById(R.id.schedule_activity_datePicker);

                    break;
            }
            convertView.setTag(childHolder);
        } else {
            switch (rowType) {
                case TIME:
                    childHolder = (TimeChild) convertView.getTag();
                    break;
                case DATE:
                    childHolder = (DateChild) convertView.getTag();
                    break;
            }
        }

        if (childHolder instanceof TimeChild) {
            TimeChild timeChild = (TimeChild) childHolder;
            timeChild.mTimePicker.setCurrentHour(item.getFormattedStartDate().getHours());
            timeChild.mTimePicker.setCurrentMinute(item.getFormattedStartDate().getMinutes());
            timeChild.mTimePicker.setOnTimeChangedListener(new OnTimeChangedListener(groupPosition, childPosition));
        } else if (childHolder instanceof DateChild) {
            DateChild dateChild = (DateChild) childHolder;
            dateChild.mDatePicker.setMinDate(Calendar.getInstance().getTimeInMillis() - TEN_SECONDS);
            dateChild.mDatePicker.init(item.getFormattedStartDate().getYear(),
                    item.getFormattedStartDate().getMonth(), item.getFormattedStartDate().getDay(),
                    new OnDateSetListener(groupPosition, childPosition));
        }
        return convertView;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return mScheduleListItems.get(groupPosition).getScheduleListItems().size();
    }

    @Override
    public int getGroupCount() {
        return mScheduleListItems.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mScheduleListItems.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mScheduleListItems.get(groupPosition).getScheduleListItems().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        ScheduleListHeader header = mScheduleListItems.get(groupPosition);
        ScheduleListHeader.RowType rowType = header.getRowType();
        BaseGroup groupHolder = new BaseGroup();
        if (convertView == null) {
            switch (rowType) {
                case TITLE:
                    convertView = mLayoutInflater.inflate(R.layout.schedule_title_layout, parent, false);
                    groupHolder.mTitleAlone = (TextView) convertView.findViewById(R.id.schedule_group_titleText);
                    groupHolder.mTitleAlone.setTypeface(SonarCloudApp.avenirMedium);
                    break;
                default:
                    convertView = mLayoutInflater.inflate(R.layout.schedule_group_item, parent, false);
                    groupHolder.mTitle = (TextView) convertView.findViewById(R.id.schedule_header_itemTitle);
                    groupHolder.mTitle.setTypeface(SonarCloudApp.avenirBook);
                    groupHolder.mSubtitle = (TextView) convertView.findViewById(R.id.schedule_header_itemSubtitle);
                    groupHolder.mSubtitle.setTypeface(SonarCloudApp.avenirBook);

            }
            convertView.setTag(groupHolder);
        } else {
            groupHolder = (BaseGroup) convertView.getTag();
        }

        switch (rowType) {
            case TITLE:
                groupHolder.mTitleAlone.setText(header.getTitle());
                convertView.setOnClickListener(null);
                break;
            default:
                groupHolder.mTitle.setText(header.getTitle());
                groupHolder.mSubtitle.setText(header.getSubtitle());
                break;
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public interface OnItemInteractionListener {
        void onTimeChanged(TimePicker picker, int hour, int minutes, int groupPos, int childPos);

        void onDateChanged(DatePicker picker, int day, int month, int year, int groupPos, int childPos);
    }

    class OnDateSetListener implements DatePicker.OnDateChangedListener {

        private int mGroupPosition;
        private int mChildPosition;

        public OnDateSetListener(int groupPos, int childPos) {
            mGroupPosition = groupPos;
            mChildPosition = childPos;
        }

        @Override
        public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            if (mOnItemInteractionListener != null) {
                mOnItemInteractionListener.onDateChanged(view, dayOfMonth, monthOfYear, year,
                        mGroupPosition, mChildPosition);
            }
        }
    }

    class OnTimeChangedListener implements TimePicker.OnTimeChangedListener {

        private int mGroupPosition;
        private int mChildPosition;

        public OnTimeChangedListener(int groupPos, int childPos) {
            mGroupPosition = groupPos;
            mChildPosition = childPos;
        }

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            Schedule schedule = mScheduleListItems.get(mGroupPosition).getScheduleListItems().get(mChildPosition);
            Date date = schedule.getFormattedStartDate();
            date.setHours(hourOfDay);
            date.setMinutes(minute);
            if (mOnItemInteractionListener != null) {
                mOnItemInteractionListener.onTimeChanged(view, hourOfDay, minute, mGroupPosition, mChildPosition);
            }
        }
    }

    public class BaseGroup {
        TextView mTitle;
        TextView mSubtitle;
        TextView mTitleAlone;
    }

    class BaseChild {
    }

    class TimeChild extends BaseChild {
        TimePicker mTimePicker;
    }

    class DateChild extends BaseChild {
        DatePicker mDatePicker;
    }
}
