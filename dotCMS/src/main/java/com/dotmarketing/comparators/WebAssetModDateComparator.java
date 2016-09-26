package com.dotmarketing.comparators;

import java.util.Comparator;

import com.dotmarketing.beans.WebAsset;

/**
 * @author Maria
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class WebAssetModDateComparator implements Comparator {

    private String orderType = "";
	
	public WebAssetModDateComparator () {
		
	}
	
	public WebAssetModDateComparator (String direction) {
		orderType = direction;
	}
	
  	public int compare(Object o1, Object o2) {

		try {

				int result;
				
				WebAsset w1 = (WebAsset) o1;
				WebAsset w2 = (WebAsset) o2;
				
	            result = (w1.getModDate().equals(w2.getModDate())) ? 0 : (w1.getModDate().before(w2.getModDate())) ? -1 : 1;

	            if (orderType.equals("asc"))
	            	return -result;
	            else
	            	return result;
	            
			} catch (ClassCastException e) {
			}
		return 0;
	}
}
