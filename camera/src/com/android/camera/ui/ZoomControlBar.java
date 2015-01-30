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
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.android.camera.R;
import com.android.camera.Util;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * A view that contains camera zoom control and its layout.
 */
public class ZoomControlBar extends ZoomControl {
    @SuppressWarnings("unused")
    private static final String TAG = "ZoomControlBar";
    private static final int THRESHOLD_FIRST_MOVE = Util.dpToPixel(10); // pixels
    // Space between indicator icon and the zoom-in/out icon.
    private static final int ICON_SPACING = Util.dpToPixel(12);

    private View mBar;
    private boolean mStartChanging;
    private int mSliderPosition = 0;
    private int mSliderLength;
    // The width of the zoom control bar (including the '+', '-' icons and the
    // slider bar) for phone in portrait orientation, or the height of that
    // for phone in landscape orientation.
    private int mSize;
    // The width of the '+' icon (the same as '-' icon) for phone in portrait
    // orientation, or the height of that for phone in landscape orientation.
    private int mIconSize;
    // mIconSize + padding
    private int mTotalIconSize;
    // save the position of slider.
    private int saveSliderPosition = 0;
    
    private int mBarSize;
    private View mZoomProgressView;

    public ZoomControlBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBar = new View(context);
        mBar.setBackgroundResource(R.drawable.zoom_slider_bar);
        addView(mBar);
        
        mZoomProgressView = new View(context);
        mZoomProgressView.setBackgroundResource(R.drawable.zoom_slider_progress_area);
        addView(mZoomProgressView);
        
        Bitmap barBmp = BitmapFactory.decodeResource(getResources(),R.drawable.zoom_slider_bar);
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
        	mBarSize = barBmp.getWidth();
        } else {
        	mBarSize = barBmp.getHeight();
        }
        mZoomSlider = addImageView(context, R.drawable.ic_zoom_slider);
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);
        mBar.setActivated(activated);
    }

    private int getSliderPosition(int offset) {
        // Calculate the absolute offset of the slider in the zoom control bar.
        // For left-hand users, as the device is rotated for 180 degree for
        // landscape mode, the zoom-in bottom should be on the top, so the
        // position should be reversed.
        int pos; // the relative position in the zoom slider bar
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            if (mOrientation == 180) {
                pos = offset - mTotalIconSize;
            } else {
                pos = mSize - mTotalIconSize - offset;
            }
        } else {
            if (mOrientation == 90) {
                pos = mSize - mTotalIconSize - offset;
            } else {
                pos = offset - mTotalIconSize;
            }
        }
        if (pos < 0) pos = 0;
        if (pos > mSliderLength) pos = mSliderLength;
        return pos;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            mSize = h;
            mIconSize = mZoomIn.getMeasuredHeight();
        } else {
            mSize = w;
            mIconSize = mZoomIn.getMeasuredWidth();
        }
        mTotalIconSize = mIconSize + ICON_SPACING;
        mSliderLength = mSize  - (2 * mTotalIconSize);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!isEnabled() || (mSize == 0)) return false;
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                setActivated(false);
                closeZoomControl();
                break;

            case MotionEvent.ACTION_DOWN:
                setActivated(true);
                mStartChanging = false;
            case MotionEvent.ACTION_MOVE:
                boolean isLandscape = (getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE);
                int pos = getSliderPosition((int)
                        (isLandscape ? event.getY() : event.getX()));
                if (!mStartChanging) {
                    // Make sure the movement is large enough before we start
                    // changing the zoom.
                    // replace mSliderPosition with saveSliderPosition, Because
                    // switching the front/back camera,mSliderPosition=-1,this
                    // is wrong.
                    int delta = saveSliderPosition - pos;
                    if ((delta > THRESHOLD_FIRST_MOVE) ||
                            (delta < -THRESHOLD_FIRST_MOVE)) {
                        mStartChanging = true;
                    }
                }
                if (mStartChanging) {
                    performZoom(1.0d * pos / mSliderLength);
                    mSliderPosition = pos;
                }
                requestLayout();
        }
        return true;
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        // layout for the left-hand camera control
        if ((orientation == 180) || (mOrientation == 180)) requestLayout();
        super.setOrientation(orientation, animation);
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        boolean isLandscape = (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);
        if (mZoomMax == 0) return;
        int size = 0;
        int offset = 0;
        if (isLandscape) {
            size = mBarSize;
            offset = (mIconSize - size)/2;
            mBar.layout(offset, mTotalIconSize, size + offset, mSize - mTotalIconSize);
        } else {
            size = mBarSize;
            offset = (mIconSize - size)/2;
            mBar.layout(mTotalIconSize, offset, mSize - mTotalIconSize, size + offset);
        }

        // For left-hand users, as the device is rotated for 180 degree,
        // the zoom-in button should be on the top.
        int pos; // slider position
        int sliderPosition;
        if (mSliderPosition != -1) { // -1 means invalid
            sliderPosition = mSliderPosition;
        } else {
            sliderPosition = (int) ((double) mSliderLength * mZoomIndex / mZoomMax);
        }
        saveSliderPosition = sliderPosition;
    	int sliderWidth = mZoomSlider.getMeasuredWidth();
    	int sliderHeight = mZoomSlider.getMeasuredHeight();
    	
    	int zoomProgressLeft = 0;
    	int zoomProgressTop = 0;
    	int zoomProgressRight = 0;
    	int zoomProgressBottom = 0;
        if (isLandscape) {
            int offsetY = 2;
            if (mOrientation == 180) {
                mZoomOut.layout(0, 0, mIconSize, mIconSize);
                mZoomIn.layout(0, mSize - mIconSize, mIconSize, mSize);
                pos = mBar.getTop() + sliderPosition;
            } else {
                mZoomIn.layout(0, 0, mIconSize, mIconSize);
                mZoomOut.layout(0, mSize - mIconSize, mIconSize, mSize);
                pos = mBar.getBottom() - sliderPosition;
            }
            if(pos > mBar.getBottom() - sliderHeight + offsetY) {
                pos = mBar.getBottom() - sliderHeight + offsetY;
            }
            if(pos < mBar.getTop() + sliderHeight) {
                offsetY = 0;
            }
            offset = (mIconSize - sliderWidth)/2;
            mZoomSlider.layout(offset, pos + offsetY, sliderWidth + offset,
                    pos + sliderWidth +offsetY);
            
            if(mOrientation == 180) {
            	zoomProgressLeft = mBar.getLeft();
                zoomProgressTop = mBar.getTop();
                zoomProgressRight = mBar.getRight();
                zoomProgressBottom = mZoomSlider.getTop() + sliderWidth/2;
            } else  {
            	zoomProgressLeft = mBar.getLeft();
                zoomProgressTop = mZoomSlider.getTop() + sliderWidth/2;
                zoomProgressRight = mBar.getRight();
                zoomProgressBottom = mBar.getBottom();
            }
        } else {
        	offset = (mIconSize - sliderHeight)/2;
            if (mOrientation == 90){
                mZoomIn.layout(0, 0, mIconSize, mIconSize);
                mZoomOut.layout(mSize - mIconSize, 0, mSize, mIconSize);
                pos = mBar.getRight() - sliderPosition - 3;
            } else {
                mZoomOut.layout(0, 0, mIconSize, mIconSize);
                mZoomIn.layout(mSize - mIconSize, 0, mSize, mIconSize);
                pos = mBar.getLeft() + sliderPosition - 3;
            }
            if(pos > mBar.getRight() - sliderWidth) {
            	pos = mBar.getRight() - sliderWidth;
            }
            mZoomSlider.layout(pos, offset,
                    (pos + sliderWidth), sliderHeight + offset);
            
            if(mOrientation == 90) {
            	zoomProgressLeft = mZoomSlider.getRight() - sliderWidth/2;;
                zoomProgressTop = mBar.getTop();
                zoomProgressRight = mBar.getRight();
                zoomProgressBottom = mBar.getBottom();
            } else  {
            	zoomProgressLeft = mBar.getLeft();
                zoomProgressTop = mBar.getTop();
                zoomProgressRight = mZoomSlider.getLeft()+sliderWidth/2;
                zoomProgressBottom = mBar.getBottom();
            }
        }
        
        mZoomProgressView.layout(zoomProgressLeft,zoomProgressTop,zoomProgressRight,zoomProgressBottom);
    }

    @Override
    public void setZoomIndex(int index) {
        super.setZoomIndex(index);
        mSliderPosition = -1; // -1 means invalid
    }
}
