package com.android.camera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.view.Window;
import android.view.WindowManager;
import android.provider.Settings;
import android.content.Context;
import android.app.Activity;
import android.util.Log;

public class CameraDefaultValueManager {
	private final String TAG = "CameraDefaultValueManager";
	
	private Map<String, String> mDefaultValueMap;
	private Context mContext;
	
	private final String DEFAULTSCREENBRIGHTNESS = "camera_default_screen_brightness_value";
	
	public CameraDefaultValueManager(Context context) {
		mContext = context;
		mDefaultValueMap = new HashMap<String, String>();
	}
	public void loadCameraDefaultValueFromFile() {
		String fileName = "/system/etc/camera_default_values.config";
		File file = new File(fileName);
		if (!file.exists()) {
			Log.d(TAG, fileName + " doesn't exist ");
			return;
		}

		try {
			BufferedReader b = new BufferedReader(new FileReader(fileName));
			String line;
			do {
				line = b.readLine();
				String[] pair = line.split("=");
				if (pair.length == 2) {
					mDefaultValueMap.put(pair[0], pair[1]);
				}
			} while (line != null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setDefaultScreenBrightness() {
		Window localWindow = ((Activity)mContext).getWindow();
		String brightness = (String)mDefaultValueMap.get(DEFAULTSCREENBRIGHTNESS);
		int brightValue = 200;
		if(brightness != null) {
			brightValue = Integer.valueOf(brightness);
		}
		WindowManager.LayoutParams localLayoutParams = localWindow
				.getAttributes();
		float f = brightValue / 255.0F;
		localLayoutParams.screenBrightness = f;
		localWindow.setAttributes(localLayoutParams);
	}

	public int getScreenBrightness() {
		int screenBrightness = 255;
		try {
			screenBrightness = Settings.System.getInt(mContext.getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS);
		} catch (Exception localException) {

		}
		return screenBrightness;
	}
}
