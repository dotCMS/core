package com.dotmarketing.business;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PasswordTrackerLocalManager;
import com.liferay.portal.ejb.PasswordTrackerLocalManagerFactory;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;
import com.liferay.portal.pwd.PwdToolkitUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;

/**
 * UserAPIImpl is an API intended to be a helper class for class to get User
 * entities from liferay's repository. Classes within the dotCMS should use this
 * API for user management. The UserAPIImpl does not do cache management. It
 * delegates this responsabilities to underlying classes.
 * 
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
	private final NotificationAPI notfAPI;
	private final BundleAPI bundleAPI;

	/**
	 * Creates an instance of the class.
	 */
	public UserAPIImpl() {
		uf = FactoryLocator.getUserFactory();
		perAPI = APILocator.getPermissionAPI();
		upAPI = APILocator.getUserProxyAPI();
		notfAPI = APILocator.getNotificationAPI();
		bundleAPI = APILocator.getBundleAPI();
	}

	@Override
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

	@Override
	public User loadUserById(String userId) throws DotDataException, DotSecurityException,com.dotmarketing.business.NoSuchUserException {
		if(!UtilMethods.isSet(userId)){
			throw new DotDataException("You must specifiy an userId to search for");
		}
		User u = uf.loadUserById(userId);
		if(!UtilMethods.isSet(u)){
			throw new com.dotmarketing.business.NoSuchUserException("No user found with passed in email");
		}
		return u;
	}

	@Override
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

	@Override
	public String encryptUserId(String userId) throws DotStateException{
		try{
			return UserManagerUtil.encryptUserId(userId);
		}catch (Exception e) {
			throw new DotStateException("Unable to encrypt userID : ", e);
		}
	}

	@Override
	public List<User> getUsersByName(String filter, int start,int limit, User user, boolean respectFrontEndRoles) throws DotDataException {
		return uf.getUsersByName(filter, start, limit);
	}

	@Override
	public User createUser(String userId, String email) throws DotDataException, DuplicateUserException {
		return uf.createUser(userId, email);
	}

	@Override
	public User getDefaultUser() throws DotDataException {
		try {
			return uf.loadDefaultUser();
		} catch (Exception e) {
			throw new DotDataException("getting default user user failed", e);
		}
	}

	@Override
	public User getSystemUser() throws DotDataException {
		User user = null;
		RoleAPI roleAPI = com.dotmarketing.business.APILocator.getRoleAPI();
		Role cmsAdminRole = roleAPI.loadCMSAdminRole();
		try {
			user = uf.loadUserById(SYSTEM_USER_ID);
		} catch (NoSuchUserException e) {
			user = createUser("system", "system@dotcmsfakeemail.org");
			user.setUserId(SYSTEM_USER_ID);
			user.setFirstName("system user");
			user.setLastName("system user");
			user.setCreateDate(new java.util.Date());
			user.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
			uf.saveUser(user);
		}
		if(!roleAPI.doesUserHaveRole(user, cmsAdminRole))
			roleAPI.addRoleToUser(cmsAdminRole.getId(), user);

		return user;
	}

	@Override
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

	@Override
	public boolean userExistsWithEmail(String email) throws DotDataException {
		return uf.userExistsWithEmail(email);
	}

	@Override
	public List<User> findAllUsers(int begin, int end) throws DotDataException {
		return uf.findAllUsers(begin, end);
	}

	@Override
	public List<User> findAllUsers() throws DotDataException {
		return uf.findAllUsers();
	}

	@Override
	public long getCountUsersByNameOrEmail(String filter) throws DotDataException {
		return uf.getCountUsersByNameOrEmail(filter);
	}

	@Override
	public List<User> getUsersByNameOrEmail(String filter, int page, int pageSize) throws DotDataException {
		return uf.getUsersByNameOrEmail(filter, page, pageSize);
	}

	@Override
	public List<String> getUsersIdsByCreationDate ( Date filterDate, int page, int pageSize ) throws DotDataException {
		return uf.getUsersIdsByCreationDate( filterDate, page, pageSize );
	}

	@Override
	public long getCountUsersByNameOrEmailOrUserID(String filter) throws DotDataException {
		return uf.getCountUsersByNameOrEmailOrUserID(filter);
	}

	@Override
	public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous) throws DotDataException {
		return uf.getCountUsersByNameOrEmailOrUserID(filter, includeAnonymous);
	}

	@Override
	public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous, boolean includeDefault)
			throws DotDataException {
		return uf.getCountUsersByNameOrEmailOrUserID(filter, includeAnonymous, includeDefault);
	}

	@Override
	public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize) throws DotDataException {
		return uf.getUsersByNameOrEmailOrUserID(filter, page, pageSize);
	}

	@Override
	public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize, boolean includeAnonymous) throws DotDataException {
		return uf.getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous);
	}

	@Override
	public List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
			int pageSize, boolean includeAnonymous, boolean includeDefault) throws DotDataException {
		return uf.getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous, includeDefault);
	}

	@Override
	public void save(User userToSave, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException,DuplicateUserException {
		String userId = userToSave.getUserId();
		if (userId == null) {
			throw new DotDataException("Can't save a user without a userId");
		}
		if (!perAPI.doesUserHavePermission(upAPI.getUserProxy(userToSave,
				APILocator.getUserAPI().getSystemUser(), false),
				PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)) {
			throw new DotSecurityException(
					"User doesn't have permission to save the user which is trying to be saved");
		}
		uf.saveUser(userToSave);
		PasswordTrackerLocalManager passwordTracker = PasswordTrackerLocalManagerFactory
				.getManager();
		try {
			if (passwordTracker.isPasswordRecyclingActive()
					&& StringUtils.isNotBlank(userToSave.getPassword())) {
				passwordTracker.trackPassword(userId, userToSave.getPassword());
			}
		} catch (PortalException | SystemException e) {
			SecurityLogger.logInfo(UserAPIImpl.class, "Password for user ["
					+ userId + "] could not be added for tracking.");
		}
		APILocator.getRoleAPI().getUserRole(userToSave);
	}

	@Override
	public void save(User userToSave, User user, boolean validatePassword,
			boolean respectFrontEndRoles) throws DotDataException,
	DotSecurityException, DuplicateUserException {
		String pwd = userToSave.getPassword();
		if (validatePassword) {
			PasswordTrackerLocalManager passwordTracker = PasswordTrackerLocalManagerFactory
					.getManager();
			try {
				if (!passwordTracker.isValidPassword(userToSave.getUserId(),
						pwd)) {
					// Get the first validation error and display it
					throw new DotDataException(passwordTracker
							.getValidationErrors().get(0).toString(),"User-Info-Save-Password-Failed");
				}
			} catch (PortalException | SystemException e) {
				throw new DotDataException(
						"An error occurred during the save process.");
			}

			// Use new password hash method
			try {
				userToSave.setPassword(PasswordFactoryProxy.generateHash(pwd));
			} catch (PasswordException e) {
				Logger.error(UserAPIImpl.class, "An error occurred generating the hashed password for userId: " + userToSave.getUserId(), e);
				throw new DotDataException("An error occurred generating the hashed password.");
			}
		}
		save(userToSave, user, respectFrontEndRoles);
	}

	@Override
	public void delete(User userToDelete, User user, boolean respectFrontEndRoles) throws DotDataException,	DotSecurityException {
		if (userToDelete.getUserId() == null) {
			throw new DotDataException("Can't delete a user without a userId");
		}
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(userToDelete,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		delete(userToDelete,user, user, respectFrontEndRoles);
	}

	@Override
	public void delete(User userToDelete, User replacementUser, User user, boolean respectFrontEndRoles) throws DotDataException,	DotSecurityException {
		if (!UtilMethods.isSet(userToDelete) || userToDelete.getUserId() == null) {
			throw new DotDataException("Can't delete a user without a userId");
		}
		if (!UtilMethods.isSet(replacementUser) || replacementUser.getUserId() == null) {
			throw new DotDataException("Can't delete a user without a replacement userId");
		}
		if (userToDelete.getUserId() == replacementUser.getUserId()) {
			throw new DotDataException("Can't delete a user without a replacement userId");
		}
		if (getAnonymousUser().getUserId() == userToDelete.getUserId()){
			throw new DotDataException("Anonymous user can not be deleted.");
		}
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(userToDelete,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}

		//replace the user reference in Inodes
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "Inodes");

		InodeFactory.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

		logDelete(DeletionStage.END, userToDelete, user, "Inodes");

		//replace the user references in contentlets
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "Contentlets");

		ContentletAPI conAPI = APILocator.getContentletAPI();
		conAPI.updateUserReferences(userToDelete,replacementUser.getUserId(), user);

		logDelete(DeletionStage.END, userToDelete, user, "Contentlets");

		//replace the user references in menulink
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "Menulinks");

		MenuLinkAPI menuAPI = APILocator.getMenuLinkAPI();
		menuAPI.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

		logDelete(DeletionStage.END, userToDelete, user, "Menulinks");

		//replace user references in htmlpages
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "HTMLPages");

		HTMLPageAPI pageAPI  = APILocator.getHTMLPageAPI();
		pageAPI.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

		logDelete(DeletionStage.END, userToDelete, user, "HTMLPages");

		//replace user references in file_assets
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "FileAssets");

		FileAPI fileAPI = APILocator.getFileAPI();
		fileAPI.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

		logDelete(DeletionStage.END, userToDelete, user, "FileAssets");

		//replace user references in containers
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "Containers");

		ContainerAPI contAPI = APILocator.getContainerAPI();
		contAPI.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

		logDelete(DeletionStage.END, userToDelete, user, "Containers");

		//replace user references in templates
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "Templates");

		TemplateAPI temAPI = APILocator.getTemplateAPI();
		temAPI.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

		logDelete(DeletionStage.END, userToDelete, user, "Templates");

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role userRole = roleAPI.loadRoleByKey(userToDelete.getUserId());
		Role replacementUserRole = roleAPI.loadRoleByKey(replacementUser.getUserId());

		//replace the user reference in workflows
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "Workflows");

		WorkflowAPI wofAPI = APILocator.getWorkflowAPI();
		wofAPI.updateUserReferences(userToDelete.getUserId(), userRole.getId(), replacementUser.getUserId(),replacementUserRole.getId());

		logDelete(DeletionStage.END, userToDelete, user, "Workflows");

		//replace the user reference in publishing bundles
		logDelete(DeletionStage.BEGINNING, userToDelete, user, "Publishing Bundles");

		bundleAPI.updateOwnerReferences(userToDelete.getUserId(), replacementUser.getUserId());

		logDelete(DeletionStage.END, userToDelete, user, "Publishing Bundles");

		//removing user roles
		perAPI.removePermissionsByRole(userRole.getId());
		roleAPI.removeAllRolesFromUser(userToDelete);

		/**
		 * Need to edit user name role system value 
		 * to allow delete the role
		 * */
		userRole.setSystem(false);
		userRole=roleAPI.save(userRole);
		roleAPI.delete(userRole);

		/*Delete role*/
		uf.delete(userToDelete);

	}

	/**
	 * 
	 * @author Daniel Silva
	 * @version 3.7
	 * @since Jul 25, 2016
	 *
	 */
	private enum DeletionStage {
		BEGINNING,
		END
	}

	/**
	 * 
	 * @param stage
	 * @param userToDelete
	 * @param user
	 * @param referenceType
	 */
	private void logDelete(DeletionStage stage, User userToDelete, User user, String referenceType) {

		String userToDeleteStr = userToDelete.getUserId() + "/" + userToDelete.getFullName();

		try {
			String msg = stage==DeletionStage.BEGINNING
				?
				MessageFormat.format(LanguageUtil.get(user,
				"com.dotmarketing.business.UserAPI.delete.beginning"), userToDeleteStr, referenceType)
				:
				MessageFormat.format(LanguageUtil.get(user,
					"com.dotmarketing.business.UserAPI.delete.end"), userToDeleteStr, referenceType);

			Logger.info(this, msg);

			notfAPI.info(msg, user.getUserId());

		} catch (LanguageException e) {
			Logger.error(this, "Error logging info of Delete user operation. User: " + userToDeleteStr);
		}
	}

	@Override
	public void saveAddress(User user, Address ad, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, currentUser, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		uf.saveAddress(user, ad);
	}

	@Override
	public Address loadAddressById(String addressId, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
		Address ad = uf.loadAddressById(addressId);
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(ad.getUserId(),APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, currentUser, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		return ad;
	}

	@Override
	public void deleteAddress(Address ad, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(ad.getUserId(),APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, currentUser, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		uf.deleteAddress(ad);
	}

	@Override
	public List<Address> loadUserAddresses(User user, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(upAPI.getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, currentUser, respectFrontEndRoles)){
			throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
		}
		return uf.loadUserAddresses(user);
	}

	@Override
	public boolean isCMSAdmin(User user) throws DotDataException {
		RoleAPI roleAPI = APILocator.getRoleAPI();
		return roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole());
	}

	@Override
	public void updatePassword(User user, String newpass, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotInvalidPasswordException, DotSecurityException {
		if(!PwdToolkitUtil.validate(newpass)) {
			throw new DotInvalidPasswordException("Invalid password");
		}

		// Use new password hash method
		try {
			user.setPassword(PasswordFactoryProxy.generateHash(newpass));
		} catch (PasswordException e) {
			Logger.error(UserAPIImpl.class, "An error occurred generating the hashed password for userId: " + user.getUserId(), e);
			throw new DotDataException("An error occurred generating the hashed password.");
		}

		user.setIcqId("");
		user.setPasswordReset(GetterUtil.getBoolean(
				PropsUtil.get(PropsUtil.PASSWORDS_CHANGE_ON_FIRST_USE)));
		save(user, currentUser, respectFrontEndRoles);

	}

	@Override
	public void markToDelete(User userToDelete) throws DotDataException {
		userToDelete.setDeleteInProgress(true);
		userToDelete.setDeleteDate(Calendar.getInstance().getTime());
		uf.saveUser(userToDelete);
	}

	@Override
	public List<User> getUnDeletedUsers() throws DotDataException {
		return uf.getUnDeletedUsers();
	}

}
