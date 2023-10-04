package com.example.drawing;

import android.app.Notification;
import android.graphics.Bitmap;
import android.view.View;

public class MyPaintView extends View {
    public Notification.Builder mPaint;
    public Bitmap mBitmap;

    public MyPaintView(MainActivity mainActivity) {
        super(mainActivity);

    }
}
