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

import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Config;
import com.liferay.portal.util.CookieKeys;

/**
 * <a href="CookieUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class CookieUtil {

	public static final String HTTP_ONLY = "HttpOnly";
	public static final String SECURE = "secure";
	public static final String COOKIES_HTTP_ONLY = "COOKIES_HTTP_ONLY";
	public static final String COOKIES_SECURE_FLAG = "COOKIES_SECURE_FLAG";

	/**
	 * 
	 * @param cookies
	 * @param name
	 * @return
	 */
	public static String get(Cookie[] cookies, String name) {
		if ((cookies != null) && (cookies.length > 0)) {
			for (int i = 0; i < cookies.length; i++) {
				String cookieName = cookies[i].getName();

				if ((cookieName != null) && (cookieName.equals(name))) {
					return cookies[i].getValue();
				}
			}
		}

		return null;
	}

	
    public static void deleteCookie(HttpServletRequest req, HttpServletResponse resp, String cookieName) {
        Cookie[] cookies = req.getCookies();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                cookie.setValue("");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                resp.addCookie(cookie);
            }
        }
    }
	
	
	
	/**
	 * 
	 * @param cookie
	 * @param tag
	 * @return
	 */
	public static String get(String cookie, String tag) {
		if (cookie == null) {
			return "";
		}

		tag = tag + "=";

		if (cookie.startsWith(tag)) {
			int y = cookie.indexOf(';');

			return cookie.substring(tag.length(), y);
		}

		tag = ";" + tag;

		int x = cookie.indexOf(tag);

		if (x != -1) {
			int y = cookie.indexOf(';', x + 1);

			return cookie.substring(x + tag.length(), y);
		}

		return "";
	}

	/**
	 * 
	 * @param cookie
	 * @param tag
	 * @param sub
	 * @return
	 */
	public static String set(String cookie, String tag, String sub) {
		if (cookie == null) {
			return "";
		}

		tag = tag + "=";

		if (cookie.startsWith(tag)) {
			int y = cookie.indexOf(';');

			StringBuffer sb = new StringBuffer();

			sb.append(tag).append(sub).append(";");
			sb.append(cookie.substring(y + 1, cookie.length()));

			return sb.toString();
		}

		tag = ";" + tag;

		int x = cookie.indexOf(tag);

		if (x != -1) {
			int y = cookie.indexOf(';', x + 1);

			StringBuffer sb = new StringBuffer();

			sb.append(cookie.substring(0, x + tag.length()));
			sb.append(sub);
			sb.append(cookie.substring(y, cookie.length()));

			return sb.toString();
		}

		return cookie + tag.substring(1, tag.length()) + sub + ";";
	}



	/**
	 * Verifies the existence of a cookie inside an array of {@link Cookie}
	 * objects.
	 * 
	 * @param cookies
	 *            - The array of cookies to verify.
	 * @param name
	 *            - The name of the cookie to locate.
	 * @return If the specified cookie name exists, returns {@code true}.
	 *         Otherwise, returns {@code false}.
	 */
	public static boolean containsCookie(Cookie[] cookies, String name) {
		if(cookies==null) return false;

		for (Cookie cookie : cookies) {
			if(cookie.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

}
