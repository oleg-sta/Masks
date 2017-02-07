package ru.flightlabs.masks.renderer;

import android.app.Activity;
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
import ru.flightlabs.masks.FastView;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.utils.FileUtils;
import ru.flightlabs.masks.utils.PoseHelper;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 06.02.2017.
 */

public class TestRenderer implements GLSurfaceView.Renderer {
    final TypedArray eyesResources;

    Activity activity;
    public static byte[] buffer;

    int programNv21ToRgba;
    int texDraw[] = new int[1];

    int texDraw2[] = new int[1];
    int texFbo2[] = new int[1];

    ByteBuffer buffer2;

    int width, height;

    Mat greyTemp;
    Mat mRgbaDummy;
    CompModel compModel;
    PoseHelper poseHelper;
    ShaderEffectHelper shaderHelper;

    private static final String TAG = "TestRenderer";

    public TestRenderer(Activity activity, TypedArray eyesResources, CompModel compModel) {
        this.activity = activity;
        this.eyesResources = eyesResources;
        this.compModel = compModel;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        initShaders();
        GLES20.glGenTextures(1, texDraw, 0);
        Log.i(TAG, "onSurfaceCreated2 " + texDraw[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texDraw[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);


        GLES20.glGenTextures(1, texDraw2, 0);
        Log.i(TAG, "onSurfaceCreated3 " + texDraw2[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texDraw2[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLES20.glGenFramebuffers(1, texFbo2, 0);
        Log.i(TAG, "onSurfaceCreated4 " + texFbo2[0]);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, texFbo2[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texDraw2[0], 0);

        Log.i(TAG, " fbo status " + GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER));
        Log.i(TAG, "onSurfaceCreated5");
        poseHelper = new PoseHelper(compModel);
        poseHelper.init(activity, width, height); // FIXME
        shaderHelper = new ShaderEffectHelper(activity, eyesResources);
        shaderHelper.init();
    }

    private void initShaders() {
        int vertexShaderId = ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(activity.getAssets(), "shaders/vss.glsl"));
        int fragmentShaderId = ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(activity.getAssets(), "shaders/fss.glsl"));
        programNv21ToRgba = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);

    }

    public void onDrawFrame(GL10 gl) {
        Log.i(TAG, "onDrawFrame");

        // init after opencv init

        //GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //GLES20.glViewport(0, 0, width, height);
        //if (true) return;

        if (buffer != null) {

            PoseHelper.PoseResult poseResult = null;
            // TODO find face and ...
            if (Static.libsLoaded) {
                if (greyTemp == null) {
                    greyTemp = new Mat(width, height, CvType.CV_8UC1);
                    mRgbaDummy = new Mat(width, height, CvType.CV_8UC4);
                }
                greyTemp.put(0, 0, buffer);

                // if back camera
                Mat grey = greyTemp.t();
                if (!FastView.cameraFacing) {
                    Core.flip(grey, grey, 1);
                } else {
                    Core.flip(grey, grey, -1);
                }

                //PhotoMaker.makePhoto(grey, activity);

                int mAbsoluteFaceSize = Math.round((int)(height * 0.33));
                boolean shapeBlendsd = shaderHelper.effectsMap.get(Static.newIndexEye).needBlendShape;
                poseResult = poseHelper.findShapeAndPose(grey, mAbsoluteFaceSize, mRgbaDummy, width, height, shapeBlendsd, shaderHelper.model, activity);

            }

            if (buffer2 == null) {
                buffer2 = ByteBuffer.allocateDirect(buffer.length);
            }
            buffer2.put(buffer);
            buffer2.position(0);
            Log.i(TAG, "onDrawFrame2 " + buffer[0]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texDraw[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, height / 4, (int)(width * 1.5), 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer2);
            GLES20.glFlush();
            Log.i(TAG, "onDrawFrame3");

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, texFbo2[0]);
            GLES20.glViewport(0, 0, width, height);
            GLES20.glUseProgram(programNv21ToRgba);
            int vPos = GLES20.glGetAttribLocation(programNv21ToRgba, "vPosition");
            int vTex = GLES20.glGetAttribLocation(programNv21ToRgba, "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            int ufacing = GLES20.glGetUniformLocation(programNv21ToRgba, "u_facing");
            GLES20.glUniform1i(ufacing, FastView.cameraFacing ? 1 : 0);

            Log.i(TAG, "onDrawFrame5");
            // convert from NV21 to RGBA
            ShaderEffectHelper.shaderEfffect2d(new Point(0, 0), new Point(width, height), texDraw[0], programNv21ToRgba, vPos, vTex);

            Log.i(TAG, "onDrawFrame6");

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, width, height);
            //GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            shaderHelper.makeShader(Static.newIndexEye, poseResult, width, height, texDraw2[0], 0, 0);

            // TODO shader...
            Log.i(TAG, "onDrawFrame4");
        }

    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged " + width + " " + height);
        this.width = width;
        this.height = height;
    }

    // TODO somethion to do with init size
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
