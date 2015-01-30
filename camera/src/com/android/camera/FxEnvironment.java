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

package com.android.camera;

import android.os.Build;
import android.os.Environment;
import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import android.util.Log;
import java.lang.reflect.InvocationTargetException; 
import java.lang.reflect.Method;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.content.Context;

public class FxEnvironment {
	private static String mInternalSdCardPath = null;
	private static String mExternalSdCardPath = null;
	private static Context mContext;
	private static boolean mIsSingleSdcard = true;
	
	public static boolean isSingleSdcard() {
		
         return mIsSingleSdcard;
	}

     public static String getExternalStoragePublicDirectory() {
    	 if(mExternalSdCardPath == null) return null;
    	 return new File(mExternalSdCardPath,Environment.DIRECTORY_DCIM).toString();
	 }

     public static String getInternalStoragePublicDirectory() {
    	 return new File(mInternalSdCardPath,Environment.DIRECTORY_DCIM).toString();
     }
     

    public static String getExternalStorageState() {
    	return isStorageState(mContext,mExternalSdCardPath);
    }
    
    public static String getInternalStorageState() {
    	return isStorageState(mContext,mInternalSdCardPath);
    }

 	public static String getExternalStorageDirectory(Context context) {
 		if(context == null) return mExternalSdCardPath;
 		mContext = context;
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
		    	if(mInternalSdCardPath != null) {
		    		mInternalSdCardPath = getInternalStorageDirectory(context);
		    	}
		    	if(mInternalSdCardPath != null &&
		    			!mInternalSdCardPath.equalsIgnoreCase(volumePath) &&
		    			isStorageExist(context,volumePath)) {
	 				path = volumePath;
	 				mIsSingleSdcard = false;
		        }
		    }
		}
        } catch (Exception ex) { 
            ex.printStackTrace(); 
        } 
        mExternalSdCardPath = path;
        return mExternalSdCardPath;
    }
 	
 	public static String getInternalStorageDirectory(Context context) {
 		if(context == null) return mInternalSdCardPath;
 		mContext = context;

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
        
           int canNotRemovableItemCount = 0;
	       for (int i = 0; i < volumes.length; i++) {
		    Boolean isRemovable = (Boolean)methodisRemovable.invoke(volumes[i]);
		    Boolean allowMassStorage = (Boolean)methodallowMassStorage.invoke(volumes[i]);
		    String volumePath = (String)methodgetPath.invoke(volumes[i]);
		    if (!isRemovable) {
		        path = volumePath;
		        if(isStorageExist(context,path)) {
		        	 mInternalSdCardPath = path;
		        }

		        canNotRemovableItemCount++;
		    }
		    
		    if(volumes.length == 2 && canNotRemovableItemCount == volumes.length) {
		    	mIsSingleSdcard = true;
		    	mExternalSdCardPath = null;
		    }
		}
        } catch (Exception ex) { 
            ex.printStackTrace(); 
        } 

        return mInternalSdCardPath;
    }
 	
 	public static boolean isStorageExist(Context context, String path) {        
        StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE); 
        Method methodgetVolumeState;  
    	  String state = null;  
    	  boolean exist = false;
    	  if (path ==null) {
    	  	return false;
        }
    	
        try { 
            Class<?>[] paramClasses = {new String().getClass()};
            methodgetVolumeState = storageManager.getClass().getMethod("getVolumeState", paramClasses); 
        
            Object[] params = {(Object)path};
        	state = (String) methodgetVolumeState.invoke(storageManager, params); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
        
        if (state != null && state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            exist = true;
        }
        
        return exist;
    } 
 	
 	public static String isStorageState(Context context, String path) {
 	  if(context == null) return null;
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










