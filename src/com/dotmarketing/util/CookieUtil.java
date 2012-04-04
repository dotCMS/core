package com.dotmarketing.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class CookieUtil {

	private static GUIDGenerator guidGenerator = null;

	public static Cookie createCookie() {
		if (guidGenerator == null) {
			try {
				guidGenerator = new GUIDGenerator();
			} catch (Exception e) {
				Logger.error(CookieUtil.class, e.getMessage(), e);
			}
		}
		// set id cookie
		String _dotCMSID = guidGenerator.getUUID();
		Cookie idCookie = new Cookie(
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE,
				_dotCMSID);
		idCookie.setPath("/");
		idCookie.setMaxAge(60 * 60 * 24 * 356 * 5);

		return idCookie;

	}

}
