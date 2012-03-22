package com.dotmarketing.viewtools;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class LangBackendWebAPI implements ViewTool {

	private HttpServletRequest request;
	Context ctx;

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
	}
	
	public String get(String key) throws PortalException, SystemException {
		User user1=com.liferay.portal.util.PortalUtil.getUser(this.request);
		return LanguageUtil.get(user1, key);
		
	}

}
