#include <DetectionBasedTracker_jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/objdetect/detection_based_tracker.hpp>
#include "opencv2/imgproc/imgproc.hpp"
#include <opencv2/calib3d/calib3d.hpp>

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
#include "ForwardDifference.h"

#include "orthographic_camera_estimation_linear.hpp"
#include "find_coeffs_linear.hpp"

#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing/render_face_detections.h>
#include <dlib/image_processing.h>
#include <dlib/gui_widgets.h>
#include <dlib/image_io.h>
#include <dlib/opencv.h>
#include <dlib/opencv/cv_image.h>

#define LOG_TAG "FaceDetection/DetectionBasedTracker"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace std;
using namespace cv;
using namespace dlib;
const int n_blendshapes = 3;

//inline void vector_Rect_to_Mat(cv::vector<Rect>& v_rect, Mat& mat)
//{
//    mat = Mat(v_rect, true);
//}

JNIEXPORT jlong JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeCreateObject
(JNIEnv * jenv, jclass, jstring jFileName, jint faceSize)
{
    LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeCreateObject enter");
    const char* jnamestr = jenv->GetStringUTFChars(jFileName, NULL);
    string stdFileName(jnamestr);
    jlong result = 0;

    try
    {
//        DetectionBasedTracker::Parameters DetectorParams;
//        if (faceSize > 0)
//            DetectorParams.minObjectSize = faceSize;
//        result = (jlong)new DetectionBasedTracker(stdFileName, DetectorParams);
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
//            DetectorParams.minObjectSize = faceSize;
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

// ВАЖНО: изображение, куда накладываем рисунок находится в landscape, т.е. маска в X,Y, а исходное изображение\результат в Y,X
JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeDrawMask(
		JNIEnv * jenv, jclass, jlong imageFrom, jlong imageTo,
		jobjectArray pointsWas1, jobjectArray pointsTo1, jobjectArray lines1,
		jobjectArray triangle1) {
	cv::Mat imageFromMat = *((Mat*) imageFrom);
	cv::Mat imageToMat = *((Mat*) imageTo); // данное изображение находится в landscape режиме
	int width = imageToMat.rows; // ширина "неправильная"


	// конвертация изначальных точек из java в C-ишный
	int pointsWasLength = jenv->GetArrayLength(pointsWas1);
	Point** pointsWas = new Point*[pointsWasLength];
	for(int i = 0; i < pointsWasLength; i++) {
		jobject point = jenv->GetObjectArrayElement((jobjectArray) pointsWas1, i);
		jclass cls = jenv->GetObjectClass(point);
		pointsWas[i] = new Point(getObjectFieldD(jenv, point, cls, "x"), getObjectFieldD(jenv, point, cls, "y"));
		jenv->DeleteLocalRef(cls);
		jenv->DeleteLocalRef(point);
	}

	// конвертация конечных точек из java в C-ишный
	int pointsToLength = jenv->GetArrayLength(pointsTo1);
	Point** pointsTo = new Point*[jenv->GetArrayLength(pointsTo1)]; // точки найденые на ч\б изображении
	for(int i = 0; i < pointsToLength; i++) {
		jobject point = jenv->GetObjectArrayElement((jobjectArray) pointsTo1, i);
		jclass cls = jenv->GetObjectClass(point);
		pointsTo[i] = new Point(getObjectFieldD(jenv, point, cls, "x"), getObjectFieldD(jenv, point, cls, "y"));
		jenv->DeleteLocalRef(cls);
		jenv->DeleteLocalRef(point);
	}

	// конвертация линий из java в C-ишный
	int linesLength = jenv->GetArrayLength(lines1);
	Line** lines = new Line*[linesLength];
	for(int i = 0; i < linesLength; i++) {
		jobject point = jenv->GetObjectArrayElement((jobjectArray) lines1, i);
		jclass cls = jenv->GetObjectClass(point);
		lines[i] = new Line(getObjectFieldI(jenv, point, cls, "pointStart"), getObjectFieldI(jenv, point, cls, "pointEnd"));
		jenv->DeleteLocalRef(cls);
		jenv->DeleteLocalRef(point);
	}

	// конвертация треугольников из java в C-ишный
	int trianglesLength = jenv->GetArrayLength(triangle1);
	Triangle** triangles = new Triangle*[trianglesLength];
	for(int i = 0; i < trianglesLength; i++) {
		jobject point = jenv->GetObjectArrayElement((jobjectArray) triangle1, i);
		jclass cls = jenv->GetObjectClass(point);
		triangles[i] = new Triangle(getObjectFieldI(jenv, point, cls, "point1"), getObjectFieldI(jenv, point, cls, "point2"), getObjectFieldI(jenv, point, cls, "point3"));
		// вычисялем для треугольника минимальные и максимальные диапазоны
		triangles[i]->minX = std::min(std::min(pointsTo[triangles[i]->p1]->x, pointsTo[triangles[i]->p2]->x), pointsTo[triangles[i]->p3]->x);
		triangles[i]->maxX = std::max(std::max(pointsTo[triangles[i]->p1]->x, pointsTo[triangles[i]->p2]->x), pointsTo[triangles[i]->p3]->x);
		triangles[i]->minY = std::min(std::min(pointsTo[triangles[i]->p1]->y, pointsTo[triangles[i]->p2]->y), pointsTo[triangles[i]->p3]->y);
		triangles[i]->maxY = std::max(std::max(pointsTo[triangles[i]->p1]->y, pointsTo[triangles[i]->p2]->y), pointsTo[triangles[i]->p3]->y);
		jenv->DeleteLocalRef(cls);
		jenv->DeleteLocalRef(point);
	}


	double test = 1.1;
	LOGD("findEyes firsts %i %i %i %i %i %i %i %i %i %i", (int)pointsWas[0]->x, (int)pointsWas[0]->y, (int)pointsTo[0]->x, (int)pointsTo[1]->y, lines[0]->p1,lines[0]->p2, triangles[0]->p1, triangles[0]->p2, triangles[0]->p3, (int)test);

	// проходка по всем треугольникам модели лица
	for (int k = 0; k < trianglesLength; k++) {
		Triangle* triangle = triangles[k];

		// вычисляем матрицу аффиновых преобразований
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

		// делаем проходку по треугольнику от минимального X до максимального X
		for (int i = triangle->minX; i < triangle->maxX; i++) {
			// вычисляем минимальную и максимальную точку пересечения с треуголником
			int* minMax = new int[4];
			minMax[2] = -1;
			minMax[3] = -1;
			getBorder(pointsTo[triangle->p1], pointsTo[triangle->p2], pointsTo[triangle->p3], i, minMax);
			getBorder(pointsTo[triangle->p2], pointsTo[triangle->p3], pointsTo[triangle->p1], i, minMax);
			getBorder(pointsTo[triangle->p3], pointsTo[triangle->p1], pointsTo[triangle->p2], i, minMax);
			if (minMax[0] >= triangle->minY && minMax[1] <= triangle->maxY) {
			for (int j = minMax[0]; j < minMax[1]; j++) {
				// вычисяоем оригинальные точки на маске
				double origX = affine.at<double>(0, 0) * i
						+ affine.at<double>(0, 1) * j + affine.at<double>(0, 2);
				double origY = affine.at<double>(1, 0) * i
						+ affine.at<double>(1, 1) * j + affine.at<double>(1, 2);

				// FIXME здесь может быть страгшная бага, необходимо проверять, что x,y не вышли за диапазоны рамок оригинального и конечного окна
				if (origX >= 0 && origX < imageFromMat.cols && origY >= 0
						&& origY < imageFromMat.rows && i > 0
						&& i < imageToMat.cols && j > 0
						&& j < imageToMat.rows) {
					// получаем пиксли из оригинального рисунка и куда накладываем
					cv::Vec4b pixelFrom = imageFromMat.at<cv::Vec4b>(origY,
							origX);
					cv::Vec4b pixelTo = imageToMat.at<cv::Vec4b>(j, i);
					int alpha = pixelFrom[3];
					// смешиваем по трем каналам(RGB)
					for (int ij = 0; ij < 3; ij++) {
						pixelTo[ij] = (pixelTo[ij] * (255 - alpha)
								+ pixelFrom[ij] * alpha) / 255;
					}
					imageToMat.at<cv::Vec4b>(j, i) = pixelTo;
				}
			}
			} else {
				LOGD("findEyes superError!!!");
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

void getBorder(Point* p1, Point* p2, Point* opposite, int x, int* minMax) {
	if (p2->x != p1->x) {
		double y = p1->y + (x - p1->x) * (p2->y - p1->y) / (p2->x - p1->x);
		double y2 = p1->y
				+ (opposite->x - p1->x) * (p2->y - p1->y) / (p2->x - p1->x);
		if (opposite->y > y2) {
			// TODO possible error
			if (minMax[2] == -1) {
				minMax[2] = 0;
				minMax[0] = (int) y;
			}
			minMax[0] = std::max(minMax[0], (int) y);
		} else {
			if (minMax[3] == -1) {
				minMax[3] = 0;
				minMax[1] = (int) y;
			}
			minMax[1] = std::min(minMax[1], (int) y);
		}

	}
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

JNIEXPORT jobjectArray JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_findLandMarks
(JNIEnv * jenv, jclass, jlong thiz, jlong imageGray, jint x, jint y, jint width, jint height, jlong thizModel)
{
	LOGD("Java_ru_flightlabs_masks_DetectionBasedTracker_findLandMarks");

	cv::Mat imageGrayInner = *((Mat*)imageGray);
	cv::Rect faceRect(x, y,  width, height);
	LOGD("findEyes imageGray %d %d", imageGrayInner.rows, imageGrayInner.cols);
	LOGD("findEyes face %d %d %d %d", faceRect.x, faceRect.y, faceRect.height, faceRect.width);
	std::vector<cv::Point> pixels;
	findLandMarks(imageGrayInner, faceRect, pixels, (ModelClass*)thizModel);


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
//        cv::vector<Rect> RectFaces;
//        ((DetectionBasedTracker*)thiz)->process(*((Mat*)imageGray));
//        ((DetectionBasedTracker*)thiz)->getObjects(RectFaces);
//        vector_Rect_to_Mat(RectFaces, *((Mat*)faces));
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

matrix<double> box_constrain_parameters(dlib::matrix<double> parameters)
{
    dlib::matrix<double> constrained_parameters = parameters;
    for (int i = 6; i < parameters.nr(); ++i)
    {
        if (parameters(i,0)<0)
           constrained_parameters(i,0) = 0;
        if (parameters(i,0)>1)
                   constrained_parameters(i,0) = 1;
    }
    return constrained_parameters;
}

JNIEXPORT jlong JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_morhpFaceInit
(JNIEnv * jenv, jclass, jstring path)
{
    LOGD("Java_ru_flightlabs_masks_DetectionBasedTracker_morhpFaceInit enter");
    const char* jnamestr = jenv->GetStringUTFChars(path, NULL);
    std::string str(jnamestr);
    FaceModel3D* model3d = new FaceModel3D(str, n_blendshapes);
    LOGD("Java_ru_flightlabs_masks_DetectionBasedTracker_morhpFaceInit exit");
    return (jlong)model3d;
}

JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_morhpFace
(JNIEnv * jenv, jclass, jlong jmatrix2dLands, jlong jmatrix3dFace, jlong jinitialParams, jlong model3dL, jint flag, jint juseLinear)
{
    LOGD("Java_ru_flightlabs_masks_DetectionBasedTracker_morhpFace enter");
    LOGD("morhpFace3 %i", juseLinear );
    bool useLinear = (juseLinear == 1)? true : false;
    cv::Mat matrix3dFace = *((Mat*)jmatrix3dFace);
    cv::Mat initialParams0 = *((Mat*)jinitialParams);
    cv::Mat matrix2dLands = *((Mat*)jmatrix2dLands);
    matrix<double> landmarks; // TODO
    landmarks.set_size(2, matrix2dLands.rows);
    for (int i = 0; i < matrix2dLands.rows; i++) {
        landmarks(0,i) = matrix2dLands.at<double>(i, 0);
        landmarks(1,i) = matrix2dLands.at<double>(i, 1);
    }
    LOGD("morhpFace1 %i %i", matrix2dLands.rows, landmarks.nc());
    FaceModel3D model3d = *((FaceModel3D*)model3dL);
    LOGD("morhpFace3");
    Shape2D model2d = Shape2D();
    OrthogonalProjectionModel projection_model = OrthogonalProjectionModel(n_blendshapes);
    LOGD("morhpFace4");
    const matrix<double> &xx = model3d.get_mean_shape3d();
    LOGD("morhpFace41");
    const matrix<double> &yy = model2d.get_shape2d(landmarks);
    LOGD("morhpFace42 %i %i", xx.nc(), yy.nc());
    dlib::matrix<double,6+n_blendshapes,1> initialParameters= projection_model.get_initial_parameters(xx, yy);
    if (flag == 1) {
       for (int i = 0; i < 6+n_blendshapes; i++)
       {
           LOGD("morhpFace411 %i %f", i, initialParams0.at<double>(i, 0));
           initialParameters(0, i) = initialParams0.at<double>(i, 0);
       }
    }
    LOGD("morhpFace43");
    ObjectiveFunctionHelper helper = ObjectiveFunctionHelper(model3d, model2d);
    LOGD("morhpFace5");
    ObjectiveFunction objFun = ObjectiveFunction(helper, projection_model);
    LOGD("morhpFace6");
    objFun.extract2d_from_image(landmarks);
    LOGD("morhpFace7");
    //objFun.set(model3d.get_all_blendshapes());
    //objFun.set(model3d.get_blendshapes());
    double val_init = objFun(initialParameters);
    LOGD("morhpFace8");

    dlib::matrix<double, 6+n_blendshapes, 1> lower = -dlib::ones_matrix<double>(6+n_blendshapes,1) * 0;
    dlib::set_subm(lower,0,0,6,1) = - 10000000;
    dlib::matrix<double, 6+n_blendshapes, 1> upper = dlib::ones_matrix<double>(6+n_blendshapes,1) * 1;
    dlib::set_subm(upper,0,0,6,1) =  10000000;
    dlib::matrix<double, 6+n_blendshapes, 1> initial_parameters_box_constrained = box_constrain_parameters(initialParameters);

    dlib::matrix<double> initial_parameters = initialParameters;


    if (!useLinear)
    {
    LOGD("morhpFace8 iteration");
    auto forward_derivative_fun = forward_derivative(objFun);
    double val = find_min_box_constrained(bfgs_search_strategy(),
                                                        objective_delta_stop_strategy(1e-2),
                                                        objFun,
                                                        forward_derivative_fun,
                                                        initial_parameters_box_constrained, lower,
                                                        upper);
     initial_parameters(6,0) = initial_parameters_box_constrained(6,0);
     initial_parameters(7,0) = initial_parameters_box_constrained(7,0);
     initial_parameters(8,0) = initial_parameters_box_constrained(8,0);

     }
     else
     {
     LOGD("morhpFace8 linear");
    //////experiment
    const int n_transformation_parameters = 6;
    const int n_blendshapes_parameters = n_blendshapes;
    dlib::matrix<double> blend_coeffs;
    eos::fitting::ScaledOrthoProjectionParameters resOrtho;

    dlib::matrix<double> current_3dshape = xx;//dlib::zeros_matrix<double>(xx.nc(), xx.nr());
    std::vector<cv::Vec2f> yy2d;
    std::vector<cv::Vec4f> xx3d;
    dlib::matrix<double> yy2 = helper.get_y(landmarks);
    	    for (int i2 = 0; i2 < xx.nc(); i2++)
    	    {
                    cv::Vec2f vec2f = cv::Vec2f(yy2(0, i2), yy2(1, i2));
    		yy2d.emplace_back(vec2f);
    		cv::Vec4f vec4f = cv::Vec4f(current_3dshape(0, i2), current_3dshape(1, i2), current_3dshape(2, i2), 1);
    		xx3d.emplace_back(vec4f);
    	    }
    	    resOrtho = eos::fitting::estimate_orthographic_projection_linear(yy2d, xx3d);

    	    blend_coeffs = find_coeffs_linear(resOrtho, yy2d, xx, model3d.get_blendshapes());
    	    LOGD("morhpFace9 %i %i %i %i", blend_coeffs.nc(), blend_coeffs.nr(), initial_parameters.nc(), initial_parameters.nr());


    	    //current_3dshape = projection_model.convert_mean_shape(blend_coeffs,xx,model3d.get_blendshapes());



    	    cv::Mat rodr;
    	    cv::Rodrigues(resOrtho.R, rodr);

    	    initial_parameters(0,0) = resOrtho.s;
    	    initial_parameters(1,0) = rodr.at<float>(0, 0);
    	    initial_parameters(2,0) = rodr.at<float>(1, 0);
    	    initial_parameters(3,0) = rodr.at<float>(2, 0);
    	    initial_parameters(4,0) = resOrtho.tx;
    	    initial_parameters(5,0) = resOrtho.ty;


    	    //set_subm(initial_parameters,0,0,n_transformation_parameters,1) = initial_parameters_tr;
            //set_subm(initial_parameters,n_transformation_parameters+1,0,n_blendshapes_parameters,1) = blend_coeffs;
            initial_parameters(6,0) = blend_coeffs(0,0);
            initial_parameters(7,0) = blend_coeffs(1,0);
            initial_parameters(8,0) = blend_coeffs(2,0);

    }
     // experiment

    /*
    double val = find_min_using_approximate_derivatives(bfgs_search_strategy(),
                                                            objective_delta_stop_strategy(1e-5),
                                                            objFun,
                                                            initialParameters, -1);
    */
    LOGD("morhpFace9 %f", initialParameters(0, 6));
    dlib::matrix<double> full_mean_3d = model3d.get_all_mean_shape3d();
    LOGD("morhpFace10");
    std::unordered_map<int, dlib::matrix<double>> all_blendshapes = model3d.get_all_blendshapes();
    //dlib::matrix<double> final_shape_3d = projection_model.convert_mean_shape(initialParameters, full_mean_3d, all_blendshapes);
    dlib::matrix<double> final_shape_3d = projection_model.convert_mean_shape(initial_parameters,full_mean_3d,all_blendshapes);
    LOGD("morhpFace2 %i %i %i", final_shape_3d.nc(), initialParameters.nr(), initialParameters.nc());
    LOGD("morhpFace2 params %f %f", initial_parameters_box_constrained(0, 6), initial_parameters_box_constrained(0, 7));
    LOGD("morhpFace2 params %f %f", initial_parameters_box_constrained(6, 0), initial_parameters_box_constrained(7, 0));
    LOGD("morhpFace2 %f %f %f", final_shape_3d(0,0), final_shape_3d(1,0), final_shape_3d(2,0));
    LOGD("morhpFace2 %f %f %f", full_mean_3d(0,0), full_mean_3d(1,0), full_mean_3d(2,0));
    for (int i = 0; i < final_shape_3d.nc(); ++i)
    {
       matrix3dFace.at<double>(i, 0) = final_shape_3d(0,i);
       matrix3dFace.at<double>(i, 1) = final_shape_3d(1,i);
       matrix3dFace.at<double>(i, 2) = final_shape_3d(2,i);
    }
    for (int i = 0; i < 6+n_blendshapes; i++)
    {
       initialParams0.at<double>(i, 0) = initial_parameters(0, i);
    }
    LOGD("Java_ru_flightlabs_masks_DetectionBasedTracker_morhpFace exit");
}

void findLandMarks(cv::Mat frame_gray, cv::Rect face, std::vector<cv::Point> &pixels, ModelClass *modelClass) {
	LOGD("findEyes1121 %i", frame_gray.type());
	LOGD("findEyes1122");
	cv_image<uchar> img(frame_gray);
	LOGD("findEyes114");
	dlib::rectangle d(face.x, face.y, face.x + face.width,
			face.y + face.height);
	LOGD("findEyes115");
	LOGD("findEyes113");
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
