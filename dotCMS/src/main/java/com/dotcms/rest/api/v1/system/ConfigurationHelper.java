package com.dotcms.rest.api.v1.system;

import static com.dotcms.util.CollectionsUtils.entry;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.mapEntries;
import static com.dotmarketing.util.WebKeys.DOTCMS_DISABLE_WEBSOCKET_PROTOCOL;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT;

import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.ReleaseInfo;
import com.liferay.util.LocaleUtil;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.routines.EmailValidator;

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
	public static final String LICENSE_LEVEL = "level";
	public static final String RELEASE_INFO = "releaseInfo";
	public static final String VERSION = "version";
	public static final String BUILD_DATE = "buildDate";
	public static final String EMAIL_REGEX = "emailRegex";
	public static final String BACKGROUND_COLOR = "background";
	public static final String PRIMARY_COLOR = "primary";
	public static final String SECONDARY_COLOR = "secondary";
	public static final String COLORS = "colors";
	public static final String LANGUAGES = "languages";
	public static final String TIMEZONES = "timezones";
	public static final String CLUSTER = "cluster";
	public static final String CLUSTER_ID = "clusterId";
	public static final String KEY_DIGEST = "companyKeyDigest";
	public static final String LOGOS = "logos";
	public static final String LOGIN_SCREEN_LOGO = "loginScreen";
	public static final String NAV_BAR_LOGO = "navBar";
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

		final User user = PortalUtil.getUser(request);
	    String backgroundColor = "NA";
		String primaryColor = "NA";
		String secondaryColor = "NA";
		String loginScreenLogo = Try.of(() -> APILocator.getCompanyAPI().getDefaultCompany().getCity()).getOrElse("NA");
		loginScreenLogo = UtilMethods.isSet(loginScreenLogo) && loginScreenLogo.startsWith("/dA") ? loginScreenLogo : "NA";
		String navBarLogo = Try.of(() -> APILocator.getCompanyAPI().getDefaultCompany().getState()).getOrElse("NA");
		navBarLogo = LicenseUtil.getLevel() > LicenseLevel.COMMUNITY.level &&
				UtilMethods.isSet(navBarLogo) && navBarLogo.startsWith("/dA") ? navBarLogo : "NA";

	    try {
			backgroundColor = APILocator.getCompanyAPI().getDefaultCompany().getSize();
			primaryColor = APILocator.getCompanyAPI().getDefaultCompany().getType();
			secondaryColor = APILocator.getCompanyAPI().getDefaultCompany().getStreet();
		}catch(Exception e){
			Logger.warn(this.getClass(), "unable to get color:" +e.getMessage());
		}

		final Map<String, Object> map = map(
				EDIT_CONTENT_STRUCTURES_PER_COLUMN,
				Config.getIntProperty(EDIT_CONTENT_STRUCTURES_PER_COLUMN, 15),
				DOTCMS_WEBSOCKET,
				map(
					DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT,
					Config.getIntProperty(DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT, 15000),
					DOTCMS_DISABLE_WEBSOCKET_PROTOCOL,
					Boolean.valueOf(Config.getBooleanProperty(DOTCMS_DISABLE_WEBSOCKET_PROTOCOL, false))
				),
				I18N_MESSAGES_MAP,
				mapEntries(
						message("notifications_title", locale), // Notifications
						message("notifications_dismiss", locale), // Dismiss
						message("notifications_dismissall", locale), // Dismiss all
						this.getRelativeTimeEntry(locale)
				),
				LICENSE,
				map(
						IS_COMMUNITY, LicenseManager.getInstance().isCommunity(),
						DISPLAY_SERVER_ID, LicenseUtil.getDisplayServerId(),
						LEVEL_NAME, LicenseUtil.getLevelName(),
						LICENSE_LEVEL, LicenseUtil.getLevel()

				),
				RELEASE_INFO,
				map(
						VERSION, ReleaseInfo.getVersion(),
						BUILD_DATE, ReleaseInfo.getBuildDateString()
				),
				EMAIL_REGEX, Constants.REG_EX_EMAIL,
				COLORS,
				map(
						BACKGROUND_COLOR, backgroundColor,
						PRIMARY_COLOR, primaryColor,
						SECONDARY_COLOR, secondaryColor
				),
				CLUSTER, clusterMap(user),
				LOGOS,
				map(
						LOGIN_SCREEN_LOGO,loginScreenLogo,
						NAV_BAR_LOGO,navBarLogo
				)
		);

	    map.put(LANGUAGES, APILocator.getLanguageAPI().getLanguages());
	    map.put(TIMEZONES, getTimeZones(locale));
	    return map;
	}

    /**
     * Returns a list of all available timezones
     * @param locale
     * @return
     */
	private List getTimeZones(final Locale locale){

        final List<Map<String, Object>> timeZoneList = new ArrayList<>();
        final String[] timeZonesIDs = TimeZone.getAvailableIDs();
        Arrays.sort(timeZonesIDs);
        for(final String id : timeZonesIDs) {
            final TimeZone timeZone = TimeZone.getTimeZone(id);
            timeZoneList.add(CollectionsUtils.map("id", timeZone.getID(), "label",
                    timeZone.getDisplayName(locale) + " (" + timeZone
                            .getID() + ")", "offset", timeZone.getRawOffset()));
        }
        return timeZoneList;
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

	/**
	 * We only need to show the Key if's an authenticated user
	 * Since it's kind of sensitive.
	 * @param user
	 * @return
	 */
	Map<String,String> clusterMap(final User user){
		final boolean validAuthenticatedUser = null != user && !user.isAnonymousUser() && user.isBackendUser();
		 if(validAuthenticatedUser){
			return map(
					 CLUSTER_ID, getClusterId(),
					 KEY_DIGEST, keyDigest()
			 );
		 } else {
			 return map(
					 CLUSTER_ID, getClusterId()
			 );
		 }
	}

	/**
	 *
	 * @return
	 */
	public static String getClusterId(){
		return ClusterFactory.getClusterId();
    }

	/**
	 *
	 * @return
	 */
	private static String keyDigest(){
      try {
		  return APILocator.getCompanyAPI().getDefaultCompany().getKeyDigest();
	  }catch (Exception e){
	     Logger.error(ConfigurationHelper.class, "Failed to retrieve key digest." ,e);
	  }
	  return null;
    }

	/**
	 * Asynchronous e-mail validation
	 * the result is communicated via system wide notification to the user
	 * @param email
	 * @return
	 * @throws DotDataValidationException
	 */
	public void validateEmail(final String email, final User user)
			throws ExecutionException, InterruptedException {

		if (!EmailValidator.getInstance().isValid(email)) {
			throw new IllegalArgumentException(
					String.format("[%s] is not a valid email.", email));
		}

		final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance()
				.getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
				CompletableFuture.runAsync(() -> {
					final String mailName = "Company configured address.";
					final Mailer mail = new Mailer();
					mail.setToName(mailName);
					mail.setToEmail(email);
					mail.setFromEmail(email);
					mail.setFromName(mailName);
					mail.setSubject("dotCMS Test email.");
					mail.setHTMLAndTextBody("This email was successfully sent by dotCMS.");
					final boolean result = mail.sendMessage();
					systemNotifyTestEmail(user, email, result);
				}, dotSubmitter);
	}

	/**
	 * given the result on the asynchronous operation this doe take care of assembling a text notification
	 * @param user
	 * @param email
	 * @param sendResult
	 */
	private void systemNotifyTestEmail(final User user, final String email,
			final boolean sendResult) {
		final String languageKey = sendResult ? "email-address-test-sent" : "email-address-test-sent-fail";
		final String fallbackText = sendResult ? String.format("e-mail successfully sent to %s", email)
						: String.format("Unable to send e-mail to %s", email);
		final String message = Try.of(() -> LanguageUtil.get(user.getLocale(), languageKey, email))
				.getOrElse(fallbackText);
		final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();
		systemMessageEventUtil.pushSimpleTextEvent(message);
	}

}
