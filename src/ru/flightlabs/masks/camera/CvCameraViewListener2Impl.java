package ru.flightlabs.masks.camera;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Date;

import ru.flightlabs.commonlib.Settings;
import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.DetectionBasedTracker;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.activity.FdActivity;
import ru.flightlabs.masks.activity.SettingsActivity;
import ru.flightlabs.masks.renderer.SimpleOpengl1Renderer;
import ru.flightlabs.masks.utils.OpencvUtils;
import ru.flightlabs.masks.utils.PoseHelper;

/**
 * Created by sov on 06.01.2017.
 */

public class CvCameraViewListener2Impl implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static SimpleOpengl1Renderer meRender;
    Point[] foundEyes = null;
    int fremaCounter = 0;

    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    CompModel compModel;
    int frameCount;
    long timeStart;
    double lastCount = 0.5f;
    DetectionBasedTracker mNativeDetector;
    Activity act;
    PoseHelper poseHelper;


    private static final String TAG = "FdActivityCam_class";

    private Mat mRgba;
    private Mat mGray;

    public CvCameraViewListener2Impl(CompModel compModel, Activity act) {
        this.compModel = compModel;
        this.act = act;
    }

    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "onCameraViewStarted");
        mGray = new Mat();
        mRgba = new Mat();
        Log.i(TAG, "onCameraFrame loadModel start");
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped");
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(TAG, "onCameraFrame " + new Date() + " count " + lastCount);
        if (compModel.mNativeDetector != null) {
            mNativeDetector = compModel.mNativeDetector;
        }
        if (frameCount == 0) {
            timeStart = System.currentTimeMillis();
            frameCount = 1;
        } else {
            frameCount++;
            if (frameCount == 10) {
                lastCount = (System.currentTimeMillis() - timeStart) / 1000f;
                frameCount = 0;
            }
        }
        Mat mRgba = inputFrame.rgba();
        if (poseHelper == null) {
            poseHelper = new PoseHelper(compModel);
            poseHelper.init(act, mRgba.width(), mRgba.height());
        }
        Mat ret = mRgba;
        Log.i(TAG, "onCameraFrame1");
        mGray = inputFrame.gray();
        Log.i(TAG, "onCameraFrame4");
        if (mRgba.height() == 0 || mRgba.width() == 0) {
            return mRgba;
        }

        Log.e(TAG, "findEyes666 " + mRgba.type());
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.cols();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        int w = mRgba.cols();
        int h = mRgba.rows();

        int counter = 0;
        // save original mRgba
        // выводим серый в направлении для поиска для дебага
        if (Settings.debugMode) {
            Mat mGrayTo = new Mat(new Size(100, 100), mGray.type());
            Imgproc.resize(mGray, mGrayTo, new Size(100, 100));
            Mat mGrayToColor = new Mat(new Size(100, 100), mRgba.type());
            Imgproc.cvtColor(mGrayTo, mGrayToColor, Imgproc.COLOR_GRAY2RGBA);
            mGrayTo.release();
            Log.e(TAG, "findEyes666 " + mRgba.height() + " " + mRgba.width());
            Mat rgbaInnerWindow = mRgba.submat(0, 100, 0, 100);
            mGrayToColor.copyTo(rgbaInnerWindow); // копируем повернутый глаз по
            // альфа-каналу(4-й слой)
            rgbaInnerWindow.release();
            mGrayToColor.release();
        }

        MatOfRect faces = compModel.findFaces(mGray, mAbsoluteFaceSize);
        Rect[] facesArray = faces.toArray();
        final boolean haveFace = facesArray.length > 0;
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((FdActivity)act).noPerson.setVisibility(haveFace? View.INVISIBLE : View.VISIBLE);
            }
        });
        Point leftCorner = null;
        Point rightCorner = null;
        int size = -1;
        for (int i = 0; i < facesArray.length; i++) {
            //facesArray[i].height = (int)(facesArray[i].height * 1.2f);
            int newSize = facesArray[i].width;
            if (newSize > size) {
                leftCorner = facesArray[i].tl();
                rightCorner = facesArray[i].br();
                size = newSize;
            }
            if (Settings.debugMode) {
                Imgproc.rectangle(mRgba, OpencvUtils.orient(leftCorner, w, h), OpencvUtils.orient(rightCorner, w, h), FACE_RECT_COLOR, 3);
            }
        }
        // поиск зрачков
        Point rEye = null;
        Point lEye = null;
        foundEyes = null;
        if (leftCorner != null && mNativeDetector != null) {
            Log.i(TAG, "mNativeDetector.findLandMarks");
            Rect r = facesArray[0];
            Log.i(TAG, "mNativeDetector.findLandMarks!!!");
            foundEyes = mNativeDetector.findLandMarks(mGray, r);
            Log.i(TAG, "findEyes116 java " + foundEyes.length);
            if (foundEyes != null && foundEyes.length > 1) {
                Log.i(TAG, "findEyes116 java " + foundEyes[0].x + " " + foundEyes[0].y);
                Log.i(TAG, "findEyes116 java " + foundEyes[1].x + " " + foundEyes[1].y);
            }
        }
        if (foundEyes == null) {
            Static.glViewMatrix2 = null;
        }
        if (foundEyes != null) {

            // FIXME you know what i mean
            poseHelper.findPose(meRender.model, foundEyes, mRgba);

            if (Settings.debugMode) {
                for (Point p : foundEyes) {
                    Imgproc.circle(mRgba, OpencvUtils.orient(p, w, h), 2, FACE_RECT_COLOR);
                }
            }
            if (FdActivity.drawMask && false) {
                int[] bases = act.getResources().getIntArray(R.array.eyes_center_y_44);
                int indexPo = 0;
                Point leftEye = null;
                Point rightEye = null;
                Point pPrev = null;
                int indexLine = 0;
                for (int lineSize : bases) {
                    for (int i = 0; i < lineSize; i++) {
                        if (indexLine == 5) {
                            if ((i == 0 || i == 3)) {
                                if (leftEye == null) {
                                    leftEye = new Point(foundEyes[indexPo].y, foundEyes[indexPo].x);
                                } else {
                                    leftEye = new Point((leftEye.x + foundEyes[indexPo].y) / 2,
                                            (leftEye.y + foundEyes[indexPo].x) / 2);
                                }
                            }
                        } else if (indexLine == 6) {
                            if ((i == 0 || i == 3)) {
                                if (rightEye == null) {
                                    rightEye = new Point(foundEyes[indexPo].y, foundEyes[indexPo].x);
                                } else {
                                    rightEye = new Point((rightEye.x + foundEyes[indexPo].y) / 2,
                                            (rightEye.y + foundEyes[indexPo].x) / 2);
                                }
                            }
                        }
                        if (indexPo < foundEyes.length) {
                            Point pNew = OpencvUtils.orient(foundEyes[indexPo], w, h);
                            if (pPrev != null && Settings.debugMode) {
                                Imgproc.line(mRgba, pPrev, pNew, FACE_RECT_COLOR);
                            }
                            pPrev = pNew;
                        }
                        indexPo++;
                    }
                    pPrev = null;
                    indexLine++;
                }
//                drawEye(mRgba, leftEye, rightEye);

                foundEyes = ru.flightlabs.masks.model.Utils.completeModel(FdActivity.pointsWas, foundEyes, new int[]{0, 16, 27});
                //mNativeDetector.drawMask(FdActivity.currentMaskLandScaped, mRgba, FdActivity.pointsWas, foundEyes, compModel.lines, compModel.trianlges);
            }

        }

        Log.i(TAG, "onCameraFrame6");
        if (Settings.debugMode) {
            Imgproc.putText(mRgba, "frames " + String.format("%.3f", (1f / lastCount) * 10) + " in 1 second.", new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
        }
        Log.i(TAG, "onCameraFrame end " + new Date());
        return mRgba;
    }

}
