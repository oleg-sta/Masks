#include <DetectionBasedTracker_jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/contrib/detection_based_tracker.hpp>
#include "opencv2/imgproc/imgproc.hpp"

#include <string>
#include <vector>
#include <queue>
#include <pthread.h>

#include <android/log.h>

#include "constants.h"
#include "helpers.h"
#include "ModelClass.cpp"
#include "Line.cpp"
#include "Triangle.cpp"

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

JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_mergeAlpha
(JNIEnv * jenv, jclass, jlong imageFrom, jlong imageTo)
{
	LOGD("findEyes Java_ru_flightlabs_masks_DetectionBasedTracker_mergeAlpha");
	cv::Mat imageFromMat = *((Mat*)imageFrom);
	cv::Mat imageToMat = *((Mat*)imageTo);
	LOGD("findEyes Java_ru_flightlabs_masks_DetectionBasedTracker_mergeAlpha2 %i %i", imageFromMat.cols, imageFromMat.rows);
	for (int i = 0; (i < imageFromMat.rows && i < imageToMat.rows); i++) {
		  //LOGD("findEyes1124");
		//LOGD("findEyes Java_ru_flightlabs_masks_DetectionBasedTracker_mergeAlpha3 %i", i);
		  for (int j = 0; (j < imageFromMat.cols && j < imageToMat.cols) ; j++) {
			  //LOGD("findEyes115");
			  //img[i][j] = frame_gray.at<uchar>(i, j);
			  cv::Vec4b pixelFrom = imageFromMat.at<cv::Vec4b>(i, j);
			  cv::Vec4b pixelTo = imageToMat.at<cv::Vec4b>(i, j);
			  int alpha = pixelFrom[3] / 2;
			  for (int ij = 0; ij < 3; ij++) {
				  pixelTo[ij] = (pixelTo[ij] * (255 - alpha) + pixelFrom[ij] * alpha) / 255;
			  }
			  imageToMat.at<cv::Vec4b>(i, j) = pixelTo;
		  }
	}
}

// ВАЖНО: изображение, куда накладываем рисунок находится в landscape
JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeDrawMask(
		JNIEnv * jenv, jclass, jlong imageFrom, jlong imageTo,
		jobjectArray pointsWas1, jobjectArray pointsTo1, jobjectArray lines1,
		jobjectArray triangle1) {
	cv::Mat imageFromMat = *((Mat*) imageFrom);
	cv::Mat imageToMat = *((Mat*) imageTo); // данное изображение находится в landscape режиме
	int width = imageToMat.rows; // ширина "неправильная"


	int pointsWasLength = jenv->GetArrayLength(pointsWas1);
	Point** pointsWas = new Point*[pointsWasLength];
	for(int i = 0; i < pointsWasLength; i++) {
		jobject point = jenv->GetObjectArrayElement((jobjectArray) pointsWas1, i);
		jclass cls = jenv->GetObjectClass(point);
		pointsWas[i] = new Point(getObjectFieldD(jenv, point, cls, "x"), getObjectFieldD(jenv, point, cls, "y"));
		jenv->DeleteLocalRef(cls);
		jenv->DeleteLocalRef(point);
	}

	int pointsToLength = jenv->GetArrayLength(pointsTo1);
	Point** pointsTo = new Point*[jenv->GetArrayLength(pointsTo1)]; // точки найденые на ч\б изображении
	for(int i = 0; i < pointsToLength; i++) {
		jobject point = jenv->GetObjectArrayElement((jobjectArray) pointsTo1, i);
		jclass cls = jenv->GetObjectClass(point);
		pointsTo[i] = new Point(getObjectFieldD(jenv, point, cls, "x"), getObjectFieldD(jenv, point, cls, "y"));
		jenv->DeleteLocalRef(cls);
		jenv->DeleteLocalRef(point);
	}

	int linesLength = jenv->GetArrayLength(lines1);
	Line** lines = new Line*[linesLength];
	for(int i = 0; i < linesLength; i++) {
		jobject point = jenv->GetObjectArrayElement((jobjectArray) lines1, i);
		jclass cls = jenv->GetObjectClass(point);
		lines[i] = new Line(getObjectFieldI(jenv, point, cls, "pointStart"), getObjectFieldI(jenv, point, cls, "pointEnd"));
		jenv->DeleteLocalRef(cls);
		jenv->DeleteLocalRef(point);
	}

	int trianglesLength = jenv->GetArrayLength(triangle1);
	Triangle** triangles = new Triangle*[trianglesLength];
	for(int i = 0; i < trianglesLength; i++) {
		jobject point = jenv->GetObjectArrayElement((jobjectArray) triangle1, i);
		jclass cls = jenv->GetObjectClass(point);
		triangles[i] = new Triangle(getObjectFieldI(jenv, point, cls, "point1"), getObjectFieldI(jenv, point, cls, "point2"), getObjectFieldI(jenv, point, cls, "point3"));
		triangles[i]->minX = std::min(std::min(pointsTo[triangles[i]->p1]->x, pointsTo[triangles[i]->p2]->x), pointsTo[triangles[i]->p3]->x);
		triangles[i]->maxX = std::max(std::max(pointsTo[triangles[i]->p1]->x, pointsTo[triangles[i]->p2]->x), pointsTo[triangles[i]->p3]->x);
		triangles[i]->minY = std::min(std::min(pointsTo[triangles[i]->p1]->y, pointsTo[triangles[i]->p2]->y), pointsTo[triangles[i]->p3]->y);
		triangles[i]->maxY = std::max(std::max(pointsTo[triangles[i]->p1]->y, pointsTo[triangles[i]->p2]->y), pointsTo[triangles[i]->p3]->y);
		jenv->DeleteLocalRef(cls);
		jenv->DeleteLocalRef(point);
	}


	double test = 1.1;
	LOGD("findEyes firsts %i %i %i %i %i %i %i %i %i %i", (int)pointsWas[0]->x, (int)pointsWas[0]->y, (int)pointsTo[0]->x, (int)pointsTo[1]->y, lines[0]->p1,lines[0]->p2, triangles[0]->p1, triangles[0]->p2, triangles[0]->p3, (int)test);

	for (int k = 0; k < trianglesLength; k++) {
		Triangle* triangle = triangles[k];

		Point2f srcTri[3];
		Point2f dstTri[3];
		Mat affine(2, 3, CV_32FC1);

		srcTri[0] = Point2f(pointsTo[triangle->p1]->x,
				pointsTo[triangle->p1]->y);
		srcTri[1] = Point2f(pointsTo[triangle->p2]->x,
				pointsTo[triangle->p2]->y);
		srcTri[2] = Point2f(pointsTo[triangle->p3]->x,
				pointsTo[triangle->p3]->y);

		dstTri[0] = Point2f(pointsWas[triangle->p1]->x,
				pointsWas[triangle->p1]->y);
		dstTri[1] = Point2f(pointsWas[triangle->p2]->x,
				pointsWas[triangle->p2]->y);
		dstTri[2] = Point2f(pointsWas[triangle->p3]->x,
				pointsWas[triangle->p3]->y);

		affine = cv::getAffineTransform(srcTri, dstTri);

		for (int i = triangle->minX; i < triangle->maxX; i++) {
			for (int j = triangle->minY; j < triangle->maxY; j++) {
				Point* curPpoint = new Point(i, j);
				if (checkInTriangle(curPpoint, triangle, pointsTo)) {
					double origX = affine.at<double>(0, 0) * i
							+ affine.at<double>(0, 1) * j
							+ affine.at<double>(0, 2);
					double origY = affine.at<double>(1, 0) * i
							+ affine.at<double>(1, 1) * j
							+ affine.at<double>(1, 2);

					cv::Vec4b pixelFrom = imageFromMat.at<cv::Vec4b>(origY,
							origX);
					cv::Vec4b pixelTo = imageToMat.at<cv::Vec4b>(width - i, j);
					int alpha = pixelFrom[3];
					for (int ij = 0; ij < 3; ij++) {
						pixelTo[ij] = (pixelTo[ij] * (255 - alpha)
								+ pixelFrom[ij] * alpha) / 255;
					}
					imageToMat.at<cv::Vec4b>(width - i, j) = pixelTo;
				}
				delete curPpoint;
			}
		}
	}
	// release resources
	for(int i = 0; i < trianglesLength; i++) {
		delete triangles[i];
	}
	delete triangles;
	for(int i = 0; i < linesLength; i++) {
		delete lines[i];
	}
	delete lines;

	for(int i = 0; i < pointsToLength; i++) {
		delete pointsTo[i];
	}
	delete pointsTo;

	for(int i = 0; i < pointsWasLength; i++) {
		delete pointsWas[i];
	}
	delete pointsWas;

}

double getObjectFieldD(JNIEnv* env, jobject obj, jclass clsFeature, const char* name) {
	jfieldID x1FieldId2 = env->GetFieldID(clsFeature, name, "D");
	return env->GetDoubleField(obj, x1FieldId2);
}

int getObjectFieldI(JNIEnv* env, jobject obj, jclass clsFeature, const char* name) {
	jfieldID x1FieldId2 = env->GetFieldID(clsFeature, name, "I");
	return env->GetIntField(obj, x1FieldId2);
}

bool checkInTriangle(Point* point, Triangle* triangle, Point** points) {
	if (point->x < triangle->minX || point->x > triangle->maxX || point->y < triangle->minY || point->y > triangle->maxY) {
		return false;
	}
    int sign1 = getSide(point, points[triangle->p1], points[triangle->p2]);
    int sign2 = getSide(point, points[triangle->p2], points[triangle->p3]);
    int sign3 = getSide(point, points[triangle->p3], points[triangle->p1]);
    if ((sign1 >= 0 && sign2 >= 0 && sign3 >= 0) || (sign1 <= 0 && sign2 <= 0 && sign3 <= 0)) {
        return true;
    }
    return false;
}

int getSide(Point* pointCheck, Point* point1, Point* point2) {
        if (point1->y != point2->y) {
            return signum((point2->x - point1->x) * (pointCheck->y - point1->y) / (point2->y - point1->y) + point1->x - pointCheck->x) * signum(point2->y - point1->y);
        } else {
            return signum(pointCheck->y - point1->y) * signum(point2->x - point1->x);
        }
    }

int signum(double value) {
	if (value > 0) {
		return 1;
	} else if (value < 0) {
		return -1;
	}
	return 0;
}

JNIEXPORT jobjectArray JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_findEyes
(JNIEnv * jenv, jclass, jlong thiz, jlong imageGray, jint x, jint y, jint width, jint height, jlong thizModel)
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

//  assign_image(img, cv_image<uchar>(frame_gray));
	array2d<int> img;
	img.set_size(frame_gray.rows, frame_gray.cols); // for grey
	LOGD("findEyes1122");
	for (int i = 0; i < frame_gray.rows; i++) {
		//LOGD("findEyes1124");
		for (int j = 0; j < frame_gray.cols; j++) {
			//LOGD("findEyes115");
			img[i][j] = frame_gray.at<uchar>(i, j);
		}
	}
	////  cv_image<bgr_pixel> image(frame_gray);
	LOGD("findEyes114");
	//std::vector<dlib::rectangle> dets;
	//dets.push_back(dlib::rectangle);
	dlib::rectangle d(face.x, face.y, face.x + face.width,
			face.y + face.height);
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
	LOGD("findEyes116");

	LOGD("findEyes116");
}
