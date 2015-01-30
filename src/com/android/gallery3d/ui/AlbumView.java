/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.ui;


import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.android.camera.R;
import com.android.gallery3d.app.GalleryActivity;
/*
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.StaticBackground;
*/
public class AlbumView extends GLView {
    @SuppressWarnings("unused")
    private static final String TAG = "AlbumView";

    private GalleryActivity mActivity;
    private boolean mDisplayBar;
    private GestureDetector mGestureDetector;
    
    private final float mMatrix[] = new float[16];
    
    private StaticBackground mBar;
    private GLView mSlot;
    private SlotView mSlotView;
    
    // TODO: change it.
    private final int mBarWidth = 100;

    public AlbumView(GalleryActivity activity, GLView slotView) {
        mActivity = activity;
        Context context = activity.getAndroidContext();
        
        mDisplayBar = true;
        
        mBar = new StaticBackground(context);
        addComponent(mBar);
        
        mSlot = slotView;
        addComponent(mSlot);
        
        /*
        SlotView mAlbumView = new SlotView();
        addComponent(mAlbumView);
        */
        mBar.setImage(R.drawable.preview, R.drawable.preview);
        //mSlot.setImage(R.drawable.preview2, R.drawable.preview2);
        
        
        mGestureDetector = new GestureDetector(context,
                new MyGestureListener(), null, true /* ignoreMultitouch */);
    }
    
    private class MyGestureListener
        extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(
            MotionEvent e1, MotionEvent e2, float dx, float dy) {
            
            if (isEnoughMove(dx)) {
                // move to left
                if (dx > 0 && mDisplayBar) {
                    mDisplayBar = false;
                    requestLayout();
                } else if (dx < 0 && !mDisplayBar) {
                    mDisplayBar = true;
                    requestLayout();
                }
                
            }
            
            return true;
        }
        
        private boolean isEnoughMove(float dx) {
            return Math.abs(dx) > 30;
        }
        
    }
    
    
    @Override
    protected void onLayout(
            boolean changeSize, int left, int top, int right, int bottom) {
        
        if (mDisplayBar) {
            mBar.layout(left, top, left + mBarWidth, bottom);
            mSlot.layout(left + mBarWidth, top, right, bottom);
        } else {
            mSlot.layout(left, top, right, bottom);
        }

        //mBar.layout(0, 0, 100, 100);
        //mSlot.layout(400, 400, 800, 800);
    }
    
    @Override
    protected void render(GLCanvas canvas) {
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
        //canvas.multiplyMatrix(mMatrix, 0);
        super.render(canvas);
        
        //mBar.render(canvas);
        //mSlot.render(canvas);
        
        canvas.restore();
    }
    
    @Override
    protected boolean onTouch(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }
}
