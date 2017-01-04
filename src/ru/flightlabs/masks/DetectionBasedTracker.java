package ru.flightlabs.masks;

import java.io.File;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import ru.flightlabs.masks.model.primitives.Line;
import ru.flightlabs.masks.model.primitives.Triangle;

import android.util.Log;

public class DetectionBasedTracker
{
    public DetectionBasedTracker(String cascadeName, int minFaceSize, String modelSp) {
        mNativeObj = nativeCreateObject(cascadeName, minFaceSize);
        if (new File(modelSp).exists()) {
            Log.e("DetectionBasedTracker", "findEyes DetectionBasedTracker !" + modelSp);
            long nat = nativeCreateModel(modelSp);
            if (nat != 0) {
                mNativeModel = nat;
            }
        } else {
            Log.e("DetectionBasedTracker", "findEyes file doesn't exists !" + modelSp);
        }
        Log.e("DetectionBasedTracker", "findEyes mNativeModel " + mNativeModel);
    }

    public void start() {
        nativeStart(mNativeObj);
    }

    public void stop() {
        nativeStop(mNativeObj);
    }

    public void setMinFaceSize(int size) {
        nativeSetFaceSize(mNativeObj, size);
    }

    public void detect(Mat imageGray, MatOfRect faces) {
        nativeDetect(mNativeObj, imageGray.getNativeObjAddr(), faces.getNativeObjAddr());
    }

    public void release() {
        nativeDestroyObject(mNativeObj);
        mNativeObj = 0;
    }
    
    public Point[] findEyes(Mat imageGray, Rect face) {
        if (mNativeModel != null) {
//            return findEyes(mNativeObj, imageGray.getNativeObjAddr(), face.x, face.y, face.width, face.height, mNativeModel);
            return findEyes(mNativeObj, imageGray.getNativeObjAddr(), face.x, face.y, face.width, face.height, mNativeModel);
        } else {
            return new Point[0];
        }
    }
    
    public void mergeAlpha(Mat fromImage, Mat toImage) {
        mergeAlpha(fromImage.getNativeObjAddr(), toImage.getNativeObjAddr());
    }
    
    public void drawMask(Mat currentMaskLandScaped, Mat mRgba, ru.flightlabs.masks.model.primitives.Point[] pointsWas, Point[] foundEyes, Line[] lines, Triangle[] trianlges) {
        nativeDrawMask(currentMaskLandScaped.getNativeObjAddr(), mRgba.getNativeObjAddr(), pointsWas,
                foundEyes, lines, trianlges);
     }

    private long mNativeObj = 0;
    private Long mNativeModel = null;

    private static native long nativeCreateObject(String cascadeName, int minFaceSize);
    private static native long nativeCreateModel(String cascadeName);
    private static native void nativeDestroyObject(long thiz);
    private static native void nativeStart(long thiz);
    private static native void nativeStop(long thiz);
    private static native void nativeSetFaceSize(long thiz, int size);
    private static native void nativeDetect(long thiz, long inputImage, long faces);
    private static native Point[] findEyes(long thiz, long inputImage, int x, int y, int height, int width, long modelSp);
    private static native void mergeAlpha(long fromImage, long toImage);
    private static native void nativeDrawMask(long maskImage, long toImage, ru.flightlabs.masks.model.primitives.Point[] pointsWas,
            Point[] foundEyes, Line[] lines, Triangle[] trianlges);

    
}
