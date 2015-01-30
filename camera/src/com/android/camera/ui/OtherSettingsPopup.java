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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.CameraSettings;
import com.android.camera.ListPreference;
import com.android.camera.PreferenceGroup;
import com.android.camera.R;
import android.hardware.Camera.Parameters;
import java.util.ArrayList;
import android.util.Log;

/* A popup window that contains several camera settings. */
public class OtherSettingsPopup extends AbstractSettingPopup
        implements InLineSettingItem.Listener,
        AdapterView.OnItemClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "OtherSettingsPopup";

    private Listener mListener;
    private ArrayList<ListPreference> mListItem = new ArrayList<ListPreference>();

    protected ViewGroup mSettingList;
    private ArrayAdapter<ListPreference> mListItemAdapter;
    private PreferenceGroup mPreferenceGroup;
    private OnClickOtherSettinPopupItemListener mOnClickOtherSettinPopupItemListener;
    private int mRawX;
    private int mRawY;
    private int mCurItemIdx = -1;
    
    static public interface Listener {
        public void onSettingChanged();
        public void onRestorePreferencesClicked();
    }
    
    public void restoreNormalTextColor() {
        mCurItemIdx = -1;
        mListItemAdapter.notifyDataSetChanged();
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mRawX = (int)event.getRawX();
        mRawY = (int)event.getRawY();
        return super.dispatchTouchEvent(event);
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTitle = (TextView) findViewById(R.id.title);
        mSettingList = (ViewGroup) findViewById(R.id.settingList);
    }

    private class OtherSettingsAdapter extends ArrayAdapter<ListPreference> {
        LayoutInflater mInflater;

        OtherSettingsAdapter() {
            super(OtherSettingsPopup.this.getContext(), 0, mListItem);
            mInflater = LayoutInflater.from(getContext());
        }

        private int getSettingLayoutId(ListPreference pref) {
            // If the preference is null, it will be the only item , i.e.
            // 'Restore setting' in the popup window.
            if ( pref == null) return R.layout.in_line_setting_restore;
            if (CameraSettings.KEY_VERSION_NUMBER.equals(pref.getKey())) {
                return R.layout.in_line_version_number;
            }

            if (CameraSettings.KEY_RECORD_LOCATION.equals(pref.getKey())
                    || CameraSettings.KEY_VIDEO_RECORD_LOCATION.equals(pref.getKey())
                    || CameraSettings.KEY_SILENT_MODE.equals(pref.getKey())) {
                return R.layout.in_line_setting_switch;
            }
            return R.layout.in_line_setting_knob;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListPreference pref = mListItem.get(position);
            
            InLineSettingItem view = (InLineSettingItem)convertView;
            int viewLayoutId = getSettingLayoutId(pref);
            if (view == null || viewLayoutId != (Integer) convertView.getId()) {
                view = (InLineSettingItem)
                        mInflater.inflate(viewLayoutId, parent, false);
                view.setId(viewLayoutId);
            }
            
            view.initialize(pref);
            view.setSettingChangedListener(OtherSettingsPopup.this);
            if(pref != null) {
                view.setAlpha(pref.isEnable() ? 1.0f : 0.4f);
                view.setSelected(mCurItemIdx == position ? true:false);

                if (mOverrideValues != null && convertView != null) {
                    for (int i = 0; i < mOverrideValues.length; i += 2) {
                        String key = mOverrideValues[i];
                        String value = mOverrideValues[i + 1];
                        if (pref != null && key.equals(pref.getKey())) {
                            ((InLineSettingItem)convertView).overrideSettings(value);
                            break;
                        }
                    }
                }
            }
            
            return view;
        }
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    public OtherSettingsPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(PreferenceGroup group, String[] keys) {
        mPreferenceGroup = group;
        // Prepare the setting items.
        boolean isSceneAutoMode = false;
        ListPreference sceneModePref = mPreferenceGroup
                .findPreference(CameraSettings.KEY_SCENE_MODE);
        if (sceneModePref != null
                && sceneModePref.getValue().equals(Parameters.SCENE_MODE_AUTO)) {
            isSceneAutoMode = true;
        }
        ListPreference picModePref = mPreferenceGroup
                .findPreference(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE);
        if (picModePref != null
                && picModePref.getValue().equals(Parameters.SCENE_MODE_HDR)) {
            isSceneAutoMode = false;
        }
        boolean isAntiShakeMode = false;
        if(picModePref != null) {
            isAntiShakeMode = getContext().getString( R.string.pref_camera_takepicture_mode_value_anti_shake).equals(picModePref.getValue());
        }
         for (int i = 0; i < keys.length; ++i) {
            ListPreference pref = group.findPreference(keys[i]);
            if(pref == null)
                continue;
            if (isSceneRelativePref(pref)) {
                if(mPreferenceGroup.findPreference(CameraSettings.KEY_SCENE_MODE) != null) {
                    if (isSceneAutoMode == false) {
                        pref.setEnable(false);
                    } else {
                        pref.setEnable(true);
                        if(isAntiShakeMode) {
                            if(pref.getKey().equals(CameraSettings.KEY_ISO)) {
                                pref.setEnable(false);
                            }
                        }
                    }
                }
            }

            mListItem.add(pref);
        }
        // Prepare the restore setting line.
        mListItem.add(null);

        mListItemAdapter = new OtherSettingsAdapter();
        ((ListView) mSettingList).setAdapter(mListItemAdapter);
        ((ListView) mSettingList).setOnItemClickListener(this);
        ((ListView) mSettingList).setSelector(android.R.color.transparent);
    }

    public void updateIntelligenceKeys(String[] intelligenceKeys,boolean toAdd) {
    	if(mListItem == null || intelligenceKeys == null) return;
    	int size = mListItem.size();
    	if(size <= 1) return;

    	if(toAdd) {
    		for (int i = 0; i < intelligenceKeys.length; ++i) {
    			ListPreference pref = mPreferenceGroup.findPreference(intelligenceKeys[i]);
    			if(pref == null)
    				continue;
    			mListItem.add(size - 1,pref);
    			size++;
    		}
    	} else {
    		int k = size - 1;
    		for(int i = intelligenceKeys.length -1; i >=0; --i) {
    			for (int j = k; j >= 0; --j) {
    				ListPreference pref = mListItem.get(j);
    				if(pref == null) continue;
    				if(intelligenceKeys[i].equals(pref.getKey())) {
    					mListItem.remove(j);
    					k = j;
    					break;
    				}
    			}
    		}
    	}
    	
    	if(mListItemAdapter != null) {
            mListItemAdapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onSettingChanged() {
        onSceneModeChanged();
        if (mListener != null) {
            mListener.onSettingChanged();
        }
    }

    public void onSceneModeChanged() {
       boolean isSceneAutoMode = false;
       
        ListPreference sceneModePref = mPreferenceGroup
                .findPreference(CameraSettings.KEY_SCENE_MODE);
        if (sceneModePref != null
                && sceneModePref.getValue().equals(Parameters.SCENE_MODE_AUTO)) {
            isSceneAutoMode = true;
        }
        ListPreference picModePref = mPreferenceGroup
                .findPreference(CameraSettings.KEY_CAMERA_TAKEPICTURE_MODE);
        if (picModePref != null
                && picModePref.getValue().equals(Parameters.SCENE_MODE_HDR)) {
            isSceneAutoMode = false;
        }
        boolean isAntiShakeMode = false;
        if(picModePref != null) {
            isAntiShakeMode = getContext().getString( R.string.pref_camera_takepicture_mode_value_anti_shake).equals(picModePref.getValue());
        }
        for (int i = 0; i < mListItem.size(); i++) {
            ListPreference pref = mListItem.get(i);
            if (pref == null)
                continue;
            if (isSceneRelativePref(pref)) {
                if(mPreferenceGroup.findPreference(CameraSettings.KEY_SCENE_MODE) != null) {
                    if (isSceneAutoMode == false) {
                        pref.setEnable(false);
                    } else {
                        pref.setEnable(true);
                        if(isAntiShakeMode) {
                            if(pref.getKey().equals(CameraSettings.KEY_ISO)) {
                                pref.setEnable(false);
                            }
                        }
                    }
                }
            }
        }
        if(mListItemAdapter != null) {
            mListItemAdapter.notifyDataSetChanged();
        }
  }
    // Scene mode can override other camera settings (ex: flash mode).
    public void overrideSettings(final String ... keyvalues) {
 //       int count = mSettingList.getChildCount();

//        if (0 == count) {
            mOverrideValues = keyvalues;
 //           return;
 //       }
    }

        // Enable/Disable the items.
    public void enableItems(final String ... keyvalues) {
        int count = mSettingList.getChildCount();
        for (int i = 0; i < keyvalues.length; i += 2) {
            String key = keyvalues[i];
            String value = keyvalues[i + 1];
            for (int j = 0; j < count; j++) {
                ListPreference pref = (ListPreference) mListItem.get(j);
                if (pref != null && key.equals(pref.getKey())) {
                    InLineSettingItem settingItem =
                            (InLineSettingItem) mSettingList.getChildAt(j);
                    if ("true".equals(value) || "TRUE".equals(value)) {
                        settingItem.setEnabled(true);
                        settingItem.setVisibility(View.VISIBLE);
                    } else {
                        settingItem.setEnabled(false);
                        settingItem.setVisibility(View.GONE);
                    }
                }
            }
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        //only used for test
        if(mListItem.get(position) != null
                && (mListItem.get(position).getKey().equals("pref_versionnumber_key")
                		|| mListItem.get(position).getKey().equals(CameraSettings.KEY_RECORD_LOCATION)
                		|| mListItem.get(position).getKey().equals(CameraSettings.KEY_VIDEO_RECORD_LOCATION)
                		|| mListItem.get(position).getKey().equals(CameraSettings.KEY_SILENT_MODE))) return;
        if ((position == mListItem.size() - 1) && (mListener != null)) {
            mListener.onRestorePreferencesClicked();
            return;
        }
        if(!mListItem.get(position).isEnable()) {
            return;
        }
        mCurItemIdx = position;

        mListItemAdapter.notifyDataSetChanged();
        if(mOnClickOtherSettinPopupItemListener != null) {
            mOnClickOtherSettinPopupItemListener.onClickOtherSettingPopupItem(
                    mRawX, mRawY, mListItem.get(position).getKey());
        }
    }

    @Override
    public void reloadPreference() {
        // 1. ChildCount is the max visible number of children, and
        //    it is less or equal count of Arrayadapter (mListItem).
        //    So this code will cause orderless of ListView.
        // 2. The function of this code has been implemented in getView
        //    of OtherSettingsAdapter, and we need to disalbe this code.
        //
         int count = mSettingList.getChildCount();
         for (int i = 0; i < count; i++) {
             InLineSettingItem settingItem =
                     (InLineSettingItem) mSettingList.getChildAt(i);
             settingItem.reloadPreference();
         }
    }
    public void setOnClickOtherSettinPopupItemListener(
            OnClickOtherSettinPopupItemListener clickOtherSettinPopupItemListener) {
        mOnClickOtherSettinPopupItemListener = clickOtherSettinPopupItemListener;
    }
    
    private boolean isSceneRelativePref(ListPreference pref) {
        return (pref.getKey().equals(CameraSettings.KEY_WHITE_BALANCE) ||
        pref.getKey().equals(CameraSettings.KEY_FOCUS_MODE) ||
        pref.getKey().equals(CameraSettings.KEY_EXPOSURE) ||
        pref.getKey().equals(CameraSettings.KEY_TOUCH_AF_AEC) ||
        pref.getKey().equals(CameraSettings.KEY_AUTOEXPOSURE) ||
        pref.getKey().equals(CameraSettings.KEY_ANTIBANDING) ||
        pref.getKey().equals(CameraSettings.KEY_ISO) ||
        pref.getKey().equals(CameraSettings.KEY_SATURATION) ||
        pref.getKey().equals(CameraSettings.KEY_CONTRAST) ||
        pref.getKey().equals(CameraSettings.KEY_SHARPNESS));
    }
}
