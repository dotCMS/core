package com.dotcms.rest.api.v1.system;

import static com.dotcms.rest.ResponseEntityView.OK;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityMapView;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.api.v1.maintenance.JVMInfoResource;
import com.dotmarketing.util.StringUtils;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dotcms.rest.WebResource;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Config;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
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
 * This is a public endpoint and requires no authentiction
 *
 * @author Jose Castro
 * @version 3.7
 * @since Jul 22, 2016
 *
 */
@SwaggerCompliant(value = "System administration and configuration APIs", batch = 4)
@Path("/v1/configuration")
@Tag(name = "System Configuration")
@SuppressWarnings("serial")
public class ConfigurationResource implements Serializable {

	private final ConfigurationHelper helper;

	private static final Set<String> WHITE_LIST = ImmutableSet.copyOf(
			Config.getStringArrayProperty("CONFIGURATION_WHITE_LIST",
					new String[] {"EMAIL_SYSTEM_ADDRESS", "WYSIWYG_IMAGE_URL_PATTERN", "CHARSET","CONTENT_PALETTE_HIDDEN_CONTENT_TYPES",
							FeatureFlagName.FEATURE_FLAG_EXPERIMENTS, FeatureFlagName.DOTFAVORITEPAGE_FEATURE_ENABLE, FeatureFlagName.FEATURE_FLAG_TEMPLATE_BUILDER_2,
					"SHOW_VIDEO_THUMBNAIL", "EXPERIMENTS_MIN_DURATION", "EXPERIMENTS_MAX_DURATION", "EXPERIMENTS_DEFAULT_DURATION", FeatureFlagName.FEATURE_FLAG_SEO_IMPROVEMENTS,
							FeatureFlagName.FEATURE_FLAG_SEO_PAGE_TOOLS, FeatureFlagName.FEATURE_FLAG_EDIT_URL_CONTENT_MAP, "CONTENT_EDITOR2_ENABLED", "CONTENT_EDITOR2_CONTENT_TYPE",
							FeatureFlagName.FEATURE_FLAG_NEW_BINARY_FIELD, FeatureFlagName.FEATURE_FLAG_ANNOUNCEMENTS, FeatureFlagName.FEATURE_FLAG_NEW_EDIT_PAGE,
							FeatureFlagName.FEATURE_FLAG_UVE_PREVIEW_MODE  }));


	private boolean isOnBlackList(final String key) {

		return null != JVMInfoResource.obfuscatePattern ? JVMInfoResource.obfuscatePattern.matcher(key).find() : false;
	}
	/**
	 * Default constructor.
	 */
	public ConfigurationResource() {
		this.helper = ConfigurationHelper.INSTANCE;
	}

	/**
	 * Retrieve the keys from dotcms Configuration (allowed on WHITE_LIST and are not restricted by the BLACK_LIST)
	 * @param request
	 * @param response
	 * @param keysQuery
	 * @return
	 * @throws IOException
	 */
	@Operation(
		summary = "Get configuration variables",
		description = "Retrieve specific configuration keys from dotCMS (allowed on WHITE_LIST and not restricted by BLACK_LIST)"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Configuration variables retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - backend user required",
					content = @Content(mediaType = "application/json"))
	})
	@Path("/config")
	@GET
	@JSONP
	@NoCache
	@Produces(MediaType.APPLICATION_JSON)
	public final Response getConfigVariables(@Context final HttpServletRequest request,
											 @Context final HttpServletResponse response,
											 @Parameter(description = "Comma-separated list of configuration keys to retrieve", required = false)
											 @QueryParam("keys") final String keysQuery)
			throws IOException {

		new WebResource.InitBuilder(request, response)
				.requiredBackendUser(true)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

		final String [] keys = StringUtils.splitByCommas(keysQuery);
		final Map<String,Object> resultMap = new LinkedHashMap<>();
		if (null != keys) {

			for (final String key : keys) {

				final String keyWithoutPrefix = this.removePrefix (key);
				if (this.WHITE_LIST.contains(keyWithoutPrefix) && !this.isOnBlackList(keyWithoutPrefix)) {

					resultMap.put(keyWithoutPrefix, recoveryFromConfig(key));
				}
			}
		}

		return Response.ok(new ResponseEntityView<>(resultMap)).build();
	}

	private String removePrefix (final String key) {

		return key.replace("list:", StringPool.BLANK)
				.replace("boolean:", StringPool.BLANK)
				.replace("number:", StringPool.BLANK);
	}


	private Object recoveryFromConfig (final String key) {

		if (key.startsWith("list:")) {

			return Arrays.asList(Config.getStringArrayProperty(key.replace("list:", StringPool.BLANK), new String[]{}));
		} else if(key.startsWith("boolean:")) {

			return Config.getBooleanProperty(key.replace("boolean:", StringPool.BLANK), false);
		} else if (key.startsWith("number:")) {

			return Config.getIntProperty(key.replace("number:", StringPool.BLANK), 0);
		}

		return Config.getStringProperty(key, "NOT_FOUND");
	}

	/**
	 * Returns the list of system properties that are set through the dotCMS
	 * configuration files.
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The JSON representation of configuration parameters.
	 */
	@Operation(
		summary = "List configuration properties",
		description = "Returns the list of system properties that are set through the dotCMS configuration files"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Configuration properties retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapView.class))),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON })
	public Response list(@Context final HttpServletRequest request) {
		try {

			final Locale locale = LocaleUtil.getLocale(request);
			final Map<String, Object> configPropsMap = helper.getConfigProperties(request, locale);
			return Response.ok(new ResponseEntityView<>(configPropsMap)).build();
		} catch (Exception e) {
			// In case of unknown error, so we report it as a 500
			return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Set value to config properties in runtime
	 *
	 * @param request
	 * @return
	 */
	@Operation(
		summary = "Set configuration properties",
		description = "Set values to configuration properties at runtime"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Configuration properties set successfully (no body)"),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - CMS Administrator role and maintenance portlet access required",
					content = @Content(mediaType = "application/json"))
	})
	@PUT
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response set(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Map of configuration properties to set", 
				required = true,
				content = @Content(schema = @Schema(type = "object"))
			)
			Map<String, String> properties) {

		new WebResource
				.InitBuilder(request, response)
				.requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
				.requiredPortlet("maintenance")
				.rejectWhenNoUser(true)
				.init();

		for (Map.Entry<String, String> entry : properties.entrySet()) {
			Config.setProperty(entry.getKey(), entry.getValue());
		}

		return Response.ok().build();
	}

	@Operation(
		summary = "Validate company email",
		description = "Send a validation email to verify the company email configuration"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Validation email sent successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityStringView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - CMS Administrator role and maintenance portlet access required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@Path("/_validateCompanyEmail")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response validateEmail(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Company email form data", 
				required = true,
				content = @Content(schema = @Schema(implementation = CompanyEmailForm.class))
			)
			final CompanyEmailForm form) throws ExecutionException, InterruptedException {

		final InitDataObject dataObject = new InitBuilder(request, response)
				.requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
				.requiredPortlet("maintenance")
				.rejectWhenNoUser(true)
				.init();

		final Tuple2<String, String> mailAndSender = helper.parseMailAndSender(form.getSenderAndEmail());
		helper.sendValidationEmail(mailAndSender._1, mailAndSender._2, dataObject.getUser());
		return Response.ok(new ResponseEntityView<>(OK)).build();
	}

}
