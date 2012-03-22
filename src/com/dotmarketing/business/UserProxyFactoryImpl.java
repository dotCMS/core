package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class UserProxyFactoryImpl extends UserProxyFactory {

	private UserProxyCache upc = CacheLocator.getUserProxyCache();
	
	@Override
	public UserProxy getUserProxy(String userId) throws DotRuntimeException, DotHibernateException{
        UserProxy up = null;
        up = upc.getUserProxyFromUserId(userId);
        if(!UtilMethods.isSet(up)){
			try {
				HibernateUtil dh = new HibernateUtil(UserProxy.class);
				dh.setQuery("from user_proxy in class com.dotmarketing.beans.UserProxy where user_id = ?");
				dh.setParam(userId);
	
				up = (UserProxy) dh.load();
				upc.addToUserProxyCache(up);
			} catch (Exception e) {
				throw new DotRuntimeException(e.getMessage(),e);
			}
			
	        if (!InodeUtils.isSet(up.getInode())) {
	    		// if we don't have a user proxy, create one 
	    		up.setUserId(userId);
	    		HibernateUtil.saveOrUpdate(up);
	    		upc.addToUserProxyCache(up);
		    }
        }
		return up;
	}

	@Override
	public UserProxy getUserProxyByLongLiveCookie(String dotCMSID) throws DotRuntimeException{
		UserProxy up = null;
		up = upc.getUserProxyFromLongCookie(dotCMSID);
        if(!UtilMethods.isSet(up)){
			try {
				HibernateUtil dh = new HibernateUtil(UserProxy.class);
				dh.setQuery("from user_proxy in class com.dotmarketing.beans.UserProxy where lower(long_lived_cookie) = lower(?)");
				dh.setParam(dotCMSID);
	
				up = (UserProxy) dh.load();
				upc.addToUserProxyCache(up);
			} catch (Exception e) {
				Logger.warn(UserProxyAPIImpl.class, "getUserProxyByLongLiveCookie failed:" + e, e);
				throw new DotRuntimeException(e.getMessage(),e);
			}
        }
		return up;
	}

	@Override
	public void saveUserProxy(UserProxy userProxy) throws DotRuntimeException, DotDataException{
		HibernateUtil hu = new HibernateUtil();
		UserProxy up = (UserProxy)hu.load(UserProxy.class, userProxy.getInode());
		try {
			BeanUtils.copyProperties(up, userProxy);
			up.setNoclicktracking(userProxy.isNoclicktracking());
		} catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
		HibernateUtil.saveOrUpdate(up);
		upc.remove(up);
	}

@Override
	public List<String> findUsersTitle() throws DotDataException {
		List<String> result = new ArrayList<String>();
		
		DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL("select distinct user_proxy.title as title from user_proxy");
		ArrayList<HashMap<String, String>> results = dotConnect.getResults();
		if ((results != null) && (0 < results.size())) {
			for (HashMap<String, String> title : results)
				if (UtilMethods.isSet(title.get("title")))
					result.add(title.get("title"));
		}
		return result;
	}

	@Override
	public HashMap<String, Object> searchUsersAndUsersProxy(String firstName, String lastName, String title, boolean showUserGroups, List<Role> roles, boolean showUserRoles, String orderBy, int page, int pageSize) throws DotDataException{
		HashMap<String, Object> result = new HashMap<String, Object>();
		int bottom = ((page - 1) * pageSize);
		int top = (page * pageSize);
		
		StringBuilder sqlCount = new StringBuilder(1024);
		sqlCount.append("select count(user_.userid) as total");
		
		StringBuilder sqlFields = new StringBuilder(1024);
		sqlFields.append("select user_.userid as userid");
		
		StringBuilder sqlFrom = new StringBuilder(1024);
		sqlFrom.append(" from user_");
		
		StringBuilder condition = new StringBuilder(1024);
		if (UtilMethods.isSet(firstName)) {
			condition.append(" where lower(user_.firstName) like ?");
		}
		
		if (UtilMethods.isSet(lastName)) {
			if (0 < condition.length())
				condition.append(" and lower(user_.lastName) like ?");
			else
				condition.append(" where lower(user_.lastName) like ?");
		}
		
		if ((UtilMethods.isSet(orderBy) && orderBy.trim().toLowerCase().startsWith("cms_role.")) ||
    		((roles != null) && (0 < roles.size())) ||
    		showUserRoles) {
			//sqlCount.append(", users_roles, role_");
			sqlFields.append(", cms_role.name as role_name");
        	//sqlFrom.append(", users_roles, role_");
			sqlFrom.append(", users_cms_roles,cms_role ");
       	}
		if ((roles != null) && (0 < roles.size())) {
			if (0 < condition.length())
       			condition.append(" and user_.userId=users_cms_roles.user_id and users_cms_roles.role_id=cms_role.id");
       		else
       			condition.append(" where user_.userId=users_cms_roles.user_id and users_cms_roles.role_id=cms_role.id");
			
			condition.append(" and cms_role.id in ('" + roles.get(0).getId() + "'");
			
			for (int i = 1; i < roles.size(); ++i) {
				condition.append(", '" + roles.get(i).getId() + "'");
			}
			
			condition.append(")");
		}
		
		if ((UtilMethods.isSet(orderBy) && (orderBy.trim().toLowerCase().startsWith("user_proxy.") || orderBy.trim().toLowerCase().startsWith("title "))) ||
    			UtilMethods.isSet(title)) {
    			//sqlCount.append(", user_proxy");
    			sqlFrom.append(", user_proxy");
    			if (0 < condition.length())
    				condition.append(" and user_.userId=user_proxy.user_id");
    			else
    				condition.append(" where user_.userId=user_proxy.user_id");
    		}
    		if (UtilMethods.isSet(title)) {
    			condition.append(" and lower(user_proxy.title) like ?");
    	}
		
		DotConnect dotConnect = new DotConnect();
		
		dotConnect.setSQL(sqlCount.append(sqlFrom).append(condition).toString());
		if (UtilMethods.isSet(firstName))
			dotConnect.addParam("%" + firstName.toLowerCase() + "%");
		
		if (UtilMethods.isSet(lastName))
			dotConnect.addParam("%" + lastName.toLowerCase() + "%");
		
		if (UtilMethods.isSet(title))
			dotConnect.addParam("%" + title.toLowerCase() + "%");
		
		ArrayList<HashMap<String, String>> results = dotConnect.getResults();
		
		if (0 < results.size()) {
			result.put("total", Long.parseLong(results.get(0).get("total")));
		}
		
		if (UtilMethods.isSet(orderBy)) {
			condition.append(" order by " + orderBy);
		}
		
		dotConnect.setSQL(sqlFields.append(sqlFrom).append(condition).toString());
		
		if (UtilMethods.isSet(firstName))
			dotConnect.addParam("%" + firstName.toLowerCase() + "%");
		
		if (UtilMethods.isSet(lastName))
			dotConnect.addParam("%" + lastName.toLowerCase() + "%");
		
		if (UtilMethods.isSet(title))
			dotConnect.addParam("%" + title.toLowerCase() + "%");
		
		dotConnect.setMaxRows(top);
		results = dotConnect.getResults();
		
		User user;
		ArrayList<User> users = new ArrayList<User>();
		ArrayList<UserProxy> usersProxy = new ArrayList<UserProxy>();
		ArrayList<String> groupNames = new ArrayList<String>();
		ArrayList<String> roleNames = new ArrayList<String>();
		String value;
		for (int i = 0; i < results.size(); i++) {
			if (bottom <= i) {
				if (i < top) {
					value = results.get(i).get("userid");
					try {
						user = APILocator.getUserAPI().loadUserById(value,APILocator.getUserAPI().getSystemUser(),true);
					} catch (Exception e) {
						Logger.error(this, e.getMessage(),e);
						throw new DotDataException(e.getMessage(), e);
					}
					users.add(user);
					usersProxy.add(getUserProxy(user.getUserId()));
					
					value = results.get(i).get("group_name");
					if (UtilMethods.isSet(value))
						groupNames.add(value);
					else
						groupNames.add("");
					
					value = results.get(i).get("role_name");
					if (UtilMethods.isSet(value))
						roleNames.add(value);
					else
						roleNames.add("");
				} else {
					break;
				}
			}    			
		}
		
		HibernateUtil.closeSession();
		result.put("users", users);
		result.put("usersProxy", usersProxy);
		result.put("groupNames", groupNames);
		result.put("roleNames", roleNames);
		return result;
	}
}
