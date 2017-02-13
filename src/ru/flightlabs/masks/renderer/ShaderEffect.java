package ru.flightlabs.masks.renderer;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import ru.flightlabs.masks.R;
import ru.flightlabs.masks.utils.FileUtils;
import ru.flightlabs.masks.utils.PoseHelper;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 06.02.2017.
 */

public abstract class ShaderEffect {

    protected Context context;

    // 3d
    public Model model;
    protected int program2dJustCopy;
    protected int program2dTriangles;

    private static final String TAG = "ShaderEffect";

    public ShaderEffect(Context contex) {
        this.context = contex;
    }
    public void init() {
        Log.i(TAG, "init");
        load3dModel();
        program2dJustCopy = ShaderUtils.createProgram(ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/vss_2d.glsl")), ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/fss_2d_simple.glsl")));
        program2dTriangles = ShaderUtils.createProgram(ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/vss_2d.glsl")), ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.getStringFromAsset(context.getAssets(), "shaders/fss_solid.glsl")));

    }

    public boolean needBlend() {
        //return effectsMap.get(Static.newIndexEye).needBlendShape;
        return false;
    }

    private void load3dModel() {
        Log.i(TAG, "load3dModel");
        model = new Model(R.raw.for_android_test,
                context);
        Log.i(TAG, "load3dModel exit");
    }

    public abstract void makeShaderMask(int indexEye, PoseHelper.PoseResult poseResult, int width, int height, int texIn, long time, int iGlobTime);


}
