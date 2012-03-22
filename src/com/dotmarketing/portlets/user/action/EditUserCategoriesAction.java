package com.dotmarketing.portlets.user.action;

import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.user.struts.UserCategoriesForm;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionMessages;

public class EditUserCategoriesAction extends DotPortletAction
{
	
	private CategoryAPI catAPI;
	
	public EditUserCategoriesAction() {
		catAPI = APILocator.getCategoryAPI();
	}
	
	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
	throws Exception {

		UserCategoriesForm userCategoriesForm = (UserCategoriesForm) form;
		User user = _getUser(req);
		String referer = req.getParameter("referer");
		
		String userProxyInode = userCategoriesForm.getUserProxy();
		boolean isNonclicktracking = userCategoriesForm.isNonclicktracking();
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userProxyInode,APILocator.getUserAPI().getSystemUser(), false);
		String[] categories = userCategoriesForm.getCategories();
		
		if(categories != null && InodeUtils.isSet(userProxy.getInode()))
		{
			try
			{
				HibernateUtil.startTransaction();
				userProxy.setNoclicktracking(isNonclicktracking);
				List<Category> myUserCategories = catAPI.getChildren(userProxy, user, false);
				for (Object o : myUserCategories) {
					if(o instanceof Category && catAPI.canUseCategory((Category)o, user, false)){
						catAPI.removeChild(userProxy, (Category)o, user, false);
					}
				}
				for(int i = 0;i < categories.length;i++)
				{
					Category category = catAPI.find(categories[i], user, false);
					if(InodeUtils.isSet(category.getInode()))
					{
						catAPI.addChild(userProxy, category, user, false);
					}					
				}
				HibernateUtil.commitTransaction();
			}
			catch(Exception ex)
			{
				Logger.warn(this,ex.toString());
				HibernateUtil.rollbackTransaction();
			}
			
		} else {
			
			userProxy.setNoclicktracking(isNonclicktracking);
			com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);
		}
		referer += "&layer=other";
		_sendToReferral(req,res,referer);
		SessionMessages.add(req,"categories");
	}
}
