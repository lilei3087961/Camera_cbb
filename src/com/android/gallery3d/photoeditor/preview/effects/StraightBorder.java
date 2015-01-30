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
 * Module: StraightBorder.java
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

/**
 * @author Saman Alvi
 *
 */
public class StraightBorder implements Border {   

	private int color;
	
	public StraightBorder() { 
		this(0xff990099);
	}
	
	public StraightBorder (int color) { 
		this.color = color;
	}
	
	public void setColor(int color) { 
		this.color = color;
	}
	
	public int getColor() { 
		return color;
	}

	@Override
	public Bitmap generateBorder(int width, int height) {
			
		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		
		for (int i = 0; i < output.getWidth(); i++) { 
			for (int j = 0; j < output.getHeight(); j++) { 
                if (i < width / 15 || j < width / 15 || i > width - width / 15
                        || j > height - width / 15) {
                	output.setPixel(i, j, color);
                } else {
                	// hopefully 0 is transparent
                	output.setPixel(i, j, 0);
                }
			}
		}
		
		return output;
	}
}
