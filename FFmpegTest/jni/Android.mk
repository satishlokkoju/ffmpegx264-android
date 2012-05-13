LOCAL_PATH := $(call my-dir)

#declare the prebuilt library
include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeg-prebuilt
LOCAL_SRC_FILES := ffmpeg/android/armv7-a/libffmpeg.so
LOCAL_EXPORT_C_INCLUDES := ffmpeg/android/armv7-a/include
LOCAL_EXPORT_LDLIBS := ffmpeg/android/armv7-a/libffmpeg.so
LOCAL_PRELINK_MODULE := true
include $(PREBUILT_SHARED_LIBRARY)

#the andzop library
include $(CLEAR_VARS)
LOCAL_ALLOW_UNDEFINED_SYMBOLS=false
LOCAL_MODULE := ffmpeg-test-jni
LOCAL_CFLAGS += -g -I$(LOCAL_PATH)/ffmpeg -I$(LOCAL_PATH)/x264 -std=gnu99
LOCAL_SRC_FILES := ffmpeg-test-jni.c ffmpeg/ffmpeg.c ffmpeg/cmdutils.c x264/x264.c x264/input/input.c \
	x264/input/timecode.c \
	x264/filters/filters.c \
	x264/filters/video/video.c \
	x264/input/raw.c x264/input/y4m.c x264/output/raw.c x264/output/matroska.c \
	x264/output/matroska_ebml.c x264/output/flv.c x264/output/flv_bytestream.c x264/filters/video/source.c \
	x264/filters/video/internal.c x264/filters/video/resize.c x264/filters/video/cache.c \
	x264/filters/video/fix_vfr_pts.c x264/filters/video/select_every.c x264/filters/video/crop.c x264/filters/video/depth.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/ffmpeg/android/armv7-a/include
LOCAL_SHARED_LIBRARY := ffmpeg-prebuilt
LOCAL_LDLIBS    := -llog -ljnigraphics -lz -lm $(LOCAL_PATH)/ffmpeg/android/armv7-a/libffmpeg.so
include $(BUILD_SHARED_LIBRARY)

