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

import java.net.URLDecoder;
import java.net.URLEncoder;
import org.apache.oro.text.perl.Perl5Util;

/**
 * <a href="JS.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.11 $
 *
 */
public class JS {

	public static final String ENCODING = "UTF-8";

	/**
	 * @deprecated Use <code>encodeURIComponent</code>.
	 */
	public static String escape(String s) {
		return encodeURIComponent(s);
	}

	/**
	 * @deprecated Use <code>decodeURIComponent</code>.
	 */
	public static String unescape(String s) {
		return decodeURIComponent(s);
	}

	public static String encodeURIComponent(String s) {

		// Encode URL

		try {
			s = URLEncoder.encode(s, ENCODING);
		}
		catch (Exception e) {
		}

		// Adjust for JavaScript specific annoyances

		s = StringUtil.replace(s, "+", "%20");
		s = StringUtil.replace(s, "%2B", "+");

		return s;
	}

	public static String decodeURIComponent(String s) {
		Perl5Util util = new Perl5Util();

		// Get rid of all unicode

		s = util.substitute("s/%u[0-9a-fA-F]{4}//g", s);

		// Adjust for JavaScript specific annoyances

		s = StringUtil.replace(s, "+", "%2B");
		s = StringUtil.replace(s, "%20", "+");

		// Decode URL

		try {
			s = URLDecoder.decode(s, ENCODING);
		}
		catch (Exception e) {
		}

		return s;
	}

}