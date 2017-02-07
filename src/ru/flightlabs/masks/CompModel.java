package ru.flightlabs.masks;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.IOException;

import ru.flightlabs.masks.model.primitives.Line;
import ru.flightlabs.masks.model.primitives.Triangle;
import ru.flightlabs.masks.utils.FileUtils;

/**
 * Created by sov on 04.01.2017.
 */

public class CompModel {

    private static final String TAG = "CompModel_class";

    public CascadeClassifier mJavaDetector;
    public volatile DetectionBasedTracker mNativeDetector;
    public Line[] lines;
    public Triangle[] trianlges;
    public File mCascadeFile;

    // in
    public Context context;

    public void loadHaarModel(int resource) {
        Log.i(TAG, "loadHaarModel " + context.getResources().getResourceName(resource));
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);

        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
        try {
            FileUtils.resourceToFile(context.getResources().openRawResource(resource), mCascadeFile);
        } catch (Resources.NotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        if (mJavaDetector.empty()) {
            Log.e(TAG, "Failed to load cascade classifier");
            mJavaDetector = null;
        } else
            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

    }

    public MatOfRect findFaces(Mat mGray, int mAbsoluteFaceSize) {
        MatOfRect faces = new MatOfRect();
        if (mJavaDetector != null) {
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO:
                    // objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        return faces;
    }
}
