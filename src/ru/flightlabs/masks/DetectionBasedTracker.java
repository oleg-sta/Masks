package ru.flightlabs.masks;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;

public class DetectionBasedTracker
{
    public DetectionBasedTracker(String cascadeName, int minFaceSize, String modelSp) {
        mNativeObj = nativeCreateObject(cascadeName, minFaceSize, modelSp);
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
    
    public Point[] findEyes(Mat imageGray, Rect face, String modelSp) {
        return findEyes(mNativeObj, imageGray.getNativeObjAddr(), face.x, face.y, face.height, face.width, modelSp);
    }

    private long mNativeObj = 0;
    private long mNativeModel = 0;

    private static native long nativeCreateObject(String cascadeName, int minFaceSize, String modelSp);
    private static native void nativeDestroyObject(long thiz);
    private static native void nativeStart(long thiz);
    private static native void nativeStop(long thiz);
    private static native void nativeSetFaceSize(long thiz, int size);
    private static native void nativeDetect(long thiz, long inputImage, long faces);
    private static native Point[] findEyes(long thiz, long inputImage, int x, int y, int height, int width, String modelSp);
}
