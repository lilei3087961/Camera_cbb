/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (c) 2012-2013, The Linux Foundation. All Rights Reserved.
 *
 * Not a Contribution, Apache licensenotifications and license are retained
 * for attribution purposes only.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class AutoFocusManager {
    private static final String TAG = "AutoFocusManager";
    
    private SensorManager mSensorManager;
    private Sensor mGravitySensor;
    
    private static final int SPEED_SHRESHOLD = 70;
    private static final int UPTATE_INTERVAL_TIME = 100;
    
    private static final int DEVICE_BECOM_STABLE = 1;
    
    private long mLastUpdateTime;
    private int mGravitySensorTag;
    private float mLastX;
    private float mLastY;
    private float mLastZ;
    
    private boolean mDeviceStable = true;
    private boolean DEBUG = false;
    
    private Handler mHandler;
    private AutoFocusListener mAutoFocusListener;
    private SensorEventListener mGravitySensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Long currentUpdateTime = System.currentTimeMillis();
            Long timeInterval = currentUpdateTime - mLastUpdateTime;
            
            if (mAutoFocusListener == null || !mAutoFocusListener.isWorking()) {
                return;
            }
            
            if (mLastUpdateTime == 0 || timeInterval > UPTATE_INTERVAL_TIME) {
                mLastUpdateTime = currentUpdateTime;
                
                float x = event.values[SensorManager.DATA_X];
                float y = event.values[SensorManager.DATA_Y];
                float z = event.values[SensorManager.DATA_Z];
                
                float deltaX = x - mLastX;
                float deltaY = y - mLastY;
                float deltaZ = z - mLastZ;
                
                mLastX = x;
                mLastY = y;
                mLastZ = z;
                
                double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
                        / timeInterval * 10000;
                
                if (DEBUG) {
                    Log.i(TAG, "Is Moving ..." + " speed=" + speed);
                }
                
                if (speed > SPEED_SHRESHOLD) {
                    Log.e(TAG, "Is Moving ..." + " speed=" + speed);
                    if (mDeviceStable) {
                        mDeviceStable = false;
                        mGravitySensorTag = 0;
                        
                        mHandler.removeMessages(DEVICE_BECOM_STABLE);
                        deviceBeginMoving();
                    } else {
                        Log.e(TAG, "device keep Moving");
                        deviceKeepMoving(speed);
                    }
                } else {
                    mGravitySensorTag += 1;
                    
                    if (mGravitySensorTag == 6) {
                        if (!mDeviceStable) {
                            mHandler.sendEmptyMessage(DEVICE_BECOM_STABLE);
                        }
                        mDeviceStable = true;
                    } else if (mGravitySensorTag > 6) {
                        deviceKeepStable();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    
    public AutoFocusManager(Context context, AutoFocusListener autoFocusListener) {
        mHandler = new MainHandler();
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAutoFocusListener = autoFocusListener;
    }
    
    public void register() {
      mDeviceStable = false;
      mGravitySensorTag = 0; // Added by zhanghongxing at 2013-07-24
      mSensorManager.registerListener(mGravitySensorListener, mGravitySensor, 
              SensorManager.SENSOR_DELAY_UI);
    }

    public void unregister() {
        mSensorManager.unregisterListener(mGravitySensorListener);
    }
    
    public void reset(){
        if (mHandler.hasMessages(DEVICE_BECOM_STABLE)) {
            mHandler.removeMessages(DEVICE_BECOM_STABLE);
        }
        mDeviceStable = true;
        mGravitySensorTag = 0;
    }
    
    private void deviceBecomeStable() {
    	Log.i("lilei","~~&& deviceBecomeStable() mGravitySensorTag:"+mGravitySensorTag);
        mAutoFocusListener.onDeviceBecomeStable();
    }

    private void deviceBeginMoving() {
    	Log.i("lilei","** deviceBeginMoving()");
        mAutoFocusListener.onDeviceBeginMoving();
    }

    private void deviceKeepMoving(double speed) {
    	Log.i("lilei","** deviceKeepMoving() speed:"+speed);
        mAutoFocusListener.onDeviceKeepMoving(speed);
    }

    private void deviceKeepStable() {
    	Log.i("lilei","~~ deviceKeepStable() ~~mGravitySensorTag:"+mGravitySensorTag);
        mAutoFocusListener.onDeviceKeepStable();
    }
    
    public static abstract interface AutoFocusListener {
        public abstract boolean isWorking();
        public abstract void onDeviceBecomeStable();
        public abstract void onDeviceBeginMoving();
        public abstract void onDeviceKeepMoving(double speed);
        public abstract void onDeviceKeepStable();
    }
    
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_BECOM_STABLE: {
                    deviceBecomeStable();
                    break;
                }
                
                default: {
                    break;
                }
            }
        }
    }
}
