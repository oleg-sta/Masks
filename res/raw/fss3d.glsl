precision mediump float;        // Set the default precision to medium. We don't need as high of a
                                // precision in the fragment shader.
uniform sampler2D u_TextureOrig; // original screen texture
uniform sampler2D u_Texture; // mask texture
varying vec2 v_TexCoordinate;
varying vec2 v_TexOrigCoordinate;

// The entry point for our fragment shader.
void main()
{
// TODO use u_TextureOrig for blending
    vec4 maskColor = texture2D(u_Texture, v_TexCoordinate);
    vec4 origColor = texture2D(u_TextureOrig, v_TexOrigCoordinate);
    float alpha =  maskColor[3] * 0.7;
    gl_FragColor = maskColor * alpha + (1.0 - alpha) * origColor;
    //gl_FragColor = maskColor;
}