package com.android.camera;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.Activity;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.Rotatable;
import android.util.Log;

public class TimerSnapManager {
	private final String TAG = "TimerSnapManager";
    public static interface TimerSnapListener {
        public void doTimerSnap();
    }
    
    private static int[] times = new int[]{
		R.drawable.number_one,R.drawable.number_two,R.drawable.number_three,R.drawable.number_four,R.drawable.number_five,
		R.drawable.number_six,R.drawable.number_seven,R.drawable.number_eight,R.drawable.number_nine,R.drawable.number_ten,
		R.drawable.number_eleven,R.drawable.number_twelve,R.drawable.number_thirteen,R.drawable.number_fourteen,
		R.drawable.number_fifteen,R.drawable.number_sixteen,R.drawable.number_seventeen,R.drawable.number_eighteen,
		R.drawable.number_nineteen,R.drawable.number_twenty};
    
    private int timecount;
    private RotateImageView image;
	private int count = 0;
	private MediaPlayer mPlayer;
    private TimerSnapListener  mTimerSnapListener;
    private int mSec = 0;
    private boolean isToStopAnim = false;
    
    public TimerSnapManager(TimerSnapListener  timerSnapListener) {
        mTimerSnapListener = timerSnapListener;
    }
    
    public void startToSnap(int sec) {
    	isToStopAnim = false;
    	if(mPlayer == null) {
    		mPlayer = MediaPlayer.create((Activity)mTimerSnapListener,R.raw.camera_timer);
    	}
        mSec = sec;
        count = mSec - 1;
        image = (RotateImageView) (((Activity)(mTimerSnapListener)).findViewById(R.id.timer_image));
        AnimationSet anim = new AnimationSet(true); 
        ScaleAnimation a = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f,
    		    Animation.RELATIVE_TO_SELF, 0.5f,
    		    Animation.RELATIVE_TO_SELF, 0.5f);    
	    anim.addAnimation(a);
	    anim.setDuration(1000);
	    anim.setStartOffset(0);
	    anim.setAnimationListener(new AnimationListener() {
		
		    @Override
		    public void onAnimationStart(Animation anim) {
		    	if(isToStopAnim) return;
			    image.setImageResource(times[count]);
			    mPlayer.start();
			    count--;
            }
		
            @Override
		    public void onAnimationRepeat(Animation anim) {
			
		    }
		
		    @Override
		    public void onAnimationEnd(Animation anim) {
		    	if(isToStopAnim) return;
			    if(count >= 0) {
				    image.clearAnimation();
			        image.startAnimation(anim);
			    } else {
			    	if(mPlayer != null) {
			    		if(mPlayer.isPlaying()) {
			    			mPlayer.stop();
			    		}
			    		mPlayer.release();
			    		mPlayer = null;
			    	}
				    image.setVisibility(View.GONE);
                    if(mTimerSnapListener != null) {
                        mTimerSnapListener.doTimerSnap();
                    }
                }
		    }
	    });
	    image.setImageResource(R.drawable.number_one);
	    image.startAnimation(anim);
	    image.setVisibility(View.VISIBLE);
    }

    public void clearTimerAnima() {
    	isToStopAnim = true;
    	if(image != null) {
    	    image.setVisibility(View.GONE);
    	    image.clearAnimation();
    	}
    	if(mPlayer != null) {
    		if(mPlayer.isPlaying()) {
    			mPlayer.stop();
    		}
    		mPlayer.release();
    		mPlayer = null;
    	}
    }

    public void setOrientation(int orientation, boolean animation) {
    	if(image != null) {
    		((Rotatable)image).setOrientation(orientation,animation);
    	}
    }
    
    public void showContinueSnapNum(int sec) {
    	mSec = sec;
        int idx = mSec - 1;
        if(idx < 0 || idx >= times.length) return;
        if(image == null) {
            image = (RotateImageView) (((Activity)(mTimerSnapListener)).findViewById(R.id.timer_image));
        }
    	image.setImageResource(times[idx]);
    	if(idx == 0) {
    	    image.setVisibility(View.VISIBLE);
    	}
    }
    
    public void hideContinueSnapNum() {
    	if(image != null)
    	    image.setVisibility(View.GONE);
    }
}
