/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (c) 2012, The Linux Foundation. All Rights Reserved.
 *
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
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

package com.android.gallery3d.photoeditor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.Gravity;
import android.widget.Toast;

import com.android.camera.R;
import com.android.gallery3d.util.BucketNames;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;

import com.android.gallery3d.common.OurEnvironment;

/**
 * Asynchronous task for saving edited photo as a new copy.
 */
public class SaveCopyTask extends AsyncTask<Bitmap, Void, Uri> {

    /**
     * Callback for the completed asynchronous task.
     */
    public interface Callback {

        void onComplete(Uri result);
    }

    private interface ContentResolverQueryCallback {

        void onCursorResult(Cursor cursor);
    }

    private static final String TIME_STAMP_NAME = "'IMG'_yyyyMMdd_HHmmss";

    private final Context context;
    private final Uri sourceUri;
    private final Callback callback;
    private final String saveFileName;
    private String saveFolderName;
    private File directory = null;

    public SaveCopyTask(Context context, Uri sourceUri, Callback callback) {
        this.context = context;
        this.sourceUri = sourceUri;
        this.callback = callback;

        saveFileName = new SimpleDateFormat(TIME_STAMP_NAME).format(
                new Date(System.currentTimeMillis()));
    }

    /**
     * The task should be executed with one given bitmap to be saved.
     */
    @Override
    protected Uri doInBackground(Bitmap... params) {
        // TODO: Support larger dimensions for photo saving.
        if (params[0] == null) {
            return null;
        }
        // Use the default save directory if the source directory cannot be saved.
        File saveDirectory = getSaveDirectory();
        directory = saveDirectory;
        if ((saveDirectory == null) || !saveDirectory.canWrite()) {
            saveDirectory = new File(OurEnvironment.getExternalStorageDirectory(),
                    BucketNames.EDITED_ALBUM);
            saveFolderName = context.getString(R.string.folder_edited);
        } else {

            // Translate the folder name in the toast popped up,
            // when saving the picture edited under that folder.
            final int bucketId = GalleryUtils.getBucketId(saveDirectory.getAbsolutePath());
            if (bucketId == MediaSetUtils.CAMERA_BUCKET_ID) {
                saveFolderName = context.getString(R.string.folder_camera);
            } else if (bucketId == MediaSetUtils.DOWNLOAD_BUCKET_ID) {
                saveFolderName = context.getString(R.string.folder_download);
            } else if (bucketId == MediaSetUtils.IMPORTED_BUCKET_ID) {
                saveFolderName = context.getString(R.string.folder_imported);
            } else if (bucketId == MediaSetUtils.SNAPSHOT_BUCKET_ID) {
                saveFolderName = context.getString(R.string.folder_screenshot);
            } else if (bucketId == MediaSetUtils.EDITED_ALBUM_BUCKET_ID) {
                saveFolderName = context.getString(R.string.folder_edited);
            } else {
                saveFolderName = saveDirectory.getName();
            }
        }

        Bitmap bitmap = params[0];
        File file = new BitmapUtils(context).saveBitmap(
                bitmap, saveDirectory, saveFileName, Bitmap.CompressFormat.JPEG);
        Uri uri = (file != null) ? insertContent(file) : null;
        bitmap.recycle();
        return uri;
    }

    @Override
    protected void onPostExecute(Uri result) {
    	// BEGIN: Modified by guoruijuan at 2013-11-11
        //about CBB-641 to modify the toast of the editted picture when the phone or SDcard is full.
    	String message = null;
    	
    	if(result != null){
    	    message = context.getString(R.string.photo_saved, saveFolderName);
    	} else {
    	    String externalStoragePath = OurEnvironment.getExternalStorageDirectory().getPath();
            String sdcardStoragePath = OurEnvironment.getSdCardStoragePath(context);
            
            if (directory.getPath().startsWith(externalStoragePath)) {
                message = context.getString(R.string.save_failure_edited_external);
            } else if (directory.getPath().startsWith(sdcardStoragePath)) {
            	message = context.getString(R.string.save_failure_edited_sdcard);
            }
    	}
        /*String message = (result == null) ? context.getString(R.string.saving_failure)
                : context.getString(R.string.photo_saved, saveFolderName);*/   	
        // END: Modified by guoruijuan at 2013-11-11	
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        callback.onComplete(result);
    }

    private void querySource(String[] projection, ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(sourceUri, projection, null, null, null);
            if ((cursor != null) && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
        } catch (Exception e) {
            // Ignore error for lacking the data column from the source.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private File getSaveDirectory() {
        final File[] dir = new File[1];
        querySource(new String[] { ImageColumns.DATA }, new ContentResolverQueryCallback () {

            @Override
            public void onCursorResult(Cursor cursor) {
                dir[0] = new File(cursor.getString(0)).getParentFile();
            }
        });
        return dir[0];
    }

    /**
     * Insert the content (saved file) with proper source photo properties.
     */
    private Uri insertContent(File file) {
        long now = System.currentTimeMillis() / 1000;

        // DATE_TAKEN is in millisecond.
        // DATE_MODIFIED and DATE_ADDED are in second.
        final ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, saveFileName);
        values.put(Images.Media.DISPLAY_NAME, file.getName());
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.DATE_TAKEN, now * 1000);
        values.put(Images.Media.DATE_MODIFIED, now);
        values.put(Images.Media.DATE_ADDED, now);
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, file.getAbsolutePath());
        values.put(Images.Media.SIZE, file.length());

        String[] projection = new String[] {
            ImageColumns.DATE_TAKEN,
            ImageColumns.LATITUDE,
            ImageColumns.LONGITUDE,
        };
        querySource(projection, new ContentResolverQueryCallback() {

            @Override
            public void onCursorResult(Cursor cursor) {
                double latitude = cursor.getDouble(1);
                double longitude = cursor.getDouble(2);
                // TODO: Change || to && after the default location issue is fixed.
                if ((latitude != 0f) || (longitude != 0f)) {
                    values.put(Images.Media.LATITUDE, latitude);
                    values.put(Images.Media.LONGITUDE, longitude);
                }
            }
        });

        return context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}
