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
 * Module: Duplicate.java
 * Project: Pixem
 *
 * Description:
 *
 *
 * Developer:   Saman Alvi
 * Date:        2011-11-11
 * Version:
 *
 *
 */
package com.android.gallery3d.photoeditor.preview.effects;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

/**
 * @author Saman Alvi
 *
 */
public class Duplicate implements Effect {
	
	@Override
	public Bitmap applyEffect(Bitmap bm) {
		Bitmap modifiedBitmap = bm.copy(Config.ARGB_8888, true);
		
		for (int x = 0; x < modifiedBitmap.getWidth(); x++) { 
			for (int y = 0; y < modifiedBitmap.getHeight(); y++) { 

				for (int k = 0; k < 25; k++) { 
					
				}
				
			}
		}
		
		return modifiedBitmap;
	}
}
