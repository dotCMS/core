package com.dotmarketing.portlets.userfilter.factories;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.userfilter.model.UserFilter;
import com.dotmarketing.portlets.usermanager.factories.UserManagerListBuilderFactory;
import com.dotmarketing.portlets.usermanager.struts.UserManagerListSearchForm;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class UserFilterFactory {

	public static java.util.List getAllUserFilter() {
		HibernateUtil dh = new HibernateUtil(UserFilter.class);
		List<UserFilter> list =null;
		try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.userfilter.model.UserFilter order by title");
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(UserFilterFactory.class, "getAllUserFilter failed:" + e, e);
		}
		return list;
	}
	
	public static java.util.List getAllUserFilterByUser(User user) {
		HibernateUtil dh = new HibernateUtil(UserFilter.class);
		List<UserFilter> list =null;
		try {
			dh.setSQLQuery("select user_filter.* from user_filter user_filter, inode inode where user_filter.inode=inode.inode and inode.owner=? order by title");
			dh.setParam(user.getUserId());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(UserFilterFactory.class, "getAllUserFilterByUser failed:" + e, e);
		}
		return list;
	}
	
	public static java.util.List getUserFilterByTitle(String title) {
		HibernateUtil dh = new HibernateUtil(UserFilter.class);
		List<UserFilter> list =null;
		try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.userfilter.model.UserFilter where lower(title) like ? order by title");
			dh.setParam("%" + title.toLowerCase() + "%");
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(UserFilterFactory.class, "getUserFilterByTitle failed:" + e, e);
		}
		return list;
	}
	
	public static java.util.List getUserFilterByTitleAndUser(String title, User user) {
		HibernateUtil dh = new HibernateUtil(UserFilter.class);
		List<UserFilter> list =null;
		try {
			dh.setSQLQuery("select user_filter.* from user_filter user_filter, inode inode where lower(user_filter.title) like ? and user_filter.inode=inode.inode and inode.owner=? order by title");
			dh.setParam("%" + title.toLowerCase() + "%");
			dh.setParam(user.getUserId());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(UserFilterFactory.class, "getUserFilterByTitleAndUser failed:" + e, e);
		}
		return list;
	}
	
	public static UserFilter getUserFilter(String inode) {
		return (UserFilter) InodeFactory.getInode(inode, UserFilter.class);
	}

	public static List<UserProxy> getUserProxiesFromFilter(UserFilter uf) throws Exception {
		UserManagerListSearchForm userForm = new UserManagerListSearchForm();
		BeanUtils.copyProperties(userForm, uf);
		List<UserProxy> userProxies = new ArrayList<UserProxy>();
		List allUsers = UserManagerListBuilderFactory.doSearch(userForm);
		Iterator it = allUsers.iterator();
		for (int i = 0; it.hasNext(); i++) {
			User user = null;
			String userId = (String) ((Map)it.next()).get("userid");
			user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
			userProxies.add(userProxy);
		}
		return userProxies;
	}
}
