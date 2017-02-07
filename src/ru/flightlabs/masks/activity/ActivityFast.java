package ru.flightlabs.masks.activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.FastView;
import ru.flightlabs.masks.ModelLoaderTask;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.adapter.MasksPagerAdapter;
import ru.flightlabs.masks.renderer.TestRenderer;

/**
 * Created by sov on 06.02.2017.
 */

/**
 * experimental fast activity with getting frame from camera on put it in GlView
 */
public class ActivityFast extends Activity {

    TypedArray eyesResourcesSmall;

    CompModel compModel;
    ProgressBar progressBar;
    boolean playSound = true;

    private static final String TAG = "ActivityFast";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    Static.libsLoaded = true;
                    // load cascade file from application resources
                    Log.e(TAG, "findEyes onManagerConnected");
                    compModel.loadHaarModel(Static.resourceDetector[0]);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    TypedArray eyesResources;
    public static GLSurfaceView gLSurfaceView;

    private SurfaceHolder mHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fast_view);

        FastView sv = (FastView) findViewById(R.id.fd_fase_surface_view);
        mHolder = sv.getHolder();
        mHolder.addCallback(sv);

        compModel = new CompModel();
        compModel.context = getApplicationContext();

        eyesResources = getResources().obtainTypedArray(R.array.masks_png);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        eyesResourcesSmall = getResources().obtainTypedArray(R.array.masks_small_png);

        final ImageView soundButton = (ImageView) findViewById(R.id.sound_button);
        findViewById(R.id.sound_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound = !playSound;
                if (playSound) {
                    soundButton.setImageResource(R.drawable.ic_sound);
                } else {
                    soundButton.setImageResource(R.drawable.ic_nosound);
                }
            }
        });
        ((CheckBox)findViewById(R.id.checkDebug)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Settings.debugMode = b;
            }
        });
        ((CheckBox)findViewById(R.id.checkBoxLinear)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Settings.useLinear = b;
            }
        });

        ViewPager viewPager = (ViewPager) findViewById(R.id.photo_pager);
        MasksPagerAdapter pager = new MasksPagerAdapter(this, eyesResourcesSmall);
        viewPager.setAdapter(pager);

        gLSurfaceView = (GLSurfaceView)findViewById(R.id.fd_glsurface);
        gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        //gLSurfaceView.setZOrderOnTop(true);
        TestRenderer meRender = new TestRenderer(this, eyesResources, compModel);
        gLSurfaceView.setEGLContextClientVersion(2);
        gLSurfaceView.setRenderer(meRender);
        gLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//RENDERMODE_WHEN_DIRTY);
        //gLSurfaceView.setZOrderOnTop(false);

    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
        //Settings.debugMode = prefs.getBoolean(Settings.DEBUG_MODE, true);
        Static.libsLoaded = false;
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        new ModelLoaderTask(progressBar).execute(compModel);
    }
}
