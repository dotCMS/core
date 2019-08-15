/*
 * CharsetEncodingFilter.java
 *
 * Created on 24 October 2007
 */
package com.dotmarketing.filters;

import static com.liferay.util.CookieUtil.COOKIES_SECURE_FLAG;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.util.CookieKeys;
import com.liferay.util.CookieUtil;

import io.vavr.control.Try;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class CookiesFilter implements Filter {

	public void init(FilterConfig arg0) throws ServletException {
		if ( com.dotmarketing.util.CookieUtil.ALWAYS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, com.dotmarketing.util.CookieUtil.HTTPS))
				|| com.dotmarketing.util.CookieUtil.HTTPS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, com.dotmarketing.util.CookieUtil.HTTPS)) ){
			Config.CONTEXT.getSessionCookieConfig().setSecure(true);
		}
	}

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = new CookieServletResponse(response);
    CookieUtil.setCookiesSecurityHeaders(req, res);
    try {
      filterChain.doFilter(req, res);
    } catch (final Exception nse) {
      if (ExceptionUtil.causedBy(nse, com.liferay.portal.NoSuchUserException.class)) {
        handleNoSuchUserException(req, res);
      }else {
        Class clazz = Try.of(() -> (Class) Class.forName(nse.getStackTrace()[0].getClassName())).getOrElse(this.getClass());
        Logger.error(clazz, nse);
      }
    }
  }

	private void handleNoSuchUserException(final HttpServletRequest request, final HttpServletResponse response) throws ServletException{
		try {
			// Invalidate session
			APILocator.getLoginServiceAPI().doActionLogout(request,response);
			//Destroy session Cookie, just in-case
			final Cookie[] cookies = request.getCookies();
			final Optional<Cookie> optionalCookie = Stream
					.of(cookies).filter(cookie -> CookieKeys.JSESSIONID.equals(cookie.getName())).findFirst();
			if(optionalCookie.isPresent()){
				final Cookie jsessionCookie = optionalCookie.get();
				jsessionCookie.setMaxAge(0);
			}
			//Now we need to force the browser to make a new request to take the user to the login page
			request.getRequestDispatcher("/c").forward(request, response);

		} catch (Exception e){
			throw new ServletException(e);
		}
	}

	public void destroy() {
	}
}
