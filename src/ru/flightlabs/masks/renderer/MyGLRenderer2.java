package ru.flightlabs.masks.renderer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.flightlabs.masks.FdActivity;
import ru.flightlabs.masks.R;

/**
 * Created by sov on 10.12.2016.
 */

public class MyGLRenderer2 implements GLSurfaceView.Renderer {

    private int[] textures = new int[1];
    FloatBuffer mVertexBuffer;
    FloatBuffer mTextureBuffer;
    FloatBuffer mColorBuffer;

    FloatBuffer mNormalBuffer;
    ShortBuffer mIndices;
    int indicesCount;

    private float mCubeRotation;
    public Model model;

    Activity activity;


    private static final String TAG = "MyGLRenderer2_class";

    public MyGLRenderer2(Activity activity) {
        this.activity = activity;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background frame color
        //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);

        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_NICEST);

        model = new Model(R.raw.head12,
                activity);

        mVertexBuffer = model.getVertices();
        mTextureBuffer = model.getTexCoords();
        mNormalBuffer = model.getNormals();
        mIndices = model.getIndices();
        indicesCount = model.getIndicesCount();

        //Generate one texture pointer...
        gl.glGenTextures(1, textures, 0);
        //...and bind it to our array
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        //Create Nearest Filtered Texture
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        //Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

        Bitmap mBitmap = BitmapFactory.decodeResource(activity.getResources(), R.raw.m1_2);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
        mBitmap.recycle();

        gl.glEnable(GL10.GL_TEXTURE_2D);
    }

    public void onDrawFrame(GL10 gl) {
        if (FdActivity.glViewMatrix2 == null) return;
        if (!FdActivity.debugMode) return;
        if (true) return;
        Log.i(TAG, "onDrawFrame");

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();


        if (FdActivity.glViewMatrix2 != null) {
            Mat s = FdActivity.glViewMatrix2;
            float[] matrixArray = new float[16];
            for(int row=0; row<4; ++row)
            {
                for(int col=0; col<4; ++col)
                {
                    matrixArray[row * 4 + col] = (float)s.get(row, col)[0];
                }
            }
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadMatrixf(matrixArray, 0);
        }
        //GLU.gluLookAt(gl, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

//        gl.glTranslatef(0.0f, 0.0f, -10.0f);
        //gl.glRotatef(mCubeRotation, 1.0f, 1.0f, 1.0f);

        gl.glFrontFace(GL10.GL_CW);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
//        gl.glColor4f(0.0f, 1.0f, 0.0f, 0.25f);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glDrawElements(GL10.GL_TRIANGLES, indicesCount, GL10.GL_UNSIGNED_SHORT,
                mIndices);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

        gl.glLoadIdentity();


        mCubeRotation -= 0.15f;
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, -45.0f, (float)width / (float)height, 0.001f, 100.0f);
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
}
