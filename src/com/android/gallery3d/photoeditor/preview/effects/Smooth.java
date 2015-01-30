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
 * Module: Smooth.java
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


/**
 * @author Saman Alvi
 *
 */
public class Smooth implements Effect {
	
	/* (non-Javadoc)
	 * @see com.android.pixem.org.Effect#applyEffect()
	 */
	@Override
	public Bitmap applyEffect(Bitmap bm) {
		Bitmap modifiedBitmap = bm.copy(Config.ARGB_8888, true);
        
		PictureMatrix m = new PictureMatrix();
        int red = 0, green = 0, blue = 0;

        int matrixSize = m.getMatrixSize();

        double[][] redPixel = new double[matrixSize][matrixSize],
                greenPixel = new double[matrixSize][matrixSize],
                bluePixel = new double[matrixSize][matrixSize];

        m.setValues(1);
        m.setMiddle(8);
        m.setFactor(9);
		
		for (int i = 0; i < modifiedBitmap.getWidth() - 2; i++) { 
			for (int j = 0; j < modifiedBitmap.getHeight() - 2; j++) { 
				
				for (int x = 0; x < matrixSize; x++) { 
					for (int y = 0; y < matrixSize; y++) { 
	                    redPixel[x][y] = Color.red(modifiedBitmap.getPixel(i + x, j + y));
						greenPixel[x][y] = Color.green(modifiedBitmap.getPixel(i + x, j + y));
						bluePixel[x][y] = Color.blue(modifiedBitmap.getPixel(i + x, j + y));
						
                        red = 0;
                        green = 0;
                        blue = 0;

                        for (int k = 0; k < matrixSize; k++) {
                            for (int n = 0; n < matrixSize; n++) {
                                red += (int) (redPixel[k][n] * m.matrix[k][n]);
                                green += (int) (greenPixel[k][n] * m.matrix[k][n]);
                                blue += (int) (bluePixel[k][n] * m.matrix[k][n]);
                            }
                        }
                        
                        red = (int) ((red / m.factor) + 5);
                        if (red > 255) {
                            red = 255;
                        }
                        if (red < 0) {
                            red = 0;
                        }
                        
                        green = (int) ((green / m.factor) + 5);
                        if (green > 255) {
                            green = 255;
                        }
                        if (green < 0) {
                            green = 0;
                        }

                        blue = (int) ((blue / m.factor) + 5);
                        if (blue > 255) {
                            blue = 255;
                        }
                        if (blue < 0) {
                            blue = 0;
                        }
                        
					}
				}
				modifiedBitmap.setPixel(i + 1, j + 1, Color.argb(Color.alpha(modifiedBitmap.getPixel(i, j)), red, green, blue));
			}
		}
		
		return modifiedBitmap;
	}
}

