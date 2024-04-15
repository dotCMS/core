package com.dotcms.rest.api.v1.system;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageRuntimeException;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;

/**
 * This utility class assists the {@link AppContextInitResource} in merging
 * all the configuration-related information into one single response. In case a
 * specific property value requires authentication, and the user is not logged
 * in yet (for example, retrieving the list of navigation menu items), an empty
 * value will be returned for that single property.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Aug 1, 2016
 *
 */
@SuppressWarnings("serial")
public class AppConfigurationHelper implements Serializable {

	private final ConfigurationHelper configurationHelper;

	private static class SingletonHolder {
		private static final AppConfigurationHelper INSTANCE = new AppConfigurationHelper();
	}

	public static AppConfigurationHelper getInstance() {
		return AppConfigurationHelper.SingletonHolder.INSTANCE;
	}

	public AppConfigurationHelper() {
		this( ConfigurationHelper.INSTANCE);
	}

	@VisibleForTesting
	public AppConfigurationHelper(ConfigurationHelper configurationHelper) {

		this.configurationHelper = configurationHelper;
	}

	/**
	 * Returns a list of dotCMS configuration properties that are needed by the
	 * Angular UI.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The list of required system properties.
	 */
	public Map<String, Object> getConfigurationData(final HttpServletRequest request)  {
		try {
			return configurationHelper.getConfigProperties(request);
		} catch (LanguageException e) {
			throw new LanguageRuntimeException(e);
		}
	}
}
