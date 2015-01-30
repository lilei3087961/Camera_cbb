package com.android.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.animation.Animator;
import com.android.camera.R;
import android.util.Log;

public class Switch extends View {

	public abstract static interface OnSwitchChangeListener {
		public abstract void onSwitchChangeListener(SwitchState newState);
	}
	private final int DURATION = 200;
	private Bitmap mOnBackground;
	private Bitmap mOffBackground;
	private Bitmap mSwitchNormalThumb;
	
	private int mWidth;
	private int mHeight;
	
	private int mThumbWidth;
	private int mThumbHeight;
	private RectF mThumbRect;
	
	private float mLastX;
	private Paint mPaint;

    private float mThumbLeftBeforeAni;
	
	public enum  SwitchState {
		OFF,ON
	};
	private SwitchState mState;
	private ValueAnimator mValueAnimator;
	
	public enum  ActionState {
		IDLE,DOWN,MOVE,UP
	};
	private ActionState mActionState;
	
	private OnSwitchChangeListener mOnSwitchChangeListener;
	
	public Switch(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		Resources res = getResources(); 
		mOnBackground = BitmapFactory.decodeResource(res, R.drawable.switch_on_bg);
		mOffBackground = BitmapFactory.decodeResource(res, R.drawable.switch_off_bg);
		mSwitchNormalThumb = BitmapFactory.decodeResource(res, R.drawable.switch_dot);

		mWidth = mOnBackground.getWidth();
		mHeight = mOnBackground.getHeight();
		
		mThumbWidth = mSwitchNormalThumb.getWidth();
		mThumbHeight = mSwitchNormalThumb.getHeight();
		mState = SwitchState.OFF;
		mThumbRect = new RectF(0,0,mThumbWidth,mThumbHeight);
		
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mActionState = ActionState.IDLE;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(mWidth, mHeight);
	}
	
	public void setSwichState(SwitchState state) {
		if(mState == state) return;
		mState = state;
		if(mState == SwitchState.OFF) {
			mThumbRect = new RectF(0,0,mThumbWidth,mThumbHeight);
		} else {
			mThumbRect = new RectF(mWidth - mThumbWidth,0,mWidth,mThumbHeight);
		}
		invalidate();
	}
	
	public void setOnSwitchChangeListener(OnSwitchChangeListener listener) {
		mOnSwitchChangeListener = listener;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		float newX = event.getX();
		
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			mActionState = ActionState.DOWN;
			if(mValueAnimator != null && mValueAnimator.isRunning()) {
	        	mValueAnimator.cancel();
	        	mValueAnimator = null;
	        }
			break;
		case MotionEvent.ACTION_MOVE:
			mActionState = ActionState.MOVE;
			mThumbRect.left += newX - mLastX;
			if(mThumbRect.left < 0) mThumbRect.left = 0;
			if(mThumbRect.left > mWidth - mThumbWidth) mThumbRect.left = mWidth - mThumbWidth;
			mThumbRect.right = mThumbRect.left + mThumbWidth;
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mActionState = ActionState.UP;
		    handleSwitchKeyUp();
		    break;
		}
		
		mLastX = newX;
		invalidate();
		return true;
	}

	private void handleSwitchKeyUp() {
        SwitchState state = mState == SwitchState.ON ? SwitchState.OFF : SwitchState.ON;

        mState = state;

        if(mValueAnimator != null && mValueAnimator.isRunning()) {
        	mValueAnimator.cancel();
        	mValueAnimator = null;
        }
		if(mState == SwitchState.OFF) {
			mValueAnimator = createValueAnimator(0,(int)mThumbRect.left);
		} else {
			mValueAnimator = createValueAnimator(0,(int)(mWidth - mThumbRect.right));
		}
		mValueAnimator.start();
	}

    public ValueAnimator createValueAnimator(int start, int end) {
         mThumbLeftBeforeAni = mThumbRect.left;
         ValueAnimator animator = ValueAnimator.ofInt(start, end);  
         animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {  

         @Override  
         public void onAnimationUpdate(ValueAnimator valueAnimator) {  
               int value = (Integer) valueAnimator.getAnimatedValue();
              if(mState == SwitchState.OFF) {
                   mThumbRect.left = mThumbLeftBeforeAni - value;
              } else {
                   mThumbRect.left = mThumbLeftBeforeAni + value;
              }
                
		      if(mThumbRect.left < 0) mThumbRect.left = 0;
		      if(mThumbRect.left > mWidth - mThumbWidth) mThumbRect.left = mWidth - mThumbWidth;
		      mThumbRect.right = mThumbRect.left + mThumbWidth;
			  invalidate();
            }  
        });

		if(mState == SwitchState.OFF) {
            animator.setDuration(DURATION / (mWidth - mThumbWidth) * (int)mThumbRect.left);  
		} else {
            animator.setDuration(DURATION / (mWidth - mThumbWidth)*(mWidth - (int)mThumbRect.right));  
		}
		animator.addListener(new Animator.AnimatorListener() {
			
			@Override
			public void onAnimationStart(Animator arg0) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animator arg0) {
				
			}
			
			@Override
			public void onAnimationEnd(Animator arg0) {
				mActionState = ActionState.IDLE;
			     if(mOnSwitchChangeListener != null) {
		                mOnSwitchChangeListener.onSwitchChangeListener(mState);
		          }
			}
			
			@Override
			public void onAnimationCancel(Animator arg0) {
				
			}
		});
        return animator;  
    }  

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		RectF thumbRect = mThumbRect;
		
		
		if(mActionState == ActionState.IDLE) {
			if(mState == SwitchState.OFF) {
				canvas.drawBitmap(mOffBackground, 0, 0, mPaint);
			} else {
				canvas.drawBitmap(mOnBackground, 0, 0, mPaint);
			}
		} else {
			canvas.save();
			canvas.drawBitmap(mOnBackground, 0, 0, mPaint);
			canvas.restore();
			canvas.save();
			canvas.clipRect(thumbRect.left + mThumbWidth/2,0,mWidth,mHeight);
			canvas.drawBitmap(mOffBackground, 0, 0, mPaint);
			canvas.restore();
		}
		
		canvas.save();
		canvas.translate(thumbRect.left, 0);
		canvas.drawBitmap(mSwitchNormalThumb, 0, thumbRect.top, mPaint);
		canvas.restore();
	}
	
}
