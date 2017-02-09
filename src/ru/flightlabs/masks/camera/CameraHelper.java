package ru.flightlabs.masks.camera;

import android.hardware.Camera;
import android.util.Log;

import java.util.List;

/**
 * Created by sov on 07.02.2017.
 */

public class CameraHelper {

    // do not hold this info here
    public static int mCameraWidth;
    public static int mCameraHeight;


    private static final String LOGTAG = "CameraHelper";

    // find the preview size that best suits with aspect ratio and lower max size
    // TODO should consider size and ratio simultaneously, e.g. min( abs(aspect - (float)w/h) + abs(w-maxWidth)/maxWidth + abs(h-maxHeight)/maxHeight)
    public static void calculateCameraPreviewSize(Camera.Parameters param, int maxWidth, int maxHeight) {
        Log.i(LOGTAG, "calculateCameraPreviewSize: "+maxWidth+"x"+maxHeight);

        List<Camera.Size> psize = param.getSupportedPreviewSizes();
        int bestWidth = 0, bestHeight = 0;
        if (psize.size() > 0) {
            float aspect = (float)maxWidth / maxHeight;
            for (Camera.Size size : psize) {
                int w = size.width, h = size.height;
                Log.d(LOGTAG, "checking camera preview size: "+w+"x"+h);
                if ( w <= maxWidth && h <= maxHeight &&
                        w >= bestWidth && h >= bestHeight &&
                        Math.abs(aspect - (float)w/h) < 0.2 ) {
                    bestWidth = w;
                    bestHeight = h;
                }
            }
            if(bestWidth <= 0 || bestHeight <= 0) {
                bestWidth  = psize.get(0).width;
                bestHeight = psize.get(0).height;
                Log.e(LOGTAG, "Error: best size was not selected, using "+bestWidth+" x "+bestHeight);
            } else {
                Log.i(LOGTAG, "Selected best size: "+bestWidth+" x "+bestHeight);
            }
            mCameraWidth  = bestWidth;
            mCameraHeight = bestHeight;
            param.setPreviewSize(bestWidth, bestHeight);
        }
        Log.i(LOGTAG, "calculateCameraPreviewSize: "+mCameraWidth+"x"+mCameraHeight);
    }
}
