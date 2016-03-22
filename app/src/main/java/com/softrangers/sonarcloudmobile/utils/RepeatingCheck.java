package com.softrangers.sonarcloudmobile.utils;

import android.content.Context;

import com.softrangers.sonarcloudmobile.R;
import com.softrangers.sonarcloudmobile.models.Schedule;
import com.softrangers.sonarcloudmobile.utils.api.Api;

import java.util.Calendar;

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

    public static Schedule setRepeating(Schedule schedule, int repeatingOption) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(schedule.getFormattedStartDate().getTime());
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
