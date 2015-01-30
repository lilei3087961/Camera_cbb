/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (c) 2011-2013, The Linux Foundation. All Rights Reserved.
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

package com.android.gallery3d.app;

import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.Window;
import android.widget.Toast;
import android.widget.TextView;

import com.android.camera.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.GalleryUtils;

public final class Gallery extends AbstractGalleryActivity implements OnCancelListener {
    public static final String EXTRA_SLIDESHOW = "slideshow";
    public static final String EXTRA_DREAM = "dream";
    public static final String EXTRA_CROP = "crop";

    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    public static final String KEY_GET_CONTENT = "get-content";
    public static final String KEY_GET_ALBUM = "get-album";
    public static final String KEY_GET_VIDEO = "get-video";
    public static final String KEY_TYPE_BITS = "type-bits";
    public static final String KEY_MEDIA_TYPES = "mediaTypes";

    private static final String TAG = "Gallery";
    private Dialog mVersionCheckDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setTitle("");
        setContentView(R.layout.main);

        if (savedInstanceState != null) {
            getStateManager().restoreFromState(savedInstanceState);
        } else {
            initializeByIntent();
        }
    }

    private void initializeByIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)) {
            startGetContent(intent);
        } else if (Intent.ACTION_PICK.equalsIgnoreCase(action)) {
            // We do NOT really support the PICK intent. Handle it as
            // the GET_CONTENT. However, we need to translate the type
            // in the intent here.
            Log.w(TAG, "action PICK is not supported");
            String type = Utils.ensureNotNull(intent.getType());
            if (type.startsWith("vnd.android.cursor.dir/")) {
                if (type.endsWith("/image")) intent.setType("image/*");
                if (type.endsWith("/video")) intent.setType("video/*");
            }
            startGetContent(intent);
        } else if (Intent.ACTION_VIEW.equalsIgnoreCase(action)
                || ACTION_REVIEW.equalsIgnoreCase(action)){
            startViewAction(intent);
        } else {
            startDefaultPage();
        }
    }

    public void startDefaultPage() {
        PicasaSource.showSignInReminder(this);
        Bundle data = new Bundle();
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
        getStateManager().startState(AlbumSetPage.class, data);
        mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.setOnCancelListener(this);
        }
    }

    private void startGetContent(Intent intent) {
        Bundle data = intent.getExtras() != null
                ? new Bundle(intent.getExtras())
                : new Bundle();
        data.putBoolean(KEY_GET_CONTENT, true);
        int typeBits = GalleryUtils.determineTypeBits(this, intent);
        data.putInt(KEY_TYPE_BITS, typeBits);
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(typeBits));
        getStateManager().startState(AlbumSetPage.class, data);
    }

    private String getContentType(Intent intent) {
        String type = intent.getType();
        if (type != null) return type;

        Uri uri = intent.getData();
        try {
            return getContentResolver().getType(uri);
        } catch (Throwable t) {
            Log.w(TAG, "get type fail", t);
            return null;
        }
    }

    // Used to check whether the dataUri is valid.
    private boolean isValidDataUri(Uri dataUri) {
        if (dataUri == null) return false;
        try {
            AssetFileDescriptor f = getContentResolver()
                    .openAssetFileDescriptor(dataUri, "r");
            f.close();
            return true;
        } catch (Throwable e) {
            Log.w(TAG, "cannot open uri: " + dataUri, e);
            return false;
        }
    }

    private void startViewAction(Intent intent) {
        Boolean slideshow = intent.getBooleanExtra(EXTRA_SLIDESHOW, false);
        if (slideshow) {
            getActionBar().hide();
            DataManager manager = getDataManager();
            Path path = manager.findPathByUri(intent.getData(), intent.getType());
            if (path == null || manager.getMediaObject(path)
                    instanceof MediaItem) {
                path = Path.fromString(
                        manager.getTopSetPath(DataManager.INCLUDE_IMAGE));
            }
            Bundle data = new Bundle();
            data.putString(SlideshowPage.KEY_SET_PATH, path.toString());
            data.putBoolean(SlideshowPage.KEY_RANDOM_ORDER, true);
            data.putBoolean(SlideshowPage.KEY_REPEAT, true);
            if (intent.getBooleanExtra(EXTRA_DREAM, false)) {
                data.putBoolean(SlideshowPage.KEY_DREAM, true);
            }
            getStateManager().startState(SlideshowPage.class, data);
        } else {
            Bundle data = new Bundle();
            DataManager dm = getDataManager();
            Uri uri = intent.getData();
            String contentType = getContentType(intent);
            if (contentType == null) {
                Toast.makeText(this,
                        R.string.no_such_item, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (uri == null) {
                int typeBits = GalleryUtils.determineTypeBits(this, intent);
                data.putInt(KEY_TYPE_BITS, typeBits);
                data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                        getDataManager().getTopSetPath(typeBits));
                getStateManager().startState(AlbumSetPage.class, data);
            } else if (contentType.startsWith(
                    ContentResolver.CURSOR_DIR_BASE_TYPE)) {
                int mediaType = intent.getIntExtra(KEY_MEDIA_TYPES, 0);
                if (mediaType != 0) {
                    uri = uri.buildUpon().appendQueryParameter(
                            KEY_MEDIA_TYPES, String.valueOf(mediaType))
                            .build();
                }
                Path setPath = dm.findPathByUri(uri, null);
                MediaSet mediaSet = null;
                if (setPath != null) {
                    mediaSet = (MediaSet) dm.getMediaObject(setPath);
                }
                if (mediaSet != null) {
                    if (mediaSet.isLeafAlbum()) {
                        data.putString(AlbumPage.KEY_MEDIA_PATH, setPath.toString());
                        data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                                dm.getTopSetPath(DataManager.INCLUDE_ALL));
                        boolean inAlbum = getStateManager().hasStateClass(AlbumPage.class);
                        // We only show cluster menu in the first AlbumPage in stack
                        data.putBoolean(AlbumPage.KEY_SHOW_CLUSTER_MENU, !inAlbum);
                        getStateManager().startState(AlbumPage.class, data);
                    } else {
                        data.putString(AlbumSetPage.KEY_MEDIA_PATH, setPath.toString());
                        getStateManager().startState(AlbumSetPage.class, data);
                    }
                } else {
                    startDefaultPage();
                }
            } else {
                // Show toast and finish current page when uri is invalid.
                if (!isValidDataUri(uri)) {
                    Toast.makeText(this,
                            R.string.no_such_item, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                // TODO: Make this parameter public so other activities can reference it.
                boolean singleItemOnly = intent.getBooleanExtra("SingleItemOnly", false);
                
                if (!singleItemOnly && uri.getScheme().equals("file")) {
                    Uri newUri = GalleryUtils.getContentUri(this, uri);
                    if (newUri != null) {
                        uri = newUri;
                    }
                }
                
                Path itemPath = dm.findPathByUri(uri, intent.getType());
                Path albumPath = null; 
                try {
                    albumPath = dm.getDefaultSetOf(itemPath);
                } catch (RuntimeException e) {
                    // get object fail from media provider,  clear the item
                    Log.w(TAG, "itemPath: " + itemPath.toString(), e);
                    albumPath = null;
                    itemPath.clear();
                    Log.w(TAG, "get media object fail, finish activity");
                    Toast.makeText(this,
                            R.string.no_such_item, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                if (!singleItemOnly && (albumPath != null)) {
                    data.putString(PhotoPage.KEY_MEDIA_SET_PATH, albumPath.toString());
                }
                data.putBoolean("SingleItemOnly", singleItemOnly);
                data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, itemPath.toString());
                if (intent.getBooleanExtra(PhotoPage.KEY_TREAT_BACK_AS_UP, false)) {
                    data.putBoolean(PhotoPage.KEY_TREAT_BACK_AS_UP, true);
                }

                // Displays the filename as title, reading the filename from the interface:
                // {@link android.provider.OpenableColumns#DISPLAY_NAME}.
                AsyncQueryHandler queryHandler = new AsyncQueryHandler(getContentResolver()) {
                    @Override
                    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                         try {
                             setTitle(getActionBarTitle(cursor));
                         } finally {
                             Utils.closeSilently(cursor);
                         }
                    }

                    private String getActionBarTitle(Cursor cursor) {
                         if ((cursor != null) && cursor.moveToFirst()) {
                             String actionBarTitle = cursor.getString(0);
                             return (Gallery.this.getResources().getBoolean(R.bool.show_action_bar_title) && null != actionBarTitle) ? actionBarTitle : "";
                         }

                         return "";
                    }
                };

                queryHandler.startQuery(0, null, uri, new String[] {OpenableColumns.DISPLAY_NAME},
                        null, null, null);

                getStateManager().startState(PhotoPage.class, data);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        int titleId = getResources().getSystem().getIdentifier(
				"action_bar_title", "id", "android");
        TextView titleView = (TextView) findViewById(titleId);
        if(titleView != null){       	

            titleView.setTextColor(R.color.text_color);
        }
        return getStateManager().createOptionsMenu(menu);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Utils.assertTrue(getStateManager().getStateCount() > 0);
        super.onResume();
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.dismiss();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mVersionCheckDialog) {
            mVersionCheckDialog = null;
        }
    }
}
