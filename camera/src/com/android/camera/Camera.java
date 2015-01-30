/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (c) 2012-2013, The Linux Foundation. All Rights Reserved.
 *
 * Not a Contribution, Apache licensenotifications and license are retained
 * for attribution purposes only.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.CameraProfile;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Window;

import com.android.camera.AutoFocusManager.AutoFocusListener;
import com.android.camera.CameraSettings;
import com.android.camera.Thumbnail;
import com.android.camera.TimerSnapManager.TimerSnapListener;
import com.android.camera.ui.CameraPicker;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.IndicatorControlContainer;
import com.android.camera.ui.SecondLevelIndicatorControlBar;
import com.android.camera.ui.PopupManager;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.RotateTextToast;
import android.os.SystemProperties;
import com.android.camera.ui.TwoStateImageView;
import com.android.camera.ui.ZoomControl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.HashMap;
import android.util.FloatMath;

import java.util.HashMap;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.graphics.Rect;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;

import android.graphics.Paint.Align;
import android.provider.Settings;
import android.view.WindowManager;
import android.R.color;
import android.annotation.TargetApi;
import android.os.Build;

/** The Camera activity which can preview and take pictures. */
public class Camera extends ActivityBase implements FocusManager.Listener,
        ModePicker.OnModeChangeListener, FaceDetectionListener,
        CameraPreference.OnPreferenceChangedListener, LocationManager.Listener,
        PreviewFrameLayout.OnSizeChangedListener,
        ShutterButton.OnShutterButtonListener ,TimerSnapManager.TimerSnapListener,
        View.OnLongClickListener {

    private static final String TAG = "camera";

    final String[] OTHER_SETTING_KEYS = {
                CameraSettings.KEY_RECORD_LOCATION,
                CameraSettings.KEY_SILENT_MODE,
                CameraSettings.KEY_CAMERA_STORAGE_PLACE, //Added by xiongzhu at 2013-04-15
                CameraSettings.KEY_TIMER_SNAP,
                CameraSettings.KEY_VOLUME_KEY_MODE,
                CameraSettings.KEY_PICTURE_SIZE,
                CameraSettings.KEY_FACE_DETECTION,
                CameraSettings.KEY_PICTURE_FORMAT,  //#TODO : Need to Decide
                CameraSettings.KEY_JPEG_QUALITY,
                CameraSettings.KEY_FOCUS_MODE,
                CameraSettings.KEY_TOUCH_AF_AEC,//Deleted for C230w, not support.
                CameraSettings.KEY_SCENE_MODE,
                CameraSettings.KEY_CAMERA_INTELLIGENCE_KEY, 
        };

    // BEGIN: Added by zhanghongxing at 2013-05-28
    final String[] OTHER_SETTING_KEYS_INTENT = {
            CameraSettings.KEY_RECORD_LOCATION,
            CameraSettings.KEY_SILENT_MODE,
            CameraSettings.KEY_PICTURE_SIZE,
            CameraSettings.KEY_FACE_DETECTION,
            CameraSettings.KEY_PICTURE_FORMAT,
            CameraSettings.KEY_JPEG_QUALITY,
            CameraSettings.KEY_FOCUS_MODE,
            CameraSettings.KEY_TOUCH_AF_AEC,
            CameraSettings.KEY_SCENE_MODE,
            CameraSettings.KEY_CAMERA_INTELLIGENCE_KEY,
            
    };
    // END:   Added by zhanghongxing at 2013-05-28

    final String[] QCOM_SETTING_KEYS = {
            CameraSettings.KEY_EXPOSURE,
            CameraSettings.KEY_WHITE_BALANCE,
            CameraSettings.KEY_ANTIBANDING,
            //CameraSettings.KEY_SELECTABLE_ZONE_AF,
            CameraSettings.KEY_SATURATION,
            CameraSettings.KEY_CONTRAST,
            CameraSettings.KEY_SHARPNESS
            //CameraSettings.KEY_AE_BRACKET_HDR
        };

    final String[] QCOM_SETTING_KEYS_1 = {
                CameraSettings.KEY_ISO,
                CameraSettings.KEY_AUTOEXPOSURE,
                CameraSettings.KEY_REDEYE_REDUCTION,
                CameraSettings.KEY_DENOISE,
                CameraSettings.KEY_LENSSHADING,
//                CameraSettings.KEY_HISTOGRAM
              //only for test
//                CameraSettings.KEY_VERSION_NUMBER,
        };

    //QCom data members
    public static boolean mBrightnessVisible = true;
    //Deleted by zhanghongxing at 2013-03-25
    // public HashMap otherSettingKeys = new HashMap(3);
    private static final int MAX_SHARPNESS_LEVEL = 6;
    private boolean mRestartPreview = false;
    private int mSnapshotMode;
    private int mBurstSnapNum = 1;
    private int mReceivedSnapNum = 0;
    public boolean mFaceDetectionEnabled = false;

    /*Histogram variables*/
    private GraphView mGraphView;
    private static final int STATS_DATA = 257;
    public static int statsdata[] = new int[STATS_DATA];
    public static boolean mHiston = false;

    //End Of Qcom data Members

    // We number the request code from 1000 to avoid collision with Gallery.
    private static final int REQUEST_CROP = 1000;
    public static final int RESTOREGPS_STATE = 1001;

    private static final int FIRST_TIME_INIT = 2;
    private static final int CLEAR_SCREEN_DELAY = 3;
    private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 4;
    private static final int CHECK_DISPLAY_ROTATION = 5;
    private static final int SHOW_TAP_TO_FOCUS_TOAST = 6;
    private static final int UPDATE_THUMBNAIL = 7;
    private static final int SWITCH_CAMERA = 8;
    private static final int SWITCH_CAMERA_START_ANIMATION = 9;
    private static final int CAMERA_OPEN_DONE = 10;
    private static final int START_PREVIEW_DONE = 11;
    private static final int OPEN_CAMERA_FAIL = 12;
    private static final int CAMERA_DISABLED = 13;
    private static final int SET_SKIN_TONE_FACTOR = 14;
    private static final int SWITCH_CAMERA_ANIMATION_DONE = 15; 

    //Added by xiongzhu at 2013-04-15
    private static final int CHANGING_STORAGE_STATE=16;
    private static final int HIDE_ZOOM_BAR = 17;
    private static final int SHOW_ZOOM_BAR = 18;
    private static final int SAVE_CACHED_IMAGE = 19;
    private final int DISMISS_SAVE_PROGRESS_BAR = 20;
    private static final int HIDE_ZOOM_BAR_DELAY = 4 * 1000;

    private static final int SET_SKIN_TONE_FACTOR_DELAY = 500; // Delay for sending msg of setting tone factor

    // The subset of parameters we need to update in setCameraParameters().
    private static final int UPDATE_PARAM_INITIALIZE = 1;
    private static final int UPDATE_PARAM_ZOOM = 2;
    private static final int UPDATE_PARAM_PREFERENCE = 4;
    private static final int UPDATE_PARAM_ALL = -1;

    //  When setCameraParametersWhenIdle() is called, we accumulate the subsets
    // needed to be updated in mUpdateSet.
    private int mUpdateSet;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    private int mZoomValue;  // The current zoom value.
    private int mZoomMax;
    private ZoomControl mZoomControl;

    private Parameters mInitialParams;
    private boolean mFocusAreaSupported;
    private boolean mMeteringAreaSupported;
    private boolean mAeLockSupported;
    private boolean mAwbLockSupported;
    private boolean mContinousFocusSupported;
    private boolean mTouchAfAecFlag;
    private boolean mPostCaptureAlertFlag;
    private boolean mZSLandHDRFlag;
    private boolean mCEffAndHDRFlag;

    private MyOrientationEventListener mOrientationListener;
    // The degrees of the device rotated clockwise from its natural orientation.
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    // The orientation compensation for icons and thumbnails. Ex: if the value
    // is 90, the UI components should be rotated 90 degrees counter-clockwise.
    private int mOrientationCompensation = 0;
    private ComboPreferences mPreferences;

    private static final String sTempCropFilename = "crop-temp";

    //Add by wangbin at 2013-03-22
    private String mSilentMode;
    private boolean mIsSilentMode = false;
    private String mTimerSnap;
    private TimerSnapManager mTimerSnapManager;
    private String mVolumeKeyMode;

    private ContentProviderClient mMediaProviderClient;
    private ShutterButton mShutterButton;
    private boolean mFaceDetectionStarted = false;

    private PreviewFrameLayout mPreviewFrameLayout;
    private SurfaceTexture mSurfaceTexture;
    private RotateDialogController mRotateDialog;

    private static final int MINIMUM_BRIGHTNESS = 0;
    private static final int MAXIMUM_BRIGHTNESS = 6;
    private int mbrightness = 3;
    private int mbrightness_step = 1;
    private ProgressBar brightnessProgressBar;
    //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
    // private ModePicker mModePicker;
    private RotateImageView mSwitchToCamera;
    private RotateImageView mSwitchToVideo;
    //private RotateImageView mSwitchToPanorama; delete by xiongzhu for cbb
    private RotateImageView mThumbnailWindow;
    //END:   Modified by zhanghongxing at 2013-01-09
    private FaceView mFaceView;
    private RotateLayout mFocusAreaIndicator;
    private Rotatable mReviewCancelButton;
    private Rotatable mReviewDoneButton;
    private View mReviewRetakeButton;
    private RotateTextToast mRotateTextToast;
    // Constant from android.hardware.Camera.Parameters
    private static final String KEY_PICTURE_FORMAT = "picture-format";
    private static final String PIXEL_FORMAT_JPEG = "jpeg";
    private static final String PIXEL_FORMAT_RAW = "raw";

    // mCropValue and mSaveUri are used only if isImageCaptureIntent() is true.
    private String mCropValue;
    private Uri mSaveUri;

    // Small indicators which show the camera settings in the viewfinder.
    private TextView mExposureIndicator;
    private ImageView mGpsIndicator;
    private ImageView mFlashIndicator;
    private ImageView mSceneIndicator;
    private ImageView mWhiteBalanceIndicator;
    private ImageView mFocusIndicator;
    // A view group that contains all the small indicators.
    private Rotatable mOnScreenIndicators;

    // We use a thread in ImageSaver to do the work of saving images and
    // generating thumbnails. This reduces the shot-to-shot time.
    private ImageSaver mImageSaver;
    // Similarly, we use a thread to generate the name of the picture and insert
    // it into MediaStore while picture taking is still in progress.
    private ImageNamer mImageNamer;

    private MediaActionSound mCameraSound;
    
    private String mFlashModeBeforeContinueTakePic;
    
    private final int CONTINUE_TAKE_PICTURE_COUNT = 20;
    
    private boolean mStorageFoceChanged = false;

    private Runnable mDoSnapRunnable = new Runnable() {
        @Override
        public void run() {
            onShutterButtonClick();
        }
    };

    private final int PREVIEW_READY_DELAY = 1300;
    private final Runnable mPreviewReadyRunnable = new Runnable() {
        @Override
        public void run() {
            if(mCameraState != IDLE && mCameraState != FOCUSING) return;
            if(mShutterButton != null) {
                 mShutterButton.setEnabled(true);//add for ARD-1105
            }
            enableCameraControls(true);
        }
    };

    private final StringBuilder mBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mBuilder);
    private final Object[] mFormatterArgs = new Object[1];

    /**
     * An unpublished intent flag requesting to return as soon as capturing
     * is completed.
     *
     * TODO: consider publishing by moving into MediaStore.
     */
    private static final String EXTRA_QUICK_CAPTURE =
            "android.intent.extra.quickCapture";

    // The display rotation in degrees. This is only valid when mCameraState is
    // not PREVIEW_STOPPED.
    private int mDisplayRotation;
    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mCameraDisplayOrientation;
    // The value for UI components like indicators.
    private int mDisplayOrientation;
    // The value for android.hardware.Camera.Parameters.setRotation.
    private int mJpegRotation;
    private boolean mFirstTimeInitialized;
    private boolean mIsImageCaptureIntent;

    private static final int PREVIEW_STOPPED = 0;
    private static final int IDLE = 1;  // preview is active
    // Focus is in progress. The exact focus state is in Focus.java.
    private static final int FOCUSING = 2;
    private static final int SNAPSHOT_IN_PROGRESS = 3;
    // Switching between cameras.
    private static final int SWITCHING_CAMERA = 4;
    private static final int TIMER_SNAP_IN_PROGRESS = 5;
    private int mCameraState = PREVIEW_STOPPED;
    private boolean mSnapshotOnIdle = false;

    private ContentResolver mContentResolver;
    private boolean mDidRegister = false;

    private LocationManager mLocationManager;

    private final ShutterCallback mShutterCallback = new ShutterCallback();
    private final PostViewPictureCallback mPostViewPictureCallback =
            new PostViewPictureCallback();
    private final RawPictureCallback mRawPictureCallback =
            new RawPictureCallback();
    private final AutoFocusCallback mAutoFocusCallback =
            new AutoFocusCallback();
    private final AutoFocusMoveCallback mAutoFocusMoveCallback =
            new AutoFocusMoveCallback();
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();
    private final StatsCallback mStatsCallback = new StatsCallback();
    private long mFocusStartTime;
    private long mShutterCallbackTime;
    public long mShutterLag;
    private long mPostViewPictureCallbackTime;
    private long mRawPictureCallbackTime;
    private long mJpegPictureCallbackTime;
    private long mOnResumeTime;
    private long mStorageSpace;
    private byte[] mJpegImageData;

    // These latency time are for the CameraLatency test.
    public long mAutoFocusTime;
    public long mShutterToPictureDisplayedTime;
    public long mPictureDisplayedToJpegCallbackTime;
    public long mJpegCallbackFinishTime;
    public long mCaptureStartTime;

    // This handles everything about focus.
    private FocusManager mFocusManager;
    private String mSceneMode;
    private Toast mNotSelectableToast;

    private final Handler mHandler = new MainHandler();
    private IndicatorControlContainer mIndicatorControlContainer;
    private SecondLevelIndicatorControlBar mSecondLevelIndicatorControlBar;
    private PreferenceGroup mPreferenceGroup;

    private boolean mQuickCapture;

    private static final int MIN_SCE_FACTOR = -10;
    private static final int MAX_SCE_FACTOR = +10;
    private int SCE_FACTOR_STEP = 10;
    private int mskinToneValue = 0;
    private boolean mSkinToneSeekBar= false;
    private boolean mSeekBarInitialized = false;
    private SeekBar skinToneSeekBar;
    private TextView LeftValue;
    private TextView RightValue;
    private TextView Title;
    
    // BEGIN: Added by zhanghongxing at 2013-06-22
    private AutoFocusManager mAutoFocusManager;
    private boolean mNeedShutterFocus;
    private boolean mNeedAutoFocus;
    private boolean mIsGravitySensorWorking;
    
    // Added by zhanghongxing at 2013-07-04
    private boolean mIsSetFlashModeOff;
    
    private boolean canDoVolumeSnap = false;
    //added by zhangzw at 2013.08.27
    private enum ContinueShutterState {
        NONE,
        SHUTTING,
        SHUTTINGFINISH,
        SAVING,
        SAVINGFINISH
    };
    private ContinueShutterState continueTakePicState = ContinueShutterState.NONE;
    private boolean mContinueShutterStarted;
    private int mContinueTakePicNum = 0;
    private Thread mContinueTakePicThread = null;
    private boolean mNeedToRestoreToNormalPicMode = false;
    private ArrayList<SaveRequest> mCacheQueue;
    private int mCacheQueueSize = 0;
    private int mSavedQueueSize = 0;
    private boolean mIsLowPowerMode = false;
    private boolean mNeedRestorePicSize = false;
    private Size mOldPicSize;
    private String mOldPictureMode;
    
    private RelativeLayout mSaveProgressBarLayout;
    
    private boolean mIsNeedUpdateSceneUI = false;
    
    //for zoom
    private float mOldDist;
    private boolean mIsZoomMode = false;
    private final int mStep = 5;

    private boolean mIsLastFocusedSuc;
    // Added by zhanghongxing at 2013-07-24
    private final SilentAutoFocusCallback mSilentAutoFocusCallback =
            new SilentAutoFocusCallback();
    
    private boolean mIsCancelAF;//Add by wangbin at 2013-7-2 for IWB-205.
    
    private String mCurIntelligenceValue;
    
    private Size mPictureSizeBeforeContinueTakePic;
    
    private boolean mNeedReloadPreference = false;
    
    private int mLastOrientation = Configuration.ORIENTATION_PORTRAIT;

    private AutoFocusListener mAutoFocusListener = new AutoFocusListener() {
        @Override
        public boolean isWorking() {
            if (mCameraState != PREVIEW_STOPPED
                    && mShutterButton != null
                    && mShutterButton.getVisibility() == View.VISIBLE
                    && mShowCameraAppView
                    && mFocusManager != null
                    && !Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(mFocusManager.getFocusMode(false))
                    && !Parameters.FOCUS_MODE_INFINITY.equals(mFocusManager.getFocusMode(false))) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onDeviceBecomeStable() {
            if (!mFocusAreaSupported && !mMeteringAreaSupported) return;
            
            //begin fix bug WEN-330 
            boolean popupShown = false;
            
            if (mSecondLevelIndicatorControlBar != null) {
                View popup = mSecondLevelIndicatorControlBar
                        .getActiveSettingPopup();
                if (popup != null && popup.isShown()) {
                    popupShown = true;
                    mNeedAutoFocus = true;
                } else {
                    mNeedAutoFocus = false;
                }
            }
            // end
            
            if (mCameraState != IDLE
                    || mFocusManager == null
                    || isFrontCamera()
                    || popupShown
                    || !Parameters.FOCUS_MODE_AUTO.equals(mFocusManager.getFocusMode(false))) {
                return;
            }
            
            String flashMode = null;
            if (mPreferences != null && mParameters != null) {

                flashMode = mPreferences.getString(
                        CameraSettings.KEY_FLASH_MODE,
                        getString(R.string.pref_camera_flashmode_default));
                if(mIsLowPowerMode) {
                    flashMode = Parameters.FLASH_MODE_OFF;
                }
                String takePictureMode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                        getString(R.string.pref_camera_takepicture_mode_default));
                
                if (getString(R.string.pref_camera_takepicture_mode_value_hdr).equals(takePictureMode)
                            && !Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                    mIsSetFlashModeOff = true;
                    mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                    mCameraDevice.setParameters(mParameters);
                }
            }
            
            if (!popupShown) {
                mNeedAutoFocus = false;
                mFocusManager.onDeviceBecomeStable();
            } else {
                mNeedAutoFocus = true;
            }
            
            // BEGIN: Added by zhanghongxing at 2013-07-04 for WXY-325
            //BEGIN:Modified by wangbin at 2013-7-15 for ARD-537.
            // if (mIsSetFlashModeOff && flashMode != null) {
            if (mIsSetFlashModeOff && flashMode != null && Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
            //END:Modified by wangbin at 2013-7-15 for ARD-537.
                mParameters.setFlashMode(flashMode);
                mCameraDevice.setParameters(mParameters);
            }
            // END:   Added by zhanghongxing at 2013-07-04
        }

        @Override
        public void onDeviceBeginMoving() {
            if (!Parameters.FOCUS_MODE_AUTO.equals(mFocusManager.getFocusMode(false))) {
                mNeedShutterFocus = true;
            }
        }

        @Override
        public void onDeviceKeepMoving(double speed) {
            if (mFocusManager == null) return;
            
            mFocusManager.onDeviceKeepMoving(speed);
        }

        @Override
        public void onDeviceKeepStable() {
            if (mNeedAutoFocus) {
                mNeedAutoFocus = false;
                onDeviceBecomeStable();
            }
        }
    };
    // END:   Added by zhanghongxing at 2013-06-22

    CameraStartUpThread mCameraStartUpThread;
    ConditionVariable mStartPreviewPrerequisiteReady = new ConditionVariable();

    // The purpose is not to block the main thread in onCreate and onResume.
    private class CameraStartUpThread extends Thread {
        private volatile boolean mCancelled;

        public void cancel() {
            mCancelled = true;
        }

        @Override
        public void run() {
            try {
                // We need to check whether the activity is paused before long
                // operations to ensure that onPause() can be done ASAP.
                if (mCancelled) return;
                mCameraDevice = Util.openCamera(Camera.this, mCameraId);
                mParameters = mCameraDevice.getParameters();
                
                if(mParameters != null) {
                    mIsZoomSupported = mParameters.isZoomSupported();
                }
                // Wait until all the initialization needed by startPreview are
                // done.
                if (mCancelled) return;
                mStartPreviewPrerequisiteReady.block();

                initializeCapabilities();
                if (mFocusManager == null) initializeFocusManager();
                if (mCancelled) return;
                setCameraParameters(UPDATE_PARAM_ALL);
                mRestartPreview = false;
                mHandler.sendEmptyMessage(CAMERA_OPEN_DONE);
                if (mCancelled) return;
                startPreview();
                mHandler.sendEmptyMessage(START_PREVIEW_DONE);
                mOnResumeTime = SystemClock.uptimeMillis();
                mHandler.sendEmptyMessage(CHECK_DISPLAY_ROTATION);
            } catch (CameraHardwareException e) {
                if(!mHandler.hasMessages(OPEN_CAMERA_FAIL)) {
                    mOpenCameraFail = true;
                    mHandler.sendEmptyMessage(OPEN_CAMERA_FAIL);
                }
            } catch (CameraDisabledException e) {
                if(!mHandler.hasMessages(CAMERA_DISABLED)) {
                    mHandler.sendEmptyMessage(CAMERA_DISABLED);
                }
            }
        }
    }

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CLEAR_SCREEN_DELAY: {
                    getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }

                case FIRST_TIME_INIT: {
                    initializeFirstTime();
                    break;
                }

                case SET_CAMERA_PARAMETERS_WHEN_IDLE: {
                    setCameraParametersWhenIdle(0);
                    break;
                }

                case CHECK_DISPLAY_ROTATION: {
                    // Set the display orientation if display rotation has changed.
                    // Sometimes this happens when the device is held upside
                    // down and camera app is opened. Rotation animation will
                    // take some time and the rotation value we have got may be
                    // wrong. Framework does not have a callback for this now.
                    if (Util.getDisplayRotation(Camera.this) != mDisplayRotation) {
                        setDisplayOrientation();
                    }
                    if (SystemClock.uptimeMillis() - mOnResumeTime < 5000) {
                        mHandler.sendEmptyMessageDelayed(CHECK_DISPLAY_ROTATION, 100);
                    }
                    break;
                }

                case SHOW_TAP_TO_FOCUS_TOAST: {
                    showTapToFocusToast();
                    break;
                }

                case UPDATE_THUMBNAIL: {
                    mImageSaver.updateThumbnail();
                    break;
                }

                case SWITCH_CAMERA: {
                    switchCamera();
                    break;
                }

                case SWITCH_CAMERA_START_ANIMATION: {
                    mCameraScreenNail.animateSwitchCamera();
                    // Delay 500ms,let the animation be shown completly.
                    mHandler.sendEmptyMessageDelayed(SWITCH_CAMERA_ANIMATION_DONE, 500);
                    break;
                }

                case CAMERA_OPEN_DONE: {
                    initializeAfterCameraOpen();
                    break;
                }

                case START_PREVIEW_DONE: {
                    mCameraStartUpThread = null;
                    setCameraState(IDLE);
                    startFaceDetection();
                    break;
                }

                case OPEN_CAMERA_FAIL: {
                    Log.i(TAG,"==zzw:case OPEN_CAMERA_FAIL");
                    mCameraStartUpThread = null;
                    mOpenCameraFail = true;
                    Util.showErrorAndFinish(Camera.this,
                            R.string.cannot_connect_camera);
                    break;
                }

                case CAMERA_DISABLED: {
                    mCameraStartUpThread = null;
                    mCameraDisabled = true;
                    Util.showErrorAndFinish(Camera.this,
                            R.string.camera_disabled);
                    break;
                }
                case SET_SKIN_TONE_FACTOR: {
                    Log.e(TAG, "yyan set tone bar: mSceneMode = " + mSceneMode);
                    // skin tone ie enabled only for auto,party and portrait BSM
                    // when color effects are not enabled
                    String colorEffect = mPreferences.getString(
                        CameraSettings.KEY_COLOR_EFFECT,
                        getString(R.string.pref_camera_coloreffect_default));
                    if((Parameters.SCENE_MODE_PARTY.equals(mSceneMode) ||
                        Parameters.SCENE_MODE_PORTRAIT.equals(mSceneMode))&&
                        (Parameters.EFFECT_NONE.equals(colorEffect) &&
                         !Parameters.SCENE_MODE_HDR.equals(mParameters.getSceneMode()))) {
                        mSeekBarInitialized = true;
                        setSkinToneFactor();
                    }
                    else{
                        Log.e(TAG, "yyan Skin tone bar: disable");
                        disableSkinToneSeekBar();
                    }
                    break;
                }

                case SWITCH_CAMERA_ANIMATION_DONE: {
                    // Enable the switch picker.
//                    mCameraPicker.setClickable(true);
                    break;

                }
                case HIDE_ZOOM_BAR:
//                    if (!isFrontCamera()) {
                        mZoomControl.setVisibility(View.INVISIBLE);
//                    }
                    break;
                case SHOW_ZOOM_BAR:
//                    if (!isFrontCamera()) {
                        mZoomControl.setVisibility(View.VISIBLE);
//                    }
                    break;

                case SAVE_CACHED_IMAGE:
                    if(mTimerSnapManager != null) {
                        mTimerSnapManager.clearTimerAnima();
                        mTimerSnapManager.hideContinueSnapNum();
                    }
                    if(mSaveProgressBarLayout.getVisibility() != View.VISIBLE) {
                        mSaveProgressBarLayout.setVisibility(View.VISIBLE);
                    }
                  Size pictureSize = mParameters.getPictureSize();
                  if(mPictureSizeBeforeContinueTakePic.width > pictureSize.width) {
                      mParameters.setPictureSize(mPictureSizeBeforeContinueTakePic.width, mPictureSizeBeforeContinueTakePic.height);
                  }
                    if(mContinueTakePicNum == CONTINUE_TAKE_PICTURE_COUNT && mCacheQueueSize < mContinueTakePicNum) {
                        new Thread() {
                            @Override
                            public void run() {
                                while(mCacheQueueSize < mContinueTakePicNum) {
                                    try {
                                        Thread.sleep(200);
                                    } catch(Exception e) {
                                    }
                                }

                                if(mCacheQueueSize == mContinueTakePicNum && !mPaused) {
                                    mCameraDevice.getCamera().setLongshot(false);
                                    saveCachedImageToQueue();
                                    startPreview();
                                    startFaceDetection();
                                }
                                mHandler.sendEmptyMessage(DISMISS_SAVE_PROGRESS_BAR);
                            }
                        }.start();
                    } else {
                        if(!mPaused) {
                            mCameraDevice.getCamera().setLongshot(false);
                        } else {
                            mSaveProgressBarLayout.setVisibility(View.GONE);
                            continueTakePicState = ContinueShutterState.NONE;
                            return;
                        }
                        if(mCacheQueue.size() > 0) {
                            saveCachedImageToQueue();
                        }
                        startPreview();
                        setCameraState(IDLE);
                        startFaceDetection();
                        mHandler.sendEmptyMessage(DISMISS_SAVE_PROGRESS_BAR);
                    }
                    break;
                case DISMISS_SAVE_PROGRESS_BAR:
                    if(mPaused) {
                        mSaveProgressBarLayout.setVisibility(View.GONE);
                        continueTakePicState = ContinueShutterState.NONE;
                        return;
                    }
                    mCacheQueueSize = 0;
                    List<String> supportedFlash = mParameters.getSupportedFlashModes();
                    if (isSupported(mFlashModeBeforeContinueTakePic, supportedFlash)) {
                        String curFlashMode = mParameters.getFlashMode();
                        if(!curFlashMode.equals(mFlashModeBeforeContinueTakePic)) {
                            mParameters.setFlashMode(mFlashModeBeforeContinueTakePic);
                        }
                    }
                    mSaveProgressBarLayout.setVisibility(View.GONE);
                    continueTakePicState = ContinueShutterState.NONE;
                    setCameraState(IDLE);
                    
                    String mode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE, 
                            getString(R.string.pref_camera_takepicture_mode_default));
                    
                    mParameters.set("snapshot-burst-num","1");
                    break;
            }
        }
    }

    private void initializeAfterCameraOpen() {
        // These depend on camera parameters.
        setPreviewFrameLayoutAspectRatio();
        mFocusManager.setPreviewSize(mPreviewFrameLayout.getWidth(),
                mPreviewFrameLayout.getHeight());
        Log.v(TAG, "in initializeAfterCameraOpen, setting focus manager preview size to "
              + mPreviewFrameLayout.getWidth() + " x " + mPreviewFrameLayout.getHeight());
        if (mIndicatorControlContainer == null) {
           initializeIndicatorControl();
        }
        
        // This should be enabled after preview is started.
        mIndicatorControlContainer.setEnabled(false);
        mSecondLevelIndicatorControlBar.setEnabled(false);
        initializeZoom();
        //updateOnScreenIndicators();
        showTapToFocusToastIfNeeded();
        //for continue take picture mode
        if (mOldPictureMode != null) {
            if(mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_continue))) {
                ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_REDEYE_REDUCTION);
                if(pref != null) pref.setEnable(false);
                pref = mPreferenceGroup.findPreference(CameraSettings.KEY_DENOISE);
                if(pref != null) pref.setEnable(false);
            } else if(mCameraId != CameraInfo.CAMERA_FACING_FRONT &&
                    mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_zsl))) {
                ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_FLASH_MODE);
                if(pref != null) ((IconListPreference) pref).setAvailable(false);
            }
        }
    }

    private void resetExposureCompensation() {
        String value = mPreferences.getString(CameraSettings.KEY_EXPOSURE,
                CameraSettings.EXPOSURE_DEFAULT_VALUE);
        if (!CameraSettings.EXPOSURE_DEFAULT_VALUE.equals(value)) {
            Editor editor = mPreferences.edit();
            editor.putString(CameraSettings.KEY_EXPOSURE, "0");
            editor.apply();
        }
    }

    private void keepMediaProviderInstance() {
        // We want to keep a reference to MediaProvider in camera's lifecycle.
        // TODO: Utilize mMediaProviderClient instance to replace
        // ContentResolver calls.
        if (mMediaProviderClient == null) {
            mMediaProviderClient = mContentResolver
                    .acquireContentProviderClient(MediaStore.AUTHORITY);
        }
    }

    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
        if (mFirstTimeInitialized) return;

        // Create orientation listener. This should be done first because it
        // takes some time to get first orientation.
        mOrientationListener = new MyOrientationEventListener(Camera.this);
        mOrientationListener.enable();

        // Initialize location service.
//        boolean recordLocation = RecordLocationPreference.get(
//                mPreferences, mContentResolver);
//        mIsRecordLocation = recordLocation;
//        mLocationManager.recordLocation(recordLocation);
        
        keepMediaProviderInstance();
        // checkStorage(); //Deleted by xiongzhu at 2013-04-15

        // Initialize shutter button.
        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setEnabled(false);
        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.setVisibility(View.VISIBLE);
        mShutterButton.setOnLongClickListener(this);

        mImageSaver = new ImageSaver();
        mImageNamer = new ImageNamer();

        mGraphView = (GraphView)findViewById(R.id.graph_view);
        mGraphView.setCameraObject(Camera.this);

        mFirstTimeInitialized = true;
        addIdleHandler();

        mTimerSnapManager = new TimerSnapManager(this);
        mCacheQueue = new ArrayList<SaveRequest>();
    }

    @Override
    public boolean onLongClick(View view) {
        if(continueTakePicState == ContinueShutterState.NONE
                && !isImageCaptureIntent()) {
            if(mPaused) return true;
            startToTakePictureContinue();
        }
        return false;
    }
    
    private void startToTakePictureContinue() {
        if(mOldPictureMode == null || !mOldPictureMode.equals(
                getString(R.string.pref_camera_takepicture_mode_value_continue))
                || !checkAndHandleAvailableSpace()) return;
        mContinueTakePicNum = 0;
        mSavedQueueSize = 0;
        mContinueShutterStarted = true;
        enableCameraControls(false);
        continueTakePicState = ContinueShutterState.SHUTTING;
        
        mPictureSizeBeforeContinueTakePic = mParameters.getPictureSize();
        if(mPictureSizeBeforeContinueTakePic.width > 2592 || mPictureSizeBeforeContinueTakePic.height > 1944) {
            mParameters.setPictureSize(2592, 1944);
        }
        mParameters.set("snapshot-burst-num",""+CONTINUE_TAKE_PICTURE_COUNT);
        mCameraDevice.getCamera().setLongshot(true);
        
        mFlashModeBeforeContinueTakePic = mParameters.getFlashMode();
        if(!Parameters.FLASH_MODE_OFF.equals(mFlashModeBeforeContinueTakePic)) {
            mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        }
        capture();
    }
   
    private void showTapToFocusToastIfNeeded() {
        // Show the tap to focus toast if this is the first start.
        if (mFocusAreaSupported &&
                mPreferences.getBoolean(CameraSettings.KEY_CAMERA_FIRST_USE_HINT_SHOWN, true)) {
            //Check the touch mode before we show the toast,
            //only show it when touch AF AEC mode is touch-on.
            //according to qcomarrays.xml,the string "touch-on" and "touch-off"
            //will not be localized.
            String touchAfAec = mPreferences.getString(
                 CameraSettings.KEY_TOUCH_AF_AEC,
                 getString(R.string.pref_camera_touchafaec_default));
            if ("touch-on".equals(touchAfAec)) {
                // Delay the toast for one second to wait for orientation.
                mHandler.sendEmptyMessageDelayed(SHOW_TAP_TO_FOCUS_TOAST, 1000);
            }
        }
    }

    private void addIdleHandler() {
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                Storage.ensureOSXCompatible();
                return false;
            }
        });
    }

    // If the activity is paused and resumed, this method will be called in
    // onResume.
    private void initializeSecondTime() {
        // Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
        mOrientationListener.enable();

        // installIntentFilter(); //Deleted by xiongzhu at 2013-04-15
        mImageSaver = new ImageSaver();
        mImageNamer = new ImageNamer();
        initializeZoom();
        keepMediaProviderInstance();
        // checkStorage(); //Deleted by xiongzhu at 2013-04-15
        hidePostCaptureAlert();

        if(mGraphView != null)
          mGraphView.setCameraObject(Camera.this);

        if (!mIsImageCaptureIntent) {
            //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
            // mModePicker.setCurrentMode(ModePicker.MODE_CAMERA);
            mSwitchToCamera.setImageResource(R.drawable.ic_switch_camera_focused);
//            mSwitchToVideo.setImageResource(R.drawable.ic_switch_video_dark);
            //mSwitchToPanorama.setImageResource(R.drawable.ic_switch_pan_dark); delete by xiongzhu for cbb
            //END:   Modified by zhanghongxing at 2013-01-09
        }
        //if (mIndicatorControlContainer != null) {
        //    mIndicatorControlContainer.reloadPreferences();
        //}
        
        if (mSecondLevelIndicatorControlBar != null) {
            mSecondLevelIndicatorControlBar.reloadPreferences();
        }
        
    }

    private class ZoomChangeListener implements ZoomControl.OnZoomChangedListener {
        @Override
        public void onZoomValueChanged(int index) {
            // Not useful to change zoom value when the activity is paused.
            if (mPaused) return;
            if (!mIsZoomMode && mHandler.hasMessages(HIDE_ZOOM_BAR)) {
                mHandler.removeMessages(HIDE_ZOOM_BAR);
                mHandler.sendEmptyMessageDelayed(HIDE_ZOOM_BAR,HIDE_ZOOM_BAR_DELAY);
            }
            mZoomValue = index;

            // Set zoom parameters asynchronously
            mParameters.setZoom(mZoomValue);
            mCameraDevice.setParametersAsync(mParameters);
        }
    }

    private void initializeZoom() {
        if (!mIsZoomSupported) return;
        mZoomMax = mParameters.getMaxZoom();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        mZoomControl.setZoomMax(mZoomMax);
        mZoomControl.setZoomIndex(mParameters.getZoom());
        mZoomControl.setOnZoomChangeListener(new ZoomChangeListener());
        
        // BEGIN: Added by zhanghongxing at 2013-05-02
//        if (isFrontCamera()) {
            mZoomControl.setVisibility(View.INVISIBLE);
//        } else {
//            mZoomControl.setVisibility(View.VISIBLE);
//        }
        // END:   Added by zhanghongxing at 2013-05-02
    }

    @Override
    public void startFaceDetection() {
        if (mFaceDetectionEnabled == false
                || mFaceDetectionStarted || mCameraState != IDLE) return;
        if (mParameters.getMaxNumDetectedFaces() > 0) {
            mFaceDetectionStarted = true;
            mFaceView.clear();
            mFaceView.setVisibility(View.VISIBLE);
            mFaceView.setDisplayOrientation(mDisplayOrientation);
            CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
            mFaceView.setMirror(info.facing == CameraInfo.CAMERA_FACING_FRONT);
            mFaceView.resume();
            mFocusManager.setFaceView(mFaceView);
            mCameraDevice.setFaceDetectionListener(this);
            mCameraDevice.startFaceDetection();
        }
    }

    @Override
    public void stopFaceDetection() {
        if (mFaceDetectionEnabled == false || !mFaceDetectionStarted)
            return;
        if (mParameters.getMaxNumDetectedFaces() > 0) {
            mFaceDetectionStarted = false;
            mCameraDevice.setFaceDetectionListener(null);
            mCameraDevice.stopFaceDetection();
            if (mFaceView != null)
            {
              mFaceView.clear();
              mFaceView.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {

        //BEGIN: Modified by xiongzhu at 2013-04-15
        // if (mCameraState == SWITCHING_CAMERA) return true;
        if (mCameraState == SWITCHING_CAMERA || mCameraState == CHANGING_STORAGE_STATE) return true;
        //END:   Modified by xiongzhu at 2013-04-15

        // Check if the popup window should be dismissed first.
        if (m.getAction() == MotionEvent.ACTION_DOWN) {
            float x = m.getX();
            float y = m.getY();
            // Dismiss the mode selection window if the ACTION_DOWN event is out
            // of its view area.
            //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
            // if ((mModePicker != null) && !Util.pointInView(x, y, mModePicker)) {
            //     mModePicker.dismissModeSelection();
            // }
            //END:   Deleted by zhanghongxing at 2013-01-09
            // Check if the popup window is visible. Indicator control can be
            // null if camera is not opened yet.
            //if (mIndicatorControlContainer != null) {
            //    View popup = mIndicatorControlContainer.getActiveSettingPopup();
            //    if (popup != null) {
            //        // Let popup window, indicator control or preview frame
           //         // handle the event by themselves. Dismiss the popup window
           //         // if users touch on other areas.
           //         if (!Util.pointInView(x, y, popup)
           //                 && !Util.pointInView(x, y, mIndicatorControlContainer)
           //                 && !Util.pointInView(x, y, mPreviewFrameLayout)) {
           //             mIndicatorControlContainer.dismissSettingPopup();
           //        }
           //     }
           // }

            if (mHandler.hasMessages(HIDE_ZOOM_BAR) && !Util.pointInView(x, y, mZoomControl)) {
                mHandler.removeMessages(HIDE_ZOOM_BAR);
                mZoomControl.setVisibility(View.INVISIBLE);
            }
            
            if (mSecondLevelIndicatorControlBar != null) {
                View popup = mSecondLevelIndicatorControlBar.getActiveSettingPopup();
                if (popup != null) {
                    // Let popup window, indicator control or preview frame
                    // handle the event by themselves. Dismiss the popup window
                    // if users touch on other areas.
                    
                    View subPopup = mSecondLevelIndicatorControlBar.getActiveSubSettingPopup();
                     if (!Util.pointInView(x, y, mSecondLevelIndicatorControlBar)) {
                         if(subPopup != null) {
                             if (!Util.pointInView(x, y, subPopup)) {
                                 mSecondLevelIndicatorControlBar.dismissSettingPopup();
                                 return true;
                             }
                         } else if(popup.getVisibility() == View.VISIBLE){
                             if (!Util.pointInView(x, y, popup) && !mRotateDialog.isRotateDialogVisible()) {
                                 mSecondLevelIndicatorControlBar.dismissSettingPopup();
                                 return true;
                             }
                         }  
                     }               
                }
            }
        }
        if(mAppBridge.isCurrentCameraPreview() && mIsZoomSupported) {
            onZoomTouchEvent(m);
            if(mIsZoomMode) return true;
        }
        return super.dispatchTouchEvent(m);
    }

    public void onZoomTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        
        case MotionEvent.ACTION_POINTER_DOWN:
            mIsZoomMode = true;
            mOldDist = spacing(event); 
            mHandler.sendEmptyMessage(SHOW_ZOOM_BAR);
            break;

        case MotionEvent.ACTION_MOVE:
            if(!mIsZoomMode || event.getPointerCount() != 2) {
                return;
            }
            
            if (mHandler.hasMessages(HIDE_ZOOM_BAR)) {
                mHandler.removeMessages(HIDE_ZOOM_BAR);
            }
            float newDist = spacing(event);
            float dis = newDist  - mOldDist;
            if(Math.abs(dis) > mStep) {
                int curZoomIdx = mZoomControl.getCurZoomIndex();
                int newZoomIdx = curZoomIdx + ((int)dis/mStep);
                if(newZoomIdx < 0 ) {
                    newZoomIdx = 0;
                } else if(newZoomIdx > mZoomMax) {
                    newZoomIdx = mZoomMax;
                }
                mZoomValue = newZoomIdx;

                // Set zoom parameters asynchronously
                if(mParameters != null && mZoomControl != null && mCameraDevice != null) {
                    mParameters.setZoom(mZoomValue);
                    mCameraDevice.setParametersAsync(mParameters);
                    mZoomControl.setZoomIndex(newZoomIdx);
                }
                mOldDist = newDist;
            }
            break;
        case MotionEvent.ACTION_POINTER_UP:
            mIsZoomMode = false;
            mHandler.sendEmptyMessageDelayed(HIDE_ZOOM_BAR,HIDE_ZOOM_BAR_DELAY);
            break;
        }
    }
    
    private float spacing(MotionEvent event) { 
        float x = event.getX(0) - event.getX(1);  
        float y = event.getY(0) - event.getY(1);  
        return FloatMath.sqrt(x * x + y * y);  
    } 
    
    private  BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received intent action=" + action);
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                checkStorage();
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                checkStorage();
            }
        }
    };

    private void initOnScreenIndicator() {
        mGpsIndicator = (ImageView) findViewById(R.id.onscreen_gps_indicator);
        /**mExposureIndicator = (TextView) findViewById(R.id.onscreen_exposure_indicator);
        mFlashIndicator = (ImageView) findViewById(R.id.onscreen_flash_indicator);
        mSceneIndicator = (ImageView) findViewById(R.id.onscreen_scene_indicator);
        mWhiteBalanceIndicator =
                (ImageView) findViewById(R.id.onscreen_white_balance_indicator);
        mFocusIndicator = (ImageView) findViewById(R.id.onscreen_focus_indicator);*/
    }

    @Override
    public void showGpsOnScreenIndicator(boolean hasSignal) {
        if (mGpsIndicator == null) {
            return;
        }
        if (hasSignal) {
            mGpsIndicator.setImageResource(R.drawable.ic_viewfinder_gps_on);
        } else {
            mGpsIndicator.setImageResource(R.drawable.ic_viewfinder_gps_no_signal);
        }
        if(mIsRecordLocation) {
            mGpsIndicator.setVisibility(View.VISIBLE);
        } else {
            mGpsIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void hideGpsOnScreenIndicator() {
        if (mGpsIndicator == null) {
            return;
        }
        mGpsIndicator.setVisibility(View.GONE);
    }

    private void updateExposureOnScreenIndicator(int value) {
        if (mExposureIndicator == null) {
            return;
        }
        //BEGIN: Modified by zhanghongxing at 2013-01-07 for DER-107
        // if (value == 0) {
        if (value == 0 || !Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
            mExposureIndicator.setText("");
            mExposureIndicator.setVisibility(View.GONE);
        } else {
            float step = mParameters.getExposureCompensationStep();
            mFormatterArgs[0] = value * step;
            mBuilder.delete(0, mBuilder.length());
            mFormatter.format("%+1.1f", mFormatterArgs);
            String exposure = mFormatter.toString();
            mExposureIndicator.setText(exposure);
            mExposureIndicator.setVisibility(View.VISIBLE);
        }
        //END:   Modified by zhanghongxing at 2013-01-07
    }

    private void updateFlashOnScreenIndicator(String value) {
        if (mFlashIndicator == null) {
            return;
        }
        if (value == null || Parameters.FLASH_MODE_OFF.equals(value)) {
            mFlashIndicator.setVisibility(View.GONE);
        } else {
            mFlashIndicator.setVisibility(View.VISIBLE);
            if (Parameters.FLASH_MODE_AUTO.equals(value)) {
                mFlashIndicator.setImageResource(R.drawable.ic_indicators_landscape_flash_auto);
            } else if (Parameters.FLASH_MODE_ON.equals(value)) {
                mFlashIndicator.setImageResource(R.drawable.ic_indicators_landscape_flash_on);
            } else {
                // Should not happen.
                mFlashIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void updateSceneOnScreenIndicator(String value) {
        if (mSceneIndicator == null) {
            return;
        }
        boolean isGone = (value == null) || (Parameters.SCENE_MODE_AUTO.equals(value));
        mSceneIndicator.setVisibility(isGone ? View.GONE : View.VISIBLE);
    }

    private void updateWhiteBalanceOnScreenIndicator(String value) {
        if (mWhiteBalanceIndicator == null) {
            return;
        }
        if (value == null || Parameters.WHITE_BALANCE_AUTO.equals(value)) {
            mWhiteBalanceIndicator.setVisibility(View.GONE);
        } else {
            mWhiteBalanceIndicator.setVisibility(View.VISIBLE);
            if (Parameters.WHITE_BALANCE_FLUORESCENT.equals(value)) {
                mWhiteBalanceIndicator.setImageResource(R.drawable.ic_indicators_fluorescent);
            } else if (Parameters.WHITE_BALANCE_INCANDESCENT.equals(value)) {
                mWhiteBalanceIndicator.setImageResource(R.drawable.ic_indicators_incandescent);
            } else if (Parameters.WHITE_BALANCE_DAYLIGHT.equals(value)) {
                mWhiteBalanceIndicator.setImageResource(R.drawable.ic_indicators_sunlight);
            } else if (Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT.equals(value)) {
                mWhiteBalanceIndicator.setImageResource(R.drawable.ic_indicators_cloudy);
            } else {
                // Should not happen.
                mWhiteBalanceIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void updateFocusOnScreenIndicator(String value) {
        if (mFocusIndicator == null) {
            return;
        }
        // Do not show the indicator if users cannot choose.
        if (mPreferenceGroup.findPreference(CameraSettings.KEY_FOCUS_MODE) == null) {
            mFocusIndicator.setVisibility(View.GONE);
        } else {
            mFocusIndicator.setVisibility(View.VISIBLE);
            if (Parameters.FOCUS_MODE_INFINITY.equals(value)) {
                mFocusIndicator.setImageResource(R.drawable.ic_indicators_landscape);
            } else if (Parameters.FOCUS_MODE_MACRO.equals(value)) {
                mFocusIndicator.setImageResource(R.drawable.ic_indicators_macro);
            } else {
                // Should not happen.
                mFocusIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void updateOnScreenIndicators() {
        updateSceneOnScreenIndicator(mParameters.getSceneMode());
        updateExposureOnScreenIndicator(CameraSettings.readExposure(mPreferences));
        updateFlashOnScreenIndicator(mParameters.getFlashMode());
        updateWhiteBalanceOnScreenIndicator(mParameters.getWhiteBalance());
        updateFocusOnScreenIndicator(mParameters.getFocusMode());
    }

    // After taken picture and when display a picture on screen, hide the
    // indicators icon on screen top-left
    private void goneIndicatorIcon() {
        if (mGpsIndicator != null) {
            mGpsIndicator.setVisibility(View.GONE);
        }
        /**if (mExposureIndicator != null) {
            mExposureIndicator.setVisibility(View.GONE);
        }
        if (mFlashIndicator != null) {
            mFlashIndicator.setVisibility(View.GONE);
        }
        if (mSceneIndicator != null) {
            mSceneIndicator.setVisibility(View.GONE);
        }
        if (mWhiteBalanceIndicator != null) {
            mWhiteBalanceIndicator.setVisibility(View.GONE);
        }
        if (mFocusIndicator != null) {
            mFocusIndicator.setVisibility(View.GONE);
        }*/
    }

    private final class ShutterCallback
            implements android.hardware.Camera.ShutterCallback {
        @Override
        public void onShutter() {
            mShutterCallbackTime = System.currentTimeMillis();
            mShutterLag = mShutterCallbackTime - mCaptureStartTime;
            Log.v(TAG, "==zzw test shutterInterval = " + mShutterLag + "ms");
            
            String takePicMode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                    getString(R.string.pref_camera_takepicture_mode_default));

            if (!mIsImageCaptureIntent && mParameters != null) {
                boolean isSupportHdr = false;
                if(Build.VERSION.SDK_INT >= 18) {
                    isSupportHdr = isSupportHdr();
                } else {
                    isSupportHdr = isHdrSupported();
                }
                if (isSupportHdr &&
                        takePicMode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_hdr))
                        && continueTakePicState == ContinueShutterState.NONE) {
                    mRotateTextToast = new RotateTextToast(Camera.this,
                            R.string.pano_review_saving_indication_str, mOrientation);
                    mRotateTextToast.show();
                }
            }

            if (continueTakePicState == ContinueShutterState.SHUTTING) {
                mContinueTakePicNum += 1;
                if(mTimerSnapManager != null) {
                    mTimerSnapManager.showContinueSnapNum(mContinueTakePicNum);
                }
            }
        }
    }

    private final class StatsCallback
           implements android.hardware.Camera.CameraDataCallback {
        @Override
        public void onCameraData(int [] data, android.hardware.Camera camera) {
            //if(!mPreviewing || !mHiston || !mFirstTimeInitialized){
            if(!mHiston || !mFirstTimeInitialized){
                return;
            }
            /*The first element in the array stores max hist value . Stats data begin from second value*/
            synchronized(statsdata) {
                System.arraycopy(data,0,statsdata,0,STATS_DATA);
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    if(mGraphView != null)
                        mGraphView.PreviewChanged();
                }
           });
        }
    }

    private final class PostViewPictureCallback implements PictureCallback {
        @Override
        public void onPictureTaken(
                byte [] data, android.hardware.Camera camera) {
            mPostViewPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToPostViewCallbackTime = "
                    + (mPostViewPictureCallbackTime - mShutterCallbackTime)
                    + "ms");
        }
    }

    private final class RawPictureCallback implements PictureCallback {
        @Override
        public void onPictureTaken(
                byte [] rawData, android.hardware.Camera camera) {
            mRawPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToRawCallbackTime = "
                    + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
        }
    }

    private final class JpegPictureCallback implements PictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
        }

        @Override
        public void onPictureTaken(
                final byte [] jpegData, final android.hardware.Camera camera) {
            Log.i(TAG,"zzw:onPictureTaken()");

            if (mPaused) {
                return;
            }

            mReceivedSnapNum = mReceivedSnapNum + 1;

            mFocusManager.resetTouchFocus();
            mFocusManager.updateFocusUI(); // Ensure focus indicator is hidden.
            
            if(continueTakePicState != ContinueShutterState.NONE) {
                //do nothing
            } else if (!mIsImageCaptureIntent && mSnapshotMode != CameraInfo.CAMERA_SUPPORT_MODE_ZSL
                && mReceivedSnapNum == mBurstSnapNum) {
                startPreview();
                setCameraState(IDLE);
                startFaceDetection();
                mShutterButton.setEnabled(true);
            }else if(mIsImageCaptureIntent) {
                if(mSnapshotMode == CameraInfo.CAMERA_SUPPORT_MODE_ZSL) {
                     stopPreview();
                }
            } else if(mReceivedSnapNum == mBurstSnapNum &&
                    continueTakePicState == ContinueShutterState.NONE) {
                setCameraState(IDLE);
                startFaceDetection();
                mShutterButton.setEnabled(true);
            }

            if (!mIsImageCaptureIntent) {
                // Calculate the width and the height of the jpeg.
                Size s = mParameters.getPictureSize();
                int orientation = Exif.getOrientation(jpegData);
                int width, height;
                if ((mJpegRotation + orientation) % 180 == 0) {
                    width = s.width;
                    height = s.height;
                } else {
                    width = s.height;
                    height = s.width;
                }
                if(mReceivedSnapNum > 1){
                    mImageNamer.generateUri(); //added to solve zsl
                }
                Uri uri = mImageNamer.getUri();
                String title = mImageNamer.getTitle();
                if(continueTakePicState != ContinueShutterState.NONE) {
                    cacheImage(jpegData, uri, title, mLocation,
                            width, height, mThumbnailViewWidth, orientation);
                } else {
                    mImageSaver.addImage(jpegData, uri, title, mLocation,
                        width, height, mThumbnailViewWidth, orientation);
                }
            } else {
                mJpegImageData = jpegData;
                if (!mQuickCapture) {
                    showPostCaptureAlert();
                    goneIndicatorIcon();
                } else {
                    doAttach();
                }
            }

            if (continueTakePicState == ContinueShutterState.NONE
                    &&!mIsImageCaptureIntent && mReceivedSnapNum == mBurstSnapNum - 1) {
                // Start capture animation.
                mCameraScreenNail.animateCapture(getCameraRotation());
            }
            
            if (mReceivedSnapNum == mBurstSnapNum) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                	public void run() {
                		canDoVolumeSnap = true;
                	}
                }, 300);
            }
        }
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
        // no support
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromtouch) {
        }
        public void onStopTrackingTouch(SeekBar bar) {
        }
    };

    private OnSeekBarChangeListener mskinToneSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
        // no support
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromtouch) {
            int value = (progress + MIN_SCE_FACTOR) * SCE_FACTOR_STEP;
            if(progress > (MAX_SCE_FACTOR - MIN_SCE_FACTOR)/2){
                RightValue.setText(String.valueOf(value));
                LeftValue.setText("");
            } else if (progress < (MAX_SCE_FACTOR - MIN_SCE_FACTOR)/2){
                LeftValue.setText(String.valueOf(value));
                RightValue.setText("");
            } else {
                LeftValue.setText("");
                RightValue.setText("");
            }

            if(value != mskinToneValue && mCameraDevice != null) {
                mskinToneValue = value;
                mParameters = mCameraDevice.getParameters();
                mParameters.set("skinToneEnhancement", String.valueOf(mskinToneValue));
                mCameraDevice.setParameters(mParameters);
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
            Log.e(TAG, "Set onStopTrackingTouch mskinToneValue = " + mskinToneValue);
            Editor editor = mPreferences.edit();
            editor.putString(CameraSettings.KEY_SKIN_TONE_ENHANCEMENT_FACTOR,
                             Integer.toString(mskinToneValue));
            editor.apply();
        }
    };
    private final class AutoFocusCallback
            implements android.hardware.Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(
                boolean focused, android.hardware.Camera camera) {
            if (mPaused) return;

            mAutoFocusTime = System.currentTimeMillis() - mFocusStartTime;
            Log.v(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms");

            mIsLastFocusedSuc = focused;
            setCameraState(IDLE);
            mFocusManager.onAutoFocus(focused);
        }
    }

    // BEGIN: Added by zhanghongxing at 2013-07-24
    private final class SilentAutoFocusCallback
            implements android.hardware.Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(
                boolean focused, android.hardware.Camera camera) {
            if (mPaused) return;

            mAutoFocusTime = System.currentTimeMillis() - mFocusStartTime;
            Log.v(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms");
            if(mCameraState != TIMER_SNAP_IN_PROGRESS) {
                setCameraState(IDLE);
            }
            mFocusManager.onSilentAutoFocus(focused);
        }
    }
    // END:   Added by zhanghongxing at 2013-07-24

    private final class AutoFocusMoveCallback
            implements android.hardware.Camera.AutoFocusMoveCallback {
        @Override
        public void onAutoFocusMoving(
            boolean moving, android.hardware.Camera camera) {
                mFocusManager.onAutoFocusMoving(moving);
        }
    }

    // Each SaveRequest remembers the data needed to save an image.
    private static class SaveRequest {
        byte[] data;
        Uri uri;
        String title;
        Location loc;
        int width, height;
        int thumbnailWidth;
        int orientation;
    }

    // We use a queue to store the SaveRequests that have not been completed
    // yet. The main thread puts the request into the queue. The saver thread
    // gets it from the queue, does the work, and removes it from the queue.
    //
    // The main thread needs to wait for the saver thread to finish all the work
    // in the queue, when the activity's onPause() is called, we need to finish
    // all the work, so other programs (like Gallery) can see all the images.
    //
    // If the queue becomes too long, adding a new request will block the main
    // thread until the queue length drops below the threshold (QUEUE_LIMIT).
    // If we don't do this, we may face several problems: (1) We may OOM
    // because we are holding all the jpeg data in memory. (2) We may ANR
    // when we need to wait for saver thread finishing all the work (in
    // onPause() or gotoGallery()) because the time to finishing a long queue
    // of work may be too long.
    private class ImageSaver extends Thread {
        private static final int QUEUE_LIMIT = 2;

        private ArrayList<SaveRequest> mQueue;
        private Thumbnail mPendingThumbnail;
        private Object mUpdateThumbnailLock = new Object();
        private boolean mStop;

        // Runs in main thread
        public ImageSaver() {
            mQueue = new ArrayList<SaveRequest>();
            start();
        }

        // Runs in main thread
        public void addImage(final byte[] data, Uri uri, String title,
                Location loc, int width, int height, int thumbnailWidth,
                int orientation) {
            SaveRequest r = new SaveRequest();
            r.data = data;
            r.uri = uri;
            r.title = title;
            r.loc = (loc == null) ? null : new Location(loc);  // make a copy
            r.width = width;
            r.height = height;
            r.thumbnailWidth = thumbnailWidth;
            r.orientation = orientation;
            synchronized (this) {
                while (mQueue.size() >= QUEUE_LIMIT) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                }
                mQueue.add(r);
                notifyAll();  // Tell saver thread there is new work to do.
            }
        }

        
        // Runs in main thread
        public void addImage(SaveRequest r) {
            synchronized (this) {
                mQueue.add(r);
                notifyAll();  // Tell saver thread there is new work to do.
            }
        }
        // Runs in saver thread
        @Override
        public void run() {
            while (true) {
                SaveRequest r;
                synchronized (this) {
                    if (mQueue.isEmpty()) {
                        notifyAll();  // notify main thread in waitDone

                        // Note that we can only stop after we saved all images
                        // in the queue.
                        if (mStop) break;

                        try {
                            wait();
                        } catch (InterruptedException ex) {
                            // ignore.
                        }
                        continue;
                    }
                    r = mQueue.get(0);
                }
                mSavedQueueSize++;
                if(Storage.LOW_STORAGE_THRESHOLD <= Storage.getAvailableSpace()) {
                    storeImage(r.data, r.uri, r.title, r.loc, r.width, r.height,
                            r.thumbnailWidth, r.orientation);
                }
                synchronized (this) {
                    mQueue.remove(0);
                    if(r != null) r = null;
                    notifyAll();  // the main thread may wait in addImage

//                    if(continueTakePicState != ContinueShutterState.NONE
//                            && mCacheQueueSize == mSavedQueueSize) {
//                        continueTakePicState = ContinueShutterState.NONE;
//                        mHandler.sendEmptyMessage(DISMISS_SAVE_PROGRESS_BAR);
//                    }
                }
            }
        }

        // Runs in main thread
        public void waitDone() {
            synchronized (this) {
                while (!mQueue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                }
            }
            updateThumbnail();
        }

        // Runs in main thread
        public void finish() {
            waitDone();
            synchronized (this) {
                mStop = true;
                notifyAll();
            }
            try {
                join();
            } catch (InterruptedException ex) {
                // ignore.
            }
        }

        // Runs in main thread (because we need to update mThumbnailView in the
        // main thread)
        public void updateThumbnail() {
            Thumbnail t;
            synchronized (mUpdateThumbnailLock) {
                mHandler.removeMessages(UPDATE_THUMBNAIL);
                t = mPendingThumbnail;
                mPendingThumbnail = null;
            }

            if (t != null) {
                mThumbnail = t;
                mThumbnailView.setBitmap(mThumbnail.getBitmap());
            }

            String takepPicMode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                    getString(R.string.pref_camera_takepicture_mode_default));

            if (!mIsImageCaptureIntent && mParameters != null) {
                boolean isSupportHdr = false;
                if(Build.VERSION.SDK_INT >= 18) {
                    isSupportHdr = isSupportHdr();
                } else {
                    isSupportHdr = isHdrSupported();
                }
                if (isSupportHdr &&
                        takepPicMode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
                    if (mReceivedSnapNum == mBurstSnapNum) {
                        if (mRotateTextToast != null) {
                            mRotateTextToast.cancel();
                        }
                    }
                }
            }
        }

        // Runs in saver thread
        private void storeImage(final byte[] data, Uri uri, String title,
                Location loc, int width, int height, int thumbnailWidth,
                int orientation) {
            String pictureFormat = mParameters.get(KEY_PICTURE_FORMAT);
            if(data == null) {
                Log.v(TAG, "image data null");
                return;
            }
            boolean ok = Storage.updateImage(mContentResolver, uri, title, loc,
                    orientation, data, width, height, pictureFormat);
            if (ok) {
                boolean needThumbnail;
                Uri newUri= Storage.updateUri();
                synchronized (this) {
                    // If the number of requests in the queue (include the
                    // current one) is greater than 1, we don't need to generate
                    // thumbnail for this image. Because we'll soon replace it
                    // with the thumbnail for some image later in the queue.
                    needThumbnail = (mQueue.size() <= 1);
                }
                //BEGIN: Modified by zhanghongxing at 2013-01-16 for DER-192
                int ratio = (int) Math.ceil((double) width / thumbnailWidth);
                int inSampleSize = Integer.highestOneBit(ratio);
                Thumbnail newt = Thumbnail.createThumbnail(
                        data, orientation, inSampleSize, newUri);
                
                if (needThumbnail) {
                    // Create a thumbnail whose width is equal or bigger than
                    // that of the thumbnail view.
                    // int ratio = (int) Math.ceil((double) width / thumbnailWidth);
                    // int inSampleSize = Integer.highestOneBit(ratio);
                    //Thumbnail t = Thumbnail.createThumbnail(
                    //            data, orientation, inSampleSize, uri);
                    //change to new Uri.
                    // Thumbnail newt = Thumbnail.createThumbnail(
                    //     data, orientation, inSampleSize, newUri);

                    synchronized (mUpdateThumbnailLock) {
                        // We need to update the thumbnail in the main thread,
                        // so send a message to run updateThumbnail().
                        mPendingThumbnail = newt;
                        mHandler.sendEmptyMessage(UPDATE_THUMBNAIL);
                        Log.i(TAG, "storeImage, mPendingThumbnail = " + mPendingThumbnail);
                    }
                } else {
                    synchronized (mUpdateThumbnailLock) {
                        mPendingThumbnail = newt;
                        Log.i(TAG, "storeImage, don't need update, mPendingThumbnail = " + mPendingThumbnail);
                    }
                }
                //END:   Modified by zhanghongxing at 2013-01-16
                Util.broadcastNewPicture(Camera.this, newUri);
            }
        }
    }

    private class ImageNamer extends Thread {
        private boolean mRequestPending;
        private ContentResolver mResolver;
        private long mDateTaken;
        private int mWidth, mHeight;
        private boolean mStop;
        private Uri mUri;
        private String mTitle;
        private String mPictureFormat;
        private Parameters mParams;
        // Runs in main thread
        public ImageNamer() {
            start();
        }

        // Runs in main thread
        public synchronized void prepareUri(ContentResolver resolver,
                long dateTaken, int width, int height, int rotation) {
            if (rotation % 180 != 0) {
                int tmp = width;
                width = height;
                height = tmp;
            }
            mRequestPending = true;
            mResolver = resolver;
            mDateTaken = dateTaken;
            mWidth = width;
            mHeight = height;
            notifyAll();
        }

        // Runs in main thread
        public synchronized Uri getUri() {
            // wait until the request is done.
            while (mRequestPending) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore.
                }
            }

            // return the uri generated
            Uri uri = mUri;
            mUri = null;
            return uri;
        }

        // Runs in main thread, should be called after getUri().
        public synchronized String getTitle() {
            return mTitle;
        }

        // Runs in namer thread
        @Override
        public synchronized void run() {
            while (true) {
                if (mStop) break;
                if (!mRequestPending) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                    continue;
                }
                cleanOldUri();
                generateUri();
                mRequestPending = false;
                notifyAll();
            }
            cleanOldUri();
        }

        // Runs in main thread
        public synchronized void finish() {
            mStop = true;
            notifyAll();
        }

        // Runs in namer thread
        private void generateUri() {
            mTitle = Util.createJpegName(mDateTaken,getApplicationContext());
//            mParams = mCameraDevice.getParameters();
            String pictureFormat = mParameters.get(KEY_PICTURE_FORMAT);
            mUri = Storage.newImage(mResolver, mTitle, mDateTaken, mWidth, mHeight, pictureFormat);
        }

        // Runs in namer thread
        private void cleanOldUri() {
            if (mUri == null) return;
            Storage.deleteImage(mResolver, mUri);
            mUri = null;
        }
    }

    private void setCameraState(int state) {
        mCameraState = state;
        switch (state) {
            case PREVIEW_STOPPED:
            case SNAPSHOT_IN_PROGRESS:
            // case FOCUSING: // Deleted by zhanghongxing at 2013-08-07
            case SWITCHING_CAMERA:
            case TIMER_SNAP_IN_PROGRESS:
                enableCameraControls(false);
                break;
            case IDLE:
                enableCameraControls(true);
                break;
            //BEGIN: Added by xiongzhu at 2013-04-15
            case CHANGING_STORAGE_STATE:
                 enableCameraControls(false);
                 break;
            //END:   Added by xiongzhu at 2013-04-15
        }
    }

    @Override
    public boolean capture() {
        // If we are already in the middle of taking a snapshot then ignore.
        if (mCameraDevice == null || mCameraState == SNAPSHOT_IN_PROGRESS
            || mCameraState == CHANGING_STORAGE_STATE //Added by xiongzhu at 2013-04-15
            || mCameraState == SWITCHING_CAMERA) {
            return false;
        }
        canDoVolumeSnap = false;
        setCameraState(SNAPSHOT_IN_PROGRESS);
        mCaptureStartTime = System.currentTimeMillis();
        mPostViewPictureCallbackTime = 0;
        mJpegImageData = null;

        // Set rotation and gps data.
        mJpegRotation = Util.getJpegRotation(mCameraId, mOrientation);
        mParameters.setRotation(mJpegRotation);
        String pictureFormat = mParameters.get(KEY_PICTURE_FORMAT);
        Location loc = null;
        if (pictureFormat != null 
            && PIXEL_FORMAT_JPEG.equalsIgnoreCase(pictureFormat)
            && mIsRecordLocation) {
            loc = mLocationManager.getCurrentLocation();
        }
        Util.setGpsParameters(mParameters, loc);
        mCameraDevice.setParameters(mParameters);

        mCameraDevice.takePicture(mShutterCallback, mRawPictureCallback,
                mPostViewPictureCallback, new JpegPictureCallback(loc));

        if(mHiston) {
            mHiston = false;
            mCameraDevice.setHistogramMode(null);
            runOnUiThread(new Runnable() {
                public void run() {
                    if(mGraphView != null)
                    mGraphView.setVisibility(View.INVISIBLE);
                }
            });
        }

        Size size = mParameters.getPictureSize();
        mImageNamer.prepareUri(mContentResolver, mCaptureStartTime,
                size.width, size.height, mJpegRotation);

        if(continueTakePicState != ContinueShutterState.NONE) {
            mBurstSnapNum = mParameters.getInt("snapshot-burst-num");
        } else {
            mBurstSnapNum = mParameters.getInt("num-snaps-per-shutter");
        }
        //if mBurstSnapNum is bigger than 2, we wait for The penultimate picture taked
        //then start capture animation.
        
        //START:modified by xiongzhu for ARD-339
        //if (!mIsImageCaptureIntent && mBurstSnapNum <= 1) {
        if (!mIsImageCaptureIntent && mBurstSnapNum <= 1 && mSnapshotMode != CameraInfo.CAMERA_SUPPORT_MODE_ZSL
                && continueTakePicState == ContinueShutterState.NONE) {
            // Start capture animation.
            mCameraScreenNail.animateCapture(getCameraRotation());
        }
        //END: modified by xiongzhu for ARD-339

        if(mSnapshotMode != CameraInfo.CAMERA_SUPPORT_MODE_ZSL) {
             mFaceDetectionStarted = false;
        }
        mReceivedSnapNum = 0;
        return true;
    }

    private int getCameraRotation() {
        return (mOrientationCompensation - mDisplayRotation + 360) % 360;
    }

    @Override
    public void setFocusParameters() {
        setCameraParameters(UPDATE_PARAM_PREFERENCE);
    }

    @Override
    public void playSound(int soundId) {
        if (mIsSilentMode) return; //Add by wangbin at 2013-03-22
        mCameraSound.play(soundId);
    }

    private int getPreferredCameraId(ComboPreferences preferences) {
        int intentCameraId = Util.getCameraFacingIntentExtras(this);
        if (intentCameraId != -1) {
            // Testing purpose. Launch a specific camera through the intent
            // extras.
            return intentCameraId;
        } else {
            return CameraSettings.readPreferredCameraId(preferences);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mPostCaptureAlertFlag = false;
        super.onCreate(savedInstanceState);
        mPreferences = new ComboPreferences(this);
        CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
        mCameraId = getPreferredCameraId(mPreferences);
        mContentResolver = getContentResolver();

        // To reduce startup time, open the camera and start the preview in
        // another thread.
        mCameraStartUpThread = new CameraStartUpThread();
        mCameraStartUpThread.start();

        setContentView(R.layout.camera);

        // Surface texture is from camera screen nail and startPreview needs it.
        // This must be done before startPreview.
        mIsImageCaptureIntent = isImageCaptureIntent();

        //BEGIN: Deleted by xiongzhu at 2013-04-15
        // createCameraScreenNail(!mIsImageCaptureIntent);
        //END:   Deleted by xiongzhu at 2013-04-15

        mPreferences.setLocalId(this, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
        mFocusAreaIndicator = (RotateLayout) findViewById(
                R.id.focus_indicator_rotate_layout);
        // we need to reset exposure for the preview
        //resetExposureCompensation(); delete by xiongzhu for CBB-354
        // Starting the preview needs preferences, camera screen nail, and
        // focus area indicator.

        mTimerSnap = mPreferences.getString(
                CameraSettings.KEY_TIMER_SNAP,
                getString(R.string.pref_timersnap_default));
        //BEGIN: Added by xiongzhu at 2013-04-15
        String storagePlace = mPreferences.getString(CameraSettings.KEY_CAMERA_STORAGE_PLACE,getString(R.string.pref_storage_place_default));
        Storage.mIsExternalStorage = storagePlace.equals(CameraSettings.CAMERA_STORAGE_SDCARD ) ? true:false;
        //
        // BEGIN: Modified by zhanghongxing at 2013-05-31
         
        initializeControlByIntent();

        //BEGIN: Added by zhanghongxing at 2013-06-22
        mAutoFocusManager = new AutoFocusManager(this, mAutoFocusListener);
        //END:   Added by zhanghongxing at 2013-06-22
        int tryCount = 2;
        String internalPath;
        String externalPath;
        while(true) {
            try{
                internalPath = FxEnvironment.getInternalStorageDirectory(getApplicationContext());
                externalPath = FxEnvironment.getExternalStorageDirectory(getApplicationContext());
                Storage.init();
                tryCount--;
                if(Storage.getAvailableSpace() >= 0 || tryCount <= 0)
                    break;
                Thread.sleep(500);
            } catch(Exception e) {
                //do nothing
            }
        }
        if (mIsImageCaptureIntent && mSaveUri != null) {
            String savePath = mSaveUri.getPath();
            if ((externalPath != null)
                    && savePath.indexOf(externalPath) != -1) {
                Storage.mIsExternalStorage = true;
            }else if ((internalPath != null)
                    && savePath.indexOf(internalPath) != -1){
                Storage.mIsExternalStorage = false;
            } else {
                Log.e(TAG, "Not found sdcard path!");
            }
        }

        if( Storage.mIsExternalStorage ) {
            mStorageSpace = Storage.getAvailableSpace();
            if ( mStorageSpace == Storage.UNAVAILABLE ) {
              SharedPreferences.Editor editor = ComboPreferences
                    .get(Camera.this).edit();
              editor.putString(CameraSettings.KEY_CAMERA_STORAGE_PLACE,CameraSettings.CAMERA_STORAGE_MEMORY);
              editor.apply();
              Storage.mIsExternalStorage=false;
              storagePlace=CameraSettings.CAMERA_STORAGE_MEMORY;
            }
        }
        
        mStoragePlace = storagePlace;
        
        mVolumeKeyMode = mPreferences.getString(CameraSettings.KEY_VOLUME_KEY_MODE,
                getString(R.string.pref_volume_key_default));
//        SharedPreferences.Editor editor = ComboPreferences.get(Camera.this).edit();
//        editor.putString(CameraSettings.KEY_SHUTTER_KEY_MODE,mShutterKeyMode);
//        editor.apply();
        createCameraScreenNail(!mIsImageCaptureIntent,Storage.mIsExternalStorage);
        //END:   Added by xiongzhu at 2013-04-15

        mStartPreviewPrerequisiteReady.open();

        //initializeControlByIntent();
        //END:   Modified by zhanghongxing at 2013-05-31

        mRotateDialog = new RotateDialogController(this, R.layout.rotate_dialog);
        mNumberOfCameras = CameraHolder.instance().getNumberOfCameras();
        mQuickCapture = getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        initializeMiscControls();
        brightnessProgressBar = (ProgressBar) findViewById(R.id.progress);
        if (brightnessProgressBar instanceof SeekBar) {
            SeekBar seeker = (SeekBar) brightnessProgressBar;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        brightnessProgressBar.setMax(MAXIMUM_BRIGHTNESS);
        brightnessProgressBar.setProgress(mbrightness);
        brightnessProgressBar.setVisibility(View.INVISIBLE);
        skinToneSeekBar = (SeekBar) findViewById(R.id.skintoneseek);
        skinToneSeekBar.setOnSeekBarChangeListener(mskinToneSeekListener);
        skinToneSeekBar.setVisibility(View.INVISIBLE);
        Title = (TextView)findViewById(R.id.skintonetitle);
        RightValue = (TextView)findViewById(R.id.skintoneright);
        LeftValue = (TextView)findViewById(R.id.skintoneleft);
        mLocationManager = new LocationManager(this, this);
        initOnScreenIndicator();
        // Make sure all views are disabled before camera is open.
        enableCameraControls(false); 
        mSeekBarInitialized = true;
    }

    private void overrideCameraSettings(final String flashMode,
            final String whiteBalance, final String focusMode,
            final String exposureMode, final String touchMode,
            final String autoExposure, final String colorEffect) {
       
       if (mIndicatorControlContainer != null) {
            mIndicatorControlContainer.enableFilter(true);
            mIndicatorControlContainer.enableFilter(false);
        }
       if (mSecondLevelIndicatorControlBar != null) {
           mSecondLevelIndicatorControlBar.setupFilter(true);
           mSecondLevelIndicatorControlBar.overrideSettings(
                   CameraSettings.KEY_FLASH_MODE, flashMode,
                   CameraSettings.KEY_WHITE_BALANCE, whiteBalance,
                   CameraSettings.KEY_FOCUS_MODE, focusMode,
                   CameraSettings.KEY_EXPOSURE, exposureMode,
                   CameraSettings.KEY_TOUCH_AF_AEC, touchMode,
                   CameraSettings.KEY_AUTOEXPOSURE, autoExposure,
                   CameraSettings.KEY_COLOR_EFFECT, colorEffect);
           mSecondLevelIndicatorControlBar.setupFilter(false);
       }
    }

    private void updateSceneModeUI() {
        // If scene mode is set, we cannot set flash mode, white balance, and
        // focus mode, instead, we read it from driver

        if (!Parameters.SCENE_MODE_AUTO.equals(mSceneMode) || mIsNeedUpdateSceneUI) {
            mIsNeedUpdateSceneUI = false;
            overrideCameraSettings(mParameters.getFlashMode(),
                    mParameters.getWhiteBalance(), mInitialParams.getFocusMode(),
                    Integer.toString(mParameters.getExposureCompensation()),
                    mParameters.getTouchAfAec(), mParameters.getAutoExposure(),
                    Parameters.EFFECT_NONE);
            String colorEffect = mPreferences.getString(
                    CameraSettings.KEY_COLOR_EFFECT,
                    getString(R.string.pref_camera_coloreffect_default));
            //if the current colorEffect is not equal "none", to refresh preference
            if (!Parameters.EFFECT_NONE.equals(colorEffect)) {
                Editor editor = mPreferences.edit();
                editor.putString(CameraSettings.KEY_COLOR_EFFECT,
                            Parameters.EFFECT_NONE);
                editor.apply();
                IconListPreference colorEffectPref =  (IconListPreference)mPreferenceGroup.findPreference(CameraSettings.KEY_COLOR_EFFECT);
                if(colorEffectPref != null) {
                    colorEffectPref.setValueIndex(0);
                }
            }

            //BEGIN: Added by zhanghongxing at 2013-01-07 for DER-107
            //if (mIndicatorControlContainer != null) {
            //    mIndicatorControlContainer.setOtherSettingButtonEnabled(false);
           // }
           
//           if (mSecondLevelIndicatorControlBar != null) {
//                mSecondLevelIndicatorControlBar
//                        .setOtherSettingButtonEnabled(false);
//            }
            //END:   Added by zhanghongxing at 2013-01-07
        } else {
            overrideCameraSettings(null, null, null, null, null, null, null);
            //BEGIN: Added by zhanghongxing at 2013-01-07 for DER-107
            //if (mIndicatorControlContainer != null) {
            //    mIndicatorControlContainer.setOtherSettingButtonEnabled(true);
            //}
            
            if (mSecondLevelIndicatorControlBar != null) {
                mSecondLevelIndicatorControlBar
                        .setOtherSettingButtonEnabled(true);
            }
            //END:   Added by zhanghongxing at 2013-01-07
        }
    }

    private void loadCameraPreferences() {
        CameraSettings settings = new CameraSettings(this, mInitialParams,
                mCameraId, CameraHolder.instance().getCameraInfo());
        mPreferenceGroup = settings.getPreferenceGroup(R.xml.camera_preferences);
    }

    private void initializeIndicatorControl() {
        // setting the indicator buttons.
        mIndicatorControlContainer =
                (IndicatorControlContainer) findViewById(R.id.indicator_control);
        
        //START:modified by xiongzhu for cbb
        mSecondLevelIndicatorControlBar = (SecondLevelIndicatorControlBar) findViewById(R.id.second_level_indicator);
        loadCameraPreferences();
        mOldPictureMode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                getString(R.string.pref_camera_takepicture_mode_default));
        if((mCameraId == CameraInfo.CAMERA_FACING_FRONT) ||
                (mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_zsl))
                        || (mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_continue))))) {
            ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_FLASH_MODE);
            if(pref != null) ((IconListPreference) pref).setAvailable(false);
        }
        if(mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
            ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_COLOR_EFFECT);
            if(pref != null) ((IconListPreference) pref).setAvailable(false);
            ListPreference sceneMode = mPreferenceGroup.findPreference(CameraSettings.KEY_SCENE_MODE);
            if(sceneMode != null) {
                sceneMode.setEnable(false);
            }
        }else if (mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_anti_shake))) {
            Editor editor = mPreferences.edit();
            editor.putString(CameraSettings.KEY_ISO,
                    getString(R.string.pref_camera_iso_value_isodeblur));
            editor.commit();
            ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_ISO);
            pref.setEnable(false);
        }
        //BEGIN: Modified by zhanghongxing at 2013-05-28
        //H901 front camera QCOM_SETTING_KEYS_1 is not supported
        HashMap otherSettingKeys = new HashMap(3);
        ArrayList<String>  otherList = new ArrayList<String>();
        if (mIsImageCaptureIntent) {
            for(int i = 0; i < OTHER_SETTING_KEYS_INTENT.length; i++) {
                otherList.add(OTHER_SETTING_KEYS_INTENT[i]);
            }
        } else {
            for(int i = 0; i < OTHER_SETTING_KEYS.length; i++)
                otherList.add(OTHER_SETTING_KEYS[i]);
        }
        
        mCurIntelligenceValue = mPreferences.getString(
                CameraSettings.KEY_CAMERA_INTELLIGENCE_KEY,
                getString(R.string.pref_intelligence_default));
        if(mCurIntelligenceValue.equals("on")) {
        	for(int i = 0; i < QCOM_SETTING_KEYS.length; i++) {
        		otherList.add(QCOM_SETTING_KEYS[i]);
        	}
        	CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        	if (info.facing != CameraInfo.CAMERA_FACING_FRONT) {
        		for(int i = 0; i < QCOM_SETTING_KEYS_1.length; i++) {
        			otherList.add(QCOM_SETTING_KEYS_1[i]);
        		}
        	}
        }
        
        
        //otherList.add(CameraSettings.KEY_VERSION_NUMBER);
        
        otherSettingKeys.put(0, (String[])otherList.toArray(new String[otherList.size()]));
        otherList.clear();
        otherList = null;
        //END:   modified by zhanghongxing at 2013-05-28
        String[] SETTING_KEYS = {
                CameraSettings.KEY_FLASH_MODE,
//                CameraSettings.KEY_WHITE_BALANCE,
//                CameraSettings.KEY_EXPOSURE,
                //CameraSettings.KEY_SCENE_MODE,
                CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                CameraSettings.KEY_COLOR_EFFECT
                };

        CameraPicker.setImageResourceId(R.drawable.camerapicker_selector);
        mIndicatorControlContainer.initialize(this, mPreferenceGroup,
                mIsZoomSupported,
                null, null);
        mSecondLevelIndicatorControlBar.initialize(this, mPreferenceGroup,
                SETTING_KEYS, otherSettingKeys);
        otherSettingKeys.clear();
        //mCameraPicker = (CameraPicker) mIndicatorControlContainer.findViewById(
        //       R.id.camera_picker);
//        mCameraPicker = (CameraPicker) mSecondLevelIndicatorControlBar.findViewById(
//                R.id.camera_picker);
        updateSceneModeUI();
        mIndicatorControlContainer.setOrientation(mOrientationCompensation, true);
        mIndicatorControlContainer.setListener(this);
        mSecondLevelIndicatorControlBar.setOrientation(mOrientationCompensation, true);
        mSecondLevelIndicatorControlBar.setListener(this);
        
        mSaveProgressBarLayout = (RelativeLayout) findViewById(R.id.save_progress_layout);
        //END:modified by xiongzhu for cbb
    }

    private boolean collapseCameraControls() {
        //if ((mIndicatorControlContainer != null)
        //        && mIndicatorControlContainer.dismissSettingPopup()) {
        //    return true;
        //}

        if (mSecondLevelIndicatorControlBar != null) {
            View popup = mSecondLevelIndicatorControlBar.getActiveSettingPopup();
            if (popup != null && popup.getVisibility() != View.GONE) {
                mSecondLevelIndicatorControlBar.dismissSettingPopup();
                return true;
            }
        }
        //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
        // if (mModePicker != null && mModePicker.dismissModeSelection()) return true;
        //END:   Deleted by zhanghongxing at 2013-01-09
        return false;
    }

    private void enableCameraControls(boolean enable) {
        //if (mIndicatorControlContainer != null) {
         //   mIndicatorControlContainer.setEnabled(enable);
         //   //BEGIN: Added by zhanghongxing at 2013-01-07 for DER-107
         //   updateSceneModeUI();
         //   //END:   Added by zhanghongxing at 2013-01-07
        //}
        
        if(continueTakePicState != ContinueShutterState.NONE && enable) {
            return;
        }
        if (mSecondLevelIndicatorControlBar != null) {
            mSecondLevelIndicatorControlBar.setEnabled(enable);
            //BEGIN: Added by zhanghongxing at 2013-01-07 for DER-107
//            updateSceneModeUI();
            //END:   Added by zhanghongxing at 2013-01-07
        }
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // if (mModePicker != null) mModePicker.setEnabled(enable);
//        if (mSwitchToCamera != null) mSwitchToCamera.setEnabled(enable);
        if (mSwitchToVideo != null) mSwitchToVideo.setEnabled(enable);
        if (mZoomControl != null) mZoomControl.setEnabled(enable);
        // if (mThumbnailView != null) mThumbnailView.setEnabled(enable);
        if (mThumbnailWindow != null) mThumbnailWindow.setEnabled(enable);
        //END:   Modified by zhanghongxing at 2013-01-09
    }

    private class MyOrientationEventListener
            extends OrientationEventListener {
        private int mCurrentOrientationSetNumber  = 0;
        // The value of ORIENTATION_SET_NUMBER can be smaller or bigger ,but be larger than zero.
        private final static int ORIENTATION_SET_NUMBER = 5;

        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = Util.roundOrientation(orientation, mOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation =
                    (mOrientation + Util.getDisplayRotation(Camera.this)) % 360;
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                setOrientationIndicator(mOrientationCompensation, true);
            } else if (ORIENTATION_SET_NUMBER > mCurrentOrientationSetNumber) {
                // set icon orientation five times although the platform orientation may be not changed.
                setOrientationIndicator(mOrientationCompensation, true);
                mOrientationCompensation = orientationCompensation;
                mCurrentOrientationSetNumber++;
            }

            // Show the toast after getting the first orientation changed.
            if (mHandler.hasMessages(SHOW_TAP_TO_FOCUS_TOAST)) {
                mHandler.removeMessages(SHOW_TAP_TO_FOCUS_TOAST);
                showTapToFocusToast();
            }
        }
    }

    private void setOrientationIndicator(int orientation, boolean animation) {
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // Rotatable[] indicators = {mThumbnailView, mModePicker,
        Rotatable[] indicators = {mThumbnailView, mSwitchToCamera, mSwitchToVideo, mThumbnailBgView,//mSwitchToPanorama, delete by xiongzhu for cbb
                mIndicatorControlContainer, mZoomControl, mFocusAreaIndicator, mFaceView,
                mReviewDoneButton, mRotateDialog, mSecondLevelIndicatorControlBar, mShutterButton
                };
        //END:   Modified by zhanghongxing at 2013-01-09
        for (Rotatable indicator : indicators) {
            if (indicator != null) indicator.setOrientation(orientation, animation);
        }

        // We change the orientation of the review cancel button only for tablet
        // UI because there's a label along with the X icon. For phone UI, we
        // don't change the orientation because there's only a symmetrical X
        // icon.
        if (mReviewCancelButton instanceof RotateLayout) {
            mReviewCancelButton.setOrientation(orientation, animation);
        }
        
        if(mTimerSnapManager != null) {
            mTimerSnapManager.setOrientation(orientation, animation);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMediaProviderClient != null) {
            mMediaProviderClient.release();
            mMediaProviderClient = null;
        }
    }

    private void checkStorage() {
        mStorageSpace = Storage.getAvailableSpace();
        updateStorageHint(mStorageSpace, mOrientationCompensation);
    }

    @OnClickAttr
    public void onThumbnailClicked(View v) {
        // BEGIN: Added by zhanghongxing at 2013-08-07
        if (mCameraState == FOCUSING) {
            cancelAutoFocus();
            mNeedAutoFocus = true;
        }
        // END:   Added by zhanghongxing at 2013-08-07

        if (isCameraIdle() && mThumbnail != null) {
            if (mImageSaver != null) mImageSaver.waitDone();
            gotoGallery();
        }
    }

    // onClick handler for R.id.btn_retake
    @OnClickAttr
    public void onReviewRetakeClicked(View v) {
        if (mPaused) return;
        if(mShutterButton != null) {
            mShutterButton.setEnabled(false);
       }
        hidePostCaptureAlert();
        startPreview();
        setCameraState(IDLE);
        startFaceDetection();

        // if retake, show the indicators icon on screen top-left.
        //updateOnScreenIndicators();
    }

    // onClick handler for R.id.btn_done
    @OnClickAttr
    public void onReviewDoneClicked(View v) {
        doAttach();
    }

    // onClick handler for R.id.btn_cancel
    @OnClickAttr
    public void onReviewCancelClicked(View v) {
        doCancel();
    }

    //BEGIN: Added by zhanghongxing at 2013-01-09 for full preview
    @OnClickAttr
    public void onSwitchToCameraClicked(View v) {
         //switchToOtherMode(ModePicker.MODE_CAMERA);
        switchToOtherMode(ModePicker.MODE_VIDEO);
    }

    @OnClickAttr
    public void onSwitchToVideoClicked(View v) {
        switchToOtherMode(ModePicker.MODE_VIDEO);
    }

    @OnClickAttr
    public void onSwitchToPanoramaClicked(View v) {
        switchToOtherMode(ModePicker.MODE_PANORAMA);
    }
    //END:   Added by zhanghongxing at 2012-01-09

    private void doAttach() {
        if (mPaused) {
            return;
        }

        byte[] data = mJpegImageData;

        if (mCropValue == null) {
            // First handle the no crop case -- just return the value.  If the
            // caller specifies a "save uri" then write the data to its
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (mSaveUri != null) {
                    String pictureFormat = mParameters.get(KEY_PICTURE_FORMAT);
                    String title = mSaveUri.toString();
                    int orientation = Exif.getOrientation(data);
                    Size s = mParameters.getPictureSize();
                    int width, height;
                    if ((mJpegRotation + orientation) % 180 == 0) {
                        width = s.width;
                        height = s.height;
                    } else {
                        width = s.height;
                        height = s.width;
                    }
                    boolean ok = Storage.updateImageByUri(mContentResolver, mSaveUri, null,
                            orientation, data, width, height, pictureFormat);
                    if(!ok) {
                        setResultEx(Activity.RESULT_CANCELED);
                        finish();
                        return;
                    }
                    Uri newUri= Storage.updateUri();
                    int ratio = (int) Math.ceil((double) width / mThumbnailViewWidth);
                    int inSampleSize = Integer.highestOneBit(ratio);
                    Thumbnail newt = Thumbnail.createThumbnail(
                            data, orientation, inSampleSize, newUri);
                    final File filesDir = getFilesDir();
                    newt.saveLastThumbnailToFile(filesDir);
                    setResultEx(RESULT_OK);
                    finish();

            } else {
                int orientation = Exif.getOrientation(data);
                Bitmap bitmap = Util.makeBitmap(data, 50 * 1024);
                bitmap = Util.rotate(bitmap, orientation);
                setResultEx(RESULT_OK,
                        new Intent("inline-data").putExtra("data", bitmap));
                finish();
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            Uri tempUri = null;
            FileOutputStream tempStream = null;
            try {
                File path = getFileStreamPath(sTempCropFilename);
                path.delete();
                tempStream = openFileOutput(sTempCropFilename, 0);
                tempStream.write(data);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                setResultEx(Activity.RESULT_CANCELED);
                finish();
                return;
            } catch (IOException ex) {
                setResultEx(Activity.RESULT_CANCELED);
                finish();
                return;
            } finally {
                Util.closeSilently(tempStream);
            }

            Bundle newExtras = new Bundle();
            if (mCropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            if (mSaveUri != null) {
                newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, mSaveUri);
            } else {
                newExtras.putBoolean("return-data", true);
            }

            Intent cropIntent = new Intent("com.android.camera.action.CROP");

            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            startActivityForResult(cropIntent, REQUEST_CROP);
        }
    }

    private void doCancel() {
        setResultEx(RESULT_CANCELED, new Intent());
        finish();
    }

   //wangbin
   public void doTimerSnap() {
       //take picture
       mFocusManager.doSnap();
//       setCameraState(IDLE);
   }
    @Override
    public void onShutterButtonFocus(boolean pressed) {
        if (mPaused || (mCameraState == SNAPSHOT_IN_PROGRESS)
                || (mCameraState == PREVIEW_STOPPED)) return;

        // collapse camera controls
        collapseCameraControls();
        // Do not do focus if there is not enough storage.
        if (pressed && !canTakePicture()) return;

        // BEGIN: Added by zhanghongxing at 2013-06-22
        if (mNeedShutterFocus || !mIsGravitySensorWorking) {
            mNeedShutterFocus = false;
        } else {
            return;
        }
        // END:   Added by zhanghongxing at 2013-06-22
        
        //BEGIN: Modified by wangbin at 2013-7-2 for IWB-250
        mIsCancelAF = getResources().getBoolean(R.bool.reset_picturesize);
        if (mIsCancelAF) {
            if (!(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(mFocusManager.getFocusMode()))) {
                if (pressed) {
                    mFocusManager.onShutterDown();
                } else {
                    mFocusManager.onShutterUp();
                }
            }
        } else {
            if (pressed) {
                mFocusManager.onShutterDown();
            } else {
                mFocusManager.onShutterUp();
            }
        }

        // if (pressed) {
        //     mFocusManager.onShutterDown();
        // } else {
        //     mFocusManager.onShutterUp();
        // }
        //END:Modified by wangbin at 2013-7-2 for IWB-205.
    }

    @Override
    public void onShutterButtonClick() {
        if(mContinueShutterStarted) {
            mContinueShutterStarted = false;
            if(continueTakePicState == ContinueShutterState.SHUTTING) {
                handleAfterContinueSnap();
            }
            return;
        }
        if (mPaused || collapseCameraControls()
                || (mCameraState == SWITCHING_CAMERA)
                || (mCameraState == CHANGING_STORAGE_STATE) //Added by xiongzhu at 2013-04-15
                || (mCameraState == PREVIEW_STOPPED)
                || (mCameraState == SNAPSHOT_IN_PROGRESS)
                || (mCameraState == TIMER_SNAP_IN_PROGRESS)
                || continueTakePicState != ContinueShutterState.NONE) return;
        
        // Do not take the picture if there is not enough storage.
        //BEGIN: Modified by zhanghongxing at 2013-01-07 for DER-449
        mStorageFoceChanged = false;
        if(!checkAndHandleAvailableSpace()) return;

        if(mSnapshotMode == CameraInfo.CAMERA_SUPPORT_MODE_ZSL) {
            mFocusManager.setZslEnable(true);
        }else{
            mFocusManager.setZslEnable(false);
        }
        mShutterButton.setEnabled(false);

        if (mZSLandHDRFlag) {
            String errorMsg = getString(R.string.both_ZSL_HDR_is_on);
            Toast.makeText(Camera.this,errorMsg , Toast.LENGTH_SHORT).show();
            mZSLandHDRFlag = false;
            // Currently HDR is not supported under ZSL mode
            //mIndicatorControlContainer.reloadPreferences();
            mSecondLevelIndicatorControlBar.reloadPreferences();
        } else if (mCEffAndHDRFlag) {
            String errorMsg = getString(R.string.both_COLOR_HDR_is_on);
            Toast.makeText(Camera.this,errorMsg , Toast.LENGTH_SHORT).show();
            mCEffAndHDRFlag = false;
            //mIndicatorControlContainer.reloadPreferences();
            mSecondLevelIndicatorControlBar.reloadPreferences();
        }

        // If the user wants to do a snapshot while the previous one is still
        // in progress, remember the fact and do it after we finish the previous
        // one and re-start the preview. Snapshot in progress also includes the
        // state that autofocus is focusing and a picture will be taken when
        // focus callback arrives.
        
        if ((mFocusManager.isFocusingSnapOnFinish() || mCameraState == SNAPSHOT_IN_PROGRESS)
                && !mIsImageCaptureIntent) {
//            mSnapshotOnIdle = true;
            return;
        }

        mSnapshotOnIdle = false;
        if(!(mTimerSnap.equals("off"))) {
             int mTimerSnapSec = 0;
             try{
                mTimerSnapSec = Integer.parseInt(mTimerSnap);
             }catch(Exception npe){
                   Log.v(TAG,"mTimerSnapSec = " + mTimerSnapSec);
             }
             if (mTimerSnapSec > 0) {
                 setCameraState(TIMER_SNAP_IN_PROGRESS);
                 if(mTimerSnapManager == null)
                     mTimerSnapManager = new TimerSnapManager(this);
                 mTimerSnapManager.startToSnap(mTimerSnapSec);
                    return;
             }
       }
        if(mStorageFoceChanged) {
            mStorageFoceChanged = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFocusManager.doSnap();
                }
            }, 500);
            
            return;
        }
        Log.i(TAG,"zzw_doSnap()");
        mFocusManager.doSnap();
    }

    
    private boolean checkAndHandleAvailableSpace() {
        mStorageSpace = Storage.getAvailableSpace();
        if (mStorageSpace <= Storage.LOW_STORAGE_THRESHOLD) {
            if(mIsImageCaptureIntent) {
                new RotateTextToast(Camera.this,R.string.sdcard_and_phone_memory_not_enough, 
                        mOrientationCompensation).show();
                return false;
            }
            if(!FxEnvironment.isSingleSdcard()) {
                String storagePlace;
                boolean oldStorage = Storage.mIsExternalStorage;
                if(Storage.mIsExternalStorage) {
                    //change to internal sdcard
                    Storage.mIsExternalStorage = false;
                    storagePlace = CameraSettings.CAMERA_STORAGE_MEMORY;
                } else {
                    //change to external sdcard
                    Storage.mIsExternalStorage = true;
                    if(mStorageSpace == Storage.UNAVAILABLE) {
                        Storage.mIsExternalStorage = oldStorage;
                        new RotateTextToast(Camera.this, R.string.phone_memory_not_enough, 
                                mOrientationCompensation).show();
                        return false;
                    }
                    storagePlace = CameraSettings.CAMERA_STORAGE_SDCARD;
                }

                if(Storage.getAvailableSpace()<= Storage.LOW_STORAGE_THRESHOLD) {
                    Storage.mIsExternalStorage = oldStorage;
                    new RotateTextToast(Camera.this, R.string.sdcard_and_phone_memory_not_enough, 
                            mOrientationCompensation).show();
                    return false;
                } 
                    
                mStorageFoceChanged = true;
                Editor editor = mPreferences.edit();
                editor.putString(CameraSettings.KEY_CAMERA_STORAGE_PLACE, storagePlace);
                editor.apply();
                if (mSecondLevelIndicatorControlBar != null) {
                       mSecondLevelIndicatorControlBar.reloadPreferences();
                        setCameraState(CHANGING_STORAGE_STATE);
                        mStoragePlace=storagePlace;
                        if(mStoragePlace.equals(CameraSettings.CAMERA_STORAGE_SDCARD)) {
                            new RotateTextToast(Camera.this, R.string.storageplace_sdcard,
                                    mOrientationCompensation).show();
                        }else {
                            new RotateTextToast(Camera.this, R.string.storageplace_memory, 
                                    mOrientationCompensation).show();
                        }
                        installIntentFilter();
                        checkStorage();
                        if (mSurfaceTexture != null) {
                            mCameraScreenNail.releaseSurfaceTexture();
                            mSurfaceTexture = null;
                        }
                        updateCameraScreenNail(!mIsImageCaptureIntent,Storage.mIsExternalStorage);
                        changeStoragePlace();
                        stopPreview();
                        startPreview();
                        mCameraScreenNail.changeStoragePlace();
                        setCameraState(IDLE);
                        startFaceDetection();
                        if (!mIsImageCaptureIntent) {
                            getLastThumbnail();
                        }      
                }
                
            } else {
                new RotateTextToast(Camera.this, R.string.phone_memory_not_enough, mOrientationCompensation).show();
                return false;
            }
        }
        return true;
    }
    private void handleAfterContinueSnap() {
        continueTakePicState = ContinueShutterState.SHUTTINGFINISH;
        if(continueTakePicState != ContinueShutterState.SAVING) {
            continueTakePicState = ContinueShutterState.SAVING;
            if(mTimerSnapManager != null) {
                mTimerSnapManager.clearTimerAnima();
                mTimerSnapManager.hideContinueSnapNum();
            }
            mSaveProgressBarLayout.setVisibility(View.VISIBLE);
            Message msg = Message.obtain(mHandler,SAVE_CACHED_IMAGE);
            msg.sendToTarget();
        }
    }
    private void installIntentFilter() {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        if(Storage.mIsExternalStorage){
            // install an intent filter to receive SD card related events.
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

    @Override
    protected void onResume() {
        Log.i(TAG,"==zzw:onResume()");
        if (mVolumeKeyMode.equals(CameraSettings.VOLUME_KEY_VALUE_SNAP)) {
            canDoVolumeSnap = true;
        }
        mOldPictureMode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                getString(R.string.pref_camera_takepicture_mode_default));
        mPostCaptureAlertFlag = false;
        if ((mPaused == true) && (mShutterButton != null)) mShutterButton.setEnabled(false);//add by xiongzhu for ARD-1105
        mPaused = false;
        super.onResume();
        if(mOpenCameraFail) {
            if(!mHandler.hasMessages(OPEN_CAMERA_FAIL))
                mHandler.sendEmptyMessage(OPEN_CAMERA_FAIL);
            return;
        }
        if (mOpenCameraFail || mCameraDisabled) return;

        mJpegPictureCallbackTime = 0;
        mZoomValue = 0;

        //BEGIN: Added by zhanghongxing at 2013-01-06 for DER-173
        RecordLocationPreference.mKey = RecordLocationPreference.KEY;
        //END:   Added by zhanghongxing at 2013-01-06

        // Start the preview if it is not started.
        if (mCameraState == PREVIEW_STOPPED && mCameraStartUpThread == null) {
            mStartPreviewPrerequisiteReady.close();
//            resetExposureCompensation();
            mCameraStartUpThread = new CameraStartUpThread();
            mCameraStartUpThread.start();
            mStartPreviewPrerequisiteReady.open();
        }
        //Check if the skinTone SeekBar could not be enabled during updateCameraParametersPreference()
       //due to the finite latency of loading the seekBar layout when switching modes
       // for same Camera Device instance
        if (mSkinToneSeekBar != true)
        {
            Log.e(TAG, "Send tone bar: mSkinToneSeekBar = " + mSkinToneSeekBar);
            mHandler.sendEmptyMessageDelayed(SET_SKIN_TONE_FACTOR, SET_SKIN_TONE_FACTOR_DELAY);
        }

        // If first time initialization is not finished, put it in the
        // message queue.
        if (!mFirstTimeInitialized) {
            mHandler.sendEmptyMessage(FIRST_TIME_INIT);
        } else {
            initializeSecondTime();
        }
        keepScreenOnAwhile();

        // Dismiss open menu if exists.
        PopupManager.getInstance(this).notifyShowPopup(null);

        if (mCameraSound == null) {
            mCameraSound = new MediaActionSound();
            // Not required, but reduces latency when playback is requested later.
            mCameraSound.load(MediaActionSound.FOCUS_COMPLETE);
        }

        if (mCameraAppView != null && mCameraAppView.getVisibility() != View.VISIBLE
                && mShowCameraAppView) {
            updateCameraAppView();
            updateThumbnailView();
        }

        //BEGIN: Modified by zhanghongxing at 2013-05-28
        String storagePlace = mPreferences.getString(CameraSettings.KEY_CAMERA_STORAGE_PLACE,
                getString(R.string.pref_storage_place_default));
        Storage.mIsExternalStorage = storagePlace.equals( CameraSettings.CAMERA_STORAGE_SDCARD ) ? true : false;
        
        String internalPath = FxEnvironment.getInternalStorageDirectory(this);
        String externalPath = FxEnvironment.getExternalStorageDirectory(this);
        if (mIsImageCaptureIntent && mSaveUri != null) {
            String savePath = mSaveUri.getPath();
            if ((externalPath != null)
                    && savePath.indexOf(externalPath) != -1) {
                Storage.mIsExternalStorage = true;
            }else if ((internalPath != null)
                    && savePath.indexOf(internalPath) != -1){
                Storage.mIsExternalStorage = false;
            } else {
                Log.e(TAG, "Not found sdcard path!");
            }
        }
        installIntentFilter(); 
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkStorage();
            }
        }, 200);
        if (mThumbnailView != null) {
            if (!mIsImageCaptureIntent)
                getLastThumbnail();
        }
        
        // BEGIN: Added by zhanghongxing at 2013-06-22
        mIsGravitySensorWorking = getResources().getBoolean(R.bool.gravity_sensor_working);
        
        if (mIsGravitySensorWorking && mAutoFocusManager != null) {
            mAutoFocusManager.register();
        }
        // END:   Added by zhanghongxing at 2013-06-22
        
        // Start location update if needed.
        mIsRecordLocation = false;
        qureyAndSetGpsState();

        if(mNeedReloadPreference) {
            mNeedReloadPreference = false;
            mIsNeedUpdateSceneUI = true;
            updateSceneModeUI();
        }
    }

    void waitCameraStartUpThread() {
        try {
            if (mCameraStartUpThread != null) {
                mCameraStartUpThread.cancel();
                mCameraStartUpThread.join();
                mCameraStartUpThread = null;
                setCameraState(IDLE);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG,"==zzw:onPause() E");
        mPaused = true;
        if(mContinueShutterStarted) {
            mContinueShutterStarted = false;
            if(continueTakePicState == ContinueShutterState.SHUTTING) {
                mCameraDevice.getCamera().setLongshot(false);
                continueTakePicState = ContinueShutterState.NONE;
            }
            if(mTimerSnapManager != null) {
                mTimerSnapManager.clearTimerAnima();
                mTimerSnapManager.hideContinueSnapNum();
            }
            mSaveProgressBarLayout.setVisibility(View.GONE);
        }
        super.onPause();
        mStartPreviewPrerequisiteReady.open();
        // If Camera application exit then close the dialog.
        if (mRotateDialog != null && mRotateDialog.isRotateDialogVisible()) {
            mRotateDialog.dismissDialog();
        }

        // Wait the camera start up thread to finish.
        waitCameraStartUpThread();
        if(mGraphView != null)
            mGraphView.setCameraObject(null);

        stopPreview();
        // Close the camera now because other activities may need to use it.
        closeCamera();
        if (mSurfaceTexture != null) {
            mCameraScreenNail.releaseSurfaceTexture();
            mSurfaceTexture = null;
        }
        if (mCameraSound != null) {
            mCameraSound.release();
            mCameraSound = null;
        }
        resetScreenOn();

        // Clear UI.
        collapseCameraControls();
        if (mFaceView != null) mFaceView.clear();

        if (mFirstTimeInitialized) {
            mOrientationListener.disable();
            if (mImageSaver != null) {
                mImageSaver.finish();
                mImageSaver = null;
                mImageNamer.finish();
                mImageNamer = null;
            }
        }
        
        // BEGIN: Added by zhanghongxing at 2013-06-22
        if (mIsGravitySensorWorking && mAutoFocusManager != null) {
            mAutoFocusManager.unregister();
        }
        // END:   Added by zhanghongxing at 2013-06-22
        
        if (mDidRegister) {
            unregisterReceiver(mReceiver);
            mDidRegister = false;
        }
        if (mLocationManager != null) mLocationManager.recordLocation(false);
        updateExposureOnScreenIndicator(0);

        // If we are in an image capture intent and has taken
        // a picture, we just clear it in onPause.
        mJpegImageData = null;

        // Remove the messages in the event queue.
        mHandler.removeMessages(FIRST_TIME_INIT);
        mHandler.removeMessages(CHECK_DISPLAY_ROTATION);
        mHandler.removeMessages(SWITCH_CAMERA);
        mHandler.removeMessages(SWITCH_CAMERA_START_ANIMATION);
        mHandler.removeMessages(CAMERA_OPEN_DONE);
        mHandler.removeMessages(START_PREVIEW_DONE);
//        mHandler.removeMessages(OPEN_CAMERA_FAIL);
//        mHandler.removeMessages(CAMERA_DISABLED);
        mHandler.removeMessages(SWITCH_CAMERA_ANIMATION_DONE);

//        if (mCameraPicker != null) mCameraPicker.setClickable(true);
        mPendingSwitchCameraId = -1;
        if (mFocusManager != null) mFocusManager.removeMessages();
        
        //START: add by xiongzhu for cbb
        String takePictureMode =mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                        getString(R.string.pref_camera_takepicture_mode_default));
        if (takePictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_panorama)) ||
                takePictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
        ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_COLOR_EFFECT);
        if(pref != null) ((IconListPreference) pref).setAvailable(true);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,getString(R.string.pref_camera_takepicture_mode_default));
        editor.apply();
        mNeedReloadPreference = true;
      }
     
        if(mTimerSnapManager != null) {
            mTimerSnapManager.clearTimerAnima();
            mTimerSnapManager.hideContinueSnapNum();
        }
        Log.i(TAG,"==zzw:onPause() X");
    }

    private void initializeControlByIntent() {
        if (mIsImageCaptureIntent) {
            // Cannot use RotateImageView for "done" and "cancel" button because
            // the tablet layout uses RotateLayout, which cannot be cast to
            // RotateImageView.
            mReviewDoneButton = (Rotatable) findViewById(R.id.btn_done);
            mReviewCancelButton = (Rotatable) findViewById(R.id.btn_cancel);
            mReviewRetakeButton = findViewById(R.id.btn_retake);
            findViewById(R.id.btn_cancel).setVisibility(View.VISIBLE);

            // Not grayed out upon disabled, to make the follow-up fade-out
            // effect look smooth. Note that the review done button in tablet
            // layout is not a TwoStateImageView.
            if (mReviewDoneButton instanceof TwoStateImageView) {
                ((TwoStateImageView) mReviewDoneButton).enableFilter(false);
            }

            setupCaptureParams();
            mThumbnailView = (RotateImageView) findViewById(R.id.thumbnail);
            mThumbnailViewWidth = mThumbnailView.getLayoutParams().width;
        } else {
            //BEGIN: Added by zhanghongxing at 2013-01-09 for full preview
            RelativeLayout thumbnailFrame = (RelativeLayout) findViewById(R.id.thumbnail_frame);
            thumbnailFrame.setVisibility(View.VISIBLE);
            mThumbnailWindow = (RotateImageView) findViewById(R.id.thumbnail_bg);
            //END:   Added by zhanghongxing at 2013-01-09
            mThumbnailView = (RotateImageView) findViewById(R.id.thumbnail);
            mThumbnailBgView = (RotateImageView) findViewById(R.id.thumbnail_bg_null);
            mThumbnailView.enableFilter(false);
            mThumbnailView.setVisibility(View.VISIBLE);
            mThumbnailViewWidth = mThumbnailView.getLayoutParams().width;

            //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
            // mModePicker = (ModePicker) findViewById(R.id.mode_picker);
            // mModePicker.setVisibility(View.VISIBLE);
            // mModePicker.setOnModeChangeListener(this);
            // mModePicker.setCurrentMode(ModePicker.MODE_CAMERA);
            RelativeLayout modeSwitcher = (RelativeLayout) findViewById(R.id.mode_switcher);
            modeSwitcher.setVisibility(View.VISIBLE);
            mSwitchToCamera = (RotateImageView) findViewById(R.id.mode_camera);
            mSwitchToCamera.enableFilter(false);
            mSwitchToCamera.setImageResource(R.drawable.ic_switch_camera_focused);
            mSwitchToCamera.setEnabled(false);
            mSwitchToVideo = (RotateImageView) findViewById(R.id.mode_video);
            mSwitchToVideo.enableFilter(false);
//            mSwitchToVideo.setImageResource(R.drawable.ic_switch_video_dark);
            /**mSwitchToPanorama = (RotateImageView) findViewById(R.id.mode_pano); delete by xiongzhu for cbb
            mSwitchToPanorama.enableFilter(false);
            mSwitchToPanorama.setImageResource(R.drawable.ic_switch_pan_dark);*/ 
            //END:   Modified by zhanghongxing at 2013-01-09
        }
    }

    private void initializeFocusManager() {
        // Create FocusManager object. startPreview needs it.
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        boolean mirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
        String[] defaultFocusModes = getResources().getStringArray(
                R.array.pref_camera_focusmode_default_array);
        mFocusManager = new FocusManager(mPreferences, defaultFocusModes,
                mFocusAreaIndicator, mInitialParams, this, mirror,
                getMainLooper());
    }

    private void initializeMiscControls() {
        // startPreview needs this.
        mPreviewFrameLayout = (PreviewFrameLayout) findViewById(R.id.frame);
        // Set touch focus listener.
        setSingleTapUpListener(mPreviewFrameLayout);

        mZoomControl = (ZoomControl) findViewById(R.id.zoom_control);
        mOnScreenIndicators = (Rotatable) findViewById(R.id.on_screen_indicators);
        mFaceView = (FaceView) findViewById(R.id.face_view);
        mPreviewFrameLayout.addOnLayoutChangeListener(this);
        mPreviewFrameLayout.setOnSizeChangedListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setDisplayOrientation();
        setPreviewFrameLayoutOrientation();
        // Change layout in response to configuration change
        LinearLayout appRoot = (LinearLayout) findViewById(R.id.camera_app_root);
        appRoot.setOrientation(
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        appRoot.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(R.layout.preview_frame, appRoot);
        //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
        // inflater.inflate(R.layout.camera_control, appRoot);
        //END:   Deleted by zhanghongxing at 2013-01-09

        // Reload layout, reset linear
        resetLinear();
        mSeekBarInitialized = true;
        // from onCreate()
        initializeControlByIntent();
        initializeFocusManager();
        initializeMiscControls();
        initializeIndicatorControl();
        mFocusAreaIndicator = (RotateLayout) findViewById(
                R.id.focus_indicator_rotate_layout);
        mFocusManager.setFocusAreaIndicator(mFocusAreaIndicator);

        // from onResume()
        if (!mIsImageCaptureIntent) updateThumbnailView();

        // from initializeFirstTime()
        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.setOnLongClickListener(this);
        mShutterButton.setVisibility(View.VISIBLE);
        initializeZoom();
        initOnScreenIndicator();
        boolean recordLocation = RecordLocationPreference.get(
                mPreferences, mContentResolver);
        mIsRecordLocation = recordLocation;
        if (recordLocation) {
            showGpsOnScreenIndicator(LocationManager.hasGpsSignal);
        }
        //updateOnScreenIndicators();
        mFaceView.clear();
        mFaceView.setVisibility(View.VISIBLE);
        mFaceView.setDisplayOrientation(mDisplayOrientation);
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        mFaceView.setMirror(info.facing == CameraInfo.CAMERA_FACING_FRONT);
        mFaceView.resume();
        mFocusManager.setFaceView(mFaceView);
        // If camera was started by capture intent and called showPostCaptureAlert already,
        // we need refresh buttons after rotate device.
        if (mIsImageCaptureIntent && mPostCaptureAlertFlag) {
            showPostCaptureAlert();
        }
        //BEGIN: Added by zhanghongxing at 2013-01-11 for full preview
        setPreviewFrameLayoutAspectRatio();
        //END:   Added by zhanghongxing at 2013-01-11

        mLastOrientation = newConfig.orientation;
    }

    private void resetLinear() {
        resetBrightnessProgressBar();
        resetSkinToneSeekBar();
        Title = resetTextView(Title, R.id.skintonetitle);
        RightValue = resetTextView(RightValue, R.id.skintoneright);
        LeftValue = resetTextView(LeftValue, R.id.skintoneleft);
    }

    private TextView resetTextView(TextView textView, int resId) {
       TextView tv = (TextView) findViewById(resId);
       tv.setText(getTextViewText(textView));
       tv.setVisibility(getTextViewVisibility(textView));
       return tv;
    }

    private CharSequence getTextViewText(TextView textView) {
       if (null != textView) {
          return textView.getText();
       }

       return null;
    }

    private int getTextViewVisibility(TextView textView) {
       if (null != textView) {
           return textView.getVisibility();
       }

       return View.INVISIBLE;
    }

    private void resetBrightnessProgressBar() {
        ProgressBar proBar = (ProgressBar) findViewById(R.id.progress);
        if (proBar instanceof SeekBar) {
              SeekBar seeker = (SeekBar) brightnessProgressBar;
                      seeker.setOnSeekBarChangeListener(mSeekListener);
        }

       proBar = (ProgressBar) findViewById(R.id.progress);
       proBar.setMax(getProgressBarMaxProgress(brightnessProgressBar, MAXIMUM_BRIGHTNESS));
       proBar.setProgress(getProgressBarProgress(brightnessProgressBar, mbrightness));
       proBar.setVisibility(getProgressBarVisibility(brightnessProgressBar));
       brightnessProgressBar = proBar;
       proBar = null;
    }

    private void resetSkinToneSeekBar() {
        SeekBar seekBar = (SeekBar) findViewById(R.id.skintoneseek);
        seekBar.setOnSeekBarChangeListener(mskinToneSeekListener);
        seekBar.setMax(getProgressBarMaxProgress(skinToneSeekBar, MAX_SCE_FACTOR-MIN_SCE_FACTOR));
        seekBar.setProgress(getProgressBarProgress(skinToneSeekBar, 0));
        seekBar.setVisibility(getProgressBarVisibility(skinToneSeekBar));
        skinToneSeekBar = seekBar;
        seekBar = null;
    }

    private int getProgressBarProgress(ProgressBar proBar, int defaultVal) {
        if (null != proBar) {
            return proBar.getProgress();
        }

        return defaultVal;
    }

    private int getProgressBarVisibility(ProgressBar proBar) {
        if (null != proBar) {
            return proBar.getVisibility();
        }

        return View.INVISIBLE;
    }

    private int getProgressBarMaxProgress(ProgressBar proBar, int defaultVal) {
        if (null != proBar) {
            return proBar.getMax();
        }

        return defaultVal;
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CROP: {
                Intent intent = new Intent();
                if (data != null) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        intent.putExtras(extras);
                    }
                }
                setResultEx(resultCode, intent);
                finish();

                File path = getFileStreamPath(sTempCropFilename);
                path.delete();

                break;
            }
            
            case RESTOREGPS_STATE: {
                ContentResolver resolver = getContentResolver();
                boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(resolver,"gps");
                boolean networkEnabled = Settings.Secure.isLocationProviderEnabled(resolver,"network");
                mIsRecordLocation = true;
                if (!(gpsEnabled || networkEnabled)) {
                    restoreGPSState();
                }
                break;
            }
        }
    }

    private boolean canTakePicture() {
        return isCameraIdle() && (mStorageSpace > Storage.LOW_STORAGE_THRESHOLD);
    }

    protected CameraManager.CameraProxy getCamera() {
        return mCameraDevice;
    }

    @Override
    public void autoFocus() {
        mFocusStartTime = System.currentTimeMillis();
        
        //BEGIN: Added by zhanghongxing at 2013-06-22
        if (mIsGravitySensorWorking && mAutoFocusManager != null) {
            mAutoFocusManager.reset();
        }
        //END:   Added by zhanghongxing at 2013-06-22
        
        mCameraDevice.autoFocus(mAutoFocusCallback);
        setCameraState(FOCUSING);
    }

    // BEGIN: Added by zhanghongxing at 2013-07-24
    @Override
    public void silentAutoFocus() {
        mFocusStartTime = System.currentTimeMillis();

        if (mIsGravitySensorWorking && mAutoFocusManager != null) {
            mAutoFocusManager.reset();
        }

        mCameraDevice.autoFocus(mSilentAutoFocusCallback);
        setCameraState(FOCUSING);
    }
    // END:   Added by zhanghongxing at 2013-07-24

    @Override
    public void cancelAutoFocus() {
        mCameraDevice.cancelAutoFocus();
        if(mCameraState != TIMER_SNAP_IN_PROGRESS) {
            setCameraState(IDLE);
        }
        setCameraParameters(UPDATE_PARAM_PREFERENCE);
    }

    // Preview area is touched. Handle touch focus.
    @Override
    protected void onSingleTapUp(View view, int x, int y) {
        if (mPaused || mCameraDevice == null || !mFirstTimeInitialized
                || mCameraState == SNAPSHOT_IN_PROGRESS
                || mCameraState == SWITCHING_CAMERA
                || mCameraState == CHANGING_STORAGE_STATE //Added by xiongzhu at 2013-04-15
                || mCameraState == PREVIEW_STOPPED
                || mCameraState == TIMER_SNAP_IN_PROGRESS) {
            return;
        }

        //If  showPostCaptureAlert, return
        if (mPostCaptureAlertFlag == true) return;

        // Do not trigger touch focus if popup window is opened.
        if (collapseCameraControls()) return;

        //If Touch AF/AEC is disabled in UI, return
        if(this.mTouchAfAecFlag == false) {
            return;
        }
        // Check if metering area or focus area is supported.
        if (!mFocusAreaSupported && !mMeteringAreaSupported) return;

        mFocusManager.onSingleTapUp(x, y);
    }

    @Override
    public void onBackPressed() {
        if (!isCameraIdle()) {
            // BEGIN: Added by zhanghongxing at 2013-08-07
            // ignore backs while we're taking a picture
            // return;
            if (mCameraState == FOCUSING) {
                cancelAutoFocus();
            } else {
                return;
            }
            // END:   Added by zhanghongxing at 2013-08-07
        }

        if (!collapseCameraControls()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    onShutterButtonFocus(true);
                }
                return true;
            case KeyEvent.KEYCODE_CAMERA:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    onShutterButtonClick();
                }
                return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if ( (mCameraState != PREVIEW_STOPPED) &&
                            (mFocusManager.getCurrentFocusState() != mFocusManager.STATE_FOCUSING) &&
                            (mFocusManager.getCurrentFocusState() != mFocusManager.STATE_FOCUSING_SNAP_ON_FINISH) ) {
                        if (mbrightness > MINIMUM_BRIGHTNESS) {
                            mbrightness-=mbrightness_step;

                            /* Set the "luma-adaptation" parameter */
                            mParameters = mCameraDevice.getParameters();
                            mParameters.set("luma-adaptation", String.valueOf(mbrightness));
                            mCameraDevice.setParameters(mParameters);
                        }

                        brightnessProgressBar.setProgress(mbrightness);
                        brightnessProgressBar.setVisibility(View.VISIBLE);

                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if ( (mCameraState != PREVIEW_STOPPED) &&
                            (mFocusManager.getCurrentFocusState() != mFocusManager.STATE_FOCUSING) &&
                            (mFocusManager.getCurrentFocusState() != mFocusManager.STATE_FOCUSING_SNAP_ON_FINISH) ) {
                        if (mbrightness < MAXIMUM_BRIGHTNESS) {
                            mbrightness+=mbrightness_step;

                            /* Set the "luma-adaptation" parameter */
                            mParameters = mCameraDevice.getParameters();
                            mParameters.set("luma-adaptation", String.valueOf(mbrightness));
                            mCameraDevice.setParameters(mParameters);

                        }
                        brightnessProgressBar.setProgress(mbrightness);
                        brightnessProgressBar.setVisibility(View.VISIBLE);

                    }
                    break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // If we get a dpad center event without any focused view, move
                // the focus to the shutter button and press it.
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    // Start auto-focus immediately to reduce shutter lag. After
                    // the shutter button gets the focus, onShutterButtonFocus()
                    // will be called again but it is fine.
                    if (collapseCameraControls()) return true;
                    onShutterButtonFocus(true);
                    if (mShutterButton.isInTouchMode()) {
                        mShutterButton.requestFocusFromTouch();
                    } else {
                        mShutterButton.requestFocus();
                    }
                    mShutterButton.setPressed(true);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mAppBridge.isCurrentCameraPreview() && !mIsImageCaptureIntent) {
                    if (mVolumeKeyMode.equals(CameraSettings.VOLUME_KEY_VALUE_SNAP)) {
                        if(canDoVolumeSnap) {
                            onShutterButtonClick();
                        }
                        return true;
                    }
                    if (mVolumeKeyMode.equals(CameraSettings.VOLUME_KEY_VALUE_ZOOM)) {
                        if(mZoomValue > 0) {
                            mZoomValue -= 6;
                            if (mZoomValue < 0) {
                                mZoomValue = 0;
                            }
                            processZoomValueChanged();
                        }
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mAppBridge.isCurrentCameraPreview() && !mIsImageCaptureIntent) {
                    if (mVolumeKeyMode.equals(CameraSettings.VOLUME_KEY_VALUE_SNAP)) {
                        if(canDoVolumeSnap) {
                            onShutterButtonClick();
                        }
                        return true;
                    }
                    if (mVolumeKeyMode.equals(CameraSettings.VOLUME_KEY_VALUE_ZOOM)) {
                        if((mZoomValue < mZoomMax)) {
                            mZoomValue += 6;
                            if (mZoomValue > mZoomMax) {
                                mZoomValue = mZoomMax;
                            }
                            processZoomValueChanged();
                        }
                        return true;
                    }
                }
                break;
                //power key shutter doesn't work in current version
            case KeyEvent.KEYCODE_POWER:
//                if (mShutterKeyMode.equals(CameraSettings.SHUTTER_KEY_VALUE_POWER)
//                        && mAppBridge.isCurrentCameraPreview()) {
//                    onShutterButtonClick();
//                    return true;
//                }
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
                if (mFirstTimeInitialized) {
                    onShutterButtonFocus(false);
                }
                return true;

            case KeyEvent.KEYCODE_BACK:
                // If the dialog display when user click back key
                // then close the dialog first.
                if (mRotateDialog != null && mRotateDialog.isRotateDialogVisible()) {
                    mRotateDialog.dismissDialog();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_POWER:
                if (mVolumeKeyMode.equals(CameraSettings.VOLUME_KEY_VALUE_SNAP)
                        || mVolumeKeyMode.equals(CameraSettings.VOLUME_KEY_VALUE_ZOOM)
                        && mAppBridge.isCurrentCameraPreview()
                        && !mIsImageCaptureIntent) {
                    mHandler.sendEmptyMessageDelayed(HIDE_ZOOM_BAR,HIDE_ZOOM_BAR_DELAY);
                    return true;
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void processZoomValueChanged() {
        if (!mIsZoomSupported) return;
        mHandler.sendEmptyMessage(SHOW_ZOOM_BAR);
        mParameters.setZoom(mZoomValue);
        mZoomControl.setZoomIndex(mParameters.getZoom());
        mCameraDevice.setParametersAsync(mParameters);
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.setZoomChangeListener(null);
            mCameraDevice.setFaceDetectionListener(null);
            mCameraDevice.setErrorCallback(null);
            CameraHolder.instance().release();
            mFaceDetectionStarted = false;
            mCameraDevice = null;
            setCameraState(PREVIEW_STOPPED);
            if(mFocusManager != null) {
                mFocusManager.onCameraReleased();
            }
        }
    }

    private void setDisplayOrientation() {
        mDisplayRotation = Util.getDisplayRotation(this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, mCameraId);
        mCameraDisplayOrientation = Util.getDisplayOrientation(0, mCameraId);
        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }
        if(mFocusManager != null)
            mFocusManager.setDisplayOrientation(mDisplayOrientation);
    }

    private void startPreview() {
        mFocusManager.resetTouchFocus();

        mCameraDevice.setErrorCallback(mErrorCallback);

        // If we're previewing already, stop the preview first (this will blank
        // the screen).
        if (mCameraState != PREVIEW_STOPPED) stopPreview();

        setDisplayOrientation();
        mCameraDevice.setDisplayOrientation(mCameraDisplayOrientation);

        if (!mSnapshotOnIdle) {
            // If the focus mode is continuous autofocus, call cancelAutoFocus to
            // resume it because it may have been paused by autoFocus call.
            if (Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(mFocusManager.getFocusMode())) {
                mCameraDevice.cancelAutoFocus();
            }
            mFocusManager.setAeAwbLock(false); // Unlock AE and AWB.
        } 
        setCameraParameters(UPDATE_PARAM_ALL);

        int oldWidth = mCameraScreenNail.getWidth();
        int oldHeight = mCameraScreenNail.getHeight();
        if(oldWidth == 0 || oldHeight == 0) {
            oldWidth  = getWindowManager().getDefaultDisplay().getWidth();
            oldHeight = getWindowManager().getDefaultDisplay().getHeight();
        }

        if(mLastOrientation != this.getResources().getConfiguration().orientation) {
            int tmp = oldWidth;
            oldWidth = oldHeight;
            oldHeight = tmp;
        }

        if (oldWidth != mCameraScreenNail.getWidth() ||
                oldHeight != mCameraScreenNail.getHeight()) {
            mCameraScreenNail.setSize(oldWidth,oldHeight);
        }
        // Should notify changed when restart camera from LockScreen
        notifyScreenNailChanged();
        if (mSurfaceTexture == null) {
            mCameraScreenNail.acquireSurfaceTexture();
            mSurfaceTexture = mCameraScreenNail.getSurfaceTexture();
        }
        mCameraDevice.setPreviewTextureSync(mSurfaceTexture);
        Log.v(TAG, "==zzw:startPreview");
        mCameraDevice.startPreviewsSync();

        mFocusManager.onPreviewStarted();

        if (mSnapshotOnIdle) {
            //BEGIN:Add by wangbin at 2013-6-27
            if (mDoSnapRunnable != null)
                mHandler.removeCallbacks(mDoSnapRunnable);
            //END :Add by wangbin at 2013-6-27
            mHandler.post(mDoSnapRunnable);
        }
        //for bug 1105
        mHandler.postDelayed(mPreviewReadyRunnable,PREVIEW_READY_DELAY);
    }

    private void stopPreview() {
        if (mCameraDevice != null && mCameraState != PREVIEW_STOPPED) {
            if(mIsLastFocusedSuc) mCameraDevice.cancelAutoFocus(); // Reset the focus.
            Log.v(TAG, "==zzw:stopPreview");
            mCameraDevice.stopPreview();
            //mFaceDetectionStarted = false;
        }
        setCameraState(PREVIEW_STOPPED);
        if (mFocusManager != null) mFocusManager.onPreviewStopped();
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    @TargetApi(18)
    private boolean isSupportHdr() {
        return isSupported("hdr",mParameters.getSupportedSceneModes());
    }
    
    private boolean isHdrSupported() {
        boolean supported = false;
        Method isHdrSupported;   
        try { 
            isHdrSupported = mParameters.getClass().getMethod("isHdrSupported"); 
        } catch (NoSuchMethodException e) { 
            e.printStackTrace(); 
            return supported;
        }
        try { 
            Boolean supportedObj = (Boolean) isHdrSupported.invoke(mParameters); 
            supported = supportedObj.booleanValue();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return supported;
    }
    
    @SuppressWarnings("deprecation")
    private void updateCameraParametersInitialize() {
        // Reset preview frame rate to the maximum because it may be lowered by
        // video camera application.
        List<Integer> frameRates = mParameters.getSupportedPreviewFrameRates();
        if (frameRates != null) {
            Integer max = Collections.max(frameRates);
            mParameters.setPreviewFrameRate(max);
        }

        mParameters.setRecordingHint(false);

        // Disable video stabilization. Convenience methods not available in API
        // level <= 14
        String vstabSupported = mParameters.get("video-stabilization-supported");
        if ("true".equals(vstabSupported)) {
            mParameters.set("video-stabilization", "false");
        }
    }

    private void updateCameraParametersZoom() {
        // Set zoom.
        if (mIsZoomSupported) {
            mParameters.setZoom(mZoomValue);
        }
    }

    private void resetZoom() {
        // Reset zoom.
        if (mIsZoomSupported) {
            mZoomValue = 0;
            setCameraParametersWhenIdle(UPDATE_PARAM_ZOOM);
            mZoomControl.setZoomIndex(0);
        }
    }

    private boolean needRestart() {
        //String zsl = mPreferences.getString(CameraSettings.KEY_ZSL,
        //                          getString(R.string.pref_camera_zsl_default));
        String mode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE, 
                getString(R.string.pref_camera_takepicture_mode_default));
        if(mCameraState != PREVIEW_STOPPED && mOldPictureMode != null) {
            if(!mOldPictureMode.equals(mode)) {    
                if((mode.equals(getString(R.string.pref_camera_takepicture_mode_value_zsl)) 
                        || mode.equals(getString(R.string.pref_camera_takepicture_mode_value_continue)))  
                        || ((mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_zsl))) 
                                || mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_continue)))) {
                    mRestartPreview = true;
                    return mRestartPreview;
                }
            }
        }
        if(mNeedToRestoreToNormalPicMode) return true;
        return mRestartPreview;
    }

    private void qcomUpdateCameraParametersPreference(){
        //qcom Related Parameter update
        //Set Brightness.
        mParameters.set("luma-adaptation", String.valueOf(mbrightness));

        if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
            // Set Touch AF/AEC parameter.
            String touchAfAec = mPreferences.getString(
                 CameraSettings.KEY_TOUCH_AF_AEC,
                 getString(R.string.pref_camera_touchafaec_default));
            if (isSupported(touchAfAec, mParameters.getSupportedTouchAfAec())) {
                mParameters.setTouchAfAec(touchAfAec);
            }
        } else {
            mParameters.setTouchAfAec(mParameters.TOUCH_AF_AEC_OFF);
            mFocusManager.resetTouchFocus();
        }
        try {
            if(mParameters.getTouchAfAec().equals(mParameters.TOUCH_AF_AEC_ON))
                this.mTouchAfAecFlag = true;
            else
                this.mTouchAfAecFlag = false;
        } catch(Exception e){
            Log.e(TAG, "Handled NULL pointer Exception");
        }

        // Set Picture Format
        // Picture Formats specified in UI should be consistent with
        // PIXEL_FORMAT_JPEG and PIXEL_FORMAT_RAW constants
        String pictureFormat = mPreferences.getString(
                CameraSettings.KEY_PICTURE_FORMAT,
                getString(R.string.pref_camera_picture_format_default));
        mParameters.set(KEY_PICTURE_FORMAT, pictureFormat);
        // Set JPEG quality.
        String jpegQuality = mPreferences.getString(
                CameraSettings.KEY_JPEG_QUALITY,
                getString(R.string.pref_camera_jpegquality_default));

        //mUnsupportedJpegQuality = false;
        Size pic_size = mParameters.getPictureSize();
        if (pic_size == null) {
            Log.e(TAG, "error getPictureSize: size is null");
        }
        else{
            if("100".equals(jpegQuality) && (pic_size.width >= 3200)){
                //mUnsupportedJpegQuality = true;
            }else {
                mParameters.setJpegQuality(JpegEncodingQualityMappings.getQualityNumber(jpegQuality));
            }
        }
        // Set Selectable Zone Af parameter.
        String selectableZoneAf = mPreferences.getString(
            CameraSettings.KEY_SELECTABLE_ZONE_AF,
            getString(R.string.pref_camera_selectablezoneaf_default));
        List<String> str = mParameters.getSupportedSelectableZoneAf();
        if (isSupported(selectableZoneAf, mParameters.getSupportedSelectableZoneAf())) {
            mParameters.setSelectableZoneAf(selectableZoneAf);
        }
        /*
        //Set LensShading
        String lensshade = mPreferences.getString(
                CameraSettings.KEY_LENSSHADING,
                getString(R.string.pref_camera_lensshading_default));
        if (isSupported(lensshade,
                mParameters.getSupportedLensShadeModes())) {
                mParameters.setLensShade(lensshade);
        }*/

        // Set color effect parameter.
        String colorEffect = mPreferences.getString(
                CameraSettings.KEY_COLOR_EFFECT,
                getString(R.string.pref_camera_coloreffect_default));
        Log.v(TAG, "Color effect value =" + colorEffect);
        if (isSupported(colorEffect, mParameters.getSupportedColorEffects())) {
            mParameters.setColorEffect(colorEffect);
        }

        mCEffAndHDRFlag = false;
        //Set AE Bracket
        String picMode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                getString(R.string.pref_camera_takepicture_mode_default));
        if(Build.VERSION.SDK_INT >= 18){
            ListPreference sceneMode = null;
            if(mPreferenceGroup != null) {
                sceneMode = mPreferenceGroup.findPreference(CameraSettings.KEY_SCENE_MODE);
            }
            if(isSupportHdr()) {
                if(picMode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
                    mParameters.setSceneMode(Parameters.SCENE_MODE_HDR);
                    mParameters.set("snapshot-burst-num", "1");
                    if(sceneMode != null) {
                        sceneMode.setEnable(false);  
                    } 
                } else {
                    mParameters.set("snapshot-burst-num", "1");
                    if(sceneMode != null) {
                        sceneMode.setEnable(true);  
                    } 
                }
            } else {
                mParameters.set("snapshot-burst-num", "1");
                if(sceneMode != null) {
                    sceneMode.setEnable(true);  
                }
            }
        } else {
            if(isHdrSupported()) {
                String hdrState = "Off";
                if(picMode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
                    hdrState = getString(R.string.pref_camera_takepicture_mode_value_hdr);
                }
                mParameters.setAEBracket(hdrState);
                if(!colorEffect.equals("none") && picMode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
                    //toast message when color effect and HDR is both opened
                    mParameters.set("num-snaps-per-shutter", "1");
                    mCEffAndHDRFlag = true;
                } else {
                    if (picMode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
                        mParameters.set("num-snaps-per-shutter", "2");
                    } else {
                        mParameters.set("num-snaps-per-shutter", "1");
                    }
                }
            } else {
                mParameters.set("num-snaps-per-shutter", "1");
            }    
        }

        // Set wavelet denoise mode
        if (mParameters.getSupportedDenoiseModes() != null) {
            String Denoise = mPreferences.getString( CameraSettings.KEY_DENOISE,
                             getString(R.string.pref_camera_denoise_default));
            
            if(mOldPictureMode != null && 
                    mOldPictureMode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_continue))) {
                Denoise = getString(R.string.pref_camera_denoise_default);
            }
            mParameters.setDenoise(Denoise);
        }
        // Set Redeye Reduction
        String redeyeReduction = mPreferences.getString(
                CameraSettings.KEY_REDEYE_REDUCTION,
                getString(R.string.pref_camera_redeyereduction_default));
        if (isSupported(redeyeReduction,
            mParameters.getSupportedRedeyeReductionModes())) {
            if(mOldPictureMode != null && 
                    mOldPictureMode.equalsIgnoreCase(
                            getString(R.string.pref_camera_takepicture_mode_value_continue))) {
                redeyeReduction = getString(R.string.pref_camera_redeyereduction_default);
            }
            mParameters.setRedeyeReductionMode(redeyeReduction);
        }
        // Set ISO parameter
        String iso = mPreferences.getString(
                CameraSettings.KEY_ISO,
                getString(R.string.pref_camera_iso_default));
        if (isSupported(iso,
                mParameters.getSupportedIsoValues())) {
                mParameters.setISOValue(iso);
        }

        //Set Saturation
        String saturationStr = mPreferences.getString(
                CameraSettings.KEY_SATURATION,
                getString(R.string.pref_camera_saturation_default));
        int saturation = Integer.parseInt(saturationStr);
        Log.v(TAG, "Saturation value =" + saturation);
        if((0 <= saturation) && (saturation <= mParameters.getMaxSaturation())){
            mParameters.setSaturation(saturation);
        }

        // Set contrast parameter.
        String contrastStr = mPreferences.getString(
                CameraSettings.KEY_CONTRAST,
                getString(R.string.pref_camera_contrast_default));
        int contrast = Integer.parseInt(contrastStr);
        Log.v(TAG, "Contrast value =" +contrast);
        if((0 <= contrast) && (contrast <= mParameters.getMaxContrast())){
            mParameters.setContrast(contrast);
        }

        // Set sharpness parameter.
        String sharpnessStr = mPreferences.getString(
                CameraSettings.KEY_SHARPNESS,"");
        if(sharpnessStr.equals("")) {
            String sharpnessPro = SystemProperties.get(CameraSettings.CAMERA_SHARPNESS_VALUE,"");
            if(!sharpnessPro.equals("")) {
                sharpnessStr = sharpnessPro;
            } else {
                sharpnessStr = getString(R.string.pref_camera_sharpness_default);
            }
            Editor editor = mPreferences.edit();
            editor.putString(CameraSettings.KEY_SHARPNESS,sharpnessStr);
            editor.commit();
        }

        int sharpness = Integer.parseInt(sharpnessStr) *
                (mParameters.getMaxSharpness()/MAX_SHARPNESS_LEVEL);
        Log.v(TAG, "Sharpness value =" + sharpness);
        if((0 <= sharpness) && (sharpness <= mParameters.getMaxSharpness())){
            mParameters.setSharpness(sharpness);
        }

        // Set auto exposure parameter.
        String autoExposure = mPreferences.getString(
                CameraSettings.KEY_AUTOEXPOSURE,
                getString(R.string.pref_camera_autoexposure_default));
        Log.v(TAG, "autoExposure value =" + autoExposure);
        if (isSupported(autoExposure, mParameters.getSupportedAutoexposure())) {
            mParameters.setAutoExposure(autoExposure);
        }

         // Set anti banding parameter.
         String antiBanding = mPreferences.getString(
                 CameraSettings.KEY_ANTIBANDING,
                 getString(R.string.pref_camera_antibanding_default));
         Log.v(TAG, "antiBanding value =" + antiBanding);
         if (isSupported(antiBanding, mParameters.getSupportedAntibanding())) {
             mParameters.setAntibanding(antiBanding);
         }

         mZSLandHDRFlag = false;
         
        String zsl = "off";
        String mode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE, getString(R.string.pref_camera_takepicture_mode_default));
        zsl = ((mode.equals(getString(R.string.pref_camera_takepicture_mode_value_zsl)))/*
                || mode.equals(getString(R.string.pref_camera_takepicture_mode_value_continue))*/) ? "on" : "off";
         // If the ZSL function is on, and the intent is
         // "MediaStore.ACTION_IMAGE_CAPTURE", the user want to see the picture
         // after taking picture, not continue capture picture mode, so we must
         // filter this intent.
        List<String> supportedFlash = mParameters.getSupportedFlashModes();
         if (zsl.equals("on")) {
            //Switch on ZSL Camera mode
            mSnapshotMode = CameraInfo.CAMERA_SUPPORT_MODE_ZSL;
            mParameters.setZSLMode(zsl);
            if (isSupported(Parameters.FLASH_MODE_OFF, supportedFlash)) {
                mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
            }
            mFocusManager.setZslEnable(true);
            mParameters.set("num-snaps-per-shutter", "1");
            mParameters.set("snapshot-burst-num","1");
            if (mode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
                mZSLandHDRFlag = true;
            }
        } else if(zsl.equals("off")) {
            mSnapshotMode = CameraInfo.CAMERA_SUPPORT_MODE_NONZSL;
            mParameters.setZSLMode(zsl);
            mFocusManager.setZslEnable(false);
        }

         if(mode.equals(getString(R.string.pref_camera_takepicture_mode_value_continue))) {
             if (isSupported(Parameters.FLASH_MODE_OFF, supportedFlash)) {
                 mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
             }
         }
         // Set face detetction parameter.
         String faceDetection = mPreferences.getString(
             CameraSettings.KEY_FACE_DETECTION,"");
         if(faceDetection == null || faceDetection.length() < 1) {
             String faceDetectionValue = SystemProperties.get(CameraSettings.CAMERA_FACE_DETECT_VALUE,"");
             if(faceDetectionValue != null
                     &&(faceDetectionValue.equals("on") || faceDetectionValue.equals("off"))) {
                 faceDetection = faceDetectionValue;
             } else {
                 faceDetection = getString(R.string.pref_camera_facedetection_default);
             }
             Editor editor = mPreferences.edit();
             editor.putString(CameraSettings.KEY_FACE_DETECTION,faceDetection);
             editor.commit();
         }
         if (isSupported(faceDetection, mParameters.getSupportedFaceDetectionModes())) {
             mParameters.setFaceDetectionMode(faceDetection);
             if(faceDetection.equals("on") && mFaceDetectionEnabled == false) {
               mFaceDetectionEnabled = true;
               startFaceDetection();
             }
             if(faceDetection.equals("off") && mFaceDetectionEnabled == true) {
               stopFaceDetection();
               mFaceDetectionEnabled = false;
             }
        }
         // skin tone ie enabled only for auto,party and portrait BSM
         // when color effects are not enabled
         if((Parameters.SCENE_MODE_PARTY.equals(mSceneMode) ||
             Parameters.SCENE_MODE_PORTRAIT.equals(mSceneMode))&&
             (Parameters.EFFECT_NONE.equals(colorEffect))) {
             //Set Skin Tone Correction factor
             Log.e(TAG, "yyan set tone bar: mSceneMode = " + mSceneMode);
             if(mSeekBarInitialized == true)
                 //setSkinToneFactor();
                 mHandler.sendEmptyMessageDelayed(SET_SKIN_TONE_FACTOR, SET_SKIN_TONE_FACTOR_DELAY);
         }

         //Set Histogram
        String histogram = mPreferences.getString(
                CameraSettings.KEY_HISTOGRAM,
                getString(R.string.pref_camera_histogram_default));
        if (isSupported(histogram,
                mParameters.getSupportedHistogramModes()) && mCameraDevice != null) {
                // Call for histogram
                if(histogram.equals("enable")) {
                runOnUiThread(new Runnable() {
                     public void run() {
                         if(mGraphView != null)
                             mGraphView.setVisibility(View.VISIBLE);
                         }
                    });
                    mCameraDevice.setHistogramMode(mStatsCallback);
                    mHiston = true;
                } else {
                    mHiston = false;
                    runOnUiThread(new Runnable() {
                         public void run() {
                             if(mGraphView != null)
                                 mGraphView.setVisibility(View.INVISIBLE);
                         }
                    });
                    mCameraDevice.setHistogramMode(null);
                }
        }
    }

    private void updateCameraParametersPreference() {
        if (mAeLockSupported) {
            mParameters.setAutoExposureLock(mFocusManager.getAeAwbLock());
        }

        if (mAwbLockSupported) {
            mParameters.setAutoWhiteBalanceLock(mFocusManager.getAeAwbLock());
        }

        if (mFocusAreaSupported) {
            mParameters.setFocusAreas(mFocusManager.getFocusAreas());
        }

        if (mMeteringAreaSupported) {
            // Use the same area for focus and metering.
            mParameters.setMeteringAreas(mFocusManager.getMeteringAreas());
        }
        // Set picture size.
        String pictureSize = mPreferences.getString(
                CameraSettings.KEY_PICTURE_SIZE, null);

        if (pictureSize == null) {
            //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
            // CameraSettings.initialCameraPictureSize(this, mParameters);
            CameraSettings.initialCameraPictureSize(this, mParameters, mCameraId);
            //END:   Modified by zhanghongxing at 2013-01-09
        } else {
            Size old_size = mParameters.getPictureSize();
            List<Size> supported = mParameters.getSupportedPictureSizes();
            CameraSettings.setCameraPictureSize(
                    pictureSize, supported, mParameters);
            Size size = mParameters.getPictureSize();
            if (old_size != null && size != null) {
                if(!size.equals(old_size) && mCameraState != PREVIEW_STOPPED) {
                    Log.v(TAG, "Picture Size changed. Restart Preview");
                    mRestartPreview = true;
                }
            }
        }
        Size size = mParameters.getPictureSize();

        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        List<Size> sizes = mParameters.getSupportedPreviewSizes();
        Size optimalSize = Util.getOptimalPreviewSize(this, sizes,mPreviewSizeRadio);
        Size original = mParameters.getPreviewSize();
        if (!original.equals(optimalSize)) {
            mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
            // Zoom related settings will be changed for different preview
            // sizes, so set and read the parameters to get latest values
//            mCameraDevice.setParameters(mParameters);
//            mParameters = mCameraDevice.getParameters();
            Log.v(TAG, "Preview Size changed. Restart Preview");
            mRestartPreview = true;
        }
        // Since change scene mode may change supported values,
        // Set scene mode first,

       //BEGIN: Added by zhanghongxing at 2013-03-11 for full preview
       // Set jpeg thumbnail size according picture aspect ratio
        List<Size> thumbnail_sizes = mParameters.getSupportedJpegThumbnailSizes();
        Size currThumbnailSize = mParameters.getJpegThumbnailSize();

        /* Get recommended postview/thumbnail sizes based on aspect ratio */
        int mAspectRatio = (int)((optimalSize.width * 4096) / optimalSize.height);
        int mTempAspectRatio;
        for (Size s : thumbnail_sizes) {
            mTempAspectRatio = (int) ((s.width * 4096) / s.height);
            if(mTempAspectRatio == mAspectRatio) {
                 Log.v(TAG, "Setting thumbnail width:"+s.width +" height:"+ s.height);
                 mParameters.setJpegThumbnailSize(s.width, s.height);
                 break;
            }
        }
        //END:   Added by zhanghongxing at 2013-03-11

        mSceneMode = mPreferences.getString(
                CameraSettings.KEY_SCENE_MODE,
                getString(R.string.pref_camera_scenemode_default));
        Log.v(TAG, "mSceneMode " + mSceneMode);
        if (isSupported(mSceneMode, mParameters.getSupportedSceneModes())) {
            //BEGIN: Modified by zhanghongxing at 2013-01-07 for DER-107
             if (!mParameters.getSceneMode().equals(mSceneMode)) {
//            if (!mParameters.getSceneMode().equals(mSceneMode) &&
//                    Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
                mParameters.setSceneMode(mSceneMode);

                // Setting scene mode will change the settings of flash mode,
                // white balance, and focus mode. Here we read back the
                // parameters, so we can know those settings.
//                mCameraDevice.setParameters(mParameters);
//                mParameters = mCameraDevice.getParameters();
            }
            //END:   Modified by zhanghongxing at 2013-01-07
        } else {
            mSceneMode = mParameters.getSceneMode();
            if (mSceneMode == null) {
                mSceneMode = Parameters.SCENE_MODE_AUTO;
            }
        }

        // Set JPEG quality.
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(mCameraId,
                CameraProfile.QUALITY_HIGH);
        mParameters.setJpegQuality(jpegQuality);

        // For the following settings, we need to check if the settings are
        // still supported by latest driver, if not, ignore the settings.

        // Set exposure compensation
        int value = CameraSettings.readExposure(mPreferences);
        int max = mParameters.getMaxExposureCompensation();
        int min = mParameters.getMinExposureCompensation();
        if (value >= min && value <= max) {
            mParameters.setExposureCompensation(value);
        } else {
            Log.w(TAG, "invalid exposure range: " + value);
        }

        if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
            // Set flash mode.
            String flashMode = mPreferences.getString(
                    CameraSettings.KEY_FLASH_MODE,
                    getString(R.string.pref_camera_flashmode_default));
            if(mIsLowPowerMode) {
                flashMode = Parameters.FLASH_MODE_OFF;
            }
            List<String> supportedFlash = mParameters.getSupportedFlashModes();
            if (isSupported(flashMode, supportedFlash)) {
                mParameters.setFlashMode(flashMode);
            } else {
                flashMode = mParameters.getFlashMode();
                if (flashMode == null) {
                    flashMode = getString(
                            R.string.pref_camera_flashmode_no_flash);
                }
            }

            // Set white balance parameter.
            String whiteBalance = mPreferences.getString(
                    CameraSettings.KEY_WHITE_BALANCE,
                    getString(R.string.pref_camera_whitebalance_default));
            if (isSupported(whiteBalance,
                    mParameters.getSupportedWhiteBalance())) {
                mParameters.setWhiteBalance(whiteBalance);
            } else {
                whiteBalance = mParameters.getWhiteBalance();
                if (whiteBalance == null) {
                    whiteBalance = Parameters.WHITE_BALANCE_AUTO;
                }
            }

            // Set focus mode.
            mFocusManager.overrideFocusMode(null);
            mParameters.setFocusMode(mFocusManager.getFocusMode());
        } else {
            //BEGIN: Added by zhanghongxing at 2013-01-07 for DER-107
            // Set default flash mode
            if (isSupported(Parameters.FLASH_MODE_OFF, 
                    mParameters.getSupportedFlashModes())) {
                mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
            }
            
            // Set default white balance
            if (isSupported(Parameters.WHITE_BALANCE_AUTO, 
                    mParameters.getSupportedWhiteBalance())) {
                mParameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
            }
            
            // Set default exposure compensation
            int exposureValue = Integer.parseInt(CameraSettings.EXPOSURE_DEFAULT_VALUE);
            int exposureMax = mParameters.getMaxExposureCompensation();
            int exposureMin = mParameters.getMinExposureCompensation();
            if (exposureValue >= exposureMin && exposureValue <= exposureMax) {
                mParameters.setExposureCompensation(exposureValue);
            } else {
                Log.w(TAG, "invalid exposure range: " + value);
            }
            //END:   Added by zhanghongxing at 2013-01-07
            mFocusManager.overrideFocusMode(mParameters.getFocusMode());
        }

        if (mContinousFocusSupported) {
            if (mParameters.getFocusMode().equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraDevice.setAutoFocusMoveCallback(mAutoFocusMoveCallback);
            } else {
                mCameraDevice.setAutoFocusMoveCallback(null);
            }
        }
        
        //Add by wangbin at 2013-03-22
        mSilentMode = mPreferences.getString(
                CameraSettings.KEY_SILENT_MODE,
                getString(R.string.pref_silentmode_default));
        if (CameraSettings.SILENT_MODE_OFF.equals(mSilentMode)) {
            mIsSilentMode = false;
            enableShutterSound(true);
        } else {
            mIsSilentMode = true;
            enableShutterSound(false);
        }
        
        mTimerSnap = mPreferences.getString(
                CameraSettings.KEY_TIMER_SNAP,
                getString(R.string.pref_timersnap_default));
        //QCom related parameters updated here.
        qcomUpdateCameraParametersPreference();
    }

    // We separate the parameters into several subsets, so we can update only
    // the subsets actually need updating. The PREFERENCE set needs extra
    // locking because the preference can be changed from GLThread as well.
    private void setCameraParameters(int updateSet) {
        if ((updateSet & UPDATE_PARAM_INITIALIZE) != 0) {
            updateCameraParametersInitialize();
        }

        if ((updateSet & UPDATE_PARAM_ZOOM) != 0) {
            updateCameraParametersZoom();
        }

        if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
            updateCameraParametersPreference();
        }

        //BEGIN: Added by zhanghongxing at 2013-01-07 for DER-107
//        if (isSupported(mSceneMode, mParameters.getSupportedSceneModes())) {
//            if (!mParameters.getSceneMode().equals(mSceneMode) &&
//                    !Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
//                mParameters.setSceneMode(mSceneMode);
//            }
//        }
        //END:   Added by zhanghongxing at 2013-01-07
        mParameters.set("capture-burst-queue-depth","6");
        mCameraDevice.setParameters(mParameters);
    }

    // If the Camera is idle, update the parameters immediately, otherwise
    // accumulate them in mUpdateSet and update later.
    private void setCameraParametersWhenIdle(int additionalUpdateSet) {
        mUpdateSet |= additionalUpdateSet;
        if (mCameraDevice == null) {
            // We will update all the parameters when we open the device, so
            // we don't need to do anything now.
            mUpdateSet = 0;
            return;
        } else if (isCameraIdle()) {
            setCameraParameters(mUpdateSet);
            if(mRestartPreview && mCameraState != PREVIEW_STOPPED) {
                Log.e(TAG, "Restarting Preview...");
                stopPreview();
                setPreviewFrameLayoutAspectRatio();
                startPreview();
                if(continueTakePicState == ContinueShutterState.NONE){
                    setCameraState(IDLE);
                }
            }
            mRestartPreview = false;
            updateSceneModeUI();
            mUpdateSet = 0;
        } else {
            if (!mHandler.hasMessages(SET_CAMERA_PARAMETERS_WHEN_IDLE)) {
                mHandler.sendEmptyMessageDelayed(
                        SET_CAMERA_PARAMETERS_WHEN_IDLE, 1000);
            }
        }

    }

    private boolean isCameraIdle() {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        return (mCameraState == IDLE) ||
                ((mFocusManager != null) && mFocusManager.isFocusCompleted()
                        // && (mCameraState != SWITCHING_CAMERA));
                        && (mCameraState != SWITCHING_CAMERA)&&(mCameraState == CHANGING_STORAGE_STATE));
        //END:   Modified by xiongzhu at 2013-04-15
    }

    public boolean isImageCaptureIntent() {
        String action = getIntent().getAction();
        return (MediaStore.ACTION_IMAGE_CAPTURE.equals(action));
    }

    private void setupCaptureParams() {
        Bundle myExtras = getIntent().getExtras();
        if (myExtras != null) {
            mSaveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            Log.e(TAG," mSaveUri = "+mSaveUri);
            mCropValue = myExtras.getString("crop");
        }
    }

    private void showPostCaptureAlert() {
        if (mIsImageCaptureIntent) {
            mPostCaptureAlertFlag = true;
            Util.fadeOut(mIndicatorControlContainer);
            Util.fadeOut(mSecondLevelIndicatorControlBar);
            Util.fadeOut(mShutterButton);

            Util.fadeIn(mReviewRetakeButton);
            Util.fadeIn((View) mReviewDoneButton);
        }
    }

    private void hidePostCaptureAlert() {
        if (mIsImageCaptureIntent) {
            mPostCaptureAlertFlag = false;
            Util.fadeOut(mReviewRetakeButton);
            Util.fadeOut((View) mReviewDoneButton);
            
            Util.fadeIn(mShutterButton);
            if (mIndicatorControlContainer != null) {
                Util.fadeIn(mIndicatorControlContainer);
            }
            if (mSecondLevelIndicatorControlBar != null) {
                Util.fadeIn(mSecondLevelIndicatorControlBar);
            }
        }
    }

    private void switchToOtherMode(int mode) {
        if (isFinishing()) return;
        if (mImageSaver != null) mImageSaver.waitDone();
        if (mThumbnail != null) ThumbnailHolder.keep(mThumbnail);
        MenuHelper.gotoMode(mode, Camera.this);
        mHandler.removeMessages(FIRST_TIME_INIT);
        finish();
    }

    @Override
    public void onModeChanged(int mode) {
        //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
        // if (mode != ModePicker.MODE_CAMERA) switchToOtherMode(mode);
        //END:   Deleted by zhanghongxing at 2013-01-09
    }

    private void qureyAndSetGpsState() {
         boolean recordLocation = RecordLocationPreference.get(
                 mPreferences, mContentResolver);
         if (recordLocation && (recordLocation != mIsRecordLocation)) {
                 boolean isLocationOn = false;
                 ContentResolver resolver = getContentResolver();
                 boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(resolver,"gps");
                 boolean networkEnabled = Settings.Secure.isLocationProviderEnabled(resolver,"network");
                 isLocationOn = (gpsEnabled || networkEnabled);
                 if (!isLocationOn) {
                     Util.showLocationAlert(Camera.this);
                 }
         }
         mIsRecordLocation = recordLocation;
         mLocationManager.recordLocation(recordLocation);
    }
    @Override
    public void onSharedPreferenceChanged() {
        // ignore the events after "onPause()"
        if (mPaused) return;

        String newmIntelligenceValue = mPreferences.getString(
                CameraSettings.KEY_CAMERA_INTELLIGENCE_KEY,
                getString(R.string.pref_intelligence_default));
        if(!mCurIntelligenceValue.equals(newmIntelligenceValue)) {
        	mCurIntelligenceValue = newmIntelligenceValue;
        	int length = QCOM_SETTING_KEYS.length;
        	CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        	if (info.facing != CameraInfo.CAMERA_FACING_FRONT) {
        		length += QCOM_SETTING_KEYS_1.length;
        	}
        	String[] intelligenceKeys = new String[length];
        	System.arraycopy(QCOM_SETTING_KEYS,0,intelligenceKeys, 0, QCOM_SETTING_KEYS.length);
        	if (info.facing != CameraInfo.CAMERA_FACING_FRONT) {
        		System.arraycopy(QCOM_SETTING_KEYS_1,0,intelligenceKeys, 
        				QCOM_SETTING_KEYS.length, QCOM_SETTING_KEYS_1.length);
        	}
        	if(mSecondLevelIndicatorControlBar != null) {
        		mSecondLevelIndicatorControlBar.updateOtherSettingsIntelligenceKeys(
        				intelligenceKeys,newmIntelligenceValue.equals("on"));
        	}
        }

        qureyAndSetGpsState();

        String takePictureMode = mPreferences.getString(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE,
                                 getString(R.string.pref_camera_takepicture_mode_default));
        boolean needReloadContolBar = false;
        if (!mOldPictureMode.equals(takePictureMode)) {
            Editor editor = mPreferences.edit();
            String anti_shake_value = getString(R.string.pref_camera_takepicture_mode_value_anti_shake);
            if (takePictureMode.equals(anti_shake_value)) {
                editor.putString(CameraSettings.KEY_ISO,
                        getString(R.string.pref_camera_iso_value_isodeblur));
                ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_ISO);
                if(pref != null) pref.setEnable(false);
            } else if(mOldPictureMode.equals(anti_shake_value)){
                editor.putString(CameraSettings.KEY_ISO,
                        getString(R.string.pref_camera_iso_value_auto));
                ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_ISO);
                if(pref != null) pref.setEnable(true);
            }
            editor.commit();
            needReloadContolBar = true;
        }
    
       if(mOldPictureMode != null && mOldPictureMode.equals(takePictureMode)) {
            if(mOldPictureMode.equals(Parameters.SCENE_MODE_HDR) ||
                    takePictureMode.equals(Parameters.SCENE_MODE_HDR)) {
                mIsNeedUpdateSceneUI = true;
                updateSceneModeUI();
            }
        }

        if(takePictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_hdr))) {
            ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_COLOR_EFFECT);
            ((IconListPreference) pref).setAvailable(false);
            needReloadContolBar = true;
        } else if(mOldPictureMode != null && !takePictureMode.equals(mOldPictureMode)) {
            ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_COLOR_EFFECT);
            ((IconListPreference) pref).setAvailable(true);
            needReloadContolBar = true;
        }
        if(takePictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_continue))
                || takePictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_zsl)) ||
                mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_FLASH_MODE);
            ((IconListPreference) pref).setAvailable(false);
            needReloadContolBar = true;
        } else if(mOldPictureMode != null && !takePictureMode.equals(mOldPictureMode)) {
            ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_FLASH_MODE);
            ((IconListPreference) pref).setAvailable(true);
            needReloadContolBar = true;
        }

        if(mOldPictureMode != null && !mOldPictureMode.equalsIgnoreCase(takePictureMode)) {
            if(mOldPictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_continue))) {
                ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_REDEYE_REDUCTION);
                if(pref != null) pref.setEnable(true);
                pref = mPreferenceGroup.findPreference(CameraSettings.KEY_DENOISE);
                if(pref != null) pref.setEnable(true);
            } else if (takePictureMode.equalsIgnoreCase(getString(R.string.pref_camera_takepicture_mode_value_continue))) {
                ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_REDEYE_REDUCTION);
                if(pref != null) pref.setEnable(false);
                pref = mPreferenceGroup.findPreference(CameraSettings.KEY_DENOISE);
                if(pref != null) pref.setEnable(false);
            }
            needReloadContolBar = true;
        }
        
        if(mSecondLevelIndicatorControlBar != null && needReloadContolBar == true) {
            mSecondLevelIndicatorControlBar.reloadPreferences();
        }
        
        if (takePictureMode.equals(getString(R.string.pref_camera_takepicture_mode_value_panorama))) {
            switchToOtherMode(ModePicker.MODE_PANORAMA);
            return;
        }
        
        //BEGIN: Added by xiongzhu at 2013-4-15
        String storagePlace=mPreferences.getString(CameraSettings.KEY_CAMERA_STORAGE_PLACE,getString(R.string.pref_storage_place_default));
        Storage.mIsExternalStorage=storagePlace.equals( CameraSettings.CAMERA_STORAGE_SDCARD ) ? true:false;
        if(!mStoragePlace.equals(storagePlace)){
            setCameraState(CHANGING_STORAGE_STATE);
            mStoragePlace=storagePlace;
            installIntentFilter();
            checkStorage();
//            CameraHolder.instance().keep();
//            closeCamera();
            if (mSurfaceTexture != null) {
                mCameraScreenNail.releaseSurfaceTexture();
                mSurfaceTexture = null;
            }
            updateCameraScreenNail(!mIsImageCaptureIntent,Storage.mIsExternalStorage);
//            CameraHolder.instance().keep();
            changeStoragePlace();
            mRestartPreview = true;
            if (!mIsImageCaptureIntent) {
                getLastThumbnail();
            }
        }
        if(needRestart()){
            Log.e(TAG, "Restarting Preview... Camera Mode Changhed");
            stopPreview();
            if(mShutterButton != null) {
                mShutterButton.setEnabled(false);
            }
            startPreview();
            setCameraState(IDLE);
            mRestartPreview = false;
        }
        setCameraParametersWhenIdle(UPDATE_PARAM_PREFERENCE);
        setPreviewFrameLayoutAspectRatio();
        //updateOnScreenIndicators();
        if (mSeekBarInitialized == true){
            Log.e(TAG, "yyan onSharedPreferenceChanged Skin tone bar: change");
                    // skin tone ie enabled only for auto,party and portrait BSM
                    // when color effects are not enabled
                    String colorEffect = mPreferences.getString(
                        CameraSettings.KEY_COLOR_EFFECT,
                        getString(R.string.pref_camera_coloreffect_default));
                    if((Parameters.SCENE_MODE_PARTY.equals(mSceneMode) ||
                        Parameters.SCENE_MODE_PORTRAIT.equals(mSceneMode))&&
                        (Parameters.EFFECT_NONE.equals(colorEffect))) {
                        ;
                    }
                    else{
                        disableSkinToneSeekBar();
                    }
        }
        mOldPictureMode = takePictureMode;
        
        mVolumeKeyMode = mPreferences.getString(CameraSettings.KEY_VOLUME_KEY_MODE,
                getString(R.string.pref_volume_key_default));
        canDoVolumeSnap = false;
        if (mVolumeKeyMode.equals(CameraSettings.VOLUME_KEY_VALUE_SNAP)) {
            canDoVolumeSnap = true;
        }
    }

    @Override
    public void onCameraPickerClicked(int cameraId) {
        if (mPaused || mPendingSwitchCameraId != -1 || !mAppBridge.isCurrentCameraPreview())
            return;
        Log.v(TAG, "Start to copy texture. cameraId=" + cameraId);
        // We need to keep a preview frame for the animation before
        // releasing the camera. This will trigger onPreviewTextureCopied.
        mCameraScreenNail.copyTexture();
        mPendingSwitchCameraId = cameraId;
        //cancel onShutterClicked message for switch camera
        if (mDoSnapRunnable != null)
            mHandler.removeCallbacks(mDoSnapRunnable);
        mSnapshotOnIdle = false;
        // Disable all camera controls.
        setCameraState(SWITCHING_CAMERA);
        //Disable the switch picker, until switch completely.
    }

    //BEGIN: Added by xiongzhu at 2013-04-15
    private void changeStoragePlace(){
        if (mPaused) return;

        // from onPause
        //closeCamera();  delete by xiongzhu at 2013-7-23
        if (mFaceView != null) mFaceView.clear();
        if (mFocusManager != null) mFocusManager.removeMessages();

        // Restart the camera and initialize the UI. From onCreate.
//        CameraOpenThread cameraOpenThread = new CameraOpenThread();
//        cameraOpenThread.start();
//        try {
//            cameraOpenThread.join();
//        } catch (InterruptedException ex) {
//             ignore
//        }
//        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
//        updateCameraParametersPreference();
//        boolean mirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
//        mFocusManager.setMirror(mirror);
//        mFocusManager.setParameters(mInitialParams);
//        setPreviewFrameLayoutAspectRatio();
//        stopPreview();
//        startPreview();
        mCameraScreenNail.changeStoragePlace();
//        setCameraState(IDLE);
//        startFaceDetection();
    }
    //END:   Added by xiongzhu at 2013-04-15

    private void switchCamera() {
        if (mPaused) return;

        if(mShutterButton != null)
            mShutterButton.setEnabled(false);
        Log.v(TAG, "Start to switch camera. id=" + mPendingSwitchCameraId);
        mCameraId = mPendingSwitchCameraId;
        mPendingSwitchCameraId = -1;
        disableSkinToneSeekBar();
        // from onPause
        closeCamera();
        collapseCameraControls();
        if (mFaceView != null) mFaceView.clear();
        if (mFocusManager != null) mFocusManager.removeMessages();

        // Restart the camera and initialize the UI. From onCreate.
        mPreferences.setLocalId(Camera.this, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
        CameraOpenThread cameraOpenThread = new CameraOpenThread();
        cameraOpenThread.start();
        try {
            cameraOpenThread.join();
        } catch (InterruptedException ex) {
            // ignore
        }
        if(mCameraDevice == null) return;
        initializeCapabilities();
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        //BEGIN: Added by zhanghongxing at 2013-01-09 for full preview
        updateCameraParametersPreference();
        mCameraDevice.setParameters(mParameters);
        //END:   Added by zhanghongxing at 2013-01-09
        boolean mirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
        mFocusManager.setMirror(mirror);
        mFocusManager.setParameters(mInitialParams);
        setPreviewFrameLayoutAspectRatio();
        startPreview();
        setCameraState(IDLE);
        startFaceDetection();
        initializeIndicatorControl();

        // from onResume
        setOrientationIndicator(mOrientationCompensation, false);
        // from initializeFirstTime
        initializeZoom();
        resetZoom();
        //updateOnScreenIndicators();
        showTapToFocusToastIfNeeded();

        // Start switch camera animation. Post a message because
        // onFrameAvailable from the old camera may already exist.
        mHandler.sendEmptyMessage(SWITCH_CAMERA_START_ANIMATION);
    }

    // Preview texture has been copied. Now camera can be released and the
    // animation can be started.
    @Override
    protected void onPreviewTextureCopied() {
        mHandler.sendEmptyMessage(SWITCH_CAMERA);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        keepScreenOnAwhile();
    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    @Override
    public void onRestorePreferencesClicked() {
        if (mPaused) return;
        
        View subPopup = mSecondLevelIndicatorControlBar.getActiveSubSettingPopup();
        if (subPopup != null) {
            mSecondLevelIndicatorControlBar.dismissSettingPopup();
            return;
        }
        //BEGIN: Add by wangbin at 2013-6-27
        //cancel onShutterClicked message for restore preferences
        if (mDoSnapRunnable != null)
            mHandler.removeCallbacks(mDoSnapRunnable);
        mSnapshotOnIdle = false;
        //END: Add by wangbin at 2013-6-27
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                restorePreferences();
            }
        };
        mRotateDialog.showAlertDialog(
                null,
                getString(R.string.confirm_restore_message),
                getString(android.R.string.ok), runnable,
                getString(android.R.string.cancel), null);
    }

    private void restorePreferences() {
        // Reset the zoom. Zoom value is not stored in preference.
        if (mIsZoomSupported) {
            mZoomValue = 0;
            mRestartPreview = false;
            setCameraParametersWhenIdle(UPDATE_PARAM_ZOOM);
            mZoomControl.setZoomIndex(0);
        }
        disableSkinToneSeekBar();
       if (mSecondLevelIndicatorControlBar != null) {
//            Size old_size = mParameters.getPictureSize();
            mSecondLevelIndicatorControlBar.dismissSettingPopup();
            CameraSettings.restorePreferences(Camera.this, mPreferences,
                    mParameters);
            mSecondLevelIndicatorControlBar.reloadPreferences();
            // re-set picture size to old_size in order to check whether need to
            // re-preview
//            mParameters.setPictureSize(old_size.width, old_size.height);
            onSharedPreferenceChanged();
        }
    }

    public void restoreGPSState() {
        Editor editor = mPreferences.edit();
        editor.putString(CameraSettings.KEY_RECORD_LOCATION,RecordLocationPreference.VALUE_OFF);
        editor.commit();
        
        if(mSecondLevelIndicatorControlBar != null) {
           mSecondLevelIndicatorControlBar.reloadPreferences();
           mIsRecordLocation = false;
           mLocationManager.recordLocation(false);
        }
    }
    
    @Override
    public void onOverriddenPreferencesClicked() {
        if (mPaused) return;
        if (mNotSelectableToast == null) {
            String str = getResources().getString(R.string.not_selectable_in_scene_mode);
            mNotSelectableToast = Toast.makeText(Camera.this, str, Toast.LENGTH_SHORT);
        }
        mNotSelectableToast.show();
    }

    @Override
    public void onFaceDetection(Face[] faces, android.hardware.Camera camera) {
        mFaceView.setFaces(faces);
    }

    private void showTapToFocusToast() {
        new RotateTextToast(this, R.string.tap_to_focus, mOrientationCompensation).show();
        // Clear the preference.
        Editor editor = mPreferences.edit();
        editor.putBoolean(CameraSettings.KEY_CAMERA_FIRST_USE_HINT_SHOWN, false);
        editor.apply();
    }

    private void initializeCapabilities() {
        mInitialParams = mCameraDevice.getParameters();
        mFocusAreaSupported = (mInitialParams.getMaxNumFocusAreas() > 0
                && isSupported(Parameters.FOCUS_MODE_AUTO,
                        mInitialParams.getSupportedFocusModes()));
        mMeteringAreaSupported = (mInitialParams.getMaxNumMeteringAreas() > 0);
        mAeLockSupported = mInitialParams.isAutoExposureLockSupported();
        mAwbLockSupported = mInitialParams.isAutoWhiteBalanceLockSupported();
        mContinousFocusSupported = mInitialParams.getSupportedFocusModes().contains(
                Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    // PreviewFrameLayout size has changed.
    @Override
    public void onSizeChanged(int width, int height) {
        if (mFocusManager != null) mFocusManager.setPreviewSize(width, height);
    }

    void setPreviewFrameLayoutOrientation(){
       boolean set = true;
       Size size = mParameters.getPictureSize();
       CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];

       setDisplayOrientation();

       if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
          if( (mDisplayRotation == 0) || (mDisplayRotation == 180) ) {
             if(info.orientation % 180 != 0)
                set = false;
             else
                set = true;
          }
          else { // mDisplayRotation = 90 or 270
             if(info.orientation % 180 != 0)
                set = true;
             else
                set = false;
          }
       }
       else {  // ORIENTATION_LANDSCAPE case
          if( (mDisplayRotation == 0) || (mDisplayRotation == 180) ) {
             if(info.orientation % 180 != 0)
                set = true;
             else
                set = false;
          }
          else { // mDisplayRotation = 90 or 270
             if(info.orientation % 180 != 0)
                set = false;
             else
                set = true;
          }
       }
       mPreviewFrameLayout.setCameraOrientation(set);
    }

    void setPreviewFrameLayoutAspectRatio() {
        setPreviewFrameLayoutOrientation();
        // Set the preview frame aspect ratio according to the picture size.
        Size size = mParameters.getPictureSize();
        mPreviewFrameLayout.setAspectRatio((double) size.width / size.height);
    }

    private void setSkinToneFactor(){
       if(mCameraDevice == null || mParameters == null || skinToneSeekBar == null) return;
       String skinToneEnhancementPref = "enable";
       if(isSupported(skinToneEnhancementPref,
               mParameters.getSupportedSkinToneEnhancementModes())){
         if(skinToneEnhancementPref.equals("enable")) {
             int skinToneValue =0;
             int progress;
               //get the value for the first time!
               if (mskinToneValue ==0){
                  String factor = mPreferences.getString(CameraSettings.KEY_SKIN_TONE_ENHANCEMENT_FACTOR, "0");
                  skinToneValue = Integer.parseInt(factor);
               }

               Log.e(TAG, "yyan Skin tone bar: enable = " + mskinToneValue);
               enableSkinToneSeekBar();
               //As a wrokaround set progress again to show the actually progress on screen.
               if(skinToneValue != 0) {
                   progress = (skinToneValue/SCE_FACTOR_STEP)-MIN_SCE_FACTOR;
                   skinToneSeekBar.setProgress(progress);
               }
          } else {
              Log.e(TAG, "yyan Skin tone bar: disable");
               disableSkinToneSeekBar();
          }
       } else {
           Log.e(TAG, "yyan Skin tone bar: Not supported");
          disableSkinToneSeekBar();
       }
    }

    private void enableSkinToneSeekBar() {
        int progress;
        if(brightnessProgressBar != null)
           brightnessProgressBar.setVisibility(View.INVISIBLE);
        skinToneSeekBar.setMax(MAX_SCE_FACTOR-MIN_SCE_FACTOR);
        skinToneSeekBar.setVisibility(View.VISIBLE);
        skinToneSeekBar.requestFocus();
        if (mskinToneValue != 0) {
            progress = (mskinToneValue/SCE_FACTOR_STEP)-MIN_SCE_FACTOR;
            mskinToneSeekListener.onProgressChanged(skinToneSeekBar,progress,false);
        }else {
            progress = (MAX_SCE_FACTOR-MIN_SCE_FACTOR)/2;
            RightValue.setText("");
            LeftValue.setText("");
        }
        skinToneSeekBar.setProgress(progress);
        Title.setText(R.string.skin_tone_enhancement); //Use string skin_tone_enhancement
        Title.setVisibility(View.VISIBLE);
        RightValue.setVisibility(View.VISIBLE);
        LeftValue.setVisibility(View.VISIBLE);
        mSkinToneSeekBar = true;
    }

    private void disableSkinToneSeekBar() {
         skinToneSeekBar.setVisibility(View.INVISIBLE);
         Title.setVisibility(View.INVISIBLE);
         RightValue.setVisibility(View.INVISIBLE);
         LeftValue.setVisibility(View.INVISIBLE);
         mskinToneValue = 0;
         mSkinToneSeekBar = false;
         Editor editor = mPreferences.edit();
         editor.putString(CameraSettings.KEY_SKIN_TONE_ENHANCEMENT_FACTOR,
                            Integer.toString(mskinToneValue - MIN_SCE_FACTOR));
         editor.apply();
         if(brightnessProgressBar != null)
             brightnessProgressBar.setVisibility(View.INVISIBLE);
    }
    
    public void enterLowPowerMode() {
        if(mCameraDevice == null) return;
        mIsLowPowerMode = true;
        IconListPreference flashPref = (IconListPreference)mPreferenceGroup.findPreference(
                CameraSettings.KEY_FLASH_MODE);
        if(!flashPref.isAvailable()) return;
        flashPref.setAvailable(false);

        mParameters = mCameraDevice.getParameters();
        mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        mCameraDevice.setParameters(mParameters);
        mSecondLevelIndicatorControlBar.dismissSettingPopup();
        mSecondLevelIndicatorControlBar.reloadPreferences();
    }
    
    public void restorePowerMode() {
        if(!mIsLowPowerMode) return;
        mIsLowPowerMode = false;
        IconListPreference flashPref = (IconListPreference)mPreferenceGroup.findPreference(
                CameraSettings.KEY_FLASH_MODE);
        if(flashPref.isAvailable()) return;
        flashPref.setAvailable(true);
        
        String flashMode = mPreferences.getString(
                CameraSettings.KEY_FLASH_MODE,
                getString(R.string.pref_camera_flashmode_default));
        mParameters = mCameraDevice.getParameters();
        mParameters.setFlashMode(flashMode);
        mCameraDevice.setParameters(mParameters);
    }
    
    public void cacheImage(final byte[] data, Uri uri, String title,
            Location loc, int width, int height, int thumbnailWidth,
            int orientation) {
         SaveRequest r = new SaveRequest();
         r.data = data;
         r.uri = uri;
         r.title = title;
         r.loc = (loc == null) ? null : new Location(loc);  // make a copy
         r.width = width;
         r.height = height;
         r.thumbnailWidth = thumbnailWidth;
         r.orientation = orientation;
         synchronized (this) {
             mCacheQueue.add(r);
         }
         
         mCacheQueueSize++;
         Log.i(TAG,"==zzw:cacheImage() mCacheQueueSize="+mCacheQueueSize);

         if(continueTakePicState == ContinueShutterState.SHUTTING) {
             if(mCacheQueue.size() >= 5) {
                 for(int i= 0; i< 5; i++) {
                     SaveRequest sr = mCacheQueue.get(0);
                     mImageSaver.addImage(sr);
                     mCacheQueue.remove(0);
                 }
             }

             if(mCacheQueueSize == CONTINUE_TAKE_PICTURE_COUNT) {
                 continueTakePicState = ContinueShutterState.SAVING;
                 Message msg = Message.obtain(mHandler,SAVE_CACHED_IMAGE);
                 msg.sendToTarget();
             }
         }
    }
    
    public synchronized void saveCachedImageToQueue() {
        if(mCacheQueue == null) return;
        for(SaveRequest r:mCacheQueue) {
            mImageSaver.addImage(r);
        }
        mCacheQueue.clear();
    }
}

/*
 * Provide a mapping for Jpeg encoding quality levels
 * from String representation to numeric representation.
 */
class JpegEncodingQualityMappings {
    private static final String TAG = "JpegEncodingQualityMappings";
    private static final int DEFAULT_QUALITY = 85;
    private static HashMap<String, Integer> mHashMap =
            new HashMap<String, Integer>();

    static {
        mHashMap.put("normal",    CameraProfile.QUALITY_LOW);
        mHashMap.put("fine",      CameraProfile.QUALITY_MEDIUM);
        mHashMap.put("superfine", CameraProfile.QUALITY_HIGH);
    }

    // Retrieve and return the Jpeg encoding quality number
    // for the given quality level.
    public static int getQualityNumber(String jpegQuality) {
        try{
            int qualityPercentile = Integer.parseInt(jpegQuality);
            if(qualityPercentile >= 0 && qualityPercentile <=100)
                return qualityPercentile;
            else
                return DEFAULT_QUALITY;
        } catch(NumberFormatException nfe){
            //chosen quality is not a number, continue
        }
        Integer quality = mHashMap.get(jpegQuality);
        if (quality == null) {
            Log.w(TAG, "Unknown Jpeg quality: " + jpegQuality);
            return DEFAULT_QUALITY;
        }
        return CameraProfile.getJpegEncodingQualityParameter(quality.intValue());
    }
}

//-------------
 //Graph View Class

class GraphView extends View {
    private Bitmap  mBitmap;
    private Paint   mPaint = new Paint();
    private Canvas  mCanvas = new Canvas();
    private float   mScale = (float)3;
    private float   mWidth;
    private float   mHeight;
    private Camera mCamera;
    private CameraManager.CameraProxy mGraphCameraDevice;
    private float scaled;
    private static final int STATS_SIZE = 256;
    private static final String TAG = "GraphView";

    //Added by zhanghongxing at 2013-01-17 for DER-474
    private Bitmap mTempBitmap;

    public GraphView(Context context, AttributeSet attrs) {
        super(context,attrs);

        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //BEGIN: Modified by zhanghongxing at 2013-01-17 for DER-474
        SystemClock.sleep(200);
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mTempBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mTempBitmap);
        mBitmap = mTempBitmap;
        if (mTempBitmap != null && !mTempBitmap.isRecycled()) {
            mTempBitmap.recycle();
            mTempBitmap = null;
        }
        // mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        // mCanvas.setBitmap(mBitmap);
        //END:   Modified by zhanghongxing at 2013-01-17
        mWidth = w;
        mHeight = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        Log.v(TAG, "in Camera.java ondraw");
        if(!Camera.mHiston ) {
            Log.e(TAG, "returning as histogram is off ");
            return;
        }
    if (mBitmap != null) {
        final Paint paint = mPaint;
        final Canvas cavas = mCanvas;
        final float border = 5;
        float graphheight = mHeight - (2 * border);
        float graphwidth = mWidth - (2 * border);
        float left,top,right,bottom;
        float bargap = 0.0f;
        float barwidth = 1.0f;

        cavas.drawColor(0xFFAAAAAA);
        paint.setColor(Color.BLACK);

        for (int k = 0; k <= (graphheight /32) ; k++) {
            float y = (float)(32 * k)+ border;
            cavas.drawLine(border, y, graphwidth + border , y, paint);
        }
        for (int j = 0; j <= (graphwidth /32); j++) {
            float x = (float)(32 * j)+ border;
            cavas.drawLine(x, border, x, graphheight + border, paint);
        }
        paint.setColor(0xFFFFFFFF);
        synchronized(Camera.statsdata) {
            for(int i=1 ; i<=STATS_SIZE ; i++)  {
                scaled = Camera.statsdata[i]/mScale;
                if(scaled >= (float)STATS_SIZE)
                    scaled = (float)STATS_SIZE;
                left = (bargap * (i+1)) + (barwidth * i) + border;
                top = graphheight + border;
                right = left + barwidth;
                bottom = top - scaled;
                cavas.drawRect(left, top, right, bottom, paint);
            }
        }
        canvas.drawBitmap(mBitmap, 0, 0, null);
        //BEGIN: Added by zhanghongxing at 2013-01-17 for DER-474
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        //END:   Added by zhanghongxing at 2013-01-17
    }
        if(Camera.mHiston && mCamera!= null) {
            mGraphCameraDevice = mCamera.getCamera();
            if(mGraphCameraDevice != null){
                mGraphCameraDevice.sendHistogramData();
            }
        }
    }
    public void PreviewChanged() {
        invalidate();
    }
    public void setCameraObject(Camera camera) {
        mCamera = camera;
    }
}
