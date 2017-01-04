package ru.flightlabs.masks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaActionSound;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.CameraGLSurfaceView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL11;

import ru.flightlabs.masks.model.ImgLabModel;
import ru.flightlabs.masks.model.SimpleModel;
import ru.flightlabs.masks.model.primitives.Line;
import ru.flightlabs.masks.model.primitives.Triangle;
import ru.flightlabs.masks.renderer.Cube;

public class FdActivity2 extends Activity implements CvCameraViewListener2, CameraGLSurfaceView.CameraTextureListener {

    int programId;// shader program
    public static Mat glViewMatrix2;

    public static final String DIRECTORY_SELFIE = "Masks";
    //public static int counter;
    public static boolean makePhoto;
    public static boolean makePhoto2;
    public static boolean preMakePhoto;
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
    private volatile DetectionBasedTracker mNativeDetector;
    private boolean loadModel = false;
    private static final int[] resourceDetector = {R.raw.lbpcascade_frontalface, R.raw.haarcascade_frontalface_alt2, R.raw.my_detector};

    public static boolean debugMode = false;
    private boolean showEyes = true;
    private String[] mEysOnOff;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

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

    MatOfRect faces;
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

    private CameraGLSurfaceView mOpenCvCameraView;

    MediaActionSound sound = new MediaActionSound();
    boolean playSound = true;
    View borderCam;
    ImageView cameraButton;

    int availableProcessors = 1;

    String detectorName;

    ru.flightlabs.masks.model.primitives.Point[] pointsWas;
    Line[] lines;
    Triangle[] trianlges;
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

//                Display display = getWindowManager().getDefaultDisplay();
//                int width = display.getWidth();
//                int height = display.getHeight();
//                mOpenCvCameraView.setMaxFrameSize(width, width);

                    mOpenCvCameraView.setMaxCameraPreviewSize(640, 480);
                    mOpenCvCameraView.enableView();

                    try {
                        // load cascade file from application resources
                        Log.e(TAG, "findEyes onManagerConnected");
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        loadHaarModel(resourceDetector[0]);

                        final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
                        detectorName = prefs.getString(Settings.MODEL_PATH, Settings.MODEL_PATH_DEFAULT);
                        Log.e(TAG, "findEyes onManagerConnectedb " + detectorName);
                        if (mNativeDetector == null) {
                            // mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0, detectorName);
                        }

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

    public class LoadModel extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Log.i(TAG, "LoadModel doInBackground");
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File fModel = new File(cascadeDir, "testing_with_face_landmarks.xml");
            try {
                int res = resourceToFile(getResources().openRawResource(R.raw.monkey_68), fModel);
                Log.i(TAG, "LoadModel doInBackground111" + res + " " + fModel.length());
            } catch (NotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //Log.i(TAG, "LoadModel doInBackground1");
            SimpleModel modelFrom = new ImgLabModel(fModel.getPath());
            Log.i(TAG, "LoadModel doInBackground2");
            //pointsWas = modelFrom.getPointsWas();
            Log.i(TAG, "LoadModel doInBackground3");
            //lines = modelFrom.getLines();
            Log.i(TAG, "LoadModel doInBackground4");
            // load ready triangulation model from file
            List<Line> linesArr = new ArrayList<Line>();
            List<Triangle> triangleArr = new ArrayList<Triangle>();
            AssetManager assetManager = getAssets();
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
            lines = linesArr.toArray(new Line[0]);
//            Triangulation trianglation = new DelaunayTriangulation();
//            lines = trianglation.convertToTriangle(pointsWas, lines);

            Log.i(TAG, "LoadModel doInBackground5");
            // load triangles from model
            trianlges = triangleArr.toArray(new Triangle[0]);
            //trianlges = StupidTriangleModel.getTriagles(pointsWas, lines);
            Log.i(TAG, "LoadModel doInBackground6");

            if (!new File(detectorName).exists()) {
                Log.i(TAG, "LoadModel doInBackground66");
                try {
                    File ertModel = new File(cascadeDir, "ert_model.dat");
                    InputStream ims = assetManager.open("sp68.dat");
                    int bytes = resourceToFile(ims, ertModel);
                    ims.close();
                    detectorName = ertModel.getAbsolutePath();
                    Log.i(TAG, "LoadModel doInBackground66 " + detectorName + " " + ertModel.exists() + " " + ertModel.length() + " " + bytes);
                } catch (NotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Log.i(TAG, "LoadModel doInBackground667", e);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Log.i(TAG, "LoadModel doInBackground667", e);
                }
            }
            mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0, detectorName);
            Log.i(TAG, "LoadModel doInBackground7");
            return null;
        }




    }

    // TODO: лучше делать асинхронно
    // загрузка рисунка с альфа каналом + поворот для наложение в landscape
    private void loadNewEye(int index) throws NotFoundException, IOException {
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File newEyeFile = new File(cascadeDir, "temp.png");
        resourceToFile(getResources().openRawResource(eyesResources.getResourceId(index, 0)), newEyeFile);
        // load eye to Mat
        // используем загрузку через андроид, т.к. opencv ломает цвета
        Bitmap bmp = BitmapFactory.decodeFile(newEyeFile.getAbsolutePath());
//        Mat newEyeTmp2 = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
        currentMaskLandScaped= new Mat();
        Utils.bitmapToMat(bmp, currentMaskLandScaped, true);


        File fModel = new File(cascadeDir, "mask_landmarks.xml");
        try {
            resourceToFile(getResources().openRawResource(eyesResourcesLandmarks.getResourceId(index, 0)), fModel);
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

//        Log.i(TAG, "loadNewEye2 " + index + " " + newEyeTmp2.type() + " " + newEyeTmp2.channels());
//        Mat newEyeTmp = newEyeTmp2.t();
//        Core.flip(newEyeTmp2.t(), newEyeTmp, 0);
//        newEyeTmp2.release();
//        currentMaskLandScaped = newEyeTmp;
        cascadeDir.delete();
        Log.i(TAG, "loadNewEye " + currentMaskLandScaped.type() + " " + currentMaskLandScaped.channels());
    }

    public static int resourceToFile(InputStream is, File toFile) throws IOException {
        FileOutputStream os = new FileOutputStream(toFile);

        int res = 0;
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
            res += bytesRead;
        }
        os.flush();
        is.close();
        os.close();
        return res;
    }

    public FdActivity2() {
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

        mOpenCvCameraView.setCameraIndex(cameraIndex);
        mOpenCvCameraView.setCameraTextureListener(this);

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
                loadHaarModel(resourceDetector[haarModel % resourceDetector.length]);
            }
        });
        findViewById(R.id.make_face).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                makeNewFace = true;
            }
        });


    }

    void changeMask(int newMask) {
        newIndexEye = newMask;
    }

    private void loadHaarModel(int resource) {
        Log.i(TAG, "loadHaarModel " + getResources().getResourceName(resource));
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);

        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
        try {
            resourceToFile(getResources().openRawResource(resource), mCascadeFile);
        } catch (NotFoundException e) {
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

//        final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
//        detectorName = prefs.getString(Settings.MODEL_PATH, Settings.MODEL_PATH_DEFAULT);
//        Log.e(TAG, "findEyes onManagerConnectedb " + detectorName);
//        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0, detectorName);
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

        int vertexShaderId = ShaderUtils.createShader(this, GLES20.GL_VERTEX_SHADER, R.raw.vertex_shader);
//        int vertexShaderId = ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, "uniform mat4 uMVPMatrix;" +
//                "attribute vec4 vPosition;" +
//                "void main() {" +
//                "  gl_Position = uMVPMatrix * vPosition;" +
//                "}");
        int fragmentShaderId = ShaderUtils.createShader(this, GLES20.GL_FRAGMENT_SHADER, R.raw.fragment_shader);
        programId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);

        Log.i(TAG, "onCameraViewStarted");
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped");
        mGray.release();
        mRgba.release();
    }

    @Override
    public boolean onCameraTexture(int texIn, int texOut, int width, int height) {
        Log.i(TAG, "onCameraTexture " + width + " " + height);

        Log.i(TAG, "onCameraTexture1");
        ByteBuffer m_bbPixels = ByteBuffer.allocateDirect(width * height * 4);
        m_bbPixels.order(ByteOrder.LITTLE_ENDIAN);
        Log.i(TAG, "onCameraTexture2");
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GL11.GL_UNSIGNED_BYTE, m_bbPixels);
        m_bbPixels.rewind();
        Log.i(TAG, "onCameraTexture3");
        byte[] pixelValues = new byte[ width * height * 4 ];

        int iPix = 0;
        for( int i = 0; i < width * height; i++ )
        {

            int line        = height - 1 - (i / width); // flipping the image upside down
            int column      = i % width;
            int bufferIndex = ( line * width + column ) * 4;

            pixelValues[bufferIndex + 0 ] = (byte)(m_bbPixels.get(bufferIndex + 0) & 0xFF) ;
            pixelValues[bufferIndex + 1 ] = (byte)(m_bbPixels.get(bufferIndex + 1) & 0xFF);
            pixelValues[bufferIndex + 2 ] = (byte)(m_bbPixels.get(bufferIndex + 2) & 0xFF);
            pixelValues[bufferIndex + 3 ] = (byte)(m_bbPixels.get(bufferIndex + 3) & 0xFF);
            if (pixelValues[bufferIndex + 2 ] != 0 || pixelValues[bufferIndex + 1 ] != 0  || pixelValues[bufferIndex ] != 0) {
                //Log.i(TAG, "onCameraTexture3 " + pixelValues[bufferIndex + 2 ] + " " + pixelValues[bufferIndex + 1 ] + " " + pixelValues[bufferIndex + 0 ]);
            }
            iPix++;
            //Log.i(TAG, "onCameraTexture3 " + pixelValues[bufferIndex + 2 ]);
        }

        Mat pic = new Mat(height, width, CvType.CV_8UC4);
        Log.i(TAG, "onCameraTexture4 not null " + iPix);
        //pic.get(width, height, pixelValues);
        pic.put(0, 0, pixelValues);
        Log.i(TAG, "onCameraTexture5");
        Mat mGray = new Mat();
        Imgproc.cvtColor(pic, mGray, Imgproc.COLOR_RGBA2GRAY);

        if (mAbsoluteFaceSize == 0) {
            mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            //mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null) {
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO:
                        // objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
            }
        } else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null) {
                Log.e(TAG, "findEyes666 start detect");
                mNativeDetector.detect(mGray, faces);
            }
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        final boolean haveFace = facesArray.length > 0;


        if (makePhoto) {
            makePhoto = false;
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File newFile = new File(file, DIRECTORY_SELFIE);
            File fileJpg = new File(newFile, "eSelfie666.jpg");
            Bitmap bitmap = Bitmap.createBitmap(pic.cols(), pic.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(pic, bitmap);
            saveBitmap(fileJpg.getPath(), bitmap);
            bitmap.recycle();
        }
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(programId);

        int uColorLocation = GLES20.glGetUniformLocation(programId, "u_Color");
        GLES20.glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);

        FloatBuffer vertexData;
        float[] vertices = {
                -0.5f, -0.2f,
                0.0f, 0.2f,
                0.5f, -0.2f,
        };

        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);


        int aPositionLocation = GLES20.glGetAttribLocation(programId, "a_Position");
        vertexData.position(0);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT,
                false, 0, vertexData);
        GLES20.glEnableVertexAttribArray(aPositionLocation);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        GLES20.glFlush(); //?

        Log.i(TAG, "onCameraTexture " + pic.height() + " " + facesArray.length);


        return false;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Log.i(TAG, "onCameraFrame");
        return null;
    }


    private static Point orient(Point point, int width, int heigth) {
        return orient(point, 0, width, heigth);
    }

    private static Point orient(Point point, int orient, int width, int heigth) {
        if (true) {
            return point;
        }
        if (orient == 3) {
            return point;
        } else if (orient == 0) {
            return new Point(point.y, heigth - point.x);
        } else if (orient == 1) {
            return new Point(width - point.x, heigth - point.y);
        } else {
            return new Point(width - point.y, point.x);
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
