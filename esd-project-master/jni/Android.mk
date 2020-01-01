LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := OCR
LOCAL_SRC_FILES := OCR.cpp letterData.h
LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS += -ljnigraphics
include $(BUILD_SHARED_LIBRARY)