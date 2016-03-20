package com.softrangers.sonarcloudmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Formatter;

/**
 * Created by eduard on 3/20/16.
 */
public class Recording implements Parcelable {

    private int mRecordingId;
    private boolean mSendComplete;
    private int mBitrate;
    private int mSampleRate;
    private int mChannels;
    private String mFormat;
    private boolean mPlayImmediately;
    private boolean mKeep;
    private String mCreated;
    private String mModified;
    private int mLength;
    private boolean mIsPlaying;

    public Recording() {}

    protected Recording(Parcel in) {
        mRecordingId = in.readInt();
        mSendComplete = in.readByte() != 0;
        mBitrate = in.readInt();
        mSampleRate = in.readInt();
        mChannels = in.readInt();
        mFormat = in.readString();
        mPlayImmediately = in.readByte() != 0;
        mKeep = in.readByte() != 0;
        mCreated = in.readString();
        mModified = in.readString();
        mLength = in.readInt();
        mIsPlaying = in.readByte() != 0;
    }

    public static final Creator<Recording> CREATOR = new Creator<Recording>() {
        @Override
        public Recording createFromParcel(Parcel in) {
            return new Recording(in);
        }

        @Override
        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };

    public int getRecordingId() {
        return mRecordingId;
    }

    public void setRecordingId(int recordingId) {
        mRecordingId = recordingId;
    }

    public boolean isSendComplete() {
        return mSendComplete;
    }

    public void setSendComplete(boolean sendComplete) {
        mSendComplete = sendComplete;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public void setBitrate(int bitrate) {
        mBitrate = bitrate;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }

    public int getChannels() {
        return mChannels;
    }

    public void setChannels(int channels) {
        mChannels = channels;
    }

    public String getFormat() {
        return mFormat;
    }

    public void setFormat(String format) {
        mFormat = format;
    }

    public boolean isPlayImmediately() {
        return mPlayImmediately;
    }

    public void setPlayImmediately(boolean playImmediately) {
        mPlayImmediately = playImmediately;
    }

    public boolean isKeep() {
        return mKeep;
    }

    public void setKeep(boolean keep) {
        mKeep = keep;
    }

    public String getCreated() {
        return mCreated;
    }

    public void setCreated(String created) {
        mCreated = created;
    }

    public String getModified() {
        return mModified;
    }

    public void setModified(String modified) {
        mModified = modified;
    }

    public int getLength() {
        return mLength;
    }

    public void setLength(int length) {
        mLength = length;
    }

    public String getFromatedLength() {
        return stringForTime(mLength);
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public void setIsPlaying(boolean isPlaying) {
        mIsPlaying = isPlaying;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Recording) {
            Recording r = (Recording) o;
            if (this.getRecordingId() == r.getRecordingId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * It will convert the given time (in milliseconds) into a user friendly string
     *
     * @param timeSeconds time in seconds
     * @return the user friendly time string
     */
    private String stringForTime(int timeSeconds) {
        StringBuilder formatBuilder = new StringBuilder();
//        int totalSeconds = timeMs / 1000;

        int seconds = timeSeconds % 60;
        int minutes = (timeSeconds / 60) % 60;
        int hours = timeSeconds / 3600;

        formatBuilder.setLength(0);
        if (hours > 0) {
            return new Formatter().format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return new Formatter().format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public static ArrayList<Recording> build(JSONObject response) {
        ArrayList<Recording> recordings = new ArrayList<>();
        try {
            JSONArray recordingsArray = response.getJSONArray("recordings");
            for (int i = 0; i < recordingsArray.length(); i++) {
                JSONObject record = recordingsArray.getJSONObject(i);
                Recording recording = new Recording();
                recording.setRecordingId(record.getInt("recordingID"));
                recording.setSendComplete(record.getBoolean("sendingComplete"));
                recording.setBitrate(record.getInt("bitrate"));
                recording.setSampleRate(record.getInt("samplerate"));
                recording.setChannels(record.getInt("channels"));
                recording.setFormat(record.getString("format"));
                recording.setPlayImmediately(record.getBoolean("playImmediately"));
                recording.setKeep(record.getBoolean("keep"));
                recording.setCreated(record.getString("created"));
                recording.setModified(record.getString("modified"));
                recording.setLength(record.getInt("length"));
                recordings.add(recording);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recordings;
    }

    public static Recording buildSingle(JSONObject record) {
        Recording recording = new Recording();
        try {
            recording.setRecordingId(record.getInt("recordingID"));
            recording.setSendComplete(record.getBoolean("sendingComplete"));
            recording.setBitrate(record.getInt("bitrate"));
            recording.setSampleRate(record.getInt("samplerate"));
            recording.setChannels(record.getInt("channels"));
            recording.setFormat(record.getString("format"));
            recording.setPlayImmediately(record.getBoolean("playImmediately"));
            recording.setKeep(record.getBoolean("keep"));
            recording.setCreated(record.getString("created"));
            recording.setModified(record.getString("modified"));
            recording.setLength(record.getInt("length"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recording;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mRecordingId);
        dest.writeByte((byte) (mSendComplete ? 1 : 0));
        dest.writeInt(mBitrate);
        dest.writeInt(mSampleRate);
        dest.writeInt(mChannels);
        dest.writeString(mFormat);
        dest.writeByte((byte) (mPlayImmediately ? 1 : 0));
        dest.writeByte((byte) (mKeep ? 1 : 0));
        dest.writeString(mCreated);
        dest.writeString(mModified);
        dest.writeInt(mLength);
        dest.writeByte((byte) (mIsPlaying ? 1 : 0));
    }
}
