package com.dotcms.util;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
	 * Return a List
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
	 * Converts from the array of Original objects to Destiny beans using a
	 * converter.
	 * Returns an array
	 *
	 * @param originArray -
	 * @param converter -
	 * @return
	 */
	public <O, D> D[] convertToArray (final Converter<O, D> converter, final Class<D> clazz, final O... originArray) {

		final D[] destinyArray =  (D[]) Array.newInstance(clazz, originArray.length);

		for (int i = 0; i < originArray.length; ++i) {

			destinyArray[i] = converter.convert(originArray[i]);
		}

		return destinyArray;
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
	 * Converts the specified input value into an {@code long}. The input value
	 * can be a String or an instance of {@link Number}.
	 * @param input
	 *         - The value to convert.
	 * @param defaultLong
	 *        - The default value in case the input cannot be converted.
	 * @return long value
	 */
	public static long toLong (final Object input, final Long defaultLong) {

		long resultLong = defaultLong;

		try {
			if (UtilMethods.isSet(input)) {
				if (input instanceof CharSequence) {
					resultLong = Long.parseLong(input.toString());
				} else if (input instanceof Number) {
					resultLong = Number.class.cast(input).longValue();
				}
			}
		} catch (NumberFormatException e) {

			resultLong = defaultLong;
		}

		return resultLong;
	}

	/**
	 * Converts 1kb to 1024
	 * Converts 1mb to 1024 * 1024
	 * Converts 1gb to 1024 * 1024 * 1024
	 * @param humanDisplaySize String human display size such as 100, 1kb, 2mb, 3gb, etc
	 * @param defaultLong long default long in case the humanDisplaySize can not be parsed
	 * @return long
	 */
	public static long toLongFromByteCountHumanDisplaySize (final String humanDisplaySize, final long defaultLong) {

		if (UtilMethods.isSet(humanDisplaySize) && humanDisplaySize.length() > 2) {

			final String postfix     = humanDisplaySize.substring(humanDisplaySize.length()-2);
			final String stringValue = humanDisplaySize.substring(0, humanDisplaySize.length()-2);
			final long  value        = toLong(stringValue, defaultLong);
			switch (postfix.toLowerCase()) {

				case "kb":
					return value != defaultLong?  value * 1024: defaultLong;

				case "mb":
					return value != defaultLong?  value * 1024 * 1024: defaultLong;

				case "gb":
					return value != defaultLong?  value * 1024 * 1024 * 1024: defaultLong;
				default:
					return toLong(humanDisplaySize, defaultLong);
			}
		}

		return toLong(humanDisplaySize, defaultLong);
	}

	/**
	 * Converts the specified input value into an {@code long}. The input value
	 * can be a String or an instance of {@link Number}.
	 * @param input
	 *         - The value to convert.
	 * @param defaultLong
	 *        - Supplier with the default value in case the input cannot be converted.
	 * @return long value
	 */
	public static long toLong (final Object input, final Supplier<Long> defaultLong) {

		long l = 0;

		try {
			if (UtilMethods.isSet(input)) {
				if (input instanceof CharSequence) {
					l = Long.parseLong(input.toString());
				} else if (input instanceof Number) {
					l = Number.class.cast(input).longValue();
				} else {
					l = defaultLong.get();
				}
			}
		} catch (NumberFormatException e) {

			l = defaultLong.get();
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
			if (input instanceof CharSequence) {
				return Integer.parseInt(CharSequence.class.cast(input).toString());
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
	 * Converts the specified input value into an {@code float}. The input value
	 * can be a String or an instance of {@link Number}.
	 *
	 * @param input
	 *            - The value to convert.
	 * @param defaultInt
	 *            - The default value in case the input cannot be converted.
	 * @return The input as {@code int}, or the default value.
	 */
	public static float toFloat(final Object input, final float defaultInt) {
		try {
			if (input instanceof CharSequence) {
				return Float.parseFloat(CharSequence.class.cast(input).toString());
			} else if (input instanceof Number) {
				return Number.class.cast(input).floatValue();
			} else {
				return defaultInt;
			}
		} catch (NumberFormatException e) {
			return defaultInt;
		}
	}

	/**
	 * Converts the specified input value into an {@code int}. The input value
	 * can be a String or an instance of {@link Number}.
	 *
	 * @param input
	 *            - The value to convert.
	 * @param defaultInt
	 *            - Supplier with the default value in case the input cannot be converted.
	 * @return The input as {@code int}, or the default value.
	 */
	public static int toInt(final Object input, final Supplier<Integer> defaultInt) {
		try {
			if (input instanceof CharSequence) {
				return Integer.parseInt(CharSequence.class.cast(input).toString());
			} else if (input instanceof Number) {
				return Number.class.cast(input).intValue();
			} else {
				return defaultInt.get();
			}
		} catch (NumberFormatException e) {
			return defaultInt.get();
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
			return Boolean.parseBoolean(strBool);
		} catch (final Exception e) {
			return defaultBool;
		}
	}

	/**
	 * Based on a value obtained from database, if it is a boolean will return a cast.
	 * Otherwise will use the {@link DbConnectionFactory} to determine the boolean value cross-db
	 * @param objectBoolean {@link Object}
	 * @return boolean
	 */
	public static boolean toBooleanFromDb(final Object objectBoolean) {

		if (null == objectBoolean) {
			return false;
		}
		return (objectBoolean instanceof Boolean)?
				Boolean.class.cast(objectBoolean):
				DbConnectionFactory.isDBTrue(objectBoolean.toString());
	}

	/**
	 * Converts bytes to human-readable format (B, KB, MB, GB, TB).
	 * This is the reverse operation of {@link #toLongFromByteCountHumanDisplaySize(String, long)}.
	 * 
	 * @param bytes the number of bytes to convert
	 * @return human-readable string representation (e.g., "1.5 MB", "512 B")
	 */
	public static String toHumanReadableByteSize(final long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		}
		
		final String[] units = {"KB", "MB", "GB", "TB"};
		int unitIndex = 0;
		double size = bytes;
		
		while (size >= 1024 && unitIndex < units.length - 1) {
			size /= 1024;
			unitIndex++;
		}
		
		return String.format("%.2f %s", size, units[unitIndex]);
	}

}
