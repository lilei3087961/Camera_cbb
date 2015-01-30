package com.android.gallery3d.photoeditor.preview.effects;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * @author 10107896
 * 
 */
public class PictureSession {

    private final static int DEFAULT_WIDTH = 240;
    private final static int DEFAULT_HEIGHT = 320;
    
    private Bitmap realSize;
	private Bitmap original;
	private Bitmap current;
	
	private Effect effect;
	private Border border;

	private ImageView img;

	public PictureSession(Bitmap bm, ImageView img) {
	    double scale = calculateScale(bm.getWidth(), bm.getHeight());
	    
	    realSize = bm;
        original = ColourUtil.scale(bm, scale, scale);
        current = original.copy(Config.ARGB_8888, true);
		
		border = new NullBorder();

		this.img = img;
	}

	

    private double calculateScale(int width, int height) {        
        if(width / DEFAULT_WIDTH >= height / DEFAULT_HEIGHT && width > DEFAULT_WIDTH) {
            return DEFAULT_WIDTH * 1.0 / width;
        } else if(width / DEFAULT_WIDTH < height / DEFAULT_HEIGHT && height > DEFAULT_HEIGHT) {
            return DEFAULT_HEIGHT * 1.0 / height;
        } else {
            return 1;
        }
    }

    public void applyEffect(Effect effect) {
        this.effect = effect;
		current = effect.applyEffect(original);
	}
    
    public Bitmap getCurrentBitmap(){
    	
    	return current;
    }

	public void addBorder(Border border) {
		this.border = border;
	}

	public void draw() {
		img.setVisibility(View.VISIBLE);
		img.setImageBitmap(getCombined(current, false));
	}
	
	public boolean save(Context context) {
		String path = Environment.getExternalStorageDirectory().toString();
		File file = new File(path, "pic.png");

		Bitmap bmp = getCombined(realSize, true);

		try {
			FileOutputStream out = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.PNG, 90, out);

			out.flush();
			out.close();

			MediaStore.Images.Media.insertImage(context.getContentResolver(),
					file.getAbsolutePath(), file.getName(), file.getName());

			Log.d("Save: ", "Successful");
			return true;
		} catch (Exception e) {
			Log.d("Save: ", e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private Bitmap getCombined(Bitmap bm, boolean applyEffect) {
		// Copy effected image
		Bitmap output = bm.copy(Config.ARGB_8888, true);

		if(applyEffect && effect != null) {
		    output = effect.applyEffect(output);
		}
		
		Canvas canvas = new Canvas(output);
		Rect rect = new Rect(0, 0, output.getWidth(), output.getHeight());

		// Draw border on it
		canvas.drawBitmap(
				border.generateBorder(output.getWidth(), output.getHeight()),
				rect, rect, new Paint());

		return output;
	}
}
