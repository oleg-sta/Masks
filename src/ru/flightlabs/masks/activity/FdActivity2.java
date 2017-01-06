package ru.flightlabs.masks.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraGLSurfaceView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;

import ru.flightlabs.masks.camera.CameraTextureListenerImpl;
import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.adapter.EffectItemsAdapter;
import ru.flightlabs.masks.ModelLoaderTask;
import ru.flightlabs.masks.R;

public class FdActivity2 extends Activity {

    CompModel compModel;

    public static boolean makePhoto;
    public static boolean makePhoto2;
    public static boolean preMakePhoto;

    private static final String TAG = "FdActivity2_class";

    private static final int[] resourceDetector = {R.raw.lbpcascade_frontalface, R.raw.haarcascade_frontalface_alt2, R.raw.my_detector};

    public static TypedArray eyesResources;
    TypedArray eyesResourcesSmall;
    TypedArray eyesResourcesLandmarks;

    int currentIndexEye = -1;
    public static int newIndexEye = 0;

    ImageView noPerson;
    ProgressBar progressBar;

    int haarModel = 0;

    int cameraIndex;
    int numberOfCameras;
    boolean cameraFacing;

    private CameraGLSurfaceView mOpenCvCameraView;

    MediaActionSound sound = new MediaActionSound();
    boolean playSound = true;
    View borderCam;
    ImageView cameraButton;

    int availableProcessors = 1;

    boolean videoWriterStart;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    mOpenCvCameraView.enableView();
                    try {
                        // load cascade file from application resources
                        Log.e(TAG, "findEyes onManagerConnected");
                        compModel.loadHaarModel(resourceDetector[0]);
                        throw new IOException(); // ну выход такой:)
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    protected boolean drawMask;

    public FdActivity2() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate " + getApplicationContext().getResources().getDisplayMetrics().density);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Runtime info = Runtime.getRuntime();
        availableProcessors = info.availableProcessors();

        final Activity d = this;
        setContentView(R.layout.face2_glu);

        mOpenCvCameraView = (CameraGLSurfaceView) findViewById(R.id.fd_activity_surface_view_gl);
        cameraIndex = 0;
        numberOfCameras = android.hardware.Camera.getNumberOfCameras();
        android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            android.hardware.Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraFacing = true;
                cameraIndex = i;
            }
        }

        compModel = new CompModel();
        compModel.context = getApplicationContext();

        mOpenCvCameraView.setCameraIndex(cameraIndex);
        mOpenCvCameraView.setCameraTextureListener(new CameraTextureListenerImpl(this, compModel));

        ListView itemsList = (ListView) findViewById(R.id.list_effects);
        TypedArray icons = getResources().obtainTypedArray(R.array.effects_array);
        eyesResources = getResources().obtainTypedArray(R.array.masks_png);
        eyesResourcesSmall = getResources().obtainTypedArray(R.array.masks_small_png);
        eyesResourcesLandmarks = getResources().obtainTypedArray(R.array.masks_points_68);
        newIndexEye = 0;
        itemsList.setAdapter(new EffectItemsAdapter(this, icons));
        itemsList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                newIndexEye = position;

            }

        });
        borderCam = findViewById(R.id.border);
        cameraButton = (ImageView)findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "saving true");
                if (!makePhoto) {
                    makePhoto = true;
                    makePhoto2 = true;
                    // MediaActionSound sound = new MediaActionSound();
                    cameraButton.setImageResource(R.drawable.ic_camera_r);
                    borderCam.setVisibility(View.VISIBLE);
                    if (playSound) {
                        sound.play(MediaActionSound.SHUTTER_CLICK);
                    }
                }
            }
        });
        cameraButton.setSoundEffectsEnabled(false);

        findViewById(R.id.video_button).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                videoWriterStart = !videoWriterStart;
            }
        });

        findViewById(R.id.setting_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(d, Settings.class));

            }
        });

        findViewById(R.id.rotate_camera).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                swapCamera();
            }
        });

        findViewById(R.id.gallery_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(d, Gallery.class));
            }
        });
        final ImageView soundButton = (ImageView) findViewById(R.id.sound_button);
        findViewById(R.id.sound_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound = !playSound;
                if (playSound) {
                    soundButton.setImageResource(R.drawable.ic_sound);
                } else {
                    soundButton.setImageResource(R.drawable.ic_nosound);
                }
                Settings.debugMode = playSound;
            }
        });
        noPerson = (ImageView) findViewById(R.id.no_person);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        CheckBox c = (CheckBox)findViewById(R.id.rgbCheckBox);
        c.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                drawMask = isChecked;
            }
        });

        findViewById(R.id.setting_button2).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                haarModel++;
                if (haarModel >= resourceDetector.length) {
                    haarModel = 0;
                }
                compModel.loadHaarModel(resourceDetector[haarModel % resourceDetector.length]);
            }
        });
        findViewById(R.id.make_face).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            }
        });
    }

    void changeMask(int newMask) {
        newIndexEye = newMask;
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
        Settings.debugMode = prefs.getBoolean(Settings.DEBUG_MODE, true);
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        new ModelLoaderTask(progressBar).execute(compModel);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    private void swapCamera() {
        cameraIndex++;
        if (cameraIndex >= numberOfCameras) {
            cameraIndex = 0;
        }
        android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraIndex, cameraInfo);
        cameraFacing = false;
        if (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cameraFacing = true;
        }
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(cameraIndex);
        mOpenCvCameraView.enableView();
    }
}