package com.dotmarketing.util;

import javax.servlet.http.Cookie;


public class CookieUtil {

	public static Cookie createCookie() {
		// set id cookie
		String _dotCMSID = UUIDGenerator.generateUuid();
		Cookie idCookie = new Cookie(
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE,
				_dotCMSID);
		idCookie.setPath("/");
		idCookie.setMaxAge(60 * 60 * 24 * 356 * 5);

		return idCookie;

	}

    public static Cookie createOncePerVisitCookie() {
        // set id cookie
        Cookie idCookie = new Cookie(
                com.dotmarketing.util.WebKeys.ONCE_PER_VISIT_COOKIE,
                UUIDGenerator.generateUuid());
        idCookie.setPath("/");
        idCookie.setMaxAge(-1);

        return idCookie;

    }

}
