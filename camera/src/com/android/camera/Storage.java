/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (c) 2012-2013, The Linux Foundation. All Rights Reserved.
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

package com.android.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import com.android.gallery3d.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Storage {
    private static final String TAG = "CameraStorage";
    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD = 20000000;

    // Added by zhanghongxing at 2013-09-03
    public static final long LOWEST_STORAGE_THRESHOLD = 9000000;

    public static long mStoreDate;
    public static String mStorePath;
    public static Uri mStoreUri;

     public static int CAMERA_BUCKET_ID = 0;
     public static int CAMERA_INTERNAL_BUCKET_ID = 0;            
      
    public static boolean mIsExternalStorage = true;  
    public static String getDirectory(){ 
        String DIRECTORY=null;
        DIRECTORY =getDCIM() + "/Camera"; 
        return DIRECTORY;  
    }  
         
    public static void init() {
        CAMERA_BUCKET_ID = GalleryUtils.getBucketId(
        			FxEnvironment.getExternalStorageDirectory(null) + "/DCIM/Camera");
        CAMERA_INTERNAL_BUCKET_ID = GalleryUtils.getBucketId(
        			FxEnvironment.getInternalStorageDirectory(null) + "/DCIM/Camera");
    }
    public static  String getRawDirectory(){   
        String DIRECTORY=null;   
        DIRECTORY =getDCIM() + "/Camera/raw"; 
        return DIRECTORY;  
    }  

    public static String getDCIM() {
        String DCIM = null;
        if (mIsExternalStorage) {
            DCIM = FxEnvironment.getExternalStoragePublicDirectory();
        } else {
            DCIM = FxEnvironment.getInternalStoragePublicDirectory();
        }
        return DCIM;
    }  
       
    public static String getBucketId(){  
        return String.valueOf(getDirectory().toLowerCase().hashCode());  
    }  
         
    public static String getCameraRawImageBucketId(){ 
        return String.valueOf(getRawDirectory().toLowerCase().hashCode());  
    } 

    public static Uri panoAddImage(ContentResolver resolver, String title, long date,
                Location location, int orientation, byte[] jpeg, int width, int height) {
        // Save the image.
        String path = Storage.generateFilepath(title);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(jpeg);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write image", e);
            return null;
        } finally {
            try {
                out.close();
                jpeg = null;
            } catch (Exception e) {
            }
        }

        Integer size = null;
        try {
            FileInputStream fis = new FileInputStream(path);
            size = Integer.valueOf(fis.available());
        } catch (Exception e) {
            return null;
        }

        // Insert into MediaStore.
        ContentValues values = new ContentValues(9);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + ".jpg");
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.MIME_TYPE, "image/jpeg");
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, size);
        values.put(ImageColumns.WIDTH, width);
        values.put(ImageColumns.HEIGHT, height);

        if (location != null) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        }

        Uri uri = null;
        try {
            uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        return uri;
    }
    //END:Add by wangbin at 2013-7-11.

    public static Uri addImage(ContentResolver resolver, String title, long date,
                Location location, int orientation, byte[] jpeg, int width, int height) {
        // Save the image.
        String path = generateFilepath(title);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(jpeg);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write image", e);
            return null;
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }

        // Insert into MediaStore.
        ContentValues values = new ContentValues(9);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + ".jpg");
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.MIME_TYPE, "image/jpeg");
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, jpeg.length);
        jpeg = null;
        values.put(ImageColumns.WIDTH, width);
        values.put(ImageColumns.HEIGHT, height);

        if (location != null) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        }

        Uri uri = null;
        try {
            uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        return uri;
    }

    // newImage() and updateImage() together do the same work as
    // addImage. newImage() is the first step, and it inserts the DATE_TAKEN and
    // DATA fields into the database.
    //
    // We also insert hint values for the WIDTH and HEIGHT fields to give
    // correct aspect ratio before the real values are updated in updateImage().
    public static Uri newImage(ContentResolver resolver, String title,
            long date, int width, int height, String pictureFormat) {
        String directory = null;
        String ext = null;
        if (pictureFormat == null ||
            pictureFormat.equalsIgnoreCase("jpeg")) {
            ext = ".jpg";
            //BEGIN: Modified by xiongzhu at 2013-04-15
            // directory = DIRECTORY;
            directory = getDirectory();
            //END:   Modified by xiongzhu at 2013-04-15
        } else if (pictureFormat.equalsIgnoreCase("raw")) {
            ext = ".raw";
            //BEGIN: Modified by xiongzhu at 2013-04-15
            // directory = RAW_DIRECTORY;
            directory = getRawDirectory();
            //END:   Modified by xiongzhu at 2013-04-15
        } else {
            Log.e(TAG, "Invalid pictureFormat " + pictureFormat);
            return null;
        }

        String path = directory + '/' + title + ext;

        mStoreDate = date;
        mStorePath = path;

       // Insert into MediaStore.
//        ContentValues values = new ContentValues(4);
//        values.put(ImageColumns.DATE_TAKEN, date);
//        values.put(ImageColumns.DATA, path);
//        values.put(ImageColumns.WIDTH, width);
//        values.put(ImageColumns.HEIGHT, height);
        Uri uri = null;
//        try {
//            uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
//        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
//            Log.e(TAG, "Failed to new image" + th);
//        }
        return uri;
    }

    // This is the second step. It completes the partial data added by
    // newImage. All columns other than DATE_TAKEN and DATA are inserted
    // here. This method also save the image data into the file.
    //
    // Returns true if the update is successful.
    public static boolean updateImage(ContentResolver resolver, Uri uri,
            String title, Location location, int orientation, byte[] jpeg,
            int width, int height, String pictureFormat) {
        // Save the image.

        String directory = null;
        String ext = null;
        if (pictureFormat == null ||
            pictureFormat.equalsIgnoreCase("jpeg")) {
            ext = ".jpg";
            //BEGIN: Modified by xiongzhu at 2013-04-15
            // directory = DIRECTORY;
            directory = getDirectory();
            //END:   Modified by xiongzhu at 2013-04-15
        } else if (pictureFormat.equalsIgnoreCase("raw")) {
            ext = ".raw";
            //BEGIN: Modified by xiongzhu at 2013-04-15
            // directory = RAW_DIRECTORY;
            directory = getRawDirectory();
            //END:   Modified by xiongzhu at 2013-04-15
        } else {
            Log.e(TAG, "Invalid pictureFormat " + pictureFormat);
            return false;
        }

        String path = directory + '/' + title + ext;

        Log.i(TAG,"====zzw:file path="+path);
        String tmpPath = path + ".tmp";
        FileOutputStream out = null;
        try {
            // Write to a temporary file and rename it to the final name. This
            // avoids other apps reading incomplete data.
            File dir = new File(directory);
            if (!dir.exists()) dir.mkdirs();
            out = new FileOutputStream(tmpPath);
            out.write(jpeg);
            out.close();
            new File(tmpPath).renameTo(new File(path));
        } catch (Exception e) {
            Log.e(TAG, "Failed to write image", e);
            return false;
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }

        // Insert into MediaStore.
        ContentValues values = new ContentValues();
        values.put(ImageColumns.DATE_TAKEN, mStoreDate);
        /*TODO
         *newImage() is the first step,updateImage() is the second step. 
         *It completes the partial data added by newImage. mStorePath will 
         *be overwritten by next newImage because it is publick static here
         */
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + ext);
        values.put(ImageColumns.SIZE, jpeg.length);
        jpeg = null;
        // Only for .jpg format, put following columns to database.
        if (ext.equals(".jpg")) {
            values.put(ImageColumns.MIME_TYPE, "image/jpeg");
            // Clockwise rotation in degrees. 0, 90, 180, or 270.
            values.put(ImageColumns.ORIENTATION, orientation);
            values.put(ImageColumns.WIDTH, width);
            values.put(ImageColumns.HEIGHT, height);
            if (location != null) {
                values.put(ImageColumns.LATITUDE, location.getLatitude());
                values.put(ImageColumns.LONGITUDE, location.getLongitude());
            }
        }

        try {
            if (ext.equals(".jpg")) {
                // For .jpg format, insert images uri into media provider
                mStoreUri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                // For .raw format, insert files uri not images uri into media provider
                mStoreUri = resolver.insert(Files.getContentUri("external"), values);
            }
            //resolver.update(uri, values, null, null);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to update image" + th);
            return false;
        }

        return true;
    }

    public static boolean updateImageByUri(ContentResolver resolver, Uri uri,
    		Location location, int orientation, byte[] jpeg,
            int width, int height, String pictureFormat) {
    	if(uri == null) return false;
        // Save the image.
        String path = uri.getPath();
        String tmpPath = path + ".tmp";
        FileOutputStream out = null;
        try {
            // Write to a temporary file and rename it to the final name. This
            // avoids other apps reading incomplete data.
            File dir = new File(getDirectory());
            if (!dir.exists()) dir.mkdirs();
            out = new FileOutputStream(tmpPath);
            out.write(jpeg);
            out.close();
            new File(tmpPath).renameTo(new File(path));
        } catch (Exception e) {
            Log.e(TAG, "Failed to write image", e);
            return false;
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }

        // Insert into MediaStore.
        ContentValues values = new ContentValues();
        values.put(ImageColumns.DATE_TAKEN, mStoreDate);
		/*TODO
		 *newImage() is the first step,updateImage() is the second step. 
		 *It completes the partial data added by newImage. mStorePath will 
		 *be overwritten by next newImage because it is publick static here
		 */
        values.put(ImageColumns.DATA, path);
//        values.put(ImageColumns.TITLE, title);
//        values.put(ImageColumns.DISPLAY_NAME, title + ext);
        values.put(ImageColumns.SIZE, jpeg.length);
        jpeg = null;
        // Only for .jpg format, put following columns to database.
        if (path.endsWith(".jpg")) {
            values.put(ImageColumns.MIME_TYPE, "image/jpeg");
            // Clockwise rotation in degrees. 0, 90, 180, or 270.
            values.put(ImageColumns.ORIENTATION, orientation);
            values.put(ImageColumns.WIDTH, width);
            values.put(ImageColumns.HEIGHT, height);
            if (location != null) {
                values.put(ImageColumns.LATITUDE, location.getLatitude());
                values.put(ImageColumns.LONGITUDE, location.getLongitude());
            }
        }

        try {
            if (path.endsWith(".jpg")) {
                // For .jpg format, insert images uri into media provider
                mStoreUri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                // For .raw format, insert files uri not images uri into media provider
                mStoreUri = resolver.insert(Files.getContentUri("external"), values);
            }
            //resolver.update(uri, values, null, null);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to update image" + th);
            return false;
        }

        return true;
    }
    public static Uri updateUri() {
        return mStoreUri;
    }

    public static void deleteImage(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to delete image: " + uri);
        }
    }

    public static String generateFilepath(String title) {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // return DIRECTORY + '/' + title + ".jpg";
        return getDirectory()+ '/' + title + ".jpg";
        //END:   Modified by xiongzhu at 2013-04-15
    }

    public static long getAvailableSpace() {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // String state = Environment.getExternalStorageState();
        String state=null;
        // added by zhu.xiong for bug ARD-98 about camera can not work at 2013-06-03 begin
        if (mIsExternalStorage) {
             //START : modified by xiongzhu for ARD-141
            /**if (Build.DOUBLE_SDCARD_SWITCH) {
                state = Environment.getSubExternalStorageState();
            } else {
                state = Environment.getExternalStorageState();
            }*/
            state = FxEnvironment.getExternalStorageState();
            //END : modified by xiongzhu for ARD-141
        } else {
            //START : modified by xiongzhu for ARD-141
            /**if (Build.DOUBLE_SDCARD_SWITCH) {
                state = Environment.getSubInternalStorageState();
            } else {
                state = Environment.getInternalStorageState();
            }*/
            state = FxEnvironment.getInternalStorageState();
           //END : modified by xiongzhu for ARD-141
        }
        // added by zhu.xiong for bug ARD-98 about camera can not work at 2013-06-03 end
        //END:   Modified by xiongzhu at 2013-04-15
        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(getDirectory());
        if (!dir.exists()) {
        	dir.mkdirs();
        }

        if(!dir.isDirectory() || !dir.canWrite())
        	return UNAVAILABLE;

        try {
            StatFs stat = new StatFs(getDirectory());
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }

    /**
     * OSX requires plugged-in USB storage to have path /DCIM/NNNAAAAA to be
     * imported. This is a temporary fix for bug#1655552.
     */
    public static void ensureOSXCompatible() {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // File nnnAAAAA = new File(DCIM, "100ANDRO");
        File nnnAAAAA = new File(getDCIM(), "100ANDRO");
        //END:   Modified by xiongzhu at 2013-04-15
        if (!(nnnAAAAA.exists() || nnnAAAAA.mkdirs())) {
            Log.e(TAG, "Failed to create " + nnnAAAAA.getPath());
        }
    }
}
