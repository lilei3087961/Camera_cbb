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
 * Module: ColoredSquares.java
 * Project: Pixem
 *
 * Description:
 *
 *
 * Developer:   10107896
 * Date:        2012-03-20
 * Version:
 *
 *
 */
package com.android.gallery3d.photoeditor.preview.effects;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

/**
 * @author 10107896
 *
 */
public class ColoredSquares implements Effect {

	/* (non-Javadoc)
	 * @see com.pixem.effects.Effect#applyEffect(android.graphics.Bitmap)
	 */
	@Override
	public Bitmap applyEffect(Bitmap bm) {
		
		Bitmap newBitmap = bm.copy(Config.ARGB_8888, true);
		
		Bitmap b1 = ColourUtil.scale(bm, 0.5, 0.5);
		Bitmap b2 = ColourUtil.scale(bm, 0.5, 0.5);
		Bitmap b3 = ColourUtil.scale(bm, 0.5, 0.5);
		Bitmap b4 = ColourUtil.scale(bm, 0.5, 0.5);

		Log.d("", "- images resized, b1 is: " + b1.getWidth() + " " + b1.getHeight());
		
		
		ColourFilter filter = new ColourFilter(Color.argb(255, 196, 50, 50));
		b1 = filter.applyEffect(b1);
		
		filter.setColour(Color.argb(255, 50, 108, 50));
		b2 = filter.applyEffect(b2);
		
		filter.setColour(Color.argb(255, 50, 50, 128));
		b3 = filter.applyEffect(b3);
		
		filter.setColour(Color.argb(255, 140, 40, 140));
		b4 = filter.applyEffect(b4);
		
		
		// Get your images from their files
		//Bitmap bottomImage = new BitmapFactory.decodeFile("myFirstPNG.png");
		//Bitmap topImage = new BitmapFactor.decodeFile("myOtherPNG.png");

		//http://stackoverflow.com/questions/2738834/combining-two-png-files-in-android
		// As described by Steve Pomeroy in a previous comment, 
		// use the canvas to combine them.
		// Start with the first in the constructor..
		//Canvas comboImage = new Canvas(bottomImage);
		// Then draw the second on top of that
		//comboImage.drawBitmap(topImage, 0f, 0f, null);
		
		Canvas comboImage = new Canvas(newBitmap);
		comboImage.drawBitmap(b1, 0, 0, null);
		comboImage.drawBitmap(b2, b1.getWidth(), 0, null);
		comboImage.drawBitmap(b3, 0, b1.getHeight(), null);
		comboImage.drawBitmap(b4, b1.getWidth(), b1.getHeight(), null);
		
		return newBitmap;
	}

}
