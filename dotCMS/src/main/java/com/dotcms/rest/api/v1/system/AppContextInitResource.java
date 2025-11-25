package com.dotcms.rest.api.v1.system;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.AccessControlAllowOrigin;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Map;

/**
 * This Jersey end-point provides access to configuration parameters that are
 * useful to the dotCMS Angular UI. System properties set through dotCMS
 * configuration files, and the menu items that logged in users can see in their
 * navigation bar, are just a couple of configuration properties that this
 * end-point can provide.
 * <p>
 * The number of configuration properties my vary depending on whether the user
 * is logged in or not before calling this end-point. For example, the list of
 * navigation menu items <b>will be returned as an empty list</b>
 *
 * This is a public endpoint and requires no authentiction.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 22, 2016
 *
 */
@Path("/v1/appconfiguration")
@SuppressWarnings("serial")
public class AppContextInitResource implements Serializable {

	private static final String CONFIG = "config";

	private final AppConfigurationHelper helper;

	/**
	 * Default constructor.
	 */
	public AppContextInitResource() {
		this( AppConfigurationHelper.getInstance());
	}

	@VisibleForTesting
	public AppContextInitResource(AppConfigurationHelper helper) {
		this.helper = helper;
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
			final Object configData = this.helper.getConfigurationData(request);
			// Return all configuration parameters in one response
			final Map<String, Object> configMap = Map.of(CONFIG, configData);

			return Response.ok(new ResponseEntityView(configMap)).build();
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

}
