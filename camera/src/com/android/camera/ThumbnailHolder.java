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

package com.android.camera;

import android.content.ContentResolver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class ThumbnailHolder {
    private static final int CLEAN_THUMBNAIL = 1;

    //BEGIN: Modified by xiongzhu at 2013-04-15
    // private static Thumbnail sLastThumbnail;
    private static Thumbnail sLastThumbnailMemory;
    private static Thumbnail sLastThumbnailSdcard;
    private static Thumbnail sLastThumbnailTemp;
    //END:   Modified by xiongzhu at 2013-04-15

    private static class LazyHandlerHolder {
        private static final HandlerThread sHandlerThread = new HandlerThread("ClearThumbnail");
        static {
            sHandlerThread.start();
        }
        public static final Handler sHandler =
                new Handler(sHandlerThread.getLooper(), new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        switch(msg.what) {
                            case CLEAN_THUMBNAIL:
                                cleanLastThumbnail();
                                break;
                        }
                        return true;
                    }
                });
    }

    private ThumbnailHolder() {
    }

    public static synchronized Thumbnail getLastThumbnail(ContentResolver resolver) {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // if (sLastThumbnail != null) {  // Thumbnail exists. Checks validity.
        //     LazyHandlerHolder.sHandler.removeMessages(CLEAN_THUMBNAIL);
        //     Thumbnail t = sLastThumbnail;
        //     sLastThumbnail = null;
        Thumbnail t = null;
        if(Storage.mIsExternalStorage){
	        if(sLastThumbnailSdcard != null){
                LazyHandlerHolder.sHandler.removeMessages(CLEAN_THUMBNAIL);
		        t = sLastThumbnailSdcard;
	            sLastThumbnailSdcard = null;
		    }
	    }else{
		    if(sLastThumbnailMemory != null){
                LazyHandlerHolder.sHandler.removeMessages(CLEAN_THUMBNAIL);
		        t = sLastThumbnailMemory; 
		        sLastThumbnailMemory = null;
		    }
		}

        if(t != null){
            LazyHandlerHolder.sHandler.removeMessages(CLEAN_THUMBNAIL);
            //BEGIN: Deleted by zhanghongxing at 2013-04-15
            // Thumbnail t = sLastThumbnail;
            // sLastThumbnail = null;
            //END:   Deleted by zhanghongxing at 2013-04-15
            if (Util.isUriValid(t.getUri(), resolver)) {
                return t;
            }
        }
        //END:   Modified by xiongzhu at 2013-04-15

        return null;
    }

    private static synchronized void cleanLastThumbnail() {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // sLastThumbnail = null;
        sLastThumbnailSdcard = null;
        sLastThumbnailMemory = null;
        //END:   Modified by xiongzhu at 2013-04-15
    }

    public static synchronized void keep(Thumbnail t) {
        //BEGIN: Modified by xiongzhu at 2013-04-15
        // sLastThumbnail = t;
        if(Storage.mIsExternalStorage){
            sLastThumbnailSdcard = t;
        } else {
            sLastThumbnailMemory = t;
        }
        //END:   Modified by xiongzhu at 2013-04-15
        LazyHandlerHolder.sHandler.removeMessages(CLEAN_THUMBNAIL);
        LazyHandlerHolder.sHandler.sendEmptyMessageDelayed(CLEAN_THUMBNAIL, 3000);
    }
}
