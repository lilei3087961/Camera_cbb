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
 * Module: Reflection.java
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
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;

/**
 * @author 10107896
 * http://xjaphx.wordpress.com/2011/11/01/image-processing-image-reflection-effect/
 */
public class Reflection implements Effect {

	/* (non-Javadoc)
	 * @see com.pixem.effects.Effect#applyEffect(android.graphics.Bitmap)
	 */
	@Override
	public Bitmap applyEffect(Bitmap bm) {
		
		try { 
			Bitmap modifiedBitmap = bm.copy(Config.ARGB_8888, true);
			int reflectionGap = 4;
			
			Matrix matrix = new Matrix();
			matrix.preScale(1, -1);
			
		    Bitmap reflectionImage = Bitmap.createBitmap(modifiedBitmap, 0, 
		    		modifiedBitmap.getHeight()/2, modifiedBitmap.getWidth(), modifiedBitmap.getHeight()/2, 
		    		matrix, false);   
			
		    // create a new bitmap with same width but taller to fit reflection
		    Bitmap bitmapWithReflection = Bitmap.createBitmap(modifiedBitmap.getWidth(), 
		    		(modifiedBitmap.getHeight()+ modifiedBitmap.getHeight()/2), Config.ARGB_8888);
		    
		    // create a new Canvas with the bitmap that's big enough for
		    // the image plus gap plus reflection
		    Canvas canvas = new Canvas(bitmapWithReflection);
		    // draw in the original image
		    canvas.drawBitmap(modifiedBitmap, 0, 0, null);
		    // draw in the gap
		    Paint defaultPaint = new Paint();
		    canvas.drawRect(0, modifiedBitmap.getHeight(), modifiedBitmap.getWidth(), 
		    		modifiedBitmap.getHeight()+ reflectionGap, defaultPaint);
		    // draw in the reflection
		    canvas.drawBitmap(reflectionImage,0, modifiedBitmap.getHeight() + reflectionGap, null);
		      
		    // create a shader that is a linear gradient that covers the reflection
		    Paint paint = new Paint();
		    LinearGradient shader = new LinearGradient(0, modifiedBitmap.getHeight(), 0,
		            bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff, 0x00ffffff,
		            TileMode.CLAMP);
		    // set the paint to use this shader (linear gradient)
		    paint.setShader(shader);
		    // set the Transfer mode to be porter duff and destination in
		    paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		    // draw a rectangle using the paint with our linear gradient
		    canvas.drawRect(0, modifiedBitmap.getHeight(), modifiedBitmap.getWidth(), 
		    		bitmapWithReflection.getHeight() + reflectionGap, paint);
		    
			return bitmapWithReflection;
		} catch (Exception e) { 
			e.printStackTrace();
			return null;
		}

	}

}
