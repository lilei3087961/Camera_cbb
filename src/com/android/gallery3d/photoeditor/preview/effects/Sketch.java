/**
 *
 * Copyright (c) 2009-2011 Envision Mobile Ltd. All rights reserved.
 *
 * Other software and company names mentioned herein or used for developing
 * and/or running the Envision Mobile Ltd's software may be trademarks or trade
 * names of their respective owners.
 *
 * Everything in the source code herein is owned by Envision Mobile Ltd.
 * The recipient of this source code hereby acknowledges and agrees that such
 * information is proprietary to Envision Mobile Ltd. and shall not be used, 
 * disclosed, duplicated, and/or reversed engineered except in accordance
 * with the express written authorization of Envision Mobile Ltd.
 *
 * Module: BlackAndWhite.java
 * Project: Pixem
 *
 * Description:
 *
 *
 * Developer:   Saman Alvi
 * Date:        2011-11-08
 * Version:
 *
 *
 */
package com.android.gallery3d.photoeditor.preview.effects;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;


public class Sketch implements Effect {
	
	public Bitmap applyEffect(Bitmap bm) { 
		
		
		int clr, red = 0, blue = 0, green = 0;
			Bitmap modifiedBitmap = bm.copy(Config.ARGB_8888, true);
			
		for (int i = 0; i < bm.getWidth(); i++) { 
			for (int j = 0; j < bm.getHeight(); j++) { 
				clr = modifiedBitmap.getPixel(i, j);
				
                red = Color.red(clr);
            	green = Color.green(clr);
            	blue = Color.blue(clr);
            	
            	if((red+blue+green)/3 < 128) {
            	    modifiedBitmap.setPixel(i, j, Color.BLACK);
            	} else {
            	    modifiedBitmap.setPixel(i, j, Color.WHITE);
            	}
            }
		}
		
		return modifiedBitmap;
	}
}

