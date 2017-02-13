package ru.flightlabs.masks.renderer;

import android.opengl.GLES20;
import android.opengl.Matrix;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import ru.flightlabs.masks.utils.PoseHelper;

/**
 * Created by sov on 13.02.2017.
 */

public class ShaderEffectHelper {

    public static void shaderEffect3d(Mat glMatrix, int texIn, int width, int height, final Model modelToDraw, int modelTextureId, float alpha, int programId, int vPos3d, int vTexFor3d) {
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

    public static void effect2dParticle(int width, int height, int programId, int vPos, float[] verticesParticels) {
        GLES20.glUseProgram(programId);

        FloatBuffer vertexData = convertArray(verticesParticels);
        GLES20.glVertexAttribPointer(vPos, 2, GLES20.GL_FLOAT, false, 0, vertexData);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, verticesParticels.length / 2);
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
}
