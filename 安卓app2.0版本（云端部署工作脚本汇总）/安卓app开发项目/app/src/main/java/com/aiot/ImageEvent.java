package com.aiot;

import android.graphics.Bitmap;

import java.util.Map;

public class ImageEvent {
    public final Bitmap bitmap;
    public final Map<String, Integer> classCounts;

    public ImageEvent(Bitmap bitmap, Map<String, Integer> classCounts) {
        this.bitmap = bitmap;
        this.classCounts = classCounts;
    }
}

