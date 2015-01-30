package com.android.gallery3d.photoeditor.preview.effects;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * @author Saman Alvi
 *
 */
public class ColourUtil {
	
	public static int average (int colour1, int colour2, double percent) { 
		
		try { 
			return Color.argb(255, (int) Math.round(Color.red(colour1) * percent + Color.red(colour2) * (1 - percent)), (int) Math.round(Color.green(colour1) * percent + Color.green(colour2) * (1 - percent)), (int) Math.round(Color.blue(colour1) * percent + Color.blue(colour2) * (1 - percent)));
		} catch (Exception e) { 
			e.printStackTrace();
			return 0;
		}
		
	}
	
	public static Bitmap switchBlueGreen(Bitmap bm) { 
		
		int colour = 0, blue = 0, green = 0;
		if (bm != null) { 
			Bitmap modifiedBitmap = bm.copy(Config.ARGB_8888, true);
			
			for (int i = 0; i < modifiedBitmap.getWidth(); i++) { 
				for (int j = 0; j < modifiedBitmap.getHeight(); j++) { 
					
					colour = modifiedBitmap.getPixel(i, j);
					blue = Color.blue(colour);
					green = Color.green(colour);
					
					modifiedBitmap.setPixel(i, j, Color.argb(255, Color.red(colour), blue, green));
				}
			}
			
			return modifiedBitmap;
		}
		
		return null;
	}
	
	public static Bitmap scale(Bitmap bm, double widthScale, double heightScale) {
        return Bitmap.createScaledBitmap(bm, (int)(bm.getWidth() * widthScale), (int)(bm.getHeight() * heightScale), false);
    }
	
	public static Drawable scaleDrawable(Drawable drawable, double d, double e) {
		
	    Bitmap bm = ((BitmapDrawable) drawable).getBitmap();
	    
	    return (new BitmapDrawable(scale(bm, d, e)));
	
	}
}
