package com.dotcms.rest;

import java.util.HashMap;
import java.util.Map;

import com.liferay.portal.model.User;

public class InitDataObject {

	private Map<String, String> paramsMap;
	private User user;

	public InitDataObject() {
		paramsMap = new HashMap<String, String>();
	}

	public Map<String, String> getParamsMap() {
		return paramsMap;
	}
	public void setParamsMap(Map<String, String> paramsMap) {
		this.paramsMap = paramsMap;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

}
