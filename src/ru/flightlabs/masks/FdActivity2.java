package ru.flightlabs.masks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.MediaActionSound;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class FdActivity2 extends Activity implements CvCameraViewListener2, CameraGLSurfaceView.CameraTextureListener {

    private volatile DetectionBasedTracker mNativeDetector;
    private int vPos;
    private int vTex;

    CompModel compModel;
    int programId;// shader program
    public static Mat glViewMatrix2;

    public static final String DIRECTORY_SELFIE = "Masks";
    //public static int counter;
    public static boolean makePhoto;
    public static boolean makePhoto2;
    public static boolean preMakePhoto;

    private static final String TAG = "FdActivity2_class";

    private boolean loadModel = false;
    private static final int[] resourceDetector = {R.raw.lbpcascade_frontalface, R.raw.haarcascade_frontalface_alt2, R.raw.my_detector};

    public static boolean debugMode = false;

    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;

    public static TypedArray eyesResources;
    TypedArray eyesResourcesSmall;
    TypedArray eyesResourcesLandmarks;

    int currentIndexEye = -1;
    public static int newIndexEye = 0;

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

    boolean videoWriterStart;

    ByteBuffer m_bbPixels;
    Mat pic;
    Mat mGray;

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

    public FdActivity2() {
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
        debugMode = prefs.getBoolean(Settings.DEBUG_MODE, true);
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

//        int vertexShaderId = ShaderUtils.createShader(this, GLES20.GL_VERTEX_SHADER, R.raw.vertex_shader);
//        int fragmentShaderId = ShaderUtils.createShader(this, GLES20.GL_FRAGMENT_SHADER, R.raw.fragment_shader);
        int vertexShaderId = ShaderUtils.createShader(this, GLES20.GL_VERTEX_SHADER, R.raw.vss);
        int fragmentShaderId = ShaderUtils.createShader(this, GLES20.GL_FRAGMENT_SHADER, R.raw.fss2);
        programId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);
        bindData(height, width);

        Log.i(TAG, "onCameraViewStarted");
        //mGray = new Mat();
        //mRgba = new Mat();
    }

    private void bindData(int width, int height) {
        vPos = GLES20.glGetAttribLocation(programId, "vPosition");
        vTex  = GLES20.glGetAttribLocation(programId, "vTexCoord");
        GLES20.glEnableVertexAttribArray(vPos);
        GLES20.glEnableVertexAttribArray(vTex);

        int matrixMvp = GLES20.glGetUniformLocation(programId, "uMVP");
        float[] matrix = new float[16];
//        matrix[0] = 1;
//        matrix[3] = - 1;
//        matrix[5] = 1;
//        matrix[7] = - 1;
//        matrix[10] = - 1;
//        matrix[7] = - 1;
//        matrix[15] = 1;

        matrix[0] = 1;
        matrix[5] = 1;
        matrix[10] = 1;
        matrix[15] = 1;

        GLES20.glUniformMatrix4fv(matrixMvp, 1, false, matrix, 0);
    }


    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped");
        mGray.release();
    }

    private Point convertToGl(Point old, int width, int height) {
        return new Point(old.x / width, 1 - old.y / height);
    }
    private int[] iFBO = null;//{0};
    @Override
    public boolean onCameraTexture(int texIn, int texOut, int width, int height) {

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
        // FIXME CameraRenderer and CameraGl... should bi fixed by of sizes of camera and FBO
        int t = width;
        width = height;
        height = t;

        Log.i(TAG, "onCameraTexture " + width + " " + height);
        Log.i(TAG, "onCameraTexture1");
        if (m_bbPixels == null) {
            m_bbPixels = ByteBuffer.allocateDirect(width * height * 4);
            m_bbPixels.order(ByteOrder.LITTLE_ENDIAN);
        }
        Log.i(TAG, "onCameraTexture2");
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, m_bbPixels);
        m_bbPixels.rewind();
        Log.i(TAG, "onCameraTexture3" + m_bbPixels.array().length);

        if (pic == null) {
            pic = new Mat(height, width, CvType.CV_8UC4);
            mGray = new Mat();
        }
        //pic.get(width, height, pixelValues);
        pic.put(0, 0, m_bbPixels.array());
        Core.flip(pic, pic, 0);
        Log.i(TAG, "onCameraTexture5");
        Imgproc.cvtColor(pic, mGray, Imgproc.COLOR_RGBA2GRAY);

        if (mAbsoluteFaceSize == 0) {
            mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            //mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = compModel.findFaces(mGray, mAbsoluteFaceSize);
        Rect[] facesArray = faces.toArray();
        final boolean haveFace = facesArray.length > 0;
        Log.i(TAG, "onCameraTexture5 " + haveFace);
        Point center = new Point(0.5, 0.5);
        Point[] foundEyes = null;
        if (haveFace) {
            if (debugMode) {
                Imgproc.rectangle(pic, facesArray[0].tl(), facesArray[0].br(), new Scalar(255, 10 ,10), 3);
            }
            center = convertToGl(new Point((2 * facesArray[0].x + facesArray[0].width) / 2.0, (2 * facesArray[0].y + facesArray[0].height) / 2.0), width, height);
            if (mNativeDetector != null) {
                foundEyes = mNativeDetector.findEyes(mGray, facesArray[0]);
                if (debugMode) {
                    for (Point p : foundEyes) {
                        Imgproc.circle(pic, p, 2, new Scalar(255, 10, 10));
                    }
                }
                center = convertToGl(new Point((foundEyes[36].x + foundEyes[39].x) / 2.0, (foundEyes[36].y + foundEyes[39].y) / 2.0), width, height);
            }
        }


        // temporary for debug purposes or maby for simple effects
        Core.flip(pic, pic, 0);
        pic.get(0, 0, m_bbPixels.array());
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIn);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,         // Type of texture
                0,                   // Pyramid level (for mip-mapping) - 0 is the top level
                GLES20.GL_RGBA,              // Internal colour format to convert to
                width,          // Image width  i.e. 640 for Kinect in standard mode
                height,          // Image height i.e. 480 for Kinect in standard mode
                0,                   // Border width in pixels (can either be 1 or 0)
                GLES20.GL_RGBA,              // Input image format (i.e. GL_RGB, GL_RGBA, GL_BGR etc.)
                GLES20.GL_UNSIGNED_BYTE,    // Image data type
                m_bbPixels);        // The actual image data itself

        GLES20.glFlush();
        GLES20.glFinish();
        if (iFBO == null) {
            //int hFBO;
            iFBO = new int[]{0};
            GLES20.glGenFramebuffers(1, iFBO, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, iFBO[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texOut, 0);
        } else {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, iFBO[0]);
        }
        GLES20.glViewport(0, 0, width, height);

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
        GLES20.glUseProgram(programId);
        int uColorLocation = GLES20.glGetUniformLocation(programId, "u_Color");
        GLES20.glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);

        int uCenter = GLES20.glGetUniformLocation(programId, "uCenter");
        GLES20.glUniform2f(uCenter, (float)center.x, (float)center.y);

        FloatBuffer vertexData;
        float[] vertices = {
                -1, -1,
                -1,  1,
                1, -1,
                1,  1
        };
//        vertices = new float[]{
//                0, 0,
//                width,  0,
//                width, height,
//                0,  height
//        };
        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);

        FloatBuffer texData;
        float[] tex = {
                0,  0,
                0,  1,
                1,  0,
                1,  1
        };
        texData = ByteBuffer
                .allocateDirect(tex.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        texData.put(tex);

        vertexData.position(0);
        GLES20.glVertexAttribPointer(vPos, 2, GLES20.GL_FLOAT, false, 0, vertexData);
        texData.position(0);
        GLES20.glVertexAttribPointer(vTex,  2, GLES20.GL_FLOAT, false, 0, texData);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIn);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "sTexture"), 0);

        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texOut); //?

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush(); //?

        Log.i(TAG, "onCameraTexture " + pic.height() + " " + facesArray.length);


        return true;
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
}