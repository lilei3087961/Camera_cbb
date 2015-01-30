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
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import com.android.camera.IconListPreference;
import com.android.camera.PreferenceGroup;
import com.android.camera.ListPreference;
import com.android.camera.CameraSettings;
import com.android.camera.R;
import com.android.camera.Util;
import com.android.camera.VideoCamera;

import java.util.ArrayList;
import java.util.HashMap;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
/**
 * A view that contains camera setting indicators which are spread over a
 * vertical bar in preview frame.
 */
public class SecondLevelIndicatorControlBar extends IndicatorControl implements
        View.OnClickListener, AbstractIndicatorButton.IndicatorChangeListener {
    @SuppressWarnings("unused")
    private static final String TAG = "SecondLevelIndicatorControlBar";
    //private static int ICON_SPACING = Util.dpToPixel(6);
    //private View mCloseIcon;
    //private View mDivider; // the divider line
    private View mPopupedIndicator;
    int mOrientation = 0;
    int mSelectedIndex = -1;
    // There are some views in the ViewGroup before adding the indicator buttons,
    // such as Close icon, divider line and the highlight bar, we need to
    // remember the count of the non-indicator buttons for getTouchViewIndex().
    int mNonIndicatorButtonCount;
    private static int mIconSpacing;
    //1 left, 2 center,3 right
    public static int mPopupLocate;
    public static int mCurIndicatorButtonMargin;
    public static boolean mIsLandscape;
    public static int mIconSize;
    private Context mContext;
    
    public SecondLevelIndicatorControlBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        //mDivider = findViewById(R.id.divider);
        //mCloseIcon = findViewById(R.id.back_to_first_level);
        //mCloseIcon.setOnClickListener(this);
        mNonIndicatorButtonCount = getChildCount();
        mPopupLocate = 1;
        mCurIndicatorButtonMargin = 37;
        
        BitmapDrawable bd = (BitmapDrawable)mContext.getResources().getDrawable(
        		R.drawable.ic_switch_photo_facing_holo_light);;
    	Bitmap bmp = bd.getBitmap();
    	mIconSize = bmp.getWidth();
    };

    public void initialize(Context context, PreferenceGroup group,
            String[] keys, HashMap otherSettingKeys) {

		Log.i(TAG,"zzw==initialize()");
        setPreferenceGroup(group);

        // Remove the original setting indicators. This happens when switching
        // between front and back cameras.
        int count = getChildCount() - mNonIndicatorButtonCount;

        //when HFR switch off to 60 or swithc 60 to off, only update preference and don't update UI
	   if (VideoCamera.bFlagHFRUpdateUI == false) {
            if (count > 0) removeControls(mNonIndicatorButtonCount, count);
            if(count > 0) PopupManager.getInstance(context).clearOtherPopupShowedListener();
            initializeCameraPicker();
            addControls(keys, otherSettingKeys);
        } else {
        	VideoCamera.bFlagHFRUpdateUI = false;
            updateControls(mNonIndicatorButtonCount, count, keys, otherSettingKeys);
        }
        if (mOrientation != 0) setOrientation(mOrientation, false);

        // Do not grey out the icons when taking a picture.
        setupFilter(false);
    }

    @Override
    public void onClick(View view) {
        dismissSettingPopup();
    }

    private int getTouchViewIndex(int touchPosition, boolean isLandscape) {
        // Calculate if the touch event is on the indicator buttons.
        int count = getChildCount();
        if (count == mNonIndicatorButtonCount) return -1;
        // The baseline will be the first indicator button's top minus spacing.
        View firstIndicatorButton = getChildAt(mNonIndicatorButtonCount);
        int realIdx = 0;
        if (isLandscape) {
        	if (touchPosition < firstIndicatorButton.getTop()) return -1;
        	int baseline = firstIndicatorButton.getBottom()+ mIconSpacing/2;
        	
        	if(touchPosition < baseline){
        		realIdx = 0;
        	} else {
        		int iconHeight = firstIndicatorButton.getMeasuredHeight();
        		int buttonRange = iconHeight + mIconSpacing;
        		int num = (touchPosition - baseline) / buttonRange;
        		
        		realIdx = ((touchPosition - baseline) % buttonRange) > 0 ? num + 1 : num;
        	}
        	
			int childIdx = realIdx + mNonIndicatorButtonCount;
			if(childIdx >= count) {
				childIdx = count -1;
			}

			if(childIdx < 0) {
				childIdx = 0;
			}
        	mCurIndicatorButtonMargin = getChildAt(childIdx).getTop();
        	
            int buttonNum = count - mNonIndicatorButtonCount;
            int mid = buttonNum/2 + buttonNum%2;
            if((realIdx + 1 == mid)) {
            	mPopupLocate = 2;        	
            } else if (realIdx +1 > mid){
            	mPopupLocate = 3;
            } else {
            	mPopupLocate = 1;
            }
        	
        } else {
        	if (touchPosition > firstIndicatorButton.getRight()) return -1;
        	int baseline = firstIndicatorButton.getLeft() - mIconSpacing/2;
        	
        	if(touchPosition > baseline){
        		realIdx = 0;
        	} else {
        		int iconWidth = firstIndicatorButton.getMeasuredWidth();
        		int buttonRange = iconWidth + mIconSpacing;
        		int num = (baseline - touchPosition) / buttonRange;
        		
        		realIdx = ((baseline - touchPosition) % buttonRange) > 0 ? num + 1 : num;
        	}
        	
			int childIdx = realIdx + mNonIndicatorButtonCount;
			if(childIdx >= count) {
				childIdx = count -1;
			}

			if(childIdx < 0) {
				childIdx = 0;
			}
        	mCurIndicatorButtonMargin = getChildAt(childIdx).getLeft();
        	
            int buttonNum = count - mNonIndicatorButtonCount;
            int mid = buttonNum/2 + buttonNum%2;
            if((realIdx + 1 == mid)) {
            	mPopupLocate = 2;        	
            } else if (realIdx +1 > mid){
            	mPopupLocate = 1;
            } else {
            	mPopupLocate = 3;
            }
        }
        return realIdx + mNonIndicatorButtonCount; 
    }

    private void dispatchRelativeTouchEvent(View view, MotionEvent event) {
        event.offsetLocation(-view.getLeft(), -view.getTop());
        view.dispatchTouchEvent(event);
        event.offsetLocation(view.getLeft(), view.getTop());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!onFilterTouchEventForSecurity(event)) return false;

        int action = event.getAction();
        if (!isEnabled()) return false;

        int index = 0;
        mIsLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        // the X (Y) of touch point for portrait (landscape) orientation
        int touchPosition = (int) (mIsLandscape ? event.getY() : event.getX());
        // second-level indicator control bar width (height) for portrait
        // (landscape) orientation
        int controlBarLength = mIsLandscape ? getHeight() : getWidth();
        if (controlBarLength == 0) return false; // the event is sent before onMeasure()
        if (touchPosition >= controlBarLength) {
            touchPosition = controlBarLength - 1;
        }
        index = getTouchViewIndex(touchPosition, mIsLandscape);

        if(index < 0)
        	return true;
        // Cancel the previous target if we moved out of it
        if ((mSelectedIndex != -1) && (index != mSelectedIndex)) {
            View p = getChildAt(mSelectedIndex);
            if (p != null) {
                int oldAction = event.getAction();
                event.setAction(MotionEvent.ACTION_CANCEL);
                dispatchRelativeTouchEvent(p, event);
                event.setAction(oldAction);

                if (p instanceof AbstractIndicatorButton) {
                    AbstractIndicatorButton b = (AbstractIndicatorButton) p;
                    b.dismissPopup();               
                }
            }
        }

        // Send event to the target
        View v = getChildAt(index);
        if (v == null) return true;
        
        //START: add by xiongzhu for cbb 
        if (v instanceof CameraPicker) {
			CameraPicker cameraPicker = (CameraPicker) v;
			if (event.getAction() == MotionEvent.ACTION_UP) {
				dismissSettingPopup();
                cameraPicker.changeCameraId();
		    }
	    }
	    //END: add by xiongzhu for cbb
	    
        // Change MOVE to DOWN if this is a new target
        if (mSelectedIndex != index && action == MotionEvent.ACTION_MOVE) {
            event.setAction(MotionEvent.ACTION_DOWN);
        }
        dispatchRelativeTouchEvent(v, event);
        mSelectedIndex = index;
        return true;
    }

    @Override
    public IndicatorButton addIndicator(Context context, IconListPreference pref) {
        IndicatorButton b = super.addIndicator(context, pref);
        //b.setBackgroundResource(R.drawable.bg_pressed);
        b.setIndicatorChangeListener(this);
        return b;
    }

    @Override
    public OtherSettingIndicatorButton addOtherSettingIndicator(Context context,
            int resId, String[] keys) {
        OtherSettingIndicatorButton b =
                super.addOtherSettingIndicator(context, resId, keys);
        //b.setBackgroundResource(R.drawable.bg_pressed);
        b.setIndicatorChangeListener(this);
        b.setId(R.id.other_setting_indicator);
        return b;
    }

    @Override
    public void onShowIndicator(View view, boolean showed) {
        // Ignore those events if not current popup.
        if (!showed && (mPopupedIndicator != view)) return;
        mPopupedIndicator = (showed ? view : null);
        // Show or dismiss the side indicator highlight.
        requestLayout();
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        mOrientation = orientation;
        super.setOrientation(orientation, animation);
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();
        if (count == 0) return;
        int width = right - left;
        int height = bottom - top;

        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            int paddingTop = getPaddingTop();
            int paddingBottom = getPaddingBottom();
            int offset = paddingTop;
            int endY = height-paddingBottom;
            mIconSpacing = (endY - paddingTop - count * mIconSize)/(count-1);
             
            for (int i = mNonIndicatorButtonCount; i < count; ++i) {
                getChildAt(i).layout(0, offset, width, offset + mIconSize);
                offset += mIconSpacing + mIconSize;
           }
        } else {
            int paddingLeft = getPaddingLeft();
            int paddingRight = getPaddingRight();
            
			 int offset = paddingLeft;
            int endX = width - paddingRight;
            mIconSpacing = (endX - paddingLeft - count *mIconSize)/(count-1);

            for (int i = count -1; i >= mNonIndicatorButtonCount; --i) {
                getChildAt(i).layout(offset, 0, offset + mIconSize, height);
                offset += mIconSpacing + mIconSize;
            }
        }
   }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        //if (mCurrentMode == MODE_VIDEO) {
            //mCloseIcon.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
       // }
       // mCloseIcon.setEnabled(enabled);
    }
    
    public void showOtherSettingPopup() {
    	 for (AbstractIndicatorButton v: mIndicators) {
             if (v instanceof OtherSettingIndicatorButton) {
                v.showPopup();
             }
         }
    }
    
    public void updateOtherSettingsIntelligenceKeys(String[] intelligenceKeys,boolean toAdd) {
    	for (AbstractIndicatorButton v: mIndicators) {
            if (v instanceof OtherSettingIndicatorButton) {
               ((OtherSettingIndicatorButton)v).updateIntelligenceKeys(intelligenceKeys,toAdd);
               break;
            }
        }
    }
}
