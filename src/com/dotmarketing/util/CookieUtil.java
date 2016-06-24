package com.dotmarketing.util;

import com.liferay.portal.util.CookieKeys;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Optional;

import static com.liferay.util.CookieUtil.COOKIES_HTTP_ONLY;
import static com.liferay.util.CookieUtil.COOKIES_SECURE_FLAG;

import static com.liferay.util.CookieUtil.containsCookie;


public class CookieUtil {

    private static final String ALWAYS = "always";
    private static final String HTTPS = "https";
    private static final String URI = "/";
    private static final int MAX_AGE_DAY_MILLIS = 60 * 60 * 24;

    public static Cookie createCookie() {
		// set id cookie
		String _dotCMSID = UUIDGenerator.generateUuid();
		Cookie idCookie = new Cookie(
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE,
				_dotCMSID);
		idCookie.setPath(URI);
		idCookie.setMaxAge(MAX_AGE_DAY_MILLIS * 356 * 5);

		return idCookie;

	}

    public static Cookie createOncePerVisitCookie() {
        // set id cookie
        Cookie idCookie = new Cookie(
                com.dotmarketing.util.WebKeys.ONCE_PER_VISIT_COOKIE,
                UUIDGenerator.generateUuid());
        idCookie.setPath(URI);
        idCookie.setMaxAge(-1);

        return idCookie;

    }
    
    public static Cookie createSiteVisitsCookie(){
    	Cookie idCookie = new Cookie(com.dotmarketing.util.WebKeys.SITE_VISITS_COOKIE, "1");
    	idCookie.setPath(URI);
    	idCookie.setMaxAge(MAX_AGE_DAY_MILLIS * 356 * 5);
    	return idCookie;
    }

	/**
	 * Creates the Json Web Token, if it hasn't been already created
	 * @param request {@link HttpServletRequest}
	 * @param response {@link HttpServletResponse}
	 * @param accessToken {@link String}
     * @param daysMaxAge {@link Optional} max days you want to keep alive the cookie, by default 1 day
     */
	public static void createJsonWebTokenCookie(final HttpServletRequest request,
                                                final HttpServletResponse response,
                                                final String accessToken,
                                                final Optional<Integer> daysMaxAge) {

        Cookie cookie = null;

        // if the JSESSIONID cookie does not exists create it
		if ( request.getCookies() == null ||
				!containsCookie(request.getCookies(), CookieKeys.JWT_ACCESS_TOKEN) ) {

			//Set-Cookie: access_token=eyJhbGciOiJIUzI1NiIsI.eyJpc3MiOiJodHRwczotcGxlL.mFrs3Zo8eaSNcxiNfvRh9dqKP4F1cB; Secure; HttpOnly;
			cookie = new Cookie(CookieKeys.JWT_ACCESS_TOKEN, accessToken);

			if ( Config.getBooleanProperty(COOKIES_HTTP_ONLY, false) ) {
				// add secure and httpOnly flag to the cookie
				cookie.setHttpOnly(true);
			}


			if ( ALWAYS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, HTTPS))
					|| HTTPS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, HTTPS))
                    && request.isSecure())  {

				cookie.setSecure(true);
			}

            cookie.setMaxAge(MAX_AGE_DAY_MILLIS * daysMaxAge.orElse(2));
			cookie.setPath(URI);
			response.addCookie(cookie);
		}
	} // createJsonWebTokenCookie.
}
