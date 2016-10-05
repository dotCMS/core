package com.dotcms.util;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.dotmarketing.util.UtilMethods;

/**
 * Utility class for conversion operations.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 8, 2016
 */
@SuppressWarnings("serial")
public class ConversionUtils implements Serializable {

    public static ConversionUtils INSTANCE =
            new ConversionUtils();

    private ConversionUtils() {}

	/**
	 * Converts from the Original to Destiny bean using a converter.
	 * 
	 * @param origin
	 *            - origin
	 * @param converter
	 *            - {@link Converter}
	 * @param <O>
	 * @param <D>
	 * @return D
	 */
    public <O, D> D convert (final O origin,
                             final Converter<O, D> converter) {

        D d = null;

        if (null != origin && null != converter) {

            d = converter.convert(origin);
        }

        return d;
    } // convert

	/**
	 * Converts from the array of Original objects to Destiny beans using a
	 * converter.
	 * 
	 * @param originArray - 
	 * @param converter - 
	 * @return
	 */
    public <O, D> List<D> convert (final O [] originArray,
                                   final Converter<O, D> converter) {

        List<D> destinyList = null;

        if (null != originArray && null != converter) {

            destinyList = CollectionsUtils.getNewList();

            for (O origin : originArray) {

                destinyList.add(converter.convert(origin));
            }
        }

        return destinyList;
    } // convert

    /**
	 * Converts from the list of Original objects to Destiny beans using a
	 * converter.
	 * 
	 * @param originList - 
	 * @param converter - 
	 * @return
	 */
	public <O, D> List<D> convert(final List<O> originList, final Converter<O, D> converter) {

		List<D> destinyList = null;

		if (null != originList && null != converter) {

			destinyList = CollectionsUtils.getNewList();

			for (O origin : originList) {

				destinyList.add(converter.convert(origin));
			}
		}

		return destinyList;
	} // convert

	/**
	 * 
	 * @param sLong
	 * @return
	 */
	public static long toLong (final String sLong) {

		return toLong(sLong, 0l);
	}

	/**
	 * 
	 * @param input
	 * @param defaultLong
	 * @return
	 */
	public static long toLong (final Object input, final Long defaultLong) {

		long l = defaultLong;

		try {
			if (UtilMethods.isSet(input)) {
				if (input instanceof String) {
					l = Long.parseLong(String.class.cast(input));
				} else if (input instanceof Number) {
					l = Number.class.cast(input).longValue();
				}
			}
		} catch (NumberFormatException e) {

			l = defaultLong;
		}

		return l;
	}

	/**
	 * Converts the specified map value into an {@code int}.
	 * 
	 * @param key
	 *            - The key to the map value.
	 * @param params
	 *            - The Map that contains the value to convert.
	 * @param defaultInt
	 *            - The default value in case the map doesn't have it, or if it
	 *            cannot be converted.
	 * @return The map value as {@code int}, or the default value.
	 */
	public static int toInt(final String key, final Map<?, ?> params, final int defaultInt) {
		int result = defaultInt;
		if (params.containsKey(key)) {
			result = toInt(params.get(key).toString(), defaultInt);
		}
		return result;
	}

	/**
	 * Converts the specified input value into an {@code int}. The input value
	 * can be a String or an instance of {@link Number}.
	 * 
	 * @param input
	 *            - The value to convert.
	 * @param defaultInt
	 *            - The default value in case the input cannot be converted.
	 * @return The input as {@code int}, or the default value.
	 */
	public static int toInt(final Object input, final int defaultInt) {
		try {
			if (input instanceof String) {
				return Integer.parseInt(String.class.cast(input));
			} else if (input instanceof Number) {
				return Number.class.cast(input).intValue();
			} else {
				return defaultInt;
			}
		} catch (NumberFormatException e) {
			return defaultInt;
		}
	}

	/**
	 * Converts the specified map value into a {@code boolean}.
	 * 
	 * @param key
	 *            - The key to the map value.
	 * @param params
	 *            - The Map that contains the value to convert.
	 * @param defaultBool
	 *            - The default value in case the map doesn't have it, or if it
	 *            cannot be converted.
	 * @return The map value as {@code boolean}, or the default value.
	 */
	public static boolean toBoolean(final String key, final Map<?, ?> params, final boolean defaultBool) {
		boolean result = defaultBool;
		if (params.containsKey(key)) {
			result = toBoolean(params.get(key).toString(), defaultBool);
		}
		return result;
	}

	/**
	 * Converts the specified input into a {@code boolean}.
	 * 
	 * @param strBool
	 *            - The String representation of the boolean.
	 * @param defaultBool
	 *            - The default value in case the input cannot be converted.
	 * @return The input as {@code defaultBool}, or the default value.
	 */
	public static boolean toBoolean(final String strBool, final boolean defaultBool) {
		try {
			return Boolean.getBoolean(strBool);
		} catch (Exception e) {
			return defaultBool;
		}
	}

}
