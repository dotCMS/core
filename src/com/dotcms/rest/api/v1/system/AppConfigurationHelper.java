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
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebContext;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

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



	private final LoginAsAPI loginAsAPI;
	private final LoginService loginService;
	private final MenuResource menuResource;
	private final ConfigurationResource configurationResource;

	private static class SingletonHolder {
		private static final AppConfigurationHelper INSTANCE = new AppConfigurationHelper();
	}

	public static AppConfigurationHelper getInstance() {
		return AppConfigurationHelper.SingletonHolder.INSTANCE;
	}

	public AppConfigurationHelper() {
		this( APILocator.getLoginAsAPI(), LoginServiceFactory.getInstance().getLoginService(),
				new MenuResource(), new ConfigurationResource());
	}

	@VisibleForTesting
	public AppConfigurationHelper(LoginAsAPI loginAsAPI, LoginService loginService, MenuResource menuResource,
								  ConfigurationResource configurationResource) {
		this.loginAsAPI = loginAsAPI;
		this.loginService = loginService;
		this.menuResource = menuResource;
		this.configurationResource = configurationResource;
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

		Object entity = null;
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
		final Response configurationResponse = configurationResource.list(request);
		final Object entity = ResponseEntityView.class.cast(configurationResponse.getEntity()).getEntity();
		return entity;
	}

	/**
	 * Return a map with the Principal and LoginAs user, the map content the follows keys:
	 *
	 * <ul>
	 *     <li>{@link AppContextInitResource#USER} for the principal user</li>
	 *     <li>{@link AppContextInitResource#LOGIN_AS_USER} for the login as user</li>
	 * </ul>
	 *
	 * @param request
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
     */
	public Map<String, Map> getUsers(final HttpServletRequest request) throws DotDataException, DotSecurityException,
			IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		User principalUser = loginAsAPI.getPrincipalUser( WebContext.getInstance( request ));
		User loginAsUser = null;

		if (principalUser == null){
			principalUser = this.loginService.getLogInUser( request );
		}else{
			loginAsUser = this.loginService.getLogInUser( request );
		}

		return map(AppContextInitResource.USER, principalUser.toMap(), AppContextInitResource.LOGIN_AS_USER,
				loginAsUser != null ? loginAsUser.toMap() : null);
	}
}
