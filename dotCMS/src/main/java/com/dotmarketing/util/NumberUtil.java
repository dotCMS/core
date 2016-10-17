package com.dotmarketing.util;

import java.text.DecimalFormat;

public class NumberUtil {

	private static final DecimalFormat formatter = new DecimalFormat("0000000000000000000.000000000000000000"); 

	/**
	 * Will pad a number/decimal to 64bit meaning 19 characters.
	 * @param n
	 * @return
	 */
	public static String pad(Number n) { 
		return formatter.format(n); 			
	}
	
}
