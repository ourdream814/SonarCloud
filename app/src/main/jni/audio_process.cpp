//
// Created by mike on 15/12/15.
//

#include <jni.h>
#include <stddef.h>
#include <android/log.h>
#include <malloc.h>
#include "../prebuild/include/opus/opus.h"

#define MAX_PACKET 1500

#define  LOG_TAG    "SO--Log"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void int_to_char(opus_uint32 i, unsigned char *ch) {
    ch[0] = i >> 24;
    ch[1] = (i >> 16) & 0xFF;
    ch[2] = (i >> 8) & 0xFF;
    ch[3] = i & 0xFF;
}

static opus_uint32 char_to_int(unsigned char *ch) {
    return (
            ((opus_uint32) ch[0] << 24) | ((opus_uint32) ch[1] << 16)
            | ((opus_uint32) ch[2] << 8) | (opus_uint32) ch[3]
    );
}

jfieldID getOpusEncoder(JNIEnv *env, jobject obj) {
    static jfieldID ptrFieldId = 0;

    if (!ptrFieldId) {
        jclass c = env->GetObjectClass(obj);
        ptrFieldId = env->GetFieldID(c, "opusEncoder", "J");
        env->DeleteLocalRef(c);
    }

    return (ptrFieldId);
}

jfieldID getOpusDecoder(JNIEnv *env, jobject obj) {
    static jfieldID ptrFieldId = 0;

    if (!ptrFieldId) {
        jclass c = env->GetObjectClass(obj);
        ptrFieldId = env->GetFieldID(c, "opusDecoder", "J");
        env->DeleteLocalRef(c);
    }

    return (ptrFieldId);
}

/*
jmethodID getEncoderCallback( JNIEnv *env, jobject obj )
{
    static jmethodID  callbackFieldID = 0;

    if( !callbackFieldID )
    {
        jclass c = env->GetObjectClass( obj );
        callbackFieldID = env->GetMethodID( c, "encoderCallback", "(I)V" );
        env->DeleteLocalRef( c );
    }

    return( callbackFieldID );
}

jmethodID getDecoderCallback( JNIEnv *env, jobject obj )
{
    static jmethodID  callbackFieldID = 0;

    if( !callbackFieldID )
    {
        jclass c = env->GetObjectClass( obj );
        callbackFieldID = env->GetMethodID( c, "decoderCallback", "(I)V" );
        env->DeleteLocalRef( c );
    }

    return( callbackFieldID );
}
 */

extern "C"
{

JNIEXPORT jint JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_createEncoder(
        JNIEnv *env,
        jobject thiz,
        jint sampleRate,
        jint channels,
        jint application) {
    jfieldID encoderFieldID = getOpusEncoder(env, thiz);
    OpusEncoder *encoder = (OpusEncoder *) env->GetLongField(thiz, encoderFieldID);

    // Error
    int error;

    if (encoder) {
        // Free the previous encoder
        error = opus_encoder_init(encoder, sampleRate, channels, application);
    }
    else {
        // Now create ANEW
        encoder = opus_encoder_create(sampleRate, channels, application, &error);

        env->SetLongField(thiz, encoderFieldID, (long) encoder);
    }

    return (error);
}

JNIEXPORT jint JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_createDecoder(
        JNIEnv *env,
        jobject thiz,
        jint sampleRate,
        jint channels) {
    jfieldID decoderFieldID = getOpusDecoder(env, thiz);
    OpusDecoder *decoder = (OpusDecoder *) env->GetLongField(thiz, decoderFieldID);

    // Error
    int error;

    if (decoder) {
        // Free the previous encoder
        error = opus_decoder_init(decoder, sampleRate, channels);
    }
    else {
        // Now create ANEW
        decoder = opus_decoder_create(sampleRate, channels, &error);

        // Set our instance var
        env->SetLongField(thiz, decoderFieldID, (long) decoder);
    }

    return (error);
}

JNIEXPORT jint JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_setBitrate(
        JNIEnv *env, jobject thiz, jint bitrate) {
    OpusEncoder *encoder = (OpusEncoder *) env->GetLongField(thiz, getOpusEncoder(env, thiz));

    if (!encoder)
        return (0);

    return (opus_encoder_ctl(encoder, OPUS_SET_BITRATE(bitrate)));
}

JNIEXPORT jint JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_setSignal(
        JNIEnv *env, jobject thiz, jint signal) {
    OpusEncoder *encoder = (OpusEncoder *) env->GetLongField(thiz, getOpusEncoder(env, thiz));

    if (!encoder)
        return (0);

    return (opus_encoder_ctl(encoder, OPUS_SET_SIGNAL(signal)));
}

JNIEXPORT jint JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_setComplexity(
        JNIEnv *env, jobject thiz, jint complexity) {
    OpusEncoder *encoder = (OpusEncoder *) env->GetLongField(thiz, getOpusEncoder(env, thiz));

    if (!encoder)
        return (0);

    return (opus_encoder_ctl(encoder, OPUS_SET_COMPLEXITY(complexity)));
}

JNIEXPORT jint JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_encode(
        JNIEnv *env,
        jobject thiz,
        jbyteArray pcm,
        jint pcmOffset,
        jint frameSize,
        jbyteArray payload,
        jint payloadOffset) {
    OpusEncoder *encoder = (OpusEncoder *) env->GetLongField(thiz, getOpusEncoder(env, thiz));

    // No dice
    if (!encoder)
        return (0);

    // Get the byte array
    jboolean isCopy;
    jbyte *pcmBytes = env->GetByteArrayElements(pcm, &isCopy);
    jbyte *payloadBytes = env->GetByteArrayElements(payload, &isCopy);

//    unsigned char data[ MAX_PACKET+4 ];

    opus_int32 bytesWritten = opus_encode(
            encoder,
            (opus_int16 *) pcmBytes + pcmOffset,
            frameSize,
            (unsigned char *) payloadBytes + 4 + payloadOffset,
            MAX_PACKET
    );

    // Release it
    env->ReleaseByteArrayElements(pcm, (jbyte *) pcmBytes, JNI_ABORT);


    if (bytesWritten > 0) {
        // Write the size to the return data
        int_to_char(bytesWritten, (unsigned char *) payloadBytes);
        // We really have more written for the header
        bytesWritten += 4;
        // Set the bytes
        env->ReleaseByteArrayElements(payload, (jbyte *) payloadBytes, 0);
    }
    else
        env->ReleaseByteArrayElements(payload, (jbyte *) payloadBytes, JNI_ABORT);


    return (bytesWritten);

//    env->CallVoidMethod( thiz, getEncoderCallback( env, thiz ), bytesWritten, returnArray );
}

JNIEXPORT jint JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_decode(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data,
        jint dataOffset,
        jint dataSize,
        jint channels,
        jint maxFrameSize,
        jbyteArray pcm,
        jint pcmOffset) {
    OpusDecoder *decoder = (OpusDecoder *) env->GetLongField(thiz, getOpusDecoder(env, thiz));

    // No dice
    if (!decoder)
        return (0);

    // Get the byte array
    jboolean isCopy;

    // Have some bytes
    jbyte *dataBytes = env->GetByteArrayElements(data, &isCopy);
    jbyte *pcmBytes = env->GetByteArrayElements(pcm, &isCopy);

//    int maxFrameSize = frameSize*6;
//    unsigned char *pcm = (unsigned char *)malloc( maxFrameSize*channels );

    int samplesDecoded = opus_decode(
            decoder,
            (unsigned char *) dataBytes + dataOffset,
            dataSize,
            (opus_int16 *) pcmBytes + pcmOffset,
            maxFrameSize,
            0
    );

    env->ReleaseByteArrayElements(data, (jbyte *) dataBytes, JNI_ABORT);

    if (samplesDecoded > 0) {
        env->ReleaseByteArrayElements(pcm, (jbyte *) pcmBytes, 0);

        return (samplesDecoded * channels * sizeof(opus_int16));
    }

    env->ReleaseByteArrayElements(pcm, (jbyte *) pcmBytes, JNI_ABORT);

    return (0);

//    env->CallVoidMethod( thiz, getDecoderCallback( env, thiz ), samplesDecoded, returnArray );
}

JNIEXPORT void JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_dealloc(
        JNIEnv *env, jobject thiz) {
    jfieldID encoderFieldID = getOpusEncoder(env, thiz);
    jfieldID decoderFieldID = getOpusDecoder(env, thiz);

    OpusEncoder *encoder = (OpusEncoder *) env->GetLongField(thiz, encoderFieldID);
    OpusDecoder *decoder = (OpusDecoder *) env->GetLongField(thiz, decoderFieldID);

    if (encoder) {
        opus_encoder_destroy(encoder);
    }

    if (decoder) {
        opus_decoder_destroy(decoder);
    }
}

JNIEXPORT jstring JNICALL Java_main_java_com_softrangers_sonarcloudmobile_utils_AudioProcessor_opusStrError(
        JNIEnv *env,
        jclass thiz,
        jint error) {
    jstring returnString = env->NewStringUTF(opus_strerror(error));

    return (returnString);
}

}