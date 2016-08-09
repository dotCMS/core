package com.dotcms.rest.api.v1.system;

import static com.dotcms.util.CollectionsUtils.map;
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
				map("notifications_title", // Notifications
						LanguageUtil.get(locale, "notifications_title"),
					"notifications_dismiss", // Dismiss
						LanguageUtil.get(locale, "notifications_dismiss"),
					"notifications_dismissall", // Dismiss all
						LanguageUtil.get(locale, "notifications_dismissall"),
					"more-than-a-year-ago", // more than a year ago.
						LanguageUtil.get(locale, "more-than-a-year-ago"),
					"x-months-ago", // {0} months ago.
						LanguageUtil.get(locale, "x-months-ago"),
					"last-month",  // last month.
						LanguageUtil.get(locale, "last-month"),
					"x-weeks-ago", // {0} weeks ago.
						LanguageUtil.get(locale, "x-weeks-ago"),
					"last-week",  // last week
						LanguageUtil.get(locale, "last-week"),
					"x-days-ago",  // {0} days ago
						LanguageUtil.get(locale, "x-days-ago"),
					"yesterday",  // Yesterday
						LanguageUtil.get(locale, "yesterday"),
					"x-hours-ago",  // {0} hours ago
						LanguageUtil.get(locale, "x-hours-ago"),
					"an-hour-ago",  // an hour ago
						LanguageUtil.get(locale, "an-hour-ago"),
					"x-minutes-ago",  // {0} minutes ago
						LanguageUtil.get(locale, "x-minutes-ago"),
					"seconds-ago",  // seconds ago
						LanguageUtil.get(locale, "seconds-ago")
					)
				);
	}



}
