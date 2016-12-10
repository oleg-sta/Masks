package ru.flightlabs.masks.renderer;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.flightlabs.masks.R;

/**
 * Created by sov on 10.12.2016.
 */

public class MyGLRenderer2 implements GLSurfaceView.Renderer {

    FloatBuffer mVertexBuffer;
    //FloatBuffer mTextureBuffer;
    FloatBuffer mColorBuffer;

    FloatBuffer mNormalBuffer;
    ShortBuffer mIndices;

    private float mCubeRotation;

    Activity activity;
    Cube c;


    private static final String TAG = "MyGLRenderer2_class";

    public MyGLRenderer2(Activity activity) {
        this.activity = activity;
        c = new Cube();

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

        Model model = new Model(R.raw.untitled,
                activity);

        mVertexBuffer = model.getVertices();
        //mTextureBuffer = model.getTexCoords();
        mNormalBuffer = model.getNormals();
        mIndices = model.getIndices();
    }

    public void onDrawFrame(GL10 gl) {
        Log.i(TAG, "onDrawFrame");
        /*
        // Redraw background color
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, mVertexBuffer);
        gl.glColor4f(1, 1, 1, 0.5f);
        //gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        //gl.glEnable(GL10.GL_TEXTURE_2D); // workaround bug 3623
        //gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, mTextureBuffer);
        //gl.glNormalPointer(1, GL10.GL_FIXED, mNormalBuffer);
        //gl.glColor4f(1, 1, 1, 0.5f);
        // gl.glColor4f(1, 1, 1, 1);
        // gl.glNormal3f(0, 0, 1);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVertexBuffer.limit());
*/

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        gl.glTranslatef(0.0f, 0.0f, -10.0f);
        gl.glRotatef(mCubeRotation, 1.0f, 1.0f, 1.0f);

        gl.glFrontFace(GL10.GL_CW);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, c.mColorBuffer);
//        gl.glColor4f(1, 1, 1, 0.5f);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_SHORT,
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
        GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
}
