package ru.flightlabs.masks.renderer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.opencv.core.Mat;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ru.flightlabs.commonlib.Settings;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.utils.FileUtils;
import ru.flightlabs.masks.utils.OpenGlHelper;
import ru.flightlabs.masks.utils.PointsConverter;
import ru.flightlabs.masks.utils.PoseHelper;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 13.02.2017.
 */

public class ShaderEffectMask extends ShaderEffect {

    // 3d
    private int vPos3d;
    private int vTexFor3d;
    private static final String TAG = "ShaderEffectMask";

    private int maskTextureid;
    private int maskTextureBlendid;
    float[] verticesParticels;

    int[] programs;
    Map<String, Model> models = new HashMap<>();
    //Map<String, Integer> textures = new HashMap<>();
    public Map<Integer, EffectShader> effectsMap = new HashMap<>();

    public ShaderEffectMask(Context contex) {
        super(contex);
    }

    public void init() {
        Log.i(TAG, "init");
        super.init();
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
            effectsMap.put(i, EffectShader.parseString(effects[i]));
        }
        load3dModel();
    }

    @Override
    public boolean needBlend() {
        return effectsMap.get(Static.newIndexEye).needBlendShape;
    }

    private void load3dModel() {
        Log.i(TAG, "load3dModel");
        maskTextureid = OpenGlHelper.loadTexture(context, R.raw.m1_2);
        maskTextureBlendid = OpenGlHelper.loadTexture(context, R.raw.m1_2);
        Log.i(TAG, "load3dModel2");
        Model modelGlasses = new Model(R.raw.glasses_3d,
                context);
        Log.i(TAG, "load3dModel3");
        Model modelHat = new Model(R.raw.hat,
                context);
        models.put("model", model);
        models.put("modelGlasses", modelGlasses);
        models.put("modelHat", modelHat);
        models.put("star", new Model(R.raw.star, context));
        models.put("face_hockey", new Model(R.raw.face_hockey, context));
        models.put("deer_horns", new Model(R.raw.deer_horns, context));
        models.put("cat_mesh", new Model(R.raw.cat_mesh, context));
        models.put("ochki_mesh", new Model(R.raw.ochki_mesh, context));
        models.put("ochki_nnada_mesh", new Model(R.raw.ochki_nnada_mesh, context));

        models.put("protivo", new Model(R.raw.protivo, context));
        models.put("sam", new Model(R.raw.sam, context));
        models.put("zhdun", new Model(R.raw.zhdun, context));
        Log.i(TAG, "load3dModel exit");
    }

    public void makeShaderMask(int indexEye, PoseHelper.PoseResult poseResult, int width, int height, int texIn, long time, int iGlobTime) {

        EffectShader effect = effectsMap.get(indexEye);
        // TODO do in background
        // FIXME not in place
        if (Static.newIndexEye != Static.currentIndexEye) {
            Static.currentIndexEye = Static.newIndexEye;
            if (effect.textureName != null && !"".equals(effect.textureName)) {
                OpenGlHelper.changeTexture(context, "textures/" + effect.textureName + ".png", maskTextureid);
            }
            if (effect.textureNamBlendshape != null && !"".equals(effect.textureNamBlendshape)) {
                OpenGlHelper.changeTexture(context, "textures/" + effect.textureNamBlendshape + ".png", maskTextureBlendid);
            }
        }

        int programId = programs[effect.programId];
        if (!"".equals(effect.textureName)) {
            // 3d effect
            // first we copy whole texture to buffer
            int vPos = GLES20.glGetAttribLocation(programs[2], "vPosition");
            int vTex = GLES20.glGetAttribLocation(programs[2], "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            ShaderEffectHelper.shaderEffect2dWholeScreen(poseResult.leftEye, poseResult.rightEye, texIn, programs[2], vPos, vTex);
            // then we draw 3d/2d object on it
            if (poseResult.foundFeatures) {

                //GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                //GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

                int vTexOrtho = GLES20.glGetAttribLocation(programs[1], "vTexCoordOrtho");
                GLES20.glEnableVertexAttribArray(vTexOrtho);

                ShaderEffectHelper.shaderEffect3d2(poseResult.glMatrix, texIn, width, height, models.get(effect.model3dName), maskTextureid, effect.alpha, programId, vPos3d, vTexFor3d, PointsConverter.convertFromProjectedTo2dPoints(poseResult.projected, width, height), vTexOrtho, Settings.flagOrtho, poseResult.initialParams);
                if (!"".equals(effect.textureNamBlendshape)) {
                    GLES20.glFinish();
                    ShaderEffectHelper.shaderEffect3d(poseResult.glMatrix, texIn, width, height, model, maskTextureBlendid, effect.alpha, programId, vPos3d, vTexFor3d);
                } else {
                    //GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                }
                //GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            }
        } else {
            // 2d effect on whole screen
            Log.i(TAG, "onCameraTexture444");
            int vPos = GLES20.glGetAttribLocation(programId, "vPosition");
            int vTex = GLES20.glGetAttribLocation(programId, "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            Log.i(TAG, "onCameraTexture4441");
            Log.i(TAG, "onCameraTexture44412");
            if (indexEye > 16 && poseResult.foundLandmarks != null) {
                Log.i(TAG, "onCameraTexture44412 using new shaders"); // new format with all 68 points
                ShaderEffectHelper.shaderEffect2dWholeScreen(poseResult, texIn, programId, vPos, vTex, width, height);
            } else {
                // should be deprecated
                ShaderEffectHelper.shaderEffect2dWholeScreen(poseResult.leftEye, poseResult.rightEye, texIn, programId, vPos, vTex);
            }
            Log.i(TAG, "onCameraTexture4445");
        }
    }

    private void effect3dParticle(Mat glMatrix, int width, int height, int programId) {
        GLES20.glUseProgram(programId);

        int matrixMvp = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");

        float[] matrixView = PoseHelper.convertToArray(glMatrix);
        float[] mMatrix = new float[16];
        Matrix.multiplyMM(mMatrix, 0, PoseHelper.createProjectionMatrixThroughPerspective(width, height), 0, matrixView, 0);
        GLES20.glUniformMatrix4fv(matrixMvp, 1, false, mMatrix, 0);

        FloatBuffer vertexData = ShaderEffectHelper.convertArray(verticesParticels);
        GLES20.glVertexAttribPointer(vPos3d, 3, GLES20.GL_FLOAT, false, 0, vertexData);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 10);
        GLES20.glFlush();
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
}
