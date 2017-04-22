package ru.flightlabs.masks.renderer;

/**
 * Created by sov on 29.01.2017.
 */

public class EffectShader {
    public int programId;
    public String model3dName;
    public String textureName;
    public float alpha;
    public boolean needBlendShape;
    public String textureNamBlendshape;

    private EffectShader() {};

    public static EffectShader parseString(String effectStr) {
        EffectShader effect = new EffectShader();
        String[] par = (effectStr + ";;;;;;;;;;;;;;0").split(";");
        effect.programId = Integer.parseInt(par[0]);
        effect.model3dName = par[1];
        effect.textureName = par[2];
        effect.alpha = Float.parseFloat(par[3]);
        effect.needBlendShape = "1".equals(par[4]);
        effect.textureNamBlendshape = par[5];
        return effect;
    }
}
