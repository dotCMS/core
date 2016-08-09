package com.dotcms.rest.api.v1.system;

import static com.dotcms.util.CollectionsUtils.entry;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.mapEntries;
import static com.dotcms.util.HttpRequestDataUtil.getHostname;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_ENDPOINTS;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_BASEURL;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_PROTOCOL;
import static com.dotmarketing.util.WebKeys.WEBSOCKET_SYSTEMEVENTS_ENDPOINT;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.util.Config;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

/**
 * A utility class that provides the required dotCMS configuration properties to
 * the {@link ConfigurationResource} end-point. The idea behind this approach is
 * to provide other system modules - such as the UI - with access to specific
 * system properties as they are needed.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 26, 2016
 *
 */
@SuppressWarnings("serial")
public class ConfigurationHelper implements Serializable {

	public static final String EDIT_CONTENT_STRUCTURES_PER_COLUMN = "EDIT_CONTENT_STRUCTURES_PER_COLUMN";
	public static final String I18N_MESSAGES_MAP = "i18nMessagesMap";
	public static ConfigurationHelper INSTANCE = new ConfigurationHelper();

	/**
	 * Private class constructor for Singleton instantiation.
	 */
	private ConfigurationHelper() {

	}

	/**
	 * Reads the required configuration properties from the dotCMS configuration
	 * files and also from the {@link HttpServletRequest} object. New properties
	 * can be added as they are needed.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param locale
	 * 			  - The {@link Locale} for i18n.
	 * @return A {@link Map} with all the required system properties.
	 */
	public Map<String, Object> getConfigProperties(final HttpServletRequest request, final Locale locale) throws LanguageException {
		return map(
				DOTCMS_WEBSOCKET_PROTOCOL,
				Config.getStringProperty(DOTCMS_WEBSOCKET_PROTOCOL, "ws"),
				DOTCMS_WEBSOCKET_BASEURL,
				Config.getAsString(DOTCMS_WEBSOCKET_BASEURL, () -> getHostname(request)),
				DOTCMS_WEBSOCKET_ENDPOINTS,
				map(WEBSOCKET_SYSTEMEVENTS_ENDPOINT,
						Config.getStringProperty(WEBSOCKET_SYSTEMEVENTS_ENDPOINT, "/api/v1/system/events")),
				EDIT_CONTENT_STRUCTURES_PER_COLUMN,
				Config.getIntProperty(EDIT_CONTENT_STRUCTURES_PER_COLUMN, 15),
				I18N_MESSAGES_MAP,
				mapEntries(message("notifications_title", locale), // Notifications
					message("notifications_dismiss", locale), // Dismiss
					message("notifications_dismissall", locale), // Dismiss all
					message("relativetime.future", locale),
					message("relativetime.past", locale),
					message("relativetime.s", locale),
					message("relativetime.m", locale),
					message("relativetime.mm", locale),
					message("relativetime.h", locale),
					message("relativetime.hh", locale),
					message("relativetime.d", locale),
					message("relativetime.dd", locale),
					message("relativetime.M", locale),
					message("relativetime.MM", locale),
					message("relativetime.y", locale),
					message("relativetime.yy", locale)
				));
	}


	private static Map.Entry<String, String> message (final String message, final Locale locale) throws LanguageException {

		return entry(message, LanguageUtil.get(locale, message));
	}



}
