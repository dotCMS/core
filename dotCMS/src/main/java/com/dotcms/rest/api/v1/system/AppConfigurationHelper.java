package com.dotcms.rest.api.v1.system;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.dotcms.cms.login.LoginService;
import com.dotcms.cms.login.LoginServiceFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.menu.MenuResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LoginAsAPI;
import com.dotcms.api.web.WebSessionContext;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageRuntimeException;
import com.liferay.portal.model.User;

import static com.dotcms.util.CollectionsUtils.map;

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
