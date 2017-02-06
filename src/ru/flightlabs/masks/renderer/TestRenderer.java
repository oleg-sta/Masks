package ru.flightlabs.masks.renderer;

import android.app.Activity;
import android.content.res.TypedArray;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import org.opencv.core.Point;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.flightlabs.masks.utils.FileUtils;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 06.02.2017.
 */

public class TestRenderer implements GLSurfaceView.Renderer{
    final TypedArray eyesResources;

    Activity activity;
    public static byte[] buffer;

    int program;
    int texDraw[] = new int[1];

    ByteBuffer buffer2;

    int width, height;



    private static final String TAG = "TestRenderer";

    public TestRenderer(Activity activity, TypedArray eyesResources) {
        this.activity = activity;
        this.eyesResources = eyesResources;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        initShaders();
        GLES20.glGenTextures(1, texDraw, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texDraw[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

    }

    private void initShaders() {
        int vertexShaderId = ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(activity.getAssets(), "shaders/vss.glsl"));
        int fragmentShaderId = ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(activity.getAssets(), "shaders/fss.glsl"));
        program = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);

    }

    public void onDrawFrame(GL10 gl) {
        Log.i(TAG, "onDrawFrame");
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glViewport(0, 0, width, height);

        if (buffer != null) {
            if (buffer2 == null) {
                buffer2 = ByteBuffer.allocateDirect(buffer.length);
            }
            buffer2.put(buffer);
            buffer2.position(0);
            Log.i(TAG, "onDrawFrame2 " + buffer[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 960 / 4, (int)(540 * 1.5), 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer2);
            Log.i(TAG, "onDrawFrame3");
            int vPos = GLES20.glGetAttribLocation(program, "vPosition");
            int vTex = GLES20.glGetAttribLocation(program, "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            shaderEfffect2d(texDraw[0], program, vPos, vTex);
            Log.i(TAG, "onDrawFrame4");
        }

    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged " + width + " " + height);
        this.width = width;
        this.height = height;
    }

    private void shaderEfffect2d(int texIn, int programId, int poss, int texx) {
        GLES20.glUseProgram(programId);

        FloatBuffer vertexData;
        float[] vertices = {
                -1, -1,
                -1,  1,
                1, -1,
                1,  1
        };

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
        GLES20.glVertexAttribPointer(poss, 2, GLES20.GL_FLOAT, false, 0, vertexData);
        texData.position(0);
        GLES20.glVertexAttribPointer(texx,  2, GLES20.GL_FLOAT, false, 0, texData);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIn);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "sTexture"), 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush(); //?
        //GLES20.glFinish();
    }

}
