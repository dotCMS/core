package com.dotcms.rest.api.v1.system;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.api.v1.maintenance.JVMInfoResource;
import com.dotcms.rest.api.v1.pagescanner.PageScannerResource;
import com.dotmarketing.util.StringUtils;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dotcms.rest.WebResource;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * This is a public endpoint and requires no authentiction
 *
 * @author Jose Castro
 * @version 3.7
 * @since Jul 22, 2016
 *
 */
@Path("/v1/configuration")
@Tag(name = "System Configuration", description = "System configuration and company settings")
@SuppressWarnings("serial")
public class ConfigurationResource implements Serializable {

	private final ConfigurationHelper helper;
	private final WebResource webResource;
    private static final String REPORT_ISSUE_INCLUDE_USER_PII = "REPORT_ISSUE_INCLUDE_USER_PII";

    /**
     * Feature flag keys in WHITE_LIST that must be serialised as native JSON booleans.
     * All other WHITE_LIST entries (strings, numbers, lists) are left as-is.
     */
    private static final Set<String> BOOLEAN_FEATURE_FLAGS = ImmutableSet.of(
            FeatureFlagName.FEATURE_FLAG_EXPERIMENTS,
            FeatureFlagName.DOTFAVORITEPAGE_FEATURE_ENABLE,
            FeatureFlagName.FEATURE_FLAG_TEMPLATE_BUILDER_2,
            FeatureFlagName.FEATURE_FLAG_SEO_IMPROVEMENTS,
            FeatureFlagName.FEATURE_FLAG_SEO_PAGE_TOOLS,
            FeatureFlagName.FEATURE_FLAG_EDIT_URL_CONTENT_MAP,
            FeatureFlagName.FEATURE_FLAG_NEW_BINARY_FIELD,
            FeatureFlagName.FEATURE_FLAG_ANNOUNCEMENTS,
            FeatureFlagName.FEATURE_FLAG_NEW_EDIT_PAGE,
            FeatureFlagName.FEATURE_FLAG_UVE_PREVIEW_MODE,
            FeatureFlagName.FEATURE_FLAG_UVE_TOGGLE_LOCK,
            FeatureFlagName.FEATURE_FLAG_UVE_STYLE_EDITOR,
            FeatureFlagName.FEATURE_FLAG_PAGE_SCANNER,
            FeatureFlagName.FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION,
            FeatureFlagName.FEATURE_FLAG_NEW_BLOCK_EDITOR,
            "CONTENT_EDITOR2_ENABLED");  // FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED

	private static final Set<String> WHITE_LIST = ImmutableSet.copyOf(
			Config.getStringArrayProperty("CONFIGURATION_WHITE_LIST",
					new String[] {"EMAIL_SYSTEM_ADDRESS", "WYSIWYG_IMAGE_URL_PATTERN", "CHARSET","CONTENT_PALETTE_HIDDEN_CONTENT_TYPES", "DEFAULT_CONTAINER",
							FeatureFlagName.FEATURE_FLAG_EXPERIMENTS, FeatureFlagName.DOTFAVORITEPAGE_FEATURE_ENABLE, FeatureFlagName.FEATURE_FLAG_TEMPLATE_BUILDER_2,
					"SHOW_VIDEO_THUMBNAIL", "EXPERIMENTS_MIN_DURATION", "EXPERIMENTS_MAX_DURATION", "EXPERIMENTS_DEFAULT_DURATION", FeatureFlagName.FEATURE_FLAG_SEO_IMPROVEMENTS,
							FeatureFlagName.FEATURE_FLAG_SEO_PAGE_TOOLS, FeatureFlagName.FEATURE_FLAG_EDIT_URL_CONTENT_MAP, "CONTENT_EDITOR2_ENABLED", "CONTENT_EDITOR2_CONTENT_TYPE",
							FeatureFlagName.FEATURE_FLAG_NEW_BINARY_FIELD, FeatureFlagName.FEATURE_FLAG_ANNOUNCEMENTS, FeatureFlagName.FEATURE_FLAG_NEW_EDIT_PAGE,
							FeatureFlagName.FEATURE_FLAG_UVE_PREVIEW_MODE, FeatureFlagName.FEATURE_FLAG_UVE_TOGGLE_LOCK, FeatureFlagName.FEATURE_FLAG_UVE_STYLE_EDITOR,
							FeatureFlagName.FEATURE_FLAG_PAGE_SCANNER,
							PageScannerResource.API_URL_PROPERTY,
                            FeatureFlagName.FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION,
                            FeatureFlagName.FEATURE_FLAG_NEW_BLOCK_EDITOR,
                            "REPORT_ISSUE_INCLUDE_USER_PII" }));

	private boolean isOnBlackList(final String key) {
		return null != JVMInfoResource.obfuscatePattern ? JVMInfoResource.obfuscatePattern.matcher(key).find() : false;
	}

	/**
	 * Default constructor.
	 */
	public ConfigurationResource() {
		this.helper = ConfigurationHelper.INSTANCE;
		this.webResource = new WebResource();
	}

	/**
	 * Test constructor — allows injecting a mock {@link WebResource}.
	 */
	ConfigurationResource(final WebResource webResource) {
		this.helper = ConfigurationHelper.INSTANCE;
		this.webResource = webResource;
	}

	/**
	 * Retrieve the keys from dotcms Configuration (allowed on WHITE_LIST and are not restricted by the BLACK_LIST)
	 * @param request
	 * @param response
	 * @param keysQuery
	 * @return
	 * @throws IOException
	 */
	@Path("/config")
	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response getConfigVariables(@Context final HttpServletRequest request,
											 @Context final HttpServletResponse response,
											 @QueryParam("keys") final String keysQuery) {

		new WebResource.InitBuilder(webResource)
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

		return Response.ok(new ResponseEntityView(resultMap)).build();
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
            final String propertyKey = key.replace("boolean:", StringPool.BLANK);
            final boolean defaultValue = REPORT_ISSUE_INCLUDE_USER_PII.equals(propertyKey);
			return Config.getBooleanProperty(propertyKey, defaultValue);
		} else if (key.startsWith("number:")) {

			return Config.getIntProperty(key.replace("number:", StringPool.BLANK), 0);
		}

		if (BOOLEAN_FEATURE_FLAGS.contains(key)) {
			return parseBooleanFlag(key);
		}

		return Config.getStringProperty(key, "NOT_FOUND");
	}

	/**
	 * Resolves a feature flag property to a native boolean, or the sentinel "NOT_FOUND"
	 * when the key is not defined anywhere (no .properties entry, no DOT_* env override).
	 * Accepted truthy values (case-insensitive, whitespace-trimmed): "true", "1".
	 * Accepted falsy values: "false", "0", "".
	 * Unrecognised values are logged as WARN and resolve to false.
	 */
	private static Object parseBooleanFlag(final String key) {
		final String rawValue = Config.getStringProperty(key, null);
		if (rawValue == null) {
			return "NOT_FOUND";
		}
		final String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
		switch (normalized) {
			case "true":
			case "1":
				return Boolean.TRUE;
			case "false":
			case "0":
			case "":
				return Boolean.FALSE;
			default:
				Logger.warn(ConfigurationResource.class,
						"Feature flag '" + key + "' has unrecognized value '" + rawValue + "'; treating as false.");
				return Boolean.FALSE;
		}
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

	/**
	 * Set value to config properties in runtime
	 *
	 * @param request
	 * @return
	 */
	@PUT
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	public Response set(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
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

	@POST
	@Path("/_validateCompanyEmail")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response validateEmail(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			final CompanyEmailForm form) throws ExecutionException, InterruptedException {

		final InitDataObject dataObject = new InitBuilder(request, response)
				.requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
				.requiredPortlet("maintenance")
				.rejectWhenNoUser(true)
				.init();

		final Tuple2<String, String> mailAndSender = helper.parseMailAndSender(form.getSenderAndEmail());
		helper.sendValidationEmail(mailAndSender._1, mailAndSender._2, dataObject.getUser());
		return Response.ok(new ResponseEntityView(OK)).build();
	}

}
