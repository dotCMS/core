/*
 * Created on Feb 17, 2005
 *
 */
package com.dotmarketing.struts;

import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portal.struts.StrutsPathAccess;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.Lazy;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author will
 *
 */
public class PortalRequestProcessor extends com.liferay.portal.struts.PortalRequestProcessor {

	/**
	 * Prefix of the front-end struts URLs. Read once: it is a fixed deployment path, and every
	 * request through this processor tests it.
	 */
	private static final Lazy<String> CMS_STRUTS_PATH =
			Lazy.of(() -> Config.getStringProperty("CMS_STRUTS_PATH", "/dotCMS/"));

	/**
	 * Tells whether the request came in through the front-end struts path rather than the portal.
	 *
	 * @param req the request being processed
	 * @return {@code true} when the request URI sits under the CMS struts path
	 */
	private static boolean isCmsStrutsPath(final HttpServletRequest req) {
		return req.getRequestURI().startsWith(CMS_STRUTS_PATH.get());
	}

	@Override
	protected String processPath(final HttpServletRequest req, final HttpServletResponse res)
			throws IOException {
		if (isCmsStrutsPath(req)) {
			return super.callParentProcessPath(req, res);
		}
		return super.processPath(req, res);
	}

	/**
	 * Resolves access for the request, whichever context the request came in through.
	 *
	 * <p>Requests under the CMS struts path used to go straight to the Struts processor, skipping
	 * the portal checks. They now run the same access resolution as every other request. They keep
	 * skipping the rest of the portal processing, and a request that is not allowed ends as a 403
	 * rather than on the portal error page, since this branch forwards to the Velocity servlet and
	 * has no portal page to render.</p>
	 *
	 * @param req     the request being processed
	 * @param res     the response, used to forward a request that is not allowed
	 * @param mapping the Struts {@link ActionMapping} whose path is about to run
	 * @return {@code true} when the request may continue
	 * @throws IOException      if the forward cannot be written
	 * @throws ServletException if the forward fails
	 */
	@Override
	protected boolean processRoles(final HttpServletRequest req, final HttpServletResponse res,
			final ActionMapping mapping) throws IOException, ServletException {

		if (isCmsStrutsPath(req)) {
			User user = null;
			try {
				user = PortalUtil.getUser(req);
			} catch (final Exception e) {
				Logger.debug(this,
						() -> "Unable to resolve the user for the request: " + e.getMessage());
			}
			if (!StrutsPathAccess.canAccess(mapping.getPath(), user)) {
				res.sendError(HttpServletResponse.SC_FORBIDDEN);
				return false;
			}
			return super.callParentProcessRoles(req, res, mapping);
		} else {
			return super.processRoles(req, res, mapping);
		}

	}

	@Override
	protected void doForward(final String uri, final HttpServletRequest req,
			final HttpServletResponse res) throws IOException, ServletException {
		if (isCmsStrutsPath(req)) {
			req.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, uri);

			req.getRequestDispatcher("/servlets/VelocityServlet").forward(req, res);
		} else {
			super.doForward(uri, req, res);
		}

	}
}