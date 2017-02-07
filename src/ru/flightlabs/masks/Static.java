package ru.flightlabs.masks;

/**
 * Created by sov on 06.01.2017.
 */

public class Static {

    public static boolean makePhoto;
    public static boolean makePhoto2;

    public static boolean drawOrigTexture;
    public static boolean libsLoaded;


    public static int currentIndexEye = -1;
    public static int newIndexEye = 0;

    public static final int[] resourceDetector = {R.raw.lbpcascade_frontalface, R.raw.haarcascade_frontalface_alt2, R.raw.my_detector};
}
