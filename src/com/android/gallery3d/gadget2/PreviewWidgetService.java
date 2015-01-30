/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (c) 2011-2012, The Linux Foundation. All Rights Reserved.
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

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.util.Log;

import com.android.camera.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;

public class PreviewWidgetService extends RemoteViewsService {

    @SuppressWarnings("unused")
    private static final String TAG = "GalleryAppWidgetService";

    public static final String EXTRA_WIDGET_TYPE = "widget-type";
    public static final String EXTRA_ALBUM_PATH = "album-path";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        int type = intent.getIntExtra(EXTRA_WIDGET_TYPE, 0);
        String albumPath = intent.getStringExtra(EXTRA_ALBUM_PATH);

        return new PhotoRVFactory((GalleryApp) getApplicationContext(),
                id, type, albumPath);
    }

    private static class EmptySource implements WidgetSource {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Bitmap getImage(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uri getContentUri(int index) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Bitmap getPreviewImage(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bitmap getHlistImage(int index) {
            throw new UnsupportedOperationException();
        }        

        @Override
        public void setContentListener(ContentListener listener) {}

        @Override
        public void reload() {}

        @Override
        public void close() {}
    }

    private static class PhotoRVFactory implements
            RemoteViewsService.RemoteViewsFactory, ContentListener {

        private final int mAppWidgetId;
        private final int mType;
        private final String mAlbumPath;
        private final GalleryApp mApp;

        private WidgetSource mSource;

        public PhotoRVFactory(GalleryApp app, int id, int type, String albumPath) {
            mApp = app;
            mAppWidgetId = id;
            mType = type;
            mAlbumPath = albumPath;
        }

        @Override
        public void onCreate() {
            if (mType == WidgetDatabaseHelper.TYPE_ALBUM) {
                Path path = Path.fromString(mAlbumPath);
                DataManager manager = mApp.getDataManager();
                MediaSet mediaSet = (MediaSet) manager.getMediaObject(path);
                mSource = mediaSet == null
                        ? new EmptySource()
                        : new MediaSetSource(mediaSet);
            } else {
                mSource = new LocalPhotoSource(mApp.getAndroidContext());
            }
            mSource.setContentListener(this);
            AppWidgetManager.getInstance(mApp.getAndroidContext())
                    .notifyAppWidgetViewDataChanged(
                    mAppWidgetId, R.id.appwidget_horizontallist_view);
        }

        @Override
        public void onDestroy() {
            mSource.close();
            mSource = null;
        }

        public int getCount() {
            return mSource.size();
        }

        public long getItemId(int position) {
            return position;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean hasStableIds() {
            return true;
        }

        public RemoteViews getLoadingView() {
            RemoteViews rv = new RemoteViews(
                    mApp.getAndroidContext().getPackageName(),
                    R.layout.appwidget2_preview_loading_item);
            rv.setProgressBar(R.id.appwidget2_preview_loading_item, 0, 0, true);
            return rv;
        }

        public RemoteViews getViewAt(int position) {
            Bitmap bitmap = null;
            if (mSource != null) {
                try {
                    bitmap = mSource.getPreviewImage(position);
                } catch (RuntimeException ex) {
                    // when fail to get item from media provider, RuntimeException will be thrown.
                    Log.w(TAG, ex);
                    return null;
                }
            } else {
                // when widget is destroyed, mSource can be null
                Log.w(TAG, "mSource is null, not to get bitmap");
                return null;
            }
            if (bitmap == null) return getLoadingView();
            // appwidget2_preview_image
            Log.w(TAG, "hzn test position:" + position);
            RemoteViews views = new RemoteViews(
                    mApp.getAndroidContext().getPackageName(),
                    R.layout.appwidget2_preview_item);
            views.setImageViewBitmap(R.id.appwidget2_preview_item, bitmap);
            if (mSource != null) {
                views.setOnClickFillInIntent(R.id.appwidget2_preview_item, new Intent()
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .setData(mSource.getContentUri(position)));
            } else {
                // when widget is destroyed, mSource can be null
                Log.w(TAG, "mSource is null, not to get content uri");
                return null;
            }
            return views;
        }

        @Override
        public void onDataSetChanged() {
            mSource.reload();
        }

        @Override
        public void onContentDirty() {
            AppWidgetManager.getInstance(mApp.getAndroidContext())
                    .notifyAppWidgetViewDataChanged(
                    mAppWidgetId, R.id.appwidget_horizontallist_view);
        }
    }
}
