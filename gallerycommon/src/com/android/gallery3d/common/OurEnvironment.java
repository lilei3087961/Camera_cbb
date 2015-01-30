package com.android.gallery3d.common;

import android.os.Environment;
import java.io.File;
import java.net.URI;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Method;
import android.os.storage.StorageManager;
import android.util.Log;
import android.content.Context;

public class OurEnvironment {
    /*in Gallery we use name ExternalStorage -> Internal Storage
     * use name SdcardStroage -> external TCard Stroage.
     * */
    
    private static final String TAG = "OurEnvironment";
    
    public static final String MEDIA_MOUNTED = Environment.MEDIA_MOUNTED;
    
    private static boolean DOUBLE_SDCARD = (SystemProperties
    		.get("ro.freecomm.double.sdcard")).equals("1");
    
    public static String DIRECTORY_DOWNLOADS = Environment.DIRECTORY_DOWNLOADS;
    
    public static String DIRECTORY_PICTURES = Environment.DIRECTORY_PICTURES;
    
    public static final String MEDIA_MOUNTED_READ_ONLY = Environment.MEDIA_MOUNTED_READ_ONLY;
    
    public static File getExternalStorageDirectory() {
    	if (DOUBLE_SDCARD) {
    		//return Environment.getSubInternalStorageDirectory();
    	}
        return Environment.getExternalStorageDirectory();
    }
    
    public static String getExternalStorageState() {
    	if (DOUBLE_SDCARD) {
    		//return Environment.getSubInternalStorageState();
    	}
        return Environment.getExternalStorageState();
    }
    
    public static File getExternalStoragePublicDirectory(String type) {
        return new File(getExternalStorageDirectory(), type);
    }
    
    public static File getExternalCacheDir() {
        if (getExternalStorageState().equals(MEDIA_MOUNTED)) {
            File cacheDir = getExternalStoragePublicDirectory("/Android/com.android.gallery3d/");
            if (cacheDir != null && !cacheDir.isDirectory()) {
                cacheDir.mkdirs();
            }
            return cacheDir;
        }
        return null;
    }
    
    public static File getSdcardStorageDirectory() {
    	if (DOUBLE_SDCARD) {
    		//return Environment.getSubExternalStorageDirectory();
    	}
        return Environment.getExternalStorageDirectory();
    }
    
    public static String getSdcardStorageState() {
    	if (DOUBLE_SDCARD) {
    		//return Environment.getSubExternalStorageState();
    	}
        return Environment.getExternalStorageState();
    }
    
    public static File getRootDirectory() {
		return Environment.getRootDirectory();
	}

	public static File getDownloadCacheDirectory() {
		return Environment.getDownloadCacheDirectory();
	}

	public static boolean isExternalStorageRemovable() {
		return Environment.isExternalStorageRemovable();
	}

	public static boolean isExternalStorageEmulated() {
		return Environment.isExternalStorageEmulated();
	}

	public static File getMediaStorageDirectory() {
		return Environment.getMediaStorageDirectory();
	}

    public static String getSdCardStoragePath(Context context) {        
        StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE); 
        Method methodGetList;  
        Method methodisRemovable; 
        Method methodallowMassStorage;
        Method methodgetPath;     
    	  Object[] volumes = {};  
    	  String path = null;
    	
        try { 
            methodGetList = storageManager.getClass().getMethod("getVolumeList");
        	volumes = (Object[]) methodGetList.invoke(storageManager); 
        } catch (Exception ex) { 
            ex.printStackTrace(); 
            return null;
        }
        
        try {
        	Class<?> cls = Class.forName("android.os.storage.StorageVolume");
            methodisRemovable = cls.getMethod("isRemovable");
            methodallowMassStorage = cls.getMethod("allowMassStorage");
            methodgetPath = cls.getMethod("getPath");
        
		for (int i = 0; i < volumes.length; i++) {
		    Boolean isRemovable = (Boolean)methodisRemovable.invoke(volumes[i]);
		    Boolean allowMassStorage = (Boolean)methodallowMassStorage.invoke(volumes[i]);
		    String volumePath = (String)methodgetPath.invoke(volumes[i]);

		    if (isRemovable && allowMassStorage) {
		        path = volumePath;
		    }
		}
        } catch (Exception ex) { 
            ex.printStackTrace(); 
        } 
        
        return path;
    }
	
    public static String isStorageState(Context context, String path) {        
  	  StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE); 
      Method methodgetVolumeState;  
  	  String state = null;
  	  if (path ==null) {
  	  	return null;
  	  }
  	
      try { 
          Class<?>[] paramClasses = {new String().getClass()};
          methodgetVolumeState = storageManager.getClass().getMethod("getVolumeState", paramClasses); 
      
          Object[] params = {(Object)path};
      	state = (String) methodgetVolumeState.invoke(storageManager, params); 
      } catch (Exception e) { 
          e.printStackTrace(); 
      } 
      
      return state;
  } 
	
}
