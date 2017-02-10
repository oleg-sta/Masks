package ru.flightlabs.masks.renderer;

import android.content.Context;
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

import ru.flightlabs.makeup.EditorEnvironment;
import ru.flightlabs.makeup.ResourcesApp;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.utils.FileUtils;
import ru.flightlabs.masks.utils.OpenGlHelper;
import ru.flightlabs.masks.utils.PointsConverter;
import ru.flightlabs.masks.utils.PoseHelper;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 06.02.2017.
 */

public class ShaderEffectHelper {

    private Context context;
    // 3d
    private int vPos3d;
    private int vTexFor3d;

    private int maskTextureid;

    // for makeup
    private int eyeShadowTextureid;
    private int eyeLashesTextureid;
    private int eyeLineTextureid;
    private int lipsTextureid;


    // 3d
    public Model model;
    float[] verticesParticels;

    int[] programs;
    int program2dJustCopy;
    int program2dTriangles;
    Map<String, Model> models = new HashMap<>();
    //Map<String, Integer> textures = new HashMap<>();
    public Map<Integer, EffectShader> effectsMap = new HashMap<>();

    private static final String TAG = "ShaderEffectHelper";

    public ShaderEffectHelper(Context contex) {
        this.context = contex;
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
            effectsMap.put(i, EffectShader.parseString(effects[i]));
        }
        load3dModel();

        program2dJustCopy = ShaderUtils.createProgram(ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/vss_2d.glsl")), ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/fss_2d_simple.glsl")));
        program2dTriangles = ShaderUtils.createProgram(ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/vss_2d.glsl")), ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/fss_solid.glsl")));

    }

    private void load3dModel() {
        Log.i(TAG, "load3dModel");
        model = new Model(R.raw.for_android_test,
                context);
        maskTextureid = OpenGlHelper.loadTexture(context, R.raw.m1_2);
        eyeShadowTextureid = OpenGlHelper.loadTexture(context, R.raw.eye2_0000_smokey_eeys);
        eyeLashesTextureid = OpenGlHelper.loadTexture(context, R.raw.eye2_0000_lash);
        eyeLineTextureid = OpenGlHelper.loadTexture(context, R.raw.eye2_0000_line);
        lipsTextureid = OpenGlHelper.loadTexture(context, R.raw.lips_icon);

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
        }

        int programId = programs[effect.programId];
        if (!"".equals(effect.textureName)) {
            // 3d effect
            // first we copy whole texture to buffer
            int vPos = GLES20.glGetAttribLocation(programs[0], "vPosition");
            int vTex = GLES20.glGetAttribLocation(programs[0], "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos);
            GLES20.glEnableVertexAttribArray(vTex);
            shaderEffect2dWholeScreen(poseResult.leftEye, poseResult.rightEye, texIn, programs[0], vPos, vTex);
            // then we draw 3d/2d object on it
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
                    effect3dParticle(poseResult.glMatrix, width, height, programId);
                } else {
                    shaderEffect3d(poseResult.glMatrix, texIn, width, height, models.get(effect.model3dName), maskTextureid, effect.alpha, programId);
                }
            }
        } else {
            // 2d effect on whole screen
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
                shaderEffect2dWholeScreen(poseResult.leftEye, poseResult.rightEye, maskTextureid, programId, vPos, vTex);
            } else {
                shaderEffect2dWholeScreen(poseResult.leftEye, poseResult.rightEye, texIn, programId, vPos, vTex);
            }
            Log.i(TAG, "onCameraTexture4445");
        }
    }

    public void makeShaderMakeUp(int indexEye, PoseHelper.PoseResult poseResult, int width, int height, int texIn, long time, int iGlobTime) {
        Log.i(TAG, "onDrawFrame6 draw maekup");
        int vPos2 = GLES20.glGetAttribLocation(program2dJustCopy, "vPosition");
        int vTex2 = GLES20.glGetAttribLocation(program2dJustCopy, "vTexCoord");
        GLES20.glEnableVertexAttribArray(vPos2);
        GLES20.glEnableVertexAttribArray(vTex2);
        ShaderEffectHelper.shaderEffect2dWholeScreen(poseResult.leftEye, poseResult.rightEye, texIn, program2dJustCopy, vPos2, vTex2);

        if (poseResult.foundLandmarks != null) {
            Point[] onImageEyeLeft = EditorEnvironment.getOnlyPoints(poseResult.foundLandmarks, 36, 6);
            Point[] onImageEyeRight = EditorEnvironment.getOnlyPoints(poseResult.foundLandmarks, 36 + 6, 6);
            // TODO add checkbox for rgb or hsv bleding
            Log.i(TAG, "onDrawFrame6 draw maekup2");
            int vPos22 = GLES20.glGetAttribLocation(program2dTriangles, "vPosition");
            int vTex22 = GLES20.glGetAttribLocation(program2dTriangles, "vTexCoord");
            GLES20.glEnableVertexAttribArray(vPos22);
            GLES20.glEnableVertexAttribArray(vTex22);
            // TODO use blendshape for eyes

            if (EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_SHADOW] != EditorEnvironment.newIndexItem && EditorEnvironment.EYE_SHADOW == EditorEnvironment.catgoryNum) {
                EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_SHADOW] = EditorEnvironment.newIndexItem;
                OpenGlHelper.changeTexture(context, ResourcesApp.eyeshadowSmall.getResourceId(EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_SHADOW], 0), eyeShadowTextureid);
            }
            if (EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_LASH] != EditorEnvironment.newIndexItem && EditorEnvironment.EYE_LASH == EditorEnvironment.catgoryNum) {
                EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_LASH] = EditorEnvironment.newIndexItem;
                OpenGlHelper.changeTexture(context, ResourcesApp.eyelashesSmall.getResourceId(EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_LASH], 0), eyeLashesTextureid);
            }
            if (EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_LINE] != EditorEnvironment.newIndexItem && EditorEnvironment.EYE_LINE == EditorEnvironment.catgoryNum) {
                EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_LINE] = EditorEnvironment.newIndexItem;
                OpenGlHelper.changeTexture(context, ResourcesApp.eyelinesSmall.getResourceId(EditorEnvironment.currentIndexItem[EditorEnvironment.EYE_LINE], 0), eyeLineTextureid);
            }
            if (EditorEnvironment.currentIndexItem[EditorEnvironment.LIPS] != EditorEnvironment.newIndexItem && EditorEnvironment.LIPS == EditorEnvironment.catgoryNum) {
                EditorEnvironment.currentIndexItem[EditorEnvironment.LIPS] = EditorEnvironment.newIndexItem;
                OpenGlHelper.changeTexture(context, ResourcesApp.lipsSmall.getResourceId(EditorEnvironment.currentIndexItem[EditorEnvironment.LIPS], 0), lipsTextureid);
            }
            Point[] onImage = PointsConverter.completePointsByAffine(onImageEyeLeft, PointsConverter.convertToOpencvPoints(EditorEnvironment.pointsLeftEye), new int[]{0, 1, 2, 3, 4, 5});
            // TODO use blendshapes
            onImage = PointsConverter.replacePoints(onImage, onImageEyeLeft, new int[]{0, 1, 2, 3, 4, 5});
            ShaderEffectHelper.effect2dTriangles(program2dTriangles, texIn, eyeShadowTextureid, PointsConverter.convertFromPointsGlCoord(onImage, width, height), PointsConverter.convertFromPointsGlCoord(EditorEnvironment.pointsLeftEye, 512, 512), vPos22, vTex22, PointsConverter.convertTriangle(EditorEnvironment.trianglesLeftEye), eyeLashesTextureid, eyeLineTextureid, false,
                    PointsConverter.convertTovec3(EditorEnvironment.currentColor[EditorEnvironment.EYE_SHADOW]), PointsConverter.convertTovec3(EditorEnvironment.currentColor[EditorEnvironment.EYE_LASH]), PointsConverter.convertTovec3(EditorEnvironment.currentColor[EditorEnvironment.EYE_LINE]),
                    EditorEnvironment.opacity[EditorEnvironment.EYE_SHADOW] / 100f, EditorEnvironment.opacity[EditorEnvironment.EYE_LASH] / 100f, EditorEnvironment.opacity[EditorEnvironment.EYE_LINE] /100f);

            Point[] onImageRight = PointsConverter.completePointsByAffine(PointsConverter.reallocateAndCut(onImageEyeRight, new int[] {3, 2, 1, 0 , 5, 4}), PointsConverter.convertToOpencvPoints(EditorEnvironment.pointsLeftEye), new int[]{0, 1, 2, 3, 4, 5});
            //onImageRight = PointsConverter.replacePoints(onImageRight, onImageEyeRight, new int[]{3, 2, 1, 0 , 5, 4});
            // FIXME flip triangle on right eyes, cause left and right triangles are not the same
            ShaderEffectHelper.effect2dTriangles(program2dTriangles, texIn, eyeShadowTextureid, PointsConverter.convertFromPointsGlCoord(onImageRight, width, height), PointsConverter.convertFromPointsGlCoord(EditorEnvironment.pointsLeftEye, 512, 512), vPos22, vTex22, PointsConverter.convertTriangle(EditorEnvironment.trianglesLeftEye), eyeLashesTextureid, eyeLineTextureid, false,
                    PointsConverter.convertTovec3(EditorEnvironment.currentColor[EditorEnvironment.EYE_SHADOW]), PointsConverter.convertTovec3(EditorEnvironment.currentColor[EditorEnvironment.EYE_LASH]), PointsConverter.convertTovec3(EditorEnvironment.currentColor[EditorEnvironment.EYE_LINE]),
                    EditorEnvironment.opacity[EditorEnvironment.EYE_SHADOW] / 100f, EditorEnvironment.opacity[EditorEnvironment.EYE_LASH] / 100f, EditorEnvironment.opacity[EditorEnvironment.EYE_LINE] /100f);

            Point[] onImageLips = EditorEnvironment.getOnlyPoints(poseResult.foundLandmarks, 48, 20);
            Point[] onImageLipsConv = PointsConverter.completePointsByAffine(onImageLips, PointsConverter.convertToOpencvPoints(EditorEnvironment.pointsWasLips), new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
            onImageLipsConv = PointsConverter.replacePoints(onImageLipsConv, onImageLips, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
            ShaderEffectHelper.effect2dTriangles(program2dTriangles, texIn, lipsTextureid, PointsConverter.convertFromPointsGlCoord(onImageLipsConv, width, height), PointsConverter.convertFromPointsGlCoord(EditorEnvironment.pointsWasLips, 512, 512), vPos22, vTex22, PointsConverter.convertTriangle(EditorEnvironment.trianglesLips), lipsTextureid, lipsTextureid, true, null, null, null, 0, 0, 0);

            // TODO add right eye
            // FIXME elements erase each other
        }
    }

    private void shaderEffect3d(Mat glMatrix, int texIn, int width, int height, final Model modelToDraw, int modelTextureId, float alpha, int programId) {
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

        FloatBuffer vertexData = convertArray(new float[]{
                -1, -1, 0,
                -1,  1, 0,
                0, 0, 0
        });
        GLES20.glVertexAttribPointer(vPos3d, 3, GLES20.GL_FLOAT, false, 0, vertexData);


        ShortBuffer indexArray = convertArray(new short[]{
                0, 1, 2
        });
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3, GLES20.GL_UNSIGNED_SHORT, indexArray);
        GLES20.glFlush();

    }

    public static void effect2dParticle(int width, int height, int programId, int vPos, float[] verticesParticels) {
        GLES20.glUseProgram(programId);

        FloatBuffer vertexData = convertArray(verticesParticels);
        GLES20.glVertexAttribPointer(vPos, 2, GLES20.GL_FLOAT, false, 0, vertexData);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, verticesParticels.length / 2);
        GLES20.glFlush();
    }

    private void effect3dParticle(Mat glMatrix, int width, int height, int programId) {
        GLES20.glUseProgram(programId);

        int matrixMvp = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");

        float[] matrixView = PoseHelper.convertToArray(glMatrix);
        float[] mMatrix = new float[16];
        Matrix.multiplyMM(mMatrix, 0, PoseHelper.createProjectionMatrixThroughPerspective(width, height), 0, matrixView, 0);
        GLES20.glUniformMatrix4fv(matrixMvp, 1, false, mMatrix, 0);

        FloatBuffer vertexData = convertArray(verticesParticels);
        GLES20.glVertexAttribPointer(vPos3d, 3, GLES20.GL_FLOAT, false, 0, vertexData);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 10);
        GLES20.glFlush();
    }

    public static void shaderEffect2dWholeScreen(Point center, Point center2, int texIn, int programId, int poss, int texx) {
        shaderEffect2dWholeScreen(center, center2, texIn, programId, poss, texx, null);
    }

    public static void effect2dTriangles(int programId, int textureForeground, int textureEffect, float[] verticesOnForeground, float[] verticesOnTexture, int posForeground, int posOnTexture, short[] triangles, Integer texture2, Integer texture3, boolean useHsv, float[] color0, float[] color1, float[] color2, float alpha0, float alpha1, float alpha2) {
        GLES20.glUseProgram(programId);


        int fAlpha = GLES20.glGetUniformLocation(programId, "f_alpha");
        GLES20.glUniform3f(fAlpha, alpha0, alpha1, alpha2);

        int fAlpha2 = GLES20.glGetUniformLocation(programId, "useHsv");
        GLES20.glUniform1i(fAlpha2, useHsv? 1 : 0);

        if (color0 != null) {
            int uColorLocation = GLES20.glGetUniformLocation(programId, "color0");
            GLES20.glUniform3f(uColorLocation, color0[0], color0[1], color0[2]);
        }
        if (color1 != null) {
            int uColorLocation = GLES20.glGetUniformLocation(programId, "color1");
            GLES20.glUniform3f(uColorLocation, color1[0], color1[1], color1[2]);
        }
        if (color2 != null) {
            int uColorLocation = GLES20.glGetUniformLocation(programId, "color2");
            GLES20.glUniform3f(uColorLocation, color2[0], color2[1], color2[2]);
        }

        FloatBuffer mVertexBuffer = convertArray(verticesOnForeground);
        GLES20.glVertexAttribPointer(posForeground, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        FloatBuffer mTextureBuffer = convertArray(verticesOnTexture);
        GLES20.glVertexAttribPointer(posOnTexture,  2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureEffect);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "u_Texture"), 0);

        if (texture2 != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "u_Texture2"), 2);
        }

        if (texture3 != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture3);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "u_Texture3"), 3);
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureForeground);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "u_TextureOrig"), 1);

        ShortBuffer mIndices = convertArray(triangles);
        // FIXME with glDrawElements use can't use other texture coordinates
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, triangles.length, GLES20.GL_UNSIGNED_SHORT, mIndices);
        GLES20.glFlush();

    }

    public static FloatBuffer convertArray(float[] vertices) {
        FloatBuffer vertexData;
        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);
        vertexData.position(0);
        return vertexData;
    }

    public static ShortBuffer convertArray(short[] indexes) {
        ShortBuffer indexArray;
        indexArray = ByteBuffer
                .allocateDirect(indexes.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        indexArray.put(indexes);
        indexArray.position(0);
        return indexArray;
    }

    public static void shaderEffect2dWholeScreen(Point center, Point center2, int texIn, int programId, int poss, int texx, Integer texIn2) {
        GLES20.glUseProgram(programId);
        int uColorLocation = GLES20.glGetUniformLocation(programId, "u_Color");
        GLES20.glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);

        int uCenter = GLES20.glGetUniformLocation(programId, "uCenter");
        GLES20.glUniform2f(uCenter, (float)center.x, (float)center.y);

        int uCenter2 = GLES20.glGetUniformLocation(programId, "uCenter2");
        GLES20.glUniform2f(uCenter2, (float)center2.x, (float)center2.y);

        FloatBuffer vertexData = convertArray(new float[]{
                -1, -1,
                -1,  1,
                1, -1,
                1,  1
        });

        FloatBuffer texData = convertArray(new float[] {
                0,  0,
                0,  1,
                1,  0,
                1,  1
        });

        GLES20.glVertexAttribPointer(poss, 2, GLES20.GL_FLOAT, false, 0, vertexData);
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
