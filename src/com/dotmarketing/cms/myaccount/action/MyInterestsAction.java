package com.dotmarketing.cms.myaccount.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.myaccount.struts.MyAccountForm;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class MyInterestsAction extends DispatchAction {
	
	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	
	public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public ActionForward unspecified(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login?referrer=/dotCMS/editInterests");
		}

		// HttpSession session = request.getSession();
		MyAccountForm form = (MyAccountForm) lf;

		//Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		loadUserInfoInRequest(form, userId, request);

		return mapping.findForward("myInterestsPage");
	}

	public ActionForward saveUserInfo(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
	throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			 return new ActionForward("/dotCMS/login");
		}
		
		//Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

		//Delete the old categories
		InodeFactory.deleteChildrenOfClass(userProxy,Category.class);

		//Save the new categories
		MyAccountForm form = (MyAccountForm) lf;
		String[] categories = form.getCategory();
		if (UtilMethods.isSet(categories)) {
			for(int i = 0;i < categories.length;i++) {
				Category category = categoryAPI.find(categories[i], user, true);
				if(InodeUtils.isSet(category.getInode())) {
					categoryAPI.addChild(userProxy, category, user, true);
				}
			}
		}
		
		HibernateUtil.flush();
		
		loadUserInfoInRequest(form, userId, request);

        request.getSession().removeAttribute(WebKeys.LOGGED_IN_USER_CATS);

		ActionMessages msg = new ActionMessages();
        msg.add(Globals.MESSAGE_KEY, new ActionMessage("message.interests.saved"));
        request.setAttribute(Globals.MESSAGE_KEY, msg);

        return mapping.findForward("myInterestsPage");
	}

	
	private void loadUserInfoInRequest(MyAccountForm form, String userId,
			HttpServletRequest request) throws Exception {

		
		User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

		// Retriving info from db
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
		
		if (!InodeUtils.isSet(userProxy.getInode())) {
			userProxy.setUserId(user.getUserId());
			HibernateUtil.saveOrUpdate(userProxy);
		}

		// Copy the attributes
		BeanUtils.copyProperties(form, user);
		BeanUtils.copyProperties(form, userProxy);
		

		// Extra user info
		form.setUserProxyInode(userProxy.getInode());

	}
}
