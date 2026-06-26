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
import com.dotmarketing.util.UtilMethods;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
     *
     * <p><b>Maintenance rule:</b> every key added here MUST also be present in WHITE_LIST.
     * The wire format is the normalised lowercase string {@code "true"} or {@code "false"} —
     * frontend callers should compare with {@code === 'true'}.  Adding a key here without
     * also adding it to WHITE_LIST will silently exclude it from the response.
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
            FeatureFlagName.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED,
            FeatureFlagName.FEATURE_FLAG_LOCALE_SELECTOR_V2,
            FeatureFlagName.FEATURE_FLAG_NEW_IMAGE_EDITOR,
            // libvips engine toggle: the new image editor reads it to gate AVIF output.
            FeatureFlagName.IMAGE_API_USE_LIBVIPS);

	private static final Set<String> WHITE_LIST = ImmutableSet.copyOf(
			Config.getStringArrayProperty("CONFIGURATION_WHITE_LIST",
					new String[] {"EMAIL_SYSTEM_ADDRESS", "WYSIWYG_IMAGE_URL_PATTERN", "CHARSET","CONTENT_PALETTE_HIDDEN_CONTENT_TYPES", "DEFAULT_CONTAINER",
							FeatureFlagName.FEATURE_FLAG_EXPERIMENTS, FeatureFlagName.DOTFAVORITEPAGE_FEATURE_ENABLE, FeatureFlagName.FEATURE_FLAG_TEMPLATE_BUILDER_2,
					"SHOW_VIDEO_THUMBNAIL", "EXPERIMENTS_MIN_DURATION", "EXPERIMENTS_MAX_DURATION", "EXPERIMENTS_DEFAULT_DURATION", FeatureFlagName.FEATURE_FLAG_SEO_IMPROVEMENTS,
							FeatureFlagName.FEATURE_FLAG_SEO_PAGE_TOOLS, FeatureFlagName.FEATURE_FLAG_EDIT_URL_CONTENT_MAP, FeatureFlagName.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED, "CONTENT_EDITOR2_CONTENT_TYPE",
							FeatureFlagName.FEATURE_FLAG_NEW_BINARY_FIELD, FeatureFlagName.FEATURE_FLAG_ANNOUNCEMENTS, FeatureFlagName.FEATURE_FLAG_NEW_EDIT_PAGE,
							FeatureFlagName.FEATURE_FLAG_UVE_PREVIEW_MODE, FeatureFlagName.FEATURE_FLAG_UVE_TOGGLE_LOCK, FeatureFlagName.FEATURE_FLAG_UVE_STYLE_EDITOR,
							FeatureFlagName.FEATURE_FLAG_PAGE_SCANNER,
							PageScannerResource.API_URL_PROPERTY,
                            FeatureFlagName.FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION,
                            FeatureFlagName.FEATURE_FLAG_NEW_BLOCK_EDITOR,
                            REPORT_ISSUE_INCLUDE_USER_PII,
                            FeatureFlagName.FEATURE_FLAG_REPORT_ISSUE_ENABLED,
                            FeatureFlagName.FEATURE_FLAG_LOCALE_SELECTOR_V2,
                            FeatureFlagName.FEATURE_FLAG_NEW_IMAGE_EDITOR,
                            // libvips engine toggle: the new image editor reads it to gate AVIF output.
                            FeatureFlagName.IMAGE_API_USE_LIBVIPS }));

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
	 * Returns a subset of dotCMS configuration values for the requested keys.
	 * Only keys present in {@code WHITE_LIST} and not blocked by the obfuscation
	 * blacklist are included in the response; unknown or blacklisted keys are
	 * silently omitted.
	 *
	 * @param request   the current HTTP request
	 * @param response  the current HTTP response
	 * @param keysQuery comma-separated list of configuration keys to retrieve;
	 *                  keys may carry a type prefix ({@code list:}, {@code boolean:},
	 *                  {@code number:}) to control deserialisation
	 * @return 200 with a {@code Map} of key → value pairs
	 */
	@Path("/config")
	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	@Operation(
			operationId = "getConfigVariables",
			summary = "Retrieve whitelisted configuration values",
			description = "Returns a map of configuration key to value for each requested key "
					+ "that is present in the server whitelist. "
					+ "Boolean feature-flag keys (those in BOOLEAN_FEATURE_FLAGS) are normalised to "
					+ "the lowercase strings \"true\" or \"false\" regardless of how they are stored "
					+ "in the properties file; undefined flags return the sentinel string \"NOT_FOUND\". "
					+ "Keys prefixed with \"number:\" return an Integer, \"list:\" returns an array of "
					+ "strings, \"boolean:\" returns a native JSON boolean. All other whitelisted keys "
					+ "return their raw string value. Keys not on the whitelist are silently excluded.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Map of key to configuration value",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(type = "object",
											description = "Map of configuration key to value. "
													+ "Value type depends on the key: normalised string \"true\"/\"false\" "
													+ "for boolean feature flags, \"NOT_FOUND\" for undefined flags, "
													+ "Integer for number:-prefixed keys, array for list:-prefixed keys, "
													+ "native boolean for boolean:-prefixed keys, raw string otherwise.",
											example = "{\"FEATURE_FLAG_EXPERIMENTS\":\"true\","
													+ "\"EMAIL_SYSTEM_ADDRESS\":\"admin@example.com\","
													+ "\"FEATURE_FLAG_UVE_STYLE_EDITOR\":\"NOT_FOUND\"}"))),
					@ApiResponse(responseCode = "401", description = "User is not authenticated")
			}
	)
	public final Response getConfigVariables(@Context final HttpServletRequest request,
											 @Context final HttpServletResponse response,
											 @QueryParam("keys") final String keysQuery) {

		new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

		final Map<String,Object> resultMap = new LinkedHashMap<>();
		if (!UtilMethods.isSet(keysQuery)) {
			return Response.ok(new ResponseEntityView(resultMap)).build();
		}

		final String[] keys = StringUtils.splitByCommas(keysQuery);
		if (null != keys) {

			for (final String key : keys) {

				final String keyWithoutPrefix = this.removePrefix(key);
				if (WHITE_LIST.contains(keyWithoutPrefix) && !this.isOnBlackList(keyWithoutPrefix)) {

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
	 * Normalises a feature flag property to the canonical lowercase string {@code "true"} or
	 * {@code "false"}, preserving the existing string wire format so that consumers built
	 * against the pre-existing contract continue to work without changes.
	 * Returns the sentinel {@code "NOT_FOUND"} when the key is not defined anywhere
	 * (no .properties entry, no DOT_* env override).
	 *
	 * <p>Accepted truthy values (case-insensitive, whitespace-trimmed): {@code "true"}, {@code "1"}.
	 * Accepted falsy values: {@code "false"}, {@code "0"}, {@code ""}.
	 * Unrecognised values are logged as WARN and normalise to {@code "false"}.
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
				return "true";
			case "false":
			case "0":
			case "":
				return "false";
			default:
				Logger.warn(ConfigurationResource.class,
						() -> "Feature flag '" + key + "' has unrecognized value '" + rawValue + "'; treating as false.");
				return "false";
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
