package ru.flightlabs.masks.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.MediaActionSound;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
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
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;

import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.camera.CvCameraViewListener2Impl;
import ru.flightlabs.masks.DetectionBasedTracker;
import ru.flightlabs.masks.adapter.EffectItemsAdapter;
import ru.flightlabs.masks.adapter.MasksPagerAdapter;
import ru.flightlabs.masks.ModelLoaderTask;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.model.ImgLabModel;
import ru.flightlabs.masks.model.SimpleModel;
import ru.flightlabs.masks.renderer.MyGLRenderer2;
import ru.flightlabs.masks.utils.FileUtils;

public class FdActivity extends Activity {

    CompModel compModel;
    public static Mat glViewMatrix2;
    MyGLRenderer2 meRender;

    private GLSurfaceView gLSurfaceView;

    private static final String TAG = "FdActivity_class";

    private volatile DetectionBasedTracker mNativeDetector;

    public static Mat currentMaskLandScaped; // рисунок хранится с альфа каналом для наложения, уже повернут для наложения в режиме landscape
    private boolean makeNewFace;

    public TypedArray eyesResources;
    TypedArray eyesResourcesSmall;
    TypedArray eyesResourcesLandmarks;
    

    boolean findPupils = true;
    boolean multi = true;
    final boolean grad = false;
    
    public ImageView noPerson;
    ProgressBar progressBar;
    
    int haarModel = 0;
    
    int cameraIndex;
    int numberOfCameras;
    boolean cameraFacing;

    private CameraBridgeViewBase mOpenCvCameraView;
    
    MediaActionSound sound = new MediaActionSound();
    boolean playSound = true;
    View borderCam;
    ImageView cameraButton;

    public static ru.flightlabs.masks.model.primitives.Point[] pointsWas;
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
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    compModel.loadHaarModel(Static.resourceDetector[0]);

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
    
    public static boolean drawMask;
    
    // TODO: лучше делать асинхронно
    // загрузка рисунка с альфа каналом + поворот для наложение в landscape
    private void loadNewEye(int index) throws NotFoundException, IOException {
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File newEyeFile = new File(cascadeDir, "temp.png");
        FileUtils.resourceToFile(getResources().openRawResource(eyesResources.getResourceId(index, 0)), newEyeFile);
        // load eye to Mat
        // используем загрузку через андроид, т.к. opencv ломает цвета
        Bitmap bmp = BitmapFactory.decodeFile(newEyeFile.getAbsolutePath());
//        Mat newEyeTmp2 = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
        currentMaskLandScaped= new Mat();
        Utils.bitmapToMat(bmp, currentMaskLandScaped, true);
        
        
        File fModel = new File(cascadeDir, "mask_landmarks.xml");
        try {
            FileUtils.resourceToFile(getResources().openRawResource(eyesResourcesLandmarks.getResourceId(index, 0)), fModel);
        } catch (NotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.i(TAG, "ModelLoaderTask doInBackground1");
        SimpleModel modelFrom = new ImgLabModel(fModel.getPath());
        Log.i(TAG, "ModelLoaderTask doInBackground2");
        pointsWas = modelFrom.getPointsWas();

        cascadeDir.delete();
        Log.i(TAG, "loadNewEye " + currentMaskLandScaped.type() + " " + currentMaskLandScaped.channels());
    }

    public FdActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate " + getApplicationContext().getResources().getDisplayMetrics().density);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Activity d = this;
        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.enableFpsMeter();
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

        mOpenCvCameraView.setCameraIndex(cameraIndex);

        compModel = new CompModel();
        compModel.context = getApplicationContext();
        mOpenCvCameraView.setCvCameraViewListener(new CvCameraViewListener2Impl(compModel, this));

        ListView itemsList = (ListView) findViewById(R.id.list_effects);
        TypedArray icons = getResources().obtainTypedArray(R.array.effects_array);
        eyesResources = getResources().obtainTypedArray(R.array.masks_png);
        eyesResourcesSmall = getResources().obtainTypedArray(R.array.masks_small_png);
        eyesResourcesLandmarks = getResources().obtainTypedArray(R.array.masks_points_68);
        Static.newIndexEye = 0;
        itemsList.setAdapter(new EffectItemsAdapter(this, icons));
        itemsList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Static.newIndexEye = position;
                // TODO start load new mask
                
            }
            
        });
        borderCam = findViewById(R.id.border);
        cameraButton = (ImageView)findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "saving true");
                if (!Static.makePhoto) {
                    Static.makePhoto = true;
                    Static.makePhoto2 = true;
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
                if (haarModel >= Static.resourceDetector.length) {
                    haarModel = 0;
                }
                compModel.loadHaarModel(Static.resourceDetector[haarModel % Static.resourceDetector.length]);
            }
        });
        findViewById(R.id.make_face).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                makeNewFace = true;
            }
        });
        
        //ViewPager viewPager = (ViewPager) findViewById(R.id.photo_pager);
        //MasksPagerAdapter pager = new MasksPagerAdapter(this, eyesResourcesSmall);
        //viewPager.setAdapter(pager);

        gLSurfaceView = (GLSurfaceView)findViewById(R.id.fd_glsurface);
        gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        gLSurfaceView.setZOrderOnTop(true); // FIXME but why did you do that? because i can
        meRender = new MyGLRenderer2(this, eyesResources);
        gLSurfaceView.setRenderer(meRender);

    }
    
    public void changeMask(int newMask) {
        Static.newIndexEye = newMask;
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
        findPupils = prefs.getBoolean(Settings.PUPILS_MODE, true);
        multi = prefs.getBoolean(Settings.MULTI_MODE, true);
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
