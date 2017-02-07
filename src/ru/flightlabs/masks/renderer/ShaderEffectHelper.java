package ru.flightlabs.masks.renderer;

import android.content.Context;
import android.content.res.TypedArray;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.camera.Effect;
import ru.flightlabs.masks.utils.FileUtils;
import ru.flightlabs.masks.utils.OpenGlHelper;
import ru.flightlabs.masks.utils.PoseHelper;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 06.02.2017.
 */

public class ShaderEffectHelper {

    private Context context;
    TypedArray eyesResources;
    // 3d
    private int vPos3d;
    private int vTexFor3d;

    private int maskTextureid;
    // 3d
    public Model model;
    float[] verticesParticels;

    int[] programs;
    Map<String, Model> models = new HashMap<>();
    Map<String, Integer> textures = new HashMap<>();
    public Map<Integer, Effect> effectsMap = new HashMap<>();

    private static final String TAG = "ShaderEffectHelper";

    public ShaderEffectHelper(Context contex, TypedArray eyesResources) {
        this.context = contex;
        this.eyesResources = eyesResources;
    }
    public void init() {
        Log.i(TAG, "init");
        // load programs
        String[] progs = context.getResources().getStringArray(R.array.programs);
        programs = new int[progs.length];
        for (int i = 0; i < progs.length; i++) {
            // TODO use glGetProgramiv to get info of attributes in particular shader
            String[] line = progs[i].split(";");
            Log.i(TAG, "load shaders " + progs[i]);
            int vertexShaderId = ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/" + line[0] + ".glsl"));
            int fragmentShaderId = ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/" + line[1] + ".glsl"));
            programs[i] = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);

            if ("2".equals(line[2])) {
            } else {
                // temporary fix
                if (i == 1) {
                    vPos3d = GLES20.glGetAttribLocation(programs[i], "vPosition");
                    GLES20.glEnableVertexAttribArray(vPos3d);
                    vTexFor3d = GLES20.glGetAttribLocation(programs[i], "a_TexCoordinate");
                    GLES20.glEnableVertexAttribArray(vTexFor3d);
                }
            }
        }
        // init effects
        String[] effects = context.getResources().getStringArray(R.array.effects);
        for (int i = 0; i < effects.length; i++) {
            effectsMap.put(i, Effect.parseString(effects[i]));
        }
        load3dModel();

    }

    private void load3dModel() {
        Log.i(TAG, "load3dModel");
        model = new Model(R.raw.for_android_test,
                context);
        //if (true) return;
        maskTextureid = OpenGlHelper.loadTexture(context, R.raw.m1_2);
        Log.i(TAG, "load3dModel2");
        Model modelGlasses = new Model(R.raw.glasses_3d,
                context);
        Log.i(TAG, "load3dModel3");
        int glassesTextureid = OpenGlHelper.loadTexture(context, R.raw.glasses);

        Model modelHat = new Model(R.raw.hat,
                context);
        int hatTextureid = OpenGlHelper.loadTexture(context, R.raw.hat_tex);
        models.put("model", model);
        textures.put("maskTextureid", maskTextureid);
        models.put("modelGlasses", modelGlasses);
        textures.put("glassesTextureid", glassesTextureid);
        models.put("modelHat", modelHat);
        textures.put("hatTextureid", hatTextureid);
        models.put("star", new Model(R.raw.star, context));
        Log.i(TAG, "load3dModel exit");
    }

    public void makeShader(int indexEye, PoseHelper.PoseResult poseResult, int width, int height, int texIn, long time, int iGlobTime) {

        // TODO do in background
        // FIXME not in place
        if (Static.newIndexEye != Static.currentIndexEye) {
            Static.currentIndexEye = Static.newIndexEye;
            OpenGlHelper.changeTexture(context, eyesResources.getResourceId(Static.newIndexEye, 0), maskTextureid);
        }

        Effect effect = effectsMap.get(indexEye);
        int programId = programs[effect.programId];
        if (!"".equals(effect.textureName)) {
            int vPos = GLES20.glGetAttribLocation(programs[0], "vPosition");
            int vTex = GLES20.glGetAttribLocation(programs[0], "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            shaderEfffect2d(poseResult.leftEye, poseResult.rightEye, texIn, programs[0], vPos, vTex);
            if (poseResult.foundFeatures) {
                // crazy simple animation
                if (indexEye == 4) {
                    animate(models.get(effect.model3dName), time);
                }
                if (indexEye == 13) {
                    Log.i(TAG, "index 13");
                    GLES20.glUseProgram(programId);
                    int uCenter2 = GLES20.glGetUniformLocation(programId, "iGlobalTime");
                    Log.i(TAG, "onCameraTexture4443 " + uCenter2 + " " + iGlobTime);
                    GLES20.glUniform1f(uCenter2, (float) iGlobTime);
                    shaderEfffect3dParticle(poseResult.glMatrix, width, height, programId);
                } else {
                    shaderEfffect3d(poseResult.glMatrix, texIn, width, height, models.get(effect.model3dName), textures.get(effect.textureName), effect.alpha, programId);
                }
            }
        } else {
            Log.i(TAG, "onCameraTexture444");
            int vPos = GLES20.glGetAttribLocation(programId, "vPosition");
            int vTex = GLES20.glGetAttribLocation(programId, "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            Log.i(TAG, "onCameraTexture4441");
            if (indexEye >= 6) {
                GLES20.glUseProgram(programId);
                int uCenter2 = GLES20.glGetUniformLocation(programId, "iGlobalTime");
                Log.i(TAG, "onCameraTexture4443 " + uCenter2 + " " + iGlobTime);
                GLES20.glUniform1f(uCenter2, (float) iGlobTime);
            }
            Log.i(TAG, "onCameraTexture44412");
            if (indexEye == 10) {
                // just experiment with fur
                shaderEfffect2d(poseResult.leftEye, poseResult.rightEye, textures.get("maskTextureid"), programId, vPos, vTex);
            } else {
                shaderEfffect2d(poseResult.leftEye, poseResult.rightEye, texIn, programId, vPos, vTex);
            }
            Log.i(TAG, "onCameraTexture4445");
        }
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

    private void shaderEfffect3dParticle(Mat glMatrix, int width, int height, int programId) {
        GLES20.glUseProgram(programId);

        int matrixMvp = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");

        float[] matrixView = PoseHelper.convertToArray(glMatrix);
        float[] mMatrix = new float[16];
        Matrix.multiplyMM(mMatrix, 0, PoseHelper.createProjectionMatrixThroughPerspective(width, height), 0, matrixView, 0);
        GLES20.glUniformMatrix4fv(matrixMvp, 1, false, mMatrix, 0);


        FloatBuffer vertexData;
        vertexData = ByteBuffer
                .allocateDirect(verticesParticels.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(verticesParticels);
        vertexData.position(0);
        GLES20.glVertexAttribPointer(vPos3d, 3, GLES20.GL_FLOAT, false, 0, vertexData);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 10);
        GLES20.glFlush();
    }

    public static void shaderEfffect2d(Point center, Point center2, int texIn, int programId, int poss, int texx) {
        shaderEfffect2d(center, center2, texIn, programId, poss, texx, null);

    }
    public static void shaderEfffect2d(Point center, Point center2, int texIn, int programId, int poss, int texx, Integer texIn2) {
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

        if (texIn2 != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIn2);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "sTexture2"), 1);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush(); //?
        //GLES20.glFinish();
    }

    private void initParticles() {
        int parts = 100;
        verticesParticels = new float[parts * 3];
        for (int i = 0; i < parts; i++) {
            float theta = 3.14f / 6f * new Random().nextFloat();
            float phi = 6.28f * new Random().nextFloat();
            verticesParticels[i * 3] = (float) (Math.sin(theta) * Math.cos(phi));
            verticesParticels[i * 3 + 1] = (float) Math.cos(theta);
            verticesParticels[i * 3 + 2] = (float)(Math.sin(theta) * Math.sin(phi));
        }
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
}
