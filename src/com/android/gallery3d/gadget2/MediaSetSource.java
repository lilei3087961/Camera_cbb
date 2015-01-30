/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (c) 2012, The Linux Foundation. All Rights Reserved.
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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;

import java.util.ArrayList;
import java.util.Arrays;

public class MediaSetSource implements WidgetSource, ContentListener {
    private static final int CACHE_SIZE = 32;

    private static final String TAG = "MediaSetSource";

    private MediaSet mSource;
    private MediaItem mCache[] = new MediaItem[CACHE_SIZE];
    private int mCacheStart;
    private int mCacheEnd;
    private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;

    private ContentListener mContentListener;

    public MediaSetSource(MediaSet source) {
        mSource = Utils.checkNotNull(source);
        mSource.addContentListener(this);
    }

    @Override
    public void close() {
        mSource.removeContentListener(this);
    }

    private void ensureCacheRange(int index) {
        if (index >= mCacheStart && index < mCacheEnd) return;

        long token = Binder.clearCallingIdentity();
        try {
            // Any one who would like to access data should require the
            // lock in DataManager to prevent concurrency issue.
            synchronized (DataManager.LOCK) {
                mCacheStart = index;
                ArrayList<MediaItem> items = mSource.getMediaItem(mCacheStart, CACHE_SIZE);
                mCacheEnd = mCacheStart + items.size();
                items.toArray(mCache);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public synchronized Uri getContentUri(int index) {
        ensureCacheRange(index);
        if (index < mCacheStart || index >= mCacheEnd) return null;
        return mCache[index - mCacheStart].getContentUri();
    }

    @Override
    public synchronized Bitmap getImage(int index) {
        ensureCacheRange(index);
        if (index < mCacheStart || index >= mCacheEnd) return null;
        return WidgetUtils.createWidgetBitmap(mCache[index - mCacheStart]);
    }

    @Override
    public synchronized Bitmap getPreviewImage(int index) {
        ensureCacheRange(index);
        if (index < mCacheStart || index >= mCacheEnd) return null;
        return WidgetUtils.createPreviewWidgetBitmap(mCache[index - mCacheStart]);
    }

    @Override
    public synchronized Bitmap getHlistImage(int index) {
        ensureCacheRange(index);
        if (index < mCacheStart || index >= mCacheEnd) return null;
        return WidgetUtils.createHlistWidgetBitmap(mCache[index - mCacheStart]);
    }

    @Override
    public void reload() {
        // Any one who would like to access data should require the
        // lock in DataManager to prevent concurrency issue.
        synchronized (DataManager.LOCK) {
            long version = mSource.reload();
            if (mSourceVersion != version) {
                mSourceVersion = version;
                mCacheStart = 0;
                mCacheEnd = 0;
                Arrays.fill(mCache, null);
            }
        }
    }

    @Override
    public void setContentListener(ContentListener listener) {
        mContentListener = listener;
    }

    @Override
    public int size() {
        long token = Binder.clearCallingIdentity();
        try {
            // Any one who would like to access data should require the
            // lock in DataManager to prevent concurrency issue.
            synchronized (DataManager.LOCK) {
                return mSource.getMediaItemCount();
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onContentDirty() {
        if (mContentListener != null) mContentListener.onContentDirty();
    }
}
