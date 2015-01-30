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
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;

/**
 * @author 10107896
 *         http://xjaphx.wordpress.com/2011/11/01/image-processing-image
 *         -reflection-effect/
 */
public class HighlightFilter implements Effect {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.pixem.effects.Effect#applyEffect(android.graphics.Bitmap)
	 */

	private Bitmap image;

	private int width;

	private int height;

	private int[] colorArray;

	public int getPixelColor(int x, int y) {
		return colorArray[y * width + x];
	}

	@Override
	public Bitmap applyEffect(Bitmap bm) {

		image = bm;
		width = bm.getWidth();
		height = bm.getHeight();

		colorArray = new int[width * height];
		image.getPixels(colorArray, 0, width, 0, 0, width, height);

		int a;
		int r;
		int g;
		int b;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {

				int c = getPixelColor(x, y);

				a = Color.alpha(c);
				r = Color.red(c);
				g = Color.green(c);
				b = Color.blue(c);

				r = (int) (r * 0.8);
				g = (int) (g * 1.6);
				b = (int) (b * 0.8);

				if (r > 255) {
					r = 255;
				}
				if (r < 0) {
					r = 0;
				}
				if (g > 255) {
					g = 255;
				}
				if (g < 0) {
					g = 0;
				}
				if (b > 255) {
					b = 255;
				}
				if (b < 0) {
					b = 0;
				}

				int resultColor = Color.argb(a, r, g, b);
				image.setPixel(x, y, resultColor);
			}
		}
		return image;
	}

}
