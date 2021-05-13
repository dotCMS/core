/*
 * ServletResponseCharacterEncoding.java
 *
 * Created on 8/10/2017
 * @author Will Ezell
 */
package com.dotmarketing.filters;

import static com.liferay.util.CookieUtil.COOKIES_HTTP_ONLY;
import static com.liferay.util.CookieUtil.COOKIES_SECURE_FLAG;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;


public class CookieServletResponse extends HttpServletResponseWrapper {

    
    
    
    final boolean requestIsSecure;
    
    public CookieServletResponse(final HttpServletResponse response, final boolean secure) {
        super(response);
        this.requestIsSecure=secure;
    }
    
    public CookieServletResponse(final ServletResponse response, final boolean secure) {
        this((HttpServletResponse) response, secure);
    }


    // prevent repeated lookups
    final static boolean COOKIES_HTTP_ONLY_FLAG = Config.getBooleanProperty(COOKIES_HTTP_ONLY, true);
    
    final static boolean SEND_COOKIES_SECURE_ALWAYS = CookieUtil.ALWAYS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, CookieUtil.ALWAYS));
    
    final static boolean SEND_COOKIES_SECURE_WHEN_HTTPS = CookieUtil.HTTPS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, CookieUtil.HTTPS));
    
   
    
    @Override
    public void addCookie(final Cookie cookie) {

        if (COOKIES_HTTP_ONLY_FLAG) {
            cookie.setHttpOnly(true);
        }
        
        if ( SEND_COOKIES_SECURE_ALWAYS || this.requestIsSecure && SEND_COOKIES_SECURE_WHEN_HTTPS){
            cookie.setSecure(true);
        }
        

        super.addCookie(cookie);
    }

}
