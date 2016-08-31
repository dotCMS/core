package com.dotcms.rest.api.v1.system;

import java.io.Serializable;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.menu.MenuResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

/**
 * This utility class assists the {@link AppConfigurationResource} in merging
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

	public static final AppConfigurationHelper INSTANCE = new AppConfigurationHelper();
	private UserAPI userAPI;

	private AppConfigurationHelper() {
		userAPI = APILocator.getUserAPI();
	}

	@VisibleForTesting
	private AppConfigurationHelper(UserAPI userAPI) {
		this.userAPI = userAPI;
	}

	/**
	 * Returns the list of menu items and sub-items for the main navigation bar
	 * displayed when users log into the dotCMS back-end.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The navigation menu items.
	 */
	public Object getMenuData(final HttpServletRequest request) {
		final MenuResource menuResource = new MenuResource();
		Object entity = new Object();
		try {
			final Response menuResponse = menuResource.getMenus(MenuResource.App.CORE_WEB.name(), request);
			entity = ResponseEntityView.class.cast(menuResponse.getEntity()).getEntity();
		} catch (Exception e) {
			// If not authenticated, return an empty response
			entity = new ArrayList<Object>();
		}
		return entity;
	}

	/**
	 * Returns a list of dotCMS configuration properties that are needed by the
	 * Angular UI.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The list of required system properties.
	 */
	public Object getConfigurationData(final HttpServletRequest request) {
		final ConfigurationResource configurationResource = new ConfigurationResource();
		final Response configurationResponse = configurationResource.list(request);
		final Object entity = ResponseEntityView.class.cast(configurationResponse.getEntity()).getEntity();
		return entity;
	}

	/**
	 * Return the LoginAs user
	 *
	 * @param request
	 * @return if a user is LoginAs then return it, in otherwise return null
	 * @throws DotSecurityException if one is thrown when the user is search
	 * @throws DotDataException if one is thrown when the user is search
     */
	public User getLoginAsUser(HttpServletRequest request) throws DotSecurityException, DotDataException {
		String principalUserId = (String) request.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID);
		User loginAsUser = null;

		if (principalUserId != null){
			loginAsUser = userAPI.loadUserById(principalUserId);
		}

		return loginAsUser;
	}
}
