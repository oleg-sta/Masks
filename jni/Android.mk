LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_MODULES:=contrib legacy stitching superres ocl objdetect ml ts videostab video photo calib3d features2d highgui imgproc flann androidcamera core java
OPENCV_LIB_TYPE:=SHARED
#OPENCV_LIB_TYPE:=STATIC
#include ../../sdk/native/jni/OpenCV.mk
include f:/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  := DetectionBasedTracker_jni.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := detection_based_tracker

include $(BUILD_SHARED_LIBRARY)
