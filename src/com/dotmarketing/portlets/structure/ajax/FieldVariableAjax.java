package com.dotmarketing.portlets.structure.ajax;

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
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class FieldVariableAjax {
	
	public List<Map<String, Object>> getFieldVariablesForField(String fieldId) throws Exception {
		
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		FieldAPI fieldAPI = APILocator.getFieldAPI();
		
		List<Map<String, Object>> resultList = new LinkedList<Map<String,Object>>();
		List<FieldVariable> fieldVars = fieldAPI.getFieldVariablesForField(fieldId, user, respectFrontendRoles);
		for(FieldVariable variable : fieldVars) {
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
	
	public String saveFieldVariable(String id, String fieldId, String name, String key, String value) throws DotRuntimeException, PortalException, 
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
			return LanguageUtil.get(user, "message.fieldvariables.key.required");
		}
	
		if (RegEX.contains(key, "[^A-Za-z0-9]")) {
			return LanguageUtil.get(user, "message.fieldvariables.exist.error.regex");
		}
	
		FieldAPI fieldAPI = APILocator.getFieldAPI();
	
		List<FieldVariable> variables = fieldAPI.getFieldVariablesForField(fieldId, user, false);
	
		FieldVariable fieldVariable = null;
		for (FieldVariable next : variables) {
			if (next.getKey().equals(key) && !next.getId().equals(id)) {
				return LanguageUtil.get(user, "message.fieldvariables.exist.error.key");
			}
			if(UtilMethods.isSet(id) && next.getId().equals(id)) {
				fieldVariable = next;
			}
		}
		
		if(fieldVariable == null) {
			fieldVariable = new FieldVariable();
		}
	
		fieldVariable.setId(id);
		fieldVariable.setFieldId(fieldId);
		fieldVariable.setName(name);
		fieldVariable.setKey(key);
		fieldVariable.setValue(value);
		fieldVariable.setLastModifierId(user.getUserId());
		fieldVariable.setLastModDate(new Date());
		try {
			fieldAPI.saveFieldVariable(fieldVariable, user, respectFrontendRoles);
		} catch (DotSecurityException e) {
			return LanguageUtil.get(user, "message.fieldvariables.permission.error.save");
		}
		
		return null;
	}
	
	public String deleteFieldVariable(String fieldVarId) throws DotDataException, DotSecurityException, DotRuntimeException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);

		FieldAPI fieldAPI = APILocator.getFieldAPI();
		FieldVariable fieldVar = fieldAPI.findFieldVariable(fieldVarId, user, respectFrontendRoles);
		fieldAPI.deleteFieldVariable(fieldVar, user, respectFrontendRoles);
		
		return LanguageUtil.get(user, "message.fieldvariables.deleted");
	}

}
