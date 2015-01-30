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
import android.graphics.BitmapFactory;
import com.android.camera.IconListPreference;
import com.android.camera.ListPreference;
import com.android.camera.R;
import android.view.LayoutInflater;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OtherSettingSubPopup extends AbstractSettingPopup implements AdapterView.OnItemClickListener {
	
    private static final String TAG = "OtherSettingSubPopup";
    private ListPreference mPreference;
    private Listener mListener;
    protected ViewGroup mSettingList;
    private ArrayList<DataHolder> mListItem;
    private int mLastSelectedIdx;
    static public interface Listener {
        public void onSettingChanged();
    }

    public class DataHolder {
    	String text;
    	int imageId;
    	int picSizeImageId;
    }
    public OtherSettingSubPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTitle = (TextView) findViewById(R.id.title);
        mSettingList = (ViewGroup) findViewById(R.id.settingList);
    }
    
    private class BasicSettingAdapter extends BaseAdapter {
    	private Context mContext;
    	private LayoutInflater mInflater;
        BasicSettingAdapter(Context context) {
            super();
            mContext = context;
            mInflater = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE); 
        }

        @Override
        public int getCount() {
        	return mListItem.size();
        }
        
        @Override
        public Object getItem(int position) {
        	return mListItem.get(position);
        }
        
        @Override
        public long getItemId(int position) {
        	return position;
        }
        
        public View getView(final int position, View convertView, ViewGroup parent) {
        	 View subPopupItemView = convertView;
        	 DataHolder holder = mListItem.get(position);

             boolean isCameraOrVideoPref = (mPreference.getKey().equals("pref_camera_picturesize_key")
                     || mPreference.getKey().equals("pref_video_quality_key"));
             if(isCameraOrVideoPref) {
                 if(subPopupItemView == null ||
                         subPopupItemView.getId() != R.layout.setting_item) {
                     subPopupItemView = (View) mInflater.inflate(
                             R.layout.setting_item, parent,false); 
                     subPopupItemView.setId(R.layout.setting_item);
                 }
             } else {
                 if(subPopupItemView == null ||
                         subPopupItemView.getId() != R.layout.other_setting_subpopup_item) {
                     subPopupItemView = (View) mInflater.inflate(
                             R.layout.other_setting_subpopup_item, parent,false);
                     subPopupItemView.setId(R.layout.other_setting_subpopup_item);
                 }
             }

            if(isCameraOrVideoPref) {
                ImageView image = (ImageView)subPopupItemView.findViewById(R.id.image);
                image.setImageBitmap(BitmapFactory.decodeResource(getResources(),holder.picSizeImageId));
            }
        	TextView tv = (TextView)subPopupItemView.findViewById(R.id.text);
        	tv.setText(holder.text);
        	ImageView checkImg = (ImageView)subPopupItemView.findViewById(R.id.check_state_img);
        	checkImg.setImageBitmap(BitmapFactory.decodeResource(getResources(),holder.imageId));
            return subPopupItemView;
       }     
    }

    public void initialize(ListPreference preference) {
    	if(mListItem == null) {
    		mListItem = new ArrayList<DataHolder>();
    	}
    	mListItem.clear();
        mPreference = preference;
        Context context = getContext();
        CharSequence[] entries = mPreference.getEntries();
        CharSequence[] entryValues = mPreference.getEntryValues();

        // Set title.
        mTitle.setText(mPreference.getTitle());
        // Prepare the ListView.
        for(int i = 0; i < entries.length; ++i) {
        	DataHolder holder = new DataHolder();  
        	holder.text = entries[i].toString();
        	holder.imageId = R.drawable.button_unselect;
        	if(mPreference.getKey().equals("pref_camera_picturesize_key") ||
        			mPreference.getKey().equals("pref_video_quality_key")) {
        		int[] iconIds = ((IconListPreference)mPreference).getIconIds();
        		holder.picSizeImageId = iconIds[i];
        	}
            mListItem.add(holder);
        }
        
        int index = mPreference.findIndexOfValue(mPreference.getValue());
        if(index < 0) {
        	index = 0;
        }
        if(mListItem.size() > 0) {
            mListItem.get(index).imageId = R.drawable.button_select;
        }
        BasicSettingAdapter listItemAdapter = new BasicSettingAdapter(context);
        ((AbsListView) mSettingList).setAdapter(listItemAdapter);
        ((AbsListView) mSettingList).setOnItemClickListener(this);
        reloadPreference();
    }

    // The value of the preference may have changed. Update the UI.
    @Override
    public void reloadPreference() {
    	 if(mLastSelectedIdx > 0 && mLastSelectedIdx < mListItem.size()) {
             mListItem.get(mLastSelectedIdx).imageId = R.drawable.button_unselect;
          }
          int index = mPreference.findIndexOfValue(mPreference.getValue());
          if (index != -1) {
        	  mListItem.get(index).imageId = R.drawable.button_select;
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
        if(mLastSelectedIdx >= 0 && mLastSelectedIdx != index && mListItem.size() > 1) {
        	 mListItem.get(index).imageId = R.drawable.button_select;
        	 mListItem.get(mLastSelectedIdx).imageId = R.drawable.button_unselect;
        }
        ((BaseAdapter) parent.getAdapter()).notifyDataSetChanged();
        
        if (mListener != null) mListener.onSettingChanged();
        mLastSelectedIdx = index;
    }
    
    public int getPrefItemSize() {
    	return mListItem.size();
    }
}
