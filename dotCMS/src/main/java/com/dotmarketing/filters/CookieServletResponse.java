/*
 * ServletResponseCharacterEncoding.java
 *
 * Created on 8/10/2017
 * @author Will Ezell
 */
package com.dotmarketing.filters;

import static com.liferay.util.CookieUtil.COOKIES_HTTP_ONLY;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;


import com.dotmarketing.util.Config;


public class CookieServletResponse extends HttpServletResponseWrapper {


    public CookieServletResponse(HttpServletResponse response) {
        super(response);
    }

    public CookieServletResponse(ServletResponse response) {
        this((HttpServletResponse) response);
    }

    @Override
    public void addCookie(Cookie cookie) {

        if (Config.getBooleanProperty(COOKIES_HTTP_ONLY, false)) {
            cookie.setHttpOnly(true);
        }

        super.addCookie(cookie);
    }

}
