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
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.graphics.Bitmap;

import com.android.camera.CameraSettings;
import com.android.camera.IconListPreference;
import com.android.camera.ListPreference;
import com.android.camera.PreferenceGroup;
import com.android.camera.R;
import android.view.View;
import android.util.Log;
import android.util.DisplayMetrics;
import android.app.Activity;
import android.os.Handler;

public class OtherSettingIndicatorButton extends AbstractIndicatorButton implements OtherSettingSubPopup.Listener, OnClickOtherSettinPopupItemListener, RotateLayout.OnLayoutFinishNotify{
    @SuppressWarnings("unused")
    private static final String TAG = "OtherSettingIndicatorButton";
    private PreferenceGroup mPreferenceGroup;
    private String[] mPrefKeys;
    private OtherSettingsPopup.Listener mListener;
    private ListPreference mPreference;
    private String[] mOverrideSettings;
    private String[] mEnableKeyValues;
    private boolean mOverrideSettingsChanged = false;
    private boolean mEnableKeyValuesChanged = false;
    private int mRawX;
    private int mRawY;
    private int mOldOrientation;
    private int mScreenWidth;
    private int mScreenHeight;
    
    int top;
    int bottom;
    int left;
    int right;

    public void setSettingChangedListener(OtherSettingsPopup.Listener listener) {
        mListener = listener;
    }

    public OtherSettingIndicatorButton(Context context, int resId,
            PreferenceGroup preferenceGroup, String[] prefKeys) {
        super(context);
        setImageResource(resId);
        mPreferenceGroup = preferenceGroup;
        mPrefKeys = prefKeys;
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
    }

    @Override
    public void overrideSettings(final String ... keyvalues) {

        mOverrideSettingsChanged = true;
        mOverrideSettings = keyvalues;
        if(mPopup != null) {
            ((OtherSettingsPopup)mPopup).onSceneModeChanged();
            ((OtherSettingsPopup)mPopup).overrideSettings(keyvalues);
        }
    }

    @Override
    public void enableItems(final String ... keyvalues) {
        mEnableKeyValuesChanged = true;
        mEnableKeyValues = keyvalues;
        if(mPopup != null) {
            ((OtherSettingsPopup)mPopup).enableItems(keyvalues);
        }
    }

    @Override
    protected void initializePopup() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) getRootView().findViewById(R.id.frame_layout);

        if(mPopupArrowContainer == null) {
             mPopupArrowContainer = (ViewGroup) getRootView().findViewById(R.id.popup_arrow_container);
        }
        mPopupArrowContainer.removeAllViews();
        
        ImageView arrowImge = new ImageView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Bitmap arrowBmp;
        if(SecondLevelIndicatorControlBar.mIsLandscape) {
            arrowBmp = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.list_triangle_hor);
            params.setMargins(0, SecondLevelIndicatorControlBar.mCurIndicatorButtonMargin + 
                    (SecondLevelIndicatorControlBar.mIconSize - arrowBmp.getHeight())/2, 0, 0);
            arrowImge.setImageBitmap(arrowBmp);
        } else {
            arrowBmp = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.list_triangle);
            params.setMargins(SecondLevelIndicatorControlBar.mCurIndicatorButtonMargin + 
                    (SecondLevelIndicatorControlBar.mIconSize - arrowBmp.getWidth())/2, 0, 0, 0);
            arrowImge.setImageBitmap(arrowBmp);
        }
        mPopupArrowContainer.addView(arrowImge,params);
        
        OtherSettingsPopup popup = (OtherSettingsPopup) inflater.inflate(
                R.layout.other_setting_popup, root, false);
        popup.setSettingChangedListener(mListener);
        popup.initialize(mPreferenceGroup, mPrefKeys);
        
        RelativeLayout.LayoutParams rlparams = (RelativeLayout.LayoutParams )popup.getLayoutParams();
        if(SecondLevelIndicatorControlBar.mIsLandscape) {
            if( SecondLevelIndicatorControlBar.mPopupLocate == 1) {
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.TRUE);
            } else if( SecondLevelIndicatorControlBar.mPopupLocate == 2) {
                rlparams.addRule(RelativeLayout.CENTER_VERTICAL,RelativeLayout.TRUE);
            } else if( SecondLevelIndicatorControlBar.mPopupLocate == 3) {
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
                int otherOffset = (int)getContext().getResources().getDimension(R.dimen.other_setting_popup_offset);
                rlparams.topMargin = otherOffset;
                rlparams.bottomMargin = otherOffset;
            }
        } else {
            if( SecondLevelIndicatorControlBar.mPopupLocate == 1) {
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,RelativeLayout.TRUE);
                int otherOffset = (int)getContext().getResources().getDimension(R.dimen.other_setting_popup_offset);
                rlparams.leftMargin = otherOffset;
                rlparams.rightMargin = otherOffset;
            } else if( SecondLevelIndicatorControlBar.mPopupLocate == 2) {
                rlparams.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
            } else if( SecondLevelIndicatorControlBar.mPopupLocate == 3) {
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
            }
        }
        
        root.addView(popup,rlparams);
        popup.setOnClickOtherSettinPopupItemListener(this);
        mPopup = popup;

        if(mOverrideSettingsChanged) {
            ((OtherSettingsPopup)mPopup).overrideSettings(mOverrideSettings);
            mOverrideSettingsChanged = false;
        }

        if(mEnableKeyValuesChanged) {
            ((OtherSettingsPopup)mPopup).overrideSettings(mEnableKeyValues);
            mEnableKeyValuesChanged = false;
        }
    }
 private void initializeSubPopup(int rawX, int rawY) {
        if(mSubPopup != null) {
            removeSubPopupWindow();
        }
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) getRootView().findViewById(R.id.frame_layout);
        OtherSettingSubPopup subPopup = (OtherSettingSubPopup) inflater.inflate(
                R.layout.other_setting_sub_popup, root, false);
        subPopup.initialize(mPreference);
        subPopup.setSettingChangedListener(this);
       
        mSubPopup = subPopup;
        
        top = mPopup.getTop();
        bottom = mPopup.getBottom();
        left = mPopup.getLeft();
        right = mPopup.getRight();
        
        int settingTitleHeight = (int)getContext().getResources().getDimension(R.dimen.popup_title_frame_min_height);
        int settingRowHeight = (int)getContext().getResources().getDimension(R.dimen.setting_row_height);
        int subPopupHeight = ((OtherSettingSubPopup)mSubPopup).getPrefItemSize() * settingRowHeight + settingTitleHeight;
        int subPopupWidth = (int)getContext().getResources().getDimension(R.dimen.setting_popup_window_width);
        
        int pad = (int)getContext().getResources().getDimension(R.dimen.setting_sub_popup_pad);
        RelativeLayout.LayoutParams rlparams = (RelativeLayout.LayoutParams )mSubPopup.getLayoutParams();
        if(SecondLevelIndicatorControlBar.mIsLandscape) {
            if(mOrientation == 90) {
                int popupHeight= right - left;
                int marginTop = pad;
                int marginBottom = pad;
                int marginLeft = left + pad;
                int marginRight = mScreenWidth - right + pad;

                if(subPopupHeight < popupHeight - pad * 2) {
                    if(rawX - subPopupHeight/2 < left + pad) {
                        marginLeft = left + pad;
                        marginRight = mScreenWidth - marginLeft - subPopupHeight;
                    } else if(rawX + subPopupHeight/2 > right - pad) {
                        marginRight = mScreenWidth - right + pad;
                        marginLeft = mScreenWidth - marginRight - subPopupHeight;
                    } else {
                        marginLeft = rawX - subPopupHeight/2;
                        marginRight = mScreenWidth - marginLeft - subPopupHeight;
                    }
                    if(marginLeft < pad) marginLeft = pad;
                    if(marginRight < pad) marginRight = pad;
                    marginLeft -= pad/3;
                    marginRight -= pad/3;
                }

                rlparams.setMargins(marginLeft,marginTop,marginRight,marginBottom);
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,RelativeLayout.TRUE);
            } else if(mOrientation == 180) {
                int popupHeight= bottom - top;
                int marginTop = pad;
                int marginBottom = pad;
                int marginLeft = right - left - 3*subPopupWidth/5;
                if(subPopupHeight < popupHeight - pad * 2) {

                    if(rawY + subPopupHeight/2 > bottom - pad) {
                        marginBottom = pad;
                        marginTop = mScreenHeight - (marginBottom + subPopupHeight);
                    } else if(rawY - subPopupHeight/2 < top + pad) {
                        marginTop = top + pad;
                        marginBottom = mScreenHeight - marginTop - subPopupHeight;
                    } else {
                        marginBottom = bottom - rawY - subPopupHeight/2;
                        marginTop = mScreenHeight - (marginBottom + subPopupHeight);
                    }

                    if(marginBottom < pad) marginBottom = pad;
                    if(marginTop < pad) marginTop = pad;
                    marginBottom -= pad/3;
                    marginTop -= pad/3;
                }
                rlparams.setMargins(marginLeft,marginTop,pad,marginBottom);
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
            } else if(mOrientation == 270) {
                int popupHeight= right - left;
                int marginTop = pad;
                int marginBottom = pad;
                int marginLeft = left + pad;
                int marginRight = mScreenWidth - right + pad;

                if(subPopupHeight < popupHeight - pad * 2) {

                    if(rawX - subPopupHeight/2 < left + pad) {
                        marginLeft = left + pad;
                        marginRight = mScreenWidth - marginLeft - subPopupHeight;
                    } else if(rawX + subPopupHeight/2 > right - pad) {
                        marginRight = mScreenWidth - right + pad;
                        marginLeft = mScreenWidth - marginRight - subPopupHeight;
                    } else {
                        marginLeft = rawX - subPopupHeight/2;
                        marginRight = mScreenWidth - marginLeft - subPopupHeight;
                    }
                    if(marginLeft < pad) marginLeft = pad;
                    if(marginRight < pad) marginRight = pad;
                    marginLeft -= pad/3;
                    marginRight -= pad/3;
                }
                rlparams.setMargins(marginLeft,marginTop,marginRight,marginBottom);
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,RelativeLayout.TRUE);
            } else if(mOrientation % 360 == 0) {
                int popupHeight= bottom - top;
                int marginTop = pad;
                int marginBottom = pad;
                int marginLeft = right - left - 3*subPopupWidth/5;
                if(subPopupHeight < popupHeight - pad * 2) {

                    if(rawY - subPopupHeight/2 < top + pad) {
                        marginTop = top + pad;
                        marginBottom = mScreenHeight - (marginTop + subPopupHeight);
                    } else if(rawY + subPopupHeight/2 > bottom - pad) {
                        marginBottom = pad;
                        marginTop = mScreenHeight - (marginBottom + subPopupHeight);
                    } else {
                        marginBottom = bottom - rawY - subPopupHeight/2;
                        marginTop = mScreenHeight - (marginBottom + subPopupHeight);
                    }
                    
                    if(marginBottom < pad) marginBottom = pad;
                    if(marginTop < pad) marginTop = pad;
                    marginBottom -= pad/3;
                    marginTop -= pad/3;
                }

                rlparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
                rlparams.setMargins(marginLeft,marginTop,pad,marginBottom);
            }

            rlparams.addRule(RelativeLayout.RIGHT_OF, R.id.popup_arrow_container);
        } else {
            if(mOrientation == 0) {
                int popupHeight = bottom - top;
                int marginBottom = mScreenHeight - bottom + pad;
                int marginTop = pad;
                if(subPopupHeight < popupHeight - pad * 2) {
                    if(rawY - subPopupHeight/2 < top + pad) {
                        marginTop = pad;
                        marginBottom = 0;
                    } else if(rawY + subPopupHeight/2 > bottom - pad) {
                    	int dY = rawY + subPopupHeight/2 - (bottom - pad);
                    	if(dY > settingRowHeight / 2) dY = settingRowHeight/2;
                        marginBottom = pad - dY;
                        marginTop = popupHeight - pad - subPopupHeight + dY;
                    } else {
                        marginTop = rawY - top - subPopupHeight/2;
                        marginBottom = 0;
                    }
                    if(marginBottom < pad) marginBottom = pad;
                    if(marginTop < pad) marginTop = pad;
                    marginBottom -= pad/3;
                    marginTop -= pad/3;
                } else {
                	marginTop += settingRowHeight/2;
                	marginBottom -= settingRowHeight/2;
                }
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
                rlparams.setMargins(0,marginTop,pad,marginBottom);
            } else if(mOrientation == 90) {
                int popupHeight= right - left;
                int marginBottom  = pad;
                int marginTop = bottom - top -3 * subPopupWidth/4;
                int marginLeft = pad;
                int marginRight = mScreenWidth - right + pad;
                if(subPopupHeight < popupHeight - pad * 2) {
                    if(rawX - subPopupHeight/2  < left + pad) {
                        marginLeft = left + pad;
                        marginRight = popupHeight - marginLeft - subPopupHeight;
                    } else if(rawX + subPopupHeight/2 + pad > right) {
                        marginRight = pad;
                        marginLeft = popupHeight - marginRight - subPopupHeight;
                    } else {
                        marginLeft = rawX - subPopupHeight/2;
                        marginRight = popupHeight - marginLeft - subPopupHeight;
                    }

                    if(marginLeft < pad) marginLeft = pad;
                    if(marginRight < pad) marginRight = pad;
                    marginLeft -= pad/3;
                    marginRight -= pad/3;
                }
                rlparams.setMargins(marginLeft,marginTop,marginRight,marginBottom);
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,RelativeLayout.TRUE);
            } else if(mOrientation == 180) {    
                int popupHeight = bottom - top;
                int marginBottom = mScreenHeight - bottom + pad;
                int marginTop = pad;

                if(subPopupHeight < popupHeight - pad * 2) {
                    if(rawY - subPopupHeight/2 < top + pad) {
                        marginTop = pad;
                        marginBottom = popupHeight - marginTop - subPopupHeight;
                    } else if(rawY + subPopupHeight/2 > bottom - pad) {
                        marginBottom = pad;
                        marginTop = popupHeight - marginBottom - subPopupHeight;
                    } else {
                        marginTop = rawY - top - subPopupHeight/2;
                        marginBottom = popupHeight - marginTop - subPopupHeight;
                    }
                    if(marginTop < pad) marginTop = pad;
                    if(marginBottom < pad) marginBottom = pad;
                    marginBottom -= pad/3;
                    marginTop -= pad/3;
                }
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
                rlparams.setMargins(pad,marginTop,pad,marginBottom);
            } else if(mOrientation == 270) {
                int popupHeight= right - left;
                int marginBottom  = pad;
                int marginTop = bottom - top - subPopupWidth/4;
                int marginRight = pad;
                int marginLeft = pad;
                if(subPopupHeight < popupHeight - pad * 2) {
                    if(rawX - subPopupHeight/2 < left + pad) {
                        marginLeft = left + pad;
                        marginRight = popupHeight - marginLeft - subPopupHeight;
                    } else if(rawX + subPopupHeight/2 > right - pad) {
                        marginRight = pad;
                        marginLeft = popupHeight - marginRight - subPopupHeight;
                    } else {
                        marginLeft = rawX - left - subPopupHeight/2;
                        marginRight = popupHeight - marginLeft - subPopupHeight;
                    }
                    
                    if(marginLeft < pad) marginLeft = pad;
                    if(marginRight < pad) marginRight = pad;
                    marginLeft -= pad/3;
                    marginRight -= pad/3;
                }
                rlparams.setMargins(marginLeft,marginTop,marginRight,marginBottom);
                rlparams.addRule(RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.TRUE);
            }    
            rlparams.addRule(RelativeLayout.BELOW, R.id.popup_arrow_container);
        }
        root.addView(mSubPopup,rlparams);
        showSubPopup();
    }
    
    @Override
    public void onClickOtherSettingPopupItem(int rawX, int rawY,String key) {
        if(key == null || key.length() == 0) return;
        mRawX = rawX;
        mRawY = rawY;
        mOldOrientation = mOrientation;
        mPreference = mPreferenceGroup.findPreference(key);
        initializeSubPopup(rawX,rawY);
    }

    @Override
    public void onSettingChanged() {
        dismissSubPopup();
        if(mPopup != null) {
            mPopup.reloadPreference();
        }
        if(mListener != null) {
            mListener.onSettingChanged();
        }
    }
    
    @Override
    public void setOrientation(int orientation, boolean animation) {
        if(mSubPopup != null) {
            mPopup.setmLayoutFinishNotify(this);
        }
        super.setOrientation(orientation,animation);
    }
        
   public void onLayoutFinishNotify(int orientation) {
       
       new Handler().post(new Runnable() {
           public void run() {
                  if(mSubPopup != null) {
                    int newRawX = 0;
                    int newRawY = 0;

                    int orientation = mOrientation;
                    if(SecondLevelIndicatorControlBar.mIsLandscape) {
                        if(mOldOrientation == 0) {
                            if(orientation == 0) {
                                newRawY = mRawY;
                            } else if(orientation == 90) {
                                newRawX = left + mRawY;
                            } else if(orientation == 180){
                                newRawY = mScreenHeight - mRawY;
                            } else if(orientation == 270){
                                newRawX = right - mRawY;
                            }
                        } else if(mOldOrientation == 90) {
                            if(orientation == 0) {
                                newRawY = mRawX - left;
                            } else if(orientation == 90){
                                newRawX = mRawX;
                            } else if(orientation == 180){
                                newRawY = mScreenHeight - (mRawX - left);
                            } else if(orientation == 270){
                                newRawX = right + (mRawX - left);
                            }
                        } else if(mOldOrientation == 180) {
                            if(orientation == 0) {
                                newRawY = mScreenHeight - mRawY;
                            } else if(orientation == 90){
                                newRawX = left + (mScreenHeight - mRawY);
                            } else if(orientation == 180){
                                newRawY = mRawY;
                            } else if(orientation == 270){
                                newRawX = right - (mScreenHeight - mRawY);
                            }
                        } else if(mOldOrientation == 270) {
                            if(orientation == 0) {
                                newRawY = right - mRawX;
                            } else if(orientation == 90){
                                newRawX = (right - mRawX);
                            } else if(orientation == 180){
                                newRawY = mScreenHeight - (right - mRawX);
                            } else if(orientation == 270){
                                newRawX = mRawX;
                            }
                        }
                    } else {
                        if(mOldOrientation == 0) {
                            if(orientation == 0) {
                                newRawY = mRawY;
                            } else if(orientation == 90) {
                                newRawX = mRawY - top;
                            } else if(orientation == 180){
                                newRawY = top + bottom - mRawY;
                            } else if(orientation == 270){
                                newRawX = mScreenWidth - (mRawY - top);
                            }
                        } else if(mOldOrientation == 90) {
                            if(orientation == 0) {
                                newRawY = top + mRawX;
                            } else if(orientation == 90){
                                newRawX = mRawX;
                            } else if(orientation == 180){
                                newRawY = bottom - mRawX;
                            } else if(orientation == 270){
                                newRawX = right - mRawX;
                            }
                        } else if(mOldOrientation == 180) {
                            if(orientation == 0) {
                                newRawY = top + bottom - mRawY;
                            } else if(orientation == 90){
                                newRawX = bottom - mRawY;
                            } else if(orientation == 180){
                                newRawY = mRawY;
                            } else if(orientation == 270){
                                newRawX = mScreenWidth - (bottom - mRawY);
                            }
                        } else if(mOldOrientation == 270) {
                            if(orientation == 0) {
                                newRawY = top + (right - mRawX);
                            } else if(orientation == 90){
                                newRawX = (right - mRawX);
                            } else if(orientation == 180){
                                newRawY = bottom - (right - mRawX);
                            } else if(orientation == 270){
                                newRawX = mRawX;
                            }
                        }
                        
                    }
                    initializeSubPopup(newRawX,newRawY);
                    
                    if (mSubPopup != null) {
                        mSubPopup.setOrientation(orientation, true);
                    }
                  }
           }
       });
    }
   
   public void updateIntelligenceKeys(String[] intelligenceKeys,boolean toAdd) {
	   ((OtherSettingsPopup)mPopup).updateIntelligenceKeys(intelligenceKeys,toAdd);
   }
}
