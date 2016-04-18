package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.softrangers.sonarcloudmobile.utils.RepeatingCheck;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by eduard on 3/20/16.
 */
public class Schedule implements Parcelable, Cloneable {
    protected int scheduleID;
    protected int recordingID;
    protected int receiverID;
    protected String startDate;
    protected Date scheduleStartDate;
    protected String endDate;
    protected Date scheduleEndDate;
    protected String minute;
    protected String hour;
    protected String day;
    protected String month;
    protected String wday;
    protected String time;
    protected Date scheduleTime;
    protected boolean deleteAfter;
    protected String created;
    protected String modified;
    protected String cronExpression;
    protected Recording recording;
    protected boolean isSelected;
    protected Date scheduleDate;
    protected Date comparatorDate;
    protected RowType mRowType;
    protected String mTitle;
    protected String mSubtitle;
    protected int mRepeatOption;

    public Schedule() {
    }

    protected Schedule(Parcel in) {
        scheduleID = in.readInt();
        recordingID = in.readInt();
        receiverID = in.readInt();
        startDate = in.readString();
        endDate = in.readString();
        minute = in.readString();
        hour = in.readString();
        day = in.readString();
        month = in.readString();
        wday = in.readString();
        time = in.readString();
        deleteAfter = in.readByte() != 0;
        created = in.readString();
        modified = in.readString();
        cronExpression = in.readString();
        recording = in.readParcelable(Recording.class.getClassLoader());
        isSelected = in.readByte() != 0;
        mTitle = in.readString();
        mSubtitle = in.readString();
        mRepeatOption = in.readInt();
        scheduleStartDate = (Date) in.readSerializable();
        scheduleEndDate = (Date) in.readSerializable();
        scheduleTime = (Date) in.readSerializable();
        scheduleDate = (Date) in.readSerializable();
    }

    public static final Creator<Schedule> CREATOR = new Creator<Schedule>() {
        @Override
        public Schedule createFromParcel(Parcel in) {
            return new Schedule(in);
        }

        @Override
        public Schedule[] newArray(int size) {
            return new Schedule[size];
        }
    };

    public int getRepeatOption() {
        return mRepeatOption;
    }

    public void setRepeatOption() {
        mRepeatOption = RepeatingCheck.checkRepeating(minute, hour, day, month, wday);
    }

    public int getScheduleID() {
        return scheduleID;
    }

    public void setScheduleID(int scheduleID) {
        this.scheduleID = scheduleID;
    }

    public int getRecordingID() {
        return recordingID;
    }

    public void setRecordingID(int recordingID) {
        this.recordingID = recordingID;
    }

    public int getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(int receiverID) {
        this.receiverID = receiverID;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getServerFormatDate(Date date) {
        SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.getDefault());
        serverFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return serverFormat.format(date);
    }

    public Date getFormattedStartDate() {
        if (startDate == null || startDate.equalsIgnoreCase("null")) {
            return getFormattedDate(time);
        } else {
            return getFormattedDate(startDate);
        }
    }

    public Date getFormattedTime() {
        if (time != null && !time.equals("null")) {
            return getFormattedDate(time);
        } else return null;
    }

    public Date getFormattedEndDate() {
        return getFormattedDate(endDate);
    }

    public Date getFormattedDate(String stringDate) {
        Date date = new Date();
        if (stringDate == null) return date;
        if (!stringDate.equalsIgnoreCase("null")) {
            String editedDate = stringDate.replace("T", " ");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.getDefault());
            try {
                date = dateFormat.parse(editedDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return date;
    }

    public String getStringDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("cccc, MMM dd, yyyy", Locale.getDefault());
        return format.format(date);
    }

    public String getStringTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("hh mm a", Locale.getDefault());
        return format.format(date);
    }

    public String getStringHour(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("hh", Locale.getDefault());
        return format.format(date);
    }

    public String getStringMinute(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("mm", Locale.getDefault());
        StringBuilder builder = new StringBuilder();
        builder.append(format.format(date));
        builder.append("\n");
        SimpleDateFormat formatAmPm = new SimpleDateFormat("a", Locale.getDefault());
        builder.append(formatAmPm.format(date));
        return builder.toString();
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getWday() {
        return wday;
    }

    public void setWday(String wday) {
        this.wday = wday;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isDeleteAfter() {
        return deleteAfter;
    }

    public void setDeleteAfter(boolean deleteAfter) {
        this.deleteAfter = deleteAfter;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public Recording getRecording() {
        return recording;
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(scheduleID);
        dest.writeInt(recordingID);
        dest.writeInt(receiverID);
        dest.writeString(startDate);
        dest.writeString(endDate);
        dest.writeString(minute);
        dest.writeString(hour);
        dest.writeString(day);
        dest.writeString(month);
        dest.writeString(wday);
        dest.writeString(time);
        dest.writeByte((byte) (deleteAfter ? 1 : 0));
        dest.writeString(created);
        dest.writeString(modified);
        dest.writeString(cronExpression);
        dest.writeParcelable(recording, flags);
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeString(mTitle);
        dest.writeString(mSubtitle);
        dest.writeInt(mRepeatOption);
        dest.writeSerializable(scheduleStartDate);
        dest.writeSerializable(scheduleEndDate);
        dest.writeSerializable(scheduleTime);
        dest.writeSerializable(scheduleDate);
    }

    public enum RowType {
        TITLE, ITEM, NONE;

        public static int getIntRowType(RowType rowType) {
            switch (rowType) {
                case TITLE:
                    return 1;
                case ITEM:
                    return 2;
                default:
                    return 0;
            }
        }

        public static RowType getRowType(int intRowType) {
            switch (intRowType) {
                case 1:
                    return TITLE;
                case 2:
                    return ITEM;
                default:
                    return NONE;
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

    public RowType getRowType() {
        return mRowType;
    }

    public void setRowType(RowType rowType) {
        mRowType = rowType;
    }

    public Date getScheduleStartDate() {
        return scheduleStartDate;
    }

    public void setScheduleStartDate(Date scheduleStartDate) {
        this.scheduleStartDate = scheduleStartDate;
    }

    public Date getScheduleEndDate() {
        return scheduleEndDate;
    }

    public void setScheduleEndDate(Date scheduleEndDate) {
        this.scheduleEndDate = scheduleEndDate;
    }

    public Date getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(Date scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Date getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(Date scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public Date getComparatorDate() {
        return comparatorDate;
    }

    public void setComparatorDate(Date comparatorDate) {
        this.comparatorDate = comparatorDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Schedule) {
            Schedule s = (Schedule) o;
            if (this.getScheduleID() == s.getScheduleID()) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<Schedule> build(JSONObject response) {
        ArrayList<Schedule> schedules = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            JSONArray schedulesArray = response.getJSONArray("schedules");
            for (int i = 0; i < schedulesArray.length(); i++) {
                JSONObject object = schedulesArray.getJSONObject(i);
                Schedule schedule = new Schedule();
                schedule.setScheduleID(object.getInt("scheduleID"));
                schedule.setRecordingID(object.getInt("recordingID"));
                schedule.setReceiverID(object.getInt("receiverID"));

                schedule.setStartDate(object.getString("startDate"));
                if (schedule.startDate != null && !schedule.startDate.equals("null"))
                    schedule.setScheduleStartDate(dateFormat.parse(schedule.startDate));

                schedule.setEndDate(object.getString("endDate"));
                if (schedule.endDate != null && !schedule.endDate.equals("null"))
                    schedule.setScheduleEndDate(dateFormat.parse(schedule.endDate));

                schedule.setMinute(object.getString("minute"));
                schedule.setHour(object.getString("hour"));
                schedule.setDay(object.getString("day"));
                schedule.setMonth(object.getString("month"));
                schedule.setWday(object.getString("wday"));

                schedule.setTime(object.getString("time"));
                if (schedule.isOneTimeSchedule())
                    schedule.setScheduleTime(dateFormat.parse(schedule.time));

                schedule.setDeleteAfter(object.getBoolean("deleteAfter"));
                schedule.setCreated(object.getString("created"));
                schedule.setModified(object.getString("modified"));
                schedule.setRecording(Recording.buildSingle(object.optJSONObject("recording")));
                schedule.setRepeatOption();

                if (schedule.time == null || schedule.time.equals("null")) {
                    schedule.parseCronJob();
                }

                schedules.add(schedule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return schedules;
    }

    public static Schedule buildSingle(JSONObject object) {
        Schedule schedule = new Schedule();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.US);
        SimpleDateFormat secondFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        try {
            schedule.setScheduleID(object.getInt("scheduleID"));
            schedule.setRecordingID(object.getInt("recordingID"));
            schedule.setReceiverID(object.getInt("receiverID"));

            schedule.setStartDate(object.getString("startDate"));
            if (schedule.startDate != null && !schedule.startDate.equals("null")) {
                try {
                    schedule.setScheduleStartDate(dateFormat.parse(schedule.startDate));
                } catch (Exception e) {
                    schedule.setScheduleStartDate(secondFormat.parse(schedule.startDate));
                }
            }

            schedule.setEndDate(object.optString("endDate", "null"));
            if (schedule.endDate != null && !schedule.endDate.equals("null")) {
                try {
                    schedule.setScheduleEndDate(dateFormat.parse(schedule.endDate));
                } catch (Exception e) {
                    schedule.setScheduleEndDate(secondFormat.parse(schedule.endDate));
                }
            }

            schedule.setMinute(object.getString("minute"));
            schedule.setHour(object.getString("hour"));
            schedule.setDay(object.getString("day"));
            schedule.setMonth(object.getString("month"));
            schedule.setWday(object.getString("wday"));

            schedule.setTime(object.optString("time", "null"));
            if (schedule.isOneTimeSchedule() && schedule.getTime().equals("null"))
                schedule.setScheduleTime(dateFormat.parse(schedule.time));

            schedule.setDeleteAfter(object.getBoolean("deleteAfter"));
            schedule.setCreated(object.getString("created"));
            schedule.setModified(object.getString("modified"));
            schedule.setRecording(Recording.buildSingle(object.optJSONObject("recording")));
            schedule.setRepeatOption();

            if (schedule.time == null || schedule.time.equals("null")) {
                schedule.parseCronJob();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return schedule;
    }

    private CronDefinition mCronDefinition = CronDefinitionBuilder.defineCron().withMinutes().and().withHours()
            .and().withDayOfMonth().and().withMonth().and().withDayOfWeek().and().instance();

    private CronParser mCronParser = new CronParser(mCronDefinition);


    public void parseCronJob() {
        Cron mCron = mCronParser.parse(minute + " " + hour + " " + day
                + " " + month + " " + wday);
        setCronExpression(mCron.asString());
    }

    public boolean isOneTimeSchedule() {
        return time != null && !time.equals("null");
    }

    public static void sortAllSchedules(Day day, Schedule s, int repeatOption) {
        ArrayList<Schedule> repeatedSchedules = new ArrayList<>();
        try {
            if (s.isOneTimeSchedule()) {
                repeatOption = 0;
            }
            boolean isExpirated = s.scheduleEndDate != null && s.scheduleEndDate.compareTo(day.getDate()) < 0;
            switch (repeatOption) {
                case 0: {
                    Schedule rs = (Schedule) s.clone();
                    rs.mRepeatOption = repeatOption;
                    int scheduleYear = rs.scheduleTime.getYear();
                    int scheduleMonth = rs.scheduleTime.getMonth();
                    int scheduleDate = rs.scheduleTime.getDate();
                    int dayYear = day.getDate().getYear();
                    int dayMonth = day.getDate().getMonth();
                    int dayDate = day.getDate().getDate();
                    boolean isAvailable = scheduleYear == dayYear &&
                            scheduleMonth == dayMonth && scheduleDate == dayDate && !rs.time.contains("00:00:00.000Z");
                    if (isAvailable) {
                        rs.comparatorDate = rs.scheduleTime;
                        repeatedSchedules.add(rs);
                    }
                    break;
                }
                case 1: {
                    boolean isAvailable = s.scheduleStartDate != null && s.scheduleStartDate.compareTo(day.getDate()) <= 0;
                    if (isAvailable && !isExpirated) {
                        for (int i = 1; i < 25; i++) {
                            Schedule rs = (Schedule) s.clone();
                            rs.mRepeatOption = repeatOption;
                            rs.scheduleDate = (Date) day.getDate().clone();
                            rs.scheduleDate.setHours(i);
                            rs.scheduleDate.setMinutes(Integer.parseInt(rs.minute));
                            rs.comparatorDate = rs.scheduleDate;
                            repeatedSchedules.add(rs);
                        }
                    }
                    break;
                }
                case 2: {
                    Schedule rs = (Schedule) s.clone();
                    rs.mRepeatOption = repeatOption;
                    rs.scheduleDate = (Date) day.getDate().clone();
                    rs.scheduleDate.setHours(Integer.parseInt(rs.getHour()));
                    rs.scheduleDate.setMinutes(
                            rs.getMinute().equals("*") ? 0 : Integer.parseInt(rs.getMinute())
                    );

                    boolean isAvailable = rs.scheduleStartDate != null && rs.scheduleStartDate.compareTo(day.getDate()) <= 0;
                    if (isAvailable && !isExpirated) {
                        rs.comparatorDate = rs.scheduleDate;
                        repeatedSchedules.add(rs);
                    }
                    break;
                }
                case 3: {
                    Schedule rs = (Schedule) s.clone();
                    rs.mRepeatOption = repeatOption;
                    rs.scheduleDate = (Date) day.getDate().clone();
                    rs.scheduleDate.setHours(
                            rs.hour.equals("*") ? 0 : Integer.parseInt(rs.hour)
                    );
                    rs.scheduleDate.setMinutes(
                            rs.getMinute().equals("*") ? 0 : Integer.parseInt(rs.getMinute())
                    );
                    int scheduleDayOfWeek = Integer.parseInt(rs.getWday());
                    int dayOfWeek = day.getDate().getDay();
                    boolean isAvailable = scheduleDayOfWeek == dayOfWeek &&
                            rs.scheduleStartDate.compareTo(day.getDate()) <= 0 && !isExpirated;

                    if (isAvailable && !isExpirated) {
                        rs.comparatorDate = rs.scheduleDate;
                        repeatedSchedules.add(rs);
                    }
                    break;
                }
                case 4: {
                    Schedule rs = (Schedule) s.clone();
                    rs.mRepeatOption = repeatOption;
                    rs.scheduleDate = (Date) day.getDate().clone();
                    rs.scheduleDate.setHours(
                            rs.hour.equals("*") ? 0 : Integer.parseInt(rs.hour)
                    );
                    rs.scheduleDate.setMinutes(
                            rs.getMinute().equals("*") ? 0 : Integer.parseInt(rs.getMinute())
                    );
                    int scheduleDayOfMonth = Integer.parseInt(s.getDay());
                    int dayOfMonth = day.getDate().getDate();
                    boolean isAvailable = scheduleDayOfMonth == dayOfMonth &&
                            rs.scheduleStartDate.compareTo(day.getDate()) <= 0 && !isExpirated;

                    if (isAvailable && !isExpirated) {
                        rs.comparatorDate = rs.scheduleDate;
                        repeatedSchedules.add(rs);
                    }
                    break;
                }
                case 5: {
                    Schedule rs = (Schedule) s.clone();
                    rs.mRepeatOption = repeatOption;
                    rs.scheduleDate = (Date) day.getDate().clone();
                    rs.scheduleDate.setHours(
                            rs.hour.equals("*") ? 0 : Integer.parseInt(rs.hour)
                    );
                    rs.scheduleDate.setMinutes(
                            rs.getMinute().equals("*") ? 0 : Integer.parseInt(rs.getMinute())
                    );
                    int scheduleMonth = Integer.parseInt(rs.getMonth());
                    int month = day.getDate().getMonth();
                    boolean isAvailable = scheduleMonth == month &&
                            rs.scheduleStartDate.compareTo(day.getDate()) <= 0 && !isExpirated;
                    if (isAvailable && !isExpirated) {
                        rs.comparatorDate = rs.scheduleDate;
                        repeatedSchedules.add(rs);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        day.addSchedules(repeatedSchedules);
    }
}
