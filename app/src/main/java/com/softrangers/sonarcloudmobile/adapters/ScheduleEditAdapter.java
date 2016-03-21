package com.softrangers.sonarcloudmobile.adapters;

import android.content.Context;
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

/**
 * Created by Eduard Albu on 21 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class ScheduleEditAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {

    private static final long TEN_SECONDS = 10000;
    private ArrayList<ScheduleListHeader> mScheduleListItems;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private OnItemInteractionListener mOnItemInteractionListener;

    public ScheduleEditAdapter(ArrayList<ScheduleListHeader> scheduleListItems, Context context) {
        mScheduleListItems = scheduleListItems;
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
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
        View row = convertView;
        Schedule item = mScheduleListItems.get(groupPosition).getScheduleListItems().get(childPosition);
        ScheduleListHeader.RowType rowType = mScheduleListItems.get(groupPosition).getRowType();
        switch (rowType) {
            case TIME:
                TimePickerChild timePickerChild = new TimePickerChild();
                row = mLayoutInflater.inflate(R.layout.schedule_time_picker, parent, false);
                timePickerChild.mTimePicker = (TimePicker) row.findViewById(R.id.schedule_activity_timePicker);
                timePickerChild.mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                    @Override
                    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                        if (mOnItemInteractionListener != null) {
                            mOnItemInteractionListener.onTimeChanged(hourOfDay, minute);
                        }
                    }
                });
                timePickerChild.mTimePicker.setCurrentHour(item.getFormattedStartDate().getHours());
                timePickerChild.mTimePicker.setCurrentMinute(item.getFormattedStartDate().getMinutes());
                break;
            case DATE:
                DatePickerChild datePickerChild = new DatePickerChild();
                row = mLayoutInflater.inflate(R.layout.schedule_date_picker, parent, false);
                datePickerChild.mDatePicker = (DatePicker) row.findViewById(R.id.schedule_activity_datePicker);
                datePickerChild.mDatePicker.setMinDate(Calendar.getInstance().getTimeInMillis() - TEN_SECONDS);
                datePickerChild.mDatePicker.init(item.getFormattedStartDate().getYear(),
                        item.getFormattedStartDate().getMonth(), item.getFormattedStartDate().getDay(),
                        new DatePicker.OnDateChangedListener() {
                            @Override
                            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                if (mOnItemInteractionListener != null) {
                                    mOnItemInteractionListener.onDateChanged(dayOfMonth, monthOfYear, year);
                                }
                            }
                        });
        }
        return row;
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
        View row = convertView;
        switch (rowType) {
            case TITLE:
                TitleGroup titleGroup = new TitleGroup();
                row = mLayoutInflater.inflate(R.layout.schedule_title_layout, parent, false);
                titleGroup.mTitle = (TextView) row.findViewById(R.id.schedule_group_titleText);
                titleGroup.mTitle.setTypeface(SonarCloudApp.avenirMedium);
                titleGroup.mTitle.setText(header.getTitle());
                row.setOnClickListener(null);
                break;
            default:
                ChildHolderGroup childHolderGroup = new ChildHolderGroup();
                row = mLayoutInflater.inflate(R.layout.schedule_group_item, parent, false);
                childHolderGroup.mTitle = (TextView) row.findViewById(R.id.schedule_header_itemTitle);
                childHolderGroup.mTitle.setTypeface(SonarCloudApp.avenirBook);
                childHolderGroup.mSubtitle = (TextView) row.findViewById(R.id.schedule_header_itemSubtitle);
                childHolderGroup.mSubtitle.setTypeface(SonarCloudApp.avenirBook);

                childHolderGroup.mTitle.setText(header.getTitle());
                childHolderGroup.mSubtitle.setText(header.getSubtitle());
        }
        return row;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public interface OnItemInteractionListener {
        void onTimeChanged(int hour, int minutes);

        void onDateChanged(int day, int month, int year);
    }

    class TimePickerChild {
        TimePicker mTimePicker;
    }

    class DatePickerChild {
        DatePicker mDatePicker;
    }

    class RepeatChild {

    }

    class TitleGroup {
        TextView mTitle;
    }

    class ChildHolderGroup {
        TextView mTitle;
        TextView mSubtitle;
    }
}
