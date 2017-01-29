package ru.flightlabs.masks.utils;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import ru.flightlabs.masks.R;
import ru.flightlabs.masks.activity.Settings;
import ru.flightlabs.masks.renderer.Model;

/**
 * Created by sov on 06.01.2017.
 */

public class PoseHelper {

    static java.util.List<Integer> p3d1;
    static java.util.List<Integer> p2d1;

    public static Mat findPose(Model model, int width, Context context, Point[] foundEyes, Mat mRgba) {
        MatOfPoint3f objectPoints = new MatOfPoint3f();
        MatOfPoint2f imagePoints  = new MatOfPoint2f();

        Mat intrinsics = Mat.eye(3, 3, CvType.CV_64F);
        Log.i("wwww2", intrinsics.get(0, 0)[0] + " " + intrinsics.get(1, 0)[0]);
        intrinsics.put(0, 0, mRgba.width()); // ?
        intrinsics.put(1, 1, mRgba.width()); // ?
        intrinsics.put(0, 2, mRgba.width() / 2);
        intrinsics.put(1, 2, mRgba.height() / 2);
        intrinsics.put(2, 2, 1);

        MatOfDouble distCoeffs = new MatOfDouble();
        Mat rvec = new Mat(3, 1, CvType.CV_64F);
        Mat tvec = new Mat(3, 1, CvType.CV_64F);

        if (p2d1 == null) {
            p3d1 = new ArrayList<>();
            p2d1 = new ArrayList<>();
            String[] p3d = context.getResources().getStringArray(R.array.points2DTo3D);
            for (String p : p3d) {
                String[] w2 = p.split(";");
                p2d1.add(Integer.parseInt(w2[0]));
                p3d1.add(Integer.parseInt(w2[1]));
            }
        }

        java.util.List<Point3> pointsList = new ArrayList<Point3>();
        java.util.List<Point> pointsList2 = new ArrayList<Point>();
//            String[] p3d = getResources().getStringArray(R.array.pointsToPnP3D);
        for (int i = 0; i < p3d1.size(); i++) {
            int p3di = p3d1.get(i);
            int p2di = p2d1.get(i);
            pointsList.add(new Point3(model.tempV[p3di * 3], model.tempV[p3di * 3 + 1], model.tempV[p3di * 3 + 2]));
            pointsList2.add(foundEyes[p2di]);
        }
        objectPoints.fromList(pointsList);
        imagePoints.fromList(pointsList2);
        //Calib3d.calibrate(List<Mat> objectPoints, List<Mat> imagePoints, Size image_size, Mat K, Mat D, List<Mat> rvecs, List<Mat> tvecs);
        if (true) {
            // TODO calculate values somehow
            tvec.put(0, 0, 0);
            tvec.put(1, 0, 0);
            tvec.put(2, 0, 2);
            rvec.put(0, 0, -0.235);
            rvec.put(1, 0, 0);
            rvec.put(2, 0, 0);
            Calib3d.solvePnP(objectPoints, imagePoints, intrinsics, distCoeffs, rvec, tvec, true, Calib3d.CV_ITERATIVE);
        } else {
            Calib3d.solvePnP(objectPoints, imagePoints, intrinsics, distCoeffs, rvec, tvec);
        }
        if (Settings.debugMode) {
            Log.i("wwww2 rvec", rvec.width() + " " + rvec.height() + " " + rvec.type());
            Imgproc.putText(mRgba, "tvec " + String.format("%.3f", tvec.get(0, 0)[0]) + String.format(" %.3f", tvec.get(1, 0)[0]) + String.format(" %.3f", tvec.get(2, 0)[0]), new Point(50, 100), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
            Imgproc.putText(mRgba, "rvec " + String.format("%.3f", rvec.get(0, 0)[0]) + String.format(" %.3f", rvec.get(1, 0)[0]) + String.format(" %.3f", rvec.get(2, 0)[0]), new Point(50, 150), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
        }

        MatOfPoint3f objectPoints3 = new MatOfPoint3f();

        MatOfPoint2f imagePoints3  = new MatOfPoint2f();
        java.util.List<Point3> pointsList3 = new ArrayList<Point3>();
        Calib3d.projectPoints(objectPoints, rvec, tvec, intrinsics, distCoeffs, imagePoints3);
        pointsList3 = new ArrayList<Point3>();
        for (int i = 0; i < model.tempV.length / 3; i++) {
            pointsList3.add(new Point3(model.tempV[i * 3],model.tempV[i * 3 + 1], model.tempV[i * 3 + 2]));
        }
        objectPoints3.fromList(pointsList3);
        Calib3d.projectPoints(objectPoints3, rvec, tvec, intrinsics, distCoeffs, imagePoints3);
        Point[] sss = imagePoints3.toArray();
        // debug
        if (Settings.debugMode) {
            for (Point e : sss) {
                Imgproc.circle(mRgba, e, 3, new Scalar(0, 255, 255), -1);
            }
            // draw main vertices
            pointsList3 = new ArrayList<>();
            pointsList3.add(new Point3(0, 0, 0));
            pointsList3.add(new Point3(1, 0, 0));
            pointsList3.add(new Point3(0, 1, 0));
            pointsList3.add(new Point3(0, 0, 1));
            objectPoints3.fromList(pointsList3);
            Calib3d.projectPoints(objectPoints3, rvec, tvec, intrinsics, distCoeffs, imagePoints3);
            Point[] sss2 = imagePoints3.toArray();
            Imgproc.line(mRgba, sss2[0], sss2[1], new Scalar(255, 0, 0), 2);
            Imgproc.line(mRgba, sss2[0], sss2[2], new Scalar(0, 255, 0), 2);
            Imgproc.line(mRgba, sss2[0], sss2[3], new Scalar(0, 0, 255), 2);
        }

        Mat rotation = new Mat(4, 4, CvType.CV_64F);
        Mat viewMatrix = new Mat(4, 4, CvType.CV_64F, new Scalar(0));
        Calib3d.Rodrigues(rvec, rotation);

        for (int row = 0; row < 3; ++row) {
            for(int col = 0; col < 3; ++col) {
                viewMatrix.put(row, col, rotation.get(row, col)[0]);
            }
            viewMatrix.put(row, 3, tvec.get(row, 0)[0]);
        }
        viewMatrix.put(3, 3, 1);
        if (Settings.debugMode) {
            drawDebug(mRgba, model, viewMatrix, intrinsics);
        }

        Mat viewMatrix2 = new Mat(4, 4, CvType.CV_64F, new Scalar(0));

        Mat cvToGl = new Mat(4, 4, CvType.CV_64F, new Scalar(0));
        cvToGl.put(0, 0, 1.0f);
        cvToGl.put(1, 1, -1.0f);
        cvToGl.put(2, 2, -1.0f);
        cvToGl.put(3, 3, 1.0f);
        Core.gemm(cvToGl, viewMatrix, 1, new Mat(), 0, viewMatrix2);

        Mat glViewMatrix = new Mat(4, 4, CvType.CV_64F, new Scalar(0));
        Core.transpose(viewMatrix2 , glViewMatrix);
        return glViewMatrix;
    }

    public static float[] convertToArray(Mat s) {
        float[] matrixArray = new float[16];
        for(int row=0; row<4; ++row)
        {
            for(int col=0; col<4; ++col)
            {
                matrixArray[row * 4 + col] = (float)s.get(row, col)[0];
            }
        }
        return matrixArray;
    }

    public static void drawDebug(Mat mRgba, Model model, Mat viewMatrix2, Mat intrinsics) {
        Mat viewMatrix = viewMatrix2.submat(0, 3, 0 , 4);
        Point[] points = new Point[model.tempV.length / 3];
        for (int c  = 0; c <  model.tempV.length / 3; c++) {
            Mat mat4 = new Mat(4, 1, CvType.CV_64F);
            mat4.put(0, 0, model.tempV[c * 3]);
            mat4.put(1, 0, model.tempV[c * 3 + 1]);
            mat4.put(2, 0, model.tempV[c * 3 + 2]);
            mat4.put(3, 0, 1);
            Mat matRes = new Mat();
            Core.gemm(viewMatrix, mat4, 1, new Mat(), 0, matRes, 0);


//            Mat matRes2 = new Mat();
//            Log.i("wwww", intrinsics.size().width + " " + intrinsics.size().height + " " + matRes.size().width + " " + matRes.size().height);
//            Core.gemm(intrinsics, matRes, 1, new Mat(), 0, matRes2, 0);

            double z = matRes.get(2, 0)[0];
            if (z != 0) {
                z = 1.0/z;
            } else z = 1;
            double x = matRes.get(0, 0)[0]  * z;
            double y = matRes.get(1, 0)[0] * z;

            x = x * intrinsics.get(0, 0)[0] + y * intrinsics.get(0, 1)[0] + intrinsics.get(0, 2)[0];
            y = x * intrinsics.get(1, 0)[0] + y * intrinsics.get(1, 1)[0] + intrinsics.get(1, 2)[0];


            points[c] = new Point(x, y);
            Imgproc.circle(mRgba, new Point(x, y), 3, new Scalar(255, 255, 255), -1);
        }
        for (int indi = 0; indi < model.indices.length / 3; indi++) {
            Imgproc.line(mRgba, points[model.indices[indi * 3]], points[model.indices[indi * 3 + 1]], new Scalar(255, 255, 0));
            Imgproc.line(mRgba, points[model.indices[indi * 3 + 1]], points[model.indices[indi * 3 + 2]], new Scalar(255, 255, 0));
            Imgproc.line(mRgba, points[model.indices[indi * 3 + 2]], points[model.indices[indi * 3]], new Scalar(255, 255, 0));
        }
    }

    public static float[] createProjectionMatrix(int width, int height) {
        float[] mProjectionMatrix = new float[16];
        float ratio = 1;
        float left = -0.5f;
        float right = 0.5f;
        float bottom = -0.5f;
        float top = 0.5f;
        float near = 0.5f;
        float far = 4;
        if (width > height) {
            ratio = (float) width / height;
            left *= ratio;
            right *= ratio;
        } else {
            ratio = (float) height / width;
            bottom *= ratio;
            top *= ratio;
        }
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        return mProjectionMatrix;
    }

    public static float[] createProjectionMatrixThroughPerspective(int width, int height) {
        float[] mProjectionMatrix = new float[16];
        Matrix.perspectiveM(mProjectionMatrix, 0, 83.26707f, (float)width / (float)height, 0.01f, 300.0f); // Specifies the field of view angle, in degrees, in the y direction. atan(0.5 * height/ width) * 2
        return mProjectionMatrix;
    }


    private static float[] createViewMatrix() {
        float[] mViewMatrix = new float[16];
        // точка положения камеры
        float eyeX = 0;
        float eyeY = 0;
        float eyeZ = 3;

        // точка направления камеры
        float centerX = 0;
        float centerY = 0;
        float centerZ = 0;

        // up-вектор
        float upX = 0;
        float upY = 1;
        float upZ = 0;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        return mViewMatrix;
    }


    public static float[] bindMatrix(int width, int height) {
        float[] mMatrix = new float[16];
        Matrix.multiplyMM(mMatrix, 0, createProjectionMatrix(width, height), 0, createViewMatrix(), 0);
        return mMatrix;
    }

}
