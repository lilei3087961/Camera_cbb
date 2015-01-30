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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.camera.IconListPreference;
import com.android.camera.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A popup window that shows one camera setting. The title is the name of the
// setting (ex: white-balance). The entries are the supported values (ex:
// daylight, incandescent, etc).
public class BasicSettingPopup extends AbstractSettingPopup implements
        AdapterView.OnItemClickListener {
    private static final String TAG = "BasicSettingPopup";
    private IconListPreference mPreference;
    private Listener mListener;
    protected ViewGroup mSettingList;
    private ArrayList<HashMap<String, Object>> mListItem;
    private int mLastSelectedIdx;
    private ArrayList<Integer> unEnabled = new ArrayList<Integer>();;
    static public interface Listener {
        public void onSettingChanged();
    }

    public BasicSettingPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTitle = (TextView) findViewById(R.id.title);
        mSettingList = (ViewGroup) findViewById(R.id.settingList);
    }
    
    private class BasicSettingAdapter extends SimpleAdapter {
        BasicSettingAdapter(Context context, List<? extends Map<String, ?>> data,
                int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public void setViewImage(ImageView v, String value) {
            if ("".equals(value)) {
                // Some settings have no icons. Ex: exposure compensation.
                v.setVisibility(View.GONE);
            } else {
                super.setViewImage(v, value);
            }
        }
        
        public View getView(final int position, View convertView, ViewGroup parent) {   
        	       View view = super.getView(position, convertView, parent);    
        	       if (unEnabled.indexOf(position) >= 0){
        	    	   view.setAlpha(0.4f);
        	    	   view.setClickable(true);
        	       }else {
        	    	   view.setAlpha(1.0f);
        	    	   view.setClickable(false);
        	       }
               return view;   
       }   

        
        
    }

    public void initialize(IconListPreference preference) {
    	unEnabled.clear(); 
    	ArrayList<String> unEnabledEntry = preference.getUnabledEntry();
    	
    	if(mListItem == null) {
    		mListItem = new ArrayList<HashMap<String, Object>>();
    	}
    	mListItem.clear();
        mPreference = preference;
        Context context = getContext();
        CharSequence[] entries = mPreference.getEntries();
        CharSequence[] entryValues = mPreference.getEntryValues();
        int[] iconIds = mPreference.getImageIds();
        if (iconIds == null) {
            iconIds = mPreference.getIconIds();
        }

        // Set title.
        mTitle.setText(mPreference.getTitle());

        // Prepare the ListView.
        
        for(int i = 0; i < entries.length; ++i) {
        	if((unEnabledEntry != null) && (unEnabledEntry.indexOf(entryValues[i]) >= 0)){
            	unEnabled.add(i);
            }
            HashMap<String, Object> map = new HashMap<String, Object>();        
            map.put("text", entries[i].toString());
            if (iconIds != null) map.put("image", iconIds[i]);
            map.put("check_state_img", R.drawable.button_unselect);
            mListItem.add(map);
        }
        
        int index = mPreference.findIndexOfValue(mPreference.getValue());
        if(index < 0) {
        	index = 0;
        }
        mListItem.get(index).put("check_state_img", R.drawable.button_select);
        SimpleAdapter listItemAdapter = new BasicSettingAdapter(context, mListItem,
                R.layout.setting_item,
                new String[] {"text", "image","check_state_img"},
                new int[] {R.id.text, R.id.image,R.id.check_state_img});
        ((AbsListView) mSettingList).setAdapter(listItemAdapter);
        ((AbsListView) mSettingList).setOnItemClickListener(this);
        reloadPreference();
    }

    // The value of the preference may have changed. Update the UI.
    @Override
    public void reloadPreference() {
       if(mLastSelectedIdx >= 0 && mLastSelectedIdx < mListItem.size()) {
           mListItem.get(mLastSelectedIdx).put("check_state_img", R.drawable.button_unselect);
        }
        int index = mPreference.findIndexOfValue(mPreference.getValue());
        if (index != -1) {
            mListItem.get(index).put("check_state_img", R.drawable.button_select);
            ((AbsListView) mSettingList).setItemChecked(index, true);
        } else {
            Log.e(TAG, "Invalid preference value.");
            mPreference.print();
        }
        
        mLastSelectedIdx = index;
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view,
            int index, long id) {
        mPreference.setValueIndex(index);
        if( mLastSelectedIdx != index && mListItem.size() > 1) {
            mListItem.get(index).put("check_state_img", R.drawable.button_select);
            mListItem.get(mLastSelectedIdx).put("check_state_img", R.drawable.button_unselect);
        }
        ((BaseAdapter) parent.getAdapter()).notifyDataSetChanged();
        
        if (mListener != null) mListener.onSettingChanged();
        mLastSelectedIdx = index;
    }
}
