/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
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

package com.android.camera;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import com.android.camera.ActivityBase;
import com.android.camera.Util;
import com.android.camera.ui.CameraPicker;
import com.android.camera.ui.PopupManager;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateTextToast;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AppBridge;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.app.StateManager;
import com.android.gallery3d.app.ActivityState;

import java.io.File;
import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.os.Build;

/**
 * Superclass of Camera and VideoCamera activities.
 */
abstract public class ActivityBase extends AbstractGalleryActivity
        implements View.OnLayoutChangeListener {

    private static final String TAG = "ActivityBase";
    private static final boolean LOGV = false;
    private static final int CAMERA_APP_VIEW_TOGGLE_TIME = 100;  // milliseconds
    private static final String ACTION_DELETE_PICTURE =
            "com.android.gallery3d.action.DELETE_PICTURE";
    private static final int DATABASE_CHANGE = 0; // Msg of database change
    private static final int DATABASE_CHANGE_DELAY = 200; // Delay for send msg of database change
    private static final String PATH_OBSEVER = "content://media/external/"; // Path for obesevering
    private int mResultCodeForTesting;
    private Intent mResultDataForTesting;
    private OnScreenHint mStorageHint;
    private HideCameraAppView mHideCameraAppView;
    private View mSingleTapArea;
    private final Handler mDateChangeHandler = new DateChangeHandler(); // Handler for database changing
    private UIHandler mHandler = new UIHandler();

    // The bitmap of the last captured picture thumbnail and the URI of the
    // original picture.
    protected Thumbnail mThumbnail;
    protected int mThumbnailViewWidth; // layout width of the thumbnail
    protected AsyncTask<Void, Void, Thumbnail> mLoadThumbnailTask;
    // An imageview showing the last captured picture thumbnail.
    protected RotateImageView mThumbnailView;
    protected RotateImageView mThumbnailBgView;
    protected CameraPicker mCameraPicker;

    protected boolean mOpenCameraFail;
    protected boolean mCameraDisabled;
    protected CameraManager.CameraProxy mCameraDevice;
    protected Parameters mParameters;
    // The activity is paused. The classes that extend this class should set
    // mPaused the first thing in onResume/onPause.
    // Give mPaused an initial value for Monkey test may use this before onResume was called
    protected boolean mPaused = true;
    protected GalleryActionBar mActionBar;

    // multiple cameras support
    protected int mNumberOfCameras;
    protected int mCameraId;
    // The activity is going to switch to the specified camera id. This is
    // needed because texture copy is done in GL thread. -1 means camera is not
    // switching.
    protected int mPendingSwitchCameraId = -1;
    private int mStreamType = AudioManager.STREAM_RING;

    protected MyAppBridge mAppBridge;
    protected CameraScreenNail mCameraScreenNail; // This shows camera preview.
    // The view containing only camera related widgets like control panel,
    // indicator bar, focus indicator and etc.
    protected View mCameraAppView;
    protected boolean mShowCameraAppView = true;
   
    //Added by xiongzhu at 2013-04-15
    public static final int STORAGE_SDCARD=0;
    public static final int STORAGE_MEMORY=1; 
    protected String mStoragePlace;
    private Thumbnail mThumbnailTemp;
   
    private static final float DEFAULT_CAMERA_BRIGHTNESS = 0.7f;
    
    // Added by zhanghongxing at 2013-04-19
    private BroadcastReceiver mBatteryLevelRcvr;
    private IntentFilter mBatteryLevelFilter;
    protected boolean mIsRecordLocation = false;
    private CameraDefaultValueManager mCameraDefaultValueManager;
    
    private boolean isQueryedToLowPowerMode = false;
    
    protected static final int OPEN_CAMERA_FAIL = 12;
    protected static final int CAMERA_DISABLED = 13;

    protected double mPreviewSizeRadio = (double)5/3;

    //support zoom or not
    protected boolean mIsZoomSupported;
    private boolean mUpdateThumbnailDelayed;
    private IntentFilter mDeletePictureFilter =
            new IntentFilter(ACTION_DELETE_PICTURE);
    private BroadcastReceiver mDeletePictureReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (mShowCameraAppView) {
                        getLastThumbnailUncached();
                    } else {
                        mUpdateThumbnailDelayed = true;
                    }
                }
            };

    protected class CameraOpenThread extends Thread {
        @Override
        public void run() {
            try {
                mCameraDevice = Util.openCamera(ActivityBase.this, mCameraId);
                mParameters = mCameraDevice.getParameters();
                mIsZoomSupported = mParameters.isZoomSupported();
            } catch (CameraHardwareException e) {
            	Log.i(TAG,"==zzw:CameraOpenThread,OPEN_CAMERA_FAIL");
                mOpenCameraFail = true;
                mHandler.sendEmptyMessage(OPEN_CAMERA_FAIL);
            } catch (CameraDisabledException e) {
                mCameraDisabled = true;
                mHandler.sendEmptyMessage(CAMERA_DISABLED);
            } 
        } 
    }

    // ContentObserver for database changing
    ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mDateChangeHandler.removeMessages(DATABASE_CHANGE);
            mDateChangeHandler.sendEmptyMessageDelayed(DATABASE_CHANGE, DATABASE_CHANGE_DELAY);
        }
    };

    // Handler for database changing
    private class DateChangeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                    case DATABASE_CHANGE: {
                        handleDatabaseChange();
                        break;
                    }
                }
        }
    }

    private void handleDatabaseChange() {
        if (null != mThumbnail && null != mThumbnail.getUri()) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().
                        query(mThumbnail.getUri(), null, null, null, null);
                // If thumbnail is invalid, get last thumbnail
                if (!isThumbnailValid(cursor)) {
                      getLastThumbnail();
                }
            } catch (Exception ex) {
                // Ignore exception
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private boolean isThumbnailValid(Cursor c) {
        return ((null != c) && (0 != c.getCount()));
    }

    @Override
    public void onCreate(Bundle icicle) {
    	//Deleted by wangbin for WAX-490.
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.disableToggleStatusBar();
        // Set a theme with action bar. It is not specified in manifest because
        // we want to hide it by default. setTheme must happen before
        // setContentView.
        //
        // This must be set before we call super.onCreate(), where the window's
        // background is removed.
        setTheme(R.style.Theme_Gallery);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        monitorBatteryState(); // Added by zhanghongxing at 2013-04-19
        super.onCreate(icicle);

        // Register mContentObserver for path of content://media/external/
        getContentResolver().
            registerContentObserver(Uri.parse(PATH_OBSEVER), true, mContentObserver);

    }

    public boolean isPanoramaActivity() {
        return false;
    }
    
    // BEGIN: Added by zhanghongxing at 2013-05-02
    public boolean isFrontCamera() {
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            return true;
        } else {
            return false;
        }
    }
    // END:   Added by zhanghongxing at 2013-05-02

    @Override
    protected void onResume() {
        super.onResume();
        
    	mCameraDefaultValueManager = new CameraDefaultValueManager(this);
    	mCameraDefaultValueManager.loadCameraDefaultValueFromFile();
    	mCameraDefaultValueManager.setDefaultScreenBrightness();
    	
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mDeletePictureReceiver, mDeletePictureFilter);

        // if the led flash light is open, turn it off
        Log.d("LED Flashlight", "send the turn off the broadcast");
        Intent intent = new Intent("qualcomm.android.LEDFlashlight.appWidgetUpdate");
        intent.putExtra("camera_led", true);
        sendBroadcast(intent);

        String status = Util.readFileFirstLine("/sys/class/power_supply/battery/status");
        if(!"Charging".equalsIgnoreCase(status)) {
            String powderStr = Util.readFileFirstLine("/sys/class/power_supply/battery/capacity");
            int level = 100;
            try {
                level = Integer.valueOf(powderStr);
            } catch (Exception e) {

            }
            if (level <= 7) {
                Util.showFinishAlert(ActivityBase.this,
                        R.string.camera_alert_battery_title,
                        R.string.camera_alert_battery_low);
            } else if (level <= 15){
                if(!isQueryedToLowPowerMode) {
                    Util.queryToLowPowerMode(ActivityBase.this,
                            R.string.low_power_title, R.string.low_power_tips);
                    isQueryedToLowPowerMode = true;
                }
            }
        }
        // BEGIN: Added by zhanghongxing at 2013-08-07
        registerReceiver(mBatteryLevelRcvr, mBatteryLevelFilter);
        // END:   Added by zhanghongxing at 2013-08-07
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mDeletePictureReceiver);

        if (LOGV) Log.v(TAG, "onPause");
        saveThumbnailToFile();

        if (mLoadThumbnailTask != null) {
            mLoadThumbnailTask.cancel(true);
            mLoadThumbnailTask = null;
        }

        if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }

        // BEGIN: Added by zhanghongxing at 2013-08-07
        Util.hintFinishAlert();
        unregisterReceiver(mBatteryLevelRcvr);
        // END:   Added by zhanghongxing at 2013-08-07
        
        mCameraDefaultValueManager = null;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // getActionBar() should be after setContentView
        mActionBar = new GalleryActionBar(this);
        mActionBar.hide();
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Prevent software keyboard or voice search from showing up.
        if (keyCode == KeyEvent.KEYCODE_SEARCH
                || keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.isLongPress()) return true;
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            //If camera preview,set volue stream as AudioManager.STREAM_MUSIC
            if (mShowCameraAppView && mStreamType != AudioManager.STREAM_MUSIC) {
                setVolumeControlStream(AudioManager.STREAM_MUSIC);
                mStreamType = AudioManager.STREAM_MUSIC;
            } else if (!mShowCameraAppView && mStreamType != AudioManager.STREAM_RING) {
                setVolumeControlStream(AudioManager.STREAM_RING);
                mStreamType = AudioManager.STREAM_RING;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    protected void setResultEx(int resultCode) {
        mResultCodeForTesting = resultCode;
        setResult(resultCode);
    }

    protected void setResultEx(int resultCode, Intent data) {
        mResultCodeForTesting = resultCode;
        mResultDataForTesting = data;
        setResult(resultCode, data);
    }

    public int getResultCode() {
        return mResultCodeForTesting;
    }

    public Intent getResultData() {
        return mResultDataForTesting;
    }

    @Override
    protected void onDestroy() {
        isQueryedToLowPowerMode = false;
        PopupManager.removeInstance(this);
        // Unregister mContentObserver
        getContentResolver().unregisterContentObserver(mContentObserver);
        // unregisterReceiver(mBatteryLevelRcvr); // Deleted by zhanghongxing at 2013-08-07
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }

    protected void updateStorageHint(long storageSpace, int orientationCompensation) {
        int message = Integer.MIN_VALUE;
        //BEGIN: Modified by xiongzhu at 2013-04-15
        if (storageSpace == Storage.UNAVAILABLE) {
            // message = R.string.no_storage;
            message = Storage.mIsExternalStorage ? R.string.no_camera_storage : R.string.no_memory_storage;
        } else if (storageSpace == Storage.PREPARING) {
            //message = R.string.preparing_sd;
            message = Storage.mIsExternalStorage ? R.string.preparing_sd : R.string.preparing_memory;
        } else if (storageSpace == Storage.UNKNOWN_SIZE) {
            // message = R.string.access_sd_fail;
            message = Storage.mIsExternalStorage ? R.string.access_sd_fail : R.string.access_memory_fail;
        } else if (storageSpace < Storage.LOW_STORAGE_THRESHOLD) {
            // message = R.string.spaceIsLow_content;
            message = Storage.mIsExternalStorage ? R.string.spaceIsLow_content : R.string.memory_spaceIsLow_content;
        }
        //END:   Modified by xiongzhu at 2013-04-15
        if (message != Integer.MIN_VALUE) {
        	try {
                new RotateTextToast(this, message, orientationCompensation).show();
        	} catch (Exception e){
        		
        	}
        }
    }

    protected void updateThumbnailView() {
        if (mThumbnail != null) {
            mThumbnailView.setBitmap(mThumbnail.getBitmap());
            mThumbnailView.setVisibility(View.VISIBLE);
        } else {
            mThumbnailView.setBitmap(null);
            mThumbnailView.setVisibility(View.GONE);
        }
    }

    protected void getLastThumbnail() {
        mThumbnail = ThumbnailHolder.getLastThumbnail(getContentResolver());
        // Suppose users tap the thumbnail view, go to the gallery, delete the
        // image, and coming back to the camera. Thumbnail file will be invalid.
        // Since the new thumbnail will be loaded in another thread later, the
        // view should be set to gone to prevent from opening the invalid image.
        updateThumbnailView();
        if (mThumbnail == null) {
            mLoadThumbnailTask = new LoadThumbnailTask(true).execute();
        }
    }

    protected void getLastThumbnailUncached() {
        if (mLoadThumbnailTask != null) mLoadThumbnailTask.cancel(true);
        mLoadThumbnailTask = new LoadThumbnailTask(false).execute();
    }

    private class LoadThumbnailTask extends AsyncTask<Void, Void, Thumbnail> {
        private boolean mLookAtCache;

        public LoadThumbnailTask(boolean lookAtCache) {
            mLookAtCache = lookAtCache;
        }

        @Override
        protected Thumbnail doInBackground(Void... params) {
            // Load the thumbnail from the file.
            ContentResolver resolver = getContentResolver();
            Thumbnail t = null;
            if (mLookAtCache) {
                t = Thumbnail.getLastThumbnailFromFile(getFilesDir(), resolver);
            }

            if (isCancelled()) return null;

            if (t == null) {
                Thumbnail result[] = new Thumbnail[1];
                // Load the thumbnail from the media provider.
                int code = Thumbnail.getLastThumbnailFromContentResolver(
                        resolver, result);
                switch (code) {
                    case Thumbnail.THUMBNAIL_FOUND:
                        return result[0];
                    case Thumbnail.THUMBNAIL_NOT_FOUND:
                        return null;
                    case Thumbnail.THUMBNAIL_DELETED:
                        cancel(true);
                        return null;
                }
            }
            return t;
        }

        @Override
        protected void onPostExecute(Thumbnail thumbnail) {
            if (isCancelled()) return;
            mThumbnail = thumbnail;
            updateThumbnailView();
        }
    }

    protected void gotoGallery() {
    	Log.i(TAG,"zzw:gotoGallery()");
        // Move the next picture with capture animation. "1" means next.
        mAppBridge.switchWithCaptureAnimation(1);
    }

    protected void saveThumbnailToFile() {
        if (mThumbnail != null && !mThumbnail.fromFile()) {
            new SaveThumbnailTask().execute(mThumbnail);
        }
    }

    private class SaveThumbnailTask extends AsyncTask<Thumbnail, Void, Void> {
        @Override
        protected Void doInBackground(Thumbnail... params) {
            final int n = params.length;
            final File filesDir = getFilesDir();
            for (int i = 0; i < n; i++) {
                params[i].saveLastThumbnailToFile(filesDir);
            }
            return null;
        }
    }

    // Call this after setContentView.
    protected void createCameraScreenNail(boolean getPictures,boolean isExternalStorage) {
        mCameraAppView = findViewById(R.id.camera_app_root);
        Bundle data = new Bundle();
        String path = "/local/all/";
        // Intent mode does not show camera roll. Use 0 as a work around for
        // invalid bucket id.
        // TODO: add support of empty media set in gallery.
        //BEGIN: Modified by xiongzhu at 2013-04-15
        if(isExternalStorage){
            path += (getPictures ? Storage.CAMERA_BUCKET_ID : "0");
        }else{
	        path += (getPictures ? Storage.CAMERA_INTERNAL_BUCKET_ID : "0");
	}
	//END:   Modified by xiongzhu at 2013-04-15
		
        data.putString(PhotoPage.KEY_MEDIA_SET_PATH, path);
        data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, path);

        // Send an AppBridge to gallery to enable the camera preview.
        mAppBridge = new MyAppBridge();
        data.putParcelable(PhotoPage.KEY_APP_BRIDGE, mAppBridge);
        getStateManager().startState(PhotoPage.class, data);
        mCameraScreenNail = mAppBridge.getCameraScreenNail();
    }
    
    //BEGIN: Added by xiongzhu at 2013-04-15
    protected void updateCameraScreenNail(boolean getPictures,boolean isExternalStorage) {
        Bundle data = new Bundle();
        String path = "/local/all/";
        // Intent mode does not show camera roll. Use 0 as a work around for
        // invalid bucket id.
        // TODO: add support of empty media set in gallery.
        // start modified by xiongzhu at 2013-3-7
        if(isExternalStorage){
            path += (getPictures ? Storage.CAMERA_BUCKET_ID : "0");
        }else{
	        path += (getPictures ? Storage.CAMERA_INTERNAL_BUCKET_ID : "0");
	    }
		
        data.putString(PhotoPage.KEY_MEDIA_SET_PATH, path);
        data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, path);
        
        if (mAppBridge != null) {
            mCameraScreenNail.recycle();
        }
        mAppBridge = new MyAppBridge();
        data.putParcelable(PhotoPage.KEY_APP_BRIDGE, mAppBridge);
        
        StateManager stateManager = getStateManager();
        ActivityState state = stateManager.getTopState();
        if(state instanceof PhotoPage) {
			GLRoot root = (GLRoot) findViewById(R.id.gl_root_view);
			root.lockRenderThread();
			try {
				stateManager.switchState(state, PhotoPage.class, data);
				mCameraScreenNail = mAppBridge.getCameraScreenNail();
			} finally {
				root.unlockRenderThread();
			}
	    }  
    }
    //END:   Added by zhanghongxing at 2013-04-15
     

    private class HideCameraAppView implements Runnable {
        @Override
        public void run() {
            // We cannot set this as GONE because we want to receive the
            // onLayoutChange() callback even when we are invisible.
            mCameraAppView.setVisibility(View.INVISIBLE);
        }
    }

    protected void updateCameraAppView() {
        if (mShowCameraAppView) {
            mCameraAppView.setVisibility(View.VISIBLE);
            // The "transparent region" is not recomputed when a sibling of
            // SurfaceView changes visibility (unless it involves GONE). It's
            // been broken since 1.0. Call requestLayout to work around it.
            mCameraAppView.requestLayout();
            // withEndAction(null) prevents the pending end action
            // mHideCameraAppView from being executed.
            mCameraAppView.animate()
                    .setDuration(CAMERA_APP_VIEW_TOGGLE_TIME)
                    .withLayer().alpha(1).withEndAction(null);
        } else {
            mCameraAppView.animate()
                    .setDuration(CAMERA_APP_VIEW_TOGGLE_TIME)
                    .withLayer().alpha(0).withEndAction(mHideCameraAppView);
        }
    }

    private void onFullScreenChanged(boolean full) {
        if (mShowCameraAppView == full) return;
        mShowCameraAppView = full;
        if (mPaused || isFinishing()) return;
        // Initialize the animation.
        if (mHideCameraAppView == null) {
            mHideCameraAppView = new HideCameraAppView();
            mCameraAppView.animate()
                .setInterpolator(new DecelerateInterpolator());
        }
        updateCameraAppView();

        // If we received DELETE_PICTURE broadcasts while the Camera UI is
        // hidden, we update the thumbnail now.
        if (full && mUpdateThumbnailDelayed) {
            getLastThumbnailUncached();
            mUpdateThumbnailDelayed = false;
        }
    }

    @Override
    public GalleryActionBar getGalleryActionBar() {
        return mActionBar;
    }

    // Preview frame layout has changed.
    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (mAppBridge == null) return;

        if (left == oldLeft && top == oldTop && right == oldRight
                && bottom == oldBottom) {
            return;
        }


        int width = right - left;
        int height = bottom - top;
        if (Util.getDisplayRotation(this) % 180 == 0) {
            mCameraScreenNail.setPreviewFrameLayoutSize(width, height);
        } else {
            // Swap the width and height. Camera screen nail draw() is based on
            // natural orientation, not the view system orientation.
            mCameraScreenNail.setPreviewFrameLayoutSize(height, width);
        }

        // Find out the coordinates of the preview frame relative to GL
        // root view.
        View root = (View) getGLRoot();
        int[] rootLocation = new int[2];
        int[] viewLocation = new int[2];
        root.getLocationInWindow(rootLocation);
        v.getLocationInWindow(viewLocation);

        int l = viewLocation[0] - rootLocation[0];
        int t = viewLocation[1] - rootLocation[1];
        int r = l + width;
        int b = t + height;
        Rect frame = new Rect(l, t, r, b);
        Log.d(TAG, "set CameraRelativeFrame as " + frame);
        mAppBridge.setCameraRelativeFrame(frame);
    }

    protected void setSingleTapUpListener(View singleTapArea) {
        mSingleTapArea = singleTapArea;
    }

    private boolean onSingleTapUp(int x, int y) {
        // Ignore if listener is null or the camera control is invisible.
        if (mSingleTapArea == null || !mShowCameraAppView) return false;

        int[] relativeLocation = Util.getRelativeLocation((View) getGLRoot(),
                mSingleTapArea);
        x -= relativeLocation[0];
        y -= relativeLocation[1];
        if (x >= 0 && x < mSingleTapArea.getWidth() && y >= 0
                && y < mSingleTapArea.getHeight()) {
            onSingleTapUp(mSingleTapArea, x, y);
            return true;
        }
        return false;
    }

    protected void onSingleTapUp(View view, int x, int y) {
    }

    protected void setSwipingEnabled(boolean enabled) {
        mAppBridge.setSwipingEnabled(enabled);
    }

    protected void notifyScreenNailChanged() {
        mAppBridge.notifyScreenNailChanged();
    }

    protected void onPreviewTextureCopied() {
    }

    //////////////////////////////////////////////////////////////////////////
    //  The is the communication interface between the Camera Application and
    //  the Gallery PhotoPage.
    //////////////////////////////////////////////////////////////////////////

    class MyAppBridge extends AppBridge implements CameraScreenNail.Listener {
        private CameraScreenNail mCameraScreenNail;
        private Server mServer;

        @Override
        public ScreenNail attachScreenNail() {
            if (mCameraScreenNail == null) {
                mCameraScreenNail = new CameraScreenNail(this);
            }
            return mCameraScreenNail;
        }

        @Override
        public void detachScreenNail() {
            mCameraScreenNail = null;
        }

        public CameraScreenNail getCameraScreenNail() {
            return mCameraScreenNail;
        }

        // Return true if the tap is consumed.
        @Override
        public boolean onSingleTapUp(int x, int y) {
            return ActivityBase.this.onSingleTapUp(x, y);
        }

        // This is used to notify that the screen nail will be drawn in full screen
        // or not in next draw() call.
        @Override
        public void onFullScreenChanged(boolean full) {
            ActivityBase.this.onFullScreenChanged(full);
        }

        @Override
        public void requestRender() {
            getGLRoot().requestRender();
        }

        public int animationRunning(boolean running) {
            return getGLRoot().animationRunning(running);
        }

        @Override
        public void onPreviewTextureCopied() {
            ActivityBase.this.onPreviewTextureCopied();
        }

        @Override
        public void setServer(Server s) {
            mServer = s;
        }

        @Override
        public boolean isPanorama() {
            return ActivityBase.this.isPanoramaActivity();
        }

        private void setCameraRelativeFrame(Rect frame) {
            if (mServer != null) mServer.setCameraRelativeFrame(frame);
        }

        private void switchWithCaptureAnimation(int offset) {
            if (mServer != null) mServer.switchWithCaptureAnimation(offset);
        }

        public boolean isCurrentCameraPreview() {
            if (mServer != null) {
                return mServer.isCurrentCameraPreview();
            } else {
                return false;
            }
        }

        private void setSwipingEnabled(boolean enabled) {
            if (mServer != null) mServer.setSwipingEnabled(enabled);
        }

        private void notifyScreenNailChanged() {
            if (mServer != null) mServer.notifyScreenNailChanged();
        }

        public void notifyFilmModeStateToScreenNail(boolean enabled) {
            if(mCameraScreenNail != null) {
                mCameraScreenNail.setFilmMode(enabled);
            }
        }
    }

    private class UIHandler extends Handler {
        private static final int MSG_REMOVE_STORAGE_HINT = 1;
        private static final int REMOVE_DELAYED_TIME = 3000; // 3 seconds

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REMOVE_STORAGE_HINT:
                if (ActivityBase.this.mStorageHint != null) {
                    ActivityBase.this.mStorageHint.cancel();
                    ActivityBase.this.mStorageHint = null;
                }
                break;
                
            case OPEN_CAMERA_FAIL:
            	Util.showErrorAndFinish(ActivityBase.this,
                        R.string.cannot_connect_camera);
            	break;
            case CAMERA_DISABLED:
            	Util.showErrorAndFinish(ActivityBase.this,
                        R.string.camera_disabled);
            	break;

            default:
                super.handleMessage(msg);
            }
        }

        public void removeStorageHintDelayed() {
            Message msg = new Message();
            msg.what = MSG_REMOVE_STORAGE_HINT;
            sendMessageDelayed(msg, REMOVE_DELAYED_TIME);
        }
    }
    
    // BEGIN: Added by zhanghongxing at 2013-04-19
    private void monitorBatteryState() {
        mBatteryLevelRcvr = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
            	if(mCameraDevice == null) return;
                int rawlevel = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", -1);
                int status = intent.getIntExtra("status", -1);
                int health = intent.getIntExtra("health", -1);
                int level = -1; // -1 for unknown
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }
                // BEGIN: Added by zhanghongxing at 2013-04-24
              if (level <= 7){
            	  if(status != BatteryManager.BATTERY_STATUS_CHARGING) {
                      Util.showFinishAlert(ActivityBase.this,
                            R.string.camera_alert_battery_title,
                            R.string.camera_alert_battery_low);
            	  }
                } else if (level <= 15){
                	//enter low power mode
                    if(status != BatteryManager.BATTERY_STATUS_CHARGING
                            && !isQueryedToLowPowerMode) {
                        isQueryedToLowPowerMode = true;
                        Util.queryToLowPowerMode(ActivityBase.this,
                                R.string.low_power_title, R.string.low_power_tips);
                    }
                    return;
                } else {
                	restorePowerMode();
                    Util.hintFinishAlert();
                }
                // END:   Added by zhanghongxing at 2013-04-24
                if (BatteryManager.BATTERY_HEALTH_OVERHEAT == health) {
                    Util.showFinishAlert(ActivityBase.this,
                            R.string.camera_alert_battery_title,
                            R.string.camera_alert_battery_hot);
                } else {
                    // BEGIN: Modified by zhanghongxing at 2013-04-24
                    switch (status) {
//                        case BatteryManager.BATTERY_STATUS_CHARGING:
                        case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        case BatteryManager.BATTERY_STATUS_UNKNOWN:
                            if (level <= 7){
                                Util.showFinishAlert(ActivityBase.this,
                                        R.string.camera_alert_battery_title,
                                        R.string.camera_alert_battery_low);
                            } else {
                                Util.hintFinishAlert();
                            }
                            break;
                        case BatteryManager.BATTERY_STATUS_FULL:
                            break;
                        default:
                            Log.e(TAG, "Battery is indescribable!");
                    }
                    // END:   Modified by zhanghongxing at 2013-04-24
                }
            }
        };
        mBatteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        // BEGIN: Deleted by zhanghongxing at 2013-08-07
        // registerReceiver(mBatteryLevelRcvr, mBatteryLevelFilter);
        // END:   Deleted by zhanghongxing at 2013-08-07
    }
    // END: Added by zhanghongxing at 2013-04-19
    
    public void enterLowPowerMode() {
    }
    
    public void restorePowerMode() {
    }
    
    protected void enableShutterSound(boolean enable) {
    	android.hardware.Camera camera = mCameraDevice.getCamera();
    	Method enableShutterSound;   
    	try { 
    		if(Build.VERSION.SDK_INT >= 18) {	
    			enableShutterSound = camera.getClass().getMethod("enableShutterSound",new Class[] {boolean.class}); 
    		} else {
    			enableShutterSound = camera.getClass().getMethod("setShutterState",new Class[] {boolean.class}); 
    		}
        } catch (NoSuchMethodException e) { 
            e.printStackTrace(); 
            return;
        }
    	try { 
    		Object[] enableObj = {new Boolean(enable)};
    		enableShutterSound.invoke(camera,enableObj); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    } 
}
