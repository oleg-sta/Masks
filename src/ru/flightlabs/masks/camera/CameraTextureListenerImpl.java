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

import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.DetectionBasedTracker;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.activity.FdActivity2;
import ru.flightlabs.masks.activity.Settings;
import ru.flightlabs.masks.renderer.Model;
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
    DetectionBasedTracker mNativeDetector;
    CompModel compModel;
    Activity act;

    int iGlobTime = 0;

    // 3d
    private int vPos3d;
    private int vTexFor3d;

    private int maskTextureid;

    ByteBuffer m_bbPixels;
    Mat mRgba;
    Mat mGray;

    int frameCount;
    long timeStart;
    double lastCount = 0.5f;

    private static final String TAG = "CameraTextureL_class";
    private int[] iFBO = null;//{0};

    int[] programs;
    String[] effects;
    Map<String, Model> models = new HashMap<>();
    Map<String, Integer> textures = new HashMap<>();

    // 3d
    private Model model;

    Mat initialParams = null;

    String modelPath;

    public CameraTextureListenerImpl(Activity act, CompModel compModel) {
        this.act = act;
        this.compModel = compModel;
    }

    public void onCameraViewStarted(int width, int height) {

        // load programs
        String[] progs = act.getResources().getStringArray(R.array.programs);
        programs = new int[progs.length];
        for (int i = 0; i < progs.length; i++) {
            String[] line = progs[i].split(";");
            int vertexShaderId = ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(act.getAssets(), "shaders/" + line[0] + ".glsl"));
            int fragmentShaderId = ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(act.getAssets(), "shaders/" + line[1] + ".glsl"));
            programs[i] = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);

            if ("2".equals(line[2])) {
            } else {
                vPos3d = GLES20.glGetAttribLocation(programs[i], "vPosition");
                GLES20.glEnableVertexAttribArray(vPos3d);
                vTexFor3d = GLES20.glGetAttribLocation(programs[i], "a_TexCoordinate");
                GLES20.glEnableVertexAttribArray(vTexFor3d);
            }
        }
        effects = act.getResources().getStringArray(R.array.effects);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();
        //m_bbPixels = ByteBuffer.allocateDirect(width * height * 4);
        // workaround due to https://code.google.com/p/android/issues/detail?id=80064
        m_bbPixels = ByteBuffer.wrap(new byte[width * height * 4]);
        m_bbPixels.order(ByteOrder.LITTLE_ENDIAN);

        load3dModel();
        //bindData(width, height);

        Log.i(TAG, "onCameraViewStarted");
    }

    private void load3dModel() {
        model = new Model(R.raw.for_android_test,
                act);
        maskTextureid = OpenGlHelper.loadTexture(act, R.raw.m1_2);
        Log.i(TAG, "load3dModel2");
        Model modelGlasses = new Model(R.raw.glasses_3d,
                act);
        Log.i(TAG, "load3dModel3");
        int glassesTextureid = OpenGlHelper.loadTexture(act, R.raw.glasses);

        Model modelHat = new Model(R.raw.hat,
                act);
        int hatTextureid = OpenGlHelper.loadTexture(act, R.raw.hat_tex);
        models.put("model", model);
        textures.put("maskTextureid", maskTextureid);
        models.put("modelGlasses", modelGlasses);
        textures.put("glassesTextureid", glassesTextureid);
        models.put("modelHat", modelHat);
        textures.put("hatTextureid", hatTextureid);
        models.put("star", new Model(R.raw.star, act));
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
        // TODO do in background
        if (FdActivity2.newIndexEye != FdActivity2.currentIndexEye) {
            FdActivity2.currentIndexEye = FdActivity2.newIndexEye;
            OpenGlHelper.changeTexture(act, FdActivity2.eyesResources.getResourceId(FdActivity2.newIndexEye, 0), maskTextureid);
        }

        if (compModel.mNativeDetector != null) {
            mNativeDetector = compModel.mNativeDetector;
        }
        Log.i(TAG, "onCameraTexture " + width + " " + height);
        Log.i(TAG, "onCameraTexture2");
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, m_bbPixels);
        m_bbPixels.rewind();
        Log.i(TAG, "onCameraTexture3" + m_bbPixels.array().length);

        //mRgba.get(width, height, pixelValues);
        mRgba.put(0, 0, m_bbPixels.array());
        Core.flip(mRgba, mRgba, 0);
        Log.i(TAG, "onCameraTexture5");
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

        if (mAbsoluteFaceSize == 0) {
            mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            //mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = compModel.findFaces(mGray, mAbsoluteFaceSize);
        Rect[] facesArray = faces.toArray();
        final boolean haveFace = facesArray.length > 0;
        Log.i(TAG, "onCameraTexture5 " + haveFace);
        Point center = new Point(0.5, 0.5);
        Point center2 = new Point(0.5, 0.5);
        Point[] foundEyes = null;
        if (haveFace) {
            if (Settings.debugMode) {
                Imgproc.rectangle(mRgba, facesArray[0].tl(), facesArray[0].br(), new Scalar(255, 10 ,10), 3);
            }
            center = OpencvUtils.convertToGl(new Point((2 * facesArray[0].x + facesArray[0].width) / 2.0, (2 * facesArray[0].y + facesArray[0].height) / 2.0), width, height);
            if (mNativeDetector != null) {
                foundEyes = mNativeDetector.findEyes(mGray, facesArray[0]);
                if (Settings.debugMode) {
                    for (Point p : foundEyes) {
                        Imgproc.circle(mRgba, p, 2, new Scalar(255, 10, 10));
                    }
                }
                center = OpencvUtils.convertToGl(new Point((foundEyes[36].x + foundEyes[39].x) / 2.0, (foundEyes[36].y + foundEyes[39].y) / 2.0), width, height);
                center2 = OpencvUtils.convertToGl(new Point((foundEyes[42].x + foundEyes[45].x) / 2.0, (foundEyes[42].y + foundEyes[45].y) / 2.0), width, height);
            }
        }

        if (Settings.debugMode) {
            Imgproc.putText(mRgba, "frames " + String.format("%.3f", (1f / lastCount) * 10) + " in 1 second.", new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
            Imgproc.putText(mRgba, "time " + iGlobTime, new Point(50, 250), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
        }
        Mat glMatrix = null;
        int indexEye = FdActivity2.currentIndexEye;
        PoseHelper.bindMatrix(100, 100);
        if (foundEyes != null) {


            boolean shapeBlends = false;
            if (indexEye < effects.length) {
                String[] effect = effects[indexEye].split(";");
                if ("1".equals(effect[4])) {
                    shapeBlends = true;
                }
            }
            if (shapeBlends) {
                Mat inputLandMarks = new Mat(68, 2, CvType.CV_64FC1);
                for (int i = 0; i < foundEyes.length; i++) {
                    inputLandMarks.put(i, 0, foundEyes[i].x);
                    inputLandMarks.put(i, 1, foundEyes[i].y);
                }
                Mat output3dShape = new Mat(113, 3, CvType.CV_64FC1);
                if (initialParams == null) {
                    initialParams = new Mat(14, 1, CvType.CV_64FC1, new Scalar(0));
                }
                if (modelPath == null) {
                    if (new File("/storage/extSdCard/models").exists()) {
                        modelPath = "/storage/extSdCard/models";
                    } else {
                        File cascadeDir = act.getApplicationContext().getDir("models", Context.MODE_PRIVATE);
                        Decompress.unzipFromAssets(act, "models.zip", cascadeDir.getPath());
                        modelPath = cascadeDir.getPath();
                    }
                    Log.i(TAG, "onCameraTexture1 " + modelPath);
                }

                mNativeDetector.morhpFace(inputLandMarks, output3dShape, initialParams, modelPath, false);
                for (int i = 0; i < output3dShape.rows(); i++) {
                    model.tempV[i * 3] = (float) output3dShape.get(i, 0)[0];
                    model.tempV[i * 3 + 1] = (float) output3dShape.get(i, 1)[0];
                    model.tempV[i * 3 + 2] = (float) output3dShape.get(i, 2)[0];
                }
                model.recalcV();
            }
            Log.i(TAG, "onCameraTexture1 " + model.tempV[0] + " " + model.tempV[1] + " " + model.tempV[2]);

            glMatrix = PoseHelper.findPose(model, width, act, foundEyes, mRgba);
            //PoseHelper.drawDebug(mRgba, model, glMatrix);
            if (Settings.debugMode) {
                for (Point e : foundEyes) {
                    Imgproc.circle(mRgba, e, 3, new Scalar(255, 255, 255), -1);
                }
            }

        }



        boolean makeAfterPhoto = false;
        // this photo for debug purpose
        if (Static.makePhoto) {
            Static.makePhoto = false;
            makeAfterPhoto= true;
            PhotoMaker.makePhoto(mRgba, act);
        }

        // temporary for debug purposes or maby for simple effects
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
        if (indexEye < effects.length) {
            String[] effect = effects[indexEye].split(";");
            int programId = programs[Integer.parseInt(effect[0])];
            if (!"".equals(effect[2])) {
                int vPos = GLES20.glGetAttribLocation(programs[0], "vPosition");
                int vTex  = GLES20.glGetAttribLocation(programs[0], "vTexCoord");
                GLES20.glEnableVertexAttribArray(vPos);
                GLES20.glEnableVertexAttribArray(vTex);
                shaderEfffect2d(center, center2, texIn, programs[0], vPos, vTex);
                if (foundEyes != null) {
                    // crazy simple animation
                    if (indexEye == 4) {
                        animate(models.get(effect[1]), time);
                    }
                    shaderEfffect3d(glMatrix, texIn, width, height, models.get(effect[1]), textures.get(effect[2]), Float.parseFloat(effect[3]), programId);
                }
            } else {
                Log.i(TAG, "onCameraTexture444");
                int vPos = GLES20.glGetAttribLocation(programId, "vPosition");
                int vTex  = GLES20.glGetAttribLocation(programId, "vTexCoord");
                GLES20.glEnableVertexAttribArray(vPos);
                GLES20.glEnableVertexAttribArray(vTex);
                Log.i(TAG, "onCameraTexture4441");
                if  (indexEye >= 6) {
                    GLES20.glUseProgram(programId);
                    int uCenter2 = GLES20.glGetUniformLocation(programId, "iGlobalTime");
                    Log.i(TAG, "onCameraTexture4443 "+ uCenter2 + " " + iGlobTime);
                    GLES20.glUniform1f(uCenter2, (float)iGlobTime);
                }
                Log.i(TAG, "onCameraTexture44412");
                shaderEfffect2d(center, center2, texIn, programId, vPos, vTex);
                Log.i(TAG, "onCameraTexture4445");
            }
        }
        // shader effect

        if (makeAfterPhoto) {
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, m_bbPixels);
            m_bbPixels.rewind();
            //mRgba.get(width, height, pixelValues);
            mRgba.put(0, 0, m_bbPixels.array());
            Core.flip(mRgba, mRgba, 0);
            PhotoMaker.makePhoto(mRgba, act);
            // save 3d model
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File newFile = new File(file, Settings.DIRECTORY_SELFIE);
            final File fileJpg = new File(newFile, "outModel.obj");
            model.saveModel(fileJpg.getPath());
        }

        Log.i(TAG, "onCameraTexture " + mRgba.height() + " " + facesArray.length);

        return !Static.drawOrigTexture;
    }

    private void animate(Model model, long time) {
        double alpha = 0.1;//time / 100000;
        double a = Math.cos(alpha);
        double b = Math.sin(alpha);
        for (int i = 0; i < model.tempV.length / 3; i++) {
            double x = model.tempV[i * 3] * a - model.tempV[i * 3 + 2] * b;
            double y = model.tempV[i * 3] * b + model.tempV[i * 3 + 2] * a;
            model.tempV[i * 3] = (float)x;
            model.tempV[i * 3 + 2] = (float)y;
        }
        model.recalcV();
    }

    private void shaderEfffect3d(Mat glMatrix, int texIn, int width, int height, final Model modelToDraw, int modelTextureId, float alpha, int programId) {
        GLES20.glUseProgram(programId);
        int matrixMvp = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");

        float[] matrixView = PoseHelper.convertToArray(glMatrix);
        float[] mMatrix = new float[16];
        Matrix.multiplyMM(mMatrix, 0, PoseHelper.createProjectionMatrixThroughPerspective(width, height), 0, matrixView, 0);
        GLES20.glUniformMatrix4fv(matrixMvp, 1, false, mMatrix, 0);

        int fAlpha = GLES20.glGetUniformLocation(programId, "f_alpha");
        GLES20.glUniform1f(fAlpha, alpha);

        FloatBuffer mVertexBuffer = modelToDraw.getVertices();
        mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPos3d, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        FloatBuffer mTextureBuffer = modelToDraw.getTexCoords();
        mTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(vTexFor3d,  2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, modelTextureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "u_Texture"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIn);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "u_TextureOrig"), 1);

        ShortBuffer mIndices = modelToDraw.getIndices();
        mIndices.position(0);
        // FIXME with glDrawElements use can't use other texture coordinates
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, modelToDraw.getIndicesCount(), GLES20.GL_UNSIGNED_SHORT, mIndices);
        GLES20.glFlush();
    }

    private void shaderEfffect3dStub(int programId) {
        GLES20.glUseProgram(programId);
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        int matrixMvp = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");
        float[] matrix = new float[16];

        matrix[0] = 1;
        matrix[5] = 1;
        matrix[10] = 1;
        matrix[15] = 1;
        GLES20.glUniformMatrix4fv(matrixMvp, 1, false, matrix, 0);

        FloatBuffer vertexData;
        float[] vertices = {
                -1, -1, 0,
                -1,  1, 0,
                0, 0, 0
        };
        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);
        vertexData.position(0);
        GLES20.glVertexAttribPointer(vPos3d, 3, GLES20.GL_FLOAT, false, 0, vertexData);


        ShortBuffer indexArray;
        short[] indexes = {
                0, 1, 2
        };
        indexArray = ByteBuffer
                .allocateDirect(indexes.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        indexArray.put(indexes);
        indexArray.position(0);


//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3, GLES20.GL_UNSIGNED_SHORT, indexArray);
        GLES20.glFlush();

    }
    private void shaderEfffect2d(Point center, Point center2, int texIn, int programId, int poss, int texx) {
        GLES20.glUseProgram(programId);
        int uColorLocation = GLES20.glGetUniformLocation(programId, "u_Color");
        GLES20.glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);

        int uCenter = GLES20.glGetUniformLocation(programId, "uCenter");
        GLES20.glUniform2f(uCenter, (float)center.x, (float)center.y);

        int uCenter2 = GLES20.glGetUniformLocation(programId, "uCenter2");
        GLES20.glUniform2f(uCenter2, (float)center2.x, (float)center2.y);

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
    }
}
