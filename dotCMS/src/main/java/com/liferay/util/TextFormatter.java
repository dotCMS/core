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

import java.text.NumberFormat;
import java.util.Locale;

/**
 * <a href="TextFormatter.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.8 $
 *
 */
public class TextFormatter {

	// Web Search --> WEB_SEARCH
	// Web Search --> websearch
	// Web Search --> web_search
	// Web Search --> WebSearch
	// Web Search --> web search
	// Web Search --> webSearch

	public static final int A = 0;
	public static final int B = 1;
	public static final int C = 2;
	public static final int D = 3;
	public static final int E = 4;
	public static final int F = 5;

	// formatId --> FormatId
	// formatId --> format id

	public static final int G = 6;
	public static final int H = 7;

	// FormatId --> formatId

	public static final int I = 8;

	public static String format(String s, int style) {
		if (Validator.isNull(s)) {
			return null;
		}

		s = s.trim();

		if (style == A) {
			return _formatA(s);
		}
		else if (style == B) {
			return _formatB(s);
		}
		else if (style == C) {
			return _formatC(s);
		}
		else if (style == D) {
			return _formatD(s);
		}
		else if (style == E) {
			return _formatE(s);
		}
		else if (style == F) {
			return _formatF(s);
		}
		else if (style == G) {
			return _formatG(s);
		}
		else if (style == H) {
			return _formatH(s);
		}
		else if (style == I) {
			return _formatI(s);
		}
		else {
			return s;
		}
	}

	public static String formatKB(double size, Locale locale) {
		NumberFormat nf = NumberFormat.getInstance(locale);
		nf.setMaximumFractionDigits(1);
		nf.setMinimumFractionDigits(1);

		return nf.format(size / 1024.0);
	}

	public static String formatKB(int size, Locale locale) {
		return formatKB((double)size, locale);
	}

	public static String formatName(String name) {
		if (Validator.isNull(name)) {
			return name;
		}

		char[] c = name.toLowerCase().trim().toCharArray();

		if (c.length > 0) {
			c[0] = Character.toUpperCase(c[0]);
		}

		for (int i = 0; i < c.length; i++) {
			if (c[i] == ' ') {
				c[i + 1] = Character.toUpperCase(c[i + 1]);
			}
		}

		return new String(c);
	}

	public static String formatPlural(String s) {
		if (Validator.isNull(s)) {
			return s;
		}

		if (s.endsWith("y")) {
			s = s.substring(0, s.length() -1) + "ies";
		}
		else {
			s = s + "s";
		}

		return s;
	}

	private static String _formatA(String s) {
		return StringUtil.replace(s.toUpperCase(), ' ', '_');
	}

	private static String _formatB(String s) {
		return StringUtil.replace(s.toLowerCase(), ' ', "");
	}

	private static String _formatC(String s) {
		return StringUtil.replace(s.toLowerCase(), ' ', '_');
	}

	private static String _formatD(String s) {
		return StringUtil.replace(s, ' ', "");
	}

	private static String _formatE(String s) {
		return s.toLowerCase();
	}

	private static String _formatF(String s) {
		s = StringUtil.replace(s, ' ', "");
		s = Character.toLowerCase(s.charAt(0)) + s.substring(1, s.length());

		return s;
	}

	private static String _formatG(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
	}

	private static String _formatH(String s) {
		StringBuffer sb = new StringBuffer();

		char[] c = s.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if (Character.isUpperCase(c[i])) {
				sb.append(" ").append(Character.toLowerCase(c[i]));
			}
			else {
				sb.append(c[i]);
			}
		}

		return sb.toString();
	}

	private static String _formatI(String s) {
		if (s.length() == 1) {
			return s.toLowerCase();
		}

		if (Character.isUpperCase(s.charAt(0)) &&
			Character.isLowerCase(s.charAt(1))) {

			return Character.toLowerCase(s.charAt(0)) +
				s.substring(1, s.length());
		}

		StringBuffer sb = new StringBuffer();

		char[] c = s.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if ((i + 1 != c.length) &&
				(Character.isLowerCase(c[i + 1]))) {

				sb.append(s.substring(i, c.length));

				break;
			}
			else {
				sb.append(Character.toLowerCase(c[i]));
			}
		}

		return sb.toString();
	}

}