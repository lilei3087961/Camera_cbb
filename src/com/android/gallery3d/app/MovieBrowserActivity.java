package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.android.camera.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.GLRoot;

public class MovieBrowserActivity extends AbstractGalleryActivity{
    
    private GalleryActionBar mActionBar;
    private static final String TAG = "MovieBrowserActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "onCreate enter");
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        mActionBar = new GalleryActionBar(this);
        
        setContentView(R.layout.main);

        initializeByIntent();
        Log.i(TAG, "onCreate out");
    }
    
    private void initializeByIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Log.i(TAG, "action ="+intent.getAction());
        startPage();
    }
    
    public void startPage() {
        PicasaSource.showSignInReminder(this);
        Bundle data = new Bundle();
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(DataManager.INCLUDE_VIDEO));
        data.putBoolean(Gallery.KEY_GET_VIDEO, true);
        getStateManager().startState(AlbumSetPage.class, data);
          
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }
    
     @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }
    
     @Override
    public void onDestroy() {
        super.onDestroy();
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().destroy();
        } finally {
            root.unlockRenderThread();
        }
    }
    
    @Override
    protected void onResume() {
        Utils.assertTrue(getStateManager().getStateCount() > 0);
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
     @Override
    public GalleryActionBar getGalleryActionBar() {
        return mActionBar;
    }
    
}

