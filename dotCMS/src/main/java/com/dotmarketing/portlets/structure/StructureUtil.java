package com.dotmarketing.portlets.structure;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class StructureUtil {

	public static String generateRegExForURLMap(String urlMapString){
		StringBuilder pattern = new StringBuilder();
		String[] urlFrags = urlMapString.split("/");
		for (String frag : urlFrags) {
			if(UtilMethods.isSet(frag)){
				pattern.append("/");
				if(!frag.startsWith("{")){
					pattern.append(frag);
				}else{
					pattern.append("(.+?)");
				}
			}
		}
		if(UtilMethods.isSet(pattern.toString())){
			pattern.append("/");
		}
		return pattern.toString();
	}
	/**
	 * This method will return a valid URLMapped URL
	 * for a contentlet if it exists.  Else, it will
	 * return null
	 * 
	 * @param con
	 * @return
	 */
	
	public static String getURLMapForContentlet(Contentlet con){
		if(con == null) return null;
		Structure s = con.getStructure();
		
		if(s == null 
				|| !UtilMethods.isSet(s.getUrlMapPattern()) 
				|| s.getUrlMapPattern().length() < 3) return null;
		
		if(true)
			throw new DotRuntimeException("I AM JUST A STUB AND NEED TO BE WRITTEN!!");
		// FINISH ME!!!
		
		return null;
		
		
	}
	
	
	
	
	
	
}
