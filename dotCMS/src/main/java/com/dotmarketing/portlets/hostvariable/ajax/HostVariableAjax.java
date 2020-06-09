package com.dotmarketing.portlets.hostvariable.ajax;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class allows the interaction with Site Variables in dotCMS. The methods exposed here are referenced via DWR by
 * the UI layer to allow users to perform CRUD operations on them.
 *
 * @author root
 * @since Mar 22, 2012
 */
public class HostVariableAjax {

	protected final UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
	protected final HostVariableAPI hostVariableAPI = APILocator.getHostVariableAPI();

	/**
	 * Saves a Site Variable in the specified Site.
	 *
	 * @param id     The ID of the Site Variable.
	 * @param siteId The ID of the Site that the variable will be assigned to.
	 * @param name   The human-readable name of the Site Variable.
	 * @param key    The unique key assigned to the Site Variable.
	 * @param value  The value of the Site Variable.
	 *
	 * @return If the Site Variable was saved correctly, returns {@code null}. Otherwise, an error message is returned.
	 *
	 * @throws DotRuntimeException  An error occurred when executing this request.
	 * @throws PortalException      An error occurred when executing this request.
	 * @throws SystemException      An error occurred when executing this request.
	 * @throws DotDataException     The logged-in user does not have the required permissions to perform this action.
	 * @throws DotSecurityException The logged-in user does not have the required permissions to perform this action.
	 */
	public String saveHostVariable(final String id, final String siteId, String name, String key, String value) throws DotRuntimeException, PortalException,
			SystemException, DotDataException, DotSecurityException {
		final User loggedInUser = this.getLoggedInUser();
		final boolean respectFrontendRoles = this.respectFrontendRoles();

		key = key.trim();
		value = value.trim();
		name = name.trim();
		name = UtilMethods.escapeDoubleQuotes(name);
		value = UtilMethods.escapeDoubleQuotes(value);
		if (!UtilMethods.isSet(key)) {
			return LanguageUtil.get(loggedInUser, "message.hostvariables.key.required");
		}
		if (RegEX.contains(key, "[^A-Za-z0-9]")) {
			return LanguageUtil.get(loggedInUser, "message.hostvariables.exist.error.regex");
		}

		final List<HostVariable> variables = this.hostVariableAPI.getVariablesForHost(siteId, loggedInUser, false);
		HostVariable siteVariable = null;
		for (final HostVariable next : variables) {
			if (next.getKey().equals(key) && !next.getId().equals(id)) {
				return LanguageUtil.get(loggedInUser, "message.hostvariables.exist.error.key");
			}
			if(UtilMethods.isSet(id) && next.getId().equals(id)) {
				siteVariable = next;
			}
		}
		if (null == siteVariable) {
			siteVariable = new HostVariable();
		}
		siteVariable.setId(id);
		siteVariable.setHostId(siteId);
		siteVariable.setName(name);
		siteVariable.setKey(key);
		siteVariable.setValue(value);
		siteVariable.setLastModifierId(loggedInUser.getUserId());
		siteVariable.setLastModDate(new Date());
		String responseMsg = null;
		try {
			this.hostVariableAPI.save(siteVariable, loggedInUser, respectFrontendRoles);
		} catch (final DotSecurityException e) {
			responseMsg = LanguageUtil.get(loggedInUser, "message.hostvariables.permission.error.save");
			Logger.error(this, String.format("An error occurred when User ID '%s' attempted to save Site Variable " +
					"'%s': %s", loggedInUser.getUserId(), name, responseMsg), e);
		}

		return responseMsg;
	}

	/**
	 * Deletes the specified Site Variable.
	 *
	 * @param siteVariableId The ID of the Site Variable that will be deleted.
	 *
	 * @throws DotDataException     The logged-in user does not have the required permissions to perform this action.
	 * @throws DotSecurityException The logged-in user does not have the required permissions to perform this action.
	 * @throws DotRuntimeException  An error occurred when executing this request.
	 * @throws PortalException      An error occurred when executing this request.
	 * @throws SystemException      An error occurred when executing this request.
	 */
	public void deleteHostVariable(final String siteVariableId) throws DotDataException, DotSecurityException, DotRuntimeException, PortalException, SystemException {
		final User loggedInUser = this.getLoggedInUser();
		final boolean respectFrontendRoles = this.respectFrontendRoles();
		final HostVariable siteVariable = this.hostVariableAPI.find(siteVariableId, loggedInUser, respectFrontendRoles);
		this.hostVariableAPI.delete(siteVariable, loggedInUser, respectFrontendRoles);
	}

	/**
	 * Returns the complete list of Site Variables associated to the specified Site ID.
	 *
	 * @param siteId The ID of the Site whose variables will be retrieved.
	 *
	 * @return The list of Site Variables, grouped by the name of the User who last modified each of them.
	 *
	 * @throws Exception An error occurred when executing this request.
	 */
	public List<Map<String, Object>> getHostVariables(final String siteId) throws Exception {
		final User loggedInUser = this.getLoggedInUser();
		final boolean respectFrontendRoles = this.respectFrontendRoles();
		final List<Map<String, Object>> resultList = new LinkedList<>();
		final List<HostVariable> siteVariableList = this.hostVariableAPI.getVariablesForHost(siteId, loggedInUser, respectFrontendRoles);
		for (final HostVariable variable : siteVariableList) {
			String lastModifierFullName = "Unknown";
			try {
				final User variableLastModifier = this.userWebAPI.loadUserById(variable.getLastModifierId(), this.userWebAPI.getSystemUser(), false);
				if (null != variableLastModifier) {
					lastModifierFullName = variableLastModifier.getFullName();
				}
			} catch (final NoSuchUserException e) {
				// The modifier user does not exist anymore. So just default its name to "Unknown"
			}
			final Map<String, Object> variableMap = variable.getMap();
			variableMap.put("lastModifierFullName", lastModifierFullName);
			resultList.add(variableMap);
		}
		return resultList;
	}

	/**
	 * Returns the User that is currently logged into the system.
	 *
	 * @return The logged in {@link User} issuing the request.
	 *
	 * @throws SystemException An error occurred when executing this request.
	 * @throws PortalException An error occurred when executing this request.
	 */
	protected User getLoggedInUser() throws SystemException, PortalException {
		final HttpServletRequest req = getHttpRequest();
		return this.userWebAPI.getLoggedInUser(req);
	}

	/**
	 * Determines whether the request must respect front-end roles or not. That is, if the User requesting the data is
	 * logged into the front-end or the back-end of the system.
	 *
	 * @return If the {@link User} is logged into the front-end, returns {@code true}. Otherwise, returns {@code false}.
	 *
	 * @throws SystemException An error occurred when executing this request.
	 * @throws PortalException An error occurred when executing this request.
	 */
	protected boolean respectFrontendRoles() throws SystemException, PortalException {
		final HttpServletRequest req = getHttpRequest();
		return this.userWebAPI.isLoggedToFrontend(req);
	}

	/**
	 * Returns the current instance of the HTTP Servlet Request.
	 *
	 * @return The {@link HttpServletRequest} object.
	 */
	protected HttpServletRequest getHttpRequest() {
		final WebContext ctx = WebContextFactory.get();
		return ctx.getHttpServletRequest();
	}

}
