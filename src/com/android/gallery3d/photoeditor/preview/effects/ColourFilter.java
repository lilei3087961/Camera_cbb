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
 * Module: ColourFilter.java
 * Project: Pixem
 *
 * Description:
 *
 *
 * Developer:   Saman Alvi
 * Date:        2011-11-10
 * Version:
 *
 *
 */
package com.android.gallery3d.photoeditor.preview.effects;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;

/**
 * @author Saman Alvi
 *
 */
public class ColourFilter implements Effect {

	private int colour;
	
	public ColourFilter(int colourChoice) { 
		colour = colourChoice;
	}

	public void setColour (int colour) { 
		this.colour = colour;
	}
	
	public int getColour() { 
		return colour;
	}
	
	@Override
	public Bitmap applyEffect(Bitmap bm) {
			
		Bitmap modifiedBitmap = bm.copy(Config.ARGB_8888, true);
		
		int red, green, blue, color;
		
		for (int x = 0; x < modifiedBitmap.getWidth(); x++) { 
			for (int y = 0; y < modifiedBitmap.getHeight(); y++) { 

				color = bm.getPixel(x, y);
				red = Color.red(color) + Color.red(colour);
				green = Color.green(color) + Color.green(colour);
				blue = Color.blue(color) + Color.blue(colour);
				
				if (red > 255)
					red = 255;
				
				if (green > 255)
					green = 255;
				
				if (blue > 255)
					blue = 255;
				
				modifiedBitmap.setPixel(x, y, Color.argb(Color.alpha(color), red, green, blue));
			}
		}
		
		return modifiedBitmap;	
	}	
}
