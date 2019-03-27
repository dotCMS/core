/*
 * Created on Oct 21, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.dotmarketing.comparators;

import java.util.Comparator;

/**
 * @author alex
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LanguageManagerComparator implements Comparator {

    
    
  	public int compare(Object o1, Object o2) {
  		int comp = 0;
		try {
			String str1 = (String) o1;
			String str2 = (String) o2;
			comp = str1.compareTo(str2);
		}catch (ClassCastException e) {
			
		}
		return comp;

  	}	
}
