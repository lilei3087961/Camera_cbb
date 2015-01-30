/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.gallery3d.gadget2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.Log;

import com.android.camera.R;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.util.ThreadPool;

public class WidgetUtils {

    private static final String TAG = "WidgetUtils";
    
    private static int sStackPhotoWidth = 220;
    private static int sStackPhotoHeight = 170;    
    
    private static int sHlistPhotoWidth = 220;
    private static int sHlistPhotoHeight = 170;

    private static int sPreviewPhotoWidth = 800;
    private static int sPreviewPhotoHeight = 600;

    private WidgetUtils() {
    }

    public static void initialize(Context context) {
        Resources r = context.getResources();

        sStackPhotoWidth = r.getDimensionPixelSize(R.dimen.stack_photo_width);
        sStackPhotoHeight = r.getDimensionPixelSize(R.dimen.stack_photo_height);
        
        sHlistPhotoWidth = r.getDimensionPixelSize(R.dimen.hlist_photo_width);
        sHlistPhotoHeight = r.getDimensionPixelSize(R.dimen.hlist_photo_height);
        
        sPreviewPhotoWidth = r.getDimensionPixelSize(R.dimen.preview_photo_width);
        sPreviewPhotoHeight = r.getDimensionPixelSize(R.dimen.preview_photo_height);
    }

    public static Bitmap createWidgetBitmap(MediaItem image) {
        Bitmap bitmap = image.requestImage(MediaItem.TYPE_THUMBNAIL)
               .run(ThreadPool.JOB_CONTEXT_STUB);
        if (bitmap == null) {
            Log.w(TAG, "fail to get image of " + image.toString());
            return null;
        }
        return createWidgetBitmap(bitmap, image.getRotation());
    }
    
    public static Bitmap createWidgetBitmap(Bitmap bitmap, int rotation) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        float scale;
        if (((rotation / 90) & 1) == 0) {
            scale = Math.max((float) sStackPhotoWidth / w,
                    (float) sStackPhotoHeight / h);
        } else {
            scale = Math.max((float) sStackPhotoWidth / h,
                    (float) sStackPhotoHeight / w);
        }

        Bitmap target = Bitmap.createBitmap(
                sStackPhotoWidth, sStackPhotoHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        canvas.translate(sStackPhotoWidth / 2, sStackPhotoHeight / 2);
        canvas.rotate(rotation);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, -w / 2, -h / 2, paint);
        return target;
    }    

    public static Bitmap createHlistWidgetBitmap(MediaItem image) {
        Bitmap bitmap = image.requestImage(MediaItem.TYPE_THUMBNAIL)
               .run(ThreadPool.JOB_CONTEXT_STUB);
        if (bitmap == null) {
            Log.w(TAG, "fail to get image of " + image.toString());
            return null;
        }
        return createHlistWidgetBitmap(bitmap, image.getRotation());
    }

    public static Bitmap createHlistWidgetBitmap(Bitmap bitmap, int rotation) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        float scale;
        if (((rotation / 90) & 1) == 0) {
            scale = Math.max((float) sHlistPhotoWidth / w,
                    (float) sHlistPhotoHeight / h);
        } else {
            scale = Math.max((float) sHlistPhotoWidth / h,
                    (float) sHlistPhotoHeight / w);
        }

        Bitmap target = Bitmap.createBitmap(
                sHlistPhotoWidth, sHlistPhotoHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        canvas.translate(sHlistPhotoWidth / 2, sHlistPhotoHeight / 2);
        canvas.rotate(rotation);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, -w / 2, -h / 2, paint);
        return target;
    }

    public static Bitmap createPreviewWidgetBitmap(MediaItem image) {
        Bitmap bitmap = image.requestImage(MediaItem.TYPE_THUMBNAIL)
               .run(ThreadPool.JOB_CONTEXT_STUB);
        if (bitmap == null) {
            Log.w(TAG, "fail to get image of " + image.toString());
            return null;
        }
        return createPreviewWidgetBitmap(bitmap, image.getRotation());
    }

    public static Bitmap createPreviewWidgetBitmap(Bitmap bitmap, int rotation) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        float scale;
        if (((rotation / 90) & 1) == 0) {
            scale = Math.max((float) sPreviewPhotoWidth / w,
                    (float) sPreviewPhotoHeight / h);
        } else {
            scale = Math.max((float) sPreviewPhotoWidth / h,
                    (float) sPreviewPhotoHeight / w);
        }

        Bitmap target = Bitmap.createBitmap(
                sPreviewPhotoWidth, sPreviewPhotoHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        canvas.translate(sPreviewPhotoWidth / 2, sPreviewPhotoHeight / 2);
        canvas.rotate(rotation);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, -w / 2, -h / 2, paint);
        return target;
    }      
}
