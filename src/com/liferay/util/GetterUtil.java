/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import java.text.DateFormat;
import java.util.Date;

/**
 * <a href="GetterUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.9 $
 *
 */
public class GetterUtil {

	public static boolean getBoolean(String value) {
		return getBoolean(value, false);
	}

	public static boolean getBoolean(String value, boolean defaultValue) {
		return get(value, defaultValue);
	}

	public static Date getDate(String value, DateFormat df) {
		return getDate(value, df, new Date());
	}

	public static Date getDate(String value, DateFormat df, Date defaultValue) {
		return get(value, df, defaultValue);
	}

	public static double getDouble(String value) {
		return getDouble(value, 0.0);
	}

	public static double getDouble(String value, double defaultValue) {
		return get(value, defaultValue);
	}

	public static float getFloat(String value) {
		return getFloat(value, (float)0);
	}

	public static float getFloat(String value, float defaultValue) {
		return get(value, defaultValue);
	}

	public static int getInteger(String value) {
		return getInteger(value, 0);
	}

	public static int getInteger(String value, int defaultValue) {
		return get(value, defaultValue);
	}

	public static long getLong(String value) {
		return getLong(value, 0);
	}

	public static long getLong(String value, long defaultValue) {
		return get(value, defaultValue);
	}

	public static short getShort(String value) {
		return getShort(value, (short)0);
	}

	public static short getShort(String value, short defaultValue) {
		return get(value, defaultValue);
	}

	public static String getString(String value) {
		return getString(value, StringPool.BLANK);
	}

	public static String getString(String value, String defaultValue) {
		return get(value, defaultValue);
	}

	public static boolean get(String value, boolean defaultValue) {
		if (Validator.isNotNull(value)) {
			try {
				value = value.trim();

				if (value.equalsIgnoreCase(_BOOLEANS[0]) ||
					value.equalsIgnoreCase(_BOOLEANS[1]) ||
					value.equalsIgnoreCase(_BOOLEANS[2]) ||
					value.equalsIgnoreCase(_BOOLEANS[3]) ||
					value.equalsIgnoreCase(_BOOLEANS[4])) {

					return true;
				}
				else {
					return false;
				}
			}
			catch (Exception e) {
			}
		}

		return defaultValue;
	}

	public static Date get(String value, DateFormat df, Date defaultValue) {
		try {
			Date date = df.parse(value.trim());

			if (date != null) {
				return date;
			}
		}
		catch (Exception e) {
		}

		return defaultValue;
	}

	public static double get(String value, double defaultValue) {
		try {
			return Double.parseDouble(_trim(value));
		}
		catch (Exception e) {
		}

		return defaultValue;
	}

	public static float get(String value, float defaultValue) {
		try {
			return Float.parseFloat(_trim(value));
		}
		catch (Exception e) {
		}

		return defaultValue;
	}

	public static int get(String value, int defaultValue) {
		try {
			return Integer.parseInt(_trim(value));
		}
		catch (Exception e) {
		}

		return defaultValue;
	}

	public static long get(String value, long defaultValue) {
		try {
			return Long.parseLong(_trim(value));
		}
		catch (Exception e) {
		}

		return defaultValue;
	}

	public static short get(String value, short defaultValue) {
		try {
			return Short.parseShort(_trim(value));
		}
		catch (Exception e) {
		}

		return defaultValue;
	}

	public static String get(String value, String defaultValue) {
		if (Validator.isNotNull(value)) {
			value = value.trim();
			value = StringUtil.replace(value, "\r\n", "\n");

			return value;
		}

		return defaultValue;
	}

	private static String _trim(String value) {
		if (value != null) {
			value = value.trim();

			StringBuffer sb = new StringBuffer();

			char[] charArray = value.toCharArray();

			for (int i = 0; i < charArray.length; i++) {
				if ((Character.isDigit(charArray[i])) ||
					(charArray[i] == '-' && i == 0) ||
					(charArray[i] == '.')) {

					sb.append(charArray[i]);
				}
			}

			value = sb.toString();
		}

		return value;
	}

	private static String[] _BOOLEANS = {"true", "t", "y", "on", "1"};

}