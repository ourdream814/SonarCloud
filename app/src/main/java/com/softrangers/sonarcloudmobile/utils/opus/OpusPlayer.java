package com.softrangers.sonarcloudmobile.utils.opus;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.softrangers.sonarcloudmobile.models.Recording;
import com.softrangers.sonarcloudmobile.utils.AudioProcessor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by eduard on 4/2/16.
 */
public class OpusPlayer {

    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNEL = 1;
    private OnPlayListener mOnPlayListener;

    public void setOnPlayListener(OnPlayListener onPlayListener) {
        mOnPlayListener = onPlayListener;
    }

    public void play(final Recording recording, final int position) {
        if (recording.isPlaying()) {
            recording.setIsPlaying(false);
            if (mOnPlayListener != null) {
                mOnPlayListener.onStopPlayback(recording, position);
            }
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playAudio(recording, position);
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        }).start();
    }

    /**
     * Plays the given file by reading bytes from it
     *
     * @param recording which contains a file path
     * @param position  of the recording in the list, used to update UI
     */
    private void playAudio(Recording recording, int position) throws Exception {
        int channelOption;
        File file = new File(recording.getFilePath());
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        FileInputStream fis = new FileInputStream(new File(recording.getFilePath()));
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(bytes, 0, bytes.length);
        bis.close();
        channelOption = AudioFormat.CHANNEL_OUT_MONO;
        AudioProcessor audioProcessor;
        // Get the decoder
        audioProcessor = AudioProcessor.decoder(SAMPLE_RATE, CHANNEL);

        int minimumBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                channelOption,
                AudioFormat.ENCODING_PCM_16BIT
        );

        int payloadOffset = 0;

        byte[] pcmBytes = new byte[audioProcessor.bufferSize];
        byte[] buffer = null;

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, channelOption,
                AudioFormat.ENCODING_PCM_16BIT, minimumBufferSize, AudioTrack.MODE_STREAM);

        // Now figure our lives
        audioTrack.play();
        recording.setIsPlaying(true);
        if (mOnPlayListener != null) {
            mOnPlayListener.onStartPlayback(recording, position);
        }
        // Now decode in sequence
        while (recording.isPlaying()) {
            // Read more
            if (payloadOffset < bytes.length) {
                // Get the length
                ByteBuffer wrapped = ByteBuffer.wrap(bytes); // big-endian by default
                int payloadSize = wrapped.getInt(payloadOffset);

                // Did we run out?
                if (payloadSize < 0 || payloadSize > AudioProcessor.MAX_PACKET) {
                    break;
                }
                payloadOffset += 4;
                // Decode us
                int bytesWritten = audioProcessor.decodePayload(bytes, payloadOffset, payloadSize, pcmBytes, 0);
                // Now append
                if (buffer == null) {
                    buffer = Arrays.copyOf(pcmBytes, bytesWritten);
                } else {
                    byte[] aBuffer = new byte[buffer.length + bytesWritten];
                    System.arraycopy(buffer, 0, aBuffer, 0, buffer.length);
                    System.arraycopy(pcmBytes, 0, aBuffer, buffer.length, bytesWritten);
                    buffer = aBuffer; // Replace
                }
                // Offset more
                payloadOffset += payloadSize;
            }

            // Shall we dance?
            if (buffer == null || buffer.length == 0) {
                // We have finished apparently
                break;
            } else {
                // Check if we have enough for our buffer or if we are done
                if (buffer.length >= minimumBufferSize || payloadOffset >= bytes.length) {
                    // Write the audio data in full
                    audioTrack.write(buffer, 0, buffer.length);
                    // We are done with our buffer
                    buffer = null;
                } // Else fill more
            }
        }

        audioTrack.stop();
        audioTrack.release();
        recording.setIsPlaying(false);
        if (mOnPlayListener != null) {
            mOnPlayListener.onStopPlayback(recording, position);
        }
        // Get rid of this
        audioProcessor.dealloc();
    }

    public interface OnPlayListener {
        void onStartPlayback(Recording recording, int position);

        void onStopPlayback(Recording recording, int position);

        void onPlaybackError(Recording recording, int position);
    }
}
