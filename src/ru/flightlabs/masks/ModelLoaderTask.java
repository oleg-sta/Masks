package ru.flightlabs.masks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ru.flightlabs.masks.activity.Settings;
import ru.flightlabs.masks.model.ImgLabModel;
import ru.flightlabs.masks.model.SimpleModel;
import ru.flightlabs.masks.model.primitives.Line;
import ru.flightlabs.masks.model.primitives.Triangle;
import ru.flightlabs.masks.utils.FileUtils;

/**
 * Loads ert model in background
 */
public class ModelLoaderTask extends AsyncTask<CompModel, Void, Void> {

    CompModel compModel;
    private static final String TAG = "LoadModel_class";
    ProgressBar pb;

    public ModelLoaderTask(ProgressBar pb) {
        this.pb = pb;
    }
    @Override
    protected Void doInBackground(CompModel... params) {
        compModel = params[0];
        Log.i(TAG, "ModelLoaderTask doInBackground");
        File cascadeDir = compModel.context.getDir("cascade", Context.MODE_PRIVATE);
        File fModel = new File(cascadeDir, "testing_with_face_landmarks.xml");
        try {
            int res = FileUtils.resourceToFile(compModel.context.getResources().openRawResource(R.raw.monkey_68), fModel);
            Log.i(TAG, "ModelLoaderTask doInBackground111" + res + " " + fModel.length());
        } catch (Resources.NotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //Log.i(TAG, "ModelLoaderTask doInBackground1");
        SimpleModel modelFrom = new ImgLabModel(fModel.getPath());
        Log.i(TAG, "ModelLoaderTask doInBackground2");
        //pointsWas = modelFrom.getPointsWas();
        Log.i(TAG, "ModelLoaderTask doInBackground3");
        //lines = modelFrom.getLines();
        Log.i(TAG, "ModelLoaderTask doInBackground4");
        // load ready triangulation model from file
        List<Line> linesArr = new ArrayList<Line>();
        List<Triangle> triangleArr = new ArrayList<Triangle>();
        AssetManager assetManager = compModel.context.getAssets();
        try {
            {
                InputStream ims = assetManager.open("bear_lines_68.txt");
                BufferedReader in = new BufferedReader(new InputStreamReader(ims));
                String line = null;
                while ((line = in.readLine()) != null) {
                    String[] spl = line.split(";");
                    if (spl.length == 2) {
                        linesArr.add(new Line(Integer.parseInt(spl[0]), Integer.parseInt(spl[1])));
                    }
                }
                ims.close();
            }
            {
                InputStream ims = assetManager.open("bear_triangles_68.txt");
                BufferedReader in = new BufferedReader(new InputStreamReader(ims));
                String line = null;
                while ((line = in.readLine()) != null) {
                    String[] spl = line.split(";");
                    if (spl.length == 3) {
                        triangleArr.add(new Triangle(Integer.parseInt(spl[0]), Integer.parseInt(spl[1]), Integer
                                .parseInt(spl[2])));
                    }
                }
                ims.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        compModel.lines = linesArr.toArray(new Line[0]);
//            Triangulation trianglation = new DelaunayTriangulation();
//            lines = trianglation.convertToTriangle(pointsWas, lines);

        Log.i(TAG, "ModelLoaderTask doInBackground5");
        // load triangles from model
        compModel.trianlges = triangleArr.toArray(new Triangle[0]);
        //trianlges = StupidTriangleModel.getTriagles(pointsWas, lines);
        Log.i(TAG, "ModelLoaderTask doInBackground6");

        final SharedPreferences prefs = compModel.context.getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
        String detectorName = prefs.getString(Settings.MODEL_PATH, Settings.MODEL_PATH_DEFAULT);
        if (!new File(detectorName).exists()) {
            detectorName = "/storage/emulated/0/best_model.dat";
        }

        if (!new File(detectorName).exists()) {
            Log.i(TAG, "ModelLoaderTask doInBackground66");
            try {
                File ertModel = new File(cascadeDir, "ert_model.dat");
                InputStream ims = assetManager.open("sp68.dat");
                int bytes = FileUtils.resourceToFile(ims, ertModel);
                ims.close();
                detectorName = ertModel.getAbsolutePath();
                Log.i(TAG, "ModelLoaderTask doInBackground66 " + detectorName + " " + ertModel.exists() + " " + ertModel.length() + " " + bytes);
            } catch (Resources.NotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i(TAG, "ModelLoaderTask doInBackground667", e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i(TAG, "ModelLoaderTask doInBackground667", e);
            }
        }
        compModel.mNativeDetector = new DetectionBasedTracker(compModel.mCascadeFile.getAbsolutePath(), 0, detectorName);
        Log.i(TAG, "ModelLoaderTask doInBackground7");
        return null;
    }

    protected void onPreExecute() {
        pb.setVisibility(View.VISIBLE);
    }
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        pb.setVisibility(View.INVISIBLE);
    }




}
