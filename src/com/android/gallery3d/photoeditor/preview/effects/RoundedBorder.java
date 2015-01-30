/**
 *
 * Module: RoundedBorder.java
 * Project: Pixem
 *
 * Description:
 *
 * Developer:   Saman Alvi
 * Date:        2011-11-08
 * Version:		1.0
 *
 *
 */
package com.android.gallery3d.photoeditor.preview.effects;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * @author Saman Alvi
 *
 */
public class RoundedBorder implements Border {

	private final static int DEFAULT_ARC_WIDTH = 5;
	private final static int DEFAULT_ARC_HEIGHT = 5;
	
	
	private int arcWidth;
	private int arcHeight;
	private int borderColor;
	
	public RoundedBorder() { 
		this(0xff990099, DEFAULT_ARC_WIDTH, DEFAULT_ARC_HEIGHT);
	}
	
	public RoundedBorder(int colour) { 
		this(colour, DEFAULT_ARC_WIDTH, DEFAULT_ARC_HEIGHT);
	}
	
	public RoundedBorder (int color, int arcWidth, int arcHeight) { 
		borderColor = color;
		this.arcWidth = arcWidth;
		this.arcHeight = arcHeight;
	}
	
	public void setBorderColor(int color) { 
		borderColor = color;
	}
	
	public void setArcWidthHeight(int arcWidth, int arcHeight) { 
		this.arcHeight = arcHeight;
		this.arcWidth = arcWidth;
	}
	
	public int getColor() { 
		return borderColor;
	}
	
	public int getArcWidth() { 
		return arcWidth;
	}
	
	public int getArcHeight() { 
		return arcHeight;
	}

	/*
	 * http://ruibm.com/?p=184
	 */
	@Override
	public Bitmap generateBorder(int width, int height) {		
		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		Paint paint = new Paint();
		Rect rect = new Rect(0, 0, width, height);
		RectF rectF = new RectF(width/15, width/15, width - width/15, height -width/15);
		
		
		canvas.drawColor(borderColor);
		
		canvas.drawRoundRect(rectF, width / arcWidth, width / arcHeight, paint);
		
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(output, rect, rect, paint);
		
		for(int x=0;x<width;x++) {
		    for(int y=0;y<height;y++) {
		        if(output.getPixel(x, y) == Color.BLACK) {
		        	output.setPixel(x, y, Color.TRANSPARENT);
		        }
		    }
		}
		
		return output;
	}
}
