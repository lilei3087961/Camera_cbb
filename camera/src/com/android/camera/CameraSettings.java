/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.SystemProperties;
import android.util.FloatMath;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import android.app.ActivityManager;
import android.graphics.ImageFormat;

/**
 *  Provides utilities and keys for Camera settings.
 */
public class CameraSettings {
    private static final int NOT_FOUND = -1;

    public static final String KEY_VERSION = "pref_version_key";
    public static final String KEY_LOCAL_VERSION = "pref_local_version_key";
    public static final String KEY_RECORD_LOCATION = "pref_camera_recordlocation_key";
    public static final String KEY_VIDEO_QUALITY = "pref_video_quality_key";
    public static final String KEY_FRONT_VIDEO_QUALITY = "pref_front_video_quality_key";
    public static final String KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL = "pref_video_time_lapse_frame_interval_key";
    public static final String KEY_PICTURE_SIZE = "pref_camera_picturesize_key";
    public static final String KEY_FRONT_PICTURE_SIZE = "pref_front_camera_picturesize_key";
    public static final String KEY_JPEG_QUALITY = "pref_camera_jpegquality_key";
    public static final String KEY_FOCUS_MODE = "pref_camera_focusmode_key";
    public static final String KEY_FLASH_MODE = "pref_camera_flashmode_key";
    public static final String KEY_VIDEOCAMERA_FLASH_MODE = "pref_camera_video_flashmode_key";
    public static final String KEY_WHITE_BALANCE = "pref_camera_whitebalance_key";
    public static final String KEY_SCENE_MODE = "pref_camera_scenemode_key";
    public static final String KEY_EXPOSURE = "pref_camera_exposure_key";
    public static final String KEY_VIDEO_EFFECT = "pref_video_effect_key";
    public static final String KEY_CAMERA_ID = "pref_camera_id_key";
    public static final String KEY_CAMERA_FIRST_USE_HINT_SHOWN = "pref_camera_first_use_hint_shown_key";
    public static final String KEY_VIDEO_FIRST_USE_HINT_SHOWN = "pref_video_first_use_hint_shown_key";

    public static final String KEY_POWER_MODE = "pref_camera_powermode_key";
    public static final String KEY_PICTURE_FORMAT = "pref_camera_pictureformat_key";
    public static final String KEY_ZSL = "pref_camera_zsl_key";
    public static final String KEY_COLOR_EFFECT = "pref_camera_coloreffect_key";
    public static final String KEY_FACE_DETECTION = "pref_camera_facedetection_key";
    public static final String KEY_TOUCH_AF_AEC = "pref_camera_touchafaec_key";
    public static final String KEY_SELECTABLE_ZONE_AF = "pref_camera_selectablezoneaf_key";
    public static final String KEY_SATURATION = "pref_camera_saturation_key";
    public static final String KEY_CONTRAST = "pref_camera_contrast_key";
    public static final String KEY_SHARPNESS = "pref_camera_sharpness_key";
    public static final String KEY_AUTOEXPOSURE = "pref_camera_autoexposure_key";
    public static final String KEY_ANTIBANDING = "pref_camera_antibanding_key";
    public static final String KEY_ISO = "pref_camera_iso_key";
    public static final String KEY_LENSSHADING = "pref_camera_lensshading_key";
    public static final String KEY_HISTOGRAM = "pref_camera_histogram_key";
    public static final String KEY_DENOISE = "pref_camera_denoise_key";
    public static final String KEY_REDEYE_REDUCTION = "pref_camera_redeyereduction_key";
    public static final String KEY_AE_BRACKET_HDR = "pref_camera_ae_bracket_hdr_key";
    
    public static final String KEY_VIDEO_SNAPSHOT_SIZE = "pref_camera_videosnapsize_key";
    public static final String KEY_VIDEO_HIGH_FRAME_RATE = "pref_camera_hfr_key";

    public static final String DEFAULT_VIDEO_QUALITY_VALUE = "custom";
    public static final String KEY_VIDEO_ENCODER = "pref_camera_videoencoder_key";
    public static final String KEY_AUDIO_ENCODER = "pref_camera_audioencoder_key";
    public static final String KEY_VIDEO_DURATION = "pref_camera_video_duration_key";
    public static final String KEY_SKIN_TONE_ENHANCEMENT = "pref_camera_skinToneEnhancement_key";
    public static final String KEY_SKIN_TONE_ENHANCEMENT_FACTOR = "pref_camera_skinToneEnhancement_factor_key";
    public static final String KEY_CAMERA_INTELLIGENCE_KEY = "pref_camera_intelligence_key";

    private static final String VIDEO_QUALITY_HIGH = "high";
    private static final String VIDEO_QUALITY_MMS = "mms";
    private static final String VIDEO_QUALITY_YOUTUBE = "youtube";

    public static final String EXPOSURE_DEFAULT_VALUE = "0";

    public static final int CURRENT_VERSION = 5;
    public static final int CURRENT_LOCAL_VERSION = 2;

    public static final int DEFAULT_VIDEO_DURATION = 0; // no limit

    private static final int MMS_VIDEO_DURATION = (CamcorderProfile.get(CamcorderProfile.QUALITY_LOW) != null) ?
                                                     CamcorderProfile.get(CamcorderProfile.QUALITY_LOW).duration :
                                                     30;
    private static final int YOUTUBE_VIDEO_DURATION = 15 * 60; // 15 mins

    private static final String TAG = "CameraSettings";

    // Formats for setPreviewFormat and setPictureFormat.
    private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
    private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
    private static final String PIXEL_FORMAT_YUV420SP_ADRENO = "yuv420sp-adreno";
    private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
    private static final String PIXEL_FORMAT_YUV420P = "yuv420p";
    private static final String PIXEL_FORMAT_RGB565 = "rgb565";
    private static final String PIXEL_FORMAT_JPEG = "jpeg";
    private static final String PIXEL_FORMAT_BAYER_RGGB = "bayer-rggb";
    private static final String PIXEL_FORMAT_RAW = "raw";
    private static final String PIXEL_FORMAT_YV12 = "yv12";
    private static final String PIXEL_FORMAT_NV12 = "nv12";

    private final Context mContext;
    private static Parameters mParameters;
    private final CameraInfo[] mCameraInfo;
    private static int mCameraId;

    //Add by wangbin at 2013-6-5
    private boolean resetPictureSize;
    //Add by wangbin at 2013-03-22
    public static final String KEY_SILENT_MODE = "pref_silentmode_key";
    public static final String SILENT_MODE_OFF = "off";
    public static final String SILENT_MODE_ON = "on";
    
    public static final String KEY_TIMER_SNAP = "pref_timersnap_key";
    
    public static final String KEY_VOLUME_KEY_MODE = "pref_volume_key";
    public static final String VOLUME_KEY_VALUE_NONE = "none";
    public static final String VOLUME_KEY_VALUE_SNAP = "snap";
    public static final String VOLUME_KEY_VALUE_ZOOM = "zoom";

    //Added by zhanghongxing at 2013-01-06 for DER-173
    public static final String KEY_VIDEO_WHITE_BALANCE = "pref_video_whitebalance_key";
    public static final String KEY_VIDEO_COLOR_EFFECT = "pref_video_coloreffect_key";
    public static final String KEY_VIDEO_RECORD_LOCATION = RecordLocationPreference.VIDEO_KEY;
    
    //Added by xiongzhu at 2013-04-15
    public static final String KEY_CAMERA_STORAGE_PLACE="pref_camera_storage_place_key";
    public static final String CAMERA_STORAGE_SDCARD = "sdcard";
    public static final String CAMERA_STORAGE_MEMORY = "memory";
    
    public static final String KEY_CAMERA_TAKEPICTURE_MODE = "pref_camera_takepicture_mode_key";
    public static final String KEY_VIDEOCAMERA_RECORDING_MODE ="pref_videocamera_recording_mode_key";
    
    public static final String KEY_VERSION_NUMBER = "pref_versionnumber_key";
    
    public static final String PRODUCT_SYSTEM_VIDEOQUALITY = "ro.freecomm.videocamera.quality";
    public static final String CAMERA_MEDIA_FILE_FORMAT = "ro.freecomm.camera.file_format";
    public static final String BACK_CAMERA_DEFAULT_PICTURE_SIZE_VALUE = "ro.bcamera.def_pic_size";
    public static final String FRONT_CAMERA_DEFAULT_PICTURE_SIZE_VALUE = "ro.fcamera.def_pic_size";
    public static final String BACK_CAMERA_DEFAULT_VIDEO_QUALITY_VALUE = "ro.bcamera.def_video_quality";
    public static final String FRONT_CAMERA_DEFAULT_VIDEO_QUALITY_VALUE = "ro.fcamera.def_video_quality";
    public static final String CAMERA_FACE_DETECT_VALUE = "ro.fcamera.face_detect";
    public static final String CAMERA_SHARPNESS_VALUE = "ro.fcamera.sharpness";
 
    public CameraSettings(Activity activity, Parameters parameters,
                          int cameraId, CameraInfo[] cameraInfo) {
        mContext = activity;
        mParameters = parameters;
        mCameraId = cameraId;
        mCameraInfo = cameraInfo;
    }

    public PreferenceGroup getPreferenceGroup(int preferenceRes) {
        PreferenceInflater inflater = new PreferenceInflater(mContext);
        PreferenceGroup group =
                (PreferenceGroup) inflater.inflate(preferenceRes);
        initPreference(group);
        return group;
    }

    public static String getDefaultVideoQuality(int cameraId,
            String defaultQuality) {
        int quality = Integer.valueOf(defaultQuality);
        if (CamcorderProfile.hasProfile(cameraId, quality)) {
            return defaultQuality;
        }
        return Integer.toString(CamcorderProfile.QUALITY_HIGH);
    }

    //BEGIN: Added by zhanghongxing at 2013-01-09 for full preview
    public static String initialVideoQuality(Context context, int currentCameraId) {
        int backCameraId = CameraHolder.instance().getBackCameraId();
        String defaultStr = null;
        if (currentCameraId == backCameraId) {
//            defaultStr = context.getString(R.string.pref_back_video_quality_default);
            defaultStr = SystemProperties.get(BACK_CAMERA_DEFAULT_VIDEO_QUALITY_VALUE,"");
        } else {
//            defaultStr = context.getString(R.string.pref_front_video_quality_default);
            defaultStr = SystemProperties.get(FRONT_CAMERA_DEFAULT_VIDEO_QUALITY_VALUE,"");
        }
        int quality = 0;
        if(defaultStr == null || defaultStr.length() < 1) {
            ArrayList<String> supprotedQuality = getSupportedVideoQuality();
            String[]  candidateArray = (currentCameraId == backCameraId) ? 
                    context.getResources().getStringArray(R.array.pref_video_quality_entryvalues):
                        context.getResources().getStringArray(R.array.pref_front_video_quality_entryvalues);
            for(String candidate:candidateArray) {
                if(supprotedQuality.indexOf(candidate) >= 0) {
                    defaultStr = candidate;
                    break;
                }
            }
        }

        if(defaultStr != null) {
            quality = Integer.valueOf(defaultStr);
            if (CamcorderProfile.hasProfile(currentCameraId, quality)) {
                SharedPreferences.Editor editor = ComboPreferences
                        .get(context).edit();
                editor.putString(KEY_VIDEO_QUALITY, defaultStr);
                editor.apply();
                return defaultStr;
            }
        }
        return Integer.toString(CamcorderProfile.QUALITY_HIGH);
    }
    //END:   Added by zhanghongxing at 2013-01-09

    //BEGIN: Added by zhanghongxing at 2013-04-10
    public String initialVideoQuality() {
        int backCameraId = CameraHolder.instance().getBackCameraId();
        String defaultStr = null;
        if (mCameraId == backCameraId) {
//            defaultStr = mContext.getString(R.string.pref_back_video_quality_default);
            defaultStr = SystemProperties.get(BACK_CAMERA_DEFAULT_VIDEO_QUALITY_VALUE,"");
        } else {
//            defaultStr = mContext.getString(R.string.pref_front_video_quality_default);
            defaultStr = SystemProperties.get(FRONT_CAMERA_DEFAULT_VIDEO_QUALITY_VALUE,"");
        }
        int quality = 0;
        if(defaultStr == null || defaultStr.length() < 1) {
            ArrayList<String> supprotedQuality = getSupportedVideoQuality();
            String[]  candidateArray = (mCameraId == backCameraId) ?
                    mContext.getResources().getStringArray(R.array.pref_video_quality_entryvalues) :
                        mContext.getResources().getStringArray(R.array.pref_front_video_quality_entryvalues);

            for(String candidate:candidateArray) {
                if(supprotedQuality.indexOf(candidate) >= 0) {
                    defaultStr = candidate;
                    break;
                }
            }
        }
        if(defaultStr != null) {
            quality = Integer.valueOf(defaultStr);
            if (CamcorderProfile.hasProfile(mCameraId, quality)) {
                SharedPreferences.Editor editor = ComboPreferences
                        .get(mContext).edit();
                editor.putString(KEY_VIDEO_QUALITY, defaultStr);
                editor.apply();
                return defaultStr;
            }
        }
        return Integer.toString(CamcorderProfile.QUALITY_HIGH);
    }
    //END:   Added by zhanghongxing at 2013-04-10

    public static void initialCameraPictureSize(
            Context context, Parameters parameters, int currentCameraId) {
        // When launching the camera app first time, we will set the picture
        // size to the first one in the list defined in "arrays.xml" and is also
        // supported by the driver.
        List<Size> supported = parameters.getSupportedPictureSizes();
        if (supported == null) return;
        //BEGIN: Added by zhanghongxing at 2013-01-09 for full preview
        int backCameraId = CameraHolder.instance().getBackCameraId();
        String defaultStr = null;
        String[] candidateArray = null;
        if (currentCameraId == backCameraId) {
//            defaultStr = context.getString(R.string.pref_back_camera_picturesize_default);
            candidateArray = context.getResources().getStringArray(
                    R.array.pref_camera_picturesize_entryvalues);
            defaultStr = SystemProperties.get(BACK_CAMERA_DEFAULT_PICTURE_SIZE_VALUE,"");
        } else {
            //BEGIN: Modified by zhanghongxing at 2013-04-09 for FBD-96/97
            // defaultStr = null;
//            defaultStr = context.getString(R.string.pref_front_camera_picturesize_default);
            candidateArray = context.getResources().getStringArray(
                    R.array.pref_front_camera_picturesize_entryvalues);
            defaultStr = SystemProperties.get(FRONT_CAMERA_DEFAULT_PICTURE_SIZE_VALUE,"");
            //END:   Modified by zhanghongxing at 2013-04-09 for FBD-96/97
        }

        //END:   Added by zhanghongxing at 2013-01-09
        for (String candidate : candidateArray) {
            if (setCameraPictureSize(candidate, supported, parameters)) {
                if (defaultStr == null || defaultStr.length() < 1) {
                    SharedPreferences.Editor editor = ComboPreferences
                            .get(context).edit();
                    editor.putString(KEY_PICTURE_SIZE, candidate);
                    editor.apply();
                    return;
                } else {
                    if (candidate.equals(defaultStr)) {
                        SharedPreferences.Editor editor = ComboPreferences
                                .get(context).edit();
                        editor.putString(KEY_PICTURE_SIZE, candidate);
                        editor.apply();
                        return;
                    }
                }
                //END:   Modified by zhanghongxing at 2013-01-09
            }
        }
        Log.e(TAG, "No supported picture size found");
    }

    public static void removePreferenceFromScreen(
            PreferenceGroup group, String key) {
        removePreference(group, key);
    }

    public static boolean setCameraPictureSize(
            String candidate, List<Size> supported, Parameters parameters) {
        int index = candidate.indexOf('x');
        if (index == NOT_FOUND) return false;
        int width = Integer.parseInt(candidate.substring(0, index));
        int height = Integer.parseInt(candidate.substring(index + 1));
        for (Size size : supported) {
            if (size.width == width && size.height == height) {
                parameters.setPictureSize(width, height);
                return true;
            }
        }
        return false;
    }

    private void qcomInitPreferences(PreferenceGroup group){
         //Qcom Preference add here
        ListPreference powerMode = group.findPreference(KEY_POWER_MODE);
        ListPreference zsl = group.findPreference(KEY_ZSL);
        ListPreference colorEffect = group.findPreference(KEY_COLOR_EFFECT);
        ListPreference faceDetection = group.findPreference(KEY_FACE_DETECTION);
        ListPreference touchAfAec = group.findPreference(KEY_TOUCH_AF_AEC);
        ListPreference selectableZoneAf = group.findPreference(KEY_SELECTABLE_ZONE_AF);
        ListPreference saturation = group.findPreference(KEY_SATURATION);
        ListPreference contrast = group.findPreference(KEY_CONTRAST);
        ListPreference sharpness = group.findPreference(KEY_SHARPNESS);
        ListPreference autoExposure = group.findPreference(KEY_AUTOEXPOSURE);
        ListPreference antiBanding = group.findPreference(KEY_ANTIBANDING);
        ListPreference mIso = group.findPreference(KEY_ISO);
        ListPreference lensShade = group.findPreference(KEY_LENSSHADING);
        ListPreference histogram = group.findPreference(KEY_HISTOGRAM);
        ListPreference denoise = group.findPreference(KEY_DENOISE);
        ListPreference redeyeReduction = group.findPreference(KEY_REDEYE_REDUCTION);
        ListPreference hfr = group.findPreference(KEY_VIDEO_HIGH_FRAME_RATE);
        ListPreference hdr = group.findPreference(KEY_AE_BRACKET_HDR);
        ListPreference jpegQuality = group.findPreference(KEY_JPEG_QUALITY);
        ListPreference videoSnapSize = group.findPreference(KEY_VIDEO_SNAPSHOT_SIZE);
        ListPreference pictureFormat = group.findPreference(KEY_PICTURE_FORMAT);
        //BEGIN: Added by zhanghongxing at 2013-01-06 for DER-173
        ListPreference videoColorEffect = group.findPreference(KEY_VIDEO_COLOR_EFFECT);
        //END:   Added by zhanghongxing at 2013-01-06
        
        
//        ListPreference videoEffect = group.findPreference(KEY_VIDEO_EFFECT);


        if (touchAfAec != null) {
            filterUnsupportedOptions(group,
                    touchAfAec, mParameters.getSupportedTouchAfAec());
        }

        if (selectableZoneAf != null) {
            filterUnsupportedOptions(group,
                    selectableZoneAf, mParameters.getSupportedSelectableZoneAf());
        }

        if (mIso != null) {
            filterUnsupportedOptions(group,
                    mIso, mParameters.getSupportedIsoValues());
        }

        /* if (lensShade!= null) {
            filterUnsupportedOptions(group,
                    lensShade, mParameters.getSupportedLensShadeModes());
        }*/

		if (redeyeReduction != null) {
            filterUnsupportedOptions(group,
                    redeyeReduction, mParameters.getSupportedRedeyeReductionModes());
        }

        if (hfr != null) {
            filterUnsupportedOptions(group,
                    hfr, mParameters.getSupportedVideoHighFrameRateModes());
        }

        if (denoise != null) {
            filterUnsupportedOptions(group,
            denoise, mParameters.getSupportedDenoiseModes());
        }

        if (pictureFormat != null) {
            // getSupportedPictureFormats() returns the List<Integar>
            // which need to convert to List<String> format.
            List<Integer> SupportedPictureFormatsInIntArray = mParameters.getSupportedPictureFormats();
            List<String>  SupportedPictureFormatsInStrArray = new ArrayList<String>();
            for (Integer picIndex : SupportedPictureFormatsInIntArray) {
                 SupportedPictureFormatsInStrArray.add(cameraFormatForPixelFormatVal(picIndex.intValue()));
            }
//            if(1 == mParameters.getInt("raw-format-supported")) {
             // getSupportedPictureFormats() doesnt have the support for RAW Pixel format.
             // so need to add explicitly the RAW Pixel Format.
//             SupportedPictureFormatsInStrArray.add(PIXEL_FORMAT_RAW);
//            }
            //BEGIN: Modified by zhanghongxing at 2013-03-12 for FBD-115
            // Remove the picture format.
            // filterUnsupportedOptions(group, pictureFormat, SupportedPictureFormatsInStrArray);
            filterUnsupportedOptions(group, pictureFormat, null);
            //END:   Modified by zhanghongxing at 2013-03-12
        }

        if (colorEffect != null) {
            filterUnsupportedOptions(group,
                    colorEffect, mParameters.getSupportedColorEffects());
            CharSequence[] entries = colorEffect.getEntries();
            if(entries == null || entries.length <= 0 ) {
                filterUnsupportedOptions(group, colorEffect, null);
            }
        }

        if (antiBanding != null) {
            filterUnsupportedOptions(group,
                     antiBanding, mParameters.getSupportedAntibanding());
        }
        if (autoExposure != null) {
            filterUnsupportedOptions(group,
                     autoExposure, mParameters.getSupportedAutoexposure());
        }
        if (!mParameters.isPowerModeSupported())
        {
             filterUnsupportedOptions(group,
                    videoSnapSize, null);
        }else{
            filterUnsupportedOptions(group, videoSnapSize, sizeListToStringList(
                    mParameters.getSupportedPictureSizes()));
        }
        if (histogram!= null) {
            filterUnsupportedOptions(group,
                    histogram, mParameters.getSupportedHistogramModes());
        }

        if (hdr!= null) {
//            filterUnsupportedOptions(group,
//                    hdr, mParameters.getSupportedAEBracketModes());
        }

        //BEGIN: Added by zhanghongxing at 2013-01-06 for DER-173
        if (videoColorEffect != null) {
            //BEGIN: Modified by zhanghongxing at 2013-04-09 for FBD-722/723
            //Remove the front camera video color effect.
             filterUnsupportedOptions(group,
                    videoColorEffect, mParameters.getSupportedColorEffects());
            CharSequence[] entries = videoColorEffect.getEntries();
            if(entries == null || entries.length <= 0 ) {
                filterUnsupportedOptions(group, videoColorEffect, null);
            }
            //END:   Modified by zhanghongxing at 2013-04-09
        }
        //END:   Added by zhanghongxing at 2013-01-06
        
//        if (videoEffect != null) {
//            filterUnsupportedOptions(group,
//            		videoEffect, mParameters.getSupportedColorEffects());
//        } 
    }

    private void initPreference(PreferenceGroup group) {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(info);
        int totalMem = (int)info.totalMem/(1024*1024);
        if( totalMem < 512) {
            // Remove the zsl and HDR option if the memory is lower than 512M.
            removePreference(group, KEY_ZSL);
            removePreference(group, KEY_AE_BRACKET_HDR);
        }
        
        //START: add by xiongzhu for ARD-265
        if (mContext.getResources().getBoolean(R.bool.remove_timedelayrecording)) {
            removePreference(group,KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
        }
       //END: add by xiongzhu for ARD-265

        //BEGIN:Add by wangbin at 2013-7-24 for WXY-741.
        if (mContext.getResources().getBoolean(R.bool.remove_silentmode)) {
            removePreference(group, KEY_SILENT_MODE);
        }
        //END:Add by wangbin at 2-13-7-24 for WXY-741.
        
        // added by zhu.xiong for bug ARD-40 about remove storage preference in single SD card case at 2013-05-30 begin
        ListPreference storagePlace = group.findPreference(KEY_CAMERA_STORAGE_PLACE);
		if (FxEnvironment.isSingleSdcard()) {
//			ArrayList<String> supported = new ArrayList<String>();
//			supported.add(mContext.getResources().getString(R.string.pref_storage_place_value_memory));
//			storagePlace.filterUnsupported(supported);
//			resetIfInvalid(storagePlace);
			//modified by zhangzw,hide storage when only one sdcard bug:ARD-1565
			removePreference(group, KEY_CAMERA_STORAGE_PLACE);
		}
		// added by zhu.xiong for bug ARD-40 about remove storage preference in single SD card case at 2013-05-30 end
		
		ArrayList<String> unEnabled = new ArrayList<String>();
		 
        ListPreference videoQuality = group.findPreference(KEY_VIDEO_QUALITY);
        ListPreference frontVideoQuality = group.findPreference(KEY_FRONT_VIDEO_QUALITY);
        ListPreference timeLapseInterval = group.findPreference(KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
        ListPreference pictureSize = group.findPreference(KEY_PICTURE_SIZE);
        ListPreference frontPictureSize = group.findPreference(KEY_FRONT_PICTURE_SIZE);
        ListPreference whiteBalance =  group.findPreference(KEY_WHITE_BALANCE);
        ListPreference sceneMode = group.findPreference(KEY_SCENE_MODE);
        ListPreference flashMode = group.findPreference(KEY_FLASH_MODE);
        ListPreference focusMode = group.findPreference(KEY_FOCUS_MODE);
        ListPreference exposure = group.findPreference(KEY_EXPOSURE);
        IconListPreference cameraIdPref =
                (IconListPreference) group.findPreference(KEY_CAMERA_ID);
        ListPreference videoFlashMode =
                group.findPreference(KEY_VIDEOCAMERA_FLASH_MODE);
        ListPreference videoEffect = group.findPreference(KEY_VIDEO_EFFECT);
        //BEGIN: Added by zhanghongxing at 2013-01-06 for DER-173
        ListPreference videoWhiteBalance =  group.findPreference(KEY_VIDEO_WHITE_BALANCE);
        //END:   Added by zhanghongxing at 2013-01-06
         
        ListPreference takePictureMode = group.findPreference(KEY_CAMERA_TAKEPICTURE_MODE);
        ListPreference volumeMode = group.findPreference(KEY_VOLUME_KEY_MODE);
        // Since the screen could be loaded from different resources, we need
        // to check if the preference is available here
        if(mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
        	if (frontVideoQuality != null) {
        		removePreference(group, KEY_VIDEO_QUALITY);
        		frontVideoQuality.setPrefKey(KEY_VIDEO_QUALITY);
                filterUnsupportedOptions(group, frontVideoQuality, getSupportedVideoQuality());
            }
        } else {
        	if (videoQuality != null) {
                filterUnsupportedOptions(group, videoQuality, getSupportedVideoQuality());
            }
        }
        resetPictureSize = mContext.getResources().getBoolean(R.bool.reset_picturesize);//Add by wangbin at 2013-6-5
        if (pictureSize != null) {
            //BEGIN: Add by wamgnin at 2013-6-5
            if (resetPictureSize) {
                needResetPictureSize(group, pictureSize);
            } else {
                int backCameraId = CameraHolder.instance().getBackCameraId();
                if (mCameraId == backCameraId) {
                   filterUnsupportedOptions(group, pictureSize, sizeListToStringList(mParameters.getSupportedPictureSizes()));
                   
                } else {	
                	removePreference(group, KEY_PICTURE_SIZE);
                	frontPictureSize.setPrefKey(KEY_PICTURE_SIZE);
                    filterUnsupportedOptions(group, frontPictureSize, sizeListToStringList(
                        mParameters.getSupportedPictureSizes()));
               }
            }
        }
        if (whiteBalance != null) {
            filterUnsupportedOptions(group,
                    whiteBalance, mParameters.getSupportedWhiteBalance());
        }
        if (sceneMode != null) {
        	List<String> supportedList = mParameters.getSupportedSceneModes();
        	if(supportedList == null || supportedList.size() <= 0) {
        		sceneMode.setEnable(false);
        	} else {
        		filterUnsupportedOptions(group,sceneMode, supportedList);
        	}
        }
        if (flashMode != null) {
            filterUnsupportedOptions(group,
                    flashMode, mParameters.getSupportedFlashModes());
        }
        if (focusMode != null) {
            if (mParameters.getMaxNumFocusAreas() == 0) {
                filterUnsupportedOptions(group,
                        focusMode, mParameters.getSupportedFocusModes());
            } /*else {
                // Remove the focus mode if we can use tap-to-focus.
                removePreference(group, focusMode.getKey());
            }*/
        }
        if (videoFlashMode != null) {
            filterUnsupportedOptions(group,
                    videoFlashMode, mParameters.getSupportedFlashModes());
        }
        if (exposure != null) buildExposureCompensation(group, exposure);
        if (cameraIdPref != null) buildCameraId(group, cameraIdPref);

        if (timeLapseInterval != null) resetIfInvalid(timeLapseInterval);
        if (videoEffect != null) {
            initVideoEffect(group, videoEffect);
            resetIfInvalid(videoEffect);
        }
        //BEGIN: Added by zhanghongxing at 2013-01-06 for DER-173
        if (videoWhiteBalance != null) {
            filterUnsupportedOptions(group,
                    videoWhiteBalance, mParameters.getSupportedWhiteBalance());
        }
        
        if (takePictureMode != null) {
        	unEnabled.clear();
        	if(mContext instanceof com.android.camera.Camera){
                if(mCameraId == CameraInfo.CAMERA_FACING_FRONT || ((com.android.camera.Camera)mContext).isImageCaptureIntent()) {
                    unEnabled.add(mContext.getResources().getString(R.string.pref_camera_takepicture_mode_value_panorama));
                    unEnabled.add(mContext.getResources().getString(R.string.pref_camera_takepicture_mode_value_hdr));
                    if (((com.android.camera.Camera)mContext).isImageCaptureIntent()/* ||
                    		mCameraId == CameraInfo.CAMERA_FACING_FRONT*/) {
                        unEnabled.add(mContext.getResources().getString(R.string.pref_camera_takepicture_mode_value_continue));
                    }
                    ((IconListPreference)takePictureMode).filterUnEnabled(unEnabled);
                }
        	}
        }
        //END:   Added by zhanghongxing at 2013-01-06
        qcomInitPreferences(group);
    }

    //Add by wangbin at 2013-6-5 for backCamera picturesize 16 : 9;frontCamera 4 : 3.
    private void needResetPictureSize(
            PreferenceGroup group, ListPreference pictureSize) {
        final double pictureSizeRatio = 0.75;
                List<Size> pictureSizeTemp = new ArrayList<Size>();
                int backCameraId = CameraHolder.instance().getBackCameraId();
                if (mCameraId == backCameraId) {
                    List<Size> supported = mParameters.getSupportedPictureSizes();
                    for (Size size : supported) {
                        double ratio = (double)size.height / size.width;
                        if (size.width == 2592 || size.height == 1944) {
                            pictureSizeTemp.add(size);
                        }
                        if (ratio < pictureSizeRatio) {
                            pictureSizeTemp.add(size);
                        }
                   }
                } else {
                    List<Size> supported = mParameters.getSupportedPictureSizes();
                    for (Size size : supported) {
                        double ratio = (double)size.height / size.width;
                        if (ratio >= pictureSizeRatio) {
                            pictureSizeTemp.add(size);
                        }
                   }
                }
                filterUnsupportedOptions(group, pictureSize, sizeListToStringList(pictureSizeTemp));
    }
    private void buildExposureCompensation(
            PreferenceGroup group, ListPreference exposure) {
        int max = mParameters.getMaxExposureCompensation();
        int min = mParameters.getMinExposureCompensation();
        if (max == 0 && min == 0) {
            removePreference(group, exposure.getKey());
            return;
        }
        float step = mParameters.getExposureCompensationStep();

        // show only integer values for exposure compensation
        int maxValue = (int) FloatMath.floor(max * step);
        int minValue = (int) FloatMath.ceil(min * step);
        CharSequence entries[] = new CharSequence[maxValue - minValue + 1];
        CharSequence entryValues[] = new CharSequence[maxValue - minValue + 1];
        for (int i = minValue; i <= maxValue; ++i) {
            entryValues[maxValue - i] = Integer.toString(Math.round(i / step));
            StringBuilder builder = new StringBuilder();
            if (i > 0) builder.append('+');
            entries[maxValue - i] = builder.append(i).toString();
        }
        exposure.setEntries(entries);
        exposure.setEntryValues(entryValues);
    }

    private void buildCameraId(
            PreferenceGroup group, IconListPreference preference) {
        int numOfCameras = mCameraInfo.length;
        if (numOfCameras < 2) {
            removePreference(group, preference.getKey());
            return;
        }

        CharSequence[] entryValues = new CharSequence[2];
        for (int i = 0; i < mCameraInfo.length; ++i) {
            int index =
                    (mCameraInfo[i].facing == CameraInfo.CAMERA_FACING_FRONT)
                    ? CameraInfo.CAMERA_FACING_FRONT
                    : CameraInfo.CAMERA_FACING_BACK;
            if (entryValues[index] == null) {
                entryValues[index] = "" + i;
                if (entryValues[((index == 1) ? 0 : 1)] != null) break;
            }
        }
        preference.setEntryValues(entryValues);
    }

    private static boolean removePreference(PreferenceGroup group, String key) {
        for (int i = 0, n = group.size(); i < n; i++) {
            CameraPreference child = group.get(i);
            if (child instanceof PreferenceGroup) {
                if (removePreference((PreferenceGroup) child, key)) {
                    return true;
                }
            }
            if (child instanceof ListPreference &&
                    ((ListPreference) child).getKey().equals(key)) {
                group.removePreference(i);
                return true;
            }
        }
        return false;
    }

    private void filterUnsupportedOptions(PreferenceGroup group,
            ListPreference pref, List<String> supported) {

        // Remove the preference if the parameter is not supported or there is
        // only one options for the settings.
        if (supported == null || supported.size() <= 1) {
        	if (pref instanceof IconListPreference){
                ((IconListPreference) pref).setAvailable(false);
        	} else {
        		removePreference(group, pref.getKey());
        	}
            return;
        }

        pref.filterUnsupported(supported);
        if (pref.getEntries().length <= 1 && !(pref instanceof IconListPreference 
        		) && !pref.getKey().equals(CameraSettings.KEY_PICTURE_SIZE)) {
            removePreference(group, pref.getKey());
            return;
        }

        resetIfInvalid(pref);
    }

    private void resetIfInvalid(ListPreference pref) {
        // Set the value to the first entry if it is invalid.
        String value = pref.getValue();
        if (pref.findIndexOfValue(value) == NOT_FOUND) {
        	if(pref.getKey().equalsIgnoreCase(KEY_VIDEO_QUALITY)) {
        		return;
        	}
            pref.setValueIndex(0);
        }
    }

    private static List<String> sizeListToStringList(List<Size> sizes) {
        ArrayList<String> list = new ArrayList<String>();
        for (Size size : sizes) {
            list.add(String.format("%dx%d", size.width, size.height));
        }
        return list;
    }

    public static void upgradeLocalPreferences(SharedPreferences pref) {
        int version;
        try {
            version = pref.getInt(KEY_LOCAL_VERSION, 0);
        } catch (Exception ex) {
            version = 0;
        }
        if (version == CURRENT_LOCAL_VERSION) return;

        SharedPreferences.Editor editor = pref.edit();
        if (version == 1) {
            // We use numbers to represent the quality now. The quality definition is identical to
            // that of CamcorderProfile.java.
            editor.remove("pref_video_quality_key");
        }
        editor.putInt(KEY_LOCAL_VERSION, CURRENT_LOCAL_VERSION);
        editor.apply();
    }

    public static void upgradeGlobalPreferences(SharedPreferences pref) {
        upgradeOldVersion(pref);
        upgradeCameraId(pref);
    }

    private static void upgradeOldVersion(SharedPreferences pref) {
        int version;
        try {
            version = pref.getInt(KEY_VERSION, 0);
        } catch (Exception ex) {
            version = 0;
        }
        if (version == CURRENT_VERSION) return;

        SharedPreferences.Editor editor = pref.edit();
        if (version == 0) {
            // We won't use the preference which change in version 1.
            // So, just upgrade to version 1 directly
            version = 1;
        }
        if (version == 1) {
            // Change jpeg quality {65,75,85} to {normal,fine,superfine}
            String quality = pref.getString(KEY_JPEG_QUALITY, "85");
            if (quality.equals("65")) {
                quality = "normal";
            } else if (quality.equals("75")) {
                quality = "fine";
            } else {
                quality = "superfine";
            }
            editor.putString(KEY_JPEG_QUALITY, quality);
            String timerSnap = pref.getString(KEY_TIMER_SNAP, "off");
            editor.putString(KEY_TIMER_SNAP, timerSnap);
            version = 2;
        }
        if (version == 2) {
            editor.putString(KEY_RECORD_LOCATION,
                    pref.getBoolean(KEY_RECORD_LOCATION, false)
                    ? RecordLocationPreference.VALUE_ON
                    : RecordLocationPreference.VALUE_NONE);
            //Add by wangbin at 2013-03-22
            boolean isSlientMode = pref.getBoolean(KEY_SILENT_MODE, false);
            editor.putString(KEY_SILENT_MODE,((isSlientMode == true) ? SILENT_MODE_ON : SILENT_MODE_OFF));

            version = 3;
        }
        //BEGIN: Modified by zhanghongxing at 2013-01-06 for DER-173
        if (version == 3) {
            editor.putString(KEY_VIDEO_RECORD_LOCATION,
                    pref.getBoolean(KEY_VIDEO_RECORD_LOCATION, false)
                    ? RecordLocationPreference.VALUE_ON
                    : RecordLocationPreference.VALUE_NONE);
            version = 4;
        }
        if (version == 4) {
            // Just use video quality to replace it and
            // ignore the current settings.
            editor.remove("pref_camera_videoquality_key");
            editor.remove("pref_camera_video_duration_key");
        }
        //END:   Modified by zhanghongxing at 2013-01-06

        editor.putInt(KEY_VERSION, CURRENT_VERSION);
        editor.apply();
    }

    private static void upgradeCameraId(SharedPreferences pref) {
        // The id stored in the preference may be out of range if we are running
        // inside the emulator and a webcam is removed.
        // Note: This method accesses the global preferences directly, not the
        // combo preferences.
        int cameraId = readPreferredCameraId(pref);
        if (cameraId == 0) return;  // fast path

        int n = CameraHolder.instance().getNumberOfCameras();
        if (cameraId < 0 || cameraId >= n) {
            writePreferredCameraId(pref, 0);
        }
    }

    public static int readPreferredCameraId(SharedPreferences pref) {
        return Integer.parseInt(pref.getString(KEY_CAMERA_ID, "0"));
    }

    public static void writePreferredCameraId(SharedPreferences pref,
            int cameraId) {
        Editor editor = pref.edit();
        editor.putString(KEY_CAMERA_ID, Integer.toString(cameraId));
        editor.apply();
    }

    public static int readExposure(ComboPreferences preferences) {
        String exposure = preferences.getString(
                CameraSettings.KEY_EXPOSURE,
                EXPOSURE_DEFAULT_VALUE);
        try {
            return Integer.parseInt(exposure);
        } catch (Exception ex) {
            Log.e(TAG, "Invalid exposure: " + exposure);
        }
        return 0;
    }

    public static int readEffectType(SharedPreferences pref) {
        String effectSelection = pref.getString(KEY_VIDEO_EFFECT, "none");
        if (effectSelection.equals("none")) {
            return EffectsRecorder.EFFECT_NONE;
        } else if (effectSelection.startsWith("goofy_face")) {
            return EffectsRecorder.EFFECT_GOOFY_FACE;
        } else if (effectSelection.startsWith("backdropper")) {
            return EffectsRecorder.EFFECT_BACKDROPPER;
        }
        Log.e(TAG, "Invalid effect selection: " + effectSelection);
        return EffectsRecorder.EFFECT_NONE;
    }

    public static Object readEffectParameter(SharedPreferences pref) {
        String effectSelection = pref.getString(KEY_VIDEO_EFFECT, "none");
        if (effectSelection.equals("none")) {
            return null;
        }
        int separatorIndex = effectSelection.indexOf('/');
        String effectParameter =
                effectSelection.substring(separatorIndex + 1);
        if (effectSelection.startsWith("goofy_face")) {
            if (effectParameter.equals("squeeze")) {
                return EffectsRecorder.EFFECT_GF_SQUEEZE;
            } else if (effectParameter.equals("big_eyes")) {
                return EffectsRecorder.EFFECT_GF_BIG_EYES;
            } else if (effectParameter.equals("big_mouth")) {
                return EffectsRecorder.EFFECT_GF_BIG_MOUTH;
            } else if (effectParameter.equals("small_mouth")) {
                return EffectsRecorder.EFFECT_GF_SMALL_MOUTH;
            } else if (effectParameter.equals("big_nose")) {
                return EffectsRecorder.EFFECT_GF_BIG_NOSE;
            } else if (effectParameter.equals("small_eyes")) {
                return EffectsRecorder.EFFECT_GF_SMALL_EYES;
            }
        } else if (effectSelection.startsWith("backdropper")) {
            // Parameter is a string that either encodes the URI to use,
            // or specifies 'gallery'.
            return effectParameter;
        }

        Log.e(TAG, "Invalid effect selection: " + effectSelection);
        return null;
    }


    public static void restorePreferences(Context context,
            ComboPreferences preferences, Parameters parameters) {
        int currentCameraId = readPreferredCameraId(preferences);

        // Clear the preferences of both cameras.
        int backCameraId = CameraHolder.instance().getBackCameraId();
        if (backCameraId != -1) {
            preferences.setLocalId(context, backCameraId);
            Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
        }
        int frontCameraId = CameraHolder.instance().getFrontCameraId();
        if (frontCameraId != -1) {
            preferences.setLocalId(context, frontCameraId);
            Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
        }

        // Switch back to the preferences of the current camera. Otherwise,
        // we may write the preference to wrong camera later.
        preferences.setLocalId(context, currentCameraId);

        upgradeGlobalPreferences(preferences.getGlobal());
        upgradeLocalPreferences(preferences.getLocal());

        // Write back the current camera id because parameters are related to
        // the camera. Otherwise, we may switch to the front camera but the
        // initial picture size is that of the back camera.
        //BEGIN: Modified by zhanghongxing at 2013-01-09 for full preview
        // initialCameraPictureSize(context, parameters);
        initialCameraPictureSize(context, parameters, currentCameraId);
        initialVideoQuality(context, currentCameraId);
        //END:   Modified by zhanghongxing at 2013-01-09
        writePreferredCameraId(preferences, currentCameraId);

        String sharpnessStr = SystemProperties.get(CameraSettings.CAMERA_SHARPNESS_VALUE,"");
        if(!sharpnessStr.equals("")) {
            SharedPreferences.Editor editor = ComboPreferences
                    .get(context).edit();
             editor.putString(CameraSettings.KEY_SHARPNESS,sharpnessStr);
             editor.commit();
        }
    }

    //private int checkSupportedVideoQuality(List <Size> supported)
    private static boolean checkSupportedVideoQuality(int width, int height){
        List <Size> supported = mParameters.getSupportedVideoSizes();
        int flag = 0;
        for (Size size : supported){
            //since we are having two profiles with same height, we are checking with height
            if (size.height == 480) {
                if (size.height == height && size.width == width) {
                    flag = 1;
                    break;
                }
            } else {
                if (size.width == width) {
                    flag = 1;
                    break;
                }
            }
        }
        if (flag == 1)
            return true;

        return false;
    }

    private static ArrayList<String> getSupportedVideoQuality() {
        ArrayList<String> supported = new ArrayList<String>();
        // Check for supported quality
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_1080P)) {
           if (checkSupportedVideoQuality(1920,1088)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_1080P));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P)) {
           if (checkSupportedVideoQuality(1280,720)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_720P));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P)) {
           if (checkSupportedVideoQuality(720,480)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_480P));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QCIF)) {
           if (checkSupportedVideoQuality(176,144)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_QCIF));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_CIF)) {
           if (checkSupportedVideoQuality(352,288)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_CIF));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_FWVGA)) {
           if (checkSupportedVideoQuality(864,480)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_FWVGA));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_WVGA)) {
           if (checkSupportedVideoQuality(800,480)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_WVGA));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_VGA)) {
           if (checkSupportedVideoQuality(640,480)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_VGA));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_WQVGA)) {
           if (checkSupportedVideoQuality(432,240)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_WQVGA));
           }
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QVGA)) {
           if (checkSupportedVideoQuality(320,240)){
              supported.add(Integer.toString(CamcorderProfile.QUALITY_QVGA));
           }
        }
        return supported;
    }

    private void initVideoEffect(PreferenceGroup group, ListPreference videoEffect) {
        CharSequence[] values = videoEffect.getEntryValues();

        boolean goofyFaceSupported =
                EffectsRecorder.isEffectSupported(EffectsRecorder.EFFECT_GOOFY_FACE);
        boolean backdropperSupported =
                EffectsRecorder.isEffectSupported(EffectsRecorder.EFFECT_BACKDROPPER) &&
                mParameters.isAutoExposureLockSupported() &&
                mParameters.isAutoWhiteBalanceLockSupported();

        ArrayList<String> supported = new ArrayList<String>();
        for (CharSequence value : values) {
            String effectSelection = value.toString();
            if (!goofyFaceSupported && effectSelection.startsWith("goofy_face")) continue;
            if (!backdropperSupported && effectSelection.startsWith("backdropper")) continue;
            supported.add(effectSelection);
        }

        filterUnsupportedOptions(group, videoEffect, supported);
    }

     public static int getVidoeDurationInMillis(String quality) {
        if (VIDEO_QUALITY_MMS.equals(quality)) {
            return MMS_VIDEO_DURATION * 1000;
        } else if (VIDEO_QUALITY_YOUTUBE.equals(quality)) {
            return YOUTUBE_VIDEO_DURATION * 1000;
        }
        return DEFAULT_VIDEO_DURATION * 1000;
    }

    public static void buildVideoEncoder(
           PreferenceGroup group, ListPreference preference, boolean h264Only) {
        int numSupported = 3;
        if (h264Only) numSupported = 1;
        CharSequence entries[] = new CharSequence[numSupported];
        CharSequence entryValues[] = new CharSequence[numSupported];
        if (h264Only) {
            entries[0] = "H264";
            entryValues[0] = "h264";
            preference.setValue("h264");
        } else {
            entries[0] = "MPEG4";
            entries[1] = "H263";
            entries[2] = "H264";
            entryValues[0] = "mpeg4";
            entryValues[1] = "h263";
            entryValues[2] = "h264";
        }
        preference.setEntries(entries);
        preference.setEntryValues(entryValues);
    }
    private String cameraFormatForPixelFormatVal(int pixel_format) {
            switch(pixel_format) {
            case ImageFormat.NV16:       return PIXEL_FORMAT_YUV422SP;
            case ImageFormat.NV21:       return PIXEL_FORMAT_YUV420SP;
            case ImageFormat.YUY2:       return PIXEL_FORMAT_YUV422I;
            case ImageFormat.YV12:       return PIXEL_FORMAT_YUV420P;
            case ImageFormat.RGB_565:    return PIXEL_FORMAT_RGB565;
            case ImageFormat.JPEG:       return PIXEL_FORMAT_JPEG;
            case ImageFormat.BAYER_RGGB: return PIXEL_FORMAT_BAYER_RGGB;
            default:                     return null;
            }
    }
}
