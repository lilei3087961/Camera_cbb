package com.android.gallery3d.photoeditor.preview.effects;

import android.graphics.Bitmap;

public class NullEffect implements Effect {

	@Override
	public Bitmap applyEffect(Bitmap bm) {
		return bm;
	}

}
