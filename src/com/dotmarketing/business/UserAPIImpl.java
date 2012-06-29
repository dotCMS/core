package com.dotmarketing.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.startup.runalways.Task00003CreateSystemRoles;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

/**
 * UserAPIImpl is an API intended to be a helper class for class to get User entities from liferay's repository.  Classes within the dotCMS
 * should use this API for user management.  The UserAPIImpl does not do cache management. It delegates this responsabilities
 * to underlying classes.
 * @author David Torres
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.9
 * @since 1.6
 */
public class UserAPIImpl implements UserAPI {

	private UserFactory uf;
	private PermissionAPI perAPI;
	private UserProxyAPI upAPI;

	public UserAPIImpl() {
		uf = FactoryLocator.getUserFactory();
		perAPI = APILocator.getPermissionAPI();
		upAPI = APILocator.getUserProxyAPI();
	}

	public User loadUserById(String userId, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException,com.dotmarketing.business.NoSuchUserException {
		if(!UtilMethods.isSet(userId)){
			throw new DotDataException("You must specifiy an userId to search for");
		}
		User u = uf.loadUserById(userId);
		if(!UtilMethods.isSet(u)){
			throw new com.dotmarketing.business.NoSuchUserException("No user found with passed in email");
		}
		if(perAPI.doesUserHavePermission(upAPI.getUserProxy(u,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
			return u;
		}else{
			throw new DotSecurityException("The User being passed in doesn't have permission to requested User");
		}
	}

	public User loadByUserByEmail(String email, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, com.dotmarketing.business.NoSuchUserException {
		if(!UtilMethods.isSet(email)){
			throw new DotDataException("You must specifiy an email to search for");
		}
		User u = uf.loadByUserByEmail(email);
		if(!UtilMethods.isSet(u)){
			throw new com.dotmarketing.business.NoSuchUserException("No user found with passed in email");
		}
		if(perAPI.doesUserHavePermission(upAPI.getUserProxy(u,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
			return u;
		}else{
			throw new DotSecurityException("The User being passed in doesn't have permission to requested User");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.dotmarketing.business.UserAPI#encryptUserId(java.lang.String)
	 */
	public String encryptUserId(String userId) throws DotStateException{
		try{
			return UserManagerUtil.encryptUserId(userId);
		}catch (Exception e) {
			throw new DotStateException("Unable to encrypt userID : ", e);
		}
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.UserAPI#getUsersByName(java.lang.String, int, int)
	 */
    public List<User> getUsersByName(String filter, int start,int limit, User user, boolean respectFrontEndRoles) throws DotDataException {
    	return uf.getUsersByName(filter, start, limit);
    }

	public User createUser(String userId, String email) throws DotDataException, DuplicateUserException {
		return uf.createUser(userId, email);
	}

	public User getDefaultUser() throws DotDataException {
		try {
			return uf.loadDefaultUser();
		} catch (Exception e) {
			throw new DotDataException("getting default user user failed", e);
		}
	}

	public User getSystemUser() throws DotDataException {
		User user = null;
		RoleAPI roleAPI = com.dotmarketing.business.APILocator.getRoleAPI();
		//Role cmsAdminRole = roleAPI.loadRoleByKey(Config.getStringProperty("CMS_ADMINISTRATOR_ROLE"));
		Role cmsAdminRole = roleAPI.loadCMSAdminRole();
		try {
			user = uf.loadUserById("system");
		} catch (NoSuchUserException e) {
			new Task00003CreateSystemRoles().executeUpgrade();
			user = uf.loadUserById("system");
		}
		if(!roleAPI.doesUserHaveRole(user, cmsAdminRole))
			roleAPI.addRoleToUser(cmsAdminRole.getId(), user);

		return user;
	}

	public User getAnonymousUser() throws DotDataException {
		User user = null;
		try {
				user = uf.loadUserById("anonymous");
		} catch (DotDataException e) {
			user = createUser("anonymous", "anonymous@dotcmsfakeemail.org");
			user.setUserId("anonymous");
			user.setFirstName("anonymous user");
			user.setCreateDate(new java.util.Date());
			user.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
			uf.saveUser(user);
			com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(com.dotmarketing.business.APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CMS_ANONYMOUS_ROLE")).getId(), user);
		} catch (NoSuchUserException e) {
			user = createUser("anonymous", "anonymous@dotcmsfakeemail.org");
			user.setUserId("anonymous");
			user.setFirstName("anonymous user");
			user.setCreateDate(new java.util.Date());
			user.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
			uf.saveUser(user);
			com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), user);
		}
		return user;
	}

	public boolean userExistsWithEmail(String email) throws DotDataException {
		return uf.userExistsWithEmail(email);
	}

	public List<User> findAllUsers(int begin, int end) throws DotDataException {
		return uf.findAllUsers(begin, end);
	}

	public List<User> findAllUsers() throws DotDataException {
		return uf.findAllUsers();
	}

	public long getCountUsersByNameOrEmail(String filter) throws DotDataException {
		return uf.getCountUsersByNameOrEmail(filter);
	}

	public List<User> getUsersByNameOrEmail(String filter, int page, int pageSize) throws DotDataException {
		return uf.getUsersByNameOrEmail(filter, page, pageSize);
	}

	@SuppressWarnings("deprecation")
	public Map<String, Object> getUsersAnRolesByName(String filter, int start, int limit) throws DotDataException {
		return uf.getUsersAnRolesByName(filter, start, limit);
	}

	public void save(User userToSave, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException,DuplicateUserException {
		if (userToSave.getUserId() == null) {
			throw new DotDataException("Can't save a user without a userId");
		}
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(userToSave,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to save the user which is trying to be saved");
		}
		uf.saveUser(userToSave);
		APILocator.getRoleAPI().getUserRole(userToSave);
	}

	public void delete(User userToDelete, User user, boolean respectFrontEndRoles) throws DotDataException,	DotSecurityException {
		if (userToDelete.getUserId() == null) {
			throw new DotDataException("Can't delete a user without a userId");
		}
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(userToDelete,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		RoleAPI roleAPI = APILocator.getRoleAPI();
		roleAPI.removeAllRolesFromUser(userToDelete);
		uf.delete(userToDelete);
	}

	public void saveAddress(User user, Address ad, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, currentUser, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		uf.saveAddress(user, ad);
	}

	public Address loadAddressById(String addressId, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
		Address ad = uf.loadAddressById(addressId);
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(ad.getUserId(),APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, currentUser, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		return ad;
	}

	public void deleteAddress(Address ad, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(ad.getUserId(),APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, currentUser, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		uf.deleteAddress(ad);
	}

	public List<Address> loadUserAddresses(User user, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, currentUser, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		return uf.loadUserAddresses(user);
	}

	public boolean isCMSAdmin(User user) throws DotDataException {
		RoleAPI roleAPI = APILocator.getRoleAPI();
		return roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole());
	}


}
