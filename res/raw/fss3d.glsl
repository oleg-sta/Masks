precision mediump float;        // Set the default precision to medium. We don't need as high of a
                                // precision in the fragment shader.
uniform sampler2D u_TextureOrig; // original screen texture
uniform sampler2D u_Texture; // mask texture
varying vec2 v_TexCoordinate;

// The entry point for our fragment shader.
void main()
{
// TODO use u_TextureOrig for blending
    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
}