/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2013, Linux Foundation. All rights reserved.
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

package com.android.gallery3d.data;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.util.FloatMath;
import android.util.Log;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool.CancelListener;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.android.internal.util.MemInfoReader;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import android.graphics.Rect;

public class DecodeUtils {
    private static final String TAG = "DecodeService";

    private static class DecodeCanceller implements CancelListener {
        Options mOptions;

        public DecodeCanceller(Options options) {
            mOptions = options;
        }

        @Override
        public void onCancel() {
            mOptions.requestCancelDecode();
        }
    }

    public static Bitmap decode(JobContext jc, FileDescriptor fd, Options options) {
        if (options == null) options = new Options();
        jc.setCancelListener(new DecodeCanceller(options));
        return ensureGLCompatibleBitmap(
                BitmapFactory.decodeFileDescriptor(fd, null, options));
    }

    public static void decodeBounds(JobContext jc, FileDescriptor fd,
            Options options) {
        Utils.assertTrue(options != null);
        options.inJustDecodeBounds = true;
        jc.setCancelListener(new DecodeCanceller(options));
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        options.inJustDecodeBounds = false;
    }

    public static Bitmap decode(JobContext jc, byte[] bytes, Options options) {
        return decode(jc, bytes, 0, bytes.length, options);
    }

    public static Bitmap decode(JobContext jc, byte[] bytes, int offset,
            int length, Options options) {
        if (options == null) options = new Options();
        jc.setCancelListener(new DecodeCanceller(options));
        return ensureGLCompatibleBitmap(
                BitmapFactory.decodeByteArray(bytes, offset, length, options));
    }

    public static void decodeBounds(JobContext jc, byte[] bytes, int offset,
            int length, Options options) {
        Utils.assertTrue(options != null);
        options.inJustDecodeBounds = true;
        jc.setCancelListener(new DecodeCanceller(options));
        BitmapFactory.decodeByteArray(bytes, offset, length, options);
        options.inJustDecodeBounds = false;
    }

    public static Bitmap decodeThumbnail(
            JobContext jc, Context context, String filePath, Options options, int targetSize, int type) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            FileDescriptor fd = fis.getFD();
            return decodeThumbnail(jc, context, fd, options, targetSize, type);
        } catch (Exception ex) {
            Log.w(TAG, ex);
            return null;
        } finally {
            Utils.closeSilently(fis);
        }
    }

    public static Bitmap decodeThumbnail(JobContext jc, Context context,
            FileDescriptor fd, Options options, int targetSize, int type) {
        /*long fileSize = 0;
        try {
             ParcelFileDescriptor parcelFD =  ParcelFileDescriptor.dup(fd);
             fileSize = parcelFD.getStatSize();
        } catch (IOException e) {
            Log.w(TAG, e);
            return null;
        }

        ActivityManager.MemoryInfo activityManagerMemoryInfo = getActivityManagerMemoryInfo(context);
        long availMem = activityManagerMemoryInfo.availMem;
        long thresholdMem = activityManagerMemoryInfo.threshold;

        //as in the process of "decodeFileDescriptor" also need lots of Memory, and will outMemory
        if((fileSize + thresholdMem) >= availMem){
            return null;
        }*/

        if (options == null) options = new Options();
        jc.setCancelListener(new DecodeCanceller(options));

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        if (jc.isCancelled()) return null;

        int w = options.outWidth;
        int h = options.outHeight;

        /*activityManagerMemoryInfo = getActivityManagerMemoryInfo(context);

        if (activityManagerMemoryInfo.lowMemory) {
            Log.w(TAG, "system in low memory status");
            return null;
         } else {
             // For bmp file , calculate max ARGB_8888 bitmap memory, each pixel takes 32 bits. so there is 4L.
             long maxBitmapMem = w * h * 4L;
             availMem = activityManagerMemoryInfo.availMem;
             thresholdMem = activityManagerMemoryInfo.threshold;

             if ((maxBitmapMem + thresholdMem) >= availMem) {
                 Log.w(TAG, "the Memory of loading the Bitmap exceeds avail Memory");
                 return null;
             }
         }*/

        if (type == MediaItem.TYPE_MICROTHUMBNAIL) {
            // We center-crop the original image as it's micro thumbnail. In this case,
            // we want to make sure the shorter side >= "targetSize".
            float scale = (float) targetSize / Math.min(w, h);
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);

            // For an extremely wide image, e.g. 300x30000, we may got OOM when decoding
            // it for TYPE_MICROTHUMBNAIL. So we add a max number of pixels limit here.
            final int MAX_PIXEL_COUNT = 640000; // 400 x 1600
            if ((w / options.inSampleSize) * (h / options.inSampleSize) > MAX_PIXEL_COUNT) {
                options.inSampleSize = BitmapUtils.computeSampleSize(
                        FloatMath.sqrt((float) MAX_PIXEL_COUNT / (w * h)));
            }
        } else {
            // For screen nail, we only want to keep the longer side >= targetSize.
            float scale = (float) targetSize / Math.max(w, h);
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
        }
        options.inJustDecodeBounds = false;

        Bitmap result = BitmapFactory.decodeFileDescriptor(fd, null, options);
        if (result == null) return null;

        // We need to resize down if the decoder does not support inSampleSize
        // (For example, GIF images)
        float scale = (float) targetSize / (type == MediaItem.TYPE_MICROTHUMBNAIL
                ? Math.min(result.getWidth(), result.getHeight())
                : Math.max(result.getWidth(), result.getHeight()));

        if (scale <= 0.5) result = BitmapUtils.resizeBitmapByScale(result, scale, true);
        return ensureGLCompatibleBitmap(result);
    }

    /**
     * Decodes the bitmap from the given byte array if the image size is larger than the given
     * requirement.
     *
     * Note: The returned image may be resized down. However, both width and height must be
     * larger than the <code>targetSize</code>.
     */
    public static Bitmap decodeIfBigEnough(JobContext jc, byte[] data,
            Options options, int targetSize) {
        if (options == null) options = new Options();
        jc.setCancelListener(new DecodeCanceller(options));

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (jc.isCancelled()) return null;
        if (options.outWidth < targetSize || options.outHeight < targetSize) {
            return null;
        }
        options.inSampleSize = BitmapUtils.computeSampleSizeLarger(
                options.outWidth, options.outHeight, targetSize);
        options.inJustDecodeBounds = false;
        return ensureGLCompatibleBitmap(
                BitmapFactory.decodeByteArray(data, 0, data.length, options));
    }
    
    /**
     * get thumbnail image from big image.
     * 
     * condition: heigth width must larger that 1024;
     * 
     * Note: here get the center rectangle of the image. size: 1024*1024
     * */
    public static Bitmap decodeThumbnailFromLargeBitmap(
            JobContext jc, String filePath, Options options) {
        final int DEFAULT_THUMBNAIL_BITMAP_SIZE = 1024;
        
        try {
            BitmapRegionDecoder decoder = createBitmapRegionDecoder(jc, filePath, false);
            
            int height = decoder.getHeight();
            int width = decoder.getWidth();
            
            int left = (width - DEFAULT_THUMBNAIL_BITMAP_SIZE) / 2;
            int top = (height - DEFAULT_THUMBNAIL_BITMAP_SIZE) / 2;
            int right = left + DEFAULT_THUMBNAIL_BITMAP_SIZE;
            int bottom = top + DEFAULT_THUMBNAIL_BITMAP_SIZE;
            
            Rect rect = new Rect(left, top, right, bottom);
        
            return decoder.decodeRegion(rect, options);
        } catch (Throwable t) {
            Log.w(TAG, t);
            return null;
        }         
    }

    // TODO: This function should not be called directly from
    // DecodeUtils.requestDecode(...), since we don't have the knowledge
    // if the bitmap will be uploaded to GL.
    public static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != null) return bitmap;
        Bitmap newBitmap = bitmap.copy(Config.ARGB_8888, false);
        bitmap.recycle();
        return newBitmap;
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(
            JobContext jc, byte[] bytes, int offset, int length,
            boolean shareable) {
        if (offset < 0 || length <= 0 || offset + length > bytes.length) {
            throw new IllegalArgumentException(String.format(
                    "offset = %s, length = %s, bytes = %s",
                    offset, length, bytes.length));
        }

        try {
            return BitmapRegionDecoder.newInstance(
                    bytes, offset, length, shareable);
        } catch (Throwable t)  {
            Log.w(TAG, t);
            return null;
        }
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(
            JobContext jc, String filePath, boolean shareable) {
        try {      	
        	if(shareable){
        		return BitmapRegionDecoder.newInstance(filePath, false);
        	}else{
            	return null;
        	}
        } catch (Throwable t)  {
            Log.w(TAG, t);
            return null;
        }
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(
            JobContext jc, FileDescriptor fd, boolean shareable) {
        try {
            return BitmapRegionDecoder.newInstance(fd, shareable);
        } catch (Throwable t)  {
            Log.w(TAG, t);
            return null;
        }
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(
            JobContext jc, InputStream is, boolean shareable) {
        try {
            return BitmapRegionDecoder.newInstance(is, shareable);
        } catch (Throwable t)  {
            // We often cancel the creating of bitmap region decoder,
            // so just log one line.
            Log.w(TAG, "requestCreateBitmapRegionDecoder: " + t);
            return null;
        }
    }

    public static ActivityManager.MemoryInfo getActivityManagerMemoryInfo(Context context) {
        ActivityManager.MemoryInfo activityManagerMemoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager)context
                .getSystemService(android.content.Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(activityManagerMemoryInfo);

        return activityManagerMemoryInfo;
   }

}
