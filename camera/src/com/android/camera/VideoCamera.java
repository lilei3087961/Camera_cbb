/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (c) ,2013 The Linux Foundation. All Rights Reserved.
 *
 * Not a Contribution, Apache licensenotifications and license are retained
 * for attribution purposes only.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.camera;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.filterpacks.videosink.MediaRecorderStopException;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.Color;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.android.camera.ActivityBase.CameraOpenThread;
import com.android.camera.ComboPreferences;
import com.android.camera.ui.CameraPicker;
import com.android.camera.ui.IndicatorControlContainer;
import com.android.camera.ui.IndicatorControlWheelContainer;
import com.android.camera.ui.SecondLevelIndicatorControlBar;
import com.android.camera.ui.PopupManager;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.ui.TwoStateImageView;
import com.android.camera.ui.ZoomControl;
import com.android.gallery3d.ui.GLRootView;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import android.graphics.Color;
import java.util.HashMap;
import android.provider.Settings;
import android.os.Build;
import android.util.FloatMath;
/**
 * The Camcorder activity.
 */
public class VideoCamera extends ActivityBase
        implements CameraPreference.OnPreferenceChangedListener,
        ShutterButton.OnShutterButtonListener, MediaRecorder.OnErrorListener,
        MediaRecorder.OnInfoListener, ModePicker.OnModeChangeListener,
        EffectsRecorder.EffectsListener, LocationManager.Listener{

    private static final String TAG = "videocamera";

    // We number the request code from 1000 to avoid collision with Gallery.
    private static final int REQUEST_EFFECT_BACKDROPPER = 1000;

    private static final int CHECK_DISPLAY_ROTATION = 3;
    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int UPDATE_RECORD_TIME = 5;
    private static final int ENABLE_SHUTTER_BUTTON = 6;
    private static final int SHOW_TAP_TO_SNAPSHOT_TOAST = 7;
    private static final int SWITCH_CAMERA = 8;
    private static final int SWITCH_CAMERA_START_ANIMATION = 9;
    private static final int SHOW_VIDEOSNAP_CAPPING_MSG = 10;
    private static final int SHOW_LOWPOWER_MODE = 11;
    private static final int SWITCH_CAMERA_ANIMATION_DONE = 12;
    private static final int STOP_RECORDING = 13;
    private GLRootView mGLRootView;
    private boolean mIsLowPowerMode = false;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;
    private boolean mIsVideoQualityChanged = false;

    //Modified by zhanghongxing at 2013-03-27 for FBD-481 500ms to 1100ms
    private static final long SHUTTER_BUTTON_TIMEOUT = 1500L; // 1100ms
    private static final long BUTTON_DELAY_TIME = 1300L;
    
    public static final int RESTOREGPS_STATE = 1001;
    
    private String mFlashMode;

    //final String[] OTHER_SETTING_KEYS = {
    //                CameraSettings.KEY_RECORD_LOCATION};
    static final String[] QCOM_SETTING_KEYS = {
                    //BEGIN: Modified by zhanghongxing at 2013-01-06 for DER-173
                    // CameraSettings.KEY_RECORD_LOCATION,
                    CameraSettings.KEY_VIDEO_RECORD_LOCATION,
                    CameraSettings.KEY_CAMERA_STORAGE_PLACE, //Added by xiongzhu at 2013-04-15
                    CameraSettings.KEY_SILENT_MODE,
                    //END:   Modified by zhanghongxing at 2013-01-06
                    //CameraSettings.KEY_VIDEO_SNAPSHOT_SIZE,
                    //CameraSettings.KEY_VIDEO_ENCODER, delete by xiongzhu for cbb
                    //CameraSettings.KEY_AUDIO_ENCODER, delete by xiongzhu for cbb
                    //CameraSettings.KEY_VIDEO_DURATION,
                    //BEGIN: Modified by zhanghongxing at 2013-01-06 for DER-173
                    // CameraSettings.KEY_COLOR_EFFECT,
                    //END:   Modified by zhanghongxing at 2013-01-06
                    //CameraSettings.KEY_VIDEO_HIGH_FRAME_RATE,
//                    CameraSettings.KEY_POWER_MODE,
                    CameraSettings.KEY_VIDEO_QUALITY
                    //CameraSettings.KEY_VERSION_NUMBER
                    };

    static final String[] QCOM_SETTING_KEYS_INTENT = {
        CameraSettings.KEY_VIDEO_RECORD_LOCATION,
        CameraSettings.KEY_SILENT_MODE,
        CameraSettings.KEY_VIDEO_QUALITY
        };

    static final String[] SETTING_KEYS = {
            CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE,
            //BEGIN: Modified by zhanghongxing at 2013-01-06 for DER-173
            // CameraSettings.KEY_WHITE_BALANCE,
            //CameraSettings.KEY_VIDEO_WHITE_BALANCE,
            //END:   Modified by zhanghongxing at 2013-01-06
            //CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL,     
            CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,
            CameraSettings.KEY_VIDEO_COLOR_EFFECT};

    /**
     * An unpublished intent flag requesting to start recording straight away
     * and return as soon as recording is stopped.
     * TODO: consider publishing by moving into MediaStore.
     */
    private static final String EXTRA_QUICK_CAPTURE =
            "android.intent.extra.quickCapture";

    private boolean mSnapshotInProgress = false;

    private static final String EFFECT_BG_FROM_GALLERY = "gallery";

    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    private ComboPreferences mPreferences;
    private PreferenceGroup mPreferenceGroup;

    private PreviewFrameLayout mPreviewFrameLayout;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private IndicatorControlContainer mIndicatorControlContainer;
    private SecondLevelIndicatorControlBar mSecondLevelIndicatorControlBar;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private View mReviewControl;
    private RotateDialogController mRotateDialog;
    private ImageView mCaptureAnimView;

    // An review image having same size as preview. It is displayed when
    // recording is stopped in capture intent.
    private ImageView mReviewImage;
    private Rotatable mReviewCancelButton;
    private Rotatable mReviewDoneButton;
    private RotateImageView mReviewPlayButton;
    private View mReviewRetakeButton;
    private RotateImageView mSwitchToCamera;
    private RotateImageView mSwitchToVideo;
    private RotateImageView mThumbnailWindow;
    private ShutterButton mShutterButton;
    private TextView mRecordingTimeView;
    private RotateLayout mBgLearningMessageRotater;
    private View mBgLearningMessageFrame;
    private LinearLayout mLabelsLinearLayout;

    private boolean mIsVideoCaptureIntent;
    private boolean mQuickCapture;

    private long mStorageSpace;

    private MediaRecorder mMediaRecorder;
    private EffectsRecorder mEffectsRecorder;
    private boolean mEffectsDisplayResult;

    private int mEffectType = EffectsRecorder.EFFECT_NONE;
    private Object mEffectParameter = null;
    private String mEffectUriFromGallery = null;
    private String mPrefVideoEffectDefault;
    private boolean mResetEffect = true;

    private boolean mSwitchingCamera;
    private boolean mMediaRecorderRecording = false;
    private long mRecordingStartTime;
    private boolean mRecordingTimeCountsDown = false;
    private RotateLayout mRecordingTimeRect;
    private long mOnResumeTime;
    // The video file that the hardware camera is about to record into
    // (or is recording into.)
    private String mVideoFilename;
    private ParcelFileDescriptor mVideoFileDescriptor;

    // The video file that has already been recorded, and that is being
    // examined by the user.
    private String mCurrentVideoFilename;
    private Uri mCurrentVideoUri;
    private ContentValues mCurrentVideoValues;

    private CamcorderProfile mProfile;

    // The video duration limit. 0 menas no limit.
    private int mMaxVideoDurationInMs;

    // Time Lapse parameters.
    private boolean mCaptureTimeLapse = false;
    // Default 0. If it is larger than 0, the camcorder is in time lapse mode.
    private int mTimeBetweenTimeLapseFrameCaptureMs = 0;
    private View mTimeLapseLabel;

    private int mDesiredPreviewWidth;
    private int mDesiredPreviewHeight;

    boolean mPreviewing = false; // True if preview is started.
    // The display rotation in degrees. This is only valid when mPreviewing is
    // true.
    private int mDisplayRotation;
    private int mCameraDisplayOrientation;

    private ContentResolver mContentResolver;

    private LocationManager mLocationManager;

    private VideoNamer mVideoNamer;

    private RotateTextToast mLowPowerToast;

    private final Handler mHandler = new MainHandler();

    private MyOrientationEventListener mOrientationListener;
    // The degrees of the device rotated clockwise from its natural orientation.
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    // The orientation compensation for icons and thumbnails. Ex: if the value
    // is 90, the UI components should be rotated 90 degrees counter-clockwise.
    private int mOrientationCompensation = 0;
    // The orientation compensation when we start recording.
    private int mOrientationCompensationAtRecordStart;

    private int mZoomValue;  // The current zoom value.
    private int mZoomMax;
    private ZoomControl mZoomControl;
    private boolean mMmsFlag = false;
    private boolean mHfr = false;
    private boolean mRestoreFlash;  // This is used to check if we need to restore the flash
                                    // status when going back from gallery.
    private long duration;          //to record the duration of the video

    private boolean mIsFromOnPause = false;

    private boolean isChangedToMms = false;

    //Add by wangbin at 2013-03-22
    private String mSilentMode;

    private String oldHFR = "";  //the old HFR
    public static boolean bFlagHFRUpdateUI = false;  //weather updateUI or not
    public static boolean bFlagDisanableToUpdateUI = false;

    //Added by zhanghongxing at 2013-04-10
    public static CameraSettings mSettings;

    //Added by xiongzhu at 2013-04-15
    private boolean mChangingStoragePlace;
    
    private ImageView mGpsIndicator;
    
    private String mCurVideoRecordingMode;

    //zoom
    private static final int HIDE_ZOOM_BAR_DELAY = 4 * 1000;
    private static final int HIDE_ZOOM_BAR = 14;
    private static final int SHOW_ZOOM_BAR = 15;
    private boolean mIsZoomMode = false;
    private final int mStep = 5;
    private float mOldDist;

    private int mLastOrientation = Configuration.ORIENTATION_PORTRAIT;

    //QCOM data Members Starts here
    static class DefaultHashMap<K, V> extends HashMap<K, V> {
        private V mDefaultValue;

        public void putDefault(V defaultValue) {
            mDefaultValue = defaultValue;
        }

        @Override
        public V get(Object key) {
            V value = super.get(key);
            return (value == null) ? mDefaultValue : value;
        }

        public K getKey(V toCheck) {
            Iterator<K> it = this.keySet().iterator();
            V val;
            K key;
            while(it.hasNext()) {
                key = it.next();
                val = this.get(key);
                if (val.equals(toCheck)) {
                    return key;
                }
            }
        return null;
        }
    }


    private static final DefaultHashMap<String, Integer>
            OUTPUT_FORMAT_TABLE = new DefaultHashMap<String, Integer>();
    private static final DefaultHashMap<String, Integer>
            VIDEO_ENCODER_TABLE = new DefaultHashMap<String, Integer>();
    private static final DefaultHashMap<String, Integer>
            AUDIO_ENCODER_TABLE = new DefaultHashMap<String, Integer>();
    private static final DefaultHashMap<String, Integer>
            VIDEOQUALITY_BITRATE_TABLE = new DefaultHashMap<String, Integer>();
    private static final DefaultHashMap<String, String>
            HFR_SIZES = new DefaultHashMap<String, String>();

    static {
        OUTPUT_FORMAT_TABLE.put("3gp", MediaRecorder.OutputFormat.THREE_GPP);
        OUTPUT_FORMAT_TABLE.put("mp4", MediaRecorder.OutputFormat.MPEG_4);
        OUTPUT_FORMAT_TABLE.putDefault(MediaRecorder.OutputFormat.DEFAULT);

        //VIDEO_ENCODER_TABLE.put("h263", MediaRecorder.VideoEncoder.H263);
        VIDEO_ENCODER_TABLE.put("h264", MediaRecorder.VideoEncoder.H264);
       // VIDEO_ENCODER_TABLE.put("mpeg4", MediaRecorder.VideoEncoder.MPEG_4_SP);
        VIDEO_ENCODER_TABLE.putDefault(MediaRecorder.VideoEncoder.DEFAULT);

        //AUDIO_ENCODER_TABLE.put("amrnb", MediaRecorder.AudioEncoder.AMR_NB);
        //AUDIO_ENCODER_TABLE.put("qcelp", MediaRecorder.AudioEncoder.QCELP);
        //AUDIO_ENCODER_TABLE.put("evrc", MediaRecorder.AudioEncoder.EVRC);
        //AUDIO_ENCODER_TABLE.put("amrwb", MediaRecorder.AudioEncoder.AMR_WB);
        AUDIO_ENCODER_TABLE.put("aac", MediaRecorder.AudioEncoder.AAC);
        //AUDIO_ENCODER_TABLE.put("aacplus", MediaRecorder.AudioEncoder.AAC_PLUS);
        //AUDIO_ENCODER_TABLE.put("eaacplus",
        //        MediaRecorder.AudioEncoder.EAAC_PLUS);
        AUDIO_ENCODER_TABLE.putDefault(MediaRecorder.AudioEncoder.DEFAULT);

        HFR_SIZES.put("800x480", "WVGA");
        HFR_SIZES.put("640x480", "VGA");
        HFR_SIZES.put("432x240", "WQVGA");
        HFR_SIZES.put("320x240", "QVGA");
    }

    private int mVideoEncoder;
    private int mAudioEncoder;
    private boolean mRestartPreview = false;
    private int videoWidth;
    private int videoHeight;
    // flag of update camera UI 
    private static int mNeedupdateCameraUI= 0;
    boolean mUnsupportedResolution = false;
    private boolean mVideoSnapSizeChanged = false;
    private boolean mUnsupportedHFRVideoSize = false;
    private boolean mUnsupportedHFRVideoCodec = false;
    //QCOM data Members ends here
    
    //Added by zhanghongxing at 2012-12-28 for DER-289
    private boolean mIsCalling = false;

    //Added by xiongzhu at 2013-04-15
    private boolean mDidRegister=false;

    // This Handler is used to post message back onto the main thread of the
    // application
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case ENABLE_SHUTTER_BUTTON:
                    mShutterButton.setEnabled(true);
                    break;

                case CLEAR_SCREEN_DELAY: {
                    getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }

                case UPDATE_RECORD_TIME: {
                    updateRecordingTime();
                    break;
                }

                case CHECK_DISPLAY_ROTATION: {
                    // Restart the preview if display rotation has changed.
                    // Sometimes this happens when the device is held upside
                    // down and camera app is opened. Rotation animation will
                    // take some time and the rotation value we have got may be
                    // wrong. Framework does not have a callback for this now.
                    if ((Util.getDisplayRotation(VideoCamera.this) != mDisplayRotation)
                            && !mChangingStoragePlace //Added by xiongzhu at 2013-04-15
                            && !mMediaRecorderRecording 
                            && !mSwitchingCamera) {
                        startPreview();
                    }
                    if (SystemClock.uptimeMillis() - mOnResumeTime < 5000) {
                        mHandler.sendEmptyMessageDelayed(CHECK_DISPLAY_ROTATION, 100);
                    }
                    break;
                }

                case SHOW_TAP_TO_SNAPSHOT_TOAST: {
//                    showTapToSnapshotToast();
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

                    // Enable all camera controls.
                    mSwitchingCamera = false;
                    break;
                }
                case SHOW_VIDEOSNAP_CAPPING_MSG:
                case SHOW_LOWPOWER_MODE:
                    showUserMsg(msg.what);
                    break;

                case SWITCH_CAMERA_ANIMATION_DONE: {
                    // Enable the switch picker.
                    break;
                }

                case STOP_RECORDING:
                	if (mMediaRecorderRecording) onStopVideoRecording();
                	// Show the toast.
                	Toast.makeText(VideoCamera.this, R.string.video_reach_size_limit,
                			Toast.LENGTH_LONG).show();
                	break;
                default:
                    Log.v(TAG, "Unhandled message: " + msg.what);
                    break;
                case HIDE_ZOOM_BAR:
                      mZoomControl.setVisibility(View.INVISIBLE);
                    break;
                case SHOW_ZOOM_BAR:
                      mZoomControl.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    //BEGIN: Added by zhanghongxing at 2012-12-28 for DER-289
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE: {
                    mIsCalling = false;
                    break;
                }
                
                case TelephonyManager.CALL_STATE_RINGING: {
                    mIsCalling = true;
                    if (mMediaRecorderRecording) {
                    	showCallingToast();
                        onStopVideoRecording();
                    }
                    break;
                }
                
                case TelephonyManager.CALL_STATE_OFFHOOK: {
                    mIsCalling = true;
                    if (mMediaRecorderRecording) {
                    	showCallingToast();
                        onStopVideoRecording();
                    }
                    break;
                }
            }
        }
    };
    
    public void showCallingToast() {
        Context context = getApplicationContext();
        CharSequence string;
        if (mMediaRecorderRecording) {
            string = getString(R.string.calling_stop_recording);
        } else {
            string = getString(R.string.calling_unable_recording);
        }
        int duration = Toast.LENGTH_LONG;
        Toast.makeText(context, string, duration).show();
    }
    //END:   Added by zhanghongxing at 2012-12-28

    private BroadcastReceiver mReceiver = null;

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                updateAndShowStorageHint();
                stopVideoRecording();
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                updateAndShowStorageHint();
                //BEGIN: Modified by zhanghongxing at 2013-05-28
                if (!mIsVideoCaptureIntent) {
                    getLastThumbnail();
                }
                //END:   Modified by zhanghongxing at 2013-05-28
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                // SD card unavailable
                // handled in ACTION_MEDIA_EJECT
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                Toast.makeText(VideoCamera.this,
                        getResources().getString(R.string.wait), Toast.LENGTH_LONG).show();
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                updateAndShowStorageHint();
            }
        }
    }

    private BroadcastReceiver mShutdownReceiver = null;

    private class ShutdownBroadcastReceiver extends BroadcastReceiver {
        private static final String TAG = "VideoCamera.ShutDownListener";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_SHUTDOWN)) {
                Log.d(TAG, "ACTION_SHUTDOWN Received, stop recording if it's in recording state");
                if (mMediaRecorderRecording) {
                    onStopVideoRecording();
                }
            }
        }
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        String fileFoamt = SystemProperties.get(CameraSettings.CAMERA_MEDIA_FILE_FORMAT,"");
        if(fileFoamt == null || fileFoamt.length() < 6) {
            fileFoamt = getString(R.string.video_file_name_format);
        } else {
            fileFoamt = "'VID'_" +fileFoamt;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(fileFoamt);

        return dateFormat.format(date);
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
        super.onCreate(savedInstanceState);

        mPreferences = new ComboPreferences(this);
        CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
        mCameraId = getPreferredCameraId(mPreferences);

        mPreferences.setLocalId(this, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());

        mNumberOfCameras = CameraHolder.instance().getNumberOfCameras();
        mPrefVideoEffectDefault = getString(R.string.pref_video_effect_default);
        resetEffect();

        //BEGIN: Added by xiongzhu at 2013-04-15
        mContentResolver = getContentResolver();
        
        setContentView(R.layout.video_camera);

        mCurVideoRecordingMode = mPreferences.getString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,
                getString(R.string.pref_videocamera_recording_mode_default));
        // Surface texture is from camera screen nail and startPreview needs it.
        // This must be done before startPreview.
        mIsVideoCaptureIntent = isVideoCaptureIntent();
        // createCameraScreenNail(!mIsVideoCaptureIntent);//deleted by xiongzhu at 2013-3-8
        String storagePlace=mPreferences.getString(CameraSettings.KEY_CAMERA_STORAGE_PLACE,getString(R.string.pref_storage_place_default));
		Storage.mIsExternalStorage=storagePlace.equals( CameraSettings.CAMERA_STORAGE_SDCARD ) ? true:false;
        
        String internalPath = FxEnvironment.getInternalStorageDirectory(getApplicationContext());
        String externalPath = FxEnvironment.getExternalStorageDirectory(getApplicationContext());
        Storage.init();
        if( Storage.mIsExternalStorage ) {
			mStorageSpace = Storage.getAvailableSpace();
			if ( mStorageSpace == Storage.UNAVAILABLE ) {
			    SharedPreferences.Editor editor = ComboPreferences
                        .get(VideoCamera.this).edit();
                editor.putString(CameraSettings.KEY_CAMERA_STORAGE_PLACE, CameraSettings.CAMERA_STORAGE_MEMORY );
                editor.apply();
			    Storage.mIsExternalStorage=false;
			    storagePlace=CameraSettings.CAMERA_STORAGE_MEMORY;
			}
		}
		mStoragePlace=storagePlace;
		installIntentFilter(); 
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateStorageHint(mStorageSpace, mOrientationCompensation);
            }
        }, 200);
        
        createCameraScreenNail(!mIsVideoCaptureIntent,Storage.mIsExternalStorage);
        //END:   Added by xiongzhu at 2013-04-15

        /*
         * To reduce startup time, we start the preview in another thread.
         * We make sure the preview is started at the end of onCreate.
         */
        CameraOpenThread cameraOpenThread = new CameraOpenThread();
        cameraOpenThread.start();

        //BEGIN: Deleted by xiongzhu at 2013-04-15
        // mContentResolver = getContentResolver();

        // setContentView(R.layout.video_camera);
        // Surface texture is from camera screen nail and startPreview needs it.
        // This must be done before startPreview.
        // mIsVideoCaptureIntent = isVideoCaptureIntent();
        // createCameraScreenNail(!mIsVideoCaptureIntent);
        //EN:   Deleted by xiongzhu at 2013-04-15

        // Make sure camera device is opened.
        try {
            cameraOpenThread.join();
            if (mOpenCameraFail) {
                Util.showErrorAndFinish(this, R.string.cannot_connect_camera);
                return;
            } else if (mCameraDisabled) {
                Util.showErrorAndFinish(this, R.string.camera_disabled);
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }
        loadCameraPreferences();
        Thread startPreviewThread = new Thread(new Runnable() {
            @Override
            public void run() {
                readVideoPreferences();
                startPreview();
                updateUIforHFR();
            }
        });
        startPreviewThread.start();

        initializeControlByIntent();
        initializeMiscControls();

        mRotateDialog = new RotateDialogController(this, R.layout.rotate_dialog);
        mQuickCapture = getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        mOrientationListener = new MyOrientationEventListener(this);
        mLocationManager = new LocationManager(this, this);

        // Make sure preview is started.
        try {
            startPreviewThread.join();
            if (mOpenCameraFail) {
                Util.showErrorAndFinish(this, R.string.cannot_connect_camera);
                return;
            } else if (mCameraDisabled) {
                Util.showErrorAndFinish(this, R.string.camera_disabled);
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }

        showTimeLapseUI(mCaptureTimeLapse);
        initializeVideoSnapshot();
        resizeForPreviewAspectRatio();

        initializeIndicatorControl();
        mSecondLevelIndicatorControlBar.setEnabled(false);
    }

    private void loadCameraPreferences() {
        //BEGIN: Modified by zhanghongxing at 2013-04-10
        // CameraSettings settings = new CameraSettings(this, mParameters,
        //         mCameraId, CameraHolder.instance().getCameraInfo());
        // Remove the video quality preference setting when the quality is given in the intent.
        // mPreferenceGroup = filterPreferenceScreenByIntent(
        //         settings.getPreferenceGroup(R.xml.video_preferences));
    	CameraSettings settings = new CameraSettings(this, mParameters,
                mCameraId, CameraHolder.instance().getCameraInfo());
        mPreferenceGroup = filterPreferenceScreenByIntent(
        		settings.getPreferenceGroup(R.xml.video_preferences));
        //END:   Modified by zhanghongxing at 2013-04-10
    }

    private boolean collapseCameraControls() {
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
        if (mIndicatorControlContainer != null) {
            mIndicatorControlContainer.setEnabled(enable);
        }
        
        if (mSecondLevelIndicatorControlBar != null) {
        	mSecondLevelIndicatorControlBar.setEnabled(enable);
        }
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // if (mModePicker != null) mModePicker.setEnabled(enable);
        if (mSwitchToCamera != null) mSwitchToCamera.setEnabled(enable);
//        if (mSwitchToVideo != null) mSwitchToVideo.setEnabled(enable);
        //if (mSwitchToPanorama != null) mSwitchToPanorama.setEnabled(enable);delete by xiongzhu for cbb
        //END:   Modified by zhanghongxing at 2013-01-09
    }

    private void initializeIndicatorControl() {
    	
    	mGpsIndicator = (ImageView) findViewById(R.id.onscreen_gps_indicator);
    	
        mIndicatorControlContainer =
                (IndicatorControlContainer) findViewById(R.id.indicator_control);
        mSecondLevelIndicatorControlBar = (SecondLevelIndicatorControlBar) findViewById(R.id.second_level_indicator);
        if (mIndicatorControlContainer == null && mSecondLevelIndicatorControlBar == null) return;
        loadCameraPreferences();

        CameraPicker.setImageResourceId(R.drawable.videocamerapicker_selector);
        HashMap otherSettingKeys = new HashMap(2);
        if(mIsVideoCaptureIntent) {
            otherSettingKeys.put(0, QCOM_SETTING_KEYS_INTENT);
        }else {
            otherSettingKeys.put(0, QCOM_SETTING_KEYS);
        }
        IconListPreference recordingMode = 
        		(IconListPreference)mPreferenceGroup.findPreference(
        				CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE);
        if(mIsVideoCaptureIntent) {
            ArrayList<String> unEnabled = new ArrayList<String>();
            unEnabled.add(getResources().getString(R.string.pref_videocamera_recording_mode_value_normal));
            recordingMode.filterUnEnabled(unEnabled);

            unEnabled.clear();
            String msmModeValue = getResources().getString(R.string.pref_videocamera_recording_mode_value_mms);
            String videoRecordingMode = mPreferences.getString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,
                    getString(R.string.pref_videocamera_recording_mode_default));
            if(!videoRecordingMode.equals(msmModeValue)) {
            	Editor editor = mPreferences.edit();
            	editor.putString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,msmModeValue);
            	editor.commit();
            	isChangedToMms = true;
            }
        }
        
        mIndicatorControlContainer.initialize(this, mPreferenceGroup,
             mIsZoomSupported, null, null);
        //END : modified by xiongzhu for ARD-100 
        mSecondLevelIndicatorControlBar.initialize(this, mPreferenceGroup,
              SETTING_KEYS, otherSettingKeys);

        otherSettingKeys.clear();
        mIndicatorControlContainer.setListener(this);
        mSecondLevelIndicatorControlBar.setListener(this);

        if (effectsActive()) {
            /* String defaultQuality = CameraSettings.getDefaultVideoQuality(mCameraId,
                getResources().getString(R.string.pref_video_quality_default));
            String videoQuality =
                mPreferences.getString(CameraSettings.KEY_VIDEO_QUALITY,
                        defaultQuality);
            int quality = Integer.valueOf(videoQuality);
            CamcorderProfile profile = CamcorderProfile.get(mCameraId, quality);
            if (profile.videoFrameHeight > 480) {
                quality = CamcorderProfile.QUALITY_480P;
                if (mIndicatorControlContainer != null) {
                    mIndicatorControlContainer.overrideSettings(
                            CameraSettings.KEY_VIDEO_QUALITY,
                            Integer.toString(CamcorderProfile.QUALITY_480P));
                }
            } */
        }

        if (effectsActive()) {
        	mSecondLevelIndicatorControlBar.overrideSettings(
                    CameraSettings.KEY_VIDEO_QUALITY,
                    Integer.toString(CamcorderProfile.QUALITY_480P));
        }

        //checkSupportedVideoEncoder();
    }

    public void restoreGPSState() {
        Editor editor = mPreferences.edit();
        editor.putString(RecordLocationPreference.VIDEO_KEY,RecordLocationPreference.VALUE_OFF);
        editor.commit();
        
    	if(mSecondLevelIndicatorControlBar != null) {
    	   mSecondLevelIndicatorControlBar.reloadPreferences();
    	   mIsRecordLocation = false;
    	   mLocationManager.recordLocation(false);
    	}
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
        if(mIsRecordLocation && !mMediaRecorderRecording) {
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
    
    private class MyOrientationEventListener
            extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            int newOrientation = Util.roundOrientation(orientation, mOrientation);

            if (mOrientation != newOrientation) {
                mOrientation = newOrientation;
                // The input of effects recorder is affected by
                // android.hardware.Camera.setDisplayOrientation. Its value only
                // compensates the camera orientation (no Display.getRotation).
                // So the orientation hint here should only consider sensor
                // orientation.
                if (effectsActive()) {
                    mEffectsRecorder.setOrientationHint(mOrientation);
                }
            }

            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation =
                    (mOrientation + Util.getDisplayRotation(VideoCamera.this)) % 360;

            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                // Do not rotate the icons during recording because the video
                // orientation is fixed after recording.
                if (!mMediaRecorderRecording) {
                    setOrientationIndicator(mOrientationCompensation, true);
                }
            }

            // Show the toast after getting the first orientation changed.
//            if (mHandler.hasMessages(SHOW_TAP_TO_SNAPSHOT_TOAST)) {
//                mHandler.removeMessages(SHOW_TAP_TO_SNAPSHOT_TOAST);
//                showTapToSnapshotToast();
//            }
        }
    }

    private void setOrientationIndicator(int orientation, boolean animation) {
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // Rotatable[] indicators = {mThumbnailView, mModePicker,
        Rotatable[] indicators = {mThumbnailView, mSwitchToCamera, mSwitchToVideo, mThumbnailBgView, //mSwitchToPanorama, delete by xiongzhu for cbb
                mBgLearningMessageRotater, mIndicatorControlContainer,
                mReviewDoneButton, mReviewPlayButton, mRotateDialog, mShutterButton,mSecondLevelIndicatorControlBar};
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

        // We change the orientation of the linearlayout only for phone UI because when in portrait
        // the width is not enough.
        if (mLabelsLinearLayout != null) {
            if (((orientation / 90) & 1) == 0) {
                mLabelsLinearLayout.setOrientation(LinearLayout.VERTICAL);
            } else {
                mLabelsLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            }
        }
        mRecordingTimeRect.setOrientation(mOrientationCompensation, animation);
    }

    private void startPlayVideoActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(mCurrentVideoUri, convertOutputFormatToMimeType(mProfile.fileFormat));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't view video " + mCurrentVideoUri, ex);
        }
    }

    @OnClickAttr
    public void onThumbnailClicked(View v) {
        if (!mMediaRecorderRecording && mThumbnail != null
                && !mChangingStoragePlace //Added by xiongzhu at 2013-04-15
                && !mSwitchingCamera
                && !mIsCalling) {
            gotoGallery();
        }
    }

    @OnClickAttr
    public void onReviewRetakeClicked(View v) {
        findViewById(R.id.frame_layout).setBackgroundColor(Color.TRANSPARENT);
        deleteCurrentVideo();
        hideAlert();
    }

    @OnClickAttr
    public void onReviewPlayClicked(View v) {
        startPlayVideoActivity();
    }

    @OnClickAttr
    public void onReviewDoneClicked(View v) {
        doReturnToCaller(true);
    }

    @OnClickAttr
    public void onReviewCancelClicked(View v) {
        stopVideoRecording();
        doReturnToCaller(false);
    }

    //BEGIN: Added by zhanghongxing at 2013-01-09 for full preview
    @OnClickAttr
    public void onSwitchToCameraClicked(View v) {
    	switchToOtherMode(ModePicker.MODE_CAMERA);
    }

    @OnClickAttr
    public void onSwitchToVideoClicked(View v) {
    	// switchToOtherMode(ModePicker.MODE_VIDEO);
    	switchToOtherMode(ModePicker.MODE_CAMERA);
    }

    @OnClickAttr
    public void onSwitchToPanoramaClicked(View v) {
    	switchToOtherMode(ModePicker.MODE_PANORAMA);
    }
    //END:   Added by zhanghongxing at 2013-01-09

    private void onStopVideoRecording() {
        mEffectsDisplayResult = true;
        boolean recordFail = stopVideoRecording();
        if (mIsVideoCaptureIntent) {
            if (!effectsActive()) {
                if (mQuickCapture) {
                    doReturnToCaller(!recordFail);
                } else if (!recordFail) {
                    showAlert();
                }
            }
            if(!recordFail) {
            	Thumbnail.deleteThumbnailFile(getFilesDir());
            }
        } else if (!recordFail){
            // Start capture animation.
            if (!mPaused) mCameraScreenNail.animateCapture(getCameraRotation());
            if (!effectsActive()) getThumbnail();
        }
        
        if(mShutterButton != null) mShutterButton.setEnabled(false);
        mHandler.postDelayed(new Runnable() {
       	 @Override
            public void run() {
       		 if(mShutterButton != null) mShutterButton.setEnabled(true);
       	 }
       },500);
    }

    private int getCameraRotation() {
        return (mOrientationCompensation - mDisplayRotation + 360) % 360;
    }

    public void onProtectiveCurtainClick(View v) {
        // Consume clicks
    }

    @Override
    public void onShutterButtonClick() {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        //if (collapseCameraControls() || mSwitchingCamera) return;
        if (collapseCameraControls() || mSwitchingCamera||mChangingStoragePlace) return;
        //END:   Modified by xiongzhu at 2013-04-15
        mStorageSpace = Storage.getAvailableSpace();
        if (mStorageSpace <= Storage.LOW_STORAGE_THRESHOLD && !mMediaRecorderRecording) {
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
        				new RotateTextToast(VideoCamera.this, R.string.phone_memory_not_enough, 
        						mOrientationCompensation).show();
        				return;
        			}
        			storagePlace = CameraSettings.CAMERA_STORAGE_SDCARD;
        		}
        		
        		long storageSpace = Storage.getAvailableSpace();
        		if(storageSpace<= Storage.LOW_STORAGE_THRESHOLD) {
        			Storage.mIsExternalStorage = oldStorage;
            	    new RotateTextToast(VideoCamera.this, R.string.sdcard_and_phone_memory_not_enough,
            	    		mOrientationCompensation).show();
        			return;
        		}
        		if(Storage.mIsExternalStorage) {
        			new RotateTextToast(VideoCamera.this, R.string.storageplace_sdcard,
                            mOrientationCompensation).show();
        		} else {
        			new RotateTextToast(VideoCamera.this, R.string.storageplace_memory,
	            			mOrientationCompensation).show();
        		}
                mStoragePlace = storagePlace;
        		Editor editor = mPreferences.edit();
        	    editor.putString(CameraSettings.KEY_CAMERA_STORAGE_PLACE, storagePlace);
        	    editor.apply();
        	    if (mSecondLevelIndicatorControlBar != null) {
        	    	   mSecondLevelIndicatorControlBar.reloadPreferences();
        	    }

                if (mSurfaceTexture != null) {
                    mCameraScreenNail.releaseSurfaceTexture();
                    mSurfaceTexture = null;
                }
                updateCameraScreenNail(!mIsVideoCaptureIntent,Storage.mIsExternalStorage);
                stopPreview();
                startPreview();
                mCameraScreenNail.changeStoragePlace();
        	} else {
        		new RotateTextToast(VideoCamera.this, R.string.phone_memory_not_enough, 
        				mOrientationCompensation).show();
        		return;
        	}
            //delay 500ms for start preview
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startVideoRecording();
                }
            }, 500);

            return;
        }
        boolean stop = mMediaRecorderRecording;
        mShutterButton.setEnabled(false);
        if (stop) {
            onStopVideoRecording();
        } else {
            startVideoRecording();
        }
        

        // Keep the shutter button disabled when in video capture intent
        // mode and recording is stopped. It'll be re-enabled when
        // re-take button is clicked.
       
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        // Do nothing (everything happens in onShutterButtonClick).
    }

    private void updateAndShowStorageHint() {
        mStorageSpace = Storage.getAvailableSpace();
        updateStorageHint(mStorageSpace, mOrientationCompensation);
    }

    private void configVideoSnapshotSize() {
      //Video Snapshot Picture size
        if(mParameters.isPowerModeSupported()) {
            String videoSnapSize = mPreferences.getString(
                             CameraSettings.KEY_VIDEO_SNAPSHOT_SIZE, null);
            List<Size> supported = mParameters.getSupportedPictureSizes();
            if (videoSnapSize == null) {
                //CameraSettings.initialCameraPictureSize(this, mParameters);
                if (supported != null) {
                    for (String candidate : this.getResources().getStringArray(
                            R.array.pref_camera_picturesize_entryvalues)) {
                        if (CameraSettings.setCameraPictureSize(candidate, supported, mParameters)) {
                            break;
                        }
                    }
                }
                Size temp = mParameters.getPictureSize();
                videoSnapSize = String.format("%dx%d", temp.width, temp.height);
            }
            int index = videoSnapSize.indexOf('x');
            int width = Integer.parseInt(videoSnapSize.substring(0, index));
            int height = Integer.parseInt(videoSnapSize.substring(index + 1));

            if ( width < mProfile.videoFrameWidth ||
                 height < mProfile.videoFrameHeight) {
                // Let the user know that snapshot resolutions lesser than
                // video size is not supported.
                 mHandler.sendEmptyMessage(SHOW_VIDEOSNAP_CAPPING_MSG);
                 videoSnapSize = String.format("%dx%d",
                 mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            }
            CameraSettings.setCameraPictureSize(
                           videoSnapSize, supported, mParameters);
        }
    }

    private void qcomReadVideoPreferences(){
        //checkSupportedVideoEncoder();
        String videoEncoder = mPreferences.getString(
                CameraSettings.KEY_VIDEO_ENCODER,
                getString(R.string.pref_camera_videoencoder_default));
        mVideoEncoder = VIDEO_ENCODER_TABLE.get(videoEncoder);
        Log.v(TAG, "Video Encoder type in application=" +mVideoEncoder);

        String audioEncoder = mPreferences.getString(
                CameraSettings.KEY_AUDIO_ENCODER,
                getString(R.string.pref_camera_audioencoder_default));
        mAudioEncoder = AUDIO_ENCODER_TABLE.get(audioEncoder);
        Log.v(TAG, "Audio Encoder type in application=" +mAudioEncoder);

        String minutesStr = mPreferences.getString(CameraSettings.KEY_VIDEO_DURATION,
                getString(R.string.pref_camera_video_duration_default));
        int minutes = -1;
        try{
            minutes = Integer.parseInt(minutesStr);
        }catch(NumberFormatException npe){
            // use default value continue
            minutes = CameraSettings.DEFAULT_VIDEO_DURATION;
        }

        if (minutes == -1) {
            // This is a special case: the value -1 means we want to use the
            // device-dependent duration for MMS messages. The value is
            // represented in seconds.
//            mMaxVideoDurationInMs = CameraSettings.getVidoeDurationInMillis("mms");
        	mMaxVideoDurationInMs = CameraSettings.DEFAULT_VIDEO_DURATION;

        } else {
            // 1 minute = 60000ms
            mMaxVideoDurationInMs = CameraSettings.DEFAULT_VIDEO_DURATION;
        }

        if(mParameters.isPowerModeSupported()) {
            String powermode = mPreferences.getString(
                    CameraSettings.KEY_POWER_MODE,
                    getString(R.string.pref_camera_powermode_default));
            Log.v(TAG, "read videopreferences power mode =" +powermode);
            String old_mode = mParameters.getPowerMode();
            if(!old_mode.equals(powermode) && mPreviewing)
            {
                mRestartPreview = true;
            }
            mParameters.setPowerMode(powermode);

            Size old_size = mParameters.getPictureSize();
            configVideoSnapshotSize();
            // Set the preview frame aspect ratio according to the picture size.
            Size size = mParameters.getPictureSize();
            Log.v(TAG, "New Video picture size : "+ size.width + " " + size.height);
            // BEGIN: Modified by zhanghongxing at 2013-04-25
            if(!size.equals(old_size)){
                if(powermode.equals("Normal_Power") && mPreviewing) {
                    Log.v(TAG, "new Video size id different from old picture size , restart..");
                    mVideoSnapSizeChanged = true;
                // }else if(mPreviewing){
                } else if(!old_mode.equals(powermode) && mPreviewing){
                    //Need to change
                    mHandler.sendEmptyMessage(SHOW_LOWPOWER_MODE);
                }
            }
            // END:   Modified by zhanghongxing at 2013-04-25
        }
    }

    private void readVideoPreferences() {
        // The preference stores values from ListPreference and is thus string type for all values.
        // We need to convert it to int manually.
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // String defaultQuality = CameraSettings.getDefaultVideoQuality(mCameraId,
        //         getResources().getString(R.string.pref_video_quality_default));
        // String videoQuality =
        //         mPreferences.getString(CameraSettings.KEY_VIDEO_QUALITY,
        //                 defaultQuality);
        String videoQuality = mPreferences.getString(CameraSettings.KEY_VIDEO_QUALITY, null);
        if (videoQuality == null) {
            videoQuality = CameraSettings.initialVideoQuality(this, mCameraId);
        } else {
            int quality = Integer.valueOf(videoQuality);
            if(!CamcorderProfile.hasProfile(mCameraId,quality)) {
                videoQuality = CameraSettings.initialVideoQuality(this, mCameraId);
		    }
        }
        //END:   Modified by zhanghongxing at 2013-01-09
        int quality = Integer.valueOf(videoQuality);
        boolean isSmsMode = mCurVideoRecordingMode
				.equals(getString(R.string.pref_videocamera_recording_mode_value_mms));
        if (isSmsMode) {
        	quality = CamcorderProfile.QUALITY_LOW;
        	mMmsFlag = true;
        }
       
        // Set video quality.
        Intent intent = getIntent();
        if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            int extraVideoQuality =
                    intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            if (extraVideoQuality > 0) {
                quality = CamcorderProfile.QUALITY_HIGH;
            } else {  // 0 is mms.
                quality = CamcorderProfile.QUALITY_LOW;
            }
            mMmsFlag = true;
        }

        // Set video duration limit. The limit is read from the preference,
        // unless it is specified in the intent.
        if (intent.hasExtra(MediaStore.EXTRA_DURATION_LIMIT)) {
            int seconds =
                    intent.getIntExtra(MediaStore.EXTRA_DURATION_LIMIT, 0);
            mMaxVideoDurationInMs = 1000 * seconds;
        } else {
            mMaxVideoDurationInMs = CameraSettings.DEFAULT_VIDEO_DURATION;
        }

        // Set effect
        mEffectType = CameraSettings.readEffectType(mPreferences);
        if (mEffectType != EffectsRecorder.EFFECT_NONE) {
            mEffectParameter = CameraSettings.readEffectParameter(mPreferences);
            // Set quality to be no higher than 480p.
            CamcorderProfile profile = CamcorderProfile.get(mCameraId, quality);
            if (profile.videoFrameHeight > 480) {
                quality = CamcorderProfile.QUALITY_480P;
                // On initial startup, can get here before indicator control is
                // enabled. In that case, UI quality override handled in
                // initializeIndicatorControl.
                if (mSecondLevelIndicatorControlBar != null) {
                	mSecondLevelIndicatorControlBar.overrideSettings(
                            CameraSettings.KEY_VIDEO_QUALITY,
                            Integer.toString(CamcorderProfile.QUALITY_480P));
                }
            }
        } else {
            mEffectParameter = null;
            if (mSecondLevelIndicatorControlBar != null) {
            	mSecondLevelIndicatorControlBar.overrideSettings(
                        CameraSettings.KEY_VIDEO_QUALITY,
                        null);
            }
        }
        
        // Read time lapse recording interval.
        String frameIntervalStr = mPreferences.getString(
                CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL,
                getString(R.string.pref_video_time_lapse_frame_interval_default));
        mTimeBetweenTimeLapseFrameCaptureMs = Integer.parseInt(frameIntervalStr);

        mCaptureTimeLapse = (mTimeBetweenTimeLapseFrameCaptureMs != 0);
        // TODO: This should be checked instead directly +1000.
        if (mCaptureTimeLapse) quality += 1000;
        mProfile = CamcorderProfile.get(mCameraId, quality);
        getDesiredPreviewSize();
        qcomReadVideoPreferences();
    }

    private void checkSupportedVideoEncoder() {
        if (mSecondLevelIndicatorControlBar != null) {
            ListPreference videoEncoderPref = mPreferenceGroup.findPreference(
                    CameraSettings.KEY_VIDEO_ENCODER);
            //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
            // String defaultQuality = CameraSettings.getDefaultVideoQuality(mCameraId,
            // 		getResources().getString(R.string.pref_video_quality_default));
            // String videoQuality =
            // 		mPreferences.getString(CameraSettings.KEY_VIDEO_QUALITY,
            // 				defaultQuality);
            String videoQuality = mPreferences.getString(CameraSettings.KEY_VIDEO_QUALITY, null);
            if (videoQuality == null) {
                videoQuality = CameraSettings.initialVideoQuality(this, mCameraId);
            }
            //END:   Modified by zhanghongxing at 2013-01-09

            if (Integer.valueOf(videoQuality) == CamcorderProfile.QUALITY_720P) {
                CameraSettings.buildVideoEncoder(mPreferenceGroup, videoEncoderPref, true);
                mSecondLevelIndicatorControlBar.reloadPreferences();
            } else {
                CameraSettings.buildVideoEncoder(mPreferenceGroup, videoEncoderPref, false);
                mSecondLevelIndicatorControlBar.reloadPreferences();
            }
        }
    }

    private void writeDefaultEffectToPrefs()  {
        ComboPreferences.Editor editor = mPreferences.edit();
        editor.putString(CameraSettings.KEY_VIDEO_EFFECT,
                getString(R.string.pref_video_effect_default));
        editor.apply();
    }

    private void getDesiredPreviewSize() {
        mParameters = mCameraDevice.getParameters();
//        if (mParameters.getSupportedVideoSizes() == null || effectsActive()) {
//            mDesiredPreviewWidth = mProfile.videoFrameWidth;
//            mDesiredPreviewHeight = mProfile.videoFrameHeight;
//        } else {  // Driver supports separates outputs for preview and video.
//            List<Size> sizes = mParameters.getSupportedPreviewSizes();
//            Size preferred = mParameters.getPreferredPreviewSizeForVideo();
//            int product = preferred.width * preferred.height;
//            Iterator<Size> it = sizes.iterator();
//            // Remove the preview sizes that are not preferred.
//            while (it.hasNext()) {
//                Size size = it.next();
//                if (size.width * size.height > product) {
//                    it.remove();
//                }
//            }
//            if(Build.VERSION.SDK_INT >= 18) {
//            	mDesiredPreviewWidth = mProfile.videoFrameWidth;
//        		mDesiredPreviewHeight = mProfile.videoFrameHeight;
//            } else {
//            	try {
//            		Method isSingleOutputEnabled;
//            		Log.i(TAG,"=====zzw===");
//            		isSingleOutputEnabled = mParameters.getClass().getMethod("isSingleOutputEnabled"); 
//            		if(!((Boolean)(isSingleOutputEnabled.invoke(mParameters)))) {
//            			Size optimalSize = Util.getOptimalPreviewSize(this, sizes,
//            					(double) mProfile.videoFrameWidth / mProfile.videoFrameHeight);
//            			mDesiredPreviewWidth = optimalSize.width;
//            			mDesiredPreviewHeight = optimalSize.height;
//            		} else {
//            			mDesiredPreviewWidth = mProfile.videoFrameWidth;
//            			mDesiredPreviewHeight = mProfile.videoFrameHeight;
//            		}
//            	} catch(Exception e) {
//            		e.printStackTrace();
//            	}
//            }
//        }

        List<Size> sizes = mParameters.getSupportedPreviewSizes();
        Size optimalSize = Util.getOptimalPreviewSize(this, sizes,mPreviewSizeRadio);
        mDesiredPreviewWidth = optimalSize.width;
        mDesiredPreviewHeight = optimalSize.height;
        Log.v(TAG, "mDesiredPreviewWidth=" + mDesiredPreviewWidth +
                ". mDesiredPreviewHeight=" + mDesiredPreviewHeight);
    }

    void setPreviewFrameLayoutOrientation(){
       boolean set = true;
       int width = mProfile.videoFrameWidth;
       int height = mProfile.videoFrameHeight;
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

    private void resizeForPreviewAspectRatio() {
        setPreviewFrameLayoutOrientation();
        mPreviewFrameLayout.setAspectRatio(
                (double) mProfile.videoFrameWidth / mProfile.videoFrameHeight);
    }

    //BEGIN: Added by xiongzhu at 2013-04-15
    private void installIntentFilter(){
        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        if(mReceiver==null){
            mReceiver = new MyBroadcastReceiver();
        }
        
		if(Storage.mIsExternalStorage){
	        registerReceiver(mReceiver, intentFilter);   
	        mDidRegister=true;
	    }
        mStorageSpace = Storage.getAvailableSpace();
	}
    //END:   Added by xiongzhu at 2013-04-15
    @Override
    protected void onResume() {
        Log.i(TAG,"onResume()");
        String videoRecordingMode = mPreferences.getString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,
                getString(R.string.pref_videocamera_recording_mode_default));

        if(isVideoCaptureIntent()) {
            String smsModeValue = getResources().getString(R.string.pref_videocamera_recording_mode_value_mms);
            if(videoRecordingMode != null && !videoRecordingMode.equals(smsModeValue)) {
                Editor editor = mPreferences.edit();
                editor.putString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,smsModeValue);
                editor.commit();
                videoRecordingMode = smsModeValue;
                isChangedToMms = true;
            }
            if(isChangedToMms) {
                onSharedPreferenceChanged();
            }
        }
        if(mSecondLevelIndicatorControlBar != null) {
            mSecondLevelIndicatorControlBar.reloadPreferences();
        }
        mPaused = false;
        super.onResume();
        if (mOpenCameraFail || mCameraDisabled) return;
        mZoomValue = 0;

        //BEGIN: Added by zhanghongxing at 2013-01-06 for DER-173
        RecordLocationPreference.mKey = RecordLocationPreference.VIDEO_KEY;
        //END:   Added by zhanghongxing at 2013-01-06

        showVideoSnapshotUI(false);

        // Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
        mOrientationListener.enable();
        if (!mPreviewing) {
            //BEGIN: Modified by zhanghongxinga at 2013-04-15
            // if (!value.startsWith("goofy") && resetEffect()) {
            if (resetEffect()) {
                mBgLearningMessageFrame.setVisibility(View.GONE);
                mSecondLevelIndicatorControlBar.reloadPreferences();
            }
            //END:   Modified by zhanghongxinga at 2013-04-15
            CameraOpenThread cameraOpenThread = new CameraOpenThread();
            cameraOpenThread.start();
            try {
                cameraOpenThread.join();
                if (mOpenCameraFail) {
                    Util.showErrorAndFinish(this, R.string.cannot_connect_camera);
                    return;
                } else if (mCameraDisabled) {
                    Util.showErrorAndFinish(this, R.string.camera_disabled);
                    return;
                }
            } catch (InterruptedException ex) {
                // ignore
            }

            readVideoPreferences();
            resizeForPreviewAspectRatio();
            startPreview();
            updateCameraUI();
//            updateUIforHFR();
        }

        // Initializing it here after the preview is started.
        initializeZoom();

        keepScreenOnAwhile();

        //BEGIN: Deleted by xiongzhu at 2013-04-15
        // install an intent filter to receive SD card related events.
        // IntentFilter intentFilter =
        //         new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        // intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        // intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        // intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        // intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        // intentFilter.addDataScheme("file");
        // mReceiver = new MyBroadcastReceiver();
        // registerReceiver(mReceiver, intentFilter);
        // mStorageSpace = Storage.getAvailableSpace();
        //END:   Deleted by xiongzhu at 2013-04-15
        
        // BEGIN: Added by zhanghongxing at 2013-07-01
        IntentFilter shutdownIntentFilter =
                new IntentFilter(Intent.ACTION_SHUTDOWN);
        mShutdownReceiver = new ShutdownBroadcastReceiver();
        registerReceiver(mShutdownReceiver, shutdownIntentFilter);
        // END:   Added by zhanghongxing at 2013-07-01

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateStorageHint(mStorageSpace, mOrientationCompensation);
            }
        }, 200);

        // Initialize location service.
//        boolean recordLocation = RecordLocationPreference.get(
//                mPreferences, mContentResolver);
//        mIsRecordLocation = recordLocation;
//        mLocationManager.recordLocation(recordLocation);

        mIsRecordLocation = false;
        qureyAndSetGpsState();
        
        if (!mIsFromOnPause && !mIsVideoCaptureIntent) {
            getLastThumbnail();
            mSwitchToVideo.setImageResource(R.drawable.ic_switch_video_focused);
        }
        mIsFromOnPause = false;

        if (mPreviewing) {
            mOnResumeTime = SystemClock.uptimeMillis();
            mHandler.sendEmptyMessageDelayed(CHECK_DISPLAY_ROTATION, 100);
        }
        // Dismiss open menu if exists.
        PopupManager.getInstance(this).notifyShowPopup(null);

        mVideoNamer = new VideoNamer();
        updateUIforHFR();
        if (mCameraAppView != null && mCameraAppView.getVisibility() != View.VISIBLE
                && mShowCameraAppView) {
            updateCameraAppView();
        }

        
        boolean recordLocation = RecordLocationPreference.get(
                mPreferences, mContentResolver);
        mIsRecordLocation = recordLocation;
        if(recordLocation) {
            mLocationManager.recordLocation(recordLocation);
        }
        
        if (videoRecordingMode.equals(getString(R.string.pref_videocamera_recording_mode_value_mms))) {
        	if (mPreferenceGroup != null) {
        		ListPreference  videoQuality = mPreferenceGroup.findPreference(CameraSettings.KEY_VIDEO_QUALITY);
        		if(videoQuality != null)
        		    videoQuality.setEnable(false);	
        	}
            mMmsFlag = true;
        }

        //BEGIN: Added by zhanghongxing at 2012-12-28 for DER-289
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        //END:   Added by zhanghongxing at 2012-12-28
    }

    private void setPreviewTexture() {
        try {
            if (effectsActive()) {
                mEffectsRecorder.setPreviewSurfaceTexture(mSurfaceTexture, mSurfaceWidth,
                    mSurfaceHeight);
            } else {
                mCameraDevice.setPreviewTextureAsync(mSurfaceTexture);
            }
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("setPreviewTexture failed", ex);
        }
    }

    private void setDisplayOrientation() {
        mDisplayRotation = Util.getDisplayRotation(this);
        mCameraDisplayOrientation = Util.getDisplayOrientation(0, mCameraId);
    }

    private void startPreview() {
        Log.v(TAG, "startPreview");

        mCameraDevice.setErrorCallback(mErrorCallback);
        if (mPreviewing == true) {
            stopPreview();
            setPreviewFrameLayoutOrientation();
            if (effectsActive() && mEffectsRecorder != null) {
                mEffectsRecorder.release();
                mEffectsRecorder = null;
            }
        }

        setDisplayOrientation();
        mCameraDevice.setDisplayOrientation(mCameraDisplayOrientation);
        setCameraParameters();

        //BEGIN: Added by xiongzhu at 2013-04-15
        if (mSurfaceTexture == null) {
			Log.e(TAG,"mSurfaceTexture is null");
            mCameraScreenNail.acquireSurfaceTexture();
            mSurfaceTexture = mCameraScreenNail.getSurfaceTexture();
        }
        //END:   Added by xiongzhu at 2013-04-15
        Log.i(TAG,"==zzw:preview-size = "+mParameters.get("preview-size"));
        try {
            if (!effectsActive()) {
                mCameraDevice.setPreviewTextureAsync(mSurfaceTexture);
                mCameraDevice.startPreviewAsync();
            } else {
                mSurfaceWidth = mCameraScreenNail.getWidth();
                mSurfaceHeight = mCameraScreenNail.getHeight();
                initializeEffectsPreview();
                mEffectsRecorder.startPreview();
            }
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview or setPreviewSurfaceTexture failed", ex);
        }

        mPreviewing = true;
        if(mShutterButton != null) mShutterButton.setEnabled(false);
        mHandler.postDelayed(new Runnable() {
        	 @Override
             public void run() {
        		 if(mSwitchToCamera != null) mSwitchToCamera.setEnabled(true);
//        		 if(mSwitchToVideo != null) mSwitchToVideo.setEnabled(true);
                 if(mShutterButton != null) mShutterButton.setEnabled(true);
        		 if(mSecondLevelIndicatorControlBar != null) mSecondLevelIndicatorControlBar.setEnabled(true);
        	 }
        },BUTTON_DELAY_TIME);
    }

    private void stopPreview() {
        if (mCameraDevice != null) {
        	Log.e(TAG,"Guru : Stop Preview");
        	mCameraDevice.stopPreview();
        }
        mPreviewing = false;
    }

    // Closing the effects out. Will shut down the effects graph.
    private void closeEffects() {
        Log.v(TAG, "Closing effects");
        mEffectType = EffectsRecorder.EFFECT_NONE;
        if (mEffectsRecorder == null) {
            Log.d(TAG, "Effects are already closed. Nothing to do");
            return;
        }
        // This call can handle the case where the camera is already released
        // after the recording has been stopped.
        mEffectsRecorder.release();
        mEffectsRecorder = null;
    }

    // By default, we want to close the effects as well with the camera.
    private void closeCamera() {
        closeCamera(true);
    }

    // In certain cases, when the effects are active, we may want to shutdown
    // only the camera related parts, and handle closing the effects in the
    // effectsUpdate callback.
    // For example, in onPause, we want to make the camera available to
    // outside world immediately, however, want to wait till the effects
    // callback to shut down the effects. In such a case, we just disconnect
    // the effects from the camera by calling disconnectCamera. That way
    // the effects can handle that when shutting down.
    //
    // @param closeEffectsAlso - indicates whether we want to close the
    // effects also along with the camera.
    private void closeCamera(boolean closeEffectsAlso) {
        Log.v(TAG, "closeCamera");
        if (mCameraDevice == null) {
            Log.d(TAG, "already stopped.");
            return;
        }

        if (mEffectsRecorder != null) {
            // Disconnect the camera from effects so that camera is ready to
            // be released to the outside world.
            mEffectsRecorder.disconnectCamera();
        }
        if (closeEffectsAlso) closeEffects();
        mCameraDevice.setZoomChangeListener(null);
        mCameraDevice.setErrorCallback(null);
        CameraHolder.instance().release();
        mCameraDevice = null;
        mPreviewing = false;
        mSnapshotInProgress = false;
    }

    @Override
    protected void onPause() {
    	Log.i(TAG,"onPause()");
        mPaused = true;
        mIsFromOnPause = true;

        // If Camera application exit then close the dialog.
        if (mRotateDialog != null && mRotateDialog.isRotateDialogVisible()) {
            mRotateDialog.dismissDialog();
        }
        if (mMediaRecorderRecording) {
            // Camera will be released in onStopVideoRecording.
            onStopVideoRecording();
        }

        closeCamera();
        if (!effectsActive()) releaseMediaRecorder();
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // if (mSurfaceTexture != null) {
        if (mSurfaceTexture != null&&mCameraScreenNail!=null) {
            mCameraScreenNail.releaseSurfaceTexture();
            mSurfaceTexture = null;
        }
        if(mSurface != null) mSurface.release();
        //END:   modified by xiongzhu at 2013-04-15
        if (effectsActive()) {
            // If the effects are active, make sure we tell the graph that the
            // surfacetexture is not valid anymore. Disconnect the graph from the
            // display.
            mEffectsRecorder.disconnectDisplay();
        } else {
            // Close the file descriptor and clear the video namer only if the
            // effects are not active. If effects are active, we need to wait
            // till we get the callback from the Effects that the graph is done
            // recording. That also needs a change in the stopVideoRecording()
            // call to not call closeCamera if the effects are active, because
            // that will close down the effects are well, thus making this if
            // condition invalid.
            closeVideoFileDescriptor();
            clearVideoNamer();
        }

        //BEGIN: Modified by xiongzhu at 2013-04-15
        // if (mReceiver != null) {
        if (mDidRegister) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
            mDidRegister = false;
        }
        //END: Modified by xiongzhu at 2013-04-15
        
        // BEGIN: Added by zhanghongxing at 2013-07-01
        if (mShutdownReceiver != null) {
            unregisterReceiver(mShutdownReceiver);
            mShutdownReceiver = null;
        }
        // END:   Added by zhanghongxing at 2013-07-01
        
        resetScreenOn();
        if (mSecondLevelIndicatorControlBar != null) {
        	mSecondLevelIndicatorControlBar.dismissSettingPopup();
        }

        if (mOrientationListener != null) mOrientationListener.disable();
        if (mLocationManager != null) mLocationManager.recordLocation(false);

        mHandler.removeMessages(CHECK_DISPLAY_ROTATION);
        mHandler.removeMessages(SWITCH_CAMERA);
        mHandler.removeMessages(SWITCH_CAMERA_START_ANIMATION);
        mHandler.removeMessages(SWITCH_CAMERA_ANIMATION_DONE);
        mHandler.removeMessages(ENABLE_SHUTTER_BUTTON);
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mHandler.removeMessages(UPDATE_RECORD_TIME);

        mPendingSwitchCameraId = -1;
        mSwitchingCamera = false;
        mChangingStoragePlace = false;//Added by xiongzhu at 2013-04-15

        if(isChangedToMms) {
        	Editor editor = mPreferences.edit();
        	editor.putString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,
        			getResources().getString(R.string.pref_videocamera_recording_mode_value_normal));
        	editor.commit();
        	isChangedToMms = false;
        }
        // Call onPause after stopping video recording. So the camera can be
        // released as soon as possible.
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onPause();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (!mMediaRecorderRecording) keepScreenOnAwhile();
    }

    @Override
    public void onBackPressed() {
        if (mPaused) return;
        if (mMediaRecorderRecording) {
            onStopVideoRecording();
        } else if (!collapseCameraControls()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Do not handle any key if the activity is paused.
        if (mPaused) {
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                if (event.getRepeatCount() == 0) {
                    mShutterButton.performClick();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.getRepeatCount() == 0) {
                    mShutterButton.performClick();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MENU:
                if (mMediaRecorderRecording) return true;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                  //removed for lava
//                if (event.getRepeatCount() == 0) {
//                    if (mParameters.isPowerModeSupported() &&
//                        mMediaRecorderRecording && !mPaused &&
//                        !mSnapshotInProgress && !effectsActive())
//                        takeVideoSnapshot();
//                }
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                mShutterButton.setPressed(false);
                return true;

            case KeyEvent.KEYCODE_BACK:
                // If the dialog display when user click back key
                // then close the dialog first.
                if (mRotateDialog != null && mRotateDialog.isRotateDialogVisible()) {
                    mRotateDialog.dismissDialog();
                    return true;
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean isVideoCaptureIntent() {
        String action = getIntent().getAction();
        return (MediaStore.ACTION_VIDEO_CAPTURE.equals(action));
    }

    private void doReturnToCaller(boolean valid) {
        Intent resultIntent = new Intent();
        int resultCode;
        if (valid) {
            resultCode = RESULT_OK;
            resultIntent.setData(mCurrentVideoUri);
        } else {
            resultCode = RESULT_CANCELED;
        }
        setResultEx(resultCode, resultIntent);
        finish();
    }

    private void cleanupEmptyFile() {
        if (mVideoFilename != null) {
            File f = new File(mVideoFilename);
            if (f.length() == 0 && f.delete()) {
                Log.v(TAG, "Empty video file deleted: " + mVideoFilename);
                mVideoFilename = null;
            }
        }
    }

    // Prepares media recorder.
    private void initializeRecorder() {
        Log.v(TAG, "initializeRecorder");
        // If the mCameraDevice is null, then this activity is going to finish
        if (mCameraDevice == null) return;

        if (mSurfaceTexture == null) {
            Log.v(TAG, "SurfaceTexture is null. Wait for surface changed.");
            return;
        }

        Intent intent = getIntent();
        Bundle myExtras = intent.getExtras();

        videoWidth = mProfile.videoFrameWidth;
        videoHeight = mProfile.videoFrameHeight;
        mUnsupportedResolution = false;

        if (mVideoEncoder == MediaRecorder.VideoEncoder.H263) {
            if (videoWidth >= 1280 && videoHeight >= 720) {
                    mUnsupportedResolution = true;
                    Toast.makeText(VideoCamera.this, R.string.error_app_unsupported,
                    Toast.LENGTH_LONG).show();
                    return;
            }
        }

        long requestedSizeLimit = 0;
        closeVideoFileDescriptor();
        if (mIsVideoCaptureIntent && myExtras != null) {
            Uri saveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            if (saveUri != null) {
                try {
                    mVideoFileDescriptor =
                            mContentResolver.openFileDescriptor(saveUri, "rw");
                    mCurrentVideoUri = saveUri;
                } catch (java.io.FileNotFoundException ex) {
                    // invalid uri
                    Log.e(TAG, ex.toString());
                }
            }
            requestedSizeLimit = myExtras.getLong(MediaStore.EXTRA_SIZE_LIMIT);
        }

        if (myExtras == null && mCurVideoRecordingMode != null &&
        		mCurVideoRecordingMode.equals(getString(R.string.pref_videocamera_recording_mode_value_mms))) {
        	requestedSizeLimit = 260249;
        }
        if(mMediaRecorder != null) {
        	mMediaRecorder.release();
        	mMediaRecorder = null;
        }
        mMediaRecorder = new MediaRecorder();

        // Unlock the camera object before passing it to media recorder.
        mCameraDevice.unlock();
        mMediaRecorder.setCamera(mCameraDevice.getCamera());
        String hfr = mParameters.getVideoHighFrameRate();
        if (!mCaptureTimeLapse && ((hfr == null) || ("off".equals(hfr)))) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mProfile.audioCodec = mAudioEncoder;
        } else {
            mProfile.audioCodec = -1; //not set
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mProfile.videoCodec = mVideoEncoder;
        mProfile.duration = 0;

        mMediaRecorder.setProfile(mProfile);
        mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);
        if (mCaptureTimeLapse) {
            mMediaRecorder.setCaptureRate((1000 / (double) mTimeBetweenTimeLapseFrameCaptureMs));
        }

        Location loc = null;
        if (mIsRecordLocation) {
        	loc = mLocationManager.getCurrentLocation();
        }
        if (loc != null) {
            mMediaRecorder.setLocation((float) loc.getLatitude(),
                    (float) loc.getLongitude());
        }


        // Set output file.
        // Try Uri in the intent first. If it doesn't exist, use our own
        // instead.
        if (mVideoFileDescriptor != null) {
            mMediaRecorder.setOutputFile(mVideoFileDescriptor.getFileDescriptor());
        } else {
            generateVideoFilename(mProfile.fileFormat);
            mMediaRecorder.setOutputFile(mVideoFilename);
        }

        // Set maximum file size.
        // BEGIN: Modified by zhanghongxing at 2013-09-03
        // long maxFileSize = mStorageSpace - Storage.LOW_STORAGE_THRESHOLD;
        long maxFileSize = mStorageSpace - Storage.LOWEST_STORAGE_THRESHOLD;
        // END:   Modified by zhanghongxing at 2013-09-03
        if (requestedSizeLimit > 0 && requestedSizeLimit < maxFileSize) {
            maxFileSize = requestedSizeLimit;
        }

        try {
            mMediaRecorder.setMaxFileSize(maxFileSize);
        } catch (RuntimeException exception) {
            // We are going to ignore failure of setMaxFileSize here, as
            // a) The composer selected may simply not support it, or
            // b) The underlying media framework may not handle 64-bit range
            // on the size restriction.
        }

        // See android.hardware.Camera.Parameters.setRotation for
        // documentation.
        // Note that mOrientation here is the device orientation, which is the opposite of
        // what activity.getWindowManager().getDefaultDisplay().getRotation() would return,
        // which is the orientation the graphics need to rotate in order to render correctly.
        int rotation = 0;
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        if (mOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - mOrientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (info.orientation + mOrientation) % 360;
            }
        } else {
            rotation = info.orientation;
        }
        mMediaRecorder.setOrientationHint(rotation);
        mOrientationCompensationAtRecordStart = mOrientationCompensation;

        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            Log.e(TAG, "prepare failed for " + mVideoFilename, e);
            releaseMediaRecorder();
            new RotateTextToast(VideoCamera.this, R.string.prepare_failed,
                    mOrientationCompensation).show();
            mCameraDevice.lock();
            return;
        }

        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnInfoListener(this);
    }

    private void initializeEffectsPreview() {
        Log.v(TAG, "initializeEffectsPreview");
        // If the mCameraDevice is null, then this activity is going to finish
        if (mCameraDevice == null) return;

        boolean inLandscape = (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);

        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];

        mEffectsDisplayResult = false;
        mEffectsRecorder = new EffectsRecorder(this);

        // TODO: Confirm none of the following need to go to initializeEffectsRecording()
        // and none of these change even when the preview is not refreshed.
        mEffectsRecorder.setCameraDisplayOrientation(mCameraDisplayOrientation);
        mEffectsRecorder.setCamera(mCameraDevice.getCamera());
        mEffectsRecorder.setCameraFacing(info.facing);
        mEffectsRecorder.setProfile(mProfile);
        mEffectsRecorder.setEffectsListener(this);
        mEffectsRecorder.setOnInfoListener(this);
        mEffectsRecorder.setOnErrorListener(this);

        // The input of effects recorder is affected by
        // android.hardware.Camera.setDisplayOrientation. Its value only
        // compensates the camera orientation (no Display.getRotation). So the
        // orientation hint here should only consider sensor orientation.
        int orientation = 0;
        if (mOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            orientation = mOrientation;
        }
        mEffectsRecorder.setOrientationHint(orientation);

        mOrientationCompensationAtRecordStart = mOrientationCompensation;

        mEffectsRecorder.setPreviewSurfaceTexture(mSurfaceTexture, mSurfaceWidth, mSurfaceHeight);

        if (mEffectType == EffectsRecorder.EFFECT_BACKDROPPER &&
                ((String) mEffectParameter).equals(EFFECT_BG_FROM_GALLERY)) {
            mEffectsRecorder.setEffect(mEffectType, mEffectUriFromGallery);
        } else {
            mEffectsRecorder.setEffect(mEffectType, mEffectParameter);
        }
    }

    private void initializeEffectsRecording() {
        Log.v(TAG, "initializeEffectsRecording");

        Intent intent = getIntent();
        Bundle myExtras = intent.getExtras();

        long requestedSizeLimit = 0;
        closeVideoFileDescriptor();
        if (mIsVideoCaptureIntent && myExtras != null) {
            Uri saveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            if (saveUri != null) {
                try {
                    mVideoFileDescriptor =
                            mContentResolver.openFileDescriptor(saveUri, "rw");
                    mCurrentVideoUri = saveUri;
                } catch (java.io.FileNotFoundException ex) {
                    // invalid uri
                    Log.e(TAG, ex.toString());
                }
            }
            requestedSizeLimit = myExtras.getLong(MediaStore.EXTRA_SIZE_LIMIT);
        }

        mEffectsRecorder.setProfile(mProfile);
        // important to set the capture rate to zero if not timelapsed, since the
        // effectsrecorder object does not get created again for each recording
        // session
        if (mCaptureTimeLapse) {
            mEffectsRecorder.setCaptureRate((1000 / (double) mTimeBetweenTimeLapseFrameCaptureMs));
        } else {
            mEffectsRecorder.setCaptureRate(0);
        }

        // Set output file
        if (mVideoFileDescriptor != null) {
            mEffectsRecorder.setOutputFile(mVideoFileDescriptor.getFileDescriptor());
        } else {
            generateVideoFilename(mProfile.fileFormat);
            mEffectsRecorder.setOutputFile(mVideoFilename);
        }

        // Set maximum file size.
        // BEGIN: Modified by zhanghongxing at 2013-09-03
        // long maxFileSize = mStorageSpace - Storage.LOW_STORAGE_THRESHOLD;
        long maxFileSize = mStorageSpace - Storage.LOWEST_STORAGE_THRESHOLD;
        // END:   Modified by zhanghongxing at 2013-09-03
        if (requestedSizeLimit > 0 && requestedSizeLimit < maxFileSize) {
            maxFileSize = requestedSizeLimit;
        }
        mEffectsRecorder.setMaxFileSize(maxFileSize);
        mEffectsRecorder.setMaxDuration(mMaxVideoDurationInMs);
    }


    private void releaseMediaRecorder() {
        Log.v(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
            cleanupEmptyFile();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mVideoFilename = null;
    }

    private void releaseEffectsRecorder() {
        Log.v(TAG, "Releasing effects recorder.");
        if (mEffectsRecorder != null) {
            cleanupEmptyFile();
            mEffectsRecorder.release();
            mEffectsRecorder = null;
        }
        mEffectType = EffectsRecorder.EFFECT_NONE;
        mVideoFilename = null;
    }

    private void generateVideoFilename(int outputFileFormat) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        // Used when emailing.
        String filename = title + convertOutputFormatToFileExt(outputFileFormat);
        String mime = convertOutputFormatToMimeType(outputFileFormat);
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // String path = Storage.DIRECTORY + '/' + filename;
        String path = Storage.getDirectory() + '/' + filename;
        //END:   Modified by xiongzhu at 2013-04-15
        String tmpPath = path + ".tmp";
        mCurrentVideoValues = new ContentValues(7);
        mCurrentVideoValues.put(Video.Media.TITLE, title);
        mCurrentVideoValues.put(Video.Media.DISPLAY_NAME, filename);
        mCurrentVideoValues.put(Video.Media.DATE_TAKEN, dateTaken);
        mCurrentVideoValues.put(Video.Media.MIME_TYPE, mime);
        mCurrentVideoValues.put(Video.Media.DATA, path);
        mCurrentVideoValues.put(Video.Media.RESOLUTION,
                Integer.toString(mProfile.videoFrameWidth) + "x" +
                Integer.toString(mProfile.videoFrameHeight));
        Location loc = null;
        if (mIsRecordLocation) {
        	loc = mLocationManager.getCurrentLocation();
        }
        if (loc != null) {
            mCurrentVideoValues.put(Video.Media.LATITUDE, loc.getLatitude());
            mCurrentVideoValues.put(Video.Media.LONGITUDE, loc.getLongitude());
        }
        mVideoFilename = tmpPath;
        Log.v(TAG, "New video filename: " + mVideoFilename);
    }

    private boolean addVideoToMediaStore() {
        boolean fail = false;
        if (mVideoFileDescriptor == null) {
            mCurrentVideoValues.put(Video.Media.SIZE,
                    new File(mCurrentVideoFilename).length());
            if (duration > 0) {
                if (mCaptureTimeLapse) {
                    duration = getTimeLapseVideoLength(duration);
                }
                mCurrentVideoValues.put(Video.Media.DURATION, duration);
            } else {
                Log.w(TAG, "Video duration <= 0 : " + duration);
            }
            try {

                // Rename the video file to the final name. This avoids other
                // apps reading incomplete data.  We need to do it after the
                // above mVideoNamer.getUri() call, so we are certain that the
                // previous insert to MediaProvider is completed.
                String finalName = mCurrentVideoValues.getAsString(
                        Video.Media.DATA);
                if (new File(mCurrentVideoFilename).renameTo(new File(finalName))) {
                    mCurrentVideoFilename = finalName;
                }

                mVideoNamer.prepareUri(mContentResolver, mCurrentVideoValues);
                mCurrentVideoUri = mVideoNamer.getUri();
                mContentResolver.update(mCurrentVideoUri, mCurrentVideoValues
                        , null, null);
                sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_VIDEO,
                        mCurrentVideoUri));
            } catch (Exception e) {
                // We failed to insert into the database. This can happen if
                // the SD card is unmounted.
                Log.e(TAG, "failed to add video to media store", e);
                mCurrentVideoUri = null;
                mCurrentVideoFilename = null;
                fail = true;
            } finally {
                Log.v(TAG, "Current video URI: " + mCurrentVideoUri);
            }
        }
        mCurrentVideoValues = null;
        return fail;
    }

    private void deleteCurrentVideo() {
        // Remove the video and the uri if the uri is not passed in by intent.
        if (mCurrentVideoFilename != null) {
            deleteVideoFile(mCurrentVideoFilename);
            mCurrentVideoFilename = null;
            if (mCurrentVideoUri != null) {
                mContentResolver.delete(mCurrentVideoUri, null, null);
                mCurrentVideoUri = null;
            }
        }
        updateAndShowStorageHint();
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }

    private PreferenceGroup filterPreferenceScreenByIntent(
            PreferenceGroup screen) {
        Intent intent = getIntent();
        if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            CameraSettings.removePreferenceFromScreen(screen,
                    CameraSettings.KEY_VIDEO_QUALITY);
        }

        if (intent.hasExtra(MediaStore.EXTRA_DURATION_LIMIT)) {
            CameraSettings.removePreferenceFromScreen(screen,
                    CameraSettings.KEY_VIDEO_QUALITY);
        }
        return screen;
    }

    // from MediaRecorder.OnErrorListener
    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(TAG, "MediaRecorder error. what=" + what + ". extra=" + extra);
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            // We may have run out of space on the sdcard.
            stopVideoRecording();
            updateAndShowStorageHint();
        }
    }

    // from MediaRecorder.OnInfoListener
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            if (mMediaRecorderRecording) onStopVideoRecording();
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            if (mMediaRecorderRecording) onStopVideoRecording();

            // Show the toast.
            Toast.makeText(this, R.string.video_reach_size_limit,
                    Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Make sure we're not recording music playing in the background, ask the
     * MediaPlaybackService to pause playback.
     */
    private void pauseAudioPlayback() {
        // Shamelessly copied from MediaPlaybackService.java, which
        // should be public, but isn't.
    	Intent pauseIntent = new Intent("com.android.music.musicservicecommand");
        pauseIntent.putExtra("command","lava_pause"); // lava_pause
        sendBroadcast(pauseIntent);
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");

        sendBroadcast(i);
        
    }
    
    private void resumeMusic() {
    	Intent resumeIntent = new Intent("com.android.music.musicservicecommand");
    	resumeIntent.putExtra("command","lava_resume"); // lava_resume
    	sendBroadcast(resumeIntent);
    }

    // For testing.
    public boolean isRecording() {
        return mMediaRecorderRecording;
    }

    private void startVideoRecording() {
        Log.v(TAG, "startVideoRecording");
        
        if(!mSecondLevelIndicatorControlBar.isEnabled()) return;
        if (!(mIsVideoCaptureIntent && isRecording())) {
        	if(!mHandler.hasMessages(ENABLE_SHUTTER_BUTTON)) {
                mHandler.sendEmptyMessageDelayed(
                		ENABLE_SHUTTER_BUTTON, SHUTTER_BUTTON_TIMEOUT);
        	} else return;
        }

        setSwipingEnabled(false);

        updateAndShowStorageHint();
        if (mStorageSpace <= Storage.LOW_STORAGE_THRESHOLD) {
            Log.v(TAG, "Storage issue, ignore the start request");
            return;
        }

        //BEGIN: Added by zhanghongxing at 2012-12-28 for DER-289
        if (mIsCalling) {
            showCallingToast();
            return;
        }
        //END:   Added by zhanghongxing at 2012-12-28

        if( mUnsupportedHFRVideoSize == true) {
            Log.v(TAG, "Unsupported HFR and video size combinations");
             mParameters = mCameraDevice.getParameters();
            String errorMsg = "Only ";
            List<Size> sizeList =  mParameters.getSupportedHfrSizes();
            for(int i=1,n=sizeList.size(); i <= n; i++){
               if(i != n){
                   errorMsg = errorMsg +
                       HFR_SIZES.get(sizeList.get(i-1).width+"x"+sizeList.get(i-1).height)+",";
               } else {
                   errorMsg = errorMsg + " and " +
                       HFR_SIZES.get(sizeList.get(i-1).width+"x"+sizeList.get(i-1).height);
               }
            }
            errorMsg = errorMsg + " are supported when HFR is on";
            Toast.makeText(VideoCamera.this,errorMsg , Toast.LENGTH_SHORT).show();
            return;
        }

        if( mUnsupportedHFRVideoCodec == true) {
            Log.v(TAG, "Unsupported HFR and video codec combinations");
            Toast.makeText(VideoCamera.this, R.string.error_app_unsupported_hfr_codec,
            Toast.LENGTH_SHORT).show();
            return;
        }

       if(mFlashMode != null && !mIsLowPowerMode) {
    		mParameters.setFlashMode(mFlashMode);
    		mCameraDevice.setParameters(mParameters);
    	}
        mCurrentVideoUri = null;
        if (effectsActive()) {
            initializeEffectsRecording();
            if (mEffectsRecorder == null) {
                Log.e(TAG, "Fail to initialize effect recorder");
                closeCameraFlash();
                return;
            }
        } else {
            initializeRecorder();
            if (mUnsupportedResolution == true) {
                Log.v(TAG, "Unsupported Resolution according to target");
                mCameraDevice.lock();
                closeCameraFlash();
                return;
            }
            if (mMediaRecorder == null) {
                Log.e(TAG, "Fail to initialize media recorder");
                closeCameraFlash();
                return;
            }
        }

        pauseAudioPlayback();

        mGpsIndicator.setVisibility(View.GONE);

        if (effectsActive()) {
            try {
                mEffectsRecorder.startRecording();
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not start effects recorder. ", e);
                releaseEffectsRecorder();
                closeCameraFlash();
                return;
            }
        } else {
            try {
                mMediaRecorder.start(); // Recording is now started
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not start media recorder. ", e);
                  Toast.makeText(VideoCamera.this, R.string.start_media_recorder_err,
            Toast.LENGTH_SHORT).show();
                releaseMediaRecorder();
                // If start fails, frameworks will not lock the camera for us.
                mCameraDevice.lock();
                setSwipingEnabled(true);
                closeCameraFlash();
                return;
            }
        }

        // Parameters may have been changed by media recorder when recording
        // starts. To reduce latency, we do not update mParameters during zoom.
        // Keep this up-to-date now. Otherwise, we may revert the video size
        // unexpectedly.
        mParameters = mCameraDevice.getParameters();

        enableCameraControls(false);
        mMediaRecorderRecording = true;
        mRecordingStartTime = SystemClock.uptimeMillis();
        showRecordingUI(true);
        updateRecordingTime();
        keepScreenOn();
        
        new Thread() {
       	 @Override
            public void run() {
       		 while(mMediaRecorderRecording) {
       			 long storageSpace = Storage.getAvailableSpace();
       			 if(storageSpace < Storage.LOW_STORAGE_THRESHOLD) {
       				 Log.i(TAG,"==zzw:storage is not enough,stop recoding");
       				 mHandler.sendEmptyMessage(STOP_RECORDING);
       				 break;
       			 }
       			 
       			 try {
       				 sleep(500);
       			 } catch(Exception e) {
       				 
       			 }
       		 }
            }
       }.start();
    }

    private void showRecordingUI(boolean recording) {
        if (recording) {
            //mIndicatorControlContainer.dismissSecondLevelIndicator();
        	if (mSecondLevelIndicatorControlBar != null && mSecondLevelIndicatorControlBar.getActiveSettingPopup() != null) {
        		mSecondLevelIndicatorControlBar.dismissSettingPopup();
        	}
            //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
            // if (mThumbnailView != null) mThumbnailView.setEnabled(false);
            if (mThumbnailWindow != null) mThumbnailWindow.setEnabled(false);
            //END:   Modified by zhanghongxing at 2013-01-09
            
            
//            mShutterButton.setBackgroundResource(R.drawable.shutterbutton_background);
            mShutterButton.setImageResource(R.drawable.btn_shutter_video_recording);
            
            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
            if (mReviewControl != null) mReviewControl.setVisibility(View.GONE);
            if (mCaptureTimeLapse) {
                mIndicatorControlContainer.startTimeLapseAnimation(
                        mTimeBetweenTimeLapseFrameCaptureMs,
                        mRecordingStartTime);
            }
        } else {
            //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
            // if (mThumbnailView != null) mThumbnailView.setEnabled(true);
            if (mThumbnailWindow != null) mThumbnailWindow.setEnabled(true);
            //END:   Modified by zhanghongxing at 2013-01-09
            
//            mShutterButton.setBackgroundResource(R.drawable.shutterbutton_background);
            mShutterButton.setImageResource(R.drawable.btn_shutter_video);
               
            mRecordingTimeView.setVisibility(View.GONE);
            if (mReviewControl != null) mReviewControl.setVisibility(View.VISIBLE);
            if (mCaptureTimeLapse) {
                mIndicatorControlContainer.stopTimeLapseAnimation();
            }
        }
    }

    private void getThumbnail() {
        if (mCurrentVideoUri != null) {
            Bitmap videoFrame = Thumbnail.createVideoThumbnailBitmap(mCurrentVideoFilename,
                    mThumbnailViewWidth);
            if (videoFrame != null) {
                mThumbnail = Thumbnail.createThumbnail(mCurrentVideoUri, videoFrame, 0);
                mThumbnailView.setBitmap(mThumbnail.getBitmap());
            }
        }
    }

    private void showAlert() {
        Bitmap bitmap = null;
        if (mVideoFileDescriptor != null) {
            bitmap = Thumbnail.createVideoThumbnailBitmap(mVideoFileDescriptor.getFileDescriptor(),
                    mPreviewFrameLayout.getWidth());
        } else if (mCurrentVideoFilename != null) {
            bitmap = Thumbnail.createVideoThumbnailBitmap(mCurrentVideoFilename,
                    mPreviewFrameLayout.getWidth());
        }
        if (bitmap != null) {
            // MetadataRetriever already rotates the thumbnail. We should rotate
            // it to match the UI orientation (and mirror if it is front-facing camera).
            CameraInfo[] info = CameraHolder.instance().getCameraInfo();
            boolean mirror = (info[mCameraId].facing == CameraInfo.CAMERA_FACING_FRONT);
            bitmap = Util.rotateAndMirror(bitmap, -mOrientationCompensationAtRecordStart,
                    mirror);
            mReviewImage.setImageBitmap(bitmap);
            mReviewImage.setVisibility(View.VISIBLE);
        }

        Util.fadeOut(mShutterButton);
        Util.fadeOut(mIndicatorControlContainer);
        Util.fadeOut(mSecondLevelIndicatorControlBar);

        Util.fadeIn(mReviewRetakeButton);
        Util.fadeIn((View) mReviewDoneButton);
        Util.fadeIn(mReviewPlayButton);
        findViewById(R.id.frame_layout).setBackgroundColor(Color.BLACK);
        showTimeLapseUI(false);

    }

    private void hideAlert() {
        mReviewImage.setVisibility(View.GONE);
        mShutterButton.setEnabled(true);
        enableCameraControls(true);

        Util.fadeOut((View) mReviewDoneButton);
        Util.fadeOut(mReviewRetakeButton);
        Util.fadeOut(mReviewPlayButton);

        Util.fadeIn(mShutterButton);
        Util.fadeIn(mIndicatorControlContainer);
        Util.fadeIn(mSecondLevelIndicatorControlBar);

        if (mCaptureTimeLapse) {
            showTimeLapseUI(true);
        }
    }

    private boolean stopVideoRecording() {
        Log.v(TAG, "stopVideoRecording");
        if(mIsRecordLocation) {
            mGpsIndicator.setVisibility(View.VISIBLE);
        }
        setSwipingEnabled(true);
        boolean fail = false;
        duration = SystemClock.uptimeMillis() - mRecordingStartTime;
        if (mMediaRecorderRecording) {
            boolean shouldAddToMediaStoreNow = false;

            try {
                if (effectsActive()) {
                    // This is asynchronous, so we can't add to media store now because thumbnail
                    // may not be ready. In such case addVideoToMediaStore is called later
                    // through a callback from the MediaEncoderFilter to EffectsRecorder,
                    // and then to the VideoCamera.
                    mEffectsRecorder.stopRecording();
                } else {
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.stop();
                    shouldAddToMediaStoreNow = true;
                }
                mCurrentVideoFilename = mVideoFilename;
                Log.v(TAG, "stopVideoRecording: Setting current video filename: "
                        + mCurrentVideoFilename);
            } catch (RuntimeException e) {
                Log.e(TAG, "stop fail",  e);
                if (mVideoFilename != null) deleteVideoFile(mVideoFilename);
                fail = true;
            }
            mMediaRecorderRecording = false;

            // If the activity is paused, this means activity is interrupted
            // during recording. Release the camera as soon as possible because
            // face unlock or other applications may need to use the camera.
            // However, if the effects are active, then we can only release the
            // camera and cannot release the effects recorder since that will
            // stop the graph. It is possible to separate out the Camera release
            // part and the effects release part. However, the effects recorder
            // does hold on to the camera, hence, it needs to be "disconnected"
            // from the camera in the closeCamera call.
            if (mPaused) {
                // Closing only the camera part if effects active. Effects will
                // be closed in the callback from effects.
                boolean closeEffects = !effectsActive();
                closeCamera(closeEffects);
            }

            showRecordingUI(false);
            if (!mIsVideoCaptureIntent) {
                enableCameraControls(true);
            }
            // The orientation was fixed during video recording. Now make it
            // reflect the device orientation as video recording is stopped.
            setOrientationIndicator(mOrientationCompensation, true);
            keepScreenOnAwhile();
            if (shouldAddToMediaStoreNow) {
                if (addVideoToMediaStore()) fail = true;
            }
        }
        // always release media recorder if no effects running
        if (!effectsActive()) {
            releaseMediaRecorder();
        }
        
        closeCameraFlash();

        resumeMusic();
        return fail;
    }

    private void closeCameraFlash() {
    	if(mFlashMode != null && !mFlashMode.equals(Parameters.FLASH_MODE_OFF)
    			&& mCameraDevice != null && mParameters != null) {
        	mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        	mCameraDevice.setParameters(mParameters);
        }
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

    private void keepScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / 1000; // round down to compute seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        StringBuilder timeStringBuilder = new StringBuilder();

        // Hours
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);

            timeStringBuilder.append(':');
        }

        // Minutes
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');

        // Seconds
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);

        // Centi seconds
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - seconds * 1000) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }

        return timeStringBuilder.toString();
    }

    private long getTimeLapseVideoLength(long deltaMs) {
        // For better approximation calculate fractional number of frames captured.
        // This will update the video time at a higher resolution.
        double numberOfFrames = (double) deltaMs / mTimeBetweenTimeLapseFrameCaptureMs;
        return (long) (numberOfFrames / mProfile.videoFrameRate * 1000);
    }

    private void updateRecordingTime() {
        if (!mMediaRecorderRecording) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;

        // Starting a minute before reaching the max duration
        // limit, we'll countdown the remaining time instead.
        boolean countdownRemainingTime = (mMaxVideoDurationInMs != 0
                && delta >= mMaxVideoDurationInMs - 60000);

        long deltaAdjusted = delta;
        if (countdownRemainingTime) {
            deltaAdjusted = Math.max(0, mMaxVideoDurationInMs - deltaAdjusted) + 999;
        }
        String text;

        long targetNextUpdateDelay;
        if (!mCaptureTimeLapse) {
            text = millisecondToTimeString(deltaAdjusted, false);
            targetNextUpdateDelay = 1000;
        } else {
            // The length of time lapse video is different from the length
            // of the actual wall clock time elapsed. Display the video length
            // only in format hh:mm:ss.dd, where dd are the centi seconds.
            text = millisecondToTimeString(getTimeLapseVideoLength(delta), true);
            targetNextUpdateDelay = mTimeBetweenTimeLapseFrameCaptureMs;
        }

        mRecordingTimeView.setText(text);

        if (mRecordingTimeCountsDown != countdownRemainingTime) {
            // Avoid setting the color on every update, do it only
            // when it needs changing.
            mRecordingTimeCountsDown = countdownRemainingTime;

            int color = getResources().getColor(countdownRemainingTime
                    ? R.color.recording_time_remaining_text
                    : R.color.recording_time_elapsed_text);

            mRecordingTimeView.setTextColor(color);
        }

        long actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
        mHandler.sendEmptyMessageDelayed(
                UPDATE_RECORD_TIME, actualNextUpdateDelay);
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }


    private void qcomSetCameraParameters(){
        //add QCOM Parameters here
        //
        // Set color effect parameter.
    	//BEGIN: Modified by zhanghongxing at 2013-01-06 for DER-173
        // String colorEffect = mPreferences.getString(
        //         CameraSettings.KEY_COLOR_EFFECT,
        //         getString(R.string.pref_camera_coloreffect_default));
    	
    	String videoColorEffect = mPreferences.getString(
                CameraSettings.KEY_VIDEO_COLOR_EFFECT,
                getString(R.string.pref_camera_coloreffect_default));
        Log.e(TAG, "===Color effect value =" + videoColorEffect);
        if (isSupported(videoColorEffect, mParameters.getSupportedColorEffects())) {
			Log.e(TAG,"===Color,setColorEffect:"+videoColorEffect);
            mParameters.setColorEffect(videoColorEffect);
        }
        
        String colorEffect = mPreferences.getString(
                CameraSettings.KEY_VIDEO_EFFECT,
                getString(R.string.pref_camera_coloreffect_default));
        //END:   Modified by zhanghongxing at 2013-01-06
        //Log.e(TAG, " effect value =" + colorEffect);
        //if (isSupported(colorEffect, mParameters.getSupportedColorEffects())) {
           // mParameters.setColorEffect(colorEffect);
        //}

        mUnsupportedHFRVideoSize = false;
        mUnsupportedHFRVideoCodec = false;
        // To set preview format as YV12 , run command
//         "adb shell setprop "debug.camera.yv12" true"
//        String yv12formatset = SystemProperties.get("debug.camera.yv12");
//        if(yv12formatset.equals("true")) {
//            Log.v(TAG, "preview format set to YV12");
//            mParameters.setPreviewFormat (ImageFormat.YV12);
//        }
//        if(videoWidth == 1280 &&  videoHeight == 720) {
//            mParameters.set("preview-format","yuv420sp");
//            mParameters.set("video-frame-format","yuv420sp");
//        } else {
            mParameters.set("preview-format","yuv420sp");
            mParameters.set("video-frame-format","yuv420sp");
//        }

        // Set High Frame Rate.
        String HighFrameRate = mPreferences.getString(
                CameraSettings.KEY_VIDEO_HIGH_FRAME_RATE,
                getString(R.string.pref_camera_hfr_default));
        if (mMmsFlag) {
            HighFrameRate = "off";
            mMmsFlag = false;
        }

        if ((oldHFR.equals("60") && HighFrameRate.equals("off")) ||
            (oldHFR.equals("off") && HighFrameRate.equals("60"))) {
            bFlagHFRUpdateUI = true;
            if(mCameraId != CameraInfo.CAMERA_FACING_BACK) {
                bFlagHFRUpdateUI = false;
            }
        }
        oldHFR = HighFrameRate;

        if(!("off".equals(HighFrameRate))){
            mUnsupportedHFRVideoSize = true;
            String hfrsize = videoWidth+"x"+videoHeight;
            Log.v(TAG, "current set resolution is : "+hfrsize);
            try {
                for(Size size :  mParameters.getSupportedHfrSizes()){
                    if(size != null) {
                        Log.v(TAG, "supported hfr size : "+ size.width+ " "+size.height);
                        if(videoWidth == size.width && videoHeight == size.height) {
                            mUnsupportedHFRVideoSize = false;
                            Log.v(TAG,"Current hfr resolution is supported");
                            break;
                        }
                    }
                }
            } catch (NullPointerException e){
                Log.e(TAG, "supported hfr sizes is null");
            }

            if(mUnsupportedHFRVideoSize)
                Log.e(TAG,"Unsupported hfr resolution");
            if(mVideoEncoder != MediaRecorder.VideoEncoder.H264){
                mUnsupportedHFRVideoCodec = true;
            }
        }
        //if (isSupported(HighFrameRate,
        //            mParameters.getSupportedVideoHighFrameRateModes()) && ! mUnsupportedHFRVideoSize) {
            mParameters.setVideoHighFrameRate(HighFrameRate);
        //}
        //else
        //  mParameters.setVideoHighFrameRate("off");
    }

    @SuppressWarnings("deprecation")
    private void setCameraParameters() {
        Log.e(TAG,"Preview dimension in App->"+mDesiredPreviewWidth+"X"+mDesiredPreviewHeight);
        mParameters.setPreviewSize(mDesiredPreviewWidth, mDesiredPreviewHeight);
        mParameters.setPreviewFrameRate(mProfile.videoFrameRate);


        videoWidth = mProfile.videoFrameWidth;
        videoHeight = mProfile.videoFrameHeight;
        String recordSize = videoWidth + "x" + videoHeight;
        //To set the parameter KEY_VIDE_SIZE
        Log.e(TAG,"Video dimension in App->"+recordSize);
        mParameters.set("video-size", recordSize);

        //BEGIN: Added by zhanghongxing at 2012-12-28 for DER-153
        // Set anti banding parameter.
        String antiBanding = getString(R.string.pref_camera_antibanding_default);
        Log.v(TAG, "antiBanding value =" + antiBanding);
        if (isSupported(antiBanding, mParameters.getSupportedAntibanding())) {
            mParameters.setAntibanding(antiBanding);
        }
        //END:   Added by zhanghongxing at 2012-12-28

        // Set flash mode.
        if (mShowCameraAppView) {
            mFlashMode = mPreferences.getString(
                    CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE,
                    getString(R.string.pref_camera_video_flashmode_default));
        } else {
        	mFlashMode = Parameters.FLASH_MODE_OFF;
        }
        List<String> supportedFlash = mParameters.getSupportedFlashModes();
        if (isSupported(mFlashMode, supportedFlash)) {
//            mParameters.setFlashMode(flashMode);
        } else {
//            flashMode = mParameters.getFlashMode();
//            if (flashMode == null) {
//                flashMode = getString(
//                        R.string.pref_camera_flashmode_no_flash);
//            }
        	mFlashMode = null;
        }

        // Set white balance parameter.
        //BEGIN: Modified by zhanghongxing at 2013-01-06 for DER-173
        // String whiteBalance = mPreferences.getString(
        //         CameraSettings.KEY_WHITE_BALANCE,
        //         getString(R.string.pref_camera_whitebalance_default));
        String whiteBalance = mPreferences.getString(
                CameraSettings.KEY_VIDEO_WHITE_BALANCE,
                getString(R.string.pref_camera_whitebalance_default));
        //END:   Modified by zhanghongxing at 2013-01-06
        if (isSupported(whiteBalance,
                mParameters.getSupportedWhiteBalance())) {
            mParameters.setWhiteBalance(whiteBalance);
        } else {
            whiteBalance = mParameters.getWhiteBalance();
            if (whiteBalance == null) {
                whiteBalance = Parameters.WHITE_BALANCE_AUTO;
            }
        }

        // Set zoom.
        if (mParameters.isZoomSupported()) {
            mParameters.setZoom(mZoomValue);
        }

        // Set continuous autofocus.
        List<String> supportedFocus = mParameters.getSupportedFocusModes();
        if (isSupported(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO, supportedFocus)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        mParameters.setRecordingHint(true);

        // Enable video stabilization. Convenience methods not available in API
        // level <= 14
        String vstabSupported = mParameters.get("video-stabilization-supported");
        if ("true".equals(vstabSupported)) {
            mParameters.set("video-stabilization", "true");
        }

        // Set picture size.
        // The logic here is different from the logic in still-mode camera.
        // There we determine the preview size based on the picture size, but
        // here we determine the picture size based on the preview size.
        List<Size> supported = mParameters.getSupportedPictureSizes();
        Size optimalSize = Util.getOptimalVideoSnapshotPictureSize(supported,
                (double) mDesiredPreviewWidth / mDesiredPreviewHeight);
        Size original = mParameters.getPictureSize();
        if (!original.equals(optimalSize)) {
            mParameters.setPictureSize(optimalSize.width, optimalSize.height);
        }
        Log.v(TAG, "Video snapshot size is " + optimalSize.width + "x" +
                optimalSize.height);

        // Set JPEG quality.
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(mCameraId,
                CameraProfile.QUALITY_HIGH);
        mParameters.setJpegQuality(jpegQuality);

        //Call Qcom related Camera Parameters
        qcomSetCameraParameters();
        
        mParameters.set("param_group_update", mNeedupdateCameraUI);

        mCameraDevice.setParameters(mParameters);
        // Keep preview size up to date.

        updateCameraScreenNailSize(mDesiredPreviewWidth, mDesiredPreviewHeight);
        
        //Add by wangbin at 2013-03-22
        mSilentMode = mPreferences.getString(
                CameraSettings.KEY_SILENT_MODE, 
                getString(R.string.pref_silentmode_default));
        if (CameraSettings.SILENT_MODE_OFF.equals(mSilentMode)) {
        	enableShutterSound(true);
        } else {
        	enableShutterSound(false);
        }
    }
	
    public void updateCameraUI(){
        mNeedupdateCameraUI = mParameters.getInt("param_group_update");
        if(mNeedupdateCameraUI != 0) {
            Log.e(TAG, "updateCameraUI: need reload parameters group to update camera UI.");
            //reload parameters group to update camera UI
            initializeIndicatorControl();
            //reset mNeedupdateCameraUI to false.
            mNeedupdateCameraUI = 0;
            if(mIsVideoQualityChanged) {
            	if(!bFlagDisanableToUpdateUI) {
                    mSecondLevelIndicatorControlBar.showOtherSettingPopup();
            	}
            	bFlagDisanableToUpdateUI = false;
                mIsVideoQualityChanged = false;
            }
        }
    }
    
    private void updateCameraScreenNailSize(int width, int height) {
        if (mCameraDisplayOrientation % 180 != 0) {
            int tmp = width;
            width = height;
            height = tmp;
        }

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
            if(mSurface != null) mSurface.release();
            mSurface = new Surface(mSurfaceTexture);
        }
    }
    private void  updateUIforHFR() {
        if (mPreferences != null &&
            mSecondLevelIndicatorControlBar != null) {

            String HighFrameRate = mPreferences.getString(
                    CameraSettings.KEY_VIDEO_HIGH_FRAME_RATE,
                    getString(R.string.pref_camera_hfr_default));
            if (HighFrameRate == null || "off".equals(HighFrameRate)) {
                mHfr = false;
            } else {
                mHfr = true;
            }

            //String prop = SystemProperties.get("persist.camera.hfr");
            //Log.e(TAG, "updateUIforHFR: prop ="+prop);
            //if (prop.length() > 0 ) {
            //    if ("on".equals(prop)) {
            //        mHfr = true;
            //    } else if("off".equals(prop)){
            //        mHfr = false;
            //    }
            //}
             
             //START : modified by xiongzhu for ARD-100
            //boolean zoom = mParameters.isZoomSupported();
              boolean zoom = mIsZoomSupported;
            //END : modified by xiongzhu for ARD-100

            Log.v(TAG, "updateUIforHFR mHfr="+ mHfr+" zoom supported=" +zoom);
            int zoomVisibility;
            if (mHfr ) {
            //    mIndicatorControlContainer.enableItems(
            //        CameraSettings.KEY_AUDIO_ENCODER, "false");

                zoomVisibility = View.INVISIBLE;
                zoom = false;
            } else {

            //    mIndicatorControlContainer.enableItems(
            //        CameraSettings.KEY_AUDIO_ENCODER, "true");
                if (zoom) {
                    zoomVisibility = View.VISIBLE;
                } else {
                    zoomVisibility = View.INVISIBLE;
                }
            }

            if (mZoomControl != null) {
                // BEGIN: Modified by zhanghongxing at 2013-05-02
                /**if (isFrontCamera()) {
                    mZoomControl.setVisibility(View.GONE);
                } else {
                    mZoomControl.setVisibility(zoomVisibility);
                }*/ //delete by xiongzhu for ARD-100
                // END:   Modified by zhanghongxing at 2013-05-02
//               mZoomControl.setVisibility(zoomVisibility);
               mZoomControl.setEnabled(zoom);
            }
        }
    }
	
    private void switchToOtherMode(int mode) {
        if (isFinishing()) return;
        if (mThumbnail != null) ThumbnailHolder.keep(mThumbnail);
        MenuHelper.gotoMode(mode, this);
        finish();
    }

    @Override
    public void onModeChanged(int mode) {
        //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
        // if (mode != ModePicker.MODE_VIDEO) switchToOtherMode(mode);
        //END:   Deleted by zhanghongxing at 2013-01-09
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_EFFECT_BACKDROPPER:
                if (resultCode == RESULT_OK) {
                    // onActivityResult() runs before onResume(), so this parameter will be
                    // seen by startPreview from onResume()
                    mEffectUriFromGallery = data.getData().toString();
                    Log.v(TAG, "Received URI from gallery: " + mEffectUriFromGallery);
                    mResetEffect = false;
                } else {
                    mEffectUriFromGallery = null;
                    Log.w(TAG, "No URI from gallery");
                    mResetEffect = true;
                }
                break;
                
            case RESTOREGPS_STATE:
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

    @Override
    public void onEffectsUpdate(int effectId, int effectMsg) {
        Log.v(TAG, "onEffectsUpdate. Effect Message = " + effectMsg);
        if (effectMsg == EffectsRecorder.EFFECT_MSG_EFFECTS_STOPPED) {
            // Effects have shut down. Hide learning message if any,
            // and restart regular preview.
            mBgLearningMessageFrame.setVisibility(View.GONE);
            checkQualityAndStartPreview();
        } else if (effectMsg == EffectsRecorder.EFFECT_MSG_RECORDING_DONE) {
            // This follows the codepath from onStopVideoRecording.
            if (mEffectsDisplayResult && !addVideoToMediaStore()) {
                if (mIsVideoCaptureIntent) {
                    if (mQuickCapture) {
                        doReturnToCaller(true);
                    } else {
                        showAlert();
                    }
                } else {
                    getThumbnail();
                }
            }
            mEffectsDisplayResult = false;
            // In onPause, these were not called if the effects were active. We
            // had to wait till the effects recording is complete to do this.
            if (mPaused) {
                closeVideoFileDescriptor();
                clearVideoNamer();
            }
        } else if (effectMsg == EffectsRecorder.EFFECT_MSG_PREVIEW_RUNNING) {
            // Enable the shutter button once the preview is complete.
            mShutterButton.setEnabled(true);
        } else if (effectId == EffectsRecorder.EFFECT_BACKDROPPER) {
            switch (effectMsg) {
                case EffectsRecorder.EFFECT_MSG_STARTED_LEARNING:
                    mBgLearningMessageFrame.setVisibility(View.VISIBLE);
                    break;
                case EffectsRecorder.EFFECT_MSG_DONE_LEARNING:
                case EffectsRecorder.EFFECT_MSG_SWITCHING_EFFECT:
                    mBgLearningMessageFrame.setVisibility(View.GONE);
                    break;
            }
        }
        // In onPause, this was not called if the effects were active. We had to
        // wait till the effects completed to do this.
        if (mPaused) {
            Log.v(TAG, "OnEffectsUpdate: closing effects if activity paused");
            closeEffects();
        }
    }

    public void onCancelBgTraining(View v) {
        // Remove training message
        mBgLearningMessageFrame.setVisibility(View.GONE);
        // Write default effect out to shared prefs
        writeDefaultEffectToPrefs();
        // Tell the indicator controller to redraw based on new shared pref values
        mSecondLevelIndicatorControlBar.reloadPreferences();
        // Tell VideoCamer to re-init based on new shared pref values.
        onSharedPreferenceChanged();
    }

    @Override
    public synchronized void onEffectsError(Exception exception, String fileName) {
        // TODO: Eventually we may want to show the user an error dialog, and then restart the
        // camera and encoder gracefully. For now, we just delete the file and bail out.
        if (fileName != null && new File(fileName).exists()) {
            deleteVideoFile(fileName);
            if (mPaused) {
                closeVideoFileDescriptor();
                clearVideoNamer();
                closeEffects();
            }
        }
        if (exception instanceof MediaRecorderStopException) {
            Log.w(TAG, "Problem recoding video file. Removing incomplete file.");
            return;
        }
        throw new RuntimeException("Error during recording!", exception);
    }

    private void initializeControlByIntent() {
        if (mIsVideoCaptureIntent) {
            // Cannot use RotateImageView for "done" and "cancel" button because
            // the tablet layout uses RotateLayout, which cannot be cast to
            // RotateImageView.
            mReviewDoneButton = (Rotatable) findViewById(R.id.btn_done);
            mReviewCancelButton = (Rotatable) findViewById(R.id.btn_cancel);
            mReviewPlayButton = (RotateImageView) findViewById(R.id.btn_play);
            mReviewRetakeButton = findViewById(R.id.btn_retake);
            findViewById(R.id.btn_cancel).setVisibility(View.VISIBLE);

            // Not grayed out upon disabled, to make the follow-up fade-out
            // effect look smooth. Note that the review done button in tablet
            // layout is not a TwoStateImageView.
            if (mReviewDoneButton instanceof TwoStateImageView) {
                ((TwoStateImageView) mReviewDoneButton).enableFilter(false);
            }
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
            RelativeLayout modeSwitcher = (RelativeLayout) findViewById(R.id.mode_switcher);
            modeSwitcher.setVisibility(View.VISIBLE);
            mSwitchToCamera = (RotateImageView) findViewById(R.id.mode_camera);
            mSwitchToCamera.enableFilter(false);
            mSwitchToVideo = (RotateImageView) findViewById(R.id.mode_video);
            mSwitchToVideo.setImageResource(R.drawable.ic_switch_video_focused);
            mSwitchToVideo.enableFilter(false);
            mSwitchToVideo.setEnabled(false);
        }
    }

    private void initializeMiscControls() {
        mPreviewFrameLayout = (PreviewFrameLayout) findViewById(R.id.frame);
        mPreviewFrameLayout.addOnLayoutChangeListener(this);
        mReviewImage = (ImageView) findViewById(R.id.review_image);

        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        
        //START: modified by xiongzhu for cbb
//        mShutterButton.setBackgroundResource(R.drawable.shutterbutton_background);
        mShutterButton.setImageResource(R.drawable.btn_shutter_video);
        mShutterButton.enableFilter(false);
        //END: modified by xiongzhu for cbb
       
        
        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.requestFocus();

        // Disable the shutter button if effects are ON since it might take
        // a little more time for the effects preview to be ready. We do not
        // want to allow recording before that happens. The shutter button
        // will be enabled when we get the message from effectsrecorder that
        // the preview is running. This becomes critical when the camera is
        // swapped.
//        if (effectsActive()) {
            mShutterButton.setEnabled(false);
//        }

        mRecordingTimeView = (TextView) findViewById(R.id.recording_time);
        mRecordingTimeRect = (RotateLayout) findViewById(R.id.recording_time_rect);
        mTimeLapseLabel = findViewById(R.id.time_lapse_label);
        // The R.id.labels can only be found in phone layout.
        // That is, mLabelsLinearLayout should be null in tablet layout.
        mLabelsLinearLayout = (LinearLayout) findViewById(R.id.labels);

        mBgLearningMessageRotater = (RotateLayout) findViewById(R.id.bg_replace_message);
        mBgLearningMessageFrame = findViewById(R.id.bg_replace_message_frame);
        mCaptureAnimView = (ImageView) findViewById(R.id.capture_anim_view);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setDisplayOrientation();

        // Change layout in response to configuration change
        LayoutInflater inflater = getLayoutInflater();
        LinearLayout appRoot = (LinearLayout) findViewById(R.id.camera_app_root);
        appRoot.setOrientation(
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        appRoot.removeAllViews();
        inflater.inflate(R.layout.preview_frame_video, appRoot);
        //BEGIN: Deleted by zhanghongxing at 2013-01-09 for full preview
        // inflater.inflate(R.layout.camera_control, appRoot);
        //END:   Deleted by zhanghongxing at 2013-01-09

        // from onCreate()
        //BEGIN: Deleted by zhanghongxing at 2013-01-11 for full preview
        // resizeForPreviewAspectRatio();
        //END:   Deleted by zhanghongxing at 2013-01-11
        initializeControlByIntent();
        mSwitchToCamera.setEnabled(true);
//        mSwitchToVideo.setEnabled(true);
        initializeMiscControls();
        showTimeLapseUI(mCaptureTimeLapse);
        initializeVideoSnapshot();
        //resizeForPreviewAspectRatio();
        initializeIndicatorControl();

        // from onResume()
        showVideoSnapshotUI(false);
        initializeZoom();
        if (!mIsVideoCaptureIntent) {
            updateThumbnailView();
            mSwitchToVideo.setImageResource(R.drawable.ic_switch_video_focused);
        }
        //BEGIN: Added by zhanghongxing at 2013-01-11 for full preview
        resizeForPreviewAspectRatio();
        //END:   Added by zhanghongxing at 2013-01-11
        
        boolean recordLocation = RecordLocationPreference.get(
                mPreferences, mContentResolver);
        mIsRecordLocation = recordLocation;
        if (recordLocation) {
            showGpsOnScreenIndicator(LocationManager.hasGpsSignal);
        }
        
        mLastOrientation = newConfig.orientation;
    }

    @Override
    public void onOverriddenPreferencesClicked() {
    }

    @Override
    public void onRestorePreferencesClicked() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
            	bFlagDisanableToUpdateUI = true;
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

         //START: modified by xiongzhu for ARD-100
        //if (mParameters.isZoomSupported()) {
          if (mIsZoomSupported) {
         //END : modified by xiongzhu for ARD-100

            mZoomValue = 0;
            setCameraParameters();
            mZoomControl.setZoomIndex(0);
        }

        if (mSecondLevelIndicatorControlBar != null) {
        	mSecondLevelIndicatorControlBar.dismissSettingPopup();
            CameraSettings.restorePreferences(this, mPreferences,
                    mParameters);
            if(mIsVideoCaptureIntent) {
            	Editor editor = mPreferences.edit();
            	editor.putString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,
            			getResources().getString(R.string.pref_videocamera_recording_mode_value_mms));
            	editor.commit();
            }
            mSecondLevelIndicatorControlBar.reloadPreferences();
            oldHFR = "";
            bFlagHFRUpdateUI = false;
            onSharedPreferenceChanged();
        }
    }

    private boolean effectsActive() {
        return (mEffectType != EffectsRecorder.EFFECT_NONE);
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
            		Util.showLocationAlert(VideoCamera.this);
            	}
        }
        mIsRecordLocation = recordLocation;
        mLocationManager.recordLocation(recordLocation);
   }
    
    @Override
    public void onSharedPreferenceChanged() {
        // ignore the events after "onPause()" or preview has not started yet
        if (mPaused) return;
        synchronized (mPreferences) {
            // If mCameraDevice is not ready then we can set the parameter in
            // startPreview().
            if (mCameraDevice == null) return;

            qureyAndSetGpsState();

            String storagePlace=mPreferences.getString(CameraSettings.KEY_CAMERA_STORAGE_PLACE,getString(R.string.pref_storage_place_default));
		    Storage.mIsExternalStorage=storagePlace.equals( CameraSettings.CAMERA_STORAGE_SDCARD ) ? true:false;
		    if( !mStoragePlace.equals( storagePlace )){  
		    	mStoragePlace=storagePlace;
			    mChangingStoragePlace=true;
			    CameraHolder.instance().keep(); 
			    closeCamera();
                if (mSurfaceTexture != null) {
                    mCameraScreenNail.releaseSurfaceTexture();
                    mSurfaceTexture = null;
                }
               updateCameraScreenNail(!mIsVideoCaptureIntent,Storage.mIsExternalStorage);

            changeStoragePlace();
            installIntentFilter();
            //BEGIN: Modified by zhanghongxing at 2013-05-28
            if (!mIsVideoCaptureIntent) {
                getLastThumbnail();
            }
            //END:   Modified by zhanghongxing at 2013-05-28
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateStorageHint(mStorageSpace, mOrientationCompensation);
                }
            }, 200);
           // return;
           }
           //END:   Added by xiongzhu at 2013-04-15
 
		    String recordingMode = mPreferences.getString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,
		    		getString(R.string.pref_videocamera_recording_mode_value_normal));
		    mCurVideoRecordingMode = recordingMode;
		    Editor editor = mPreferences.edit();
		    if(recordingMode.equals(getString(R.string.pref_videocamera_recording_mode_value_normal))) {
		    	editor.putString(CameraSettings.KEY_VIDEO_DURATION, "0");
		    } else {
		    	editor.putString(CameraSettings.KEY_VIDEO_DURATION, "-1");
		    }
		    editor.apply();
            // Check if the current effects selection has changed
            if (updateEffectSelection()) return;

            readVideoPreferences();
            showTimeLapseUI(mCaptureTimeLapse);
            // We need to restart the preview if preview size is changed.
            Size size = mParameters.getPreviewSize();
            if (size.width != mDesiredPreviewWidth
                    || size.height != mDesiredPreviewHeight || mRestartPreview) {
                if (!effectsActive()) {
                    stopPreview();
                } else {
                    mEffectsRecorder.release();
                    mEffectsRecorder = null;
                }
                resizeForPreviewAspectRatio();
                startPreview(); // Parameters will be set in startPreview().
                mIsVideoQualityChanged = true;
            }else if(mVideoSnapSizeChanged){
                //Restart Preview for Full size Live shot picture dimension change
                if (!effectsActive()) {
                    mCameraDevice.stopPreview();
                } else {
                    mEffectsRecorder.release();
                }
                startPreview();
                mVideoSnapSizeChanged = false;
                mRestartPreview = false;
            } else {
                setCameraParameters();
            }
        }
        reloadWhenSmsMode();
        updateCameraUI( );
        updateUIforHFR( );
    }

    @Override
    public void onCameraPickerClicked(int cameraId) {
        if (mPaused || mPendingSwitchCameraId != -1) return;
        if (mSecondLevelIndicatorControlBar.getActiveSettingPopup() != null) mSecondLevelIndicatorControlBar.dismissSettingPopup();
        Log.d(TAG, "Start to copy texture.");
        // We need to keep a preview frame for the animation before
        // releasing the camera. This will trigger onPreviewTextureCopied.
        mCameraScreenNail.copyTexture();
        mPendingSwitchCameraId = cameraId;
        // Disable all camera controls.
        mSwitchingCamera = true;
        //Disable the switch picker, until switch completely.
    }

    //BEGIN: Added by xiongzhu at 2013-04-15
    private void changeStoragePlace(){
        if (mPaused) return;
	
        //closeCamera(); modified by xiongzhu at 2013-7-23
        // Restart the camera and initialize the UI. From onCreate.
        CameraOpenThread cameraOpenThread = new CameraOpenThread();
        cameraOpenThread.start();
        try {
            cameraOpenThread.join();
        } catch (InterruptedException ex) {
            // ignore
        }
        startPreview();
        setPreviewFrameLayoutOrientation();
        resizeForPreviewAspectRatio();
  
        mCameraScreenNail.changeStoragePlace();     
        mChangingStoragePlace = false;
    }
    //END:   Added by xiongzhu at 2013-04-15

    private void switchCamera() {
        if (mPaused) return;

        Log.d(TAG, "Start to switch camera.");
        mCameraId = mPendingSwitchCameraId;
        mPendingSwitchCameraId = -1;

        bFlagDisanableToUpdateUI = true;
        closeCamera();

        // Restart the camera and initialize the UI. From onCreate.
        mPreferences.setLocalId(this, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
        CameraOpenThread cameraOpenThread = new CameraOpenThread();
        cameraOpenThread.start();
        try {
            cameraOpenThread.join();
        } catch (InterruptedException ex) {
            // ignore
        }
        if(mCameraDevice == null) return;
        loadCameraPreferences(); //Added by zhanghongxing at 2013-04-10
        mCurVideoRecordingMode = mPreferences.getString(CameraSettings.KEY_VIDEOCAMERA_RECORDING_MODE,
                getString(R.string.pref_videocamera_recording_mode_default));
        readVideoPreferences();
        oldHFR = mPreferences.getString(
                CameraSettings.KEY_VIDEO_HIGH_FRAME_RATE,
                getString(R.string.pref_camera_hfr_default));
        initializeVideoSnapshot();
        setPreviewFrameLayoutOrientation();
        resizeForPreviewAspectRatio();
        initializeIndicatorControl();
        mSecondLevelIndicatorControlBar.setEnabled(false);
        startPreview();
        bFlagDisanableToUpdateUI = false;
        // From onResume
        initializeZoom();
        resetZoom(); // Added by zhanghongxing at 2013-05-02
        setOrientationIndicator(mOrientationCompensation, false);

        // Start switch camera animation. Post a message because
        // onFrameAvailable from the old camera may already exist.
        mHandler.sendEmptyMessage(SWITCH_CAMERA_START_ANIMATION);
        updateUIforHFR();
        reloadWhenSmsMode();
    }

    // BEGIN: Added by zhanghongxing at 2013-05-02
    public void resetZoom() {
        // Reset zoom.
        
         //START: modified by xiongzhu for ARD-100
        //if (mParameters.isZoomSupported()) {
          if (mIsZoomSupported) {
         //END : modified by xiongzhu for ARD-100
            mZoomValue = 0;
            setCameraParameters();
            mZoomControl.setZoomIndex(0);
        }
    }
    // END:   Added by zhanghongxing at 2013-05-02

    // Preview texture has been copied. Now camera can be released and the
    // animation can be started.
    @Override
    protected void onPreviewTextureCopied() {
        mHandler.sendEmptyMessage(SWITCH_CAMERA);
    }

    private boolean updateEffectSelection() {
        int previousEffectType = mEffectType;
        Object previousEffectParameter = mEffectParameter;
        mEffectType = CameraSettings.readEffectType(mPreferences);
        mEffectParameter = CameraSettings.readEffectParameter(mPreferences);

        if (mEffectType == previousEffectType) {
            if (mEffectType == EffectsRecorder.EFFECT_NONE) return false;
            if (mEffectParameter.equals(previousEffectParameter)) return false;
        }
        Log.v(TAG, "New effect selection: " + mPreferences.getString(
                CameraSettings.KEY_VIDEO_EFFECT, "none"));

        if (mEffectType == EffectsRecorder.EFFECT_NONE) {
            // Stop effects and return to normal preview
            mEffectsRecorder.stopPreview();
            mPreviewing = false;
            return true;
        }
        if (mEffectType == EffectsRecorder.EFFECT_BACKDROPPER &&
            ((String) mEffectParameter).equals(EFFECT_BG_FROM_GALLERY)) {
            // Request video from gallery to use for background
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setDataAndType(Video.Media.EXTERNAL_CONTENT_URI,
                             "video/*");
            i.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(i, REQUEST_EFFECT_BACKDROPPER);
            return true;
        }
        if (previousEffectType == EffectsRecorder.EFFECT_NONE) {
            // Stop regular preview and start effects.
            stopPreview();
            checkQualityAndStartPreview();
        } else {
            // Switch currently running effect
            mEffectsRecorder.setEffect(mEffectType, mEffectParameter);
        }
        return true;
    }

    // Verifies that the current preview view size is correct before starting
    // preview. If not, resets the surface texture and resizes the view.
    private void checkQualityAndStartPreview() {
        readVideoPreferences();
        showTimeLapseUI(mCaptureTimeLapse);
        Size size = mParameters.getPreviewSize();
        if (size.width != mDesiredPreviewWidth
                || size.height != mDesiredPreviewHeight) {
            resizeForPreviewAspectRatio();
        }
        // Start up preview again
        startPreview();
        updateUIforHFR();
    }

    private void showTimeLapseUI(boolean enable) {
        if (mTimeLapseLabel != null) {
            mTimeLapseLabel.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // if (mSwitchingCamera) return true;
        if (mSwitchingCamera||mChangingStoragePlace) return true;
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
            // Check if the popup window is visible.
            View popup = mSecondLevelIndicatorControlBar.getActiveSettingPopup();
            if (popup != null) {
                // Let popup window, indicator control or preview frame handle the
                // event by themselves. Dismiss the popup window if users touch on
                // other areas.
//                if (!Util.pointInView(x, y, popup)
//                        && !Util.pointInView(x, y, mSecondLevelIndicatorControlBar)) {
//                	mSecondLevelIndicatorControlBar.dismissSettingPopup();
//                }
                
            	View subPopup = mSecondLevelIndicatorControlBar.getActiveSubSettingPopup();
            	if(!Util.pointInView(x, y, mSecondLevelIndicatorControlBar)) {
            		if(subPopup != null) {
            			if(!Util.pointInView(x, y, subPopup)) {
            				mSecondLevelIndicatorControlBar.dismissSettingPopup();
            				return true;
            			}
            		} else if(popup.getVisibility() == View.VISIBLE){
                        if(!Util.pointInView(x, y, popup) && !mRotateDialog.isRotateDialogVisible()) {
                        	mSecondLevelIndicatorControlBar.dismissSettingPopup();
                        	return true;
            			}
            		}
        		}
            }
        }

        if(mAppBridge.isCurrentCameraPreview()) {
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

    private class ZoomChangeListener implements ZoomControl.OnZoomChangedListener {
        @Override
        public void onZoomValueChanged(int index) {
            // Not useful to change zoom value when the activity is paused.
            if (mPaused) return;

            mZoomValue = index;

            // Set zoom parameters asynchronously
            mParameters.setZoom(mZoomValue);
            mCameraDevice.setParametersAsync(mParameters);
        }
    }

    private void initializeZoom() {
        mZoomControl = (ZoomControl) findViewById(R.id.zoom_control);

        //START : modified by xiongzhu for ARD-100
        //if (!mParameters.isZoomSupported()) return;
        if (!mIsZoomSupported) return;

        mZoomMax = mParameters.getMaxZoom();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        mZoomControl.setZoomMax(mZoomMax);
        mZoomControl.setZoomIndex(mParameters.getZoom());
        mZoomControl.setOnZoomChangeListener(new ZoomChangeListener());
        mZoomControl.setVisibility(View.INVISIBLE);
        // BEGIN: Added by zhanghongxing at 2013-05-02
        /**if (isFrontCamera()) {
            mZoomControl.setVisibility(View.GONE);
        } else {
            mZoomControl.setVisibility(View.VISIBLE);
        }*///delete by xiongzhu for ARD-100
        // END:   Added by zhanghongxing at 2013-05-02
    }

    private void initializeVideoSnapshot() {
        if (mParameters.isVideoSnapshotSupported() && !mIsVideoCaptureIntent) {
//            setSingleTapUpListener(mPreviewFrameLayout);
            // Show the tap to focus toast if this is the first start.
//            if (mPreferences.getBoolean(
//                        CameraSettings.KEY_VIDEO_FIRST_USE_HINT_SHOWN, true)) {
                // Delay the toast for one second to wait for orientation.
//                mHandler.sendEmptyMessageDelayed(SHOW_TAP_TO_SNAPSHOT_TOAST, 1000);
//            }
        } else {
            setSingleTapUpListener(null);
        }
    }

    void showVideoSnapshotUI(boolean enabled) {
        if (mParameters.isVideoSnapshotSupported() && !mIsVideoCaptureIntent) {
            if (enabled) {
                mPreviewFrameLayout.setBackgroundColor(Color.TRANSPARENT);
            }
            mPreviewFrameLayout.showBorder(enabled);
            mIndicatorControlContainer.enableZoom(!enabled);
            mShutterButton.setEnabled(!enabled);
        }
    }

    private void takeVideoSnapshot() {
        // Set rotation and gps data.
        int rotation = Util.getJpegRotation(mCameraId, mOrientation);
        mParameters.setRotation(rotation);
        Location loc = null;
        if (mIsRecordLocation) {
        	loc = mLocationManager.getCurrentLocation();
        }
        Util.setGpsParameters(mParameters, loc);
        mCameraDevice.setParameters(mParameters);

        Log.v(TAG, "Video snapshot start");
        mCameraDevice.takePicture(null, null, null, new JpegPictureCallback(loc));
        showVideoSnapshotUI(true);
        mSnapshotInProgress = true;
    }

    // Preview area is touched. Take a picture.
    @Override
    protected void onSingleTapUp(View view, int x, int y) {
          //removed for lava
//        if (mMediaRecorderRecording && effectsActive()) {
//            new RotateTextToast(this, R.string.disable_video_snapshot_hint,
//                    mOrientation).show();
//            return;
//        }
//
//        if (mPaused || mSnapshotInProgress
//                || !mMediaRecorderRecording || effectsActive()) {
//            return;
//        }
//        takeVideoSnapshot();
//        return;
    }

    @Override
    protected void updateCameraAppView() {
        super.updateCameraAppView();
        // Reset mSurfaceTexture
        if (effectsActive()) {
            mEffectsRecorder.setPreviewSurfaceTexture(mSurfaceTexture, mSurfaceWidth,
                    mSurfaceHeight);
        }
        if (!mPreviewing || mParameters.getFlashMode() == null) return;

        // When going to and back from gallery, we need to turn off/on the flash.
        if (!mShowCameraAppView) {
            if (mParameters.getFlashMode().equals(Parameters.FLASH_MODE_OFF)) {
                mRestoreFlash = false;
                return;
            }
            mRestoreFlash = true;
            setCameraParameters();
        } else if (mRestoreFlash) {
            mRestoreFlash = false;
            setCameraParameters();
        }
    }

    private final class JpegPictureCallback implements PictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
        }

        @Override
        public void onPictureTaken(byte [] jpegData, android.hardware.Camera camera) {
            Log.v(TAG, "onPictureTaken");
            //BEGIN: Modified by zhanghongxing at 2013-01-05 for DER-210
            // mSnapshotInProgress = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	mSnapshotInProgress = false;
                }
            }, 200);
            //END:   Modified by zhanghongxing at 2013-01-05
            showVideoSnapshotUI(false);
            storeImage(jpegData, mLocation);
        }
    }

    private void storeImage(final byte[] data, Location loc) {
        long dateTaken = System.currentTimeMillis();
        String title = Util.createJpegName(dateTaken,getApplicationContext());
        int orientation = Exif.getOrientation(data);
        Size s = mParameters.getPictureSize();
        Uri uri = Storage.addImage(mContentResolver, title, dateTaken, loc, orientation, data,
                s.width, s.height);
        if (uri != null) {
            // Create a thumbnail whose width is equal or bigger than that of the preview.
            int ratio = (int) Math.ceil((double) mParameters.getPictureSize().width
                    / mPreviewFrameLayout.getWidth());
            int inSampleSize = Integer.highestOneBit(ratio);
            mThumbnail = Thumbnail.createThumbnail(data, orientation, inSampleSize, uri);
            if ((mThumbnail != null) && (mThumbnailView != null)) {
                mThumbnailView.setBitmap(mThumbnail.getBitmap());
            }
            Util.broadcastNewPicture(this, uri);
        }
    }

    private boolean resetEffect() {
        if (mResetEffect) {
            String value = mPreferences.getString(CameraSettings.KEY_VIDEO_EFFECT,
                    mPrefVideoEffectDefault);
            if (!mPrefVideoEffectDefault.equals(value)) {
                writeDefaultEffectToPrefs();
                return true;
            }
        }
        mResetEffect = true;
        return false;
    }

    private String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    private String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return ".mp4";
        }
        return ".3gp";
    }

    private void closeVideoFileDescriptor() {
        if (mVideoFileDescriptor != null) {
            try {
                mVideoFileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Fail to close fd", e);
            }
            mVideoFileDescriptor = null;
        }
    }

    private void showTapToSnapshotToast() {
        new RotateTextToast(this, R.string.video_snapshot_hint, mOrientationCompensation)
                .show();
        // Clear the preference.
        Editor editor = mPreferences.edit();
        editor.putBoolean(CameraSettings.KEY_VIDEO_FIRST_USE_HINT_SHOWN, false);
        editor.apply();
    }

    private void showUserMsg(int msgId) {
        if (msgId == SHOW_VIDEOSNAP_CAPPING_MSG) {
            new RotateTextToast(this, R.string.snapshot_lower_than_video,
                                mOrientation).show();
        }else if(msgId == SHOW_LOWPOWER_MODE) {
            mLowPowerToast = new RotateTextToast(this, R.string.snapshotsize_low_powermode_1,
                            mOrientation);
            mLowPowerToast.show();
            // Too many words to show, delay to update another part.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mLowPowerToast != null) {
                        mLowPowerToast.setText(R.string.snapshotsize_low_powermode_2, mOrientation);
                        mLowPowerToast = null;
                    }
                }
            }, RotateTextToast.TOAST_DURATION / 2);
        }
    }

    private void clearVideoNamer() {
        if (mVideoNamer != null) {
            mVideoNamer.finish();
            mVideoNamer = null;
        }
    }

    private static class VideoNamer extends Thread {
        private boolean mRequestPending;
        private ContentResolver mResolver;
        private ContentValues mValues;
        private boolean mStop;
        private Uri mUri;

        // Runs in main thread
        public VideoNamer() {
            start();
        }

        // Runs in main thread
        public synchronized void prepareUri(
                ContentResolver resolver, ContentValues values) {
            mRequestPending = true;
            mResolver = resolver;
            mValues = new ContentValues(values);
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
            Uri uri = mUri;
            mUri = null;
            return uri;
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
            try {
                Uri videoTable = Uri.parse("content://media/external/video/media");
                mUri = mResolver.insert(videoTable, mValues);
            } catch (Exception e) {
                // We failed to generate Uri. This can happen if
                // the SD card is unmounted.
                Log.e(TAG, "failed to generate uri", e);
            }
        }

        // Runs in namer thread
        private void cleanOldUri() {
            if (mUri == null) return;
            mResolver.delete(mUri, null, null);
            mUri = null;
        }
    }
    
    public void enterLowPowerMode() {
    	if(mCameraDevice == null) return;
    	mIsLowPowerMode = true;
    	IconListPreference flashPref = (IconListPreference)mPreferenceGroup.findPreference(
    			CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE);
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
    			CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE);
    	if(flashPref.isAvailable()) return;
    	flashPref.setAvailable(true);
    	
    	String flashMode = mPreferences.getString(
                CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE,
                getString(R.string.pref_camera_video_flashmode_default));
    	mParameters = mCameraDevice.getParameters();
        mParameters.setFlashMode(flashMode);
        mCameraDevice.setParameters(mParameters);
    }

    private void reloadWhenSmsMode() {
        boolean isSmsMode = mCurVideoRecordingMode
				.equals(getString(R.string.pref_videocamera_recording_mode_value_mms));
		if (mPreferenceGroup != null) {
			ListPreference videoQualityPref = mPreferenceGroup
					.findPreference(CameraSettings.KEY_VIDEO_QUALITY);
			if(videoQualityPref == null) return;
			if (isSmsMode) {
				videoQualityPref.setEnable(false);
			} else {
				videoQualityPref.setEnable(true);
			}
			if (mSecondLevelIndicatorControlBar != null) {
				mSecondLevelIndicatorControlBar.reloadPreferences();
			}
		}
    }
}
