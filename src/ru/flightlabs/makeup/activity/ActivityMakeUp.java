package ru.flightlabs.makeup.activity;

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
import android.widget.SeekBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import ru.flightlabs.makeup.CommonI;
import ru.flightlabs.makeup.EditorEnvironment;
import ru.flightlabs.makeup.ResourcesApp;
import ru.flightlabs.makeup.adapter.CategoriesPagerAdapter;
import ru.flightlabs.makeup.adapter.ColorsPagerAdapter;
import ru.flightlabs.makeup.adapter.FilterPagerAdapter;
import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.ModelLoaderTask;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.activity.Settings;
import ru.flightlabs.masks.camera.FastCameraView;
import ru.flightlabs.masks.renderer.MaskRenderer;

/**
 * Created by sov on 08.02.2017.
 */

public class ActivityMakeUp extends Activity implements CommonI {

    public static EditorEnvironment editorEnvironment;
    ResourcesApp resourcesApp;
    CompModel compModel;
    ProgressBar progressBar;
    public static GLSurfaceView gLSurfaceView;

    private SurfaceHolder mHolder;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_makeup);

        FastCameraView sv = (FastCameraView) findViewById(R.id.fd_fase_surface_view);
        mHolder = sv.getHolder();
        mHolder.addCallback(sv);

        compModel = new CompModel();
        compModel.context = getApplicationContext();

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        resourcesApp = new ResourcesApp(this);

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
                Settings.useCalman = b;
            }
        });
        ViewPager viewPagerCategories = (ViewPager) findViewById(R.id.categories);
        CategoriesPagerAdapter pagerCategories = new CategoriesPagerAdapter(this, getResources().getStringArray(R.array.categories));
        viewPagerCategories.setAdapter(pagerCategories);

        editorEnvironment = new EditorEnvironment(getApplication().getApplicationContext(), resourcesApp);
        changeCategory(0);
        ((SeekBar)findViewById(R.id.opacity)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                editorEnvironment.opacity[editorEnvironment.catgoryNum] = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        gLSurfaceView = (GLSurfaceView)findViewById(R.id.fd_glsurface);
        gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        //gLSurfaceView.setZOrderOnTop(true);
        MaskRenderer meRender = new MaskRenderer(this, compModel);
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
        gLSurfaceView.onResume();
        editorEnvironment.init();
        Settings.makeUp = true;
    }


    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        gLSurfaceView.onPause();
        //TODO has something todo with FastCameraView (rlease, close etc.)
    }

    public void changeCategory(int position) {
        int resourceId = R.array.colors_shadow;

        editorEnvironment.newIndexItem = 0;
        editorEnvironment.catgoryNum = position;
        ViewPager viewPager = (ViewPager) findViewById(R.id.elements);
        TypedArray iconsCategory = null;
        if (position == 0) {
            iconsCategory = resourcesApp.eyelashesSmall;
            resourceId = R.array.colors_eyelashes;
        } else if (position == 1) {
            iconsCategory = resourcesApp.eyeshadowSmall;
            resourceId = R.array.colors_shadow;
        } else if (position == 2) {
            resourceId = R.array.colors_eyelashes;
            iconsCategory = resourcesApp.eyelinesSmall;
        } else {
            iconsCategory = resourcesApp.lipsSmall;
            resourceId = R.array.colors_lips;
        }
        FilterPagerAdapter pager = new FilterPagerAdapter(this, iconsCategory);
        viewPager.setAdapter(pager);

        ViewPager viewPagerColors = (ViewPager) findViewById(R.id.colors);
        ColorsPagerAdapter pagerColors = new ColorsPagerAdapter(this, getResources().getIntArray(resourceId));
        viewPagerColors.setAdapter(pagerColors);
        ((SeekBar)findViewById(R.id.opacity)).setProgress(editorEnvironment.opacity[editorEnvironment.catgoryNum]);
    }

    @Override
    public void changeItemInCategory(int newItem) {
        editorEnvironment.newIndexItem = newItem;

    }

    public void changeColor(int color, int position) {
        if (position == 0) {
            editorEnvironment.currentColor[editorEnvironment.catgoryNum] = -1;
            return;
        }
        editorEnvironment.currentColor[editorEnvironment.catgoryNum] = color & 0xFFFFFF;
    }
}
