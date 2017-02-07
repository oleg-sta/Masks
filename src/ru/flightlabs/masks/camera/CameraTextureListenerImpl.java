package ru.flightlabs.masks.camera;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.CameraGLSurfaceView;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.DetectionBasedTracker;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.activity.FdActivity2;
import ru.flightlabs.masks.activity.Settings;
import ru.flightlabs.masks.renderer.Model;
import ru.flightlabs.masks.renderer.ShaderEffectHelper;
import ru.flightlabs.masks.utils.Decompress;
import ru.flightlabs.masks.utils.FileUtils;
import ru.flightlabs.masks.utils.OpenGlHelper;
import ru.flightlabs.masks.utils.OpencvUtils;
import ru.flightlabs.masks.utils.PhotoMaker;
import ru.flightlabs.masks.utils.PoseHelper;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 05.01.2017.
 */

public class CameraTextureListenerImpl implements CameraGLSurfaceView.CameraTextureListener {

    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;
    CompModel compModel;
    Context act;

    int iGlobTime = 0;

    ByteBuffer m_bbPixelsGrey;
    ByteBuffer m_bbPixels;
    Mat mRgba;
    Mat mGray;
    Mat mGrayprogram;

    int frameCount;
    long timeStart;
    double lastCount = 0.5f;

    private static final String TAG = "CameraTextureL_class";
    private int[] iFBO = null;//{0};

    private int[] texGray, grayFbo;
    int programGrey;

    ShaderEffectHelper shaderHelper;

    // 3d
    PoseHelper poseHelper;

    public CameraTextureListenerImpl(Activity act, CompModel compModel) {
        this.act = act;
        this.compModel = compModel;
    }

    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();
        mGrayprogram = new Mat(height, (4 * width + 3) / 4, CvType.CV_8UC1);

        //m_bbPixels = ByteBuffer.allocateDirect(width * height * 4);
        // workaround due to https://code.google.com/p/android/issues/detail?id=80064
        m_bbPixels = ByteBuffer.wrap(new byte[width * height * 4]);
        m_bbPixels.order(ByteOrder.LITTLE_ENDIAN);

        m_bbPixelsGrey = ByteBuffer.wrap(new byte[(4 * width + 3) / 4 * height]);
        m_bbPixelsGrey.order(ByteOrder.LITTLE_ENDIAN);

        poseHelper = new PoseHelper(compModel);
        poseHelper.init(act, width, height);

        shaderHelper = new ShaderEffectHelper(act, FdActivity2.eyesResources);
        shaderHelper.init();
        Log.i(TAG, "onCameraViewStarted");
    }

    private void initFboGray(int width, int height) {
        texGray = new int[]{0};
        GLES20.glGenTextures(1, texGray, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texGray[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, (width + 3) / 4, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        grayFbo = new int[]{0};
        GLES20.glGenFramebuffers(1, grayFbo, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, grayFbo[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texGray[0], 0);

        int vertexShaderId = ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(act.getAssets(), "shaders/vss.glsl"));
        int fragmentShaderId = ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(act.getAssets(), "shaders/fss_grey.glsl"));
        programGrey = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);
    }


    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped");
        mGray.release();
    }

    public boolean onCameraTexture(int texIn, int texOut, int width, int height) {
        long time = System.currentTimeMillis();
        iGlobTime++;
        if (iGlobTime % 100 == 0) {
            iGlobTime = 0;
        }
        Log.i(TAG, "onCameraTexture");
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

        // screen to Mat
        if (grayFbo == null) {
            initFboGray(width, height);
        }
        Mat findGray = null;
        //if (Settings.debugMode)
        if (!Settings.debugMode && false) {
            // strange, but it's slower then color!!!
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, grayFbo[0]);
            GLES20.glViewport(0, 0, (width + 3) / 4, height);
            int vPos = GLES20.glGetAttribLocation(programGrey, "vPosition");
            int vTex = GLES20.glGetAttribLocation(programGrey, "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            ShaderEffectHelper.shaderEfffect2d(new Point(0, 0), new Point(width, height), texIn, programGrey, vPos, vTex);
            GLES20.glReadPixels(0, 0, (width + 3) / 4, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, m_bbPixelsGrey);
            m_bbPixelsGrey.rewind();
            mGrayprogram.put(0, 0, m_bbPixelsGrey.array());
            findGray = mGrayprogram;
        } else {
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, m_bbPixels);
            m_bbPixels.rewind();
            mRgba.put(0, 0, m_bbPixels.array());
            Core.flip(mRgba, mRgba, 0);
            Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
            findGray = mGray;
        }

        if (mAbsoluteFaceSize == 0) {
            mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            //mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        int indexEye = Static.newIndexEye;
        final Effect effect = shaderHelper.effectsMap.get(indexEye);
        Log.i(TAG, "indexEye " + indexEye + " effect " + effect.programId + " " + effect.model3dName + " " + effect.textureName);

        PoseHelper.PoseResult poseResult = poseHelper.findShapeAndPose(findGray, mAbsoluteFaceSize, mRgba, width, height, effect.needBlendShape, shaderHelper.model, act);

        boolean makeAfterPhoto = false;
        // this photo for debug purpose
        if (Static.makePhoto) {
            Static.makePhoto = false;
            makeAfterPhoto= true;
            PhotoMaker.makePhoto(mRgba, act);
            Mat c = new Mat();
            Imgproc.cvtColor(mGrayprogram, c, Imgproc.COLOR_GRAY2RGBA);
            PhotoMaker.makePhoto(c, act);
        }

        // temporary for debug purposes or maby for simple effects
        if (Settings.debugMode) {
            Core.flip(mRgba, mRgba, 0);
            mRgba.get(0, 0, m_bbPixels.array());

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
        }


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

        // shader effect
        shaderHelper.makeShader(indexEye, poseResult, width, height, texIn, time, iGlobTime);
          // shader effect

        if (makeAfterPhoto) {
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, m_bbPixels);
            m_bbPixels.rewind();
            //mRgba.get(width, height, pixelValues);
            mRgba.put(0, 0, m_bbPixels.array());
            Core.flip(mRgba, mRgba, 0);
            PhotoMaker.makePhoto(mRgba, act);
            // save 3d model
//            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
//            File newFile = new File(file, Settings.DIRECTORY_SELFIE);
//            final File fileJpg = new File(newFile, "outModel.obj");
//            model.saveModel(fileJpg.getPath());
        }


        return !Static.drawOrigTexture;
    }
}
