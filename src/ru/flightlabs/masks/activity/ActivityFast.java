package ru.flightlabs.masks.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.Set;

import ru.flightlabs.commonlib.Settings;
import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.adapter.AdaptersNotifier;
import ru.flightlabs.masks.adapter.CategoriesNewAdapter;
import ru.flightlabs.masks.camera.FastCameraView;
import ru.flightlabs.masks.ModelLoaderTask;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.adapter.MasksPagerAdapter;
import ru.flightlabs.masks.renderer.MaskRenderer;
import ru.flightlabs.masks.renderer.ShaderEffectMask;
import us.feras.ecogallery.EcoGallery;
import us.feras.ecogallery.EcoGalleryAdapterView;

/**
 * Acivity uses direct frame byte and opengl view
 */
public class ActivityFast extends Activity implements ModelLoaderTask.Callback, AdaptersNotifier {

    TypedArray eyesResourcesSmall;

    CompModel compModel;
    ProgressBar progressBar;
    FastCameraView cameraView;

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
                    Log.e(TAG, "findLandMarks onManagerConnected");
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fast_view);

        Settings.clazz = Gallery.class;

        cameraView = (FastCameraView) findViewById(R.id.fd_fase_surface_view);

        compModel = new CompModel();
        compModel.context = getApplicationContext();

        eyesResources = getResources().obtainTypedArray(R.array.masks_png);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        eyesResourcesSmall = getResources().obtainTypedArray(R.array.masks_small_png);

        final ImageView soundButton = (ImageView) findViewById(R.id.sound_button);
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
        ((CheckBox)findViewById(R.id.useCalman)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Settings.useKalman = b;
            }
        });

        EcoGallery viewPager = (EcoGallery) findViewById(R.id.elements);
        TypedArray iconsCategory = getResources().obtainTypedArray(R.array.masks);

        final CategoriesNewAdapter pager = new CategoriesNewAdapter(this, iconsCategory, new String[]{"d"});

        viewPager.setAdapter(pager);
        viewPager.setOnItemClickListener(new EcoGalleryAdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(EcoGalleryAdapterView<?> parent, View view, int position, long id) {
                pager.selected = position;
                changeItemInCategory(position);
                //pager.notifyDataSetChanged();
            }
        });
        viewPager.setOnItemSelectedListener(new EcoGalleryAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(EcoGalleryAdapterView<?> parent, View view, int position, long id) {
                pager.selected = position;
                changeItemInCategory(position);
            }

            @Override
            public void onNothingSelected(EcoGalleryAdapterView<?> parent) {

            }
        });
        findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Static.makePhoto = true;
            }
        });
        findViewById(R.id.rotate_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.swapCamera();
            }
        });
        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplication(), SettingsActivity.class));
            }
        });

        gLSurfaceView = (GLSurfaceView)findViewById(R.id.fd_glsurface);
        gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        //gLSurfaceView.setZOrderOnTop(true);
        MaskRenderer meRender = new MaskRenderer(this, compModel, new ShaderEffectMask(this));
        gLSurfaceView.setEGLContextClientVersion(2);
        gLSurfaceView.setRenderer(meRender);
        gLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//RENDERMODE_WHEN_DIRTY);
        meRender.frameCamera = cameraView.frameCamera;
        //gLSurfaceView.setZOrderOnTop(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
        //SettingsActivity.debugMode = prefs.getBoolean(SettingsActivity.DEBUG_MODE, true);
        Static.libsLoaded = false;
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        new ModelLoaderTask(this).execute(compModel);
        gLSurfaceView.onResume();
        //Settings.makeUp = false;
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        gLSurfaceView.onPause();
        //TODO has something todo with FastCameraView (rlease, close etc.)
        cameraView.disableView();
    }

    @Override
    public void onModelLoaded() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void changeItemInCategory(int newItem) {
        Static.newIndexEye = newItem;
    }

    @Override
    public void changeColor(int color, int position) {

    }
}