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

import org.owasp.esapi.ESAPI;

import com.dotmarketing.util.RegEX;

/**
 * <a href="Xss.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Clarence Shen
 * @version $Revision: 1.3 $
 *
 */
public class Xss {

	public static final String XSS_REGEXP_PATTERN = GetterUtil.getString(SystemProperties.get(Xss.class.getName() + ".regexp.pattern"));

	public static String strip(String text) {
		if (text == null) {
			return null;
		}
		return RegEX.replace(text, "", XSS_REGEXP_PATTERN);
	}
	
	public static boolean URLHasXSS(String url){
		if (url == null) {
			return false;
		}
		return RegEX.contains(url, XSS_REGEXP_PATTERN);	
	}
	
	public static String escapeHTMLAttrib(String value) {
	    return value!=null ? ESAPI.encoder().encodeForHTMLAttribute(value) : "";
	}

}