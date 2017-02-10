package ru.flightlabs.masks.utils;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.video.Video;

import ru.flightlabs.masks.model.primitives.Triangle;

/**
 * Created by sov on 08.02.2017.
 */

public class PointsConverter {
    public static float[] convertFromPointsGlCoord(Point[] points, int widthSurf, int heightSurf) {
        float[] res = new float[points.length * 2];
        for (int i = 0; i < points.length; i++) {
            res[i * 2] = (float) points[i].x / widthSurf * 2 - 1;
            res[i * 2 + 1] = (1 - (float) points[i].y / heightSurf) * 2 - 1;
        }
        return res;
    }

    public static float[] convertFromPointsGlCoord(ru.flightlabs.masks.model.primitives.Point[] points, int widthSurf, int heightSurf) {
        float[] res = new float[points.length * 2];
        for (int i = 0; i < points.length; i++) {
            res[i * 2] = (float) points[i].x / widthSurf;
            res[i * 2 + 1] = (float) points[i].y / heightSurf;
        }
        return res;
    }

    public static Point[] addEyePoints(Point[] onImageEyeLeft) {
        Point[] res = new Point[onImageEyeLeft.length + 4];
        for (int i = 0; i < onImageEyeLeft.length; i++) {
            res[i] = onImageEyeLeft[i];
        }
        res[onImageEyeLeft.length] = new Point(onImageEyeLeft[0].x - 20, onImageEyeLeft[0].y - 20);
        res[onImageEyeLeft.length + 1] = new Point(onImageEyeLeft[3].x + 20, onImageEyeLeft[3].y - 20);
        res[onImageEyeLeft.length + 2] = new Point(onImageEyeLeft[3].x + 20, onImageEyeLeft[3].y + 20);
        res[onImageEyeLeft.length + 3] = new Point(onImageEyeLeft[0].x - 20, onImageEyeLeft[0].y + 20);
        return res;
    }

    public static Point[] convertToOpencvPoints(ru.flightlabs.masks.model.primitives.Point[] onImageEyeLeft) {
        Point[] res = new Point[onImageEyeLeft.length];
        for (int i = 0; i < onImageEyeLeft.length; i++) {
            res[i] = new Point(onImageEyeLeft[i].x, onImageEyeLeft[i].y);
        }
        return res;
    }

    public static short[] convertTriangle(Triangle[] trianglesLeftEye) {
        short[] res = new short[trianglesLeftEye.length * 3];
        for (int i = 0; i < trianglesLeftEye.length; i++) {
            res[i * 3] = (short) trianglesLeftEye[i].point1;
            res[i * 3 + 1] = (short) trianglesLeftEye[i].point2;
            res[i * 3 + 2] = (short) trianglesLeftEye[i].point3;
        }
        return res;
    }

    public static Mat points2dToMat(Point[] points) {
        return points2dToMat(points, false);
    }

    public static Mat points2dToMat(Point[] points, boolean withHomogeneous) {
        int coeff = withHomogeneous ? 3 : 2;
        Mat inputLandMarks = new Mat(points.length, coeff, CvType.CV_64FC1);
        double[] buff = new double[inputLandMarks.cols() * inputLandMarks.rows()];
        for (int i = 0; i < points.length; i++) {
            buff[i * coeff] = points[i].x;
            buff[i * coeff + 1] = points[i].y;
            if (withHomogeneous) {
                buff[i * coeff + 2] = 1;
            }

        }
        inputLandMarks.put(0, 0, buff);
        return inputLandMarks;
    }

    public static Point[] reallocateAndCut(Point[] texture, int[] correspondence) {
        Point[] res = new Point[correspondence.length];
        for (int i = 0; i < correspondence.length; i++) {
            res[i] = texture[correspondence[i]];
        }
        return res;
    }

    public static Point[] matTo2dPoints(Mat dst) {
        Point[] res = new Point[dst.rows()];
        double[] buffShape = new double[dst.cols() * dst.rows()];
        dst.get(0, 0, buffShape);
        int rows = dst.rows();
        int cols = dst.cols();
        for (int i = 0; i < rows; i++) {
            res[i] = new Point((float) buffShape[i * cols], (float) buffShape[i * cols + 1]);
        }
        return res;
    }

    public static void matToFloatArray(Mat output3dShape, float[] tempV) {
        double[] buffShape = new double[output3dShape.cols() * output3dShape.rows()];
        output3dShape.get(0, 0, buffShape);
        int rows = output3dShape.rows();
        int cols = output3dShape.cols();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++)
                tempV[i * cols + j] = (float) buffShape[i * cols + j];
        }

    }

    public static Point[] completePointsByAffine(Point[] onImage, Point[] texture, int[] correspondence) {
        Mat srcImage = PointsConverter.points2dToMat(onImage);
        Mat srcTexture = PointsConverter.points2dToMat(PointsConverter.reallocateAndCut(texture, correspondence));
        Log.i("completePointsByAffine", srcTexture.type() + " " + srcTexture.rows() + " " + srcTexture.cols());
        Log.i("completePointsByAffine", srcImage.type() + " " + srcImage.rows() + " " + srcImage.cols());
        //Mat affine = Video.estimateRigidTransform(srcTexture, srcImage, true); // TODO use false,  do not use this method
        Mat affine = solveBy(srcTexture, srcImage); // TODO use false
        Log.i("completePointsByAffine", affine.type() + " " + affine.rows() + " " + affine.cols());
        Mat dst = new Mat();
        try {
            Core.gemm(PointsConverter.points2dToMat(texture, true), affine.t(), 1, new Mat(), 0, dst, 0);
        } catch (Exception e) {
            // FIXME just for working, we need to use another way - manually find this matrices
            return texture;
        }
        return PointsConverter.matTo2dPoints(dst);
    }

    public static Point[] replacePoints(Point[] src, Point[] from, int[] correspondence) {
        Point[] res = new Point[src.length];
        for (int i = 0; i < src.length; i++) {
            res[i] = new Point(src[i].x, src[i].y);
        }
        for (int i = 0; i < correspondence.length; i++) {
            res[correspondence[i]] = new Point(from[correspondence[i]].x, from[correspondence[i]].y);
        }
        return res;
    }

    public static Mat convertToTwoDst(Mat src) {
        Mat res = new Mat(src.rows() / 3, 3, CvType.CV_64FC1);
        double[] buff = new double[src.cols() * src.rows()];
        src.get(0, 0, buff);
        res.put(0, 0, buff);
        return res;
    }

    public static Mat convertToOneDst(Mat src) {
        Mat res = new Mat(src.rows() * 2, 1, CvType.CV_64FC1);
        double[] buff = new double[src.cols() * src.rows()];
        src.get(0, 0, buff);
        res.put(0, 0, buff);
        return res;
    }

    public static Mat convertToOneSrc(Mat src) {
        Mat res = new Mat(src.rows() * 2, 6, CvType.CV_64FC1);
        double[] buff = new double[src.cols() * src.rows()];
        double[] buffRes = new double[res.cols() * res.rows()];
        src.get(0, 0, buff);
        for (int i = 0; i < src.rows(); i++) {
            buffRes[i * 12] = buff[i * 2];
            buffRes[i * 12 + 1] = buff[i * 2 + 1];
            buffRes[i * 12 + 2] = 1;
            buffRes[i * 12 + 9] = buff[i * 2];
            buffRes[i * 12 + 10] = buff[i * 2 + 1];
            buffRes[i * 12 + 11] = 1;

        }
        res.put(0, 0, buffRes);
        return res;
    }

    public static Mat solveBy(Mat src, Mat dst) {
        Mat aff = new Mat();
        Core.solve(convertToOneSrc(src), convertToOneDst(dst), aff, Core.DECOMP_SVD);
        return convertToTwoDst(aff);
    }

    public static float[] convertTovec3(int i) {
        float[] res = new float[3];
        res[2] = (i % 256) / 256f;
        res[1] = ((i / 256 ) % 256) / 256f;
        res[0] = (i / 256 / 256) / 256f;
        return res;
    }
}
