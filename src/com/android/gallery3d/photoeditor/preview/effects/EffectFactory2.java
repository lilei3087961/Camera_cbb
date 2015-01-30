package com.android.gallery3d.photoeditor.preview.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.graphics.Color;


public class EffectFactory2 {
	
	private List<Effect> effects;
	private HashMap<String, Effect > mEffectsMap = new HashMap<String, Effect>();
		
	public static final String EFFECT_AUTOFIX= "autofix";		
	public static final String EFFECT_SHADOW= "shadow";	
	public static final String EFFECT_FILLLIGHT= "filllight";	
	public static final String EFFECT_HIGHLIGHT= "highlight";
	
	
	public static final String EFFECT_TEMPERATURE= "temperature";	
	public static final String EFFECT_SATURATION= "saturation";	
	public static final String EFFECT_GRAYSCALE= "grayscale";	
	public static final String EFFECT_SEPIA= "sepia";		
	public static final String EFFECT_NEGATIVE= "negative";	
	public static final String EFFECT_TINT= "tint";		
	public static final String EFFECT_DUOTONE= "duotone";	
	public static final String EFFECT_DOODLE= "doodle";		
	

	public static final String EFFECT_CROSSPROCESS= "crossprocess";	
	public static final String EFFECT_POSTERIZE= "posterize";		
	public static final String EFFECT_LOMOISH= "lomoish";	
	public static final String EFFECT_DOCUMENTTARY= "documentary";		
	public static final String EFFECT_VIGNETTE= "vignette";	
	public static final String EFFECT_GRAIN= "grain";		
	public static final String EFFECT_FISHEYE= "fisheye";	
	
	
	public EffectFactory2() {
		
		//OK
		mEffectsMap.put(EFFECT_FISHEYE, new Contrast());
		mEffectsMap.put(EFFECT_SEPIA, new Sepia());
		mEffectsMap.put(EFFECT_AUTOFIX, new NullEffect());
		mEffectsMap.put(EFFECT_GRAYSCALE, new BlackAndWhite());//		
		
		mEffectsMap.put(EFFECT_CROSSPROCESS, new NullEffect());
		mEffectsMap.put(EFFECT_DOCUMENTTARY, new Sketch());
		mEffectsMap.put(EFFECT_DOODLE, new NullEffect());
		mEffectsMap.put(EFFECT_DUOTONE, new ColourFilter(Color.argb(255, 70, 70, 0)));
		mEffectsMap.put(EFFECT_FILLLIGHT, new ColourFilter(Color.argb(255, 70, 0, 0)));
		mEffectsMap.put(EFFECT_GRAIN, new ColourFilter(Color.argb(255, 70, 0, 0)));
		mEffectsMap.put(EFFECT_HIGHLIGHT, new ColourFilter(Color.argb(255, 70, 60, 0)));
		mEffectsMap.put(EFFECT_LOMOISH, new ColourFilter(Color.argb(255, 0, 170, 60)));
		mEffectsMap.put(EFFECT_NEGATIVE, new ColourFilter(Color.argb(255, 0, 70, 0)));
		mEffectsMap.put(EFFECT_POSTERIZE, new ColourFilter(Color.argb(255, 70, 70, 70)));
		mEffectsMap.put(EFFECT_SATURATION, new ColourFilter(Color.argb(255, 0, 0, 70)));
		mEffectsMap.put(EFFECT_SHADOW, new ColourFilter(Color.argb(255, 70, 100, 0)));
		mEffectsMap.put(EFFECT_TEMPERATURE, new ColourFilter(Color.argb(255, 70, 0, 100)));
		mEffectsMap.put(EFFECT_TINT, new ColourFilter(Color.argb(255, 70, 60, 100)));
		mEffectsMap.put(EFFECT_VIGNETTE, new Smooth());
		
		//OK			
	}
	
	public Effect getEffectByKey(String key){
		return mEffectsMap.get(key);
	}
	
	
	public int getNumberOfEffects() {
		return effects.size();
	}
	
	
	
	public Effect getEffect(int id) {
		return effects.get(id);
	}
}
