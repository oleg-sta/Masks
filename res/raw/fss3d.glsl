precision mediump float;        // Set the default precision to medium. We don't need as high of a
                                // precision in the fragment shader.
uniform sampler2D u_Texture;
varying vec2 v_TexCoordinate;

// The entry point for our fragment shader.
void main()
{
    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
}