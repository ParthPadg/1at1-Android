package org.oneat1.android.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import org.oneat1.android.R;

/**
 * Created by parthpadgaonkar on 1/4/17.
 */

public class TypefaceTextView extends TextView {
    public TypefaceTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (!isInEditMode()) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TypefaceTextView, 0, 0);

        int typefaceEnumValue = array.getInt(R.styleable.TypefaceTextView_typeface, 0);
        Typeface typeface = OA1Font.getFromStyleableInt(typefaceEnumValue);

        setTypeface(typeface);
        setPaintFlags(getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);

        array.recycle();
    }
}
}
