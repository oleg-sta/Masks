package ru.flightlabs.masks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    public static final String DIRECTORY_SELFIE = "Masks";
    //public static int counter;
    public static boolean makePhoto;
    // Size constants of eye
    int kEyePercentTop = 25;
    int kEyePercentSide = 13;
    int kEyePercentHeight = 30;
    int kEyePercentWidth = 35;

    // Algorithm Parameters
    int kFastEyeWidth = 50;
    int kWeightBlurSize = 5;
    boolean kEnableWeight = true;
    float kWeightDivisor = 1.0f;
    double kGradientThreshold = 50.0;

    private static final String TAG = "FdActivity_class";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar EYE_RECT_COLOR = new Scalar(0, 0, 255, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker mNativeDetector;

    private boolean debugMode = true;
    private boolean showEyes = true;
    private String[] mEysOnOff;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;
    
    private final static int maxSizeEyeWidth = 367;

    Mat currentMaskLandScaped; // рисунок хранится с альфа каналом для наложения, уже повернут для наложения в режиме landscape
    
    TypedArray eyesResources;
    int currentIndexEye = -1;
    int newIndexEye = 0;
    
    MatOfRect faces;
    Point[] foundEyes = null;
    int fremaCounter = 0;
    
    boolean findPupils = true;
    boolean multi = true;
    final boolean grad = false;
    
    ImageView noPerson;
    
    int frameCount;
    long timeStart;
    double lastCount = 0.5f;
    
    int cameraIndex;
    int numberOfCameras;
    boolean cameraFacing;

    private CameraBridgeViewBase mOpenCvCameraView;
    
    MediaActionSound sound = new MediaActionSound();
    boolean playSound = true;
    View borderCam;
    ImageView cameraButton;
    
    int availableProcessors = 1;
    
    String detectorName;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
            case LoaderCallbackInterface.SUCCESS: {
                Log.i(TAG, "OpenCV loaded successfully");

                // Load native library after(!) OpenCV initialization
                System.loadLibrary("detection_based_tracker");

                try {
                    // load cascade file from application resources
                    Log.e(TAG, "findEyes onManagerConnected");
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);

                    mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                    resourceToFile(getResources().openRawResource(R.raw.lbpcascade_frontalface), mCascadeFile);

                    mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    if (mJavaDetector.empty()) {
                        Log.e(TAG, "Failed to load cascade classifier");
                        mJavaDetector = null;
                    } else
                        Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                    final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
                    detectorName = prefs.getString(Settings.MODEL_PATH, Settings.MODEL_PATH_DEFAULT);
                    Log.e(TAG, "findEyes onManagerConnectedb " + detectorName);
                    mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0, detectorName);

//                    AssetManager assetManager = getApplication().getAssets();
//                    detectorName = getFilesDir() + File.separator + "sp.dat";
//                    resourceToFile(assetManager.open("sp.dat"), new File(detectorName));
                    
                    //cascadeDir.delete();

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                }
                Runtime info = Runtime.getRuntime();
                availableProcessors = info.availableProcessors();

                mOpenCvCameraView.enableView();
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
        resourceToFile(getResources().openRawResource(eyesResources.getResourceId(index, 0)), newEyeFile);
        // load eye to Mat
        // используем загрузку через андроид, т.к. opencv ломает цвета
        Bitmap bmp = BitmapFactory.decodeFile(newEyeFile.getAbsolutePath());
        Mat newEyeTmp2 = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bmp, newEyeTmp2, true);
        
        Log.i(TAG, "loadNewEye2 " + index + " " + newEyeTmp2.type() + " " + newEyeTmp2.channels());
        Mat newEyeTmp = newEyeTmp2.t();
        Core.flip(newEyeTmp2.t(), newEyeTmp, 0);
        newEyeTmp2.release();
        currentMaskLandScaped = newEyeTmp;
        cascadeDir.delete();
        Log.i(TAG, "loadNewEye " + currentMaskLandScaped.type() + " " + currentMaskLandScaped.channels());
    }

    public static void resourceToFile(InputStream is, File toFile) throws IOException {
        FileOutputStream os = new FileOutputStream(toFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
    }

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

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
        eyesResources = getResources().obtainTypedArray(R.array.eyes);
        newIndexEye = eyesResources.length() - 1;
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
            }
        });
        noPerson = (ImageView) findViewById(R.id.no_person);
        
        CheckBox c = (CheckBox)findViewById(R.id.rgbCheckBox);
        c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                drawMask = isChecked;
            }
        });
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
        debugMode = prefs.getBoolean(Settings.DEBUG_MODE, false);
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
        Mat ret = inputFrame.rgba();
        Log.i(TAG, "onCameraFrame1");
        Mat mGrayTmp = inputFrame.gray();
        Log.i(TAG, "onCameraFrame2");
        if (mGray == null) {
            mGray = mGrayTmp.t();
        }
        Log.i(TAG, "onCameraFrame3");
        if (cameraFacing) {
            Core.flip(mGrayTmp.t(), mGray, 0);
        } else {
            mGray = mGrayTmp.t();
        }
        Log.i(TAG, "onCameraFrame4");

        if (!cameraFacing) {
            mRgba = ret;
        } else {
            if (mRgba == null) {
                mRgba = new Mat((int) ret.size().height, (int) ret.size().width, ret.type());
            }
            Log.i(TAG, "onCameraFrame5");
            Core.flip(ret, mRgba, 1);
            Log.i(TAG, "onCameraFrame6");
        }

        Log.e(TAG, "findEyes666 " + mRgba.type());
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.cols();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null) {
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO:
                                                                        // objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
            }
        } else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }

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
        for (int i = 0; i < facesArray.length; i++) {
            facesArray[i].height = (int)(facesArray[i].height * 1.2f);
            if (i == 0) {
                leftCorner = facesArray[i].tl();
                rightCorner = facesArray[i].br();
            }
            Core.rectangle(mRgba, new Point(leftCorner.y, leftCorner.x), new Point(rightCorner.y, rightCorner.x),
                    FACE_RECT_COLOR, 3);
        }
        // поиск зрачков
        Point rEye = null;
        Point lEye = null;
        foundEyes = null;
        if (leftCorner != null) {
            Log.i(TAG, "mNativeDetector.findEyes");
            Rect r = facesArray[0];
            if (true) {
                Log.i(TAG, "mNativeDetector.findEyes!!!");
                foundEyes = mNativeDetector.findEyes(mGray, r, detectorName);  
                Log.i(TAG, "findEyes116 java " + foundEyes.length);
                if (foundEyes != null && foundEyes.length > 1) {
                    Log.i(TAG, "findEyes116 java " + foundEyes[0].x + " " + foundEyes[0].y);
                    Log.i(TAG, "findEyes116 java " + foundEyes[1].x + " " + foundEyes[1].y);
                }
            } else {
//                foundEyes = new ReturnClass();
//                foundEyes.left = new Point(r.width * 0.25, r.height * 0.4);
//                foundEyes.right = new Point(r.width * 0.75, r.height * 0.4);
            }
//            if (debugMode && foundEyes != null) {
//                for (Point p : foundEyes) {
//                    Core.circle(mRgba, new Point(p.y, p.x), 10,
//                            FACE_RECT_COLOR);
//                }
//            }
        }
        
        if (foundEyes != null) {
            for (Point p : foundEyes) {
                Core.circle(mRgba, new Point(p.y, p.x), 2, FACE_RECT_COLOR);
            }
            if (drawMask) {
                int[] bases = getResources().getIntArray(R.array.eyes_center_y);
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
                            Point pNew = new Point(foundEyes[indexPo].y, foundEyes[indexPo].x);
                            if (pPrev != null) {
                                Core.line(mRgba, pPrev, pNew, FACE_RECT_COLOR);
                            }
                            pPrev = pNew;
                        }
                        indexPo++;
                    }
                    pPrev = null;
                    indexLine++;
                }
                drawEye(mRgba, leftEye, rightEye);
            }

        }

        Log.i(TAG, "onCameraFrame6");
        if (debugMode) {
            Core.putText(mRgba, "frames " + String.format("%.3f", (1f / lastCount) * 10) + " in 1 second.", new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
        }
        Log.i(TAG, "onCameraFrame end " + new Date());
        if (makePhoto) {
            makePhoto = false;
            final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
            int counter = prefs.getInt(Settings.COUNTER_PHOTO, 0);
            counter++;
            Editor editor = prefs.edit();
            editor.putInt(Settings.COUNTER_PHOTO, counter);
            editor.commit();
            Log.i(TAG, "saving start " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File newFile=new File(file, DIRECTORY_SELFIE);
            if(!newFile.exists()){
                newFile.mkdirs();
            }
            Mat mRgbaToSave = mRgba.t();
            Core.flip(mRgba.t(), mRgbaToSave, 1);
            String text = "eSelfie by FlightLabs.ru";
            int[] yOut = new int[1];
            Size sizeText = Core.getTextSize(text, Core.FONT_HERSHEY_SIMPLEX, 1, 2, yOut);
            Core.rectangle(mRgbaToSave, new Point(mRgbaToSave.width() - sizeText.width, mRgbaToSave.height() - sizeText.height - yOut[0]), new Point(mRgbaToSave.width(), mRgbaToSave.height()), new Scalar(0, 0, 0), -1);
            Core.putText(mRgbaToSave, text, new Point(mRgbaToSave.width() - sizeText.width, mRgbaToSave.height() - yOut[0]), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
            File fileJpg = new File(newFile, "eSelfie" + counter + ".jpg");
            
            Bitmap bitmap = Bitmap.createBitmap(mRgbaToSave.cols(), mRgbaToSave.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgbaToSave, bitmap);
            saveBitmap(fileJpg.getPath(),bitmap);
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
        fremaCounter++;
        return mRgba;
    }
    
    private void drawEye(Mat mRgba, Point rEye, Point lEye) {
        Point centerEye = new Point((rEye.x + lEye.x) / 2, (rEye.y + lEye.y) / 2);
        double distanceEye = Math.sqrt(Math.pow(rEye.x - lEye.x, 2) +  Math.pow(rEye.y - lEye.y, 2));
        double scale = distanceEye / maxSizeEyeWidth;
        double angle = -angleOfYx(lEye, rEye); // угол поворота глаз от горизонта
        
        
//        Point centerEyePic = new Point(currentEye.cols() / 2, currentEye.rows() / 2);
        Point centerEyePic = new Point(378, currentMaskLandScaped.rows() / 2);
        Rect bbox = new RotatedRect(centerEyePic, new Size(currentMaskLandScaped.size().width * scale, currentMaskLandScaped.size().height * scale), angle).boundingRect();
        
        Mat affineMat = Imgproc.getRotationMatrix2D(centerEyePic, angle, scale);
        double[] x1 = affineMat.get(0, 2);
        double[] y1 = affineMat.get(1, 2);
        x1[0] = x1[0] + bbox.width * centerEyePic.x / currentMaskLandScaped.cols() - centerEyePic.x;
        y1[0] = y1[0] + bbox.height / 2.0 - centerEyePic.y;
        Point leftPoint = new Point(centerEye.x - bbox.width * centerEyePic.x / currentMaskLandScaped.cols(), centerEye.y - bbox.height / 2.0);
        if (leftPoint.y < 0) {
            bbox.height = (int)(bbox.height + leftPoint.y);
            y1[0] = y1[0] + leftPoint.y;
            leftPoint.y = 0;
        }
        if (leftPoint.x < 0) {
            bbox.width = (int)(bbox.width + leftPoint.x);
            x1[0] = x1[0] + leftPoint.x;
            leftPoint.x = 0;
        }
        if ((leftPoint.y + bbox.height) > mRgba.height()) {
            int delta = (int)(leftPoint.y + bbox.height - mRgba.height());
            bbox.height = bbox.height - delta;
        }
        if ((leftPoint.x + bbox.width) > mRgba.width()) {
            int delta = (int)(leftPoint.x + bbox.width - mRgba.width());
            bbox.width = bbox.width - delta;
        }
        affineMat.put(0, 2, x1);
        affineMat.put(1, 2, y1);
        
        Size newSize = new Size(bbox.size().width, bbox.size().height);
        Mat sizedRotatedEye = new Mat(newSize, currentMaskLandScaped.type());
        Imgproc.warpAffine(currentMaskLandScaped, sizedRotatedEye, affineMat, newSize);
        
        int newEyeHeight = sizedRotatedEye.height();
        int newEyeWidth = sizedRotatedEye.width();
        Rect r = new Rect((int) (leftPoint.x), (int) (leftPoint.y),
                newEyeWidth, newEyeHeight);
        
        Core.rectangle(mRgba, r.tl(), r.br(), new Scalar(255, 0, 0), 3);
        Mat rgbaInnerWindow = mRgba.submat(r.y, r.y + r.height, r.x, r.x + r.width);
        
        List<Mat> layers = new ArrayList<Mat>();
        Core.split(sizedRotatedEye, layers);
//        sizedRotatedEye.copyTo(rgbaInnerWindow, layers.get(3)); // копируем повернутый глаз по альфа-каналу(4-й слой)
//        mNativeDetector.mergeAlpha(sizedRotatedEye, mRgba);
        mNativeDetector.mergeAlpha(sizedRotatedEye, rgbaInnerWindow);
//        for(int i = 0; i < sizedRotatedEye.cols(); i++) {
//            for(int j = 0; j < sizedRotatedEye.rows(); j++) {
//                double[] pixel = sizedRotatedEye.get(j, i);
//                double aplha = pixel[3];
//                double[] pixelOld = rgbaInnerWindow.get(j, i);
//                for (int ij = 0; ij < 3; ij++) {
//                    pixelOld[ij] = (pixelOld[ij] * (255 - aplha) + pixel[ij] * aplha) / 255;
//                }
//                
//                rgbaInnerWindow.put(j, i, pixelOld);
//            }
//        }
        rgbaInnerWindow.release();
        sizedRotatedEye.release();
        
        // ------------------CHECK------------------------------
//        int ij = 0;
//        Mat alpha = layers.get(3);
//        for(int i = 0; i < alpha.cols(); i++) {
//            for(int j = 0; j < alpha.rows(); j++) {
//                double jee = alpha.get(j, i)[0];
//                if (jee > 0) {
//                    Log.i(TAG, "loadNewEye2 dc " + ij + " " + jee);
//                }
//                ij++;
//            }
//        }
        // ------------------CHECK------------------------------
        
        if (debugMode) {
            Core.rectangle(mRgba, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), FACE_RECT_COLOR, 3);
            Core.circle(mRgba, lEye, 9, FACE_RECT_COLOR, 4);
            Core.circle(mRgba, rEye, 9, FACE_RECT_COLOR, 4);
        }
    }
    
    public static double angleOfYx(Point p1, Point p2) {
        // NOTE: Remember that most math has the Y axis as positive above the X.
        // However, for screens we have Y as positive below. For this reason, 
        // the Y values are inverted to get the expected results.
        final double deltaX = (p1.y - p2.y);
        final double deltaY = (p2.x - p1.x);
        final double result = Math.toDegrees(Math.atan2(deltaY, deltaX)); 
        return (result < 0) ? (360d + result) : result;
    }
    
    private void saveBitmap(String toFile, Bitmap bmp) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(toFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    
    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }
}
