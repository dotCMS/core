package com.dotcms.rest.api.v1.system;

import static com.dotcms.util.CollectionsUtils.entry;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.mapEntries;
import static com.dotcms.util.HttpRequestDataUtil.getHostname;
import static com.dotmarketing.util.WebKeys.*;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rest.api.v1.system.websocket.SystemEventsWebSocketEndPoint;
import com.dotmarketing.util.Config;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.util.ReleaseInfo;
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
	public static final String WEB_SOCKET_SECURE_PROTOCOL = "wss";
	public static final String WEB_SOCKET_PROTOCOL = "ws";
	public static final String LICENSE = "license";
	public static final String IS_COMMUNITY = "isCommunity";
	public static final String DISPLAY_SERVER_ID = "displayServerId";
	public static final String LEVEL_NAME = "levelName";
	public static final String RELEASE_INFO = "releaseInfo";
	public static final String VERSION = "version";
	public static final String BUILD_DATE = "buildDate";
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
				Config.getAsString(DOTCMS_WEBSOCKET_PROTOCOL, () -> getWebSocketProtocol(request)),
				DOTCMS_WEBSOCKET_BASEURL,
				Config.getAsString(DOTCMS_WEBSOCKET_BASEURL, () -> getHostname(request)),
				DOTCMS_WEBSOCKET_ENDPOINTS,
				map(WEBSOCKET_SYSTEMEVENTS_ENDPOINT,
						Config.getStringProperty(WEBSOCKET_SYSTEMEVENTS_ENDPOINT, SystemEventsWebSocketEndPoint.API_WS_V1_SYSTEM_EVENTS)),
				EDIT_CONTENT_STRUCTURES_PER_COLUMN,
				Config.getIntProperty(EDIT_CONTENT_STRUCTURES_PER_COLUMN, 15),
				DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT,
				Config.getIntProperty(DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT, 1000),
				DOTCMS_DISABLE_WEBSOCKET_PROTOCOL,
				Boolean.valueOf( Config.getBooleanProperty(DOTCMS_DISABLE_WEBSOCKET_PROTOCOL, false) ),
				I18N_MESSAGES_MAP,
				mapEntries(
						message("notifications_title", locale), // Notifications
						message("notifications_dismiss", locale), // Dismiss
						message("notifications_dismissall", locale), // Dismiss all
						this.getRelativeTimeEntry(locale)
				),
				DOTCMS_PAGINATION_ROWS,
				Config.getIntProperty(DOTCMS_PAGINATION_ROWS, 10),
				DOTCMS_PAGINATION_LINKS,
				Config.getIntProperty(DOTCMS_PAGINATION_LINKS, 5),
				LICENSE,
				map(
						IS_COMMUNITY,      LicenseManager.getInstance().isCommunity(),
						DISPLAY_SERVER_ID, LicenseUtil.getDisplayServerId(),
						LEVEL_NAME,        LicenseUtil.getLevelName()
				),
				RELEASE_INFO,
				map(
						VERSION,           ReleaseInfo.getVersion(),
						BUILD_DATE,        ReleaseInfo.getBuildDateString()
				)
		);
	}

	private String getWebSocketProtocol (final HttpServletRequest request) {

		return request.isSecure()? WEB_SOCKET_SECURE_PROTOCOL : WEB_SOCKET_PROTOCOL;
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
