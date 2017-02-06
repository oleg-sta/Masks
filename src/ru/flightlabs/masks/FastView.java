package ru.flightlabs.masks;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ru.flightlabs.masks.activity.ActivityFast;
import ru.flightlabs.masks.renderer.TestRenderer;

/**
 * Created by sov on 06.02.2017.
 */

public class FastView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private byte mBuffer[];
    private static final String TAG = "FastView";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public FastView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCamera = Camera.open();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.i(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        Log.d(TAG, "surfaceChanged " + format + " " + w + " " + h);
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        Camera.Parameters params = mCamera.getParameters();
        Log.d(TAG, "preview format " + params.getPreviewFormat());
        List<int[]> s = params.getSupportedPreviewFpsRange();
        for (int[] sd : s) {
            Log.d(TAG, "preview format " + Arrays.toString(sd));
        }
        int[] s2 = new int[2];
        params.getPreviewFpsRange(s2);
        Log.d(TAG, "preview format " + Arrays.toString(s2));
        params.setPreviewFormat(ImageFormat.NV21);
        params.setPreviewSize(960, 540);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !android.os.Build.MODEL.equals("GT-I9100"))
            params.setRecordingHint(true);
        List<String> FocusModes = params.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
        {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        params.setPreviewFpsRange( 30000, 30000 );
        mCamera.setParameters(params);

        int size = w * h;
        size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
        mBuffer = new byte[size];
        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            //mCamera.setPreviewDisplay(mHolder);

            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);

            // do not preview
            SurfaceTexture mSurfaceTexture = new SurfaceTexture(10);
            mCamera.setPreviewTexture(mSurfaceTexture);

            mCamera.startPreview();
            //mCamera.setPreviewCallback(this);
            Log.d(TAG, "Got a camera frame " + ImageFormat.getBitsPerPixel(params.getPreviewFormat()) + " " + params.getPreviewSize().height + " " + params.getPreviewSize().width);

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(TAG, "Got a camera frame " + data.length);
        mCamera.addCallbackBuffer(mBuffer);
        // TODO copy data to another buffer
        if (TestRenderer.buffer == null) {
            TestRenderer.buffer = new byte[data.length];
        }
        System.arraycopy(data, 0, TestRenderer.buffer, 0, data.length);
        ActivityFast.gLSurfaceView.requestRender();

        if (true) return;


    }
}
