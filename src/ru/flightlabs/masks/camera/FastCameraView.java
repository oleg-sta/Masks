package ru.flightlabs.masks.camera;

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

import ru.flightlabs.masks.renderer.MaskRenderer;

/**
 * Created by sov on 06.02.2017.
 */

public class FastCameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final int MAGIC_TEXTURE_ID = 10;
    public static boolean cameraFacing;
    private byte mBuffer[];
    private static final String TAG = "FastView";
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;

    int numberOfCameras;
    int cameraIndex;

    int previewWidth;
    int previewHeight;

    public FastCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

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

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        getHolder().addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged " + format + " " + w + " " + h);
        previewHeight = h;
        previewWidth = w;
        // TODO release camera if was
        startCameraPreview(w , h);
    }

    private void startCameraPreview(int w, int h) {
        Log.d(TAG, "startCameraPreview " + w + " " + h);
        mCamera = Camera.open(cameraIndex);
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

//        if (mHolder.getSurface() == null){
//            // preview surface does not exist
//            return;
//        }

        // stop preview before making changes
//        try {
//            mCamera.stopPreview();
//        } catch (Exception e){
//            // ignore: tried to stop a non-existent preview
//        }

        // we transpose view
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
        CameraHelper.calculateCameraPreviewSize(params, h, w);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !android.os.Build.MODEL.equals("GT-I9100"))
            params.setRecordingHint(true);
        List<String> FocusModes = params.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
        {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        List<int[]> frameRates = params.getSupportedPreviewFpsRange();
        int last = frameRates.size() - 1;
        int minFps = (frameRates.get(last))[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
        int maxFps = (frameRates.get(last))[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
        params.setPreviewFpsRange(minFps, maxFps);
        Log.d(TAG, "preview fps: " + minFps + ", " + maxFps);

        //params.setPreviewFpsRange( 30000, 30000 );
        mCamera.setParameters(params);

        int size = w * h;
        size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
        mBuffer = new byte[size];
        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            //mCamera.setPreviewDisplay(mHolder);

            // TODO if necessary you could use more buffer for speed
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);

            // do not preview
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                mCamera.setPreviewTexture(mSurfaceTexture);
            } else
                mCamera.setPreviewDisplay(null);

            mCamera.startPreview();
            //mCamera.setPreviewCallback(this);
            Log.d(TAG, "Got a camera frame " + ImageFormat.getBitsPerPixel(params.getPreviewFormat()) + " " + params.getPreviewSize().height + " " + params.getPreviewSize().width);

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(TAG, "Got a camera frame " + data.length);
        // TODO copy data to another bufferFromCamera
        if (MaskRenderer.bufferFromCamera == null) {
            MaskRenderer.bufferFromCamera = new byte[data.length];
        }
        synchronized (FastCameraView.class) {
            // TODO find face and features here or another thread for optimization
            // TODO we can not copy buffer just find face features, morph face and let it go to renderer
            System.arraycopy(data, 0, MaskRenderer.bufferFromCamera, 0, data.length);
        }
        // we should add buffer to queue, dut to buffer
        mCamera.addCallbackBuffer(mBuffer);
    }

    public void disableView() {
        releaseCamera();
    }

    private void releaseCamera() {
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
            }
            mCamera = null;
        }
    }

    public void swapCamera() {
        releaseCamera();
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
        startCameraPreview(previewWidth, previewHeight);
    }
}
