package com.softrangers.sonarcloudmobile.utils.opus;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.Nullable;

import com.softrangers.sonarcloudmobile.utils.AudioProcessor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLSocket;

/**
 * Created by eduard on 4/2/16.
 *
 */
public class OpusRecorder {

    private static RecorderState recorderState;
    public static final int SAMPLE_RATE = 48000;
    private static final int CHANNEL = 1;
    private OnRecordListener mOnRecordListener;

    public OpusRecorder() {
        recorderState = RecorderState.STOPPED;
    }

    public void setOnRecordListener(OnRecordListener onRecordListener) {
        mOnRecordListener = onRecordListener;
    }

    public void startRecording() {
        if (recorderState == RecorderState.RECORDING || recorderState == RecorderState.STREAMING
                || recorderState == RecorderState.PAUSED) {
            stopRecording();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                recordAudio(null);
            }
        }).start();
    }

    public void startStreaming(final SSLSocket sslSocket) {
        if (recorderState == RecorderState.RECORDING || recorderState == RecorderState.STREAMING
                || recorderState == RecorderState.PAUSED) {
            stopRecording();
        }
        recorderState = RecorderState.STREAMING;
        new Thread(new Runnable() {
            @Override
            public void run() {
                recordAudio(sslSocket);
            }
        }).start();
    }

    /**
     * Start recording the audio from device microphone
     */
    private void recordAudio(@Nullable SSLSocket sslSocket) {
        try {
            // We'll be throwing stuff here
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int channelOption = AudioFormat.CHANNEL_IN_MONO;

            int bufferSizeInBytes;

            // Get our opus recorder
            AudioProcessor audioProcessor = AudioProcessor.encoder(SAMPLE_RATE, CHANNEL, AudioProcessor.OPUS_APPLICATION_VOIP);

            // The buffer
            byte[] recordBuffer = new byte[audioProcessor.pcmBytes];
            byte[] encodeBuffer = new byte[audioProcessor.bufferSize];

            bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelOption, AudioFormat.ENCODING_PCM_16BIT);

            AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, channelOption,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
            audioRecorder.startRecording();
            if (recorderState != RecorderState.STREAMING) {
                recorderState = RecorderState.RECORDING;
            }
            if (mOnRecordListener != null) {
                mOnRecordListener.onRecordStarted(recorderState);
            }

            OutputStream serverStream = null;
            if (sslSocket != null && !sslSocket.isClosed()) {
                serverStream = sslSocket.getOutputStream();
            }
            while (recorderState == RecorderState.RECORDING || recorderState == RecorderState.STREAMING) {
                audioRecorder.read(recordBuffer, 0, recordBuffer.length);
                // Encode
                int bytesWritten = audioProcessor.encodePCM(recordBuffer, 0, encodeBuffer, 0);
                outputStream.write(encodeBuffer, 0, bytesWritten);
                if (serverStream != null && !sslSocket.isClosed()) {
                    serverStream.write(encodeBuffer, 0, bytesWritten);
                }
            }
            // Stop the recorder
            audioRecorder.stop();
            audioRecorder.release();

            // Get rid
            audioProcessor.dealloc();

            if (recorderState == RecorderState.PAUSED) {
                if (mOnRecordListener != null) {
                    mOnRecordListener.onRecordPaused(outputStream.toByteArray(), recorderState);
                }
            } else if (recorderState == RecorderState.STOPPED) {
                if (mOnRecordListener != null) {
                    mOnRecordListener.onRecordFinished(outputStream.toByteArray(), recorderState);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            recorderState = RecorderState.STOPPED;
            if (mOnRecordListener != null) {
                mOnRecordListener.onRecordFailed(e, recorderState);
            }
        }
    }

    /**
     * Stop the recording process if there are any
     */
    public void stopRecording() {
        if (recorderState == RecorderState.PAUSED) {
            if (mOnRecordListener != null) {
                mOnRecordListener.onRecordFinished(new byte[0], RecorderState.STOPPED);
            }
        }
        recorderState = RecorderState.STOPPED;
    }

    public void pauseRecording() {
        recorderState = RecorderState.PAUSED;
    }

    public enum RecorderState {
        RECORDING, RESUME_RECORDING, STOPPED, PAUSED, STREAMING
    }

    public boolean isRecording() {
        return recorderState != RecorderState.STOPPED;
    }

    public boolean isPaused() {
        return recorderState == RecorderState.PAUSED;
    }

    public RecorderState getRecorderState() {
        return recorderState;
    }

    public interface OnRecordListener {
        void onRecordStarted(RecorderState recorderState);

        void onRecordPaused(byte[] audioData, RecorderState recorderState);

        void onRecordFinished(byte[] audioData, RecorderState recorderState);

        void onRecordFailed(Exception e, RecorderState recorderState);
    }
}
