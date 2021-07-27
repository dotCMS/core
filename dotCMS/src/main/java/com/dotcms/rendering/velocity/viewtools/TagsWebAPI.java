package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TagUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

public class TagsWebAPI implements ViewTool {
	private HttpServletRequest request;
	Context ctx;

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
	}
	
	public List getTagsByUser(User user) throws DotDataException {
		List tagsUser = (List) request.getSession().getAttribute(WebKeys.LOGGED_IN_USER_TAGS);
		if (!UtilMethods.isSet(tagsUser) || tagsUser.size() == 0) {

			tagsUser = APILocator.getTagAPI().getTagInodesByInode(String.valueOf(user.getUserId()));
			request.getSession().setAttribute(WebKeys.LOGGED_IN_USER_TAGS, tagsUser);
		}
		return tagsUser;
	}
	
	public List getTagsByNonLoggedUser() {
		HttpSession session = request.getSession();
		return (List) session.getAttribute(com.dotmarketing.util.WebKeys.NON_LOGGED_IN_USER_TAGS);
	}

	/**
	 * Method that accrues a given String of tag names with a CSV format to the current {@link Visitor}
	 *
	 * @param tags String of tag names with a CSV format to accrue
	 */
	public void accrueTags(String tags) {
		//Accrue the given tags
		TagUtil.accrueTags(request, tags);
	}

}