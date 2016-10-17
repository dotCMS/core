package com.dotcms.rest.api.v1.system;

import static com.dotcms.util.CollectionsUtils.entry;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.mapEntries;
import static com.dotcms.util.HttpRequestDataUtil.getHostname;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_ENDPOINTS;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_BASEURL;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_PROTOCOL;
import static com.dotmarketing.util.WebKeys.WEBSOCKET_SYSTEMEVENTS_ENDPOINT;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.util.Config;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.LocaleUtil;

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
				DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT,
				Config.getIntProperty(DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT, 1000),
				I18N_MESSAGES_MAP,
				mapEntries(
						message("notifications_title", locale), // Notifications
						message("notifications_dismiss", locale), // Dismiss
						message("notifications_dismissall", locale), // Dismiss all
						this.getRelativeTimeEntry(locale)
				)
				);
	}

	/**
	 * Reads the required configuration properties from the dotCMS configuration
	 * files and also from the {@link HttpServletRequest} object. New properties
	 * can be added as they are needed.
	 *
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return A {@link Map} with all the required system properties.
	 */
	public Map<String, Object> getConfigProperties(final HttpServletRequest request) throws LanguageException {
		final Locale locale = LocaleUtil.getLocale(request);
		return getConfigProperties(request, locale);
	}

	private Map.Entry<String, Object> getRelativeTimeEntry(final Locale locale) throws LanguageException {

		return entry("relativeTime",
				mapEntries(
						message("future", "relativetime.future", locale),
						message("past", "relativetime.past", locale),
						message("s", "relativetime.s", locale),
						message("m", "relativetime.m", locale),
						message("mm", "relativetime.mm", locale),
						message("h", "relativetime.h", locale),
						message("hh", "relativetime.hh", locale),
						message("d", "relativetime.d", locale),
						message("dd", "relativetime.dd", locale),
						message("M", "relativetime.M", locale),
						message("MM", "relativetime.MM", locale),
						message("y", "relativetime.y", locale),
						message("yy", "relativetime.yy", locale)
				));

	}

	private static Map.Entry<String, Object> message (final String key, final String message, final Locale locale) throws LanguageException {

		return entry(key, LanguageUtil.get(locale, message));
	}

	private static Map.Entry<String, Object> message (final String message, final Locale locale) throws LanguageException {

		return message(message, message, locale);
	}



}
