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

import javax.servlet.http.HttpServletRequest;

/**
 * <a href="BrowserSniffer_IW.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class BrowserSniffer_IW {

	public static BrowserSniffer_IW getInstance() {
		return _instance;
	}

	public boolean is_ie(HttpServletRequest req) {
		return BrowserSniffer.is_ie(req);
	}

	public boolean is_ie_4(HttpServletRequest req) {
		return BrowserSniffer.is_ie_4(req);
	}

	public boolean is_ie_5(HttpServletRequest req) {
		return BrowserSniffer.is_ie_5(req);
	}

	public boolean is_ie_5_5(HttpServletRequest req) {
		return BrowserSniffer.is_ie_5_5(req);

	}

	public boolean is_ie_5_5_up(HttpServletRequest req) {
		return BrowserSniffer.is_ie_5_5_up(req);
	}

	public boolean is_linux(HttpServletRequest req) {
		return BrowserSniffer.is_linux(req);
	}

	public boolean is_mozilla(HttpServletRequest req) {
		return BrowserSniffer.is_mozilla(req);
	}

	public boolean is_mozilla_1_3_up(HttpServletRequest req) {
		return BrowserSniffer.is_mozilla_1_3_up(req);
	}

	public boolean is_ns_4(HttpServletRequest req) {
		return BrowserSniffer.is_ns_4(req);
	}

	public boolean is_rtf(HttpServletRequest req) {
		return BrowserSniffer.is_rtf(req);
	}

	public boolean is_wml(HttpServletRequest req) {
		return BrowserSniffer.is_wml(req);
	}

	private BrowserSniffer_IW() {
	}

	private static BrowserSniffer_IW _instance = new BrowserSniffer_IW();

}