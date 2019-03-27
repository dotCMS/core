package com.dotcms.rest.api.v1.system;

import java.io.Serializable;
import java.util.Locale;
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
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.liferay.util.LocaleUtil;

/**
 * This Jersey end-point provides access to configuration parameters that are
 * set through the property files for dotCMS configuration:
 * {@code dotmarketing-config.properties}, and
 * {@code dotcms-config-cluster.properties}. By default, <b>not all
 * configuration properties are available through this end-point</b>, they must
 * be programmatically read and returned.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 22, 2016
 *
 */
@Path("/v1/configuration")
@SuppressWarnings("serial")
public class ConfigurationResource implements Serializable {

	private final ConfigurationHelper helper;

	/**
	 * Default constructor.
	 */
	public ConfigurationResource() {
		this.helper = ConfigurationHelper.INSTANCE;
	}

	/**
	 * Returns the list of system properties that are set through the dotCMS
	 * configuration files.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The JSON representation of configuration parameters.
	 */
	@GET
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response list(@Context final HttpServletRequest request) {
		try {

			final Locale locale = LocaleUtil.getLocale(request);
			final Map<String, Object> configPropsMap = helper.getConfigProperties(request, locale);
			return Response.ok(new ResponseEntityView(configPropsMap)).build();
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

}
