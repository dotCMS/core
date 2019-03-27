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

/**
 * <a href="Html.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Clarence Shen
 * @version $Revision: 1.10 $
 *
 */
public class Html {

	public static final String H_1_BEGIN = "<h1>";
	public static final String H_1_END   = "</h1>";

	public static final String H_2_BEGIN = "<h2>";
	public static final String H_2_END   = "</h2>";

	public static final String H_3_BEGIN = "<h3>";
	public static final String H_3_END   = "</h3>";

	public static final String B_BEGIN   = "<b>";
	public static final String B_END     = "</b>";

	public static final String PRE_BEGIN = "<b>";
	public static final String PRE_END   = "</b>";

	public static final String BR        = "<br/>";

	public static final String SPACE     = "&nbsp;";

	public static String h1 (final String header) {

		return new StringBuilder(H_1_BEGIN).append(header).append(H_1_END).toString();
	}

	public static String h2 (final String header) {

		return new StringBuilder(H_2_BEGIN).append(header).append(H_2_END).toString();
	}

	public static String h3 (final String header) {

		return new StringBuilder(H_3_BEGIN).append(header).append(H_3_END).toString();
	}

	public static String b (final String text) {

		return new StringBuilder(B_BEGIN).append(text).append(B_END).toString();
	}

	public static String pre (final String text) {

		return new StringBuilder(PRE_BEGIN).append(text).append(PRE_END).toString();
	}

	public static String br() {
		return BR;
	}

	public static String space(final int howMany) {
		final StringBuilder builder = new StringBuilder();

		for (int i = 1; i <= howMany; ++i) {

			builder.append(SPACE);
		}

		return builder.toString();
	}


	public static String formatFrom(String text) {
		if (text == null) {
			return null;
		}

		// Optimize this

		text = StringUtil.replace(text, "&#42;", "*");
		text = StringUtil.replace(text, "&#47;", "/");
		text = StringUtil.replace(text, "&#58;", ":");
		text = StringUtil.replace(text, "&#63;", "?");

		return text;
	}

	public static String formatTo(String text) {
		if (text == null) {
			return null;
		}

		int pos = text.indexOf("& ");

		if (pos != -1) {
			text = StringUtil.replace(text, "& ", "&amp; ");
		}

		StringBuffer sb = new StringBuffer();
		char c;

		for (int i = 0; i < text.length(); i++) {
			c = text.charAt(i);

			switch (c) {
				case '<':
					sb.append("&lt;");
					break;

				case '>':
					sb.append("&gt;");
					break;

				case '\'':
					sb.append("&#39;");
					break;

				case '\"':
					sb.append("&quot;");
					break;

				default:
					if (((int)c) > 255) {
						sb.append("&#").append(((int)c)).append(";");
					}
					else {
						sb.append(c);
					}
			}
		}

		return sb.toString();
	}

	public static String stripComments(String text) {
		if (text == null) {
			return null;
		}

		StringBuffer sb = new StringBuffer();
		int x = 0;
		int y = text.indexOf("<!--");

		while (y != -1) {
			sb.append(text.substring(x, y));
			x = text.indexOf("-->", y) + 3;
			y = text.indexOf("<!--", x);
		}

		if (y == -1) {
			sb.append(text.substring(x, text.length()));
		}

		return sb.toString();

		/*
		int x = text.indexOf("<!--");
		int y = text.indexOf("-->");

		if (x != -1 && y != -1) {
			return stripComments(
				text.substring(0, x) + text.substring(y + 3, text.length()));
		}
		*/

		/*
		Perl5Util util = new Perl5Util();

		text = util.substitute("s/<!--.*-->//g", text);
		*/

		//return text;
	}

	/**
	 * This method will not allow something like "text <no closing tag" You would get "test " back
	 * 
	 * @param text
	 * @return
	 */
	public static String stripHtml(String text) {
		if (text == null) {
			return null;
		}

		text = stripComments(text);

		StringBuffer sb = new StringBuffer();
		int x = 0;
		int y = text.indexOf("<");

		while (y != -1) {
			sb.append(text.substring(x, y));

			x = text.indexOf(">", y) + 1;

			if (x==0 || x < y) {

				// <b>Hello</b

				break;
			}

			y = text.indexOf("<", x);
		}

		if (y == -1) {
			sb.append(text.substring(x, text.length()));
		}

		return sb.toString();
	}

}