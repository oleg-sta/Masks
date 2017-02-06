package ru.flightlabs.masks.activity;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import ru.flightlabs.masks.FastView;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.renderer.MyGLRenderer2;
import ru.flightlabs.masks.renderer.TestRenderer;

/**
 * Created by sov on 06.02.2017.
 */

/**
 * experimental fast activity with getting frame from camera on put it in GlView
 */
public class ActivityFast extends Activity {

    TypedArray eyesResources;
    public static GLSurfaceView gLSurfaceView;

    private SurfaceHolder mHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fast_view);

        FastView sv = (FastView) findViewById(R.id.fd_fase_surface_view);
        mHolder = sv.getHolder();
        mHolder.addCallback(sv);

        eyesResources = getResources().obtainTypedArray(R.array.masks_png);

        gLSurfaceView = (GLSurfaceView)findViewById(R.id.fd_glsurface);
        gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        gLSurfaceView.setZOrderOnTop(true);
        TestRenderer meRender = new TestRenderer(this, eyesResources);
        gLSurfaceView.setEGLContextClientVersion(2);
        gLSurfaceView.setRenderer(meRender);
        gLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    }
}
