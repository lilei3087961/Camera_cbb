/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;


import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera.CameraSettings;
import com.android.camera.IconListPreference;
import com.android.camera.ListPreference;
import com.android.camera.PreferenceGroup;
import com.android.camera.R;
import com.android.camera.VideoCamera;

import java.util.ArrayList;

import java.util.HashMap;
import android.util.Log;

/**
 * A view that contains camera setting indicators.
 */
public abstract class IndicatorControl extends RelativeLayout implements
        IndicatorButton.Listener, OtherSettingsPopup.Listener, Rotatable {
    @SuppressWarnings("unused")
    private static final String TAG = "IndicatorControl";
    public static final int MODE_CAMERA = 0;
    public static final int MODE_VIDEO = 1;

//    private OtherSettingsPopup[] mOtherSettingsPopups;

    private OnPreferenceChangedListener mListener;
    protected OnIndicatorEventListener mOnIndicatorEventListener;
    protected CameraPicker mCameraPicker;
    protected ZoomControl mZoomControl;

    private PreferenceGroup mPreferenceGroup;

    protected int mCurrentMode = MODE_CAMERA;

    ArrayList<AbstractIndicatorButton> mIndicators =
            new ArrayList<AbstractIndicatorButton>();
    
    //Added by zhanghongxing at 2013-01-07 for DER-107
    public static ArrayList<OtherSettingIndicatorButton> mOtherButtons = 
            new ArrayList<OtherSettingIndicatorButton>();

    public void setListener(OnPreferenceChangedListener listener) {
        mListener = listener;
        if (mCameraPicker != null) mCameraPicker.setListener(listener);
    }

    public IndicatorControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        int count = getChildCount();
        for (int i = 0 ; i < count ; ++i) {
            View view = getChildAt(i);
            if (view instanceof Rotatable) {
                ((Rotatable) view).setOrientation(orientation, animation);
            }
        }
    }

    public void setOnIndicatorEventListener(OnIndicatorEventListener listener) {
        mOnIndicatorEventListener = listener;
    }

    public void setPreferenceGroup(PreferenceGroup group) {
        mPreferenceGroup = group;
        // Preset the current mode from the title of preference group.
        String title = group.getTitle();
        if (title.equals(getContext().getString(
                R.string.pref_camcorder_settings_category))) {
            mCurrentMode = MODE_VIDEO;
        }
    }

    protected void addControls(String[] keys, HashMap otherSettingKeys) {
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                IconListPreference pref =
                        (IconListPreference) mPreferenceGroup.findPreference(keys[i]);

                if (pref != null && pref.isAvailable()) {
                    addIndicator(getContext(), pref);
                } else if (pref != null && !pref.isAvailable()){
                	addUnavailableIndicator(getContext(), pref);
                }
            }
        }

        // Add other settings indicator.
        if (otherSettingKeys != null) {
//            mOtherSettingsPopups = new OtherSettingsPopup[otherSettingKeys.size()];
            //BEGIN: Added by zhanghongxing at 2013-01-29
            mOtherButtons.clear();
            //END:   Added by zhanghongxing at 2013-01-29
            for (int i = 0; i < otherSettingKeys.size(); i++) {
                addOtherSettingIndicator(getContext(),
                                         R.drawable.othersetting_selector, (String []) otherSettingKeys.get(i));
            }
        }
    }

    protected void removeControls(int index, int count) {
        for (int i = index; i < index + count; i++) {
			if (getChildAt(i) instanceof AbstractIndicatorButton) {
            AbstractIndicatorButton b = (AbstractIndicatorButton) getChildAt(i);
            b.removePopupWindow();
            mIndicators.remove(b);
		   }
        }
        removeViews(index, count);
    }

    protected void updateControls(int index, int count, String[] keys, HashMap otherSettingKeys) {
        //if key value equal pref_video_quality_key, only update preference and don't update UI.
        for (int i = index; i < index + count; i++) {
        	//START :modified by xiongzhu for cbb-230
        	if (getChildAt(i) instanceof AbstractIndicatorButton) {
                AbstractIndicatorButton b = (AbstractIndicatorButton) getChildAt(i); 
                if (b instanceof OtherSettingIndicatorButton) {
                	//find preference and update preference
                	IconListPreference pref = (IconListPreference) mPreferenceGroup.findPreference("pref_video_quality_key");
                	if (pref != null) {
                		// move preference and don't update UI 
                		b.removePopupWindow();
                		int oldIndex = mIndicators.indexOf(b);
                		mIndicators.remove(b);
                		removeView(b);
                		OtherSettingIndicatorButton b_new;
                		b_new = new OtherSettingIndicatorButton(getContext(),
                				R.drawable.othersetting_selector,mPreferenceGroup,(String []) otherSettingKeys.get(0));
                		b_new.setSettingChangedListener(this);
                		b_new.setContentDescription(getResources().getString(
                				R.string.pref_camera_settings_category));
                		b_new.setId(R.id.other_setting_indicator);
                		addView(b_new, i);
                		mIndicators.add(oldIndex, b_new);
                		Log.i(TAG,"zzw==VideoCamera.bFlagDisanableToUpdateUI = "+VideoCamera.bFlagDisanableToUpdateUI);
                		if(!VideoCamera.bFlagDisanableToUpdateUI) {
                		    b_new.showPopup();
                		}
                		VideoCamera.bFlagDisanableToUpdateUI = false;
                	}
	          }
              //END :modified by xiongzhu for cbb-230
           }
        }
    }

    protected void initializeCameraPicker() {
        // Ignore if camera picker has been initialized.
        //if (mCameraPicker != null) return; delete by xiongzhu for cbb
        ListPreference pref = mPreferenceGroup.findPreference(
                CameraSettings.KEY_CAMERA_ID);
        if (pref == null) return;
        mCameraPicker = new CameraPicker(getContext());
        mCameraPicker.initialize(pref);
        addView(mCameraPicker);
    }

    protected void initializeZoomControl(boolean zoomSupported) {
        if (zoomSupported) {
            mZoomControl = (ZoomControl) findViewById(R.id.zoom_control);
            mZoomControl.setVisibility(View.VISIBLE);
        } else if (mZoomControl != null) {
            mZoomControl.setVisibility(View.GONE);
            mZoomControl = null;
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        // Return false so the pressed feedback of the back/front camera switch
        // can be showed right away.
        return false;
    }

    public IndicatorButton addIndicator(Context context, IconListPreference pref) {
        IndicatorButton b = new IndicatorButton(context, pref);
        b.setSettingChangedListener(this);
        b.setContentDescription(pref.getTitle());
        addView(b);
        mIndicators.add(b);
        return b;
    }
    
    public IndicatorButton addUnavailableIndicator(Context context, IconListPreference pref) {
        IndicatorButton b = new IndicatorButton(context, pref);
        b.setContentDescription(pref.getTitle());
        addView(b);
        mIndicators.add(b);
        return b;
    }

    public OtherSettingIndicatorButton addOtherSettingIndicator(Context context,
            int resId, String[] keys) {
        OtherSettingIndicatorButton b = new OtherSettingIndicatorButton(
                context, resId, mPreferenceGroup, keys);
        b.setSettingChangedListener(this);
        b.setContentDescription(getResources().getString(
                R.string.pref_camera_settings_category));
        b.setId(R.id.other_setting_indicator);
        addView(b);
        mIndicators.add(b);
        //BEGIN: Added by zhanghongxing at 2013-01-07 for DER-107
        mOtherButtons.add(b);
        //END:   Added by zhanghongxing at 2012-01-07
        return b;
    }

    //BEGIN: Added by zhanghongxing at 2013-01-07 for DER-107
    public void setOtherSettingButtonEnabled(boolean enabled) {
        for (OtherSettingIndicatorButton v: mOtherButtons) {
            v.setEnabled(enabled);
        }
    }
    //END:   Added by zhanghongxing at 2013-01-07

    @Override
    public void onRestorePreferencesClicked() {
        if (mListener != null) {
            mListener.onRestorePreferencesClicked();
        }
    }

    @Override
    public void onSettingChanged() {
        if (mListener != null) {
            mListener.onSharedPreferenceChanged();
        }
    }

    public boolean dismissSettingPopup() {
        for (AbstractIndicatorButton v: mIndicators) {
            if (v.dismissPopup()) {
                invalidate();
                return true;
            }
        }
        return false;
    }

    public View getActiveSettingPopup() {
        for (AbstractIndicatorButton v: mIndicators) {
            View result = v.getPopupWindow();
            
            if (result != null) {
            	return result;
            }
        }
        return null;
    }

    public View getActiveSubSettingPopup() {
        for (AbstractIndicatorButton v: mIndicators) {
            View result = v.getSubPopupWindow();
            
            if (result != null) {
            	return result;
            }
        }
        return null;
    }
    
    // Scene mode may override other camera settings (ex: flash mode).
    public void overrideSettings(final String ... keyvalues) {
        if (keyvalues.length % 2 != 0) {
            throw new IllegalArgumentException();
        }

        for (AbstractIndicatorButton b: mIndicators) {
            b.overrideSettings(keyvalues);
        }
    }


    // Scene mode may override other camera settings (ex: flash mode).
    public void enableItems(final String ... keyvalues) {
        if (keyvalues.length % 2 != 0) {
            throw new IllegalArgumentException();
        }

        for (AbstractIndicatorButton b: mIndicators) {
            b.enableItems(keyvalues);
        }
    }

    public void reloadPreferences() {
        mPreferenceGroup.reloadValue();
        for (AbstractIndicatorButton b: mIndicators) {
            b.reloadPreference();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            // Zoom buttons and shutter button are controlled by the activity.
            if (v instanceof AbstractIndicatorButton) {
                v.setEnabled(enabled);
                // Show or hide the indicator buttons during recording.
                //if (mCurrentMode == MODE_VIDEO) {
                //    v.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
               // }
            }
        }
        if (mCameraPicker != null) {
            mCameraPicker.setEnabled(enabled);
            //if (mCurrentMode == MODE_VIDEO) {
            //    mCameraPicker.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
            //}
        }
    }

    public void setupFilter(boolean enabled) {
        for (int i = 0, count = getChildCount(); i < count; i++) {
            View v = getChildAt(i);
            if (v instanceof TwoStateImageView) {
                ((TwoStateImageView) v).enableFilter(enabled);
            }
        }
    }
}
