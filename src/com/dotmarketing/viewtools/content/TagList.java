/**
 * 
 */
package com.dotmarketing.viewtools.content;

import java.util.ArrayList;

import com.dotmarketing.util.UtilMethods;

/**
 * Used as a wrapper around an ArrayList of Tags to return to the front-end of dotCMS from the 
 * ContentTool. 
 * 
 * @author Jason Tesser
 * @since 1.9.1.3
 *
 */
public class TagList extends ArrayList<String> {

	protected TagList(String tagValue) {
		super();
		if(UtilMethods.isSet(tagValue)){
			for(String t : tagValue.split(",")){
				add(t.trim());
			
			}
		}
		
	}
	
	/**
	 * The raw Tag values is a comma separated list of the selected tags.  
	 */
	private String rawTagValues;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2259803707187904180L;

	/**
	 * @param rawTagValues the rawTagValues to set
	 */
	protected void setRawTagValues(String rawTagValues) {
		this.rawTagValues = rawTagValues;
	}

	/**
	 * @return the rawTagValues
	 */
	public String getRawTagValues() {
		return rawTagValues;
	}
	
}
