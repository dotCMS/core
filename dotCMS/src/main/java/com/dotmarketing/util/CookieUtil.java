package com.dotmarketing.util;

import com.liferay.portal.util.CookieKeys;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Optional;

import static com.liferay.util.CookieUtil.COOKIES_HTTP_ONLY;
import static com.liferay.util.CookieUtil.COOKIES_SECURE_FLAG;

/**
 * Provides utility methods to interact with HTTP Cookies.
 *
 * @author root
 * @version 2.x, 3.7
 * @since Mar 22, 2012
 *
 */
public class CookieUtil {

    public static final String ALWAYS = "always";
    public static final String HTTPS = "https";
    public static final String URI = "/";
    private static final int MAX_AGE_DAY_MILLIS = 60 * 60 * 24;
    public static final int DEFAULT_JWT_MAX_AGE_DAYS = 14;

    /**
     *
     * @return
     */
    public static Cookie createCookie() {
        // set id cookie
        String _dotCMSID = UUIDGenerator.generateUuid();
        Cookie idCookie = new Cookie(
            com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE,
            _dotCMSID);
        idCookie.setPath(URI);
        idCookie.setMaxAge(MAX_AGE_DAY_MILLIS * 356 * 5);

        setHttpOnlyCookie(idCookie);

        return idCookie;

    }

    /**
     *
     * @return
     */
    public static Cookie createOncePerVisitCookie() {
        // set id cookie
        Cookie idCookie = new Cookie(
            com.dotmarketing.util.WebKeys.ONCE_PER_VISIT_COOKIE,
            UUIDGenerator.generateUuid());
        idCookie.setPath(URI);
        idCookie.setMaxAge(-1);

        setHttpOnlyCookie(idCookie);

        return idCookie;

    }

    /**
     *
     * @return
     */
    public static Cookie createSiteVisitsCookie(){
        Cookie idCookie = new Cookie(com.dotmarketing.util.WebKeys.SITE_VISITS_COOKIE, "1");
        idCookie.setPath(URI);
        idCookie.setMaxAge(MAX_AGE_DAY_MILLIS * 356 * 5);

        setHttpOnlyCookie(idCookie);

        return idCookie;
    }

    /**
     * Creates the JSON Web Token (JWT) if it hasn't been created.
     *
     * @param request
     *            - The {@link HttpServletRequest} object.
     * @param response
     *            - The {@link HttpServletResponse} object.
     * @param accessToken
     *            - The {@link String} representation of the JWT.
     * @param daysMaxAge
     *            - {@link Optional} The maximum number of days you want to keep
     *            the cookie. Defaults to 14 days.
     */
    public static void createJsonWebTokenCookie(final HttpServletRequest request,
                                                final HttpServletResponse response,
                                                final String accessToken,
                                                final Optional<Integer> daysMaxAge) {

        //Set-Cookie: access_token=eyJhbGciOiJIUzI1NiIsI.eyJpc3MiOiJodHRwczotcGxlL.mFrs3Zo8eaSNcxiNfvRh9dqKP4F1cB; Secure; HttpOnly;
        final Cookie cookie = new Cookie(CookieKeys.JWT_ACCESS_TOKEN, accessToken);

        // add secure and httpOnly flag to the cookie
        setHttpOnlyCookie(cookie);

        if ( ALWAYS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, HTTPS))
            || HTTPS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, HTTPS))
            && request.isSecure())  {

            cookie.setSecure(true);
        }

        if (daysMaxAge.orElse(DEFAULT_JWT_MAX_AGE_DAYS) > 0) {

            cookie.setMaxAge(MAX_AGE_DAY_MILLIS * daysMaxAge.orElse(DEFAULT_JWT_MAX_AGE_DAYS));
        }
        cookie.setPath(URI);
        response.addCookie(cookie);

    } // createJsonWebTokenCookie.

    /**
     * Set to expire all cookies
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    public static void setExpireCookies (final HttpServletRequest request,
                                         final HttpServletResponse response) {

        final Cookie[] cookies =
            request.getCookies();

        if (null != cookies) {
            for (Cookie cookie : cookies) {
                if(WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE.equalsIgnoreCase(cookie.getName())){
                    continue;
                }
                cookie.setValue("");
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }
    } // setExpireCookies.

    
    /**
     * Set to expire all cookies
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    public static void expireCookie (final String cookieName, final HttpServletRequest request,
                                         final HttpServletResponse response) {

        final Cookie[] cookies =
            request.getCookies();

        if (null != cookies) {

            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    cookie.setValue("");
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }
        }
    } // setExpireCookies.
    
    
    /**
     * Set HttpOnly to cookie on demand and if needed
     */
 	public static void setHttpOnlyCookie(final Cookie cookie) {
 		//https://github.com/dotCMS/core/issues/11912
 		if (Config.getBooleanProperty(COOKIES_HTTP_ONLY, false)) {
 			cookie.setHttpOnly(true);			
 		}
 	} // setHttpOnlyCookie.
}
