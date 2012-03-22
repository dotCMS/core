package com.dotmarketing.portlets.user.ajax;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.generic.SortTool;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.UserProxyAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchRoleException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.Encryptor;

import edu.emory.mathcs.backport.java.util.Collections;

public class UserAjax {

	// Constants for internal use only
	private static final String USER_TYPE_VALUE = "user";
	private static final String ROLE_TYPE_VALUE = "role";

	public Map<String, Object> getUserById(String userId) throws DotDataException {

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		UserProxyAPI uProxyWebAPI = APILocator.getUserProxyAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		UserAPI uAPI = APILocator.getUserAPI();

		User user = null;
		try {
			user = uAPI.loadUserById(userId,uWebAPI.getSystemUser(), !uWebAPI.isLoggedToBackend(request));

			Map<String, Object> aRecord = user.toMap();
			aRecord.put("id", user.getUserId());
			aRecord.put("type", USER_TYPE_VALUE);
			aRecord.put("name", user.getFullName());
			aRecord.put("firstName", user.getFirstName());
			aRecord.put("lastName", user.getLastName());
			aRecord.put("emailaddress", user.getEmailAddress());

			UserProxy up = uProxyWebAPI.getUserProxy(user, uWebAPI.getSystemUser(), !uWebAPI.isLoggedToBackend(request));
			aRecord.putAll(up.getMap());

			return aRecord;

		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	public String addUser (String userId, String firstName, String lastName, String email, String password) throws DotDataException, DotRuntimeException, PortalException, SystemException, DotSecurityException {

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI uAPI = APILocator.getUserAPI();

		User user = uAPI.createUser(userId, email);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setPassword(Encryptor.digest(password));
		uAPI.save(user, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));

		return user.getUserId();

	}

	public String updateUser (String userId, String newUserID, String firstName, String lastName, String email, String password) throws DotRuntimeException, PortalException, SystemException,
		DotDataException, DotSecurityException {

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI uAPI = APILocator.getUserAPI();
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		UserProxyAPI upAPI = APILocator.getUserProxyAPI();
		User userToSave;
		User loggedInUser = uWebAPI.getLoggedInUser(request);

		try {
			userToSave = (User)uAPI.loadUserById(userId,uAPI.getSystemUser(),false).clone();
			userToSave.setModified(false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
		userToSave.setFirstName(firstName);
		userToSave.setLastName(lastName);
		userToSave.setEmailAddress(email);
		if(password != null) {
			userToSave.setPassword(Encryptor.digest(password));
		}

		if(userToSave.getUserId().equalsIgnoreCase(loggedInUser.getUserId())){
			uAPI.save(userToSave, uAPI.getSystemUser(), !uWebAPI.isLoggedToBackend(request));
		}else if(perAPI.doesUserHavePermission(upAPI.getUserProxy(userToSave,uAPI.getSystemUser(), false), PermissionAPI.PERMISSION_EDIT,loggedInUser, false)){
			uAPI.save(userToSave, loggedInUser, !uWebAPI.isLoggedToBackend(request));
		}else{
			throw new DotSecurityException("User doesn't have permission to save the user which is trying to be saved");
		}
		return userToSave.getUserId();

	}

	public boolean deleteUser (String userId) throws DotHibernateException {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI uAPI = APILocator.getUserAPI();

		User user;
		try {
			HibernateUtil.startTransaction();
			user = uAPI.loadUserById(userId,uWebAPI.getLoggedInUser(request),false);
			APILocator.getContentletAPI().removeUserReferences(userId);
			uAPI.delete(user, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
			HibernateUtil.commitTransaction();
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Logger.error(this, e.getMessage(), e);
			return false;
		}

		return true;
	}

	public List<Map<String, Object>> getUserRoles (String userId) throws DotDataException {
		List<Map<String, Object>> roleMaps = new ArrayList<Map<String,Object>>();
		if(UtilMethods.isSet(userId)){
			RoleAPI roleAPI = APILocator.getRoleAPI();
			List<com.dotmarketing.business.Role> roles = roleAPI.loadRolesForUser(userId, false);
			for(com.dotmarketing.business.Role r : roles) {
				roleMaps.add(r.toMap());
			}
		}
		return roleMaps;
	}

	public Map<String, Boolean> getUserRolesValues (String userId, String hostIdentifier) throws DotDataException {
		Map<String, Boolean> userPerms = new HashMap<String,Boolean>();
		if(UtilMethods.isSet(userId)){
			RoleAPI roleAPI = APILocator.getRoleAPI();
			List<com.dotmarketing.business.Role> roles = roleAPI.loadRolesForUser(userId, false);
			for(com.dotmarketing.business.Role r : roles) {
				List<Permission> perms = APILocator.getPermissionAPI().getPermissionsByRole(r, false);
				for (Permission p : perms) {
					String permType = p.getType();
					permType = permType.equals(Folder.class.getCanonicalName())?"folderModifiable":
						 permType.equals(Template.class.getCanonicalName())?"templateModifiable":
						 permType.equals(Container.class.getCanonicalName())?"containerModifiable":
						 permType.equals(File.class.getCanonicalName())?"fileModifiable":"";

					Boolean hasPerm = userPerms.get(permType)!=null?userPerms.get(permType):false;

					 if(UtilMethods.isSet(permType) && p.getInode().equals(hostIdentifier)) {
						 userPerms.put(permType, hasPerm | (p.getPermission()==PermissionAPI.PERMISSION_EDIT ||
								 p.getPermission()==PermissionAPI.PERMISSION_PUBLISH));
					 }
				}
			}
		}
		return userPerms;
	}

	public void updateUserRoles (String userId, List<String> roleIds) throws DotDataException, NoSuchUserException, DotRuntimeException, PortalException, SystemException, DotSecurityException {

		WebContext ctx = WebContextFactory.get();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI uAPI = APILocator.getUserAPI();

		List<com.dotmarketing.business.Role> userRoles = roleAPI.loadRolesForUser(userId);

		User user = uAPI.loadUserById(userId,uWebAPI.getLoggedInUser(request),false);

		//Remove all roles not assigned
		for(com.dotmarketing.business.Role r : userRoles) {
			if(!roleIds.contains(r.getId())) {
				if(r.isEditUsers())
					roleAPI.removeRoleFromUser(r, user);
			}
		}

		for(com.dotmarketing.business.Role r : roleAPI.loadRolesForUser(userId)) {
			if(roleIds.contains(r.getId())) {
				roleIds.remove(r.getId());
			}
		}

		//Adding missing roles
		for(String roleId : roleIds) {
			com.dotmarketing.business.Role r = roleAPI.loadRoleById(roleId);
			if(r.isEditUsers())
				roleAPI.addRoleToUser(r, user);
		}


	}

	public List<Map<String, String>> loadUserAddresses(String userId) throws DotDataException {

		UserAPI uAPI = APILocator.getUserAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		User user = null;
		List<Address> userAddresses = new ArrayList<Address>();
		try {
			if(UtilMethods.isSet(userId)){
				user = uAPI.loadUserById(userId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
				userAddresses = uAPI.loadUserAddresses(user, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
			}
		} catch (NoSuchUserException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotRuntimeException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}

		List<Map<String, String>> addressesToReturn = new ArrayList<Map<String,String>>();
		for(Address add : userAddresses) {
			addressesToReturn.add(add.toMap());
		}
		return addressesToReturn;
	}

	public Map<String, String> addNewUserAddress(String userId, String addressDescription, String street1, String street2, String city, String state,
			String zip, String country, String phone, String fax, String cell) throws DotDataException {

		UserAPI uAPI = APILocator.getUserAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		User user = null;
		try {
			user = uAPI.loadUserById(userId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
		} catch (NoSuchUserException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotRuntimeException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}

		Address ad = new Address();
		ad.setDescription(addressDescription);
		ad.setStreet1(street1);
		ad.setStreet2(street2);
		ad.setCity(city);
		ad.setState(state);
		ad.setZip(zip);
		ad.setCountry(country);
		ad.setPhone(phone);
		ad.setFax(fax);
		ad.setCell(cell);

		try {
			uAPI.saveAddress(user, ad, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
		} catch (DotDataException e) {
			throw new DotDataException(e.getCause().toString(), e);
		} catch (DotRuntimeException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}

		return ad.toMap();

	}

	public Map<String, String> saveUserAddress(String userId, String addressId, String addressDescription, String street1, String street2, String city, String state,
			String zip, String country, String phone, String fax, String cell) throws DotDataException {

		UserAPI uAPI = APILocator.getUserAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		User user = null;
		try {
			user = uAPI.loadUserById(userId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
		} catch (NoSuchUserException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotRuntimeException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}

		Address ad = new Address();
		ad.setAddressId(addressId);
		ad.setDescription(addressDescription);
		ad.setStreet1(street1);
		ad.setStreet2(street2);
		ad.setCity(city);
		ad.setState(state);
		ad.setZip(zip);
		ad.setCountry(country);
		ad.setPhone(phone);
		ad.setFax(fax);
		ad.setCell(cell);

		try {
			uAPI.saveAddress(user, ad, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
		} catch (DotRuntimeException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}

		return ad.toMap();

	}

	public String deleteAddress(String userId, String addressId) throws DotDataException {
		UserAPI uAPI = APILocator.getUserAPI();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		Address ad;
		try {
			ad = uAPI.loadAddressById(addressId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
			uAPI.deleteAddress(ad, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
		} catch (DotRuntimeException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}

		return addressId;
	}

	public void saveUserAddittionalInfo(String userId, boolean active, String prefix, String suffix, String title, String company, String website, String[] additionalVars)
	 	throws DotDataException {

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		UserAPI uAPI = APILocator.getUserAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		try {

			User user = uAPI.loadUserById(userId,uWebAPI.getLoggedInUser(request),false);

			UserProxyAPI uProxyAPI = APILocator.getUserProxyAPI();
			User u = uAPI.loadUserById(userId, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
			UserProxy up = uProxyAPI.getUserProxy(u, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));


			if(!active && u.getUserId().equals(uWebAPI.getLoggedInUser(request).getUserId())){
				throw new DotRuntimeException(LanguageUtil.get(uWebAPI.getLoggedInUser(request),"deactivate-your-own-user-error"));
			}

			u.setActive(active);
			up.setPrefix(prefix);
			up.setSuffix(suffix);
			up.setTitle(title);
			up.setCompany(company);
			up.setWebsite(website);
			for(int i = 1; i <= additionalVars.length; i++) {
				up.setVar(i, additionalVars[i - 1]);
			}

			uAPI.save(u, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));
			uProxyAPI.saveUserProxy(up, uWebAPI.getLoggedInUser(request), !uWebAPI.isLoggedToBackend(request));

		} catch (DotRuntimeException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	private void setActive(boolean active) {
		// TODO Auto-generated method stub

	}

	public Map<String, Object> getRoleById(String roleId) throws SystemException {
		RoleAPI api = APILocator.getRoleAPI();
		Role role;
		try {
			role = com.dotmarketing.business.APILocator.getRoleAPI().loadRoleById(roleId);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
		if(role == null){
			return null;
		}
		HashMap<String, Object> aRecord = new HashMap<String, Object>();
		aRecord.put("id", role.getId());
		aRecord.put("type", ROLE_TYPE_VALUE);
		aRecord.put("name", role.getName());
		aRecord.put("emailaddress", "");
		return aRecord;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getUsersAndRolesList(String assetInode, String permission, Map<String, String> params) throws SystemException {

		int start = 0;
		if(params.containsKey("start"))
			start = Integer.parseInt((String)params.get("start"));

		int limit = -1;
		if(params.containsKey("limit"))
			limit = Integer.parseInt((String)params.get("limit"));

		String query = "";
		if(params.containsKey("query"))
			query = (String) params.get("query");

		boolean hideSystemRoles =false;
		if(params.get("hideSystemRoles")!=null){
			hideSystemRoles = params.get("hideSystemRoles").equals("true")?true:false;
		}

		Map<String, Object> results;

		if ( (InodeUtils.isSet(assetInode) && !assetInode.equals("0")) && (UtilMethods.isSet(permission) && !permission.equals("0")) ) {
			results = processUserAndRoleListWithPermissionOnInode(assetInode, permission, query, start, limit, hideSystemRoles);
		} else {
			results = processUserAndRoleList(query, start, limit, hideSystemRoles);
		}

		return results;
	}


	@SuppressWarnings("unchecked")
	public Map<String, Object> getRolesList(String assetInode, String permission, Map<String, String> params) throws SystemException {

		int start = 0;
		if(params.containsKey("start"))
			start = Integer.parseInt((String)params.get("start"));

		int limit = -1;
		if(params.containsKey("limit"))
			limit = Integer.parseInt((String)params.get("limit"));

		String query = "";
		if(params.containsKey("query"))
			query = (String) params.get("query");

		boolean hideSystemRoles =false;
		if(params.get("hideSystemRoles")!=null){
			hideSystemRoles = params.get("hideSystemRoles").equals("true")?true:false;
		}

		Map<String, Object> results;

		if ( (InodeUtils.isSet(assetInode) && !assetInode.equals("0")) && (UtilMethods.isSet(permission) && !permission.equals("0")) ) {
			results = processRoleListWithPermissionOnInode(assetInode, permission, query, start, limit, hideSystemRoles);
		} else {
			results = processRoleList(query, start, limit, hideSystemRoles);
		}


		return results;
	}



	public Map<String, Object> getUsersList(String assetInode, String permission, Map<String, String> params) throws SystemException {

		int start = 0;
		if(params.containsKey("start"))
			start = Integer.parseInt((String)params.get("start"));

		int limit = -1;
		if(params.containsKey("limit"))
			limit = Integer.parseInt((String)params.get("limit"));

		String query = "";
		if(params.containsKey("query"))
			query = (String) params.get("query");

		Map<String, Object> results;

		if ( (InodeUtils.isSet(assetInode) && !assetInode.equals("0")) && (UtilMethods.isSet(permission) && !permission.equals("0")) ) {
			results = processUserListWithPermissionOnInode(assetInode, permission, query, start, limit);
		} else {
			results = processUserList(query, start, limit);
		}

		return results;
	}

	public List getUsersList2(String assetInode, String permission, Map<String, String> params) throws SystemException {

		int start = 0;
		if(params.containsKey("start"))
			start = Integer.parseInt((String)params.get("start"));

		int limit = -1;
		if(params.containsKey("limit"))
			limit = Integer.parseInt((String)params.get("limit"));

		String query = "";
		if(params.containsKey("query"))
			query = (String) params.get("query");

		Map<String, Object> results;

		if ( (UtilMethods.isSet(assetInode) && !assetInode.equals("0")) && (UtilMethods.isSet(permission) && !permission.equals("0")) ) {
			results = processUserListWithPermissionOnInode(assetInode, permission, query, start, limit);
		} else {
			results = processUserList(query, start, limit);
		}

		return (List) results.get("data");
	}

	private Map<String, Object> processRoleListWithPermissionOnInode(String assetInode, String permission, String query, int start, int limit,
			boolean hideSystemRoles) {

		Map<String, Object> results;

		try {
			int permissionType = Integer.parseInt(permission);
			String inode = assetInode;
			results = new RolesListTemplate(inode, permissionType, query, start, limit, hideSystemRoles) {

				PermissionAPI perAPI = APILocator.getPermissionAPI();

				@Override
				public int getRoleCount(boolean hideSystemRoles) throws NoSuchRoleException,
						SystemException {
					return perAPI.getRoleCount(inode, permissionType, filter, hideSystemRoles);
				}

				@Override
				public List<Role> getRoles(boolean hideSystemRoles) throws NoSuchRoleException,
						SystemException {
					List<Role> roles = perAPI.getRoles(inode, permissionType, filter, start, limit, hideSystemRoles);
					Collections.sort(roles, new Comparator<Role>(){

						public int compare(Role o1, Role o2) {
							return o1.getName().compareTo(o2.getName());
						}

					});

					return roles;
				}


			}.perform();
		}
		catch(NumberFormatException nfe) {
    		Logger.warn(UserAjax.class, String.format("::getUsersAndRolesList -> Invalid parameters inode(%s) permission(%s).", assetInode, permission));
    		results = new HashMap<String,Object>(0);
		}

		return results;

	}

private Map<String, Object> processRoleList(String query, int start, int limit, boolean hideSystemRoles) {

		Map<String, Object> results = new RolesListTemplate("", 0, query, start, limit, hideSystemRoles)
		{

			RoleAPI roleAPI = APILocator.getRoleAPI();


			@Override
			public int getRoleCount(boolean hideSystemRoles) throws NoSuchRoleException, SystemException {
				List<Role> roleList;
				try {
					roleList = APILocator.getRoleAPI().findRolesByNameFilter(filter,0,0);
				} catch (DotDataException e) {
					Logger.error(UserAjax.class,e.getMessage(),e);
					throw new SystemException(e);
				}
				List<Role> roleListTemp = new ArrayList<Role>(roleList);
				for(Role r : roleListTemp) {
					if(PortalUtil.isSystemRole(r) && !r.getFQN().startsWith("Users"))
						roleList.remove(r);
				}
				return roleList.size();
			}


			@Override
			public List<Role> getRoles(boolean hideSystemRoles) throws NoSuchRoleException, SystemException {
				List<Role> roleList;
				try {
					roleList = APILocator.getRoleAPI().findRolesByNameFilter(filter, start, limit);
				} catch (DotDataException e) {
					Logger.error(UserAjax.class,e.getMessage(),e);
					throw new SystemException(e);
				}
				List<Role> roleListTemp = new ArrayList<Role>(roleList);
				for(Role r : roleListTemp) {
					if(PortalUtil.isSystemRole(r)&& hideSystemRoles && !r.getFQN().startsWith("Users"))
						roleList.remove(r);
				}
				return roleList;
			}

		}
		.perform();

		return results;

	}





	private Map<String, Object> processUserAndRoleListWithPermissionOnInode(String assetInode, String permission, String query, int start, int limit,
			boolean hideSystemRoles) {

		Map<String, Object> results;

		try {
			int permissionType = Integer.parseInt(permission);
			String inode = assetInode;
			results = new UsersAndRolesListTemplate(inode, permissionType, query, start, limit, hideSystemRoles) {

				PermissionAPI perAPI = APILocator.getPermissionAPI();

				@Override
				public int getRoleCount(boolean hideSystemRoles) throws NoSuchRoleException,
						SystemException {
					return perAPI.getRoleCount(inode, permissionType, filter, hideSystemRoles);
				}

				@Override
				public List<Role> getRoles(boolean hideSystemRoles) throws NoSuchRoleException,
						SystemException {
					List<Role> roles = perAPI.getRoles(inode, permissionType, filter, start, limit, hideSystemRoles);
					Collections.sort(roles, new Comparator<Role>(){

						public int compare(Role o1, Role o2) {
							return o1.getName().compareTo(o2.getName());
						}

					});

					return roles;
				}

				@Override
				public int getUserCount() {
					return perAPI.getUserCount(inode, permissionType, filter);
				}

				@Override
				public List<User> getUsers(int newStart, int newLimit) {
					return perAPI.getUsers(inode, permissionType, filter, newStart, newLimit);
				}
			}.perform();
		}
		catch(NumberFormatException nfe) {
    		Logger.warn(UserAjax.class, String.format("::getUsersAndRolesList -> Invalid parameters inode(%s) permission(%s).", assetInode, permission));
    		results = new HashMap<String,Object>(0);
		}

		return results;

	}

	private Map<String, Object> processUserAndRoleList(String query, int start, int limit, boolean hideSystemRoles) {

		Map<String, Object> results = new UsersAndRolesListTemplate("", 0, query, start, limit, hideSystemRoles)
		{

			RoleAPI roleAPI = APILocator.getRoleAPI();
			UserAPI userAPI = APILocator.getUserAPI();

			@Override
			public int getRoleCount(boolean hideSystemRoles) throws NoSuchRoleException, SystemException {
				List<Role> roleList;
				try {
					roleList = APILocator.getRoleAPI().findRolesByNameFilter(filter,0,0);
				} catch (DotDataException e) {
					Logger.error(UserAjax.class,e.getMessage(),e);
					throw new SystemException(e);
				}
				List<Role> roleListTemp = new ArrayList<Role>(roleList);
				for(Role r : roleListTemp) {
					if(PortalUtil.isSystemRole(r))
						roleList.remove(r);
				}
				return roleList.size();
			}

			@Override
			public List<Role> getRoles(boolean hideSystemRoles) throws NoSuchRoleException, SystemException {
				List<Role> roleList;
				try {
					roleList = APILocator.getRoleAPI().findRolesByNameFilter(filter, start, limit);
				} catch (DotDataException e) {
					Logger.error(UserAjax.class,e.getMessage(),e);
					throw new SystemException(e);
				}
				List<Role> roleListTemp = new ArrayList<Role>(roleList);
				for(Role r : roleListTemp) {
					if(PortalUtil.isSystemRole(r)&& hideSystemRoles)
						roleList.remove(r);
				}
				return roleList;
			}

			@Override
			public int getUserCount() {
				try {
					return new Long(userAPI.getCountUsersByNameOrEmail(filter)).intValue();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
					return 0;
				}
			}

			@Override
			public List<User> getUsers(int newStart, int newLimit) {
				try {
					return userAPI.getUsersByName(filter, newStart, newLimit, APILocator.getUserAPI().getSystemUser(),false);
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
					return new ArrayList<User>();
				}
			}
		}
		.perform();

		return results;

	}

	private Map<String, Object> processUserListWithPermissionOnInode(String assetInode, String permission, String query, int start, int limit) {

		Map<String, Object> results;

		try {
			int permissionType = Integer.parseInt(permission);
			String inode = assetInode;
			results = new UsersListTemplate(inode, permissionType, query, start, limit) {

				PermissionAPI perAPI = APILocator.getPermissionAPI();

				@Override
				public int getUserCount() {
					return perAPI.getUserCount(inode, permissionType, filter);
				}

				@Override
				public List<User> getUsers() {
					return perAPI.getUsers(inode, permissionType, filter, start, limit);
				}
			}.perform();
		}
		catch(NumberFormatException nfe) {
			Logger.warn(UserAjax.class, String.format("::getUsersList -> Invalid parameters inode(%s) permission(%s).", assetInode, permission));
			results = new HashMap<String,Object>(0);
		}

		return results;
	}

	private Map<String, Object> processUserList(String query, int start, int limit) {

		Map<String, Object> results = new UsersListTemplate("", 0, query, start, limit)
		{
				UserAPI userAPI = APILocator.getUserAPI();

				@Override
				public int getUserCount() {
					try {
						return new Long(userAPI.getCountUsersByNameOrEmail(filter)).intValue();
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
						return 0;
					}
				}

				@Override
				public List<User> getUsers() {
					try {
						return userAPI.getUsersByNameOrEmail(filter, start, limit);
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
						return new ArrayList<User>();
					}
				}
		}
		.perform();

		return results;
	}


	public boolean hasUserRoles(String userId, String[] roles) {
		User user;
		try {
			user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return false;
		}
		for(String roleName : roles) {
			try {
				if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, roleName))
					return true;
			} catch (DotDataException e) {
				Logger.error(UserAjax.class,e.getMessage(),e);
				return false;
			}
		}
		return false;
	}

	public List<Map<String, Object>> getUserCategories(String userId) throws PortalException, SystemException, DotDataException, DotSecurityException {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserProxyAPI userProxyAPI = APILocator.getUserProxyAPI();

		CategoryAPI catAPI = APILocator.getCategoryAPI();
		UserProxy uProxy = userProxyAPI.getUserProxy(userId, uWebAPI.getLoggedInUser(request), uWebAPI.isLoggedToBackend(request));
		List<Category> children = catAPI.getChildren(uProxy, uWebAPI.getLoggedInUser(request), uWebAPI.isLoggedToBackend(request));

		List<Map<String, Object>> toReturn = new ArrayList<Map<String,Object>>();
		for(Category child: children) {
			toReturn.add(child.getMap());
		}

		return toReturn;
	}

	public void updateUserCategories(String userId, String[] categories) throws PortalException, SystemException, DotSecurityException, DotDataException {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserProxyAPI userProxyAPI = APILocator.getUserProxyAPI();

		User user = uWebAPI.getLoggedInUser(request);
		boolean respectFrontend = uWebAPI.isLoggedToBackend(request);

		CategoryAPI catAPI = APILocator.getCategoryAPI();
		UserProxy userProxy = userProxyAPI.getUserProxy(userId, uWebAPI.getLoggedInUser(request), uWebAPI.isLoggedToBackend(request));

		HibernateUtil.startTransaction();
		List<Category> myUserCategories = catAPI.getChildren(userProxy, user, respectFrontend);
		for (Object o : myUserCategories) {
			if(o instanceof Category && catAPI.canUseCategory((Category)o, user, respectFrontend)){
				catAPI.removeChild(userProxy, (Category)o, user, respectFrontend);
			}
		}
		for(int i = 0;i < categories.length;i++)
		{
			Category category = catAPI.find(categories[i], user, respectFrontend);
			if(InodeUtils.isSet(category.getInode()))
			{
				catAPI.addChild(userProxy, category, user, respectFrontend);
			}
		}
		HibernateUtil.commitTransaction();
	}

	public void updateUserLocale(String userId, String timeZoneId, String languageId) throws DotDataException, PortalException, SystemException, DotSecurityException {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserAPI userAPI = APILocator.getUserAPI();

		User user = uWebAPI.getLoggedInUser(request);
		boolean respectFrontend = uWebAPI.isLoggedToBackend(request);

		User toUpdate = userAPI.loadUserById(userId, user, respectFrontend);
		toUpdate.setTimeZoneId(timeZoneId);
		toUpdate.setLanguageId(languageId);
		userAPI.save(toUpdate, user, respectFrontend);

	}

	public void disableUserClicktracking(String userId, boolean disabled) throws PortalException, SystemException, DotSecurityException, DotDataException {

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		UserProxyAPI userProxyAPI = APILocator.getUserProxyAPI();

		User user = uWebAPI.getLoggedInUser(request);
		boolean respectFrontEndRoles = uWebAPI.isLoggedToBackend(request);

		HibernateUtil.startTransaction();
		UserProxy toUpdate = userProxyAPI.getUserProxy(userId, user, respectFrontEndRoles);
		toUpdate.setNoclicktracking(disabled);
		userProxyAPI.saveUserProxy(toUpdate, user, respectFrontEndRoles);
		HibernateUtil.commitTransaction();

	}


	// Helper classes. They implement the template method design pattern.
	@SuppressWarnings("unused")
	private abstract class UsersAndRolesListTemplate {

		protected String inode;
		protected int permissionType;
		protected String filter;
		protected int start;
		protected int limit;
		protected boolean hideSystemRoles;

		public abstract int getRoleCount(boolean hideSystemRoles) throws NoSuchRoleException, SystemException;
		public abstract List<Role> getRoles(boolean hideSystemRoles) throws NoSuchRoleException, SystemException;
		public abstract int getUserCount();
		public abstract List<User> getUsers(int start, int limit);

		public UsersAndRolesListTemplate(String inode, int permissionType, String filter, int start, int limit, boolean hideSystemRoles) {
			this.inode = inode;
			this.permissionType = permissionType;
			this.filter = filter;
			this.start = start;
			this.limit = limit;
			this.hideSystemRoles = hideSystemRoles;
		}

		public Map<String, Object> perform() {

			ArrayList<Map<String, String>> list = null;						// Keeps a list of roles and/or users
			Map<String, Object> results = new HashMap<String, Object>(2);	// Keeps the objects in container needed by the Ajax proxy (client-side)
			int totalItemCount = 0;											// Keeps the grand total of items
																			// (No. of roles + No. of users)
			List<Role> roles = null;
			List<User> users = new ArrayList<User>();
			int realRoleCount = 0;
			int realUserCount = 0;

			// Step 1. Retrieve roles, beginning from "start" parameter, up to a number of "limit" items, filtered by "filter" parameter.
			try {

				totalItemCount = getRoleCount(hideSystemRoles);
				if( start < totalItemCount ) {
					roles = getRoles(hideSystemRoles);
					realRoleCount = roles.size();
				}

			// Step 2. Retrieve users by matching name for the remaining of page, if needed.

				if( realRoleCount < limit || limit < 0 ) {

					// Since one page should be filled in with up to "limit" number of items, fill the remaining of page with users
					int realStart = start - totalItemCount < 0 ? 0 : start - totalItemCount;
					int realLimit = limit - realRoleCount < 0 ? -1 : limit - realRoleCount;
					users = getUsers(realStart, realLimit);
					realUserCount = users.size();
				}

				totalItemCount += getUserCount();

				// Step 3. Get the CMS Admins
				if(realRoleCount != 0 && (realRoleCount < limit || limit < 0))
				{
					Role CMSAdministratorRole = com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole();
					List<User> CMSAdministrators = com.dotmarketing.business.APILocator.getRoleAPI().findUsersForRole(CMSAdministratorRole.getId());
					String localFilter = filter.toLowerCase();
					for(User administrator : CMSAdministrators)
					{
						if(administrator.getFullName().toLowerCase().contains(filter))
						{
							if(!users.contains(administrator) && !administrator.getUserId().equals("system"))
							{
								users.add(administrator);
							}
						}
					}
				}

				try
				{
					SortTool sortTool = new SortTool();
					users = (List<User>) sortTool.sort(users.toArray(),"firstName");

				}
				catch(Exception ex)
				{
					Logger.info(UserAjax.class,"couldn't sort the users by first name" + ex.getMessage());
				}

				try
				{
					SortTool sortTool = new SortTool();
					roles = (List<Role>) sortTool.sort(roles.toArray(),"name");

				}
				catch(Exception ex)
				{
					Logger.info(UserAjax.class,"couldn't sort the roles by name" + ex.getMessage());
				}

				//Step 4. Assemble all of this information into an appropriate container to the view
				if( roles != null || users != null ) {

					int pageSize = realRoleCount + realUserCount;
					list = new ArrayList<Map<String, String>>(pageSize);

					if( roles != null ) {
						for(Role aRole : roles) {
							Map<String, String> aRecord = new HashMap<String, String>();
							aRecord.put("id", aRole.getId());
							aRecord.put("type", ROLE_TYPE_VALUE);
							aRecord.put("name", aRole.getName());
							aRecord.put("emailaddress", ROLE_TYPE_VALUE);
							list.add(aRecord);
						}
					}

					if( users != null ) {
						for( User aUser : users ) {
							Map<String, String> aRecord = new HashMap<String, String>();
							String fullName = aUser.getFullName();
							fullName = (UtilMethods.isSet(fullName) ? fullName : " ");
							String emailAddress = aUser.getEmailAddress();
							emailAddress = (UtilMethods.isSet(emailAddress) ? emailAddress : " ");
							aRecord.put("id", aUser.getUserId());
							aRecord.put("type", USER_TYPE_VALUE);
							aRecord.put("name", fullName);
							aRecord.put("emailaddress", emailAddress);
							list.add(aRecord);
						}
					}

				}
				// No roles nor users retrieved. So create an empty list.
				else {
					list = new ArrayList<Map<String, String>>(0);
				} //end if

				Collections.sort(list, new Comparator <Map<String, String>>(){

					public int compare(Map<String, String> record1,
							Map<String, String> record2) {

						return record1.get("name").compareTo(record2.get("name"));
					}



				});
			}
			catch(Exception ex) {
	    		Logger.warn(UsersAndRolesListTemplate.class, "::perform -> Could not process list of roles and users.");
				list = new ArrayList<Map<String, String>>(0);
			}

			results.put("data", list);
			results.put("total", totalItemCount);

			return results;

		}

	}
	// Helper classes. They implement the template method design pattern.
	@SuppressWarnings("unused")
	private abstract class RolesListTemplate {

		protected String inode;
		protected int permissionType;
		protected String filter;
		protected int start;
		protected int limit;
		protected boolean hideSystemRoles;

		public abstract int getRoleCount(boolean hideSystemRoles) throws NoSuchRoleException, SystemException;
		public abstract List<Role> getRoles(boolean hideSystemRoles) throws NoSuchRoleException, SystemException;


		public RolesListTemplate(String inode, int permissionType, String filter, int start, int limit, boolean hideSystemRoles) {
			this.inode = inode;
			this.permissionType = permissionType;
			this.filter = filter;
			this.start = start;
			this.limit = limit;
			this.hideSystemRoles = hideSystemRoles;
		}

		public Map<String, Object> perform() {

			ArrayList<Map<String, String>> list = null;						// Keeps a list of roles and/or users
			Map<String, Object> results = new HashMap<String, Object>(2);	// Keeps the objects in container needed by the Ajax proxy (client-side)
			int totalItemCount = 0;											// Keeps the grand total of items
																			// (No. of roles + No. of users)
			List<Role> roles = null;

			int realRoleCount = 0;


			// Step 1. Retrieve roles, beginning from "start" parameter, up to a number of "limit" items, filtered by "filter" parameter.
			try {

				totalItemCount = getRoleCount(hideSystemRoles);
				if( start < totalItemCount ) {
					roles = getRoles(hideSystemRoles);
					realRoleCount = roles.size();
				}

			// Step 2. Retrieve users by matching name for the remaining of page, if needed.

				if( realRoleCount < limit || limit < 0 ) {

					// Since one page should be filled in with up to "limit" number of items, fill the remaining of page with users
					int realStart = start - totalItemCount < 0 ? 0 : start - totalItemCount;
					int realLimit = limit - realRoleCount < 0 ? -1 : limit - realRoleCount;

				}


				// Step 3. Get the CMS Admins
				if(realRoleCount != 0 && (realRoleCount < limit || limit < 0))
				{
					Role CMSAdministratorRole = com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole();
					List<User> CMSAdministrators = com.dotmarketing.business.APILocator.getRoleAPI().findUsersForRole(CMSAdministratorRole.getId());
					String localFilter = filter.toLowerCase();
					for(User administrator : CMSAdministrators)
					{
						if(administrator.getFullName().toLowerCase().contains(filter))
						{

						}
					}
				}

				try
				{
					SortTool sortTool = new SortTool();


				}
				catch(Exception ex)
				{
					Logger.info(UserAjax.class,"couldn't sort the users by first name" + ex.getMessage());
				}

				try
				{
					SortTool sortTool = new SortTool();
					roles = (List<Role>) sortTool.sort(roles.toArray(),"name");

				}
				catch(Exception ex)
				{
					Logger.info(UserAjax.class,"couldn't sort the roles by name" + ex.getMessage());
				}

				//Step 4. Assemble all of this information into an appropriate container to the view
				if( roles != null  ) {

					int pageSize = realRoleCount ;
					list = new ArrayList<Map<String, String>>(pageSize);

					if( roles != null ) {
						for(Role aRole : roles) {
							Map<String, String> aRecord = new HashMap<String, String>();
							aRecord.put("id", aRole.getId());
							aRecord.put("type", ROLE_TYPE_VALUE);
							aRecord.put("name", aRole.getName());
							aRecord.put("emailaddress", ROLE_TYPE_VALUE);
							list.add(aRecord);
						}
					}


				}
				// No roles nor users retrieved. So create an empty list.
				else {
					list = new ArrayList<Map<String, String>>(0);
				} //end if

				Collections.sort(list, new Comparator <Map<String, String>>(){

					public int compare(Map<String, String> record1,
							Map<String, String> record2) {

						return record1.get("name").compareTo(record2.get("name"));
					}



				});
			}
			catch(Exception ex) {
	    		Logger.warn(UsersAndRolesListTemplate.class, "::perform -> Could not process list of roles");
				list = new ArrayList<Map<String, String>>(0);
			}

			results.put("data", list);
			results.put("total", totalItemCount);

			return results;
		}
	}


	private abstract class UsersListTemplate {

		protected String inode;
		protected int permissionType;
		protected String filter;
		protected int start;
		protected int limit;

		public abstract int getUserCount();
		public abstract List<User> getUsers();

		public UsersListTemplate(String inode, int permissionType, String filter, int start, int limit) {
			this.inode = inode;
			this.permissionType = permissionType;
			this.filter = filter;
			this.start = start;
			this.limit = limit;
		}

		public Map<String, Object> perform() {

			ArrayList<Map<String, String>> list = null;						// Keeps a list of users
			Map<String, Object> results = new HashMap<String, Object>(2);	// Keeps the objects in container needed by the Ajax proxy (client-side)
			int totalItemCount = 0;											// Keeps the grand total of items
																			// (No. of users)
			List<User> users = null;
			int realUserCount = 0;

			// Step 1. Retrieve users, beginning from "start" parameter, up to a number of "limit" items, filtered by "filter" parameter.
			try {

				totalItemCount = getUserCount();
				if( start < totalItemCount ) {
					users = getUsers();
					realUserCount = users.size();
				}

			// Step 2. Assemble all of this information into an appropriate container to the view
				if( users != null ) {

					int pageSize = realUserCount;
					list = new ArrayList<Map<String, String>>(pageSize);

					for( User aUser : users ) {
						Map<String, String> aRecord = new HashMap<String, String>();
						String fullName = aUser.getFullName();
						fullName = (UtilMethods.isSet(fullName) ? fullName : " ");
						String emailAddress = aUser.getEmailAddress();
						emailAddress = (UtilMethods.isSet(emailAddress) ? emailAddress : " ");
						aRecord.put("id", aUser.getUserId());
						aRecord.put("type", USER_TYPE_VALUE);
						aRecord.put("name", fullName);
						aRecord.put("emailaddress",emailAddress);
						list.add(aRecord);
					}
				}
				// No users retrieved. So create an empty list.
				else {
					list = new ArrayList<Map<String, String>>(0);
				} //end if
			}
			catch(Exception ex) {
	    		Logger.warn(UserAjax.class, "::processUsersList -> Could not process list of users.");
				list = new ArrayList<Map<String, String>>(0);
			}

			results.put("data", list);
			results.put("total", totalItemCount);

			return results;

		}

	}

}
