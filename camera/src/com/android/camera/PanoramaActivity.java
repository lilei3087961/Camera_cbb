/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (c) 2013, The Linux Foundation. All Rights Reserved.
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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.android.camera.ui.PopupManager;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateLayout;
import com.android.gallery3d.ui.GLRootView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemProperties;

/**
 * Activity to handle panorama capturing.
 */
public class PanoramaActivity extends ActivityBase implements
        ModePicker.OnModeChangeListener, SurfaceTexture.OnFrameAvailableListener,
        ShutterButton.OnShutterButtonListener {
    public static final int DEFAULT_SWEEP_ANGLE = 400;
    public static final int DEFAULT_BLEND_MODE = Mosaic.BLENDTYPE_HORIZONTAL;
    public static final int DEFAULT_CAPTURE_PIXELS = 800 * 480;

    private static final int MSG_LOW_RES_FINAL_MOSAIC_READY = 1;
    private static final int MSG_RESET_TO_PREVIEW_WITH_THUMBNAIL = 2;
    private static final int MSG_GENERATE_FINAL_MOSAIC_ERROR = 3;
    private static final int MSG_RESET_TO_PREVIEW = 4;
    private static final int MSG_CLEAR_SCREEN_DELAY = 5;
    private static final int MSG_CHECK_STORAGE = 6;

    //The rest of the storage space that can be used.
    private long mStorageSpace;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    private static final String TAG = "PanoramaActivity";
    private static final int PREVIEW_STOPPED = 0;
    private static final int PREVIEW_ACTIVE = 1;
    private static final int CAPTURE_STATE_VIEWFINDER = 0;
    private static final int CAPTURE_STATE_MOSAIC = 1;

    private static final String GPS_DATE_FORMAT_STR = "yyyy:MM:dd";
    private static final String GPS_TIME_FORMAT_STR = "kk/1,mm/1,ss/1";
    private static final String DATETIME_FORMAT_STR = "yyyy:MM:dd kk:mm:ss";

    // Speed is in unit of deg/sec
    private static final float PANNING_SPEED_THRESHOLD = 25f;

    private ContentResolver mContentResolver;

    private GLRootView mGLRootView;
    private ViewGroup mPanoLayout;
    private LinearLayout mCaptureLayout;
    private View mReviewLayout;
    private ImageView mReview;
    private RotateLayout mCaptureIndicator;
    private PanoProgressBar mPanoProgressBar;
    private PanoProgressBar mSavingProgressBar;
    private View mPreviewArea;
    private View mLeftIndicator;
    private View mRightIndicator;
    private MosaicPreviewRenderer mMosaicPreviewRenderer;
    private TextView mTooFastPrompt;
    private ShutterButton mShutterButton;
    private Object mWaitObject = new Object();

    private DateFormat mGPSDateStampFormat;
    private DateFormat mGPSTimeStampFormat;
    private DateFormat mDateTimeStampFormat;

    private String mPreparePreviewString;
    private String mDialogTitle;
    private String mDialogOkString;
    private String mDialogPanoramaFailedString;
    private String mDialogWaitingPreviousString;

    private int mIndicatorColor;
    private int mIndicatorColorFast;

    private boolean mUsingFrontCamera;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mCameraState;
    private int mCaptureState;
    private PowerManager.WakeLock mPartialWakeLock;
    //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
    // private ModePicker mModePicker;
    private RotateImageView mSwitchToCamera;
//  private RotateImageView mSwitchToVideo;
    //private RotateImageView mSwitchToPanorama; delete by xiongzhu for cbb
    private RotateImageView mThumbnailWindow;
    //END:   Modified by zhanghongxing at 2013-01-09
    private MosaicFrameProcessor mMosaicFrameProcessor;
    private boolean mMosaicFrameProcessorInitialized;
    private AsyncTask <Void, Void, Void> mWaitProcessorTask;
    private long mTimeTaken;
    private Handler mMainHandler;
    private SurfaceTexture mCameraTexture;
    private boolean mThreadRunning;
    private boolean mCancelComputation;
    private float[] mTransformMatrix;
    private float mHorizontalViewAngle;
    private float mVerticalViewAngle;

    private boolean mIsFromOnPause = false;

    //Add by wangbin at 2013-03-22
    private String mSilentMode;
    private boolean mIsSilentMode = false;
    private boolean mIsLandScape = false;

    // Prefer FOCUS_MODE_INFINITY to FOCUS_MODE_CONTINUOUS_VIDEO because of
    // getting a better image quality by the former.
    private String mTargetFocusMode = Parameters.FOCUS_MODE_INFINITY;

    private PanoOrientationEventListener mOrientationEventListener;
    // The value could be 0, 90, 180, 270 for the 4 different orientations measured in clockwise
    // respectively.
    private int mDeviceOrientation;
    private int mDeviceOrientationAtCapture;
    private int mCameraOrientation;
    private int mOrientationCompensation = 0;

    private RotateDialogController mRotateDialog;

    private MediaActionSound mCameraSound;

    private Runnable mUpdateTexImageRunnable;
    private Runnable mOnFrameAvailableRunnable;

    //Added by xiongzhu at 2013-04-15
    private ComboPreferences mPreferences;
    
    //Added by zhanghongxing at 2013-01-14 for DER-465
    private boolean mDidRegister = false;

    private class SetupCameraThread extends Thread {
        @Override
        public void run() {
            try {
                setupCamera();
            } catch (CameraHardwareException e) {
                mOpenCameraFail = true;
            } catch (CameraDisabledException e) {
                mCameraDisabled = true;
            }
        }
    }

    private class MosaicJpeg {
        public MosaicJpeg(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.isValid = true;
        }

        public MosaicJpeg() {
            this.data = null;
            this.width = 0;
            this.height = 0;
            this.isValid = false;
        }
        // Only used to distinguish between consistency and size error.
        public MosaicJpeg(int invalid) {
            this.data = null;
            this.width = 0;
            this.height = 0;
            this.isValid = false;
            this.invalid = invalid;
        }

        public final byte[] data;
        public final int width;
        public final int height;
        public final boolean isValid;
        // Convenient to distinguish the types of errors.
        public int invalid;
    }

    private class PanoOrientationEventListener extends OrientationEventListener {
        public PanoOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mDeviceOrientation = Util.roundOrientation(orientation, mDeviceOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mDeviceOrientation
                    + Util.getDisplayRotation(PanoramaActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                // Do not rotate the icons during capturing because the Camrea
                // orientation is fixed after capturing.
                if (mCaptureState == CAPTURE_STATE_VIEWFINDER) {
                    setOrientationIndicator(mOrientationCompensation, true);
                }
            }
        }
    }

    private void setOrientationIndicator(int orientation, boolean animation) {
		Log.i(TAG,"setOrientationIndicator() mSwitchToCamera="+mSwitchToCamera);
        //BEGIN: Modified by zhanghongxing at 2013-02-28 for full preview
        // Rotatable[] indicators = {mThumbnailView, mModePicker};
        Rotatable[] indicators = {mThumbnailView, mSwitchToCamera,mThumbnailBgView, //mSwitchToPanorama delete by xiongzhu for cbb
        mShutterButton};
        //END:   Modified by zhanghongxing at 2013-02-28
        for (Rotatable indicator : indicators) {
            if (indicator != null) indicator.setOrientation(orientation, animation);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        // Dismiss the mode selection window if the ACTION_DOWN event is out of
        // its view area.
        if (m.getAction() == MotionEvent.ACTION_DOWN) {
            //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
            // if ((mModePicker != null)
            //         && !Util.pointInView(m.getX(), m.getY(), mModePicker)) {
            //     mModePicker.dismissModeSelection();
            // }
            //END:   Deleted by zhanghongxing at 2013-01-09
        }
        return super.dispatchTouchEvent(m);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        createContentView();

        //BEGIN: Modified by xiongzhu at 2013-04-15
        mPreferences = new ComboPreferences(this);
        CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
        mPreferences.setLocalId(this,CameraHolder.instance().getBackCameraId());
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());

        //Add by wangbin at 2013-03-22
        mSilentMode = mPreferences.getString(
                CameraSettings.KEY_SILENT_MODE,
                getString(R.string.pref_silentmode_default));
        if (CameraSettings.SILENT_MODE_OFF.equals(mSilentMode)) {
            mIsSilentMode = false;
        } else {
            mIsSilentMode = true;
        }

        String storagePlace = mPreferences.getString(CameraSettings.KEY_CAMERA_STORAGE_PLACE, getString(R.string.pref_storage_place_default));
 		Storage.mIsExternalStorage=storagePlace.equals( CameraSettings.CAMERA_STORAGE_SDCARD ) ? true:false;
		
		if( Storage.mIsExternalStorage ) {
		    mStorageSpace = Storage.getAvailableSpace();
			if (mStorageSpace == Storage.UNAVAILABLE) {
			    SharedPreferences.Editor editor = ComboPreferences
                        .get(PanoramaActivity.this).edit();
                editor.putString(CameraSettings.KEY_CAMERA_STORAGE_PLACE, CameraSettings.CAMERA_STORAGE_MEMORY);
                editor.apply();
			    Storage.mIsExternalStorage = false;
			    storagePlace = CameraSettings.CAMERA_STORAGE_MEMORY;
			}
		}
		
		mStoragePlace = storagePlace;
        installIntentFilter();
        getLastThumbnail();

        setOrientationIndicator(mOrientationCompensation, true);

        mContentResolver = getContentResolver();
        // createCameraScreenNail(true);
        createCameraScreenNail(true,Storage.mIsExternalStorage);
        //END:   Modified by xiongzhu at 2013-04-15

        mUpdateTexImageRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if the activity is paused here can speed up the onPause() process.
                if (mPaused) return;
                mCameraTexture.updateTexImage();
                mCameraTexture.getTransformMatrix(mTransformMatrix);
            }
        };

        // This runs in UI thread.
        mOnFrameAvailableRunnable = new Runnable() {
            @Override
            public void run() {
                // Frames might still be available after the activity is paused.
                // If we call onFrameAvailable after pausing, the GL thread will crash.
                if (mPaused) return;

                if (mGLRootView.getVisibility() != View.VISIBLE) {
                    mMosaicPreviewRenderer.showPreviewFrameSync();
                    mGLRootView.setVisibility(View.VISIBLE);
                } else {
                    if (mCaptureState == CAPTURE_STATE_VIEWFINDER) {
                        mMosaicPreviewRenderer.showPreviewFrame();
                    } else {
                        mMosaicPreviewRenderer.alignFrame();
                        mMosaicFrameProcessor.processFrame();
                    }
                }
            }
        };

        mGPSDateStampFormat = new SimpleDateFormat(GPS_DATE_FORMAT_STR);
        mGPSTimeStampFormat = new SimpleDateFormat(GPS_TIME_FORMAT_STR);
        mDateTimeStampFormat = new SimpleDateFormat(DATETIME_FORMAT_STR);
        TimeZone tzUTC = TimeZone.getTimeZone("UTC");
        mGPSDateStampFormat.setTimeZone(tzUTC);
        mGPSTimeStampFormat.setTimeZone(tzUTC);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Panorama");

        mOrientationEventListener = new PanoOrientationEventListener(this);

        mTransformMatrix = new float[16];
        mMosaicFrameProcessor = MosaicFrameProcessor.getInstance();

        Resources appRes = getResources();
        mPreparePreviewString = appRes.getString(R.string.pano_dialog_prepare_preview);
        mDialogTitle = appRes.getString(R.string.pano_dialog_title);
        mDialogOkString = appRes.getString(R.string.dialog_ok);
        mDialogPanoramaFailedString = appRes.getString(R.string.pano_dialog_panorama_failed);
        mDialogWaitingPreviousString = appRes.getString(R.string.pano_dialog_waiting_previous);

        mGLRootView = (GLRootView) getGLRoot();

        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_LOW_RES_FINAL_MOSAIC_READY:
                        onBackgroundThreadFinished();
                        showFinalMosaic((Bitmap) msg.obj);
                        saveHighResMosaic();
                        break;
                    case MSG_RESET_TO_PREVIEW_WITH_THUMBNAIL:
                        onBackgroundThreadFinished();
                        // If the activity is paused, save the thumbnail to the file here.
                        // If not, it will be saved in onPause.
                        if (mPaused) saveThumbnailToFile();
                        // Set the thumbnail bitmap here because mThumbnailView must be accessed
                        // from the UI thread.
                        if (mThumbnail != null) mThumbnailView.setBitmap(mThumbnail.getBitmap());

                        resetToPreview();
                        clearMosaicFrameProcessorIfNeeded();
                        break;
                    case MSG_GENERATE_FINAL_MOSAIC_ERROR:
                        onBackgroundThreadFinished();
                        if (mPaused) {
                            resetToPreview();
                        } else {
                            mRotateDialog.showAlertDialog(
                                    mDialogTitle, mDialogPanoramaFailedString,
                                    mDialogOkString, new Runnable() {
                                        @Override
                                        public void run() {
                                            resetToPreview();
                                        }},
                                    null, null);
                        }
                        clearMosaicFrameProcessorIfNeeded();
                        break;
                    case MSG_RESET_TO_PREVIEW:
                        onBackgroundThreadFinished();
                        resetToPreview();
                        clearMosaicFrameProcessorIfNeeded();
                        break;
                    case MSG_CLEAR_SCREEN_DELAY:
                        getWindow().clearFlags(WindowManager.LayoutParams.
                                FLAG_KEEP_SCREEN_ON);
                        break;
                    case MSG_CHECK_STORAGE:
                        checkStorageAndShowHint();
                        break;
                }
            }
        };
        mMainHandler.sendEmptyMessage(MSG_CHECK_STORAGE);
    }

    //Check the storage state and show the hint if necessary.
    private void checkStorageAndShowHint() {
        mStorageSpace = Storage.getAvailableSpace();
        updateStorageHint(mStorageSpace, mOrientationCompensation);
    }

    @Override
    public boolean isPanoramaActivity() {
        return true;
    }

    private void setupCamera() throws CameraHardwareException, CameraDisabledException {
        openCamera();
        Parameters parameters = mCameraDevice.getParameters();
        setupCaptureParams(parameters);
        configureCamera(parameters);
    }

    private void releaseCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.setPreviewCallbackWithBuffer(null);
            CameraHolder.instance().release();
            mCameraDevice = null;
            mCameraState = PREVIEW_STOPPED;
        }
    }

    private void openCamera() throws CameraHardwareException, CameraDisabledException {
        int cameraId = CameraHolder.instance().getBackCameraId();
        // If there is no back camera, use the first camera. Camera id starts
        // from 0. Currently if a camera is not back facing, it is front facing.
        // This is also forward compatible if we have a new facing other than
        // back or front in the future.
        if (cameraId == -1) cameraId = 0;
        mCameraDevice = Util.openCamera(this, cameraId);
        mCameraOrientation = Util.getCameraOrientation(cameraId);
        if (cameraId == CameraHolder.instance().getFrontCameraId()) mUsingFrontCamera = true;
    }

    private boolean findBestPreviewSize(List<Size> supportedSizes, boolean need5To3,
            boolean needSmaller) {
        int pixelsDiff = DEFAULT_CAPTURE_PIXELS;
        boolean hasFound = false;
        for (Size size : supportedSizes) {
            int h = size.height;
            int w = size.width;
            // we only want 4:3 format.
            int d = DEFAULT_CAPTURE_PIXELS - h * w;
            if (needSmaller && d < 0) { // no bigger preview than 960x720.
                continue;
            }
            //BEGIN: Modified by zhanghongxing at 2013-03-27 for FBD-573
             if (need5To3 && (h * 5 != w * 3)) {
//            if (need16To9 && (h * 16 != w * 9)) {
                continue;
            }
            //END:   Modified by zhanghongxing at 2013-03-27
            d = Math.abs(d);
            if (d < pixelsDiff) {
                mPreviewWidth = w;
                mPreviewHeight = h;
                pixelsDiff = d;
                hasFound = true;
            }
        }
        return hasFound;
    }

    private void setupCaptureParams(Parameters parameters) {
        List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
        if (!findBestPreviewSize(supportedSizes, true, true)) {
            Log.w(TAG, "No 4:3 ratio preview size supported.");
            if (!findBestPreviewSize(supportedSizes, false, true)) {
                Log.w(TAG, "Can't find a supported preview size smaller than 960x720.");
                findBestPreviewSize(supportedSizes, false, false);
            }
        }
        Log.v(TAG, "preview h = " + mPreviewHeight + " , w = " + mPreviewWidth);
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);

        List<int[]> frameRates = parameters.getSupportedPreviewFpsRange();
        int last = frameRates.size() - 1;
        int minFps = (frameRates.get(last))[Parameters.PREVIEW_FPS_MIN_INDEX];
        int maxFps = (frameRates.get(last))[Parameters.PREVIEW_FPS_MAX_INDEX];
        parameters.setPreviewFpsRange(minFps, maxFps);
        Log.v(TAG, "preview fps: " + minFps + ", " + maxFps);

        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (supportedFocusModes.indexOf(mTargetFocusMode) >= 0) {
            parameters.setFocusMode(mTargetFocusMode);
        } else {
            // Use the default focus mode and log a message
            Log.w(TAG, "Cannot set the focus mode to " + mTargetFocusMode +
                  " becuase the mode is not supported.");
        }

        parameters.setRecordingHint(false);

        mHorizontalViewAngle = parameters.getHorizontalViewAngle();
        mVerticalViewAngle =  parameters.getVerticalViewAngle();
        
        //BEGIN: Added by zhanghongxing at 2012-12-28 for DER-153
        // Set anti banding parameter.
        String antiBanding = getString(R.string.pref_camera_antibanding_default);
        Log.v(TAG, "antiBanding value =" + antiBanding);
        if (isSupported(antiBanding, parameters.getSupportedAntibanding())) {
            parameters.setAntibanding(antiBanding);
        }
        //END:   Added by zhanghongxing at 2012-12-28
    }
    
    //BEGIN: Added by zhanghongxing at 2012-12-28 for DER-153
    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }
    //END:   Added by zhanghongxing at 2012-12-28

    public int getPreviewBufSize() {
        PixelFormat pixelInfo = new PixelFormat();
        PixelFormat.getPixelFormatInfo(mCameraDevice.getParameters().getPreviewFormat(), pixelInfo);
        // TODO: remove this extra 32 byte after the driver bug is fixed.
        return (mPreviewWidth * mPreviewHeight * pixelInfo.bitsPerPixel / 8) + 32;
    }

    private void configureCamera(Parameters parameters) {
        mCameraDevice.setParameters(parameters);
    }

    private void switchToOtherMode(int mode) {
        if (isFinishing()) return;
        if (mThumbnail != null) ThumbnailHolder.keep(mThumbnail);
        MenuHelper.gotoMode(mode, this);
        mMainHandler.removeMessages(MSG_CHECK_STORAGE);
        finish();
    }

    @Override
    public void onModeChanged(int mode) {
        //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
        //if (mode != ModePicker.MODE_PANORAMA) switchToOtherMode(mode);
        //END:   Deleted by zhanghongxing at 2013-01-09
    }

    private void configMosaicPreview(int w, int h) {
        stopCameraPreview();
        mCameraScreenNail.setSize(w, h);
        if (mCameraScreenNail.getSurfaceTexture() == null) {
            mCameraScreenNail.acquireSurfaceTexture();
        } else {
            mCameraScreenNail.releaseSurfaceTexture();
            mCameraScreenNail.acquireSurfaceTexture();
            notifyScreenNailChanged();
        }
        boolean isLandscape = (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);
        if (mMosaicPreviewRenderer != null) mMosaicPreviewRenderer.release();
        mMosaicPreviewRenderer = new MosaicPreviewRenderer(
                mCameraScreenNail.getSurfaceTexture(), w, h, isLandscape);

        mCameraTexture = mMosaicPreviewRenderer.getInputSurfaceTexture();
        if (!mPaused && !mThreadRunning && mWaitProcessorTask == null) {
            resetToPreview();
        }
    }

    // Receives the layout change event from the preview area. So we can set
    // the camera preview screennail to the same size and initialize the mosaic
    // preview renderer.
    @Override
    public void onLayoutChange(View v, int l, int t, int r, int b,
            int oldl, int oldt, int oldr, int oldb) {
        super.onLayoutChange(v, l, t, r, b, oldl, oldt, oldr, oldb);
        if (l == oldl && t == oldt && r == oldr && b == oldb
                && mCameraScreenNail.getSurfaceTexture() != null) {
            // Nothing changed and this has been called already.
            return;
        }
        configMosaicPreview(r - l, b - t);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surface) {
        /* This function may be called by some random thread,
         * so let's be safe and jump back to ui thread.
         * No OpenGL calls can be done here. */
        runOnUiThread(mOnFrameAvailableRunnable);
    }

    private void hideDirectionIndicators() {
        mLeftIndicator.setVisibility(View.INVISIBLE);
        mRightIndicator.setVisibility(View.INVISIBLE);
    }

    private void showDirectionIndicators(int direction) {
        switch (direction) {
            case PanoProgressBar.DIRECTION_NONE:
                mLeftIndicator.setVisibility(View.VISIBLE);
                mRightIndicator.setVisibility(View.VISIBLE);
                break;
            case PanoProgressBar.DIRECTION_LEFT:
                mLeftIndicator.setVisibility(View.VISIBLE);
                mRightIndicator.setVisibility(View.INVISIBLE);
                break;
            case PanoProgressBar.DIRECTION_RIGHT:
                mLeftIndicator.setVisibility(View.INVISIBLE);
                mRightIndicator.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void startCapture() {
        // Reset values so we can do this again.
        mCancelComputation = false;
        mTimeTaken = System.currentTimeMillis();
        setSwipingEnabled(false);
        mCaptureState = CAPTURE_STATE_MOSAIC;
        
//        mShutterButton.setBackgroundResource(R.drawable.shutterbutton_background);
        mShutterButton.setImageResource(R.drawable.btn_shutter_pan_recording);

        mCaptureIndicator.setVisibility(View.VISIBLE);
        showDirectionIndicators(PanoProgressBar.DIRECTION_NONE);
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // mThumbnailView.setEnabled(false);
        mThumbnailWindow.setEnabled(false);
        //END:   Modified by zhanghongxing at 2013-01-09

        mMosaicFrameProcessor.setProgressListener(new MosaicFrameProcessor.ProgressListener() {
            @Override
            public void onProgress(boolean isFinished, float panningRateX, float panningRateY,
                    float progressX, float progressY) {
                float accumulatedHorizontalAngle = progressX * mHorizontalViewAngle;
                float accumulatedVerticalAngle = progressY * mVerticalViewAngle;
                if (isFinished
                        || (Math.abs(accumulatedHorizontalAngle) >= DEFAULT_SWEEP_ANGLE)
                        || (Math.abs(accumulatedVerticalAngle) >= DEFAULT_SWEEP_ANGLE)) {
                    stopCapture(false);
                } else {
                    float panningRateXInDegree = panningRateX * mHorizontalViewAngle;
                    float panningRateYInDegree = panningRateY * mVerticalViewAngle;
                    updateProgress(panningRateXInDegree, panningRateYInDegree,
                            accumulatedHorizontalAngle, accumulatedVerticalAngle);
                }
            }
        });

        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // if (mModePicker != null) mModePicker.setEnabled(false);
        if (mSwitchToCamera != null) mSwitchToCamera.setEnabled(false);
//        if (mSwitchToVideo != null) mSwitchToVideo.setEnabled(false);
        //if (mSwitchToPanorama != null) mSwitchToPanorama.setEnabled(false);  delete by xiongzhu for cbb
        //END:   Modified by zhanghongxing at 2013-01-09

        mPanoProgressBar.reset();
        // TODO: calculate the indicator width according to different devices to reflect the actual
        // angle of view of the camera device.
        mPanoProgressBar.setIndicatorWidth(20);
        mPanoProgressBar.setMaxProgress(DEFAULT_SWEEP_ANGLE);
        mPanoProgressBar.setVisibility(View.VISIBLE);
        mDeviceOrientationAtCapture = mDeviceOrientation;
        keepScreenOn();
    }

    private void stopCapture(boolean aborted) {
        mCaptureState = CAPTURE_STATE_VIEWFINDER;
        mCaptureIndicator.setVisibility(View.GONE);
        hideTooFastIndication();
        hideDirectionIndicators();

        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // mThumbnailView.setEnabled(true);
        mThumbnailWindow.setEnabled(true);
        //END:   Modified by zhanghongxing at 2013-01-09

        // The orientation was fixed during capturing. Now make it
        // reflect the device orientation as capturing is stopped.
        setOrientationIndicator(mOrientationCompensation, true);
        
        mMosaicFrameProcessor.setProgressListener(null);
        stopCameraPreview();

        mCameraTexture.setOnFrameAvailableListener(null);
        
//        mShutterButton.setBackgroundResource(R.drawable.shutterbutton_background);
        mShutterButton.setImageResource(R.drawable.btn_shutter_pan);
        if (!aborted && !mThreadRunning) {
            mRotateDialog.showWaitingDialog(mPreparePreviewString);
            runBackgroundThread(new Thread() {
                @Override
                public void run() {
					if(mPaused) return;
                    MosaicJpeg jpeg = generateFinalMosaic(false);

                    if (jpeg != null && jpeg.isValid) {
                        Bitmap bitmap = null;
                        bitmap = BitmapFactory.decodeByteArray(jpeg.data, 0, jpeg.data.length);
                        mMainHandler.sendMessage(mMainHandler.obtainMessage(
                                MSG_LOW_RES_FINAL_MOSAIC_READY, bitmap));
                        jpeg = null;
                    } else {
                       // Whether the consistency and size error occurs, if it is,
                       // then gives a specific message.
                       if (!showDetailFailMessages(jpeg.invalid)) {
                               mMainHandler.sendMessage(mMainHandler.obtainMessage(
                                    MSG_RESET_TO_PREVIEW));
                        }
                    }
                }
            });
        }
        // do we have to wait for the thread to complete before enabling this?
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // if (mModePicker != null) mModePicker.setEnabled(true);
        if (mSwitchToCamera != null) mSwitchToCamera.setEnabled(true);
//        if (mSwitchToVideo != null) mSwitchToVideo.setEnabled(true);
        //if (mSwitchToPanorama != null) mSwitchToPanorama.setEnabled(true); delete by xiongzhu for cbb
        //END:   Modified by zhanghongxing at 2013-01-09
        keepScreenOnAwhile();
    }

    private void showTooFastIndication() {
        mTooFastPrompt.setVisibility(View.VISIBLE);
        // The PreviewArea also contains the border for "too fast" indication.
        mPreviewArea.setVisibility(View.VISIBLE);
        mPanoProgressBar.setIndicatorColor(mIndicatorColorFast);
        mLeftIndicator.setEnabled(true);
        mRightIndicator.setEnabled(true);
    }

    private void hideTooFastIndication() {
        mTooFastPrompt.setVisibility(View.GONE);
        // We set "INVISIBLE" instead of "GONE" here because we need mPreviewArea to have layout
        // information so we can know the size and position for mCameraScreenNail.
        mPreviewArea.setVisibility(View.INVISIBLE);
        mPanoProgressBar.setIndicatorColor(mIndicatorColor);
        mLeftIndicator.setEnabled(false);
        mRightIndicator.setEnabled(false);
    }

    private void updateProgress(float panningRateXInDegree, float panningRateYInDegree,
            float progressHorizontalAngle, float progressVerticalAngle) {
        mGLRootView.requestRender();

        // TODO: Now we just display warning message by the panning speed.
        // Since we only support horizontal panning, we should display a warning message
        // in UI when there're significant vertical movements.
        if ((Math.abs(panningRateXInDegree) > PANNING_SPEED_THRESHOLD)
            || (Math.abs(panningRateYInDegree) > PANNING_SPEED_THRESHOLD)) {
            showTooFastIndication();
        } else {
            hideTooFastIndication();
        }
        int angleInMajorDirection =
                (Math.abs(progressHorizontalAngle) > Math.abs(progressVerticalAngle))
                ? (int) progressHorizontalAngle
                : (int) progressVerticalAngle;
        mPanoProgressBar.setProgress((angleInMajorDirection));
    }

    private void setViews(Resources appRes) {
        mCaptureState = CAPTURE_STATE_VIEWFINDER;
        mPanoProgressBar = (PanoProgressBar) findViewById(R.id.pano_pan_progress_bar);
        mPanoProgressBar.setBackgroundColor(appRes.getColor(R.color.pano_progress_empty));
        mPanoProgressBar.setDoneColor(appRes.getColor(R.color.pano_progress_done));
        mPanoProgressBar.setIndicatorColor(mIndicatorColor);
        mPanoProgressBar.setOnDirectionChangeListener(
                new PanoProgressBar.OnDirectionChangeListener () {
                    @Override
                    public void onDirectionChange(int direction) {
                        if (mCaptureState == CAPTURE_STATE_MOSAIC) {
                            showDirectionIndicators(direction);
                        }
                    }
                });

        mLeftIndicator = findViewById(R.id.pano_pan_left_indicator);
        mRightIndicator = findViewById(R.id.pano_pan_right_indicator);
        mLeftIndicator.setEnabled(false);
        mRightIndicator.setEnabled(false);
        mTooFastPrompt = (TextView) findViewById(R.id.pano_capture_too_fast_textview);
        // This mPreviewArea also shows the border for visual "too fast" indication.
        mPreviewArea = findViewById(R.id.pano_preview_area);
        mPreviewArea.addOnLayoutChangeListener(this);

        mSavingProgressBar = (PanoProgressBar) findViewById(R.id.pano_saving_progress_bar);
        mSavingProgressBar.setIndicatorWidth(0);
        mSavingProgressBar.setMaxProgress(100);
        mSavingProgressBar.setBackgroundColor(appRes.getColor(R.color.pano_progress_empty));
        mSavingProgressBar.setDoneColor(appRes.getColor(R.color.pano_progress_done));

        mCaptureIndicator = (RotateLayout) findViewById(R.id.pano_capture_indicator);

        //BEGIN: Added by zhanghongxing at 2013-01-09 for full preview
        RelativeLayout thumbnailFrame = (RelativeLayout) findViewById(R.id.thumbnail_frame);
        thumbnailFrame.setVisibility(View.VISIBLE);
        mThumbnailWindow = (RotateImageView) findViewById(R.id.thumbnail_bg);
        //END:   Added by zhanghongxing at 2013-01-09
        mThumbnailView = (RotateImageView) findViewById(R.id.thumbnail);
        mThumbnailBgView = (RotateImageView) findViewById(R.id.thumbnail_bg_null);
        mThumbnailView.enableFilter(false);
        mThumbnailViewWidth = mThumbnailView.getLayoutParams().width;

        mReviewLayout = findViewById(R.id.pano_review_layout);
        mReview = (ImageView) findViewById(R.id.pano_reviewarea);

        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // mModePicker = (ModePicker) findViewById(R.id.mode_picker);
        // mModePicker.setVisibility(View.VISIBLE);
        // mModePicker.setOnModeChangeListener(this);
        // mModePicker.setCurrentMode(ModePicker.MODE_PANORAMA);
        //RelativeLayout modeSwitcher = (RelativeLayout) findViewById(R.id.mode_switcher);
        //modeSwitcher.setVisibility(View.VISIBLE);
        RelativeLayout closePano = (RelativeLayout) findViewById(R.id.close_pano);
        closePano.setVisibility(View.VISIBLE);
        mSwitchToCamera = (RotateImageView) findViewById(R.id.close_pano_btn);
        mSwitchToCamera.enableFilter(false);
        mSwitchToCamera.setImageResource(R.drawable.btn_pan_close_img);
//        mSwitchToVideo = (RotateImageView) findViewById(R.id.mode_video);
//        mSwitchToVideo.enableFilter(false);
//        mSwitchToVideo.setImageResource(R.drawable.ic_switch_video_dark);
        /**mSwitchToPanorama = (RotateImageView) findViewById(R.id.mode_pano); delete by xiongzhu for cbb
        mSwitchToPanorama.enableFilter(false);
        mSwitchToPanorama.setImageResource(R.drawable.ic_switch_pan_holo_light);*/
        //END:   Modified by zhanghongxing at 2013-01-09

        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        
//        mShutterButton.setBackgroundResource(R.drawable.shutterbutton_background);
        mShutterButton.setImageResource(R.drawable.btn_shutter_pan);
        
        mShutterButton.setOnShutterButtonListener(this);

        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            Rotatable[] rotateLayout = {
                    (Rotatable) findViewById(R.id.pano_pan_progress_bar_layout),
                    (Rotatable) findViewById(R.id.pano_capture_too_fast_textview_layout),
                    (Rotatable) findViewById(R.id.pano_review_saving_indication_layout),
                    (Rotatable) findViewById(R.id.pano_saving_progress_bar_layout),
                    (Rotatable) findViewById(R.id.pano_review_cancel_button_layout),
                    (Rotatable) findViewById(R.id.pano_rotate_reviewarea),
                    mRotateDialog,
                    mCaptureIndicator,
                    //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
                    // mModePicker,
//                    mSwitchToVideo,
                    //mSwitchToPanorama, delete by xiongzhu for cbb
                    //END:   Modified by zhanghongxing at 2013-01-09
                    mThumbnailView};
            for (Rotatable r : rotateLayout) {
                r.setOrientation(270, false);
            }
        } else {
            // Even if the orientation is 0, we still need to set because it might be previously
            // set when the configuration is portrait.
            mRotateDialog.setOrientation(0, false);
        }
    }

    private void createContentView() {
        setContentView(R.layout.panorama);
        Resources appRes = getResources();
        mCaptureLayout = (LinearLayout) findViewById(R.id.camera_app_root);
        mIndicatorColor = appRes.getColor(R.color.pano_progress_indication);
        mIndicatorColorFast = appRes.getColor(R.color.pano_progress_indication_fast);
        mPanoLayout = (ViewGroup) findViewById(R.id.pano_layout);
        mRotateDialog = new RotateDialogController(this, R.layout.rotate_dialog);
        setViews(appRes);
    }

    @Override
    public void onShutterButtonClick() {
        // If mCameraTexture == null then GL setup is not finished yet.
        // No buttons can be pressed.
        if (mPaused || mThreadRunning || mCameraTexture == null) return;
        // Since this button will stay on the screen when capturing, we need to check the state
        // right now.
        switch (mCaptureState) {
            case CAPTURE_STATE_VIEWFINDER:
                //Disable taking picture function if there is not enough storage.
                if (mStorageSpace <= Storage.LOW_STORAGE_THRESHOLD) {
                    updateStorageHint(mStorageSpace, mOrientationCompensation);
                    Log.i(TAG, "There is not enough storage. remaining=" + mStorageSpace);
                    break;
                }
                //BEGIN: Modified by wangbin at 2013-03-22
                if (!mIsSilentMode) {
                    mCameraSound.play(MediaActionSound.START_VIDEO_RECORDING);
                }
                //mCameraSound.play(MediaActionSound.START_VIDEO_RECORDING);
                //END: Modified by wangbin at 2013-03-22
                startCapture();
                break;
            case CAPTURE_STATE_MOSAIC:
                //BEGIN: Modified by wangbin at 2013-03-22
                if (!mIsSilentMode) {
                    mCameraSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                }
                //mCameraSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                //END: Modified by wangbin at 2013-03-22
                stopCapture(false);
        }
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
    }

    public void reportProgress() {
        mSavingProgressBar.reset();
        mSavingProgressBar.setRightIncreasing(true);
        Thread t = new Thread() {
            @Override
            public void run() {
                while (mThreadRunning) {
                    final int progress = mMosaicFrameProcessor.reportProgress(
                            true, mCancelComputation);

                    try {
                        synchronized (mWaitObject) {
                            mWaitObject.wait(50);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Panorama reportProgress failed", e);
                    }
                    // Update the progress bar
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSavingProgressBar.setProgress(progress);
                        }
                    });
                }
            }
        };
        t.start();
    }

    public void saveHighResMosaic() {
        runBackgroundThread(new Thread() {
            @Override
            public void run() {
                mPartialWakeLock.acquire();
                MosaicJpeg jpeg;
                try {
					if(mPaused) return;
                    jpeg = generateFinalMosaic(true);
                } finally {
                    mPartialWakeLock.release();
                }

                if (jpeg == null) {  // Cancelled by user.
                    mMainHandler.sendEmptyMessage(MSG_RESET_TO_PREVIEW);
                } else if (!jpeg.isValid) {  // Error when generating mosaic.
                        // Whether the consistency and size error occurs, if it is,
                        // then gives a specific message.
                        if (!showDetailFailMessages(jpeg.invalid)) {
                                mMainHandler.sendEmptyMessage(MSG_GENERATE_FINAL_MOSAIC_ERROR);
                        }
                } else {
                    // The panorama image returned from the library is oriented based on the
                    // natural orientation of a camera. We need to set an orientation for the image
                    // in its EXIF header, so the image can be displayed correctly.
                    // The orientation is calculated from compensating the
                    // device orientation at capture and the camera orientation respective to
                    // the natural orientation of the device.
                    int orientation;
                    if (mUsingFrontCamera) {
                        // mCameraOrientation is negative with respect to the front facing camera.
                        // See document of android.hardware.Camera.Parameters.setRotation.
                        orientation = (mDeviceOrientationAtCapture - mCameraOrientation + 360) % 360;
                    } else {
                        orientation = (mDeviceOrientationAtCapture + mCameraOrientation) % 360;
                    }
                    Uri uri = savePanorama(jpeg.data, jpeg.width, jpeg.height, orientation);
                    if (uri != null) {
                        // Create a thumbnail whose width and height is equal or bigger
                        // than the thumbnail view's width.
                        int ratio = (int) Math.ceil(
                                (double) (jpeg.height > jpeg.width ? jpeg.width : jpeg.height)
                                / mThumbnailViewWidth);
                        int inSampleSize = Integer.highestOneBit(ratio);
                        orientation = Exif.getOrientation(jpeg.data);
                        mThumbnail = null;
                        mThumbnail = Thumbnail.createThumbnail(
                                jpeg.data, orientation, inSampleSize, uri);
                        Util.broadcastNewPicture(PanoramaActivity.this, uri);
                    }
                    mMainHandler.sendMessage(
                            mMainHandler.obtainMessage(MSG_RESET_TO_PREVIEW_WITH_THUMBNAIL));
                }
            }
        });
        reportProgress();
    }

    private void runBackgroundThread(Thread thread) {
        mThreadRunning = true;
        thread.start();
    }

    private void onBackgroundThreadFinished() {
        mThreadRunning = false;
        mRotateDialog.dismissDialog();
    }

    private void cancelHighResComputation() {
        mCancelComputation = true;
        synchronized (mWaitObject) {
            mWaitObject.notify();
        }
    }

    @OnClickAttr
    public void onCancelButtonClicked(View v) {
        if (mPaused || mCameraTexture == null) return;
        cancelHighResComputation();
    }

    @OnClickAttr
    public void onThumbnailClicked(View v) {
        if (mPaused || mThreadRunning || mCameraTexture == null
                || mThumbnail == null) return;
        gotoGallery();
    }

    //BEGIN: Added by zhanghongxing at 2013-01-09 for full preview
    @OnClickAttr
    public void onSwitchToCameraClicked(View v) {
    	switchToOtherMode(ModePicker.MODE_CAMERA);
    }

    @OnClickAttr
    public void onSwitchToVideoClicked(View v) {
    	//switchToOtherMode(ModePicker.MODE_VIDEO);
    	switchToOtherMode(ModePicker.MODE_CAMERA);
    }

    @OnClickAttr
    public void onSwitchToPanoramaClicked(View v) {
    	// switchToOtherMode(ModePicker.MODE_PANORAMA);
    }
    //END:   Added by zhanghongxing at 2012-01-09


    // This function will be called upon the first camera frame is available.
    private void reset() {
        mCaptureState = CAPTURE_STATE_VIEWFINDER;

        // We should set mGLRootView visible too. However, since there might be no
        // frame available yet, setting mGLRootView visible should be done right after
        // the first camera frame is available and therefore it is done by
        // mOnFirstFrameAvailableRunnable.
        setSwipingEnabled(true);
        mReviewLayout.setVisibility(View.GONE);
        
//        mShutterButton.setBackgroundResource(R.drawable.shutterbutton_background);
        //mShutterButton.setBackgroundResource(R.drawable.btn_shutter_pan);
        
        mPanoProgressBar.setVisibility(View.GONE);
        // When switch to Gallary that wouldn't set
        // preview-layout visible.
        if (mShowCameraAppView) {
            mCaptureLayout.setVisibility(View.VISIBLE);
        }
        mMosaicFrameProcessor.reset();
        if (mMosaicPreviewRenderer != null) {
            mMosaicPreviewRenderer.previewImageReset();
        }
    }

    private void resetToPreview() {
        reset();
        if (!mPaused) startCameraPreview();
    }

    private void showFinalMosaic(Bitmap bitmap) {
        if (bitmap != null) {
        	BitmapDrawable bitmapDrawable = (BitmapDrawable) mReview.getDrawable();
        	if(bitmapDrawable != null && bitmapDrawable.getBitmap() != null
        			&& !bitmapDrawable.getBitmap().isRecycled()) {
        		bitmapDrawable.getBitmap().recycle();
        	}	
            mReview.setImageBitmap(bitmap);
        }

        mGLRootView.setVisibility(View.GONE);
        mCaptureLayout.setVisibility(View.GONE);
        mReviewLayout.setVisibility(View.VISIBLE);
    }

    private Uri savePanorama(byte[] jpegData, int width, int height, int orientation) {
        if (jpegData != null) {
            String fileFoamt = SystemProperties.get(CameraSettings.CAMERA_MEDIA_FILE_FORMAT,"");
            if(fileFoamt == null || fileFoamt.length() < 6) {
                fileFoamt = getString(R.string.pano_file_name_format);
            } else {
                fileFoamt = "'PANO'_" +fileFoamt;
            }
            String filename = PanoUtil.createName(fileFoamt, mTimeTaken);
            //BEGIN:Deleted by wangbin at 2013-7-11.
            // Uri uri = Storage.addImage(mContentResolver, filename, mTimeTaken, null,
            //         orientation, jpegData, width, height);
            // if (uri != null) {
            //END:deleted by wangbin at 2013-7-11.
            String filepath = Storage.generateFilepath(filename);
            try {
                ExifInterface exif = new ExifInterface(filepath);

                exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,
                        mGPSDateStampFormat.format(mTimeTaken));
                exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP,
                        mGPSTimeStampFormat.format(mTimeTaken));
                exif.setAttribute(ExifInterface.TAG_DATETIME,
                        mDateTimeStampFormat.format(mTimeTaken));
                exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                        getExifOrientation(orientation));

                exif.saveAttributes();
            } catch (IOException e) {
                Log.e(TAG, "Cannot set EXIF for " + filepath, e);
            }
            // }//Deleted by wangbin at 2013-7-11.
            //Add by wangbin at 2013-7-11.
            Uri uri = Storage.panoAddImage(mContentResolver, filename, mTimeTaken, null,orientation, jpegData, width, height);
            return uri;
        }
        return null;
    }

    private static String getExifOrientation(int orientation) {
        switch (orientation) {
            case 0:
                return String.valueOf(ExifInterface.ORIENTATION_NORMAL);
            case 90:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_90);
            case 180:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_180);
            case 270:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_270);
            default:
                throw new AssertionError("invalid: " + orientation);
        }
    }

    private void clearMosaicFrameProcessorIfNeeded() {
        if (!mPaused || mThreadRunning) return;
        // Only clear the processor if it is initialized by this activity
        // instance. Other activity instances may be using it.
        if (mMosaicFrameProcessor != null) {
            mMosaicFrameProcessor.clear();
            mMosaicFrameProcessorInitialized = false;
        }
    }

    private void initMosaicFrameProcessorIfNeeded() {
        if (mPaused || mThreadRunning) return;
        mMosaicFrameProcessor.initialize(
                mPreviewWidth, mPreviewHeight, getPreviewBufSize());
        mMosaicFrameProcessorInitialized = true;
    }

    @Override
    protected void onPause() {
		if(mReviewLayout.getVisibility() == View.VISIBLE) {
           cancelHighResComputation();
		   //sleep 100ms,make sure cancel operation is finshed.
		   try {
               Thread.sleep(100);
		   } catch(Exception e) {
		   }
		}
        mPaused = true;
        mIsFromOnPause = true;
        super.onPause();

        if (mRotateDialog != null && mRotateDialog.isRotateDialogVisible()) {
            mRotateDialog.dismissDialog();
        }
        mOrientationEventListener.disable();
        // Stop the capturing first.
        if (mCaptureState == CAPTURE_STATE_MOSAIC) {
            stopCapture(true);
            reset();
        }
        stopCameraPreview();
        releaseCamera();
        mCameraTexture = null;

        // The preview renderer might not have a chance to be initialized before
        // onPause().
        if (mMosaicPreviewRenderer != null) {
        	mMosaicPreviewRenderer.previewImageReset();
            mMosaicPreviewRenderer.release();
            mMosaicPreviewRenderer = null;
        }

        if (mMosaicFrameProcessor != null) {
        	mMosaicFrameProcessor.reset();
            mMosaicFrameProcessor.clear();
            mMosaicFrameProcessorInitialized = false;
        }
        if (mWaitProcessorTask != null) {
            mWaitProcessorTask.cancel(true);
            mWaitProcessorTask = null;
        }
        resetScreenOn();
        if (mCameraSound != null) {
            mCameraSound.release();
            mCameraSound = null;
        }
        if (mCameraScreenNail != null) {
            mCameraScreenNail.releaseSurfaceTexture();
        }
        //BEGIN: Added by zhanghongxing at 2013-01-14 for DER-465
        if (mDidRegister) {
            unregisterReceiver(mReceiver);
            mDidRegister = false;
        }
        //END:   Added by zhanghongxing at 2013-01-14
        mMainHandler.removeMessages(MSG_CHECK_STORAGE);
        mMainHandler.removeMessages(DEFAULT_SWEEP_ANGLE);
        System.gc();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Drawable lowResReview = null;
        if (mThreadRunning) lowResReview = mReview.getDrawable();

        mIsLandScape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        // Change layout in response to configuration change
        mCaptureLayout.setOrientation(
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        mCaptureLayout.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(R.layout.preview_frame_pano, mCaptureLayout);
        //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
        // inflater.inflate(R.layout.camera_control, mCaptureLayout);
        //END:   Deleted by zhanghongxing at 2013-01-09

        mPanoLayout.removeView(mReviewLayout);
        inflater.inflate(R.layout.pano_review, mPanoLayout);

        setViews(getResources());
        if (mThreadRunning) {
            mReview.setImageDrawable(lowResReview);
            mCaptureLayout.setVisibility(View.GONE);
            mReviewLayout.setVisibility(View.VISIBLE);
        }

        updateThumbnailView();
    }

    @Override
    protected void onResume() {
        mPaused = false;
        super.onResume();
        mOrientationEventListener.enable();

        mCaptureState = CAPTURE_STATE_VIEWFINDER;

        SetupCameraThread setupCameraThread = new SetupCameraThread();
        setupCameraThread.start();
        try {
            setupCameraThread.join();
        } catch (InterruptedException ex) {
            // ignore
        }

        if (mOpenCameraFail) {
            Util.showErrorAndFinish(this, R.string.cannot_connect_camera);
            return;
        } else if (mCameraDisabled) {
            Util.showErrorAndFinish(this, R.string.camera_disabled);
            return;
        }

        // Set up sound playback for shutter button
        if(mCameraSound != null) {
        	mCameraSound.release();
        	mCameraSound = null;
        }
        mCameraSound = new MediaActionSound();
        mCameraSound.load(MediaActionSound.START_VIDEO_RECORDING);
        mCameraSound.load(MediaActionSound.STOP_VIDEO_RECORDING);

        // Check if another panorama instance is using the mosaic frame processor.
        mRotateDialog.dismissDialog();
        if (!mThreadRunning && mMosaicFrameProcessor.isMosaicMemoryAllocated()) {
            mGLRootView.setVisibility(View.GONE);
            mRotateDialog.showWaitingDialog(mDialogWaitingPreviousString);
            if(mWaitProcessorTask != null) {
            	mWaitProcessorTask.cancel(true);
            	mWaitProcessorTask = null;
            }
            mWaitProcessorTask = new WaitProcessorTask().execute();
        } else {
            if (!mThreadRunning) mGLRootView.setVisibility(View.VISIBLE);
            // Camera must be initialized before MosaicFrameProcessor is
            // initialized. The preview size has to be decided by camera device.
            initMosaicFrameProcessorIfNeeded();
            int w = mPreviewArea.getWidth();
            int h = mPreviewArea.getHeight();
            if (w != 0 && h != 0) {  // The layout has been calculated.
                configMosaicPreview(w, h);
            }
        }
        if (!mIsFromOnPause) getLastThumbnail();
        mIsFromOnPause = false;
        keepScreenOnAwhile();

        // Dismiss open menu if exists.
        PopupManager.getInstance(this).notifyShowPopup(null);
        mMainHandler.sendEmptyMessage(MSG_CHECK_STORAGE);
        updateCameraAppView();

        // while saving the pano picture,view items in capture layout should be invisible.
        // And only those in review layout should be visible.
        if(mThreadRunning){
            mCaptureLayout.setVisibility(View.GONE);
            mReviewLayout.setVisibility(View.VISIBLE);
        }

        //BEGIN: Added by zhanghongxing at 2013-01-14 for DER-465
        installIntentFilter();
        //END:   Added by zhanghongxing at 2013-01-14
    }

    //BEGIN: Added by zhanghongxing at 2013-01-14 for DER-465
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received intent action=" + action);
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                mMainHandler.sendEmptyMessage(MSG_CHECK_STORAGE);
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                mMainHandler.sendEmptyMessage(MSG_CHECK_STORAGE);
                getLastThumbnail();
            }
        }
    };
    
    private void installIntentFilter() {
        // install an intent filter to receive SD card related events.
        //BEGIN: Modified by xiongzhu at 2013-04-15
        if(Storage.mIsExternalStorage){
            IntentFilter intentFilter =
                    new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
            intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
            intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
            intentFilter.addDataScheme("file");
            registerReceiver(mReceiver, intentFilter);
            mDidRegister = true;
        }
        //END:   Modified by xiongzhu at 2013-04-15
    }
    //END:   Added by zhanghongxing at 2013-01-14

    // When switch to preview then set preview-layout visible
    // so that can get key from user.
    @Override
    protected void updateCameraAppView() {
        super.updateCameraAppView();
        if (mShowCameraAppView) {
            mCaptureLayout.setVisibility(View.VISIBLE);
        }
    }
    /**
     * Generate the final mosaic image.
     *
     * @param highRes flag to indicate whether we want to get a high-res version.
     * @return a MosaicJpeg with its isValid flag set to true if successful; null if the generation
     *         process is cancelled; and a MosaicJpeg with its isValid flag set to false if there
     *         is an error in generating the final mosaic.
     */
    public MosaicJpeg generateFinalMosaic(boolean highRes) {
        int mosaicReturnCode = mMosaicFrameProcessor.createMosaic(highRes);
        if (mosaicReturnCode == Mosaic.MOSAIC_RET_CANCELLED) {
            return null;
        } else if (mosaicReturnCode == Mosaic.MOSAIC_RET_ERROR) {
            return new MosaicJpeg();
        } else if (mosaicReturnCode == Mosaic.BLEND_RET_ERROR_CONSISTENCY) {
            return new MosaicJpeg(Mosaic.BLEND_RET_ERROR_CONSISTENCY);
        } else if (mosaicReturnCode == Mosaic.BLEND_RET_ERROR_MOSAIC_SIZE) {
            return new MosaicJpeg(Mosaic.BLEND_RET_ERROR_MOSAIC_SIZE);
        }

        byte[] imageData = mMosaicFrameProcessor.getFinalMosaicNV21();
        if (imageData == null) {
            Log.e(TAG, "getFinalMosaicNV21() returned null.");
            return new MosaicJpeg();
        }

        int len = imageData.length - 8;
        int width = (imageData[len + 0] << 24) + ((imageData[len + 1] & 0xFF) << 16)
                + ((imageData[len + 2] & 0xFF) << 8) + (imageData[len + 3] & 0xFF);
        int height = (imageData[len + 4] << 24) + ((imageData[len + 5] & 0xFF) << 16)
                + ((imageData[len + 6] & 0xFF) << 8) + (imageData[len + 7] & 0xFF);
        Log.v(TAG, "ImLength = " + (len) + ", W = " + width + ", H = " + height);

        if (width <= 0 || height <= 0) {
            // TODO: pop up an error message indicating that the final result is not generated.
            Log.e(TAG, "width|height <= 0!!, len = " + (len) + ", W = " + width + ", H = " +
                    height);
            return new MosaicJpeg();
        }

        YuvImage yuvimage = new YuvImage(imageData, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        try {
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "Exception in storing final mosaic", e);
            return new MosaicJpeg();
        }
        imageData = null;
        return new MosaicJpeg(out.toByteArray(), width, height);
    }

    private void startCameraPreview() {
        if (mCameraDevice == null) {
            // Camera open failed. Return.
            return;
        }

        // This works around a driver issue. startPreview may fail if
        // stopPreview/setPreviewTexture/startPreview are called several times
        // in a row. mCameraTexture can be null after pressing home during
        // mosaic generation and coming back. Preview will be started later in
        // onLayoutChange->configMosaicPreview. This also reduces the latency.
        if (mCameraTexture == null) return;

        // If we're previewing already, stop the preview first (this will blank
        // the screen).
        if (mCameraState != PREVIEW_STOPPED) stopCameraPreview();

        // Set the display orientation to 0, so that the underlying mosaic library
        // can always get undistorted mPreviewWidth x mPreviewHeight image data
        // from SurfaceTexture.
        if(mCameraOrientation == 180) {
            //for 8660 devices
            mCameraDevice.setDisplayOrientation(mCameraOrientation);
        } else {
            mCameraDevice.setDisplayOrientation(0);
        }

        if (mCameraTexture != null) mCameraTexture.setOnFrameAvailableListener(this);
        mCameraDevice.setPreviewTextureAsync(mCameraTexture);

        mCameraDevice.startPreviewAsync();
        mCameraState = PREVIEW_ACTIVE;
    }

    private void stopCameraPreview() {
        if (mCameraDevice != null && mCameraState != PREVIEW_STOPPED) {
            Log.v(TAG, "stopPreview");
            mCameraDevice.stopPreview();
        }
        mCameraState = PREVIEW_STOPPED;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (mCaptureState != CAPTURE_STATE_MOSAIC) keepScreenOnAwhile();
    }

    @Override
    public void onBackPressed() {
        // If panorama is generating low res or high res mosaic, ignore back
        // key. So the activity will not be destroyed.
        if (mThreadRunning && mAppBridge.isCurrentCameraPreview()) return;
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // if (mModePicker != null && mModePicker.dismissModeSelection()) return;
        //END:   Modified by zhanghongxing at 2013-01-09
        super.onBackPressed();
    }

    private void resetScreenOn() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMainHandler.sendEmptyMessageDelayed(MSG_CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private void keepScreenOn() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private class WaitProcessorTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            synchronized (mMosaicFrameProcessor) {
                while (!isCancelled() && mMosaicFrameProcessor.isMosaicMemoryAllocated()) {
                    try {
                        mMosaicFrameProcessor.wait();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mWaitProcessorTask = null;
            mRotateDialog.dismissDialog();
            mGLRootView.setVisibility(View.VISIBLE);
            initMosaicFrameProcessorIfNeeded();
            int w = mPreviewArea.getWidth();
            int h = mPreviewArea.getHeight();
            if (w != 0 && h != 0) {  // The layout has been calculated.
                configMosaicPreview(w, h);
            }
            resetToPreview();
        }
    }
    /**
     * According to the error type setting different message.
     *
     * @param invalid error type.
     * @return true, illustration of consistency and size check does not pass when generate the
     *         final mosaic image; else false, may be other errors from blend the mosaic.
     */
    private boolean showDetailFailMessages(int invalid) {
        Resources appRes = getResources();
        boolean showFailedMessages = true;
        switch (invalid) {
            case Mosaic.BLEND_RET_ERROR_CONSISTENCY:
            case Mosaic.BLEND_RET_ERROR_MOSAIC_SIZE:
                mDialogPanoramaFailedString = appRes.getString(R.string.pano_dialog_panorama_consistency_failed);
                break;
            default:
                showFailedMessages = false;
                break;
        }
        if (showFailedMessages) {
            mMainHandler.sendEmptyMessage(MSG_GENERATE_FINAL_MOSAIC_ERROR);
        }
        return showFailedMessages;
    }
}
