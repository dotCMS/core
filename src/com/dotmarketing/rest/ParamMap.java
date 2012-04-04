package com.dotmarketing.rest;

import java.util.HashMap;

import com.dotmarketing.util.UtilMethods;

public class ParamMap<K, V> extends HashMap<K, V>{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int getInt(final String key, final int defaultValue){
		try{
			return Integer.parseInt((String)this.get(key));
			
		}
		catch(final Exception e){
			return defaultValue;
		}
		
	}
	
	public long getLong(final String key, final long defaultValue){
		try{
			return Long.parseLong((String)this.get(key));
			
		}
		catch(final Exception e){
			return defaultValue;
		}
		
	}
	public boolean getBoolean(final String key, final boolean defaultValue){
		try{
			return Boolean.parseBoolean((String) this.get(key));
			
		}
		catch(final Exception e){
			return defaultValue;
		}
		
	}
	
	public String getString(final String key, final String defaultValue){
		return (UtilMethods.isSet(this.get(key))) ? this.get(key).toString() : defaultValue;
			
	}
	public String get(final String key, final String defaultValue){
		return (UtilMethods.isSet(this.get(key))) ? this.get(key).toString() : defaultValue;
			
	}
	
	
}
