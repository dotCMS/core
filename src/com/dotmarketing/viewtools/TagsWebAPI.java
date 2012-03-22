package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.tag.factories.TagFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class TagsWebAPI implements ViewTool {
	private HttpServletRequest request;
	Context ctx;

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
	}
	
	public List getTagsByUser(User user) {
		List tagsUser = (List) request.getSession().getAttribute(WebKeys.LOGGED_IN_USER_TAGS);
		if (!UtilMethods.isSet(tagsUser) || tagsUser.size() == 0) {
			UserProxy up;
			try {
				up = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user.getUserId(),APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				return new ArrayList();
			}	
			tagsUser = TagFactory.getTagInodeByInode(String.valueOf(up.getInode()));
			request.getSession().setAttribute(WebKeys.LOGGED_IN_USER_TAGS, tagsUser);
		}
		return tagsUser;
	}
	
	public List getTagsByNonLoggedUser() {
		HttpSession session = request.getSession();
		return (List) session.getAttribute(com.dotmarketing.util.WebKeys.NON_LOGGED_IN_USER_TAGS);
	}
}