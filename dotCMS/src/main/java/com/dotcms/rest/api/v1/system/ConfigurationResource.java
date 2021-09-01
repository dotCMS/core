package com.dotcms.rest.api.v1.system;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotmarketing.util.StringUtils;
import com.google.common.collect.ImmutableSet;
import io.vavr.Tuple2;

import java.io.IOException;
import java.io.Serializable;
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
@SuppressWarnings("serial")
public class ConfigurationResource implements Serializable {

	private final ConfigurationHelper helper;

	private static final Pattern BLACK_LIST = Try.of(()->Pattern.compile(
			Config.getStringProperty("OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES", "passw|pass|passwd|secret|key|token"),
			Pattern.CASE_INSENSITIVE)).getOrNull();

	private static final Set<String> WHITE_LIST = ImmutableSet.copyOf(
			Config.getStringArrayProperty("CONFIGURATION_WHITE_LIST",
					new String[] {"EMAIL_SYSTEM_ADDRESS", "CHARSET","DEFAULT_LANGUAGE_COUNTRY","DEFAULT_LANGUAGE",
					"DEFAULT_LANGUAGE_CODE","DEFAULT_LANGUAGE_STR", "DEFAULT_LANGUAGE_COUNTRY_CODE", "CMS_STRUTS_PATH",
					"PATH_TO_REDIRECT", "PATH_TO_IMAGES", "REPORT_PATH", "ASSET_PATH", "CONTENT_AUTOSAVE_INTERVAL",
					"DEFAULT_HEIGHT","DEFAULT_WIDTH", "DEFAULT_BG_R_COLOR", "DEFAULT_BG_G_COLOR", "DEFAULT_BG_B_COLOR",
					"ACCRUE_TAGS_IN_URLMAPS","ACCRUE_TAGS_IN_PAGES", "ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", "PULLPERSONALIZED_PERSONA_WEIGHT",
					"ASSETS_SEARCH_AND_REPLACE_ALLOWED_FILE_TYPES","CONTENT_VERSION_HARD_LINK", "CONTENT_ALLOW_ZERO_LENGTH_FILES", "DEFAULT_PAGE_CACHE_SECONDS",
					"ENABLE_NAV_PERMISSION_CHECK","CONTENT_ESCAPE_HTML_TEXT", "CMS_INDEX_PAGE", "DEFAULT_REST_PAGE_COUNT",	"HEADLESS_USER_CONTENT_DELIVERY",
					"DEFAULT_VANITY_URL_TO_DEFAULT_LANGUAGE","WHITELISTED_HEADERS", "WHITELISTED_PARAMS", "WHITELISTED_COOKIES"}));



	private boolean isOnBlackList(final String key) {

		return null != BLACK_LIST? BLACK_LIST.matcher(key).find() : false;
	}
	/**
	 * Default constructor.
	 */
	public ConfigurationResource() {
		this.helper = ConfigurationHelper.INSTANCE;
	}

	@Path("/config")
	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response getConfigVariables(@Context final HttpServletRequest request,
											 @Context final HttpServletResponse response,
											 @QueryParam("keys") final String keysQuery)
			throws IOException {

		// todo: not sure about this
		final InitDataObject initData = new WebResource.InitBuilder(request, response)
				.requiredBackendUser(true)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.requiredPortlet("maintenance")
				.init();

		final String [] keys = StringUtils.splitByCommas(keysQuery);
		final Map<String,Object> resultMap = new LinkedHashMap<>();
		if (null != keys) {

			for (final String key : keys) {

				if (this.WHITE_LIST.contains(key) && !this.isOnBlackList(key)) {

					resultMap.put(key, Config.getStringProperty(key, "NOT_FOUND"));
				}
			}
		}

		return Response.ok(resultMap).build();
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
