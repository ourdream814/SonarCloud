package main.java.com.softrangers.sonarcloudmobile.utils;

/**
 * Created by mike on 15/12/16.
 */
public class AudioProcessor {
    public static final int OPUS_APPLICATION_VOIP = 2048;
    public static final int OPUS_APPLICATION_AUDIO = 2049;
    public static final int OPUS_APPLICATION_RESTRICTED_LOWDELAY = 2051;
    public static final int OPUS_SIGNAL_VOICE = 3001;
    public static final int OPUS_SIGNAL_MUSIC = 3002;

    public static final int OPUS_OK = 0;

    public static final int MAX_PACKET = 1500;

    public static final double frameSizeMilliseconds = 20;

    public long opusEncoder = 0;
    public long opusDecoder = 0;
    public long webRTC = 0;

    public int channels;

    private int frameSize;
    private int maxFrameSize;
    public int pcmBytes;
    public int bufferSize;

    static {
        System.loadLibrary("AudioProcessor");
    }

    private native int createEncoder(int sampleRate, int channels, int application);

    private native String opusStrError(int error);

    private native int createDecoder(int sampleRate, int channels);

    public native int setBitrate(int bitrate);

    public native int setSignal(int signal);

    public native int setComplexity(int complexity);

    private native int encode( byte[] pcm, int pcmOffset, int frameSize, byte[] payload, int payloadOffset );

    private native int decode( byte[] data, int dataOffset, int dataSize, int channels, int maxFrameSize, byte[] pcm, int pcmOffset );

    public native void dealloc();

    public static AudioProcessor encoder( int sampleRate, int channels, int application ) {
        // Get our C object
        AudioProcessor processor = new AudioProcessor();
        processor.initEncoder( sampleRate, channels, application );

        // Give the world back our processor
        return( processor );
    }

    public static AudioProcessor decoder( int sampleRate, int channels ) {
        // Construct ouru C guy
        AudioProcessor processor = new AudioProcessor();
        processor.initDecoder( sampleRate, channels );

        // Give back
        return( processor );
    }

    public void initEncoder( int sampleRate, int channels, int application ) {
        int result = createEncoder( sampleRate, channels, application );

        if( result!=OPUS_OK )
            throw new Error( "Error creating encoder. "+opusStrError( result ) );

        frameSize = frameSizeFromMilliseconds( sampleRate );
        bufferSize = MAX_PACKET;
        pcmBytes = frameSize*channels*2; // 2 is sizeof( opus_int16 )

        this.channels = channels;
    }

    public void initDecoder( int sampleRate, int channels ) {
        int result = createDecoder( sampleRate, channels );

        if( result!=OPUS_OK )
            throw new Error( "Error creating decoder. "+opusStrError( result ) );

        this.channels = channels;
        frameSize = frameSizeFromMilliseconds( sampleRate );
        maxFrameSize = frameSize*6;
        bufferSize = maxFrameSize*channels*2;
    }

    public int encodePCM( byte[] pcm, int pcmOffset, byte[] payload, int payloadOffset ) {
        if( payload.length-payloadOffset<bufferSize )
            throw new Error( "Payload writable bytes is less than recommended buffer size of "+bufferSize+"." );

        // Now encode
        int bytesWritten = encode( pcm, pcmOffset, frameSize, payload, payloadOffset );

        if( bytesWritten==0 )
            throw new Error( "Error encoding." );
        else if( bytesWritten<0 )
            throw new Error( "Error encoding. "+opusStrError( bytesWritten ) );

        // Now thing
        return( bytesWritten ); // Give back to life
    }

    public int decodePayload( byte[] payload, int payloadOffset, int payloadSize, byte[] pcm, int pcmOffset ) {
        if( pcm.length-pcmOffset<bufferSize )
            throw new Error( "PCM writable bytes is less than recommended buffer size of "+bufferSize+"." );

        // Now thing
        int bytesWritten = decode(
                payload,
                payloadOffset,
                payloadSize,
                channels,
                maxFrameSize,
                pcm,
                pcmOffset
        );

        if( bytesWritten==0 )
            throw new Error( "Error decoding." );
        else if( bytesWritten<0 )
            throw new Error( "Error decoding. "+opusStrError( bytesWritten ) );

        // Now thing
        return( bytesWritten ); // Send back o0ur love with a fruits basket
    }

    public int frameSizeFromMilliseconds( int sampleRate ) {
        if( frameSizeMilliseconds==2.5 )
            return( sampleRate/400 );
        else if( frameSizeMilliseconds==5 )
            return( sampleRate/200 );
        else if( frameSizeMilliseconds==10 )
            return( sampleRate/100 );
        else if( frameSizeMilliseconds==20 )
            return( sampleRate/50 );
        else if( frameSizeMilliseconds==40 )
            return( sampleRate/25 );
        else if( frameSizeMilliseconds==60 )
            return( 3*sampleRate/50 );

        throw new Error( "frameSizeMilliseconds not set to a supported value." );
    }

    // Be sexy
}
