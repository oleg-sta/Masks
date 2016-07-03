#include <DetectionBasedTracker_jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/contrib/detection_based_tracker.hpp>

#include <string>
#include <vector>
#include <queue>
#include <pthread.h>

#include <android/log.h>

#include "constants.h"
#include "helpers.h"
#include "ModelClass.cpp"

#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing/render_face_detections.h>
#include <dlib/image_processing.h>
#include <dlib/gui_widgets.h>
#include <dlib/image_io.h>
#include <dlib/opencv.h>


#define LOG_TAG "FaceDetection/DetectionBasedTracker"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace std;
using namespace cv;
using namespace dlib;


inline void vector_Rect_to_Mat(cv::vector<Rect>& v_rect, Mat& mat)
{
    mat = Mat(v_rect, true);
}

JNIEXPORT jlong JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeCreateObject
(JNIEnv * jenv, jclass, jstring jFileName, jint faceSize)
{
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeCreateObject enter");
    const char* jnamestr = jenv->GetStringUTFChars(jFileName, NULL);
    string stdFileName(jnamestr);
    jlong result = 0;

    try
    {
        DetectionBasedTracker::Parameters DetectorParams;
        if (faceSize > 0)
            DetectorParams.minObjectSize = faceSize;
        result = (jlong)new DetectionBasedTracker(stdFileName, DetectorParams);
    }
    catch(cv::Exception& e)
    {
        LOGD("nativeCreateObject caught cv::Exception: %s", e.what());
        jclass je = jenv->FindClass("org/opencv/core/CvException");
        if(!je)
            je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, e.what());
    }
    catch (...)
    {
        LOGD("nativeCreateObject caught unknown exception");
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code of DetectionBasedTracker.nativeCreateObject()");
        return 0;
    }

    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeCreateObject exit");
    return result;
}

JNIEXPORT jlong JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeCreateModel
(JNIEnv * jenv, jclass, jstring jFileName)
{
	LOGD("findEyes119 dd");
	jlong result = 0;
	result = (jlong)new ModelClass(jenv->GetStringUTFChars(jFileName, NULL));
	LOGD("findEyes119 dde111 %i", result);
	return result;
}

JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeDestroyObject
(JNIEnv * jenv, jclass, jlong thiz)
{
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeDestroyObject enter");
    try
    {
        if(thiz != 0)
        {
            ((DetectionBasedTracker*)thiz)->stop();
            delete (DetectionBasedTracker*)thiz;
        }
    }
    catch(cv::Exception& e)
    {
        LOGD("nativeestroyObject caught cv::Exception: %s", e.what());
        jclass je = jenv->FindClass("org/opencv/core/CvException");
        if(!je)
            je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, e.what());
    }
    catch (...)
    {
        LOGD("nativeDestroyObject caught unknown exception");
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code of DetectionBasedTracker.nativeDestroyObject()");
    }
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeDestroyObject exit");
}

JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeStart
(JNIEnv * jenv, jclass, jlong thiz)
{
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeStart enter");
    try
    {
        ((DetectionBasedTracker*)thiz)->run();
    }
    catch(cv::Exception& e)
    {
        LOGD("nativeStart caught cv::Exception: %s", e.what());
        jclass je = jenv->FindClass("org/opencv/core/CvException");
        if(!je)
            je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, e.what());
    }
    catch (...)
    {
        LOGD("nativeStart caught unknown exception");
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code of DetectionBasedTracker.nativeStart()");
    }
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeStart exit");
}

JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeStop
(JNIEnv * jenv, jclass, jlong thiz)
{
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeStop enter");
    try
    {
        ((DetectionBasedTracker*)thiz)->stop();
    }
    catch(cv::Exception& e)
    {
        LOGD("nativeStop caught cv::Exception: %s", e.what());
        jclass je = jenv->FindClass("org/opencv/core/CvException");
        if(!je)
            je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, e.what());
    }
    catch (...)
    {
        LOGD("nativeStop caught unknown exception");
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code of DetectionBasedTracker.nativeStop()");
    }
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeStop exit");
}

JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeSetFaceSize
(JNIEnv * jenv, jclass, jlong thiz, jint faceSize)
{
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeSetFaceSize enter");
    try
    {
        if (faceSize > 0)
        {
            DetectionBasedTracker::Parameters DetectorParams = \
            ((DetectionBasedTracker*)thiz)->getParameters();
            DetectorParams.minObjectSize = faceSize;
            ((DetectionBasedTracker*)thiz)->setParameters(DetectorParams);
        }
    }
    catch(cv::Exception& e)
    {
        LOGD("nativeStop caught cv::Exception: %s", e.what());
        jclass je = jenv->FindClass("org/opencv/core/CvException");
        if(!je)
            je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, e.what());
    }
    catch (...)
    {
        LOGD("nativeSetFaceSize caught unknown exception");
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code of DetectionBasedTracker.nativeSetFaceSize()");
    }
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeSetFaceSize exit");
}

JNIEXPORT jobjectArray JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_findEyes
(JNIEnv * jenv, jclass, jlong thiz, jlong imageGray, jint x, jint y, jint height, jint width, jlong thizModel)
{
	LOGD("Java_ru_flightlabs_masks_DetectionBasedTracker_findEyes");

	cv::Mat imageGrayInner = *((Mat*)imageGray);
	cv::Rect faceRect(x, y,  width, height);
	LOGD("findEyes imageGray %d %d", imageGrayInner.rows, imageGrayInner.cols);
	LOGD("findEyes face %d %d %d %d", faceRect.x, faceRect.y, faceRect.height, faceRect.width);
	std::vector<cv::Point> pixels;
	findEyes(imageGrayInner, faceRect, pixels, (ModelClass*)thizModel);


	jclass clsPoint = jenv->FindClass("org/opencv/core/Point");
	jobjectArray jobAr = jenv->NewObjectArray(pixels.size(), clsPoint, NULL);

	jmethodID constructorPoint = jenv->GetMethodID(clsPoint, "<init>", "(DD)V");
	int i = 0;
	for (std::vector<cv::Point>::iterator it = pixels.begin() ; it != pixels.end(); ++it) {
		jobject objPoint = jenv->NewObject(clsPoint, constructorPoint, (double)(*it).x, (double)(*it).y);
		jenv->SetObjectArrayElement(jobAr, i, objPoint);
		i++;
	}

	return jobAr;
}

JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeDetect
(JNIEnv * jenv, jclass, jlong thiz, jlong imageGray, jlong faces)
{
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeDetect enter");
    try
    {
        cv::vector<Rect> RectFaces;
        ((DetectionBasedTracker*)thiz)->process(*((Mat*)imageGray));
        ((DetectionBasedTracker*)thiz)->getObjects(RectFaces);
        vector_Rect_to_Mat(RectFaces, *((Mat*)faces));
    }
    catch(cv::Exception& e)
    {
        LOGD("nativeCreateObject caught cv::Exception: %s", e.what());
        jclass je = jenv->FindClass("org/opencv/core/CvException");
        if(!je)
            je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, e.what());
    }
    catch (...)
    {
        LOGD("nativeDetect caught unknown exception");
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code DetectionBasedTracker.nativeDetect()");
    }
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeDetect exit");
}

void findEyes(cv::Mat frame_gray, cv::Rect face, std::vector<cv::Point> &pixels, ModelClass *modelClass) {
  LOGD("findEyes111");
  shape_predictor sp;
  LOGD("findEyes112");
  //array2d<int> img;
  LOGD("findEyes1121 %i", frame_gray.type());
  bool rgb = false;
  if (frame_gray.type() == 24) {
	  rgb = true;
  }
  if (rgb) {
  array2d<rgb_pixel> img;

//  assign_image(img, cv_image<uchar>(frame_gray));
  if (rgb) {
	  img.set_size(frame_gray.rows, frame_gray.cols);
  } else {
	  img.set_size(frame_gray.cols, frame_gray.rows); // for grey
  }
  LOGD("findEyes1122");
  for (int i = 0; i < frame_gray.cols; i++) {
	  //LOGD("findEyes1124");
	  for (int j = 0; j < frame_gray.rows; j++) {
		  //LOGD("findEyes115");
		  //img[i][j] = frame_gray.at<uchar>(i, j);
		  cv::Vec4b pixel = frame_gray.at<cv::Vec4b>(i, j);
		  //LOGD("findEyes1126");
          rgb_pixel p;
          p.red   = pixel[0];
          p.green = pixel[1];
          p.blue  = pixel[2];
          //LOGD("findEyes1128");
//		  assign_pixel(img[i][j], p); // for grey
//		  assign_pixel(img[j][i], p); // for rgb
		  if (rgb) {
			  assign_pixel(img[j][i], p); // for rgb
		  } else {
			  assign_pixel(img[i][j], p); // for grey
		  }
	  }
  }
////  cv_image<bgr_pixel> image(frame_gray);
  LOGD("findEyes114");
  //std::vector<dlib::rectangle> dets;
  //dets.push_back(dlib::rectangle);
  dlib::rectangle d(face.x, face.y, face.x + face.width, face.y + face.height);
  LOGD("findEyes115");
  //deserialize(s) >> sp;
  LOGD("findEyes113");
//  full_object_detection shape = sp(img, d);
  full_object_detection shape = modelClass->getsp(img, d);
  LOGD("findEyes116 %i", shape.num_parts());
  if (shape.num_parts() > 2) {
	  LOGD("findEyes116 %i %i", shape.part(0).x(), shape.part(0).y());
	  LOGD("findEyes116 %i %i", shape.part(1).x(), shape.part(1).y());
  }
  for (int i = 0; i < shape.num_parts(); i++) {
	  pixels.push_back(cv::Point(shape.part(i).x(), shape.part(i).y()));
  }
  } else {
	  array2d<int> img;
	  img.set_size(frame_gray.cols, frame_gray.rows); // for grey
	  LOGD("findEyes1122");
	  for (int i = 0; i < frame_gray.cols; i++) {
		  //LOGD("findEyes1124");
		  for (int j = 0; j < frame_gray.rows; j++) {
			  //LOGD("findEyes115");
			  img[i][j] = frame_gray.at<uchar>(i, j);
		  }
	  }
	////  cv_image<bgr_pixel> image(frame_gray);
	  LOGD("findEyes114");
	  //std::vector<dlib::rectangle> dets;
	  //dets.push_back(dlib::rectangle);
	  dlib::rectangle d(face.x, face.y, face.x + face.width, face.y + face.height);
	  LOGD("findEyes115");
	  //deserialize(s) >> sp;
	  LOGD("findEyes113");
	//  full_object_detection shape = sp(img, d);
	  full_object_detection shape = modelClass->getsp(img, d);
	  LOGD("findEyes116 %i", shape.num_parts());
	  if (shape.num_parts() > 2) {
		  LOGD("findEyes116 %i %i", shape.part(0).x(), shape.part(0).y());
		  LOGD("findEyes116 %i %i", shape.part(1).x(), shape.part(1).y());
	  }
	  for (int i = 0; i < shape.num_parts(); i++) {
		  pixels.push_back(cv::Point(shape.part(i).x(), shape.part(i).y()));
	  }
  }
  LOGD("findEyes116");


  LOGD("findEyes116");
}
