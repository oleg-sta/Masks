package ru.flightlabs.masks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
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
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import ru.flightlabs.masks.model.ImgLabModel;
import ru.flightlabs.masks.model.SimpleModel;
import ru.flightlabs.masks.model.primitives.Line;
import ru.flightlabs.masks.model.primitives.Triangle;
import ru.flightlabs.masks.renderer.MyGLRenderer2;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    CompModel compModel;
    public static Mat glViewMatrix2;
    MyGLRenderer2 meRender;

    private GLSurfaceView gLSurfaceView;

    public static final String DIRECTORY_SELFIE = "Masks";
    //public static int counter;
    public static boolean makePhoto;
    public static boolean makePhoto2;
    public static boolean preMakePhoto;

    private static final String TAG = "FdActivity_class";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private Mat mRgba;
    private Mat mGray;
    private volatile DetectionBasedTracker mNativeDetector;
    private boolean loadModel = false; 
    private static final int[] resourceDetector = {R.raw.lbpcascade_frontalface, R.raw.haarcascade_frontalface_alt2, R.raw.my_detector};

    public static boolean debugMode = false;

    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;
    
    private final static int maxSizeEyeWidth = 367;

    Mat currentMaskLandScaped; // рисунок хранится с альфа каналом для наложения, уже повернут для наложения в режиме landscape
    private boolean makeNewFace;
    
    public static TypedArray eyesResources;
    TypedArray eyesResourcesSmall;
    TypedArray eyesResourcesLandmarks;
    
    int currentIndexEye = -1;
    public static int newIndexEye = 0;
    
    Point[] foundEyes = null;
    int fremaCounter = 0;
    
    boolean findPupils = true;
    boolean multi = true;
    final boolean grad = false;
    
    ImageView noPerson;
    ProgressBar progressBar;
    
    int frameCount;
    long timeStart;
    double lastCount = 0.5f;
    
    int haarModel = 0;
    
    int cameraIndex;
    int numberOfCameras;
    boolean cameraFacing;

    private CameraBridgeViewBase mOpenCvCameraView;
    
    MediaActionSound sound = new MediaActionSound();
    boolean playSound = true;
    View borderCam;
    ImageView cameraButton;
    
    int availableProcessors = 1;
    
    ru.flightlabs.masks.model.primitives.Point[] pointsWas;
    VideoWriter videoWriter;
    VideoWriter videoWriterOrig;
    boolean videoWriterStart;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
            case LoaderCallbackInterface.SUCCESS: {
                Log.i(TAG, "OpenCV loaded successfully");

                // Load native library after(!) OpenCV initialization
                System.loadLibrary("detection_based_tracker");
                compModel = new CompModel();
                compModel.context = getApplicationContext();

                mOpenCvCameraView.enableView();

                try {
                    // load cascade file from application resources
                    Log.e(TAG, "findEyes onManagerConnected");
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    compModel.loadHaarModel(resourceDetector[0]);

                    throw new IOException(); // ну выход такой:)
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                }
                Runtime info = Runtime.getRuntime();
                availableProcessors = info.availableProcessors();
                
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
        Log.i(TAG, "LoadModel doInBackground1");
        SimpleModel modelFrom = new ImgLabModel(fModel.getPath());
        Log.i(TAG, "LoadModel doInBackground2");
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
        mOpenCvCameraView.setCvCameraViewListener(this);

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
                debugMode = playSound;
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
                makeNewFace = true;
            }
        });
        
        ViewPager viewPager = (ViewPager) findViewById(R.id.photo_pager);
        MasksPagerAdapter pager = new MasksPagerAdapter(this, eyesResourcesSmall);
        viewPager.setAdapter(pager);

        gLSurfaceView = (GLSurfaceView)findViewById(R.id.fd_glsurface);
        gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        gLSurfaceView.setZOrderOnTop(true);
        meRender = new MyGLRenderer2(this);
        gLSurfaceView.setRenderer(meRender);
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
        debugMode = prefs.getBoolean(Settings.DEBUG_MODE, true);
        findPupils = prefs.getBoolean(Settings.PUPILS_MODE, true);
        multi = prefs.getBoolean(Settings.MULTI_MODE, true);
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "onCameraViewStarted");
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped");
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Log.i(TAG, "onCameraFrame " + new Date() + " count " + lastCount);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(mNativeDetector != null? View.INVISIBLE : View.VISIBLE);
            }
        });
        if (!loadModel) {
            Log.i(TAG, "onCameraFrame loadModel start");
            loadModel = true;
            new LoadModel().execute(compModel);
        }
        if (compModel.mNativeDetector != null) {
            mNativeDetector = compModel.mNativeDetector;
        }
        if (newIndexEye != currentIndexEye) {
            try {
                loadNewEye(newIndexEye);
            } catch (NotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            currentIndexEye = newIndexEye;
        }
        
        if (frameCount == 0) {
            timeStart = System.currentTimeMillis();
            frameCount = 1;
        } else {
            frameCount++;
            if (frameCount == 10) {
                lastCount = (System.currentTimeMillis() - timeStart) / 1000f;
                frameCount = 0;
            }
        }
        Mat mRgba = inputFrame.rgba();
        Mat ret = mRgba;
        if (videoWriterStart) {
            if (videoWriter == null) {
                final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
                int counter = prefs.getInt(Settings.COUNTER_PHOTO, 0);
                counter++;
                Editor editor = prefs.edit();
                editor.putInt(Settings.COUNTER_PHOTO, counter);
                editor.commit();
                File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                File newFile = new File(file, DIRECTORY_SELFIE);
                if (!newFile.exists()) {
                    newFile.mkdirs();
                }
                File fileJpg = new File(newFile, "Masks" + counter + ".avi");
                File fileJpgOrig = new File(newFile, "MasksOrig" + counter + ".avi");
                videoWriter = new VideoWriter(fileJpg.getPath(), VideoWriter.fourcc('M', 'J', 'P', 'G'), 10, new Size(ret.width(), ret.height()));
                videoWriterOrig = new VideoWriter(fileJpgOrig.getPath(), VideoWriter.fourcc('M', 'J', 'P', 'G'), 10, new Size(ret.width(), ret.height()));
                Log.i(TAG, "onCameraFrame open video stream " + videoWriter.isOpened());
            }
        } else {
            if (videoWriter != null) {
                videoWriter.release();
                videoWriterOrig.release();
                videoWriter = null;
                videoWriterOrig = null;
            }
        }
        Log.i(TAG, "onCameraFrame1");
        mGray = inputFrame.gray();
        Log.i(TAG, "onCameraFrame4");
        if (mRgba.height() == 0 || mRgba.width() == 0) {
            return mRgba;
        }

        Log.e(TAG, "findEyes666 " + mRgba.type());
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.cols();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }
        
        int w = mRgba.cols();
        int h = mRgba.rows();
        
        if (videoWriterOrig != null) {
            Log.i(TAG, "onCameraFrame write to video");
            videoWriterOrig.write(mRgba);
        }
        
        int counter = 0;
        // save original pic
        if (makePhoto) {
            preMakePhoto = true;
            makePhoto = false;
            final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
            counter = prefs.getInt(Settings.COUNTER_PHOTO, 0);
            counter++;
            Editor editor = prefs.edit();
            editor.putInt(Settings.COUNTER_PHOTO, counter);
            editor.commit();
            
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File newFile = new File(file, DIRECTORY_SELFIE);
            if (!newFile.exists()) {
                newFile.mkdirs();
            }
            
            Mat mRgbaToSave = mRgba.t();
            Core.flip(mRgba.t(), mRgbaToSave, 1);
            File fileJpg = new File(newFile, "eSelfie_orig_" + counter + ".jpg");
            
            Bitmap bitmap = Bitmap.createBitmap(mRgbaToSave.cols(), mRgbaToSave.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgbaToSave, bitmap);
            FileUtils.saveBitmap(fileJpg.getPath(),bitmap);
            bitmap.recycle();
            mRgbaToSave.release();
            MediaScannerConnection.scanFile(this, new String[] { fileJpg.getPath() }, new String[] { "image/jpeg" }, null);
            
            File fileJpg2 = new File(newFile, "eSelfie_gray_" + counter + ".jpg");
            Bitmap bitmap2 = Bitmap.createBitmap(mGray.cols(), mGray.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mGray, bitmap2);
            FileUtils.saveBitmap(fileJpg2.getPath(),bitmap2);
            bitmap2.recycle();
            MediaScannerConnection.scanFile(this, new String[] { fileJpg2.getPath() }, new String[] { "image/jpeg" }, null);
        }
        
        // выводим серый в направлении для поиска для дебага
        if (debugMode) {
            Mat mGrayTo = new Mat(new Size(100, 100), mGray.type());
            Imgproc.resize(mGray, mGrayTo, new Size(100, 100));
            Mat mGrayToColor = new Mat(new Size(100, 100), mRgba.type());
            Imgproc.cvtColor(mGrayTo, mGrayToColor, Imgproc.COLOR_GRAY2RGBA);
            mGrayTo.release();
            Log.e(TAG, "findEyes666 " + mRgba.height() + " " + mRgba.width());
            Mat rgbaInnerWindow = mRgba.submat(0, 100, 0, 100);
            mGrayToColor.copyTo(rgbaInnerWindow); // копируем повернутый глаз по
                                                  // альфа-каналу(4-й слой)
            rgbaInnerWindow.release();
            mGrayToColor.release();
        }
        
        if (currentMaskLandScaped != null && debugMode) {
            Mat mGrayTo = new Mat(new Size(100, 100), currentMaskLandScaped.type());
            Imgproc.resize(currentMaskLandScaped, mGrayTo, new Size(100, 100));
            //Mat mGrayToColor = new Mat(new Size(100, 100), mRgba.type());
            //Imgproc.cvtColor(mGrayTo, mGrayToColor, Imgproc.COLOR_GRAY2RGBA);
            //mGrayTo.release();
            //Log.e(TAG, "findEyes666 " + mRgba.height() + " " + mRgba.width());
            Mat rgbaInnerWindow = mRgba.submat(100, 200, 0, 100);
            mGrayTo.copyTo(rgbaInnerWindow); // копируем повернутый глаз по
                                                  // альфа-каналу(4-й слой)
            rgbaInnerWindow.release();
            mGrayTo.release();
        }
        MatOfRect faces = compModel.findFaces(mGray, mAbsoluteFaceSize);
        Rect[] facesArray = faces.toArray();
        final boolean haveFace = facesArray.length > 0;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                noPerson.setVisibility(haveFace? View.INVISIBLE : View.VISIBLE);
            }
        });
        Point leftCorner = null;
        Point rightCorner = null;
        int size = -1;
        for (int i = 0; i < facesArray.length; i++) {
            //facesArray[i].height = (int)(facesArray[i].height * 1.2f);
            int newSize = facesArray[i].width;
            if (newSize > size) {
                leftCorner = facesArray[i].tl();
                rightCorner = facesArray[i].br();
                size = newSize;
            }
            if (debugMode) {
                Imgproc.rectangle(mRgba, Helper.orient(leftCorner, w, h), Helper.orient(rightCorner, w, h), FACE_RECT_COLOR, 3);
            }
        }
        // поиск зрачков
        Point rEye = null;
        Point lEye = null;
        foundEyes = null;
        if (leftCorner != null && mNativeDetector != null) {
            Log.i(TAG, "mNativeDetector.findEyes");
            Rect r = facesArray[0];
                Log.i(TAG, "mNativeDetector.findEyes!!!");
                foundEyes = mNativeDetector.findEyes(mGray, r);
                Log.i(TAG, "findEyes116 java " + foundEyes.length);
                if (foundEyes != null && foundEyes.length > 1) {
                    Log.i(TAG, "findEyes116 java " + foundEyes[0].x + " " + foundEyes[0].y);
                    Log.i(TAG, "findEyes116 java " + foundEyes[1].x + " " + foundEyes[1].y);
                }
        }
        if (foundEyes == null) {
            glViewMatrix2 = null;
        }
        if (foundEyes != null) {

            // FIXME you know what i mean
            MatOfPoint3f objectPoints = new MatOfPoint3f();
            MatOfPoint2f imagePoints  = new MatOfPoint2f();

            Mat intrinsics = Mat.eye(3, 3, CvType.CV_32F);
            intrinsics.put(0, 0,  mGray.width()); // ?
            intrinsics.put(1, 1,  mGray.width()); // ?
            intrinsics.put(0, 2, mGray.width() / 2);
            intrinsics.put(1, 2, mGray.height() / 2);
            intrinsics.put(2, 2, 1);

            Mat cameraMatrix = new Mat();
            MatOfDouble distCoeffs = new MatOfDouble();
            Mat rvec = new Mat();
            Mat tvec = new Mat();

            java.util.List<Integer> p3d1 = new ArrayList<>();
            java.util.List<Integer> p2d1 = new ArrayList<>();
            String[] p3d = getResources().getStringArray(R.array.points2DTo3D);
            for (String p : p3d) {
                String[] w2 = p.split(";");
                p2d1.add(Integer.parseInt(w2[0]));
                p3d1.add(Integer.parseInt(w2[1]));
            }

            java.util.List<Point3> pointsList = new ArrayList<Point3>();
            java.util.List<Point> pointsList2 = new ArrayList<Point>();
//            String[] p3d = getResources().getStringArray(R.array.pointsToPnP3D);
            for (int i = 0; i < p3d1.size(); i++) {
                int p3di = p3d1.get(i);
                int p2di = p2d1.get(i);
                pointsList.add(new Point3(meRender.model.tempV[p3di * 3], meRender.model.tempV[p3di * 3 + 1], meRender.model.tempV[p3di * 3 + 2]));
                pointsList2.add(Helper.orient(foundEyes[p2di], w, h));
            }
            objectPoints.fromList(pointsList);
            imagePoints.fromList(pointsList2);
            //Calib3d.calibrate(List<Mat> objectPoints, List<Mat> imagePoints, Size image_size, Mat K, Mat D, List<Mat> rvecs, List<Mat> tvecs);
            Calib3d.solvePnP(objectPoints, imagePoints, intrinsics, distCoeffs, rvec, tvec);

            Log.i("www", "rvec");
            for (int i = 0; i < rvec.height(); i++) {
                for (int j = 0; j < rvec.width(); j++) {
                    Log.i("www", "" + rvec.get(i, j)[0]);
                }
            }
            Log.i("www", "tvec");

            for (int i = 0; i < tvec.height(); i++) {
                for (int j = 0; j < tvec.width(); j++) {
                    Log.i("www", "" + tvec.get(i, j)[0]);
                }
            }
            Log.i("www", "tve0.02");
            MatOfPoint3f objectPoints3 = new MatOfPoint3f();

            MatOfPoint2f imagePoints3  = new MatOfPoint2f();
            java.util.List<Point3> pointsList3 = new ArrayList<Point3>();
            Calib3d.projectPoints(objectPoints, rvec, tvec, intrinsics, distCoeffs, imagePoints3);
            Point[] pp = imagePoints3.toArray();
            pointsList3 = new ArrayList<Point3>();
            for (int i = 0; i < meRender.model.tempV.length / 3; i++) {
                pointsList3.add(new Point3(meRender.model.tempV[i * 3],meRender.model.tempV[i * 3 + 1], meRender.model.tempV[i * 3 + 2]));
            }
            objectPoints3.fromList(pointsList3);
            Calib3d.projectPoints(objectPoints3, rvec, tvec, intrinsics, distCoeffs, imagePoints3);
            Point[] pp2 = imagePoints3.toArray();
            for (int i = 0; i < meRender.model.indices.length / 3; i++) {
                Imgproc.line(mRgba, pp2[meRender.model.indices[i * 3]], pp2[meRender.model.indices[i * 3 + 1]], new Scalar(0, 0, 255, 255));
                Imgproc.line(mRgba, pp2[meRender.model.indices[i * 3 + 1]], pp2[meRender.model.indices[i * 3 + 2]], new Scalar(0, 0, 255, 255));
                Imgproc.line(mRgba, pp2[meRender.model.indices[i * 3 + 2]], pp2[meRender.model.indices[i * 3]], new Scalar(0, 0, 255, 255));
            }
            for (Point pp22 : pp) {
                Imgproc.circle(mRgba, pp22, 3, new Scalar(0, 255, 255, 255), -1);
            }

            Mat rotation = new Mat(4, 4, CvType.CV_64F);
            Mat viewMatrix = new Mat(4, 4, CvType.CV_64F, new Scalar(0));
            Calib3d.Rodrigues(rvec, rotation);

            for (int row = 0; row < 3; ++row) {
                for(int col = 0; col < 3; ++col) {
                    viewMatrix.put(row, col, rotation.get(row, col)[0]);
                }
                viewMatrix.put(row, 3, tvec.get(row, 0)[0]);
            }
            viewMatrix.put(3, 3, 1);

            for (int i = 0; i < tvec.height(); i++) {
                for (int j = 0; j < tvec.width(); j++) {
                    Log.i("wwwglViewMatrixtvec", i + " " + j + " " + tvec.get(i, j)[0]);
                }
            }

            for (int i = 0; i < viewMatrix.height(); i++) {
                for (int j = 0; j < viewMatrix.width(); j++) {
                    Log.i("wwwglViewMatrixt222", i + " " + j + " " + viewMatrix.get(i, j)[0]);
                }
            }

            Mat viewMatrix2 = new Mat(4, 4, CvType.CV_64F, new Scalar(0));

            Mat cvToGl = new Mat(4, 4, CvType.CV_64F, new Scalar(0));
            cvToGl.put(0, 0, 1.0f);
            cvToGl.put(1, 1, -1.0f);
            cvToGl.put(2, 2, -1.0f);
            cvToGl.put(3, 3, 1.0f);
            Core.gemm(cvToGl, viewMatrix, 1, new Mat(), 0, viewMatrix2);

            for (int i = 0; i < viewMatrix2.height(); i++) {
                for (int j = 0; j < viewMatrix2.width(); j++) {
                    Log.i("wwwglViewMatrixt222mu", i + " " + j + " " + viewMatrix2.get(i, j)[0]);
                }
            }


            Mat glViewMatrix = new Mat(4, 4, CvType.CV_64F, new Scalar(0));
            Core.transpose(viewMatrix2 , glViewMatrix);
            for (int i = 0; i < glViewMatrix.height(); i++) {
                for (int j = 0; j < glViewMatrix.width(); j++) {
                    Log.i("wwwglViewMatrix", i + " " + j + " " + glViewMatrix.get(i, j)[0]);
                }
            }
            Log.i("www", "tve0.02");
            glViewMatrix2 = glViewMatrix;

            if (makeNewFace) {
                makeNewFace = false;
                currentMaskLandScaped.release();
                currentMaskLandScaped = new Mat();
                mRgba.copyTo(currentMaskLandScaped);// = текущий экран + точки;
                pointsWas = new ru.flightlabs.masks.model.primitives.Point[foundEyes.length];
                for (int i = 0; i < foundEyes.length; i++) {
                    pointsWas[i] = new ru.flightlabs.masks.model.primitives.Point(foundEyes[i].x, foundEyes[i].y);
                }
            }
            
            if (debugMode) {
                for (Point p : foundEyes) {
                    Imgproc.circle(mRgba, Helper.orient(p, w, h), 2, FACE_RECT_COLOR);
                }
            }
            if (drawMask) {
                int[] bases = getResources().getIntArray(R.array.eyes_center_y_44);
                int indexPo = 0;
                Point leftEye = null;
                Point rightEye = null;
                Point pPrev = null;
                int indexLine = 0;
                for (int lineSize : bases) {
                    for (int i = 0; i < lineSize; i++) {
                        if (indexLine == 5) {
                            if ((i == 0 || i == 3)) {
                                if (leftEye == null) {
                                    leftEye = new Point(foundEyes[indexPo].y, foundEyes[indexPo].x);
                                } else {
                                    leftEye = new Point((leftEye.x + foundEyes[indexPo].y) / 2,
                                            (leftEye.y + foundEyes[indexPo].x) / 2);
                                }
                            }
                        } else if (indexLine == 6) {
                            if ((i == 0 || i == 3)) {
                                if (rightEye == null) {
                                    rightEye = new Point(foundEyes[indexPo].y, foundEyes[indexPo].x);
                                } else {
                                    rightEye = new Point((rightEye.x + foundEyes[indexPo].y) / 2,
                                            (rightEye.y + foundEyes[indexPo].x) / 2);
                                }
                            }
                        }
                        if (indexPo < foundEyes.length) {
                            Point pNew = Helper.orient(foundEyes[indexPo], w, h);
                            if (pPrev != null && debugMode) {
                                Imgproc.line(mRgba, pPrev, pNew, FACE_RECT_COLOR);
                            }
                            pPrev = pNew;
                        }
                        indexPo++;
                    }
                    pPrev = null;
                    indexLine++;
                }
//                drawEye(mRgba, leftEye, rightEye);
                
                foundEyes = ru.flightlabs.masks.model.Utils.completeModel(pointsWas, foundEyes, new int[]{0, 16, 27});
                mNativeDetector.drawMask(currentMaskLandScaped, mRgba, pointsWas, foundEyes, compModel.lines, compModel.trianlges);
            }

        }

        Log.i(TAG, "onCameraFrame6");
        if (debugMode) {
            Imgproc.putText(mRgba, "frames " + String.format("%.3f", (1f / lastCount) * 10) + " in 1 second.", new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
            String resource = getResources().getResourceName(resourceDetector[haarModel % resourceDetector.length]);
            resource = resource.substring(resource.indexOf("raw") + 3);
            Imgproc.putText(mRgba, "haarModel " + resource, new Point(50, 100), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
        }
        Log.i(TAG, "onCameraFrame end " + new Date());
        if (preMakePhoto) {
            preMakePhoto = false;
            Log.i(TAG, "saving start " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File newFile=new File(file, DIRECTORY_SELFIE);
            if (!newFile.exists()) {
                newFile.mkdirs();
            }
            Mat mRgbaToSave = mRgba.t();
            Core.flip(mRgba.t(), mRgbaToSave, 1);
            String text = "eSelfie by FlightLabs.ru";
            int[] yOut = new int[1];
            Size sizeText = Imgproc.getTextSize(text, Core.FONT_HERSHEY_SIMPLEX, 1, 2, yOut);
            Imgproc.rectangle(mRgbaToSave, new Point(mRgbaToSave.width() - sizeText.width, mRgbaToSave.height() - sizeText.height - yOut[0]), new Point(mRgbaToSave.width(), mRgbaToSave.height()), new Scalar(0, 0, 0), -1);
            Imgproc.putText(mRgbaToSave, text, new Point(mRgbaToSave.width() - sizeText.width, mRgbaToSave.height() - yOut[0]), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
            File fileJpg = new File(newFile, "eSelfie" + counter + ".jpg");
            
            Bitmap bitmap = Bitmap.createBitmap(mRgbaToSave.cols(), mRgbaToSave.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgbaToSave, bitmap);
            FileUtils.saveBitmap(fileJpg.getPath(),bitmap);
            bitmap.recycle();
            mRgbaToSave.release();
            // TODO посмотреть альтернативные способы
            MediaScannerConnection.scanFile(this, new String[] { fileJpg.getPath() }, new String[] { "image/jpeg" }, null);
            final Activity d = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    borderCam.setVisibility(View.INVISIBLE);
                    cameraButton.setImageResource(R.drawable.ic_camera);
                    //startActivity(new Intent(d, Gallery.class));
               }
            });
            if (foundEyes != null) {
                OutputStream out;
                try {
                    out = new FileOutputStream(new File(newFile, "eSelfie" + counter + ".jpg.txt"));
                    for (Point point : foundEyes) {
                        out.write((point.x + ";" + point.y + "\r\n").getBytes());
                    }
                    out.close();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "saving end " + true);
        }
        if (videoWriter != null) {
            Log.i(TAG, "onCameraFrame write to video");
            videoWriter.write(mRgba);
        }
        fremaCounter++;
        return mRgba;
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
