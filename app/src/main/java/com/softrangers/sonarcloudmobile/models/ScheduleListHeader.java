package com.softrangers.sonarcloudmobile.models;

import java.util.ArrayList;

/**
 * Created by Eduard Albu on 21 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class ScheduleListHeader {

    private RowType mRowType;
    private String mTitle;
    private String mSubtitle;
    private ArrayList<Schedule> mScheduleListItems;

    public ScheduleListHeader(RowType rowType) {
        mRowType = rowType;
    }

    public enum RowType {
        TITLE, DATE, TIME, REPEAT, REPEAT_UNTIL;

        public static int getIntRowType(RowType rowType) {
            switch (rowType) {
                case TITLE:
                    return 0;
                case DATE:
                    return 1;
                case TIME:
                    return 2;
                case REPEAT:
                    return 3;
                default:
                    return 4;
            }
        }
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getSubtitle() {
        return mSubtitle;
    }

    public void setSubtitle(String subtitle) {
        mSubtitle = subtitle;
    }

    public ArrayList<Schedule> getScheduleListItems() {
        return mScheduleListItems;
    }

    public void setScheduleListItems(ArrayList<Schedule> scheduleListItems) {
        mScheduleListItems = scheduleListItems;
    }

    public RowType getRowType() {
        return mRowType;
    }
}
