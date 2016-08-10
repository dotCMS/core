package com.dotcms.rest.api.v1.system;

import java.io.Serializable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.AccessControlAllowOrigin;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

/**
 * This Jersey end-point provides access to configuration parameters that are
 * useful to the dotCMS Angular UI. System properties set through dotCMS
 * configuration files, and the menu items that logged in users can see in their
 * navigation bar, are just a couple of configuration properties that this
 * end-point can provide.
 * <p>
 * The number of configuration properties my vary depending on whether the user
 * is logged in or not before calling this end-point. For example, the list of
 * navigation menu items <b>will be returned as an empty list</b> if the user is
 * not authenticated yet.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 22, 2016
 *
 */
@Path("/v1/appconfiguration")
@SuppressWarnings("serial")
public class AppConfigurationResource implements Serializable {

	private static final String MENU = "menu";
	private static final String CONFIG = "config";
	private static final String USER = "user";

	private final AppConfigurationHelper helper;
	private final UserAPI userAPI;

	/**
	 * Default constructor.
	 */
	public AppConfigurationResource() {
		this( AppConfigurationHelper.INSTANCE, APILocator.getUserAPI());
	}

	@VisibleForTesting
	public AppConfigurationResource(AppConfigurationHelper helper, UserAPI userApi) {
		this.helper = helper;
		this.userAPI = userApi;
	}

	/**
	 * Returns the list of system properties that are useful to the UI layer.
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The JSON representation of configuration parameters.
	 */
	@GET
	@JSONP
	@NoCache
	@AccessControlAllowOrigin
	@InitRequestRequired
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response list(@Context final HttpServletRequest request) {
		try {

			final HttpSession session = request.getSession();
			String userId = (String) session.getAttribute(WebKeys.USER_ID);

			User user = null;

			if (userId != null) {
				user = this.userAPI.loadUserById( userId );
			}

			final Object menuData = this.helper.getMenuData(request);
			final Object configData = this.helper.getConfigurationData(request);
			// Return all configuration parameters in one response
			final Map<String, Object> configMap = CollectionsUtils.map(MENU, menuData, CONFIG, configData,
					USER, user != null ? user.toMap() : null);
			return Response.ok(new ResponseEntityView(configMap)).build();
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

}
