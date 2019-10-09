package com.dotmarketing.util;

import java.text.DecimalFormat;
import java.util.function.Supplier;

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

	/**
	 * try to convert to integer the string, if any error will return the defaultOne.
	 * @param sInt {@link String}
	 * @param defaultOne supplier int
	 * @return int
	 */
	public static int toInt (final String sInt, final Supplier<Integer> defaultOne) {

		try {
			return (UtilMethods.isSet(sInt))?
				Integer.parseInt(sInt):defaultOne.get();
		} catch(NumberFormatException e) {
			return defaultOne.get();
		}
	} // toInt.

	/**
	 * try to convert to long the string, if any error will return the defaultOne.
	 * @param sLong {@link String}
	 * @param defaultOne supplier long
	 * @return long
	 */
	public static long toLong (final String sLong, final Supplier<Long> defaultOne) {

		try {
			return (UtilMethods.isSet(sLong))?
					Long.parseLong(sLong):defaultOne.get();
		} catch(NumberFormatException e) {
			return defaultOne.get();
		}
	} // toLong.

    /**
     * try to convert to boolean the string, if any error will return the defaultOne.
     * @param sBoolean {@link String}
     * @param defaultOne supplier boolean
     * @return Boolean
     */
    public static Boolean toBoolean (final String sBoolean, final Supplier<Boolean> defaultOne) {

        try {
            return UtilMethods.isSet(sBoolean)?
                    Boolean.valueOf(sBoolean):defaultOne.get();
        } catch(NumberFormatException e) {
            return defaultOne.get();
        }
    } // toBoolean.
	
} // E:O:F:NumberUtil
