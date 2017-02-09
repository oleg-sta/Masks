package ru.flightlabs.makeup;

import android.content.Context;
import android.content.res.TypedArray;

import org.opencv.core.Point;
import org.opencv.core.Rect;

import ru.flightlabs.masks.R;

/**
 * Created by sov on 27.11.2016.
 */

public class ResourcesApp {

    // unbelieveable!
    public static Rect face;
    public static Point[] pointsOnFrame;
    public static EditorEnvironment editor;

    // FIXME make it small
    public static TypedArray eyelashesSmall;
    public static TypedArray eyeshadowSmall;
    public static TypedArray eyelinesSmall;
    public static TypedArray lipsSmall;

    public ResourcesApp(Context context) {
        eyelashesSmall = context.getResources().obtainTypedArray(R.array.eyelashes);
        eyeshadowSmall = context.getResources().obtainTypedArray(R.array.eyeshadow);
        eyelinesSmall = context.getResources().obtainTypedArray(R.array.eyelines);
        lipsSmall = context.getResources().obtainTypedArray(R.array.lips);
    }
}