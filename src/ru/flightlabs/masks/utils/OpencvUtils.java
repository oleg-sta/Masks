package ru.flightlabs.masks.utils;

import org.opencv.core.Point;

/**
 * Created by sov on 04.01.2017.
 */

public class OpencvUtils {

    public static Point orient(Point point, int width, int heigth) {
        return orient(point, 0, width, heigth);
    }

    public static Point orient(Point point, int orient, int width, int heigth) {
        if (true) {
            return point;
        }
        if (orient == 3) {
            return point;
        } else if (orient == 0) {
            return new Point(point.y, heigth - point.x);
        } else if (orient == 1) {
            return new Point(width - point.x, heigth - point.y);
        } else {
            return new Point(width - point.y, point.x);
        }
    }

    public static double angleOfYx(Point p1, Point p2) {
        // NOTE: Remember that most math has the Y axis as positive above the X.
        // However, for screens we have Y as positive below. For this reason,
        // the Y values are inverted to get the expected results.
        final double deltaX = (p1.y - p2.y);
        final double deltaY = (p2.x - p1.x);
        final double result = Math.toDegrees(Math.atan2(deltaY, deltaX));
        return (result < 0) ? (360d + result) : result;
    }

    public static Point convertToGl(Point old, int width, int height) {
        return new Point(old.x / width, 1 - old.y / height);
    }
}
