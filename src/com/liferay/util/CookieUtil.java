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
	
	/*
	 * Adds the secure and httpOnly flag to cookies depending on the config by adding the SET-COOKIE header to the response
	 * @param req The HttpServletRequest object
	 * @param res the HttpServletResponse object
	 */
	

	public static void setCookiesSecurityHeaders(HttpServletRequest req, HttpServletResponse res) {
		setCookiesSecurityHeaders(req, res, null);

	}
	
	
	/*
	 * Adds the secure and httpOnly flag to cookies depending on the config by adding the SET-COOKIE header to the response
	 * @param req The HttpServletRequest object
	 * @param res the HttpServletResponse object
	 * @cookies an optional list with the names of the cookies that will only be affected. 
	 */
	

public static HttpServletResponse setCookiesSecurityHeaders(HttpServletRequest req, HttpServletResponse res, Set<String> cookies) {

		//there are cookies on the request
		if(req.getCookies()!=null) {
			for(Cookie cookie : req.getCookies()){

				if(cookie.getName().equals(CookieKeys.ID))
					continue;

				// if we are using websphere do not change the JSESSIONID vaules
                if(cookie.getName().equals("JSESSIONID") && (!Config.getBooleanProperty("COOKIES_SESSION_COOKIE_FLAGS_MODIFIABLE", false) || ServerDetector.isWebSphere())) {
                    continue;
                }

                // if JSESSIONID exists updates its value
                if(cookie.getName().equals("JSESSIONID") && req.getSession(false)!=null) {
                	cookie.setValue(req.getSession(false).getId());
                	cookie.setPath("/");
				}

                // if the COOKIES_SECURE_FLAG is set to "https" or "always" in a secure connection
                // all cookie's security flags are force to true
                //
                // Important: adding flags to cookie and moving it to the response will set
                // the expire property to a value of -1 so the cookie will turn into a
                // "session cookie".  This will happen only if the COOKIES_SECURE_FLAG property is
                // is set to force the secure flags.  If the cookies do not need to be altered
                // they won't be affected on the loop
                //
				if(Config.getStringProperty("COOKIES_SECURE_FLAG", "https").equals("always")
						|| (Config.getStringProperty("COOKIES_SECURE_FLAG", "https").equals("https") && req.isSecure())) {
					if(Config.getBooleanProperty("COOKIES_HTTP_ONLY", false)){
						// add secure and httpOnly flag to the cookie
						cookie.setSecure(true);
						cookie.setHttpOnly(true);
					}else{
						// only secure flag added to he cookie
						cookie.setSecure(true);
						cookie.setHttpOnly(false);
					}
					// the cookie is added directly to the response
					res.addCookie(cookie);
				} else {
					// if the connection is not secure we still need to set the JSESSIONID value and flags
					if(cookie.getName().equals("JSESSIONID")){
						if(Config.getBooleanProperty("COOKIES_HTTP_ONLY", false)){
							// only the httpOnly value is added to the cookie
							cookie.setSecure(false);
							cookie.setHttpOnly(true);
						}
						else{
							// no flags added to the cookie
							cookie.setSecure(false);
							cookie.setHttpOnly(false);
						}
						res.addCookie(cookie);
					}
				}
			}
		}

		// if the JSESSIONID cookie does not exists create it
		if(req.getSession(false)!=null && (req.getCookies()==null || !containsCookie(req.getCookies(), "JSESSIONID"))) {

			if(Config.getStringProperty("COOKIES_SECURE_FLAG", "https").equals("always")
					|| (Config.getStringProperty("COOKIES_SECURE_FLAG", "https").equals("https") && req.isSecure())) {
				Cookie cookie = new Cookie("JSESSIONID", req.getSession(false).getId());
				cookie.setPath("/");
				if(Config.getBooleanProperty("COOKIES_HTTP_ONLY", false)){
					// add secure and httpOnly flag to the cookie
					cookie.setSecure(true);
					cookie.setHttpOnly(true);
				}
				else{
					// only secure flag added to he cookie
					cookie.setSecure(true);
					cookie.setHttpOnly(false);
				}
				res.addCookie(cookie);
			}
		}
		return res;
	}
	
	private static boolean containsCookie(Cookie[] cookies, String name) {
		if(cookies==null) return false;
		
		for (Cookie cookie : cookies) {
			if(cookie.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

}