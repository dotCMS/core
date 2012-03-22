package com.dotmarketing.portlets.mailinglists.ajax;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.DotReindexStateException;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

public class MailingListAjax {

	private PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	private User getUser() throws PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = PortalUtil.getUser(request);
		boolean respectFrontendRoles = false;
		if(user == null) {
			//Assuming is a front-end access
			respectFrontendRoles = true;
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
		}

		return user;

	}

	/**
	 * @param permissionAPI the permissionAPI to set
	 */
	public void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	public Map<String, Object> getSusbscribers (String mailingListInode, String userId, Map<String, String> params) throws PortalException, SystemException, DotDataException {

		User currentUser = getUser();

		Map<String, Object> results = new HashMap<String, Object> ();

		int start = 0;
		if(params.containsKey("start"))
			start = Integer.parseInt((String)params.get("start"));

		int limit = -1;
		if(params.containsKey("limit"))
			limit = Integer.parseInt((String)params.get("limit"));

		String orderBy = "";
		if(params.containsKey("order"))
			orderBy = (String)params.get("order");

		List<UserProxy> userProxies;
		try {
			userProxies = MailingListFactory.getMailingListSubscribers(mailingListInode, userId, start, limit, orderBy);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
			return results;
		}
		int count;
		try {
			count = MailingListFactory.getMailingListSubscribersCount(mailingListInode, userId);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return results;
		}

		List<Map<String, Object>> users = new ArrayList<Map<String,Object>> ();

		for (UserProxy up : userProxies) {
			Map<String, Object> map = up.getMap();
			map.put("hasPermissionToWrite", permissionAPI.doesUserHavePermission(up, PERMISSION_WRITE, currentUser));
			users.add(map);
		}

		results.put("total", count);
		results.put("data", users);

		return results;
	}

	public Map<String, Object> getUnsusbscribers (String mailingListInode, String userId, Map<String, String> params) throws DotDataException {
		Map<String, Object> results = new HashMap<String, Object> ();
		User currentUser;
		try {
			currentUser = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (NoSuchUserException e) {
			Logger.warn(this, "User doesn't Exist");
			return results;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return results;
		} 

		int start = 0;
		if(params.containsKey("start"))
			start = Integer.parseInt((String)params.get("start"));

		int limit = -1;
		if(params.containsKey("limit"))
			limit = Integer.parseInt((String)params.get("limit"));

		String orderBy = "";
		if(params.containsKey("order"))
			orderBy = (String)params.get("order");

		List<UserProxy> userProxies;
		try {
			userProxies = MailingListFactory.getMailingListUnsubscribers(mailingListInode, userId, start, limit, orderBy);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return results;
		} 
		int count;
		try {
			count = MailingListFactory.getMailingListUnsubscribersCount(mailingListInode, userId);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return results;
		}

		List<Map<String, Object>> users = new ArrayList<Map<String,Object>> ();

		for (UserProxy up : userProxies) {
			Map<String, Object> map = up.getMap();
			map.put("hasPermissionToWrite", permissionAPI.doesUserHavePermission(up, PERMISSION_WRITE, currentUser));
			users.add(map);
		}

		results.put("total", count);
		results.put("data", users);

		return results;
	}

	public Map<String, Object> getBounces (String mailingListInode, String userId, Map<String, String> params) throws DotDataException {
		Map<String, Object> results = new HashMap<String, Object> ();
		User currentUser;
		try {
			currentUser = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (NoSuchUserException e) {
			Logger.warn(this, "User doesn't Exist");
			return results;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return results;
		}

		int start = 0;
		if(params.containsKey("start"))
			start = Integer.parseInt((String)params.get("start"));

		int limit = -1;
		if(params.containsKey("limit"))
			limit = Integer.parseInt((String)params.get("limit"));

		String orderBy = "";
		if(params.containsKey("order"))
			orderBy = (String)params.get("order");

		List<UserProxy> userProxies;
		try {
			userProxies = MailingListFactory.getMailingListBounces(mailingListInode, userId, start, limit, orderBy);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return results;
		}
		int count;
		try {
			count = MailingListFactory.getMailingListBouncesCount(mailingListInode, userId);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return results;
		}

		List<Map<String, Object>> users = new ArrayList<Map<String,Object>> ();

		for (UserProxy up : userProxies) {
			Map<String, Object> map = up.getMap();
			map.put("hasPermissionToWrite", permissionAPI.doesUserHavePermission(up, PERMISSION_WRITE, currentUser));
			users.add(map);
		}

		results.put("total", count);
		results.put("data", users);

		return results;
	}

	public void deleteSubscribers (String mailingListInode, String[] inodes) {
		for (String inode : inodes) {
			UserProxy currentUser;
			try {
				currentUser = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(inode,APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
			MailingListFactory.deleteSubscriberFromMailingList(ml , currentUser);
		}
	}	

	public void deleteAllSubscribers (String mailingListInode) {
		MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
		MailingListFactory.deleteAllSubscribersFromMailingList(ml);
	}	

	public void deleteUnsubscribers (String mailingListInode, String[] inodes) {
		for (String inode : inodes) {
			UserProxy currentUser;
			try {
				currentUser = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(inode,APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
			MailingListFactory.deleteUnsubscriberFromMailingList(ml , currentUser);
		}
	}	

	public void deleteAllUnsubscribers (String mailingListInode) {
		MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
		MailingListFactory.deleteAllUnsubscribersFromMailingList(ml);
	}	

	public void deleteBounces (String mailingListInode, String[] inodes) {
		for (String inode : inodes) {
			UserProxy currentUser;
			try {
				currentUser = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(inode,APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotReindexStateException(e.getMessage(), e);
			}
			MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
			MailingListFactory.deleteBounceFromMailingList(ml , currentUser);
		}
	}	

	public void deleteAllBounces (String mailingListInode) {
		MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
		MailingListFactory.deleteAllBouncesFromMailingList(ml);
	}

	/**
	 * Return a map list of the mailing lists the user is subscribe
	 * @param user
	 * @return List<Map<String, Object>>
	 */
	public List<Map<String, Object>> getUserMailingLists(String userid){
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>> ();
		User user;
		try {
			user = APILocator.getUserAPI().loadUserById(userid,APILocator.getUserAPI().getSystemUser(),false);
		} catch (NoSuchUserException e) {
			Logger.warn(this, "User doesn't Exist");
			return list;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return list;
		} 
		
		List<MailingList> mls = MailingListFactory.getMailingListsBySubscriber(user);
		for(MailingList ml : mls){
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("title", ml.getTitle());
			map.put("inode", ml.getInode());
			map.put("userid", ml.getUserId());
			list.add(map);
		}

		return list;
	}
	
	/**
	 * Add user to the mailing list and return a map list of the mailing lists the user is subscribe
	 * @param user
	 * @return List<Map<String, Object>>
	 */
	public List<Map<String, Object>> addUserToMailingList(String mlInode, String userid){
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>> ();
		User user;
		UserProxy userproxy;
		try {
			user = APILocator.getUserAPI().loadUserById(userid,APILocator.getUserAPI().getSystemUser(),false);
			userproxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userid,APILocator.getUserAPI().getSystemUser(), false);
		} catch (NoSuchUserException e) {
			Logger.warn(this, "User doesn't Exist");
			return list;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return list;
		} 		
		MailingList mailingList = MailingListFactory.getMailingListsByInode(mlInode);
		
		try {
			MailingListFactory.addMailingSubscriber(mailingList, userproxy, false);
		} catch (DotHibernateException e) {
			Logger.error(this,e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		}
		
		
		List<MailingList> mls = MailingListFactory.getMailingListsBySubscriber(user);
		for(MailingList ml : mls){
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("title", ml.getTitle());
			map.put("inode", ml.getInode());
			map.put("userid", ml.getUserId());
			list.add(map);
		}

		return list;
	}

	/**
	 * Delete user from the mailing list and return a map list of the mailing lists the user is subscribe
	 * @param user
	 * @return List<Map<String, Object>>
	 */
	public List<Map<String, Object>> deleteUserFromMailingList(String mlInode, String userid ){
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>> ();
		UserProxy userproxy;
		User user;
		try {
			user = APILocator.getUserAPI().loadUserById(userid,APILocator.getUserAPI().getSystemUser(),false);
			userproxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userid,APILocator.getUserAPI().getSystemUser(), false);
		} catch (NoSuchUserException e) {
			Logger.warn(this, "User doesn't Exist");
			return list;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return list;
		} 		

		MailingList mailingList = MailingListFactory.getMailingListsByInode(mlInode);
		
		MailingListFactory.deleteUserFromMailingList(mailingList, userproxy);
		
		List<MailingList> mls = MailingListFactory.getMailingListsBySubscriber(user);
		for(MailingList ml : mls){
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("title", ml.getTitle());
			map.put("inode", ml.getInode());
			map.put("userid", ml.getUserId());
			list.add(map);
		}

		return list;
	}

}
