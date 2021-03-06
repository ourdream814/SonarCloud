package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.softrangers.sonarcloudmobile.utils.RepeatingCheck;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by eduard on 3/20/16.
 */
public class Schedule implements Parcelable {
    private int scheduleID;
    private int recordingID;
    private int receiverID;
    private String startDate;
    private String endDate;
    private String minute;
    private String hour;
    private String day;
    private String month;
    private String wday;
    private String time;
    private boolean deleteAfter;
    private String created;
    private String modified;
    private Recording recording;
    private boolean isSelected;

    private RowType mRowType;
    private String mTitle;
    private String mSubtitle;
    private int mRepeatOption;

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
        recording = in.readParcelable(Recording.class.getClassLoader());
        isSelected = in.readByte() != 0;
        mRepeatOption = in.readInt();
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

    public Date getFormattedEndDate() {
        return getFormattedDate(endDate);
    }

    private Date getFormattedDate(String stringDate) {
        Date date = new Date();
        if (stringDate == null) return date;
        if (!stringDate.equalsIgnoreCase("null")) {
            if (stringDate.contains("00:00:00.000Z")) return null;
            String editedDate = stringDate.replace("T", " ");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
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
        try {
            JSONArray schedulesArray = response.getJSONArray("schedules");
            for (int i = 0; i < schedulesArray.length(); i++) {
                JSONObject object = schedulesArray.getJSONObject(i);
                Schedule schedule = new Schedule();
                schedule.setScheduleID(object.getInt("scheduleID"));
                schedule.setRecordingID(object.getInt("recordingID"));
                schedule.setReceiverID(object.getInt("receiverID"));
                schedule.setStartDate(object.getString("startDate"));
                schedule.setEndDate(object.getString("endDate"));
                schedule.setMinute(object.getString("minute"));
                schedule.setHour(object.getString("hour"));
                schedule.setDay(object.getString("day"));
                schedule.setMonth(object.getString("month"));
                schedule.setWday(object.getString("wday"));
                schedule.setTime(object.getString("time"));
                schedule.setDeleteAfter(object.getBoolean("deleteAfter"));
                schedule.setCreated(object.getString("created"));
                schedule.setModified(object.getString("modified"));
                schedule.setRecording(Recording.buildSingle(object.getJSONObject("recording")));
                schedule.setRepeatOption();
                schedule = RepeatingCheck.setRepeating(schedule, schedule.getRepeatOption());
                schedules.add(schedule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return schedules;
    }

    public static Schedule buildSingle(JSONObject response) {
        Schedule schedule = new Schedule();
        try {
            schedule.setScheduleID(response.getInt("scheduleID"));
            schedule.setRecordingID(response.getInt("recordingID"));
            schedule.setReceiverID(response.getInt("receiverID"));
            schedule.setStartDate(response.getString("startDate"));
            schedule.setEndDate(response.getString("endDate"));
            schedule.setMinute(response.getString("minute"));
            schedule.setHour(response.getString("hour"));
            schedule.setDay(response.getString("day"));
            schedule.setMonth(response.getString("month"));
            schedule.setWday(response.getString("wday"));
            schedule.setTime(response.getString("time"));
            schedule.setDeleteAfter(response.getBoolean("deleteAfter"));
            schedule.setCreated(response.getString("created"));
            schedule.setModified(response.getString("modified"));
            schedule.setRecording(Recording.buildSingle(response.getJSONObject("recording")));
            schedule.setRepeatOption();
            schedule = RepeatingCheck.setRepeating(schedule, schedule.getRepeatOption());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return schedule;
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
        dest.writeParcelable(recording, flags);
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeInt(mRepeatOption);
    }
}
