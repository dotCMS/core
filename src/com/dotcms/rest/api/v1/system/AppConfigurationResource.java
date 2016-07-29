package com.dotcms.rest.api.v1.system;

import java.io.Serializable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import com.dotcms.rest.api.v1.menu.MenuResource;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.CollectionsUtils;

/**
 * This Jersey end-point provides access to configuration parameters that are
 * useful to the dotCMS Angular UI. System properties set through dotCMS
 * configuration files, and the menu items that logged in users can see in their
 * navigation bar, are just a couple of configuration properties that this
 * end-point can provide.
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

	/**
	 * Default constructor.
	 */
	public AppConfigurationResource() {
		
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
			final MenuResource menuResource = new MenuResource();
			final Response menuResponse = menuResource.getMenus(MenuResource.App.CORE_WEB.name(), request);
			final ConfigurationResource configurationResource = new ConfigurationResource();
			final Response configurationResponse = configurationResource.list(request);
			// Return all configuration parameters in one response
			final Map<String, Object> configMap = CollectionsUtils.map(
					MENU, ResponseEntityView.class.cast(menuResponse.getEntity()).getEntity(), 
					CONFIG, ResponseEntityView.class.cast(configurationResponse.getEntity()).getEntity());
			return Response.ok(new ResponseEntityView(configMap)).build();
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

}
