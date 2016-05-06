package com.softrangers.sonarcloudmobile.utils;

import android.content.Context;
import android.util.Log;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.utils.api.Api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Eduard Albu on 22 03 2016
 * project sonarcloud-android
 *
 * @author eduard.albu@gmail.com
 */
public class RepeatingCheck {

    private static final String ALL = "*";
    private static final String NOT_SET = "null";

    public static int checkRepeating(String minute, String hour, String day, String month, String weekDay) {
        if (minute == null || hour == null || day == null || month == null || weekDay == null)
            return 0;
        if (!minute.equals(ALL) && !minute.equals(NOT_SET) && hour.equals(ALL)
                && day.equals(ALL) && month.equals(ALL) && weekDay.equals(ALL)) {
            return 1;
        }
        if (!minute.equals(ALL) && !minute.equals(NOT_SET) && !hour.equals(ALL) && !hour.equals(NOT_SET)
                && day.equals(ALL) && month.equals(ALL) && weekDay.equals(ALL)) {
            return 2;
        }
        if (!weekDay.equals(ALL) && !weekDay.equals(NOT_SET)) return 3;
        if (!day.equals(ALL) && !weekDay.equals(NOT_SET)) return 4;
        if (!month.equals(ALL) && !month.equals(NOT_SET)) return 5;

        return 0;
    }

    public static Schedule setRepeating(Schedule schedule, int repeatingOption) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.US);
        SimpleDateFormat secondFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getDefault());
        String date = schedule.getStartDate() == null || schedule.getStartDate().equals("null") ? dateFormat.format(new Date(calendar.getTimeInMillis())) : schedule.getStartDate();
        try {
            calendar.setTimeInMillis(dateFormat.parse(date).getTime());
        } catch (Exception e) {
            calendar.setTimeInMillis(secondFormat.parse(date).getTime());
            Log.e("RepeatingCheck", e.getMessage());
        }
        switch (repeatingOption) {
            case 1:
                schedule.setMinute(String.valueOf(calendar.get(Calendar.MINUTE)));
                schedule.setHour(ALL);
                schedule.setDay(ALL);
                schedule.setMonth(ALL);
                schedule.setWday(ALL);
                return schedule;
            case 2:
                schedule.setMinute(String.valueOf(calendar.get(Calendar.MINUTE)));
                schedule.setHour(String.valueOf(calendar.get(Calendar.HOUR)));
                schedule.setDay(ALL);
                schedule.setMonth(ALL);
                schedule.setWday(ALL);
                return schedule;
            case 3:
                schedule.setMinute(ALL);
                schedule.setHour(ALL);
                schedule.setDay(ALL);
                schedule.setMonth(ALL);
                schedule.setWday(String.valueOf(calendar.get(Calendar.DAY_OF_WEEK)));
                return schedule;
            case 4:
                schedule.setMinute(ALL);
                schedule.setHour(ALL);
                schedule.setDay(ALL);
                schedule.setMonth(String.valueOf(calendar.get(Calendar.MONTH)));
                schedule.setWday(ALL);
                return schedule;
            default:
                schedule.setMinute(null);
                schedule.setHour(null);
                schedule.setDay(null);
                schedule.setWday(null);
                schedule.setMonth(null);
                return schedule;
        }
    }
}
