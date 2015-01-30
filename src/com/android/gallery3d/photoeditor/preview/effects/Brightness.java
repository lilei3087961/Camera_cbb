/**
 *
 * Copyright (c) 2009-2012 Envision Mobile Ltd. All rights reserved.
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
 * Module: Brightness.java
 * Project: Pixem
 *
 * Description:
 *
 *
 * Developer:   10107896
 * Date:        2012-07-29
 * Version:		1.0
 *
 *
 */
package com.android.gallery3d.photoeditor.preview.effects;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;

/**
 * @author 10107896
 *
 */
public class Brightness implements Effect {

	//TODO: we need to add a slideer for brightness and so on
	private int value = 0;
	
	/* (non-Javadoc)
	 * @see com.pixem.effects.Effect#applyEffect(android.graphics.Bitmap)
	 */
	@Override
	public Bitmap applyEffect(Bitmap bm) {
		
		Bitmap modifiedBitmap = bm.copy(Config.ARGB_8888, true);
		
		int alpha = 0, red = 0, blue = 0, green = 0;
		int pixel = 0;
		
		try { 
			for (int i = 0; i < modifiedBitmap.getWidth(); i++) { 
				for (int j = 0; j < modifiedBitmap.getHeight(); j++) { 
					pixel = modifiedBitmap.getPixel(i, j);
					alpha = Color.alpha(pixel);
					red = Color.red(pixel);
					green = Color.green(pixel);
					blue = Color.blue(pixel);
					
					red += value;
					green += value;
					blue += value;
					
					if (red > 255)
						red = 255;
					else if (red < 0)
						red = 0;
					
					if (green > 255)
						green = 255;
					else if (green < 0)
						green = 0;
					
					if (blue > 255) 
						blue = 255;
					else if (blue < 0)
						blue = 0;
					
					modifiedBitmap.setPixel(i, j, Color.argb(alpha, red, green, blue));
				}
			}
			
			return modifiedBitmap;
		} catch (Exception e) { 
			e.printStackTrace();
			return null;
		}
	}
	
	public void setValue(int value) { 
		this.value = value;
	}

}
