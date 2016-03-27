package com.softrangers.sonarcloudmobile.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Eduard Albu on 25 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class Day {

    private Date mDate;
    private String mMonthAndDay;
    private String mServerDate;
    private ArrayList<Schedule> mSchedules;
    private boolean isSelected;
    private Calendar mCalendar = Calendar.getInstance();

    public Day() {
        mSchedules = new ArrayList<>();
    }

    public static ArrayList<Day> getYearDays() {
        ArrayList<Day> dates = new ArrayList<>();
        for (int i = 1; i < 366; i++) {
            Day day = new Day();
            day.mCalendar.set(Calendar.DAY_OF_YEAR, i);
            day.mCalendar.set(Calendar.HOUR, 0);
            day.mCalendar.set(Calendar.MINUTE, 0);
            day.mCalendar.set(Calendar.SECOND, 0);
            day.mCalendar.set(Calendar.MILLISECOND, 0);
            Date date = new Date(day.mCalendar.getTimeInMillis());
            day.mDate = date;
            day.mMonthAndDay = day.getMonthAndDay(date);
            day.mServerDate = day.getServerDate(date);
            dates.add(day);
        }
        return dates;
    }

    private String getMonthAndDay(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return dateFormat.format(date);
    }

    private String getServerDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public Date getDate() {
        return mDate;
    }

    public String getMonthAndDay() {
        return mMonthAndDay;
    }

    public String getServerDate() {
        return mServerDate;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public ArrayList<Schedule> getSchedules() {
        Collections.sort(mSchedules, new Comparator<Schedule>() {
            @Override
            public int compare(Schedule lhs, Schedule rhs) {
                return lhs.getComparatorDate().compareTo(rhs.getComparatorDate());
            }
        });
        return mSchedules;
    }

    public void addSchedules(Schedule schedule) {
        mSchedules.add(schedule);
    }

    public void addSchedules(ArrayList<Schedule> schedules) {
//        mSchedules.clear();
        mSchedules.addAll(schedules);
    }

    public void clearSchedules() {
        mSchedules.clear();
    }
}
