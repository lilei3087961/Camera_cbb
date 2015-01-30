/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
 *
 * Not a Contribution. Apache license notifications and license are retained
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

package com.android.gallery3d.app;

import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.android.camera.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.FilterDeleteSet;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MtpSource;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.SnailAlbum;
import com.android.gallery3d.data.SnailItem;
import com.android.gallery3d.data.SnailSource;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.DetailsHelper.DetailsSource;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRoot.OnGLIdleListener;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.ImportCompleteListener;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;

public class PhotoPage extends ActivityState implements
        PhotoView.Listener, OrientationManager.Listener, AppBridge.Server {
    private static final String TAG = "PhotoPage";

    private static final int MSG_HIDE_BARS = 1;
    private static final int MSG_LOCK_ORIENTATION = 2;
    private static final int MSG_UNLOCK_ORIENTATION = 3;
    private static final int MSG_ON_FULL_SCREEN_CHANGED = 4;
    private static final int MSG_UPDATE_ACTION_BAR = 5;
    private static final int MSG_UNFREEZE_GLROOT = 6;
    private static final int MSG_WANT_BARS = 7;

    private static final int HIDE_BARS_TIMEOUT = 3500;
    private static final int UNFREEZE_GLROOT_TIMEOUT = 250;

    private static final int REQUEST_SLIDESHOW = 1;
    private static final int REQUEST_CROP = 2;
    private static final int REQUEST_CROP_PICASA = 3;
    private static final int REQUEST_EDIT = 4;
    private static final int REQUEST_PLAY_VIDEO = 5;

    public static final String KEY_MEDIA_SET_PATH = "media-set-path";
    public static final String KEY_MEDIA_ITEM_PATH = "media-item-path";
    public static final String KEY_INDEX_HINT = "index-hint";
    public static final String KEY_OPEN_ANIMATION_RECT = "open-animation-rect";
    public static final String KEY_APP_BRIDGE = "app-bridge";
    public static final String KEY_TREAT_BACK_AS_UP = "treat-back-as-up";

    public static final String KEY_RETURN_INDEX_HINT = "return-index-hint";

    private GalleryApp mApplication;
    private SelectionManager mSelectionManager;

    private PhotoView mPhotoView;
    private PhotoPage.Model mModel;
    private DetailsHelper mDetailsHelper;
    private boolean mShowDetails;
    private Path mPendingSharePath;

    // mMediaSet could be null if there is no KEY_MEDIA_SET_PATH supplied.
    // E.g., viewing a photo in gmail attachment
    private FilterDeleteSet mMediaSet;
    private Menu mMenu;

    private int mCurrentIndex = 0;
    private Handler mHandler;
    private boolean mShowBars = true;
    private volatile boolean mActionBarAllowed = true;
    private GalleryActionBar mActionBar;
    private MyMenuVisibilityListener mMenuVisibilityListener;
    private boolean mIsMenuVisible;
    private MediaItem mCurrentPhoto = null;
    private MenuExecutor mMenuExecutor;
    private boolean mIsActive;
    private ShareActionProvider mShareActionProvider;
    private String mSetPathString;
    // This is the original mSetPathString before adding the camera preview item.
    private String mOriginalSetPathString;
    private AppBridge mAppBridge;
    private SnailItem mScreenNailItem;
    private SnailAlbum mScreenNailSet;
    private OrientationManager mOrientationManager;
    private boolean mHasActivityResult;
    private boolean mTreatBackAsUp;

    // The item that is deleted (but it can still be undeleted before commiting)
    private Path mDeletePath;
    private boolean mDeleteIsFocus;  // whether the deleted item was in focus

    private NfcAdapter mNfcAdapter;

    public static interface Model extends PhotoView.Model {
        public void resume();
        public void pause();
        public boolean isEmpty();
        public void setCurrentPhoto(Path path, int indexHint);
        public int getIndexbyPath(Path path);
    }

    private class MyMenuVisibilityListener implements OnMenuVisibilityListener {
        @Override
        public void onMenuVisibilityChanged(boolean isVisible) {
            mIsMenuVisible = isVisible;
            refreshHidingMessage();
        }
    }

    private final GLView mRootPane = new GLView() {

        @Override
        protected void renderBackground(GLCanvas view) {
			view.clearBuffer();
        	if(isCurrentCameraPreview()) return;
            if(mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                view.fillRect(0, 0, 1900, 1080, Color.WHITE);
            }else  if (mActivity.getResources().getConfiguration().orientation ==Configuration.ORIENTATION_PORTRAIT){
                view.fillRect(0, 0, 1080, 1900, Color.WHITE);
            }
        }

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mPhotoView.layout(0, 0, right - left, bottom - top);
            if (mShowDetails) {
                mDetailsHelper.layout(left, mActionBar.getHeight(), right, bottom);
            }
        }
    };

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        mActionBar = mActivity.getGalleryActionBar();
        mSelectionManager = new SelectionManager(mActivity, false);
        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);

        mPhotoView = new PhotoView(mActivity);
        mPhotoView.setListener(this);
        mRootPane.addComponent(mPhotoView);
        mApplication = (GalleryApp)((Activity) mActivity).getApplication();
        mOrientationManager = mActivity.getOrientationManager();
        mOrientationManager.addListener(this);
        mActivity.getGLRoot().setOrientationSource(mOrientationManager);

        mSetPathString = data.getString(KEY_MEDIA_SET_PATH);
        mOriginalSetPathString = mSetPathString;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity.getAndroidContext());
        Path itemPath = Path.fromString(data.getString(KEY_MEDIA_ITEM_PATH));
        mTreatBackAsUp = data.getBoolean(KEY_TREAT_BACK_AS_UP, false);
        boolean needFinishState = false;

        if (mSetPathString != null) {
            mAppBridge = (AppBridge) data.getParcelable(KEY_APP_BRIDGE);
            if (mAppBridge != null) {
                mAppBridge.setServer(this);
                mOrientationManager.lockOrientation();

                // Get the ScreenNail from AppBridge and register it.
                int id = SnailSource.newId();
                Path screenNailSetPath = SnailSource.getSetPath(id);
                Path screenNailItemPath = SnailSource.getItemPath(id);
                mScreenNailSet = (SnailAlbum) mActivity.getDataManager()
                        .getMediaObject(screenNailSetPath);
                mScreenNailItem = (SnailItem) mActivity.getDataManager()
                        .getMediaObject(screenNailItemPath);
                mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());

                // Combine the original MediaSet with the one for ScreenNail
                // from AppBridge.
                mSetPathString = "/combo/item/{" + screenNailSetPath +
                        "," + mSetPathString + "}";

                // Start from the screen nail.
                itemPath = screenNailItemPath;

                // Action bar should not be displayed when camera starts.
                mFlags |= FLAG_HIDE_ACTION_BAR | FLAG_HIDE_STATUS_BAR;
                mShowBars = false;
            }

            MediaSet originalSet = mActivity.getDataManager()
                    .getMediaSet(mSetPathString);
            mSelectionManager.setSourceMediaSet(originalSet);
            mSetPathString = "/filter/delete/{" + mSetPathString + "}";
            mMediaSet = (FilterDeleteSet) mActivity.getDataManager()
                    .getMediaSet(mSetPathString);
            mCurrentIndex = data.getInt(KEY_INDEX_HINT, 0);
            if (mMediaSet == null) {
                Log.w(TAG, "failed to restore " + mSetPathString);
            }
            PhotoDataAdapter pda = new PhotoDataAdapter(
                    mActivity, mPhotoView, mMediaSet, itemPath, mCurrentIndex,
                    mAppBridge == null ? -1 : 0,
                    mAppBridge == null ? false : mAppBridge.isPanorama());
            mModel = pda;
            mPhotoView.setModel(mModel);

            pda.setDataListener(new PhotoDataAdapter.DataListener() {

                @Override
                public void onPhotoChanged(int index, Path item) {
                    mCurrentIndex = index;
                    if (item != null) {
                        MediaItem photo = mModel.getMediaItem(0);
                        if (photo != null) updateCurrentPhoto(photo);
                    }
                    updateBars();
                }

                @Override
                public void onLoadingFinished() {
                    if (!mModel.isEmpty()) {
                        MediaItem photo = mModel.getMediaItem(0);
                        if (photo != null) updateCurrentPhoto(photo);
                    } else if (mIsActive) {
                        // We only want to finish the PhotoPage if there is no
                        // deletion that the user can undo.
                        if (mMediaSet.getNumberOfDeletions() == 0) {
                            mActivity.getStateManager().finishState(
                                    PhotoPage.this);
                        }
                    }
                }

                @Override
                public void onLoadingStarted() {
                }
            });
        } else {
            // Get default media set by the URI
            try {
                MediaItem mediaItem = (MediaItem)
                        mActivity.getDataManager().getMediaObject(itemPath);
                mModel = new SinglePhotoDataAdapter(mActivity, mPhotoView, mediaItem);
                mPhotoView.setModel(mModel);
                updateCurrentPhoto(mediaItem);
            } catch (RuntimeException e) {
                needFinishState = true;
            }
        }

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_HIDE_BARS: {
                        hideBars();
                        break;
                    }
                    case MSG_LOCK_ORIENTATION: {
                        mOrientationManager.lockOrientation();
                        break;
                    }
                    case MSG_UNLOCK_ORIENTATION: {
                        mOrientationManager.unlockOrientation();
                        break;
                    }
                    case MSG_ON_FULL_SCREEN_CHANGED: {
                        mAppBridge.onFullScreenChanged(message.arg1 == 1);
                        break;
                    }
                    case MSG_UPDATE_ACTION_BAR: {
                        updateBars();
                        break;
                    }
                    case MSG_WANT_BARS: {
                        wantBars();
                        break;
                    }
                    case MSG_UNFREEZE_GLROOT: {
                        mActivity.getGLRoot().unfreeze();
                        break;
                    }
                    default: throw new AssertionError(message.what);
                }
            }
        };

        if (needFinishState) {
            mActivity.getStateManager().finishState(PhotoPage.this);
            return;
        }
        // start the opening animation only if it's not restored.
        if (restoreState == null) {
            mPhotoView.setOpenAnimationRect((Rect) data.getParcelable(KEY_OPEN_ANIMATION_RECT));
        }
    }

    private void updateShareURI(Path path) {
        if (mShareActionProvider != null) {
            DataManager manager = mActivity.getDataManager();
            int type = manager.getMediaType(path);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(MenuExecutor.getMimeType(type));
            intent.putExtra(Intent.EXTRA_STREAM, manager.getContentUri(path));
            mShareActionProvider.setShareIntent(intent);
            if (mNfcAdapter != null) {
                mNfcAdapter.setBeamPushUris(new Uri[]{manager.getContentUri(path)},
                        (Activity)mActivity);
            }
            mPendingSharePath = null;
        } else {
            // This happens when ActionBar is not created yet.
            mPendingSharePath = path;
        }
    }

    private void updateCurrentPhoto(MediaItem photo) {
        if (mCurrentPhoto == photo) return;
        mCurrentPhoto = photo;
        if (mCurrentPhoto == null) return;
        updateMenuOperations();
        updateTitle();
        if (mShowDetails) {
            mDetailsHelper.reloadDetails(mModel.getCurrentIndex());
        }
        if ((photo.getSupportedOperations() & MediaItem.SUPPORT_SHARE) != 0) {
            updateShareURI(photo.getPath());
        }
    }

    private void updateTitle() {
        if (mCurrentPhoto == null) return;
        boolean showTitle = mActivity.getAndroidContext().getResources().getBoolean(
                R.bool.show_action_bar_title);
        if (showTitle && mCurrentPhoto.getName() != null){
            mPhotoView.setTitleName(mCurrentPhoto.getName(), true);
            //mActionBar.setTitle(mCurrentPhoto.getName());
            mActionBar.setTitle("");
        } else {
            mActionBar.setTitle("");
        }
    }

    private void updateMenuOperations() {
        if (mMenu == null) return;
        MenuItem item = mMenu.findItem(R.id.action_slideshow);
        if (item != null) {
            item.setVisible(canDoSlideShow());
        }
        // when starting camera,hide menu.
        if (mCurrentPhoto == null) {
            MenuExecutor.updateMenuOperation(mMenu, 0);
            return;
        }

        int supportedOperations = mCurrentPhoto.getSupportedOperations();
        if (!GalleryUtils.isEditorAvailable((Context) mActivity, "image/*")) {
            supportedOperations &= ~MediaObject.SUPPORT_EDIT;
        }
        // If current photo page is single item only, to cut some menu items
        boolean singleItemOnly = mData.getBoolean("SingleItemOnly", false);
        if (singleItemOnly) {
            supportedOperations &= ~MediaObject.SUPPORT_DELETE;
            supportedOperations &= ~MediaObject.SUPPORT_ROTATE;
            supportedOperations &= ~MediaObject.SUPPORT_SHARE;
            supportedOperations &= ~MediaObject.SUPPORT_CROP;
            supportedOperations &= ~MediaObject.SUPPORT_INFO;
            
            
            /* enter Gallery from email/contract, with singleItemOnly mode,
             * setas and edit has some bug, we cut setas and edit menu items.
             * */
            supportedOperations &= ~MediaObject.SUPPORT_SETAS;
            supportedOperations &= ~MediaObject.SUPPORT_EDIT;
        }

        MenuExecutor.updateMenuOperation(mMenu, supportedOperations);
    }

    private boolean canDoSlideShow() {
        if (mMediaSet == null || mCurrentPhoto == null) {
            return false;
        }
        if (mCurrentPhoto.getMediaType() != MediaObject.MEDIA_TYPE_IMAGE) {
            return false;
        }
        if (MtpSource.isMtpPath(mOriginalSetPathString)) {
            return false;
        }
        return true;
    }

    //////////////////////////////////////////////////////////////////////////
    //  Action Bar show/hide management
    //////////////////////////////////////////////////////////////////////////

    private void showBars() {
        mPhotoView.setTitleName(mCurrentPhoto.getName(), true);
        if (mShowBars)
            return;
        mShowBars = true;
        mOrientationManager.unlockOrientation();
        mActionBar.show();
        mActivity.getGLRoot().setLightsOutMode(false);
        refreshHidingMessage();
    }

    private void hideBars() {
        mPhotoView.setTitleName(mCurrentPhoto.getName(), false);
        if (!mShowBars)
            return;
        mShowBars = false;
        mActionBar.hide();
        mActivity.getGLRoot().setLightsOutMode(true);
        mHandler.removeMessages(MSG_HIDE_BARS);
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        if (!mIsMenuVisible) {
            mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);
        }
    }

    private boolean canShowBars() {
        // No bars if we are showing camera preview.
        if (mAppBridge != null && mCurrentIndex == 0) return false;
        // No bars if it's not allowed.
        if (!mActionBarAllowed) return false;

        return true;
    }

    private void wantBars() {
        if (canShowBars()) showBars();
    }

    private void toggleBars() {
        if (mShowBars) {
            hideBars();
        } else {
            if (canShowBars()) showBars();
        }
    }

    private void updateBars() {
        if (!canShowBars()) {
            hideBars();
        }
    }

    @Override
    public void onOrientationCompensationChanged() {
        mActivity.getGLRoot().requestLayoutContentPane();
    }

    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mAppBridge == null || !switchWithCaptureAnimation(-1)) {
            // We are leaving this page. Set the result now.
            setResult();
            if (mTreatBackAsUp) {
                onUpPressed();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void onUpPressed() {
        if (mActivity.getStateManager().getStateCount() > 1) {
            super.onBackPressed();
            return;
        }

        if (mOriginalSetPathString == null) return;

        if (mAppBridge == null) {
            // We're in view mode so set up the stacks on our own.
            Bundle data = new Bundle(getData());
            data.putString(AlbumPage.KEY_MEDIA_PATH, mOriginalSetPathString);
            data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                    mActivity.getDataManager().getTopSetPath(
                            DataManager.INCLUDE_ALL));
            mActivity.getStateManager().switchState(this, AlbumPage.class, data);
        } else {
            // Start the real gallery activity to view the camera roll.
            Uri uri = Uri.parse("content://media/external/file?bucketId="
                    + MediaSetUtils.CAMERA_INTERNAL_BUCKET_ID);
            Bundle data = new Bundle(getData());
            
            String[] arrSplit = mOriginalSetPathString.split("/");
            String bucketId = null;
            if(arrSplit!=null){            	
            	
            	bucketId = arrSplit[arrSplit.length - 1];
            }
            if(bucketId != null){
            	uri = Uri.parse("content://media/external/file?bucketId=" + bucketId);
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(uri, ContentResolver.CURSOR_DIR_BASE_TYPE + "/image");
            try {
            	((Activity) mActivity).startActivity(intent);
            } catch(Exception e) {
            	e.printStackTrace();
            }
        }
    }

    private void setResult() {
        Intent result = null;
        if (!mPhotoView.getFilmMode()) {
            result = new Intent();
            result.putExtra(KEY_RETURN_INDEX_HINT, mCurrentIndex);
        }
        setStateResult(Activity.RESULT_OK, result);
    }

    //////////////////////////////////////////////////////////////////////////
    //  AppBridge.Server interface
    //////////////////////////////////////////////////////////////////////////

    @Override
    public void setCameraRelativeFrame(Rect frame) {
        mPhotoView.setCameraRelativeFrame(frame);
    }

    @Override
    public boolean isCurrentCameraPreview() {
        return ((mCurrentIndex == 0) && (mAppBridge != null));
    }

    @Override
    public boolean switchWithCaptureAnimation(int offset) {
        return mPhotoView.switchWithCaptureAnimation(offset);
    }

    @Override
    public void setSwipingEnabled(boolean enabled) {
        mPhotoView.setSwipingEnabled(enabled);
    }

    @Override
    public void notifyScreenNailChanged() {
        mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());
        mScreenNailSet.notifyChange();
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        MenuInflater inflater = ((Activity) mActivity).getMenuInflater();
        inflater.inflate(R.menu.photo, menu);
        mShareActionProvider = GalleryActionBar.initializeShareActionProvider(menu);
        if (mPendingSharePath != null) updateShareURI(mPendingSharePath);
        mMenu = menu;
        updateMenuOperations();
        updateTitle();
        return true;
    }

    private MenuExecutor.ProgressListener mConfirmDialogListener =
            new MenuExecutor.ProgressListener() {
        @Override
        public void onProgressUpdate(int index) {}

        @Override
        public void onProgressComplete(int result) {}

        @Override
        public void onConfirmDialogShown() {
            mHandler.removeMessages(MSG_HIDE_BARS);
        }

        @Override
        public void onConfirmDialogDismissed(boolean confirmed) {
            refreshHidingMessage();
        }
    };

    @Override
    protected boolean onItemSelected(MenuItem item) {
        refreshHidingMessage();
        MediaItem current = mModel.getMediaItem(0);

        if (current == null) {
            // item is not ready, ignore
            return true;
        }

        int currentIndex = mModel.getCurrentIndex();
        if(mAppBridge != null)
            currentIndex = currentIndex-1;
        Path path = current.getPath();

        DataManager manager = mActivity.getDataManager();
        int action = item.getItemId();
        String confirmMsg = null;
        switch (action) {
            case android.R.id.home: {
                onUpPressed();
                return true;
            }
            case R.id.action_slideshow: {
                Bundle data = new Bundle();
                data.putString(SlideshowPage.KEY_SET_PATH, mMediaSet.getPath().toString());
                data.putString(SlideshowPage.KEY_ITEM_PATH, path.toString());
                data.putInt(SlideshowPage.KEY_PHOTO_INDEX, currentIndex);
                data.putBoolean(SlideshowPage.KEY_REPEAT, true);
                mActivity.getStateManager().startStateForResult(
                        SlideshowPage.class, REQUEST_SLIDESHOW, data);
                return true;
            }
            case R.id.action_crop: {
                Activity activity = (Activity) mActivity;
                Intent intent = new Intent(CropImage.CROP_ACTION);
                intent.setClass(activity, CropImage.class);
                intent.setData(manager.getContentUri(path));
                activity.startActivity(intent);
                return true;
            }
            //case R.id.action_edit_bottom:
            //case R.id.action_edit: {
                // Get the mimeType and set it by setDataAndType().
            //    Intent intent = new Intent(Intent.ACTION_EDIT)
            //            .setDataAndType(manager.getContentUri(path),
            //                    MenuExecutor.getMimeType(manager.getMediaType(path)))
            //            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // When back from PhotoEditor, directly show the previous page rather than show edited picture.
            //    ((Activity) mActivity).startActivity(Intent.createChooser(intent, null));
            //    return true;
            //}
            case R.id.action_details: {
                if (mShowDetails) {
                    hideDetails();
                } else {
                    showDetails(currentIndex);
                }
                return true;
            }
            case R.id.action_delete:
                confirmMsg = mActivity.getResources().getQuantityString(
                        R.plurals.delete_selection, 1);
            case R.id.action_setas:
            case R.id.action_rotate_ccw:
            case R.id.action_rotate_cw:
            case R.id.action_show_on_map:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(item, confirmMsg, mConfirmDialogListener);
                return true;
            case R.id.action_import:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(item, confirmMsg,
                        new ImportCompleteListener(mActivity));
                return true;
            default :
                return false;
        }
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
    }

    private void showDetails(int index) {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, new MyDetailsSource());
            mDetailsHelper.setCloseListener(new CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.reloadDetails(index);
        mDetailsHelper.show();
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Callbacks from PhotoView
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSingleTapUp(int x, int y) {
        if (mAppBridge != null) {
            if (mAppBridge.onSingleTapUp(x, y)) return;
        }

        MediaItem item = mModel.getMediaItem(0);
        if (item == null || item == mScreenNailItem) {
            // item is not ready or it is camera preview, ignore
            return;
        }

        boolean playVideo =
                (item.getSupportedOperations() & MediaItem.SUPPORT_PLAY) != 0;

        if (playVideo) {
            // determine if the point is at center (1/6) of the photo view.
            // (The position of the "play" icon is at center (1/6) of the photo)
            int w = mPhotoView.getWidth();
            int h = mPhotoView.getHeight();
            playVideo = (Math.abs(x - w / 2) * 12 <= w)
                && (Math.abs(y - h / 2) * 12 <= h);
        }

        if (playVideo) {
            playVideo((Activity) mActivity, item.getPlayUri(), item.getName());
        } else {
            toggleBars();
        }
    }

    @Override
    public void lockOrientation() {
        mHandler.sendEmptyMessage(MSG_LOCK_ORIENTATION);
    }

    @Override
    public void unlockOrientation() {
        mHandler.sendEmptyMessage(MSG_UNLOCK_ORIENTATION);
    }

    @Override
    public void onActionBarAllowed(boolean allowed) {
        mActionBarAllowed = allowed;
        mHandler.sendEmptyMessage(MSG_UPDATE_ACTION_BAR);
    }

    @Override
    public void onActionBarWanted() {
        mHandler.sendEmptyMessage(MSG_WANT_BARS);
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        Message m = mHandler.obtainMessage(
                MSG_ON_FULL_SCREEN_CHANGED, full ? 1 : 0, 0);
        m.sendToTarget();
    }

    // How we do delete/undo:
    //
    // When the user choose to delete a media item, we just tell the
    // FilterDeleteSet to hide that item. If the user choose to undo it, we
    // again tell FilterDeleteSet not to hide it. If the user choose to commit
    // the deletion, we then actually delete the media item.
    @Override
    public void onDeleteImage(Path path, int offset) {
        onCommitDeleteImage();  // commit the previous deletion
        mDeletePath = path;
        mDeleteIsFocus = (offset == 0);
        // Use mModel to get correct current index.
        try {
            mMediaSet.addDeletion(path, mModel.getCurrentIndex() + offset);
        } catch (Exception e) {}
    }

    @Override
    public void onUndoDeleteImage() {
        if (mDeletePath == null) return;
        // If the deletion was done on the focused item, we want the model to
        // focus on it when it is undeleted.
        if (mDeleteIsFocus) mModel.setFocusHintPath(mDeletePath);
        try {
            mMediaSet.removeDeletion(mDeletePath);
        } catch (Exception e) {}
        mDeletePath = null;
    }

    @Override
    public void onCommitDeleteImage() {
        if (mDeletePath == null) return;
        mSelectionManager.deSelectAll();
        mSelectionManager.toggle(mDeletePath);
        mMenuExecutor.onMenuClicked(R.id.action_delete, null, true, false);
        mDeletePath = null;
    }

    public static void playVideo(Activity activity, Uri uri, String title) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/*")
                    .putExtra(Intent.EXTRA_TITLE, title)
                    .putExtra(MovieActivity.KEY_TREAT_UP_AS_BACK, true);
            activity.startActivityForResult(intent, REQUEST_PLAY_VIDEO);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setCurrentPhotoByIntent(Intent intent) {
        //Do not setCurrentPhoto when camera preview is showing.
        if (mAppBridge != null && mCurrentIndex == 0) {
            return;
        }
        if (intent == null) return;
        Path path = mApplication.getDataManager()
                .findPathByUri(intent.getData(), intent.getType());
        if (path != null) {
            mModel.setCurrentPhoto(path, mCurrentIndex);
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        mHasActivityResult = true;
        switch (requestCode) {
            case REQUEST_EDIT:
                setCurrentPhotoByIntent(data);
                break;
            case REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    setCurrentPhotoByIntent(data);
                }
                break;
            case REQUEST_CROP_PICASA: {
                if (resultCode == Activity.RESULT_OK) {
                    Context context = mActivity.getAndroidContext();
                    String message = context.getString(R.string.crop_saved,
                            context.getString(R.string.folder_download));
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_SLIDESHOW: {
                if (data == null) break;
                String path = data.getStringExtra(SlideshowPage.KEY_ITEM_PATH);
                int index = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                //If videos exist or start from camera, when return from slideshow,
                //index will not match, so update index by path.
                
                if (path != null) {
                	
                    int indexbyPath = mModel.getIndexbyPath(Path.fromString(path));
                    if (indexbyPath != -1) {
                        index = indexbyPath;
                    }
                    mModel.setCurrentPhoto(Path.fromString(path), index);
                }
            }
        }
    }

    private class PreparePhotoFallback implements OnGLIdleListener {
        private PhotoFallbackEffect mPhotoFallback = new PhotoFallbackEffect();
        private boolean mResultReady = false;

        public synchronized PhotoFallbackEffect get() {
            while (!mResultReady) {
                Utils.waitWithoutInterrupt(this);
            }
            return mPhotoFallback;
        }

        @Override
        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
            mPhotoFallback = mPhotoView.buildFallbackEffect(mRootPane, canvas);
            synchronized (this) {
                mResultReady = true;
                notifyAll();
            }
            return false;
        }
    }

    private void preparePhotoFallbackView() {
        GLRoot root = mActivity.getGLRoot();
        PreparePhotoFallback task = new PreparePhotoFallback();
        root.unlockRenderThread();
        PhotoFallbackEffect anim;
        try {
            root.addOnGLIdleListener(task);
            anim = task.get();
        } finally {
            root.lockRenderThread();
        }
        mActivity.getTransitionStore().put(
                AlbumPage.KEY_RESUME_ANIMATION, anim);
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;

        mActivity.getGLRoot().unfreeze();
        mHandler.removeMessages(MSG_UNFREEZE_GLROOT);
        if (isFinishing()) preparePhotoFallbackView();

        DetailsHelper.pause();
        //if (mShowDetails) hideDetails();
        mPhotoView.pause();
        mModel.pause();
        mHandler.removeMessages(MSG_HIDE_BARS);
        mActionBar.removeOnMenuVisibilityListener(mMenuVisibilityListener);

        onCommitDeleteImage();
        mMenuExecutor.pause();
        if (mMediaSet != null) mMediaSet.clearDeletion();
    }

    @Override
    public void onCurrentImageUpdated() {
        mActivity.getGLRoot().unfreeze();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivity.getGLRoot().freeze();
        mIsActive = true;
        setContentPane(mRootPane);

        mModel.resume();
        mPhotoView.resume();
        if (mMenuVisibilityListener == null) {
            mMenuVisibilityListener = new MyMenuVisibilityListener();
        }
        mActionBar.setDisplayOptions(false, true);
        mActionBar.setHomeButton(true);
        mActionBar.addOnMenuVisibilityListener(mMenuVisibilityListener);

        //BEGIN: Deleted by wangbin at 2013-04-24
        /*
        if (mAppBridge != null && !mHasActivityResult) {
            mPhotoView.resetToFirstPicture();
        }
        */
        //END: Deleted by wangbin at 2013-04-24
        mHasActivityResult = false;
        mHandler.sendEmptyMessageDelayed(MSG_UNFREEZE_GLROOT, UNFREEZE_GLROOT_TIMEOUT);
        // In some times, current photo is not the same as the sharing photo.
        // For example: View a picture(pic1) in Gallery and share via
        // message. Click attachment icon to replace the sharing picture by
        // another picture(pic2), view pic2 and discard the message, the UI will
        // return to Gallery. In this case, the sharing URI is the uri is still
        // the path of pic2 because when the user view pic2 the path of pic2 had
        // been set to shareActionProvider as share uri, so need to refresh the
        // sharing URI when the current photo support share.
        if (mCurrentPhoto != null
                && (mCurrentPhoto.getSupportedOperations() & MediaItem.SUPPORT_SHARE) != 0) {
            updateShareURI(mCurrentPhoto.getPath());
        }
    }

    @Override
    protected void onDestroy() {
        if (mAppBridge != null) {
            mAppBridge.setServer(null);
            mScreenNailItem.setScreenNail(null);
            mAppBridge.detachScreenNail();
            mAppBridge = null;
            mScreenNailSet = null;
            mScreenNailItem = null;
        }
        mPhotoView.destroy();
        mOrientationManager.removeListener(this);
        mActivity.getGLRoot().setOrientationSource(null);

        // Remove all pending messages.
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
        System.gc();
    }

    public void notifyFilmModeStateToScreenNail(boolean enabled) {
        if(mAppBridge != null) {
            mAppBridge.notifyFilmModeStateToScreenNail(enabled);
        }
    }
    private class MyDetailsSource implements DetailsSource {
        private int mIndex;

        @Override
        public MediaDetails getDetails() {
            return mModel.getMediaItem(0).getDetails();
        }

        @Override
        public int size() {
            int count = mMediaSet.getMediaItemCount();
            if(mAppBridge != null)
                count = count-1;
            return mMediaSet != null ? count : 1;
        }

        @Override
        public int findIndex(int indexHint) {
            mIndex = indexHint;
            return indexHint;
        }

        @Override
        public int getIndex() {
            return mIndex;
        }
    }

}
