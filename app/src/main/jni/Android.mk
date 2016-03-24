LOCAL_PATH := $(call my-dir)

# static library info
LOCAL_MODULE := libopus
LOCAL_SRC_FILES := ../prebuild/libopus.a
LOCAL_EXPORT_C_INCLUDES := ../prebuild/include
include $(PREBUILT_STATIC_LIBRARY)

# wrapper info
include $(CLEAR_VARS)
LOCAL_C_INCLUDES += ../prebuild/include
LOCAL_MODULE    := audio_process
LOCAL_SRC_FILES := audio_process.cpp
LOCAL_STATIC_LIBRARIES := libopus
include $(BUILD_SHARED_LIBRARY)