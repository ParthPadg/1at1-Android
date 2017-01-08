package org.oneat1.android.util;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import org.oneat1.android.OA1App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by parthpadgaonkar on 1/4/17.
 */
public enum OA1Font {
    HELVETICA_BOLD("HelveticaNeue-Bold.otf"),
    HELVETICA_REGULAR("HelveticaNeue-Roman.otf"),
    FUTURA_BOLD("Futura-Bold.ttf"),
    FUTURA_REGULAR("Futura-Regular.ttf");

    private static final Logger LOG = LoggerFactory.getLogger(OA1Font.class);
    private static final Map<OA1Font, Typeface> FONT_CACHE = new EnumMap<>(OA1Font.class);
    private String fontPath;

    OA1Font(String fontPath) {
        this.fontPath = "fonts/".concat(fontPath);
    }

    public static void init() {
        OA1Font[] values = values();
        for (int i = 0, len = values.length; i < len; i++) {
            values[i].get();
        }
    }

    public Typeface get() {
        return get(this);
    }

    @NonNull
    public static Typeface get(@NonNull OA1Font font) {
        Context context = OA1App.getInstance();
        if (context == null) {
            LOG.warn("Attempted to access fonts before initialization!");
            return Typeface.DEFAULT;
        }
        synchronized (FONT_CACHE) {
            Typeface cache = FONT_CACHE.get(font);
            if (cache == null) {
                Typeface created = Typeface.createFromAsset(context.getAssets(), font.fontPath);
                if (created != null) {
                    FONT_CACHE.put(font, created);
                }
            }
            return FONT_CACHE.get(font);
        }
    }

    public static Typeface getFromStyleableInt(@IntRange(from = 0, to = 3) int value) {
        switch (value) {
            case 0:
                return get(HELVETICA_BOLD);
            case 1:
                return get(HELVETICA_REGULAR);
            case 2:
                return get(FUTURA_BOLD);
            case 3:
                return get(FUTURA_REGULAR);
        }
        //should never reach here
        throw new IllegalArgumentException("Error - this method cannot be called with this value: " + value);
    }
}
