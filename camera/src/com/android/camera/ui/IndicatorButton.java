/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import java.util.ArrayList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.Bitmap;
import com.android.camera.VideoCamera;
import com.android.camera.CameraSettings;
import com.android.camera.IconListPreference;
import com.android.camera.R;
import android.view.MotionEvent;

// An indicator button that represents one camera setting. Ex: flash. Pressing it opens a popup
// window.
public class IndicatorButton extends AbstractIndicatorButton
        implements BasicSettingPopup.Listener, EffectSettingPopup.Listener, ColorEffectSettingPopup.Listener {
    private static final String TAG = "IndicatorButton";
    private IconListPreference mPreference;
    // Scene mode can override the original preference value.
    private String mOverrideValue;
    private Listener mListener;

    static public interface Listener {
        public void onSettingChanged();
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    public IndicatorButton(Context context, IconListPreference pref) {
        super(context);
        mPreference = pref;
        reloadPreference();
    }
    
    @Override
    public void reloadPreference() {
    	if (!mPreference.isAvailable()) {
    		if (mPreference.getKey().equals(CameraSettings.KEY_FLASH_MODE) || mPreference.getKey().equals(CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE)){
    		setImageResource(R.drawable.ic_flash_dis);
    		} else if (mPreference.getKey().equals(CameraSettings.KEY_COLOR_EFFECT) || mPreference.getKey().equals(CameraSettings.KEY_VIDEO_COLOR_EFFECT)) {
    			setImageResource(R.drawable.ic_coloreffect_dis);
    		}
    		super.reloadPreference();
    		return;
    	}
        int[] largeIconIds = mPreference.getLargeIconIds();
        if (largeIconIds != null) {
            // Each entry has a corresponding icon.
            int index;
            if (mOverrideValue == null) {
                index = mPreference.findIndexOfValue(mPreference.getValue());
                // If can not find current index, set index to the first entryValues.
                if (index == -1) {
                    CharSequence[] entryValue = mPreference.getEntryValues();
                    CharSequence[] DefaultEntryValue = mPreference.getDefaultEntryValues();
                    if ((DefaultEntryValue != null) && mPreference.getKey().equals(CameraSettings.KEY_VIDEO_QUALITY))
                    {
                         //set video quality to default values
                         CharSequence[] DefaultValue = mPreference.getDefaultValues();
                         ArrayList<CharSequence> Defaultentry = new ArrayList<CharSequence>();
                         for (int i = 0, len = DefaultEntryValue.length; i < len; i++) {
                              Defaultentry.add(DefaultEntryValue[i]);
                         }
                         int size = Defaultentry.size();
                         entryValue = Defaultentry.toArray(new CharSequence[size]);
                         mPreference.setEntryValues(entryValue);
                         mPreference.setValue(VideoCamera.mSettings.initialVideoQuality());
                         largeIconIds = mPreference.getDefaultLargeIconId();
                         mPreference.setIconIds(largeIconIds);
                    }else{
                         mPreference.setValue(entryValue[0].toString());
                    }
                    index = mPreference.findIndexOfValue(mPreference.getValue());
                    mPreference.print();
                    Log.e(TAG, "Reset icon index. index = "+index);
                }
            } else {
                index = mPreference.findIndexOfValue(mOverrideValue);
                if (index == -1) {
                    // Avoid the crash if camera driver has bugs.
                    Log.e(TAG, "Fail to find override value=" + mOverrideValue);
                    mPreference.print();
                    return;
                }
            }
            if (!mPreference.getKey().equals(CameraSettings.KEY_COLOR_EFFECT) && 
            		!mPreference.getKey().equals(CameraSettings.KEY_VIDEO_COLOR_EFFECT)){
               setImageResource(largeIconIds[index]);
            } else {
               setImageResource(R.drawable.effect_preview_top_selector);
            }
        } else {
        	if (!mPreference.getKey().equals(CameraSettings.KEY_COLOR_EFFECT)){
            // The preference only has a single icon to represent it.
        		setImageResource(mPreference.getSingleIcon());
        	} else {
        		setImageResource(R.drawable.effect_preview_top_selector);
            }
        }
        super.reloadPreference();
    }

    public String getKey() {
        return mPreference.getKey();
    }

    @Override
    public boolean isOverridden() {
        return mOverrideValue != null;
    }

    @Override
    public void overrideSettings(final String ... keyvalues) {
        mOverrideValue = null;
        for (int i = 0; i < keyvalues.length; i += 2) {
            String key = keyvalues[i];
            String value = keyvalues[i + 1];
            if (key.equals(getKey())) {
                mOverrideValue = value;
                setEnabled(value == null);
                break;
            }
        }
        reloadPreference();

        if(mPopup instanceof ColorEffectSettingPopup) {
             ((ColorEffectSettingPopup)mPopup).reloadPreference();
        }
    }

    @Override
    public void enableItems(final String ... keyvalues) {
        Log.e(TAG, "Calling enableItems");
        for (int i = 0; i < keyvalues.length; i += 2) {
            String key = keyvalues[i];
            String value = keyvalues[i + 1];
            if (key.equals(getKey())) {
                setEnabled(value == null);
                break;
            }
        }
    }

    @Override
    protected void initializePopup() {
    	if (!mPreference.isAvailable()) {
    		return;
    	}

        updateArrowImagePos();

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) getRootView().findViewById(R.id.frame_layout);
        AbstractSettingPopup popup;
        if (CameraSettings.KEY_VIDEO_COLOR_EFFECT.equals(getKey())
        		|| CameraSettings.KEY_COLOR_EFFECT.equals(getKey())) {
        	ColorEffectSettingPopup colorEffect = (ColorEffectSettingPopup) inflater.inflate(
                    R.layout.color_setting_effect_popup, root, false);
        	colorEffect.initialize(mPreference);
        	colorEffect.setSettingChangedListener(this);
            mPopup = colorEffect;
        } else {
            BasicSettingPopup basic = (BasicSettingPopup) inflater.inflate(
                    R.layout.basic_setting_popup, root, false);
            basic.initialize(mPreference);
            basic.setSettingChangedListener(this);
            mPopup = basic;
        }
        RelativeLayout.LayoutParams rlparams = (RelativeLayout.LayoutParams )mPopup.getLayoutParams();
        if(SecondLevelIndicatorControlBar.mIsLandscape) {
        	if( SecondLevelIndicatorControlBar.mPopupLocate == 1) {
        		rlparams.addRule(RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.TRUE);
        		int basicOffset = (int)getContext().getResources().getDimension(R.dimen.basic_setting_popup_offset);
        		rlparams.topMargin = basicOffset;
        	} else if( SecondLevelIndicatorControlBar.mPopupLocate == 2) {
        		rlparams.addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE);
        	} else if( SecondLevelIndicatorControlBar.mPopupLocate == 3) {
        		rlparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        		if(mPopup instanceof ColorEffectSettingPopup) {
        			int effectOffset = (int)getContext().getResources().getDimension(R.dimen.effect_setting_popup_offset);
        			rlparams.topMargin = effectOffset;
        			rlparams.bottomMargin = effectOffset;
        		}
        	}
        	rlparams.addRule(RelativeLayout.RIGHT_OF, R.id.popup_arrow_container);
        } else {
        	if( SecondLevelIndicatorControlBar.mPopupLocate == 1) {
        		rlparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,RelativeLayout.TRUE);
        		if(mPopup instanceof ColorEffectSettingPopup) {
        			int effectOffset = (int)getContext().getResources().getDimension(R.dimen.effect_setting_popup_offset);
        			rlparams.leftMargin = effectOffset;
        			rlparams.rightMargin = effectOffset;
        		}
        	} else if( SecondLevelIndicatorControlBar.mPopupLocate == 2) {
        		rlparams.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
        	} else if( SecondLevelIndicatorControlBar.mPopupLocate == 3) {
        		rlparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        		int basicOffset = (int)getContext().getResources().getDimension(R.dimen.basic_setting_popup_offset);
        		rlparams.rightMargin = basicOffset;
        	}
        	
        	rlparams.addRule(RelativeLayout.BELOW, R.id.popup_arrow_container);
        }
        
        root.addView(mPopup,rlparams);
    }

    @Override
    public void onSettingChanged() {
        reloadPreference();
        // Dismiss later so the activated state can be updated before dismiss.
        dismissPopupDelayed();
        if (mListener != null) {
            mListener.onSettingChanged();
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	if (!mPreference.isAvailable()) {
    		return true;
    	}
    	
    	return super.onTouchEvent(ev);
    }
}
