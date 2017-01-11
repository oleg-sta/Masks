LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
#include (f:/dlib-18.18/dlib/cmake)

OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_MODULES:=contrib legacy stitching superres ocl objdetect ml ts videostab video photo calib3d features2d highgui imgproc flann androidcamera core java
OPENCV_LIB_TYPE:=SHARED
#OPENCV_LIB_TYPE:=STATIC
#include ../../sdk/native/jni/OpenCV.mk
include f:/openCV-android-sdk-3.1.0/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  := DetectionBasedTracker_jni.cpp 3DFaceModel.cpp 3DFaceModel.h OrthogonalProjectionModel.cpp OrthogonalProjectionModel.h ObjectiveFunctionHelper.cpp ObjectiveFunctionHelper.h Shape2D.cpp Shape2D.h ObjectiveFunction.cpp ObjectiveFunction.h TestObjectiveFunction.cpp TestObjectiveFunction.h
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl
LOCAL_CFLAGS += -std=c++11

LOCAL_MODULE     := detection_based_tracker

include $(BUILD_SHARED_LIBRARY)