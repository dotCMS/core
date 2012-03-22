package com.dotmarketing.portlets.mailinglists.factories;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
/**
 *
 * @author  will
 */
public class MailingListFactory {

	private static final String DEFAULT_MAILING_LIST_UNSUBSCRIBERS = "Do Not Send List";
	private static MailingList MAILING_LIST_UNSUBSCRIBERS = null;
	/**
	 * Returns true if the user belongs to the MAILINGLISTS_ADMIN_ROLE 
	 * @param user
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	public static boolean isMailingListAdmin (User user) throws PortalException, SystemException {
		List<Role> roles;
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e) {
			Logger.error(MailingListFactory.class,e.getMessage(),e);
			throw new SystemException(e);
		}
		Iterator<Role> rolesIt = roles.iterator();
		boolean isMailingListAdmin = false;
		while (rolesIt.hasNext()) {
			Role role = (Role) rolesIt.next();
			if (role.getName().equals(Config.getStringProperty("MAILINGLISTS_ADMIN_ROLE"))) {
				isMailingListAdmin = true;
				break;
			}
		}
		return isMailingListAdmin;
	}

	/**
	 * Returns true if the user belongs to the MAILINGLISTS_EDITOR_ROLE 
	 * @param user
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	public static boolean isMailingListEditor (User user) throws PortalException, SystemException {
		List<Role> roles;
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e) {
			Logger.error(MailingListFactory.class,e.getMessage(),e);
			throw new SystemException(e);
		}
		Iterator<Role> rolesIt = roles.iterator();
		boolean isMailingListEditor = false;
		while (rolesIt.hasNext()) {
			Role role = (Role) rolesIt.next();
			if (role.getName().equals(Config.getStringProperty("MAILINGLISTS_EDITOR_ROLE"))) {
				isMailingListEditor = true;
				break;
			}
		}
		return isMailingListEditor;
	}

	/**
	 * Returns true if the user belongs to the MAILINGLISTS_ADMIN_ROLE or to the  USER_MANAGER_ADMIN_ROLE
	 * @param user
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	public static boolean isMailingListManager (User user) throws PortalException, SystemException {
		List<Role> roles;
		try {
			
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e) {
			Logger.error(MailingListFactory.class,e.getMessage(),e);
			throw new SystemException(e);
		}
		Iterator<Role> rolesIt = roles.iterator();
		boolean isMailingListAdmin = false;
		while (rolesIt.hasNext()) {
			Role role = (Role) rolesIt.next();
			if (role.getName().equals(Config.getStringProperty("MAILINGLISTS_ADMIN_ROLE")) ||
					role.getName().equals(Config.getStringProperty("USER_MANAGER_ADMIN_ROLE"))) {
				isMailingListAdmin = true;
				break;
			}
		}
		return isMailingListAdmin;
	}

	/**
	 * Returns the mailing list specified
	 * @param inode
	 * @return MailingList
	 * @throws PortalException
	 * @throws SystemException
	 */
	@SuppressWarnings("unchecked")
	public static MailingList getMailingListsByInode(String inode) {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		MailingList ml =null;
		try {
			dh.setQuery(
			"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' and inode = ? ");
			dh.setParam(inode);
			ml = (MailingList)dh.load();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getMailingListsByInode failed:" + e, e);
		}
		return ml;
	}
	
	/**
	 * Returns a list of mailing lists that belongs to the given user
	 * @param user
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MailingList> getMailingListsByUser(User u) {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List<MailingList> list =null;
		try {
			dh.setQuery(
			"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' and user_id = ? order by title");
			dh.setParam(u.getUserId());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getMailingListsByUser failed:" + e, e);
		}
		return list;
	}

	/**
	 * Returns a list of mailing lists that belongs to the given user
	 * @param u
	 * @param orderby Order field to use
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MailingList> getMailingListsByUser(User u, String orderby) {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List<MailingList> list =null;
		try {
			dh.setQuery(
					"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' and user_id = ? order by " + orderby);
			dh.setParam(u.getUserId());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getMailingListsByUser failed:" + e, e);
		}
		return list;
	}

	/**
	 * Returns a list of mailing lists that belongs to the given user
	 * @param u
	 * @param orderby Order field to use
	 * @param direction sort direction (desc or asc)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MailingList> getMailingListsByUser(User u, String orderby,String direction) {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List<MailingList> list=null ;
		try {
			dh.setQuery(
					"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' and user_id = ? order by " + orderby + " " + direction);
			dh.setParam(u.getUserId());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getMailingListsByUser failed:" + e, e);
		}
		return list;
	}

	/**
	 * Retrieves all the mailing lists
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MailingList> getAllMailingLists() {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List<MailingList> list =null;
		try {
			dh.setQuery(
			"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' order by title");
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getAllMailingLists failed:" + e, e);
		}
		return list;
	}

	/**
	 * Retrieves all the mailing lists sort by specified field
	 * @param orderby
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MailingList> getAllMailingLists(String orderby) {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List<MailingList> list=null ;
		try {
			dh.setQuery(
					"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' order by " + orderby);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getAllMailingLists failed:" + e, e);
		}
		return list;
	}

	/**
	 * Retrieves all the mailing lists sort by specified field
	 * @param orderby
	 * @param direction
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MailingList> getAllMailingLists(String orderby,String direction) {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List<MailingList> list =null;
		try {
			dh.setQuery(
					"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' order by " + orderby + " " + direction);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getAllMailingLists failed:" + e, e);
		}
		return list;
	}

	/**
	 * Retrieves the list of all public mailing list
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MailingList> getAllPublicLists() {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List<MailingList> list =null;
		try {
			dh.setQuery(
					"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' and public_list = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " order by inode");
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getAllPublicLists failed:" + e, e);
		}
		return list;
	}

	public static MailingList newInstance() {
		MailingList m = new MailingList();
		m.setPublicList(false);
		return m;
	}

	/**
	 * get the Unsubscribers Mailing List, if it doesn't exists, it's created an returned 
	 * @return the Unsubscribers Mailing List
	 */
	public static MailingList getUnsubscribersMailingList() {
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		String title = Config.getStringProperty("MAILING_LIST_UNSUBSCRIBERS");
		if(!UtilMethods.isSet(title)){
			title = DEFAULT_MAILING_LIST_UNSUBSCRIBERS;
		}
		if(!UtilMethods.isSet(MAILING_LIST_UNSUBSCRIBERS) || !MAILING_LIST_UNSUBSCRIBERS.getTitle().equals(title) ){
			try {
				dh.setQuery(
						"from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList where type='mailing_list' and title = ? and user_id=? and public_list = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue());
				dh.setParam(title);
				dh.setParam(WebKeys.MAILING_LIST_SYSTEM);
				MailingList ml =  (MailingList) dh.load();

				if(!InodeUtils.isSet(ml.getInode())){
					ml = new MailingList();
					ml.setUserId(WebKeys.MAILING_LIST_SYSTEM);
					ml.setTitle(title);
					ml.setPublicList(true);
				}

				HibernateUtil.saveOrUpdate(ml);
				MAILING_LIST_UNSUBSCRIBERS = ml;
			} catch (DotHibernateException e) {
				Logger.error(MailingListFactory.class, "getUnsubscribersMailingList failed:" + e, e);
			}
		}

		return MAILING_LIST_UNSUBSCRIBERS;
	}

	//Methods to retrieve users from mailing list as subscribers, unsubscribers and bounces
	private static String mailingListUsersPullQuery = " from user_proxy, user_, " +
	"inode user_proxy_1_, tree where tree.parent = ? and user_proxy.user_id = user_.userid and " +
	"tree.child = user_proxy.inode and tree.relation_type = ? " +
	"and user_proxy_1_.inode = user_proxy.inode";
	private static String mailingListUsersPermissionsFilter = " and exists (select * from cms_role, " +
	"users_cms_roles, permission where cms_role.id = users_cms_roles.role_id and " +
	"users_cms_roles.user_id = ? and permission.roleid = cms_role.id and " +
	"permission = '" + String.valueOf(PERMISSION_READ) + "' " +
	"and inode_id = user_proxy.inode) ";

	/**
	 * Return the list of subscriber of a mailing list
	 */
	@SuppressWarnings("unchecked")
	public static List<UserProxy> getMailingListSubscribers (MailingList ml) throws Exception {
		List<UserProxy> sl = getMailingListSubscribers (ml.getInode(), null, -1, -1, null);
		return sl;
	}

	/**
	 * Return the list of subscriber of a mailing list for pagination and filter 
	 * by only the subscribers the user can see, if userId is null it will pull all results
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws NoSuchUserException 
	 */
	@SuppressWarnings("unchecked")
	public static List<UserProxy> getMailingListSubscribers (String mlInode, String userId, int start, int limit, String orderBy) throws Exception {
		String query = "select {user_proxy.*} " + mailingListUsersPullQuery;

		boolean filter = false;
		//Retrieving only the subscriber that the user is able to see 
		if(UtilMethods.isSet(userId)) {
			User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			if(!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
				query += mailingListUsersPermissionsFilter;
				filter = true;
			}
		}

		if(UtilMethods.isSet(orderBy)) {
			query += " order by " + orderBy;
		}

		HibernateUtil dh = new HibernateUtil(UserProxy.class);
		dh.setSQLQuery(query);
		dh.setParam(mlInode);
		dh.setParam("subscriber");
		if(filter) {
			dh.setParam(userId);
		}
		if(start > -1) dh.setFirstResult(start);
		if(limit > 0) dh.setMaxResults(limit);
		List<UserProxy> sl = dh.list();
		return sl;
	}

	/**
	 * Returns the count of subscribers of a mailing list
	 * @param mlInode
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static int getMailingListSubscribersCount (String mlInode) throws Exception{
		return getMailingListSubscribersCount(mlInode, null);
	}

	/**
	 * Return the count of subscribers in a mailing list filter by
	 * what the user can see if a userId is supplied
	 * 
	 * @param mlInode Mailing list inode
	 * @param userId User id to filter the count
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static int getMailingListSubscribersCount (String mlInode, String userId) throws Exception {
		String query = "select count(*) as total " + mailingListUsersPullQuery;

		boolean filter = false;
		//Retrieving only the subscriber that the user is able to see 
		if(UtilMethods.isSet(userId)) {
			User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			if(!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
				query += mailingListUsersPermissionsFilter;
				filter = true;
			}
		}

		DotConnect dc = new DotConnect();
		

		dc.setSQL(query);
		dc.addParam(mlInode);
		dc.addParam("subscriber");
		if(filter) {
			dc.addParam(userId);
		}
		return  dc.getInt("total");
	}

	/**
	 * Return the list of user who unsubscribed from a mailing list
	 */
	@SuppressWarnings("unchecked")
	public static List<UserProxy> getMailingListUnsubscribers (MailingList ml) throws Exception {
		List<UserProxy> sl = getMailingListUnsubscribers (ml.getInode(), null, -1, -1, null);
		return sl;
	}

	/**
	 * Return the list of user who unsubscribed from a mailing list for pagination and filter 
	 * by only the subscribers the user can see, if userId is null it will pull all results
	 */
	@SuppressWarnings("unchecked")
	public static List<UserProxy> getMailingListUnsubscribers (String mlInode, String userId, int start, int limit, String orderBy) throws Exception {
		String query = "select {user_proxy.*} " + mailingListUsersPullQuery;

		boolean filter = false;
		//Retrieving only the subscriber that the user is able to see 
		if(UtilMethods.isSet(userId)) {
			User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			if(!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
				query += mailingListUsersPermissionsFilter;
				filter = true;
			}
		}

		if(UtilMethods.isSet(orderBy)) {
			query += " order by " + orderBy;
		}

		HibernateUtil dh = new HibernateUtil(UserProxy.class);
		dh.setSQLQuery(query);
		dh.setParam(mlInode);
		dh.setParam("unsubscriber");
		if(filter) {
			dh.setParam(userId);
		}
		if(start > -1) dh.setFirstResult(start);
		if(limit > 0) dh.setMaxResults(limit);
		List<UserProxy> sl = dh.list();
		return sl;
	}

	/**
	 * Return the count of people how unsubscribe from a mailing list 
	 * @param mlInode
	 * @return
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public static int getMailingListUnsubscribersCount (String mlInode) throws Exception {
		return getMailingListUnsubscribersCount(mlInode, null);
	}

	/**
	 * Return the count of people how unsubscribe from a mailing list filter by
	 * what the passed user can see if a userId is supplied
	 * 
	 * @param mlInode Mailing list inode
	 * @param userId User id to filter the count
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static int getMailingListUnsubscribersCount (String mlInode, String userId) throws Exception {
		String query = "select count(*) as total " + mailingListUsersPullQuery;

		boolean filter = false;
		//Retrieving only the subscriber that the user is able to see 
		if(UtilMethods.isSet(userId)) {
			User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			if(!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
				query += mailingListUsersPermissionsFilter;
				filter = true;
			}
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(query);
		dc.addParam(mlInode);
		dc.addParam("unsubscriber");
		if(filter) {
			dc.addParam(userId);
		}
		return  dc.getInt("total");
	}	

	/**
	 * Return the list of bounces or errors from a mailing list
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public static List<UserProxy> getMailingListBounces (MailingList ml) throws Exception {
		List<UserProxy> sl = getMailingListBounces (ml.getInode(), null, -1, -1, null);
		return sl;
	}

	/**
	 * Return the list of bounces or errors from a mailing list for pagination and filter 
	 * by only the subscribers the user can see, if userId is null it will pull all results
	 */
	@SuppressWarnings("unchecked")
	public static List<UserProxy> getMailingListBounces (String mlInode, String userId, int start, int limit, String orderBy) throws Exception {
		String query = "select {user_proxy.*} " + mailingListUsersPullQuery;

		boolean filter = false;
		//Retrieving only the subscriber that the user is able to see 
		if(UtilMethods.isSet(userId)) {
			User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			if(!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
				query += mailingListUsersPermissionsFilter;
				filter = true;
			}
		}

		if(UtilMethods.isSet(orderBy)) {
			query += " order by " + orderBy;
		}

		HibernateUtil dh = new HibernateUtil(UserProxy.class);
		dh.setSQLQuery(query);
		dh.setParam(mlInode);
		dh.setParam("bounce");
		if(filter) {
			dh.setParam(userId);
		}
		if(start > -1) dh.setFirstResult(start);
		if(limit > 0) dh.setMaxResults(limit);
		List<UserProxy> sl = dh.list();
		return sl;
	}

	/**
	 * Returns the count of bounces/errors of a mailing list
	 * @param mlInode
	 * @return
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public static int getMailingListBouncesCount (String mlInode) throws Exception {
		return getMailingListBouncesCount (mlInode, null);
	}

	/**
	 * Return the count of bounces/errors in a mailing list filter by
	 * what the user can see if a userId is supplied
	 * 
	 * @param mlInode Mailing list inode
	 * @param userId User id to filter the count
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static int getMailingListBouncesCount (String mlInode, String userId) throws Exception{
		String query = "select count(*) as total " + mailingListUsersPullQuery;

		boolean filter = false;
		//Retrieving only the subscriber that the user is able to see 
		if(UtilMethods.isSet(userId)) {
			User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			if(!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
				query += mailingListUsersPermissionsFilter;
				filter = true;
			}
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(query);
		dc.addParam(mlInode);
		dc.addParam("bounce");
		if(filter) {
			dc.addParam(userId);
		}
		return  dc.getInt("total");
	}	


	//methods to manipulate mailing lists

	/**
	 * Add a subscriber to the passed mailing
	 * @param ml The mailing list to add it
	 * @param up The user to add
	 * @param force Forces the subscription even if the user was unsubscribed or belongs to the bounces
	 * @return true if the user was successfully added to the list, false - if the user could be added because belongs to the unsubscribers  
	 * @throws DotHibernateException 
	 */
	public static boolean addMailingSubscriber (MailingList ml, UserProxy up, boolean force) throws DotHibernateException {

		Tree currentRel = TreeFactory.getTree(ml, up, null);

		if((currentRel != null && InodeUtils.isSet(currentRel.getChild())) && !force)
			return false;
		else if ((currentRel != null && InodeUtils.isSet(currentRel.getChild())) && force) {

			currentRel.setRelationType("subscriber");
			TreeFactory.saveTree(currentRel);
			HibernateUtil.saveOrUpdate(ml);

		} else {

			ml.addChild(up, "subscriber");
			MailingList m = getMailingListsByInode(ml.getInode());
			HibernateUtil.saveOrUpdate(m);

		}

		return true;
	}

	/**
	 * return a list of all the mailing list (private and public) where the user is susbcribed
	 * @param u user whom mailing lists are obtained
	 * @return list of all the mailing list (private and public) where the user is susbcribed
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<MailingList> getMailingListsBySubscriber(User u) {
		UserProxy up;
		try {
			up = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(u,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(MailingListFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}	
		List<MailingList> mailingLists = InodeFactory.getParentsOfClassByRelationType(up, MailingList.class, "subscriber");
		return mailingLists;
	}

	/**
	 * Unsubscribe a subscriber from the passed mailing
	 * @param ml The mailing list 
	 * @param up The user to unsubscribed
	 * @param force Forces the unsubscription even if the user was never in the list
	 * @return true if the user was successfully unsubscribed from the list, 
	 * 			false - if the user could not be unsubscribed because never belonged to the list   
	 * @throws DotHibernateException 
	 */
	public static boolean unsubcribeFromMailingList (MailingList ml, UserProxy up, boolean force) throws DotHibernateException {
		Tree currentRel = TreeFactory.getTree(ml, up);
		if (currentRel != null && InodeUtils.isSet(currentRel.getChild())) {

			currentRel.setRelationType("unsubscriber");
			TreeFactory.saveTree(currentRel);
			HibernateUtil.saveOrUpdate(ml);

			return true;
		} else if(force) {

			ml.addChild(up, "unsubscriber");
			HibernateUtil.saveOrUpdate(ml);

			return true;
		}
		return false;
	}	

	/**
	 * Delete a user from the given mailing regardless if it's a subscriber, unsubscriber or bounce
	 * @param ml The mailing list 
	 * @param up The user to remove
	 * @return true if the user was successfully removed from the list, 
	 * 			false - if the user could not be removed because never belonged to the list   
	 */
	public static boolean deleteUserFromMailingList (MailingList ml, UserProxy up) {
		Tree currentRel = TreeFactory.getTree(ml, up, null);
		if (currentRel != null && InodeUtils.isSet(currentRel.getChild())) {
			TreeFactory.deleteTree(currentRel);
			return true;
		} 
		return false;
	}	

	/**
	 * Delete subscriber of the subscriber to the given mailing
	 * @param ml The mailing list 
	 * @param up The user to remove
	 * @return true if the user was successfully removed from the list, 
	 * 			false - if the user could not be removed because never belonged to the list   
	 */
	public static boolean deleteSubscriberFromMailingList (MailingList ml, UserProxy up) {
		Tree currentRel = TreeFactory.getTree(ml.getInode(), up.getUserId(), "subscriber");
		if (currentRel != null && InodeUtils.isSet(currentRel.getChild())) {
			TreeFactory.deleteTree(currentRel);
			return true;
		} 
		return false;
	}	

	/**
	 * Delete subscriber of the subscriber to the given mailing
	 * @param ml The mailing list 
	 * @param up The user to remove
	 * @return true if the user was successfully removed from the list, 
	 * 			false - if the user could not be removed because never belonged to the list   
	 */
	public static boolean deleteUnsubscriberFromMailingList (MailingList ml, UserProxy up) {
		Tree currentRel = TreeFactory.getTree(ml.getInode(), up.getUserId(), "unsubscriber");
		if (currentRel != null && InodeUtils.isSet(currentRel.getChild())) {
			TreeFactory.deleteTree(currentRel);
			return true;
		} 
		return false;
	}	

	/**
	 * Delete subscriber of the subscriber to the given mailing
	 * @param ml The mailing list 
	 * @param up The user to remove
	 * @return true if the user was successfully removed from the list, 
	 * 			false - if the user could not be removed because never belonged to the list   
	 */
	public static boolean deleteBounceFromMailingList (MailingList ml, UserProxy up) {
		Tree currentRel = TreeFactory.getTree(ml.getInode(), up.getUserId(), "bounce");
		if (currentRel != null && InodeUtils.isSet(currentRel.getChild())) {
			TreeFactory.deleteTree(currentRel);
			return true;
		} 
		return false;
	}	

	/**
	 * Change a user to the bounce list from the passed mailing
	 * @param ml The mailing list 
	 * @param up The user to change his status to bounced
	 * @return true if the user was successfully added to the bounce list, 
	 * 			false - if the user could not be added to the bounces list because never belonged to the list   
	 * @throws DotHibernateException 
	 */
	public static boolean markAsBounceFromMailingList (MailingList ml, UserProxy up) throws DotHibernateException {
		Tree currentRel = TreeFactory.getTree(ml, up, null);
		if (currentRel != null && InodeUtils.isSet(currentRel.getChild())) {
			TreeFactory.deleteTree(currentRel);
			ml.addChild(up, "bounce");
			HibernateUtil.saveOrUpdate(ml);
			return true;
		}
		return false;
	}

	/**
	 * Removes all the associated subscriber from the given list
	 * @param ml
	 */
	public static void deleteAllSubscribersFromMailingList(MailingList ml) {
		DotConnect dc = new DotConnect ();
		dc.setSQL("delete from tree where parent = ? and relation_type = 'subscriber'");
		dc.addParam(ml.getInode());
		dc.getResult();
	}	

	/**
	 * Removes all the associated unsubscriber from the given list
	 * @param ml
	 */
	public static void deleteAllUnsubscribersFromMailingList(MailingList ml) {
		DotConnect dc = new DotConnect ();
		dc.setSQL("delete from tree where parent = ? and relation_type = 'unsubscriber'");
		dc.addParam(ml.getInode());
		dc.getResult();
	}	

	/**
	 * Removes all the associated bounce from the given list
	 * @param ml
	 */
	public static void deleteAllBouncesFromMailingList(MailingList ml) {
		DotConnect dc = new DotConnect ();
		dc.setSQL("delete from tree where parent = ? and relation_type = 'bounce'");
		dc.addParam(ml.getInode());
		dc.getResult();
	}

	/**
	 * Checks if a user is subscribed to a mailing list
	 * @param ml
	 * @param up
	 * @return true if the user is a subscriber of the given list
	 */
	public static boolean isSubscribed(MailingList ml, UserProxy up) {
		Tree tree = TreeFactory.getTree(ml.getInode(), up.getInode(), "subscriber");

		return tree != null && InodeUtils.isSet(tree.getParent()) && InodeUtils.isSet(tree.getChild());
	}

	/**
	 * Retrieves all the mailing lists that match condition.
	 * 
	 * @param condition
	 * @return Mailing lists that match the search condition.
	 */
	public static List getAllMailingListsCondition(String condition) {
		// Case insensitive search
		condition = "'%" + condition.toLowerCase() + "%'";

		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List list =null;
		try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList " + 
					    "where type='mailing_list' and lower(title) like " +
						condition + " order by title");
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getAllMailingListsCondition failed:"+ e, e);
		}
		return list;
	}

	/**
	 * Retrieves mailing lists that belong a user and match condition.
	 * 
	 * @param user
	 * @param condition
	 * @return Mailing lists for user that match the search condition.
	 */
	public static List getMailingListsByUserCondition(User user, String condition) {
		// Case insensitive search
		condition = "'%" + condition.toLowerCase() + "%'";
		
		HibernateUtil dh = new HibernateUtil(MailingList.class);
		List list =null ;
		try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.mailinglists.model.MailingList " +
					    "where type='mailing_list' and user_id = ? " +
					    "and lower(title) like " + condition + " order by title");
			dh.setParam(user.getUserId());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(MailingListFactory.class, "getMailingListsByUserCondition failed:"+ e, e);
		}
		return list;
	}		
}
