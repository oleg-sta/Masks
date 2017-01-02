package ru.flightlabs.masks.renderer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.opencv.core.Mat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.flightlabs.masks.FdActivity;
import ru.flightlabs.masks.R;

/**
 * Created by sov on 10.12.2016.
 */

public class MyGLRenderer2 implements GLSurfaceView.Renderer {

    public int currText = -1;
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

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_NICEST);

        model = new Model(R.raw.for_android_test,
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

        Bitmap mBitmap = BitmapFactory.decodeResource(activity.getResources(), FdActivity.eyesResources.getResourceId(FdActivity.newIndexEye, 0));
        currText = FdActivity.newIndexEye;
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
        mBitmap.recycle();

        gl.glEnable(GL10.GL_TEXTURE_2D);
    }

    private void changeTexture(GL10 gl, int resourceID) {
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
        // Load the bitmap into the bound texture.
        Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), resourceID);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }

    public void onDrawFrame(GL10 gl) {
//        if (true) return;
        Log.i(TAG, "onDrawFrame");

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        if (!FdActivity.debugMode) return;
        Mat s = FdActivity.glViewMatrix2;
        if (s == null) return;
        if (FdActivity.newIndexEye != currText) {
            currText = FdActivity.newIndexEye;
            changeTexture(gl, FdActivity.eyesResources.getResourceId(FdActivity.newIndexEye, 0));
        }


        if (s != null) {
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

        if (FdActivity.makePhoto2) {
            FdActivity.makePhoto2 = false;
            WindowManager w = activity.getWindowManager();
            Display d = w.getDefaultDisplay();
            int width = d.getWidth();
            int height = d.getHeight();
            saveScreenshot(gl, width, height);
        }

        mCubeRotation -= 0.15f;
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        // FIXME angle need to be calculated
        GLU.gluPerspective(gl, 80.0f, (float)width / (float)height, 0.01f, 300.0f);
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    // FIXME doesn't work
    private void saveScreenshot(GL10 gl, int width, int height) {
        Log.i(TAG, "saving start ");
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File newFile = new File(file, FdActivity.DIRECTORY_SELFIE);
        File fileJpg = new File(newFile, "eSelfie" + new Random().nextInt(1000) + ".png");


        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        gl.glReadPixels(0, 0, width, height,
                GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buf);
        buf.rewind();
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(fileJpg));
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
            bmp.recycle();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "saving end " );
    }
}
