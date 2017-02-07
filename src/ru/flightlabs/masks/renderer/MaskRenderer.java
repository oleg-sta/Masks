package ru.flightlabs.masks.renderer;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.camera.FastCameraView;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.camera.CameraHelper;
import ru.flightlabs.masks.utils.FileUtils;
import ru.flightlabs.masks.utils.PoseHelper;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 06.02.2017.
 */

public class MaskRenderer implements GLSurfaceView.Renderer {
    final TypedArray eyesResources;

    int widthSurf;
    int heightSurf;

    int iGlobTime = 0;
    Context context;
    public static byte[] bufferFromCamera;

    int programNv21ToRgba;
    int texFromCamera[] = new int[2];

    int texRgba[] = new int[1];
    int fboRgba[] = new int[1];

    ByteBuffer bufferY;
    ByteBuffer bufferUV;

    Mat greyTemp;
    Mat mRgbaDummy;
    CompModel compModel;
    PoseHelper poseHelper;
    ShaderEffectHelper shaderHelper;

    private static final String TAG = "MaskRenderer";

    public MaskRenderer(Context context, TypedArray eyesResources, CompModel compModel) {
        this.context = context;
        this.eyesResources = eyesResources;
        this.compModel = compModel;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        initShaders();
        GLES20.glGenTextures(2, texFromCamera, 0);
        Log.i(TAG, "onSurfaceCreated2 " + texFromCamera[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texFromCamera[0]);
        // FIXME use pixel to pixel, not average neighbours
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        Log.i(TAG, "onSurfaceCreated2 " + texFromCamera[1]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texFromCamera[1]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        shaderHelper = new ShaderEffectHelper(context, eyesResources);
        shaderHelper.init();
    }

    private void initShaders() {
        int vertexShaderId = ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/vss.glsl"));
        int fragmentShaderId = ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/fss_n21_to_rgba.glsl"));
        programNv21ToRgba = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);

    }

    public void onDrawFrame(GL10 gl) {
        Log.i(TAG, "onDrawFrame");
        long time = System.currentTimeMillis();
        iGlobTime++;
        if (iGlobTime % 100 == 0) {
            iGlobTime = 0;
        }
        int mCameraWidth = CameraHelper.mCameraWidth;
        int mCameraHeight = CameraHelper.mCameraHeight;

        if (bufferFromCamera != null && Static.libsLoaded) {

            PoseHelper.PoseResult poseResult = null;
            synchronized (FastCameraView.class) {
                if (greyTemp == null) {
                    greyTemp = new Mat(mCameraHeight, mCameraWidth, CvType.CV_8UC1);
                    mRgbaDummy = new Mat(mCameraHeight, mCameraWidth, CvType.CV_8UC4);
                }
                greyTemp.put(0, 0, bufferFromCamera);


                int cameraSize = mCameraWidth * mCameraHeight;
                if (bufferY == null) {
                    bufferY = ByteBuffer.allocateDirect(cameraSize);
                    bufferUV = ByteBuffer.allocateDirect(cameraSize / 2);
                }
                bufferY.put(bufferFromCamera, 0, cameraSize);
                bufferY.position(0);
                bufferUV.put(bufferFromCamera, cameraSize, cameraSize / 2);
                bufferUV.position(0);
                Log.i(TAG, "onDrawFrame2 " + bufferFromCamera[0]);
                Log.i(TAG, "onDrawFrame2 " + bufferY.limit());
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texFromCamera[0]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mCameraWidth, (int) (mCameraHeight), 0,
                        GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, bufferY);
                GLES20.glFlush();
                Log.i(TAG, "onDrawFrame2 " + bufferY.limit());
                //bufferY.position(heightSurf * widthSurf);
                Log.i(TAG, "onDrawFrame2 " + bufferY.limit());
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texFromCamera[1]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mCameraWidth, (int) (mCameraHeight * 0.5), 0,
                        GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, bufferUV);
                GLES20.glFlush();
                Log.i(TAG, "onDrawFrame2 " + bufferY.limit());
                Log.i(TAG, "onDrawFrame3");
            }
            // if back camera
            Mat grey = greyTemp.t();
            if (!FastCameraView.cameraFacing) {
                Core.flip(grey, grey, 1);
            } else {
                Core.flip(grey, grey, -1);
            }

            int mAbsoluteFaceSize = Math.round((int) (mCameraWidth * 0.33));
            boolean shapeBlendsd = shaderHelper.effectsMap.get(Static.newIndexEye).needBlendShape;
            poseResult = poseHelper.findShapeAndPose(grey, mAbsoluteFaceSize, mRgbaDummy, widthSurf, heightSurf, shapeBlendsd, shaderHelper.model, context);

            // convert from NV21 to RGBA
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboRgba[0]);
            GLES20.glViewport(0, 0, widthSurf, heightSurf);
            GLES20.glUseProgram(programNv21ToRgba);
            int vPos = GLES20.glGetAttribLocation(programNv21ToRgba, "vPosition");
            int vTex = GLES20.glGetAttribLocation(programNv21ToRgba, "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            int ufacing = GLES20.glGetUniformLocation(programNv21ToRgba, "u_facing");
            GLES20.glUniform1i(ufacing, FastCameraView.cameraFacing ? 1 : 0);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(programNv21ToRgba, "cameraWidth"), mCameraWidth);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(programNv21ToRgba, "cameraHeight"), mCameraWidth);
            Log.i(TAG, "onDrawFrame5");
            ShaderEffectHelper.shaderEfffect2d(new Point(0, 0), new Point(widthSurf, heightSurf), texFromCamera[0], programNv21ToRgba, vPos, vTex, texFromCamera[1]);

            Log.i(TAG, "onDrawFrame6");

            // draw effect on rgba
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, widthSurf, heightSurf);
            shaderHelper.makeShader(Static.newIndexEye, poseResult, widthSurf, heightSurf, texRgba[0], time, iGlobTime);
            Log.i(TAG, "onDrawFrame4");
        }

    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged " + width + " " + height);
        GLES20.glGenTextures(1, texRgba, 0);
        Log.i(TAG, "onSurfaceCreated3 " + texRgba[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texRgba[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLES20.glGenFramebuffers(1, fboRgba, 0);
        Log.i(TAG, "onSurfaceCreated4 " + fboRgba[0]);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboRgba[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texRgba[0], 0);

        Log.i(TAG, " fbo status " + GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER));
        Log.i(TAG, "onSurfaceCreated5");
        poseHelper = new PoseHelper(compModel);
        poseHelper.init(context, width, height); // FIXME

        this.widthSurf = width;
        this.heightSurf = height;
    }

}
