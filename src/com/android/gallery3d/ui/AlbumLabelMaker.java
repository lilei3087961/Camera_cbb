/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import com.android.camera.R;
import com.android.gallery3d.data.BitmapPool;
import com.android.gallery3d.data.DataSourceType;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.ThreadPool.JobContext;


import android.graphics.Paint;

public class AlbumLabelMaker {
    private static final String TAG = "AlbumLabelMaker";
    
    private static final int FONT_COLOR_TITLE = Color.WHITE;
    private static final int FONT_COLOR_COUNT = 0x80FFFFFF;  // 50% white
    private static final int FONT_COLOR_BAR_TEXT = 0xFF545454;
    

    // We keep a border around the album label to prevent aliasing
    private static final int BORDER_SIZE = 1;
    private static final int BACKGROUND_COLOR = 0x40000000; // 25% Dark

    private final LabelSpec mSpec;
    private final TextPaint mTitlePaint;
    private final TextPaint mCountPaint;
    private final Context mContext;

    private int mLabelWidth;
    private BitmapPool mBitmapPool;

    private final LazyLoadedBitmap mLocalSetIcon;
    private final LazyLoadedBitmap mPicasaIcon;
    private final LazyLoadedBitmap mCameraIcon;
    private final LazyLoadedBitmap mMtpIcon;

    private final TextPaint mBarTitlePaint;
    private final TextPaint mBarCountPaint;

    public AlbumLabelMaker(Context context, LabelSpec spec) {
        mContext = context;
        mSpec = spec;
        mTitlePaint = getTextPaint(spec.titleFontSize, FONT_COLOR_TITLE, false);
        mCountPaint = getTextPaint(spec.countFontSize, FONT_COLOR_COUNT, true);

        mLocalSetIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_folder);
        mPicasaIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_picasa);
        mCameraIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_camera);
        mMtpIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_ptp);
        
        mBarTitlePaint = getBarTextPaint(spec.titleFontSize, FONT_COLOR_BAR_TEXT, true);
        mBarCountPaint = getBarTextPaint(spec.countFontSize, FONT_COLOR_BAR_TEXT, true);
    }

    public static int getBorderSize() {
        return BORDER_SIZE;
    }

    private Bitmap getOverlayAlbumIcon(int sourceType) {
        switch (sourceType) {
            case DataSourceType.TYPE_CAMERA:
                return mCameraIcon.get();
            case DataSourceType.TYPE_LOCAL:
                return mLocalSetIcon.get();
            case DataSourceType.TYPE_MTP:
                return mMtpIcon.get();
            case DataSourceType.TYPE_PICASA:
                return mPicasaIcon.get();
        }
        return null;
    }

    private static TextPaint getTextPaint(int textSize, int color, boolean isBold) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setShadowLayer(2f, 0f, 0f, Color.BLACK);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
        return paint;
    }
    
    private static TextPaint getBarTextPaint(int textSize, int color, boolean isBold) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setShadowLayer(2f, 0f, 0f, Color.WHITE);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
        return paint;
    }

    private class LazyLoadedBitmap {
        private Bitmap mBitmap;
        private int mResId;

        public LazyLoadedBitmap(int resId) {
            mResId = resId;
        }

        public synchronized Bitmap get() {
            if (mBitmap == null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                mBitmap = BitmapFactory.decodeResource(
                        mContext.getResources(), mResId, options);
            }
            return mBitmap;
        }
    }

    public synchronized void setLabelWidth(int width) {
        if (mLabelWidth == width) return;
        mLabelWidth = width;
        int borders = 2 * BORDER_SIZE;
        mBitmapPool = new BitmapPool(
                width + borders, mSpec.labelBackgroundHeight + borders, 16);
    }

    public ThreadPool.Job<Bitmap> requestLabel(
            String title, String count, int sourceType) {
        return new AlbumLabelJob(title, count, sourceType);
    }
    
    public ThreadPool.Job<Bitmap> requestBarLabel(
            String title, String count, int sourceType) {
        return new BarLabelJob(title, count, sourceType);
    }

    static void drawText(Canvas canvas,
            int x, int y, String text, int lengthLimit, TextPaint p) {
        // The TextPaint cannot be used concurrently
        synchronized (p) {
            text = TextUtils.ellipsize(
                    text, p, lengthLimit, TextUtils.TruncateAt.END).toString();
            canvas.drawText(text, x, y - p.getFontMetricsInt().ascent, p);
        }
    }

    private class AlbumLabelJob implements ThreadPool.Job<Bitmap> {
        private final String mTitle;
        private final String mCount;
        private final int mSourceType;

        public AlbumLabelJob(String title, String count, int sourceType) {
            mTitle = title;
            mCount = count;
            mSourceType = sourceType;
        }

        @Override
        public Bitmap run(JobContext jc) {
            LabelSpec s = mSpec;

            String title = mTitle;
            String count = mCount;
            Bitmap icon = getOverlayAlbumIcon(mSourceType);

            Bitmap bitmap;
            int labelWidth;

            synchronized (this) {
                labelWidth = mLabelWidth;
                bitmap = mBitmapPool.getBitmap();
            }

            if (bitmap == null) {
                int borders = 2 * BORDER_SIZE;
                bitmap = Bitmap.createBitmap(labelWidth + borders,
                        s.labelBackgroundHeight + borders, Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            canvas.clipRect(BORDER_SIZE, BORDER_SIZE,
                    bitmap.getWidth() - BORDER_SIZE,
                    bitmap.getHeight() - BORDER_SIZE);
            canvas.drawColor(BACKGROUND_COLOR, PorterDuff.Mode.SRC);

            canvas.translate(BORDER_SIZE, BORDER_SIZE);

            // draw title
            if (jc.isCancelled()) return null;
            int x = s.leftMargin;
            int y = s.titleOffset;
            drawText(canvas, x, y, title, labelWidth - s.leftMargin, mTitlePaint);

            // draw the count
            if (jc.isCancelled()) return null;
            if (icon != null) x = s.iconSize;
            y += s.titleFontSize + s.countOffset;
            drawText(canvas, x, y, count,
                    labelWidth - s.leftMargin - s.iconSize, mCountPaint);

            // draw the icon
            if (icon != null) {
                if (jc.isCancelled()) return null;
                float scale = (float) s.iconSize / icon.getWidth();
                canvas.translate(0, bitmap.getHeight()
                        - Math.round(scale * icon.getHeight()));
                canvas.scale(scale, scale);
                canvas.drawBitmap(icon, 0, 0, null);
            }

            return bitmap;
        }
    }

    

    private class BarLabelJob implements ThreadPool.Job<Bitmap> {
        private final String mTitle;
        private final String mCount;
        private final int mSourceType;

        public BarLabelJob(String title, String count, int sourceType) {
            mTitle = title;
            mCount = count;
            mSourceType = sourceType;
        }

        @Override
        public Bitmap run(JobContext jc) {
            LabelSpec s = mSpec;

            String title = mTitle;
            String count = String.format("(%s)",mCount);
            Bitmap icon = getOverlayAlbumIcon(mSourceType);

            Bitmap bitmap;
            int labelWidth;

            synchronized (this) {
                labelWidth = mLabelWidth;
                bitmap = mBitmapPool.getBitmap();
                bitmap = null;
            }

            if (bitmap == null) {
                int borders = 2 * BORDER_SIZE;
                bitmap = Bitmap.createBitmap(labelWidth + borders,
                        s.labelBackgroundHeight + borders, Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            canvas.clipRect(BORDER_SIZE, BORDER_SIZE,
                    bitmap.getWidth() - BORDER_SIZE,
                    bitmap.getHeight() - BORDER_SIZE);
            canvas.translate(BORDER_SIZE, BORDER_SIZE);
            
            // draw the icon
            if (icon != null) {
                if (jc.isCancelled()) return null;
                float scale = (float) s.iconSize / icon.getWidth();
                canvas.scale(scale, scale);
                canvas.drawBitmap(icon, 0, 0, null);
            }
            
            canvas.translate(s.leftMargin, s.titleOffset);
            

            int lenCount = (int)mCountPaint.measureText(count, 0, count.length());
            int lenTitle = (int)mTitlePaint.measureText(title, 0, title.length());
            int maxTitleWidth = labelWidth - lenCount;
            
            if (lenTitle < maxTitleWidth) {
                int offset = (maxTitleWidth - lenTitle)/2;
                drawText(canvas, offset, 0, title, maxTitleWidth, mBarTitlePaint);
                drawText(canvas, (labelWidth - lenCount - offset), 0, count, lenCount, mBarCountPaint);
            } else {
                drawText(canvas, 0, 0, title, maxTitleWidth, mBarTitlePaint);
                drawText(canvas, (labelWidth - lenCount), 0, count, lenCount, mBarCountPaint);
            }
            return bitmap;
        }
    }

    public void recycleLabel(Bitmap label) {
        mBitmapPool.recycle(label);
    }

    public void clearRecycledLabels() {
        if (mBitmapPool != null) mBitmapPool.clear();
    }
}
