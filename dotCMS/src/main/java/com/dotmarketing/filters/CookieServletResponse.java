/*
 * ServletResponseCharacterEncoding.java
 *
 * Created on 8/10/2017
 * @author Will Ezell
 */
package com.dotmarketing.filters;

import static com.liferay.util.CookieUtil.COOKIES_HTTP_ONLY;
import static com.liferay.util.CookieUtil.COOKIES_SECURE_FLAG;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;


public class CookieServletResponse extends HttpServletResponseWrapper {

    public CookieServletResponse(final HttpServletResponse response) {
        super(response);
    }

    public CookieServletResponse(final ServletResponse response) {
        this((HttpServletResponse) response);
    }

    @Override
    public void addCookie(final Cookie cookie) {

        if (Config.getBooleanProperty(COOKIES_HTTP_ONLY, false)) {
            cookie.setHttpOnly(true);
        }
        if ( CookieUtil.ALWAYS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, CookieUtil.HTTPS))
                || CookieUtil.HTTPS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, CookieUtil.HTTPS)) ){
            cookie.setSecure(true);
        }

        super.addCookie(cookie);
    }

}
