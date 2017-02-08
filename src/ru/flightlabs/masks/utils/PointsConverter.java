package ru.flightlabs.masks.utils;

import org.opencv.core.Point;

import ru.flightlabs.masks.model.primitives.Triangle;

/**
 * Created by sov on 08.02.2017.
 */

public class PointsConverter {
    public static float[] convertFromPointsGlCoord(Point[] points, int widthSurf, int heightSurf) {
        float[] res = new float[points.length * 2];
        for (int i = 0; i < points.length; i++) {
            res[i * 2] = (float)points[i].x / widthSurf * 2 - 1;
            res[i * 2 + 1] = (1 - (float)points[i].y / heightSurf) * 2 - 1;
        }
        return res;
    }
    public static float[] convertFromPointsGlCoord(ru.flightlabs.masks.model.primitives.Point[] points, int widthSurf, int heightSurf) {
        float[] res = new float[points.length * 2];
        for (int i = 0; i < points.length; i++) {
            res[i * 2] = (float)points[i].x / widthSurf;
            res[i * 2 + 1] = (float)points[i].y / heightSurf;
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

    public static short[] convertTriangle(Triangle[] trianglesLeftEye) {
        short[] res = new short[trianglesLeftEye.length * 3];
        for (int i = 0; i < trianglesLeftEye.length; i++) {
            res[i * 3] =  (short)trianglesLeftEye[i].point1;
            res[i * 3 + 1] =  (short)trianglesLeftEye[i].point2;
            res[i * 3 + 2] =  (short)trianglesLeftEye[i].point3;
        }
        return res;
    }
}
