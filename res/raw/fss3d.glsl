precision mediump float;        // Set the default precision to medium. We don't need as high of a
                                // precision in the fragment shader.
// The entry point for our fragment shader.
void main()
{
    gl_FragColor = vec4(1.0, 1.0, 0.5, 1.0);
  }