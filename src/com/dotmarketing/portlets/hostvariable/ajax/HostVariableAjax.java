package com.dotmarketing.portlets.hostvariable.ajax;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class HostVariableAjax {

	protected HostAPI hostAPI = APILocator.getHostAPI();

	public String saveHostVariable(String id, String hostId, String name, String key, String value) throws DotRuntimeException, PortalException, 
		SystemException, DotDataException, DotSecurityException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		
		key = key.trim();
		value = value.trim();
		name = name.trim();
		name = UtilMethods.escapeDoubleQuotes(name);
		value = UtilMethods.escapeDoubleQuotes(value);

		if (key.equals("")) {
			return LanguageUtil.get(user, "message.hostvariables.key.required");
		}

		if (RegEX.contains(key, "[^A-Za-z0-9]")) {
			return LanguageUtil.get(user, "message.hostvariables.exist.error.regex");
		}

		HostVariableAPI hostVariableAPI = APILocator.getHostVariableAPI();

		List<HostVariable> variables = hostVariableAPI.getVariablesForHost(hostId, user, false);

		HostVariable hostVariable = null;
		for (HostVariable next : variables) {
			if (next.getKey().equals(key) && !next.getId().equals(id)) {
				return LanguageUtil.get(user, "message.hostvariables.exist.error.key");
			}
			if(UtilMethods.isSet(id) && next.getId().equals(id)) {
				hostVariable = next;
			}
		}
		
		if(hostVariable == null) {
			hostVariable = new HostVariable();
		}

		hostVariable.setId(id);
		hostVariable.setHostId(hostId);
		hostVariable.setName(name);
		hostVariable.setKey(key);
		hostVariable.setValue(value);
		hostVariable.setLastModifierId(user.getUserId());
		hostVariable.setLastModDate(new Date());
		try {
			hostVariableAPI.save(hostVariable, user, respectFrontendRoles);
		} catch (DotSecurityException e) {
			return LanguageUtil.get(user, "message.hostvariables.permission.error.save");
		}
		
		return null;

	}

	public void deleteHostVariable(String hvarId) throws DotDataException, DotSecurityException, DotRuntimeException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);

		HostVariableAPI hostVariableAPI = APILocator.getHostVariableAPI();
		HostVariable hvar = hostVariableAPI.find(hvarId, user, respectFrontendRoles);
		hostVariableAPI.delete(hvar, user, respectFrontendRoles);

	}

	public List<Map<String, Object>> getHostVariables(String hostId) throws Exception {
		
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		HostVariableAPI hostVariableAPI = APILocator.getHostVariableAPI();
		
		List<Map<String, Object>> resultList = new LinkedList<Map<String,Object>>();
		List<HostVariable> hvars = hostVariableAPI.getVariablesForHost(hostId, user, respectFrontendRoles);
		for(HostVariable variable : hvars) {
			Map<String, Object> variableMap = variable.getMap();
			User variableLastModifier = userWebAPI.loadUserById(variable.getLastModifierId(), userWebAPI.getSystemUser(), false);
			String lastModifierFullName = "Unknown";
			if(variableLastModifier != null)
				lastModifierFullName = variableLastModifier.getFullName();
			variableMap.put("lastModifierFullName", lastModifierFullName);
			resultList.add(variableMap);
		}
		
		return resultList;

	}
}
