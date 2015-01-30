/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.FloatMath;

// StringTexture is a texture shows the content of a specified String.
//
// To create a StringTexture, use the newInstance() method and specify
// the String, the font size, and the color.
class StringTexture extends CanvasTexture {
    private final String mText;
    private final TextPaint mPaint;
    private final FontMetricsInt mMetrics;

    private StringTexture(String text, TextPaint paint,
            FontMetricsInt metrics, int width, int height) {
        super(width, height);
        mText = text == null ? "" : text;
        mPaint = paint;
        mMetrics = metrics;
    }

    public static TextPaint getDefaultPaint(float textSize, int color) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setShadowLayer(2f, 0f, 0f, Color.BLACK);
        return paint;
    }

    public static StringTexture newInstance(
            String text, float textSize, int color) {
        return newInstance(text, getDefaultPaint(textSize, color));
    }

    public static StringTexture newInstance(
            String text, float textSize, int color,
            float lengthLimit, boolean isBold) {
        TextPaint paint = getDefaultPaint(textSize, color);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
        if (lengthLimit > 0) {
            text = TextUtils.ellipsize(
                    text, paint, lengthLimit, TextUtils.TruncateAt.END).toString();
        }
        return newInstance(text, paint);
    }

    private static StringTexture newInstance(String text, TextPaint paint) {
        FontMetricsInt metrics = paint.getFontMetricsInt();
        int width = text == null ? 1 : (int) FloatMath.ceil(paint.measureText(text));
        int height = metrics.bottom - metrics.top;
        // The texture size needs to be at least 1x1.
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        int viewW = 480;
        if(width >= viewW) {
        	long textLength = text.length();
            int viewLength = (int)(textLength * viewW / width - 4);
            text = text.substring(0, viewLength)+"...";
            width = text == null ? 1 : (int) FloatMath.ceil(paint.measureText(text));

        }
        return new StringTexture(text, paint, metrics, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas, Bitmap backing) {
        canvas.translate(0, -mMetrics.ascent);
        canvas.drawText(mText, 0, 0, mPaint);
    }

    public static StringTexture newInstance(
            String text, float textSize, int color,int maxWidth) {
        return newInstance(text, getDefaultPaint(textSize, color),maxWidth);
    }

    private static StringTexture newInstance(String text, TextPaint paint,int maxWidth) {
        FontMetricsInt metrics = paint.getFontMetricsInt();
        int width = text == null ? 1 : (int) FloatMath.ceil(paint.measureText(text));
        int height = metrics.bottom - metrics.top;
        // The texture size needs to be at least 1x1.
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        int viewW = maxWidth;
        if(width >= viewW) {
            long textLength = text.length();
            int viewLength = (int)(textLength * viewW / width - 4);
            text = text.substring(0, viewLength)+"...";
            width = text == null ? 1 : (int) FloatMath.ceil(paint.measureText(text));

        }
        return new StringTexture(text, paint, metrics, width, height);
    }
}
