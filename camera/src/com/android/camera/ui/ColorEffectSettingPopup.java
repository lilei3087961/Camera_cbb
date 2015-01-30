package com.android.camera.ui;

import com.android.camera.IconListPreference;

import com.android.camera.R;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class ColorEffectSettingPopup extends AbstractSettingPopup implements View.OnClickListener {

	private final String TAG = "ColorEffectSettingPopup";
	private IconListPreference mPreference;
	private Listener mListener;
	private LinearLayout mColorEffecdtItemsContainer;
	private int[] mImages;
	private CharSequence[] mEntries;
	
	private int mLastItemIdx;
	private final int mEffectTvId = 1;
	private final int mEffectLineId = 2;

	static public interface Listener {
		public void onSettingChanged();
	}

	public ColorEffectSettingPopup(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mTitle = (TextView) findViewById(R.id.title);
		mColorEffecdtItemsContainer = (LinearLayout) findViewById(R.id.color_effecdt_items_container);
	}

	public void initialize(IconListPreference preference) {
		mPreference = preference;
		Context context = getContext();
		mEntries = mPreference.getEntries();
		mImages = mPreference.getLargeIconIds();
		constructColorEffectView();
		reloadPreference();
	}

	@Override
	public void reloadPreference() {
		setItemSelect(mLastItemIdx,false);
		mLastItemIdx = mPreference.findIndexOfValue(mPreference.getValue());
		setItemSelect(mLastItemIdx,true);
	}

	public void setSettingChangedListener(Listener listener) {
		mListener = listener;
	}

	private void constructColorEffectView() {
		Context context = getContext();

		for (int idx = 0; idx < mImages.length; idx++) {
			LinearLayout itemLayout = new LinearLayout(context);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			itemLayout.setLayoutParams(params);
			itemLayout.setOrientation(LinearLayout.VERTICAL);
			itemLayout.setPadding(50, 20, 50, 10);
			itemLayout.setId(mImages[idx]);
			itemLayout.setOnClickListener(this);

			ImageView imageview = new ImageView(context);
			LayoutParams imgeBtnParams = new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			imageview.setLayoutParams(imgeBtnParams);
			imageview.setScaleType(ScaleType.CENTER_INSIDE);
			imageview.setBackgroundResource(mImages[idx]);

			itemLayout.addView(imageview);

			TextView tv = new TextView(context);
			tv.setId(mEffectTvId);
			LayoutParams tvParams = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			tv.setTextSize(16);
			tv.setTextColor(Color.BLACK);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			tv.setText(mEntries[idx]);
			itemLayout.addView(tv);
			
//			LinearLayout line = new LinearLayout(context);
//			LayoutParams lineparams = new LayoutParams(LayoutParams.MATCH_PARENT,2);
//			line.setLayoutParams(lineparams);
//			line.setBackgroundColor(Color.GRAY);
//			line.setVisibility(View.GONE);
//			line.setId(mEffectLineId);
//			itemLayout.addView(line);
			
			mColorEffecdtItemsContainer.addView(itemLayout);
		}
	}
	
	@Override
	public void onClick(View view) {
		int id = view.getId();
		int index = 0;
		boolean find = false;
		for(index = 0; index < mImages.length; index++) {
			if(id == mImages[index]) {
				find = true;
				break;
			}
		}
		
		if(!find) {
			return;
		}
		if(mLastItemIdx != index) {
			setItemSelect(mLastItemIdx,false);
			setItemSelect(index,true);
			mLastItemIdx = index;
		}
		mPreference.setValueIndex(index);
		if (mListener != null)
			mListener.onSettingChanged();

	}
	
	private void setItemSelect(int index, boolean selected) {
		if(index < 0 || index > mImages.length -1) {
			return;
		}
		LinearLayout itemLay = (LinearLayout)mColorEffecdtItemsContainer.getChildAt(index);
		TextView tv = (TextView)itemLay.findViewById(mEffectTvId);
//		LinearLayout line = (LinearLayout)itemLay.findViewById(mEffectLineId);
		if(selected) {
			tv.setTextColor(Color.rgb(0,0xff,0xff));
//			line.setVisibility(View.VISIBLE);
		} else {
			tv.setTextColor(Color.BLACK);
//			line.setVisibility(View.GONE);
		}
	}
}
