package com.dotcms;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory.WebContextBuilder;
import com.dotcms.repackage.org.directwebremoting.impl.DefaultContainer;
import com.dotcms.repackage.org.directwebremoting.impl.DefaultWebContextBuilder;
import com.dotmarketing.servlets.test.ServletTestRunner;

/**
 * This class provides utility methods to simulate a user login to dotCMS. It is
 * very useful in cases where business logic requires a logged-in user. The main
 * goal of this class is to hold common login simulation methods that can be
 * used by any test class. Please add as many methods as required.
 * 
 * @author Jose Castro
 * @version 3.3
 * @since Nov 25, 2015
 *
 */
public class DwrAuthenticationUtil {

	private DefaultContainer container = null;
	private DefaultWebContextBuilder webContextBuilder = null;

	/**
	 * Sets up the {@link WebContext} set by the DWR servlet. This method is
	 * useful when testing some of the classes used by DWR (such as,
	 * {@code RoleAjax}, {@code UserAjax}, etc.) that require user verification
	 * in some of their methods.
	 * <p>
	 * When accessing those classes through a test class, no DWR context is set
	 * yet, which causes NPEs when the dotCMS code tries to get the
	 * {@link WebContext} class used to get the {@link HttpServletRequest}
	 * object. This method sets up all the required objects for you.
	 * </p>
	 * 
	 * @param requestAttrs
	 *            - A simple {@link Map} containing the attributes that will be
	 *            set in the {@link HttpServletRequest} object. Can be
	 *            {@code null} if not required.
	 * @param sessionAttrs
	 *            - A simple {@link Map} containing the attributes that will be
	 *            set in the {@link HttpSession} object. Can be {@code null} if
	 *            not required.
	 */
	public void setupWebContext(Map<String, Object> requestAttrs, Map<String, Object> sessionAttrs) {
		final HttpServletRequest req = ServletTestRunner.localRequest.get();
		final HttpServletResponse res = ServletTestRunner.localResponse.get();
		HttpSession session = req.getSession(true);
		Set<String> keySet = null;
		if (requestAttrs != null && !requestAttrs.isEmpty()) {
			keySet = requestAttrs.keySet();
			for (String key : keySet) {
				req.setAttribute(key, requestAttrs.get(key));
			}
		}
		if (sessionAttrs != null && !sessionAttrs.isEmpty()) {
			keySet = sessionAttrs.keySet();
			for (String key : keySet) {
				session.setAttribute(key, sessionAttrs.get(key));
			}
		}
		this.container = new DefaultContainer();
		this.webContextBuilder = new DefaultWebContextBuilder();
		this.webContextBuilder.engageThread(this.container, req, res);
		this.container.addBean(WebContextBuilder.class, this.webContextBuilder);
		WebContextFactory.attach(this.container);
	}

	/**
	 * Shuts down the DWR Web Context that was previously created. After calling
	 * this method, the {@link WebContextFactory#get()} method will return a
	 * {@code null} object, which means the DWR authentication has been
	 * terminated. The main goal of this method is to avoid having an active DWR
	 * {@link WebContext} when not needed.
	 */
	public void shutdownWebContext() {
		this.container.servletDestroyed();
		this.webContextBuilder.disengageThread();
	}
}
