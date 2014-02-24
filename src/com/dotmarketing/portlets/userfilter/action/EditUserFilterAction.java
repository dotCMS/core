package com.dotmarketing.portlets.userfilter.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Properties;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.userfilter.factories.UserFilterFactory;
import com.dotmarketing.portlets.userfilter.model.UserFilter;
import com.dotmarketing.portlets.usermanager.action.ViewUserManagerListAction;
import com.dotmarketing.portlets.usermanager.struts.UserManagerListSearchForm;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionMessages;

public class EditUserFilterAction extends DotPortletAction {

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
			ActionResponse res) throws Exception {

		String cmd = req.getParameter(com.liferay.portal.util.Constants.CMD);
		UserManagerListSearchForm searchForm = (UserManagerListSearchForm) form;

		_retrieveProperties(req, res, config, form);
		req.setAttribute(WebKeys.MAILING_LIST_VIEW, new ArrayList());
		if (com.liferay.portal.util.Constants.VIEW.equals(cmd)) {
			// viewing user filter
			try {
				_getUserFilter(form, req, res);

				req.setAttribute(WebKeys.USER_FILTER_LIST_INODE, searchForm.getUserFilterListInode());
				req.setAttribute(WebKeys.USER_FILTER_LIST_TITLE, searchForm.getUserFilterTitle());
				req.setAttribute(WebKeys.USERMANAGERLISTFORM, form);
				searchForm = (UserManagerListSearchForm) form;
				ViewUserManagerListAction._doSearch(searchForm, req, res);
				
				String referrer = req.getParameter("referrer");
				if (UtilMethods.isSet(referrer)) {
					res.sendRedirect(SecurityUtils.stripReferer(referrer));
				} else {
					setForward(req, "portlet.ext.userfilter.edit_userfilter");
					return;
				}
			} catch (Exception e) {
				SessionMessages.add(req, "error", "message.userfilter.error.getting");
				_handleException(e, req);
			}
		}
		else if ("deleteUserFilter".equals(cmd)) {
			// deleting user filter
			try {

				_deleteUserFilter(form, req, res);

				String redirect = req.getParameter("redirect");
	        	redirect = URLDecoder.decode(redirect, "UTF-8");
	        	_sendToReferral(req, res, redirect);
	        	return;
			} catch (Exception e) {
				SessionMessages.add(req, "error", "message.userfilter.error.deleting_user_filter");
				_handleException(e, req);
			}
		}
		else if (com.liferay.portal.util.Constants.SAVE.equals(cmd)) {
			// Save / Update user filter
			try {
				_save(form, req, res);

				_getUserFilter(form, req, res);
				
				req.setAttribute(WebKeys.USER_FILTER_LIST_INODE, searchForm.getUserFilterListInode());
				req.setAttribute(WebKeys.USER_FILTER_LIST_TITLE, searchForm.getUserFilterTitle());
				req.setAttribute(WebKeys.USERMANAGERLISTFORM, form);
				searchForm = (UserManagerListSearchForm) form;
				ViewUserManagerListAction._doSearch(searchForm, req, res);

				String referrer = req.getParameter("referrer");
				if (UtilMethods.isSet(referrer)) {
					res.sendRedirect(SecurityUtils.stripReferer(referrer));
				} else {
					setForward(req, "portlet.ext.userfilter.edit_userfilter");
					return;
				}
			} catch (Exception e) {
				SessionMessages.add(req, "error", "message.userfilter.error.creating");
				_handleException(e, req);
			}
		}
		else if (com.liferay.portal.util.Constants.SEARCH.equals(cmd)) {
			// Searching
			try {
				ViewUserManagerListAction._doSearch(searchForm, req, res);
				req.setAttribute(WebKeys.USER_FILTER_LIST_INODE, searchForm.getUserFilterListInode());
				req.setAttribute(WebKeys.USER_FILTER_LIST_TITLE, searchForm.getUserFilterTitle());
				req.setAttribute(WebKeys.USERMANAGERLISTFORM, form);
				setForward(req, "portlet.ext.userfilter.edit_userfilter");
				return;
			} catch (Exception e) {
				SessionMessages.add(req, "error", "message.userfilter.error.deleting_user");
				_handleException(e, req);
			}
		}
		else if (com.liferay.portal.util.Constants.DELETE.equals(cmd)) {
			// Deleting user
			try {
				_delete(form, req, res);

				if (UtilMethods.isSet(req.getParameter("returnPath"))) {
					setForward(req, SecurityUtils.stripReferer(req.getParameter("returnPath")));
				}
				else {
					_getUserFilter(form, req, res);
					
					req.setAttribute(WebKeys.USER_FILTER_LIST_INODE, searchForm.getUserFilterListInode());
					req.setAttribute(WebKeys.USER_FILTER_LIST_TITLE, searchForm.getUserFilterTitle());
					req.setAttribute(WebKeys.USERMANAGERLISTFORM, form);
					searchForm = (UserManagerListSearchForm) form;
				}

				ViewUserManagerListAction._doSearch(searchForm, req, res);
				req.setAttribute(WebKeys.USERMANAGERLISTFORM, form);

				String referrer = req.getParameter("referrer");
				if (UtilMethods.isSet(referrer)) {
					res.sendRedirect(SecurityUtils.stripReferer(referrer));
				} else {
					setForward(req, "portlet.ext.userfilter.edit_userfilter");
					return;
				}
			} catch (Exception e) {
				SessionMessages.add(req, "error", "message.userfilter.error.deleting_user");
				_handleException(e, req);
			}
		}
		else if (com.liferay.portal.util.Constants.EDIT.equals(cmd)) {
			_getUserFilter(form, req, res);
			
			req.setAttribute(WebKeys.USER_FILTER_LIST_INODE, searchForm.getUserFilterListInode());
			req.setAttribute(WebKeys.USER_FILTER_LIST_TITLE, searchForm.getUserFilterTitle());
			searchForm = (UserManagerListSearchForm) form;
			ViewUserManagerListAction._doSearch(searchForm, req, res);
			req.setAttribute(WebKeys.USERMANAGERLISTFORM, form);

			setForward(req, "portlet.ext.userfilter.edit_userfilter");
			return;
			
		}
		req.setAttribute(WebKeys.USERMANAGERLISTFORM, form);
		setForward(req, "portlet.ext.userfilter.edit_userfilter");
    }

	//Adding / Saving User Filter
	private void _save(ActionForm form, ActionRequest req, ActionResponse res)
	throws Exception {
		User user = _getUser(req);
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		UserManagerListSearchForm mlForm = (UserManagerListSearchForm) form;
		UserFilter uf;
		if (InodeUtils.isSet(mlForm.getUserFilterListInode())) {
			uf = (UserFilter) InodeFactory.getInode(mlForm.getUserFilterListInode(), UserFilter.class);

			//removing permissions on the user filter\
			perAPI.removePermissions(uf);
		}
		else {
			uf = new UserFilter();
		}

		BeanUtils.copyProperties(uf, mlForm);
		uf.setCategories(mlForm.getCategoriesStr());
		HibernateUtil.startTransaction();
		HibernateUtil.saveOrUpdate(uf);
		HibernateUtil.commitTransaction();

		// read permission
		String[] readPermissions = req.getParameterValues("readRole");
		// write permission
		String[] writePermissions = req.getParameterValues("writeRole");
		//adding roles to user filter
		Permission permission = null;
		if (readPermissions != null) {
			for (int n = 0; n < readPermissions.length; n++) {
				permission = new Permission(uf.getInode(), readPermissions[n], PERMISSION_READ);
				perAPI.save(permission, uf, user, false);
			}
		}

		if (writePermissions != null) {
			for (int n = 0; n < writePermissions.length; n++) {
				permission = new Permission(uf.getInode(), writePermissions[n],	PERMISSION_WRITE);
				perAPI.save(permission, uf, user, false);
			}
		}
		mlForm.setUserFilterListInode(uf.getInode());
		SessionMessages.add(req, "message", "message.userfilter.save");
	}

	//Getting User Filter
	private void _getUserFilter(ActionForm form, ActionRequest req, ActionResponse res)
	throws Exception {
		String inode = req.getParameter("inode");
		UserManagerListSearchForm searchForm = (UserManagerListSearchForm) form;

		if (!InodeUtils.isSet(inode)) {
			inode = searchForm.getUserFilterListInode();
		}
		
		if (InodeUtils.isSet(inode)) {
			UserFilter uf = UserFilterFactory.getUserFilter(inode);
	
			searchForm.setUserFilterListInode(inode);
	
			BeanUtils.copyProperties(searchForm, uf);
			searchForm.setCategories(uf.getCategoriesArray());
		}
	}

	//Deleting User manager
	private void _delete(ActionForm form, ActionRequest req, ActionResponse res)
	throws Exception {

		String userId = req.getParameter("userID");
		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();

		User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
	
		//deletes user proxy
		InodeFactory.deleteInode(userProxy);
		//deletes liferay user
		APILocator.getUserAPI().delete(APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false),com.dotmarketing.business.APILocator.getUserAPI().getSystemUser(), false);

		SessionMessages.add(req, "message", "message.usermanager.deleted");

	}

	//Deleting User Filter
	private void _deleteUserFilter(ActionForm form, ActionRequest req, ActionResponse res)
	throws Exception {
		UserManagerListSearchForm mlForm = (UserManagerListSearchForm) form;
		UserFilter uf;
		if (InodeUtils.isSet(mlForm.getUserFilterListInode())) {
			uf = (UserFilter) InodeFactory.getInode(mlForm.getUserFilterListInode(), UserFilter.class);

			//removing permissions on the user filter
			PermissionAPI perAPI = APILocator.getPermissionAPI();
			perAPI.removePermissions(uf);
			
			InodeFactory.deleteInode(uf);
			HibernateUtil.flush();
			HibernateUtil.closeSession();
			SessionMessages.add(req, "message", "message.userfilter.success.delete");
		}
		else {
			SessionMessages.add(req, "error", "message.userfilter.error.delete");
		}

	}
	private void _retrieveProperties(ActionRequest req,	ActionResponse res, PortletConfig config, ActionForm form)
	throws Exception {

		Properties properties = new Properties();
	
		try {
			//Loading properties file
	
            String filePath = Thread.currentThread().getContextClassLoader().getResource("user_manager_config.properties").getPath();
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(filePath));
	
			if (is != null) {
				properties.load(is);
			}
			is.close();
		} catch (Exception e) {
			Logger.error(this, "Could not load this file = user_manager_config.properties", e);
		}
	
		PortletSession sess = req.getPortletSession();
		req.setAttribute(WebKeys.USERMANAGER_PROPERTIES, properties);
		sess.setAttribute(WebKeys.USERMANAGER_PROPERTIES, properties);
	}

}
