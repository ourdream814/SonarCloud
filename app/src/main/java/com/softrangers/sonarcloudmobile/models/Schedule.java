package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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

    public Schedule() {}

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

    public Date getFormattedStartDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault());
        Date date = new Date();
        try {
            date = dateFormat.parse(startDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
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
                schedules.add(schedule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return schedules;
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
    }
}
