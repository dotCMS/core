package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchRoleException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

/**
 * PermissionAPI is an API intended to be a helper class for class to get Permissions.  Classes within the dotCMS
 * should use this API for permissions.  The PermissionAPI will goto the PermissionCache for you so you can use the PermissionAPI
 * trusting that you will not be continually hitting the database.
 *
 * @author David Torres (2009)
 */
public class PermissionBitAPIImpl implements PermissionAPI {


	PermissionFactory permissionFactory;

	public PermissionBitAPIImpl() {
	}

	/**
	 * Builds a PermissionAPI initialized with its dependent objects.
	 * @return PermissionFactory service reference
	 */
	public PermissionBitAPIImpl(PermissionFactory serviceRef) {
		setPermissionFactory(serviceRef);
	}


	/**
	 * Gets the Permission Factory service reference used in this API .
	 * @return PermissionFactory service reference
	 */
	public PermissionFactory getPermissionFactory() {
		return permissionFactory;
	}

	/**
	 * Sets a Permission Factory for this API
	 * @param PermissionFactory service reference
	 * @return Nothing
	 */
	public void setPermissionFactory(PermissionFactory permissionFactory) {
		this.permissionFactory = permissionFactory;
	}

	/**
	 *
	 * @param permissions
	 * @param permissionTypeToLoadFor
	 * @return List of type Role for a particular permission.  ie.. All roles with read permission from the collection of permissions passed in
	 */
	private List<Role> loadRolesForPermission(List<Permission> permissions, int permissionTypeToLoadFor) throws NoSuchRoleException {
		ArrayList<Role> roles = new ArrayList<Role>();
		for (Permission permission : permissions) {
			if(permission.matchesPermission(permissionTypeToLoadFor))	{
				try {
					Role r = APILocator.getRoleAPI().loadRoleById(permission.getRoleId());
					if(r != null){
						roles.add(r);
					}else{
						//Preventing failures on deleted roles
						Logger.warn(this, "An orphan permission object found, the referenced role does not exist in the system");
					}
				} catch (DotDataException e) {
					//Preventing failures on deleted roles
					Logger.warn(this, "An orphan permission object found, the referenced role does not exist in the system", e);
				}
			}
		}
		return roles;
	}

	/**
	 *
	 * @param permissions
	 * @param permissionTypeToLoadFor
	 * @return List of type Role for a particular permission.  ie.. All roles with read permission from the collection of permissions passed in
	 */
	private List<Role> loadRolesForPermission(List<Permission> permissions, int permissionTypeToLoadFor, String roleNameFilter) throws NoSuchRoleException {
		SortedSet<Role> roles = new TreeSet<Role>();

		boolean isRoleNameFilterValid = UtilMethods.isSet(roleNameFilter);
		for (Permission permission : permissions) {
			if(permission.matchesPermission(permissionTypeToLoadFor))	{
				try {
					Role aRole = APILocator.getRoleAPI().loadRoleById( permission.getRoleId() );
					if( !isRoleNameFilterValid ) {
						roles.add(aRole);
					}
					else if( aRole.getName().indexOf(roleNameFilter) > -1 ) {
						roles.add(aRole);
					}
				} catch (Exception e) {
					Logger.warn(this, e.toString());
				}
			}
		}
		return new ArrayList<Role>(roles);
	}


	/**
	 * This is not intended to be used to check permission because it doesn't check for cms administrator privileges
	 * @param user
	 * @param permissions
	 * @param requiredPermissionType
	 * @return If the user has the required permission for the collection of permissions passed in
	 */
	private boolean doRolesHavePermission(List<String> userRoleIDs, List<Permission> permissions, int requiredPermissionType){
		for (Permission permission : permissions) {
			if(permission.matchesPermission(requiredPermissionType)
					&& userRoleIDs.contains(permission.getRoleId())){
				return true;
			}
		}
		return false;
	}


	public boolean doesRoleHavePermission(Permissionable permissionable, int permissionType, Role role, boolean respectFrontendRoles) throws DotDataException {
		return doesRoleHavePermission(permissionable, permissionType, role);
	}

	public boolean doesRoleHavePermission(Permissionable permissionable, int permissionType, Role role) throws DotDataException {

		// if we have bad data
		if (permissionable != null && (!InodeUtils.isSet(permissionable.getPermissionId())) || (role == null)) {
			return false;
		}

		List<Permission> perms =  getPermissions(permissionable, true);
		for(Permission p : perms){
			if(p.matchesPermission(permissionType) && p.getRoleId().equals(role.getId())){
				return true;
			}
		}
		return false;
	}

	private boolean checkRelatedPermissions (List<RelatedPermissionableGroup> list, Role role) throws DotDataException {
		if(list == null) return true;
		for (RelatedPermissionableGroup group: list) {
			boolean hasPermissionForGroup = group.isRequiresAll()?true:false;
			for(Permissionable related: group.getRelatedPermissionables()) {
				boolean hasPermissionOverPermissionable = doesRoleHavePermission(related, group.getRelatedRequiredPermission(), role);
				if(group.isRequiresAll() && !hasPermissionOverPermissionable) {
					hasPermissionForGroup = false;
					break;
				} else if (!group.isRequiresAll() && hasPermissionOverPermissionable) {
					hasPermissionForGroup = true;
					break;
				}
			}
			if(!hasPermissionForGroup)
				return false;
		}
		return true;
	}

	private boolean checkRelatedPermissions (List<RelatedPermissionableGroup> list, User user) throws DotDataException {
		List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		for (Role role : roles) {
			if(checkRelatedPermissions(list, role))
				return true;
		}
		return false;
	}



	public List<Permission> getInheritablePermissionsRecurse(Permissionable permissionable) throws DotDataException {
		List<Permission> fPerms = getInheritablePermissions(permissionable, false);
		Permissionable parent = permissionable.getParentPermissionable();
		while(parent != null){
			fPerms.addAll(getInheritablePermissions(parent, false));
			parent = parent.getParentPermissionable();
		}

		return fPerms;


	}






	public boolean doesUserHaveInheriablePermissions(Permissionable parentPermissionable, String type, int requiredPermissions, User user) throws DotDataException {

		List<Permission> fPerms = getInheritablePermissionsRecurse(parentPermissionable);
		String asset = null;
		boolean haveType=false;
		for(Permission p : fPerms){

			// stop recursing if we have already found permissions
			// for the type of asset and the user did not have them.
			if(haveType && !asset.equals(p.getInode())){
				return false;
			}


			if(type.equals(p.getType())){
				if(p.getPermission() == requiredPermissions){
					if(com.dotmarketing.business.APILocator.getRoleAPI().
							doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadRoleById(p.getRoleId()))){
						return true;
					}
				}
				haveType = true;
			}

			asset = p.getInode();
		}
		return false;
	}




	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#doesUserHavePermission(com.dotmarketing.beans.Inode, int, com.liferay.portal.model.User)
	 */
	public boolean doesUserHavePermission(Permissionable permissionable, int permissionType, User user) throws DotDataException {
		return doesUserHavePermission(permissionable, permissionType, user, true);
	}


	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#doesUserHavePermission(com.dotmarketing.beans.Inode, int, com.liferay.portal.model.User, boolean)
	 */
	public boolean doesUserHavePermission(Permissionable permissionable, int permissionType, User user, boolean respectFrontendRoles) throws DotDataException {

		// if we have bad data
		if ((permissionable == null) || (!InodeUtils.isSet(permissionable.getPermissionId()))) {
			if(permissionable != null){
				Logger.debug(this.getClass(), "Trying to get permissions on null inode of type :" + permissionable.getPermissionType()) ;
				Logger.debug(this.getClass(), "Trying to get permissions on null inode of class :" + permissionable.getClass()) ;
			}
			return false;
		}

		if(user!=null && user.getUserId().equals(APILocator.getUserAPI().getSystemUser().getUserId())){
			return true;
		}

		// http://jira.dotmarketing.net/browse/DOTCMS-6943
		// everybody should be able to use file structures
        if (permissionable instanceof Structure
                && (permissionType==PERMISSION_WRITE || permissionType==PERMISSION_PUBLISH)
                && ((Structure)permissionable).getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET)
            return true;

		Role adminRole;
		Role anonRole;
		Role frontEndUserRole;
		Role cmsOwnerRole;
		try {
			adminRole = APILocator.getRoleAPI().loadCMSAdminRole();
			anonRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
			frontEndUserRole = APILocator.getRoleAPI().loadLoggedinSiteRole();
			cmsOwnerRole = APILocator.getRoleAPI().loadCMSOwnerRole();
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}

		if(user != null && APILocator.getRoleAPI().doesUserHaveRole(user, adminRole))
			return true;

		List<RelatedPermissionableGroup> permissionDependencies = permissionable.permissionDependencies(permissionType);


		List<Permission> perms =  getPermissions(permissionable, true);

		for(Permission p : perms){
			if(p.matchesPermission(permissionType)){
				if(respectFrontendRoles){
					// if we are anonymous
					if(p.getRoleId().equals(anonRole.getId())){
						return true;
						//if logged in site user has permission
					}else if(user != null && p.getRoleId().equals(frontEndUserRole.getId())){
						return true;
					}
				}
				// if owner and owner has required permission return true
				try {
					if(p.getRoleId().equals(cmsOwnerRole.getId()) && user != null &&
							permissionable.getOwner() != null && permissionable.getOwner().equals(user.getUserId()) &&
							checkRelatedPermissions(permissionDependencies, user)){
						return true;
					}
				} catch (DotDataException e1) {
					Logger.error(this, e1.getMessage(), e1);
					throw new DotRuntimeException(e1.getMessage(), e1);
				}
			}
		}

		// at this point, there is no anon, logged in site user and the owner do not have permissions
		//If we don't have a user, return false
		if(user ==null){
			return false;
		}

		List<Role> roles;
		try {
			roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}
		List<String> userRoleIds= new ArrayList<String>();
		for (Role role : roles) {
			try{
				String roleID = role.getId();
				userRoleIds.add(roleID);
				if(roleID.equals(adminRole.getId())){
					// if CMS Admin return true
					return true;
				}
			}catch (Exception e) {
				Logger.error(this, "Roleid should be a long : ",e);
			}
		}

		return doRolesHavePermission(userRoleIds,getPermissions(permissionable, true),permissionType);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#removePermissionsOnInode(com.dotmarketing.beans.Inode)
	 */
	public void removePermissions(Permissionable permissionable) throws DotDataException {

		permissionFactory.removePermissions(permissionable);

	}

	//This method can be used later
	public void setDefaultCMSAnonymousPermissions(Permissionable permissionable) throws DotDataException{
		Role cmsAnonymousRole;
		try {
			cmsAnonymousRole = APILocator.getRoleAPI().loadRoleByKey(Config
					.getStringProperty("CMS_ANONYMOUS_ROLE"));
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}


		Permission cmsAnonymousPermission = new Permission();
		cmsAnonymousPermission.setRoleId(cmsAnonymousRole.getId());
		cmsAnonymousPermission.setPermission(PERMISSION_READ);
		cmsAnonymousPermission.setInode(permissionable.getPermissionId());
		try {
			permissionFactory.savePermission(cmsAnonymousPermission, permissionable);
		} catch (DataAccessException e) {
			Logger.error(getClass(), "setDefaultCMSAnonymousPermissions failed persisting permission for permissionable: " + permissionable.getPermissionId(), e);
			throw e;
		}
		if(permissionable.isParentPermissionable()) {

			//Default hosts permissions
			Permission p = new Permission(Host.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

			//Default sub-folders permissions
			p = new Permission(Folder.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

			//Default files permissions
			p = new Permission(File.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

			//Default links permissions
			p = new Permission(Link.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

			//Default pages permissions
			p = new Permission(HTMLPage.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

			//Default content permissions
			p = new Permission(Contentlet.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

		}
	}


	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#setDefaultCMSAdminPermissions(com.dotmarketing.beans.Inode)
	 */
	public void setDefaultCMSAdminPermissions (Permissionable permissionable) throws DotDataException {
		Role cmsAdminRole;
		try {
			cmsAdminRole = APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CMS_ADMINISTRATOR_ROLE"));
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}


		Permission cmsAdminPermission = new Permission();
		cmsAdminPermission.setRoleId(cmsAdminRole.getId());
		cmsAdminPermission.setPermission(permissionFactory.maskOfAllPermissions());
		cmsAdminPermission.setInode(permissionable.getPermissionId());
		cmsAdminPermission.setBitPermission(true);
		try {
			permissionFactory.savePermission(cmsAdminPermission, permissionable);
		} catch (DataAccessException e) {
			Logger.error(getClass(), "setDefaultCMSAdminPermissions failed persisting permission for permissionable: " + permissionable.getPermissionId(), e);
			throw e;
		}

	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#copyPermissions(com.dotmarketing.beans.Inode, com.dotmarketing.beans.Inode)
	 */
	public void copyPermissions(Permissionable from, Permissionable to) throws DotDataException {

		permissionFactory.removePermissions(to);

		List<Permission> fromPerms = getPermissions(from, true, true);
		if(from.isParentPermissionable() && to.isParentPermissionable())
			fromPerms.addAll(permissionFactory.getInheritablePermissions(from));
		for (Permission permission : fromPerms) {
			Permission newPerm = new Permission(permission.getType(), to.getPermissionId(), permission.getRoleId(), permission.getPermission(), true);
			try {
				permissionFactory.savePermission(newPerm, to);
			} catch (DataAccessException e) {
				Logger.error(getClass(), "copyPermissions failed on saving new permission to target permissionable: " + to.getPermissionId(), e);
				throw e;
			}
		}

	}

	/**
	 * This method uses the permission cache to return a permission.
	 * @param id of permission to find
	 * @return  Permission
	 */
	public  Permission find(String id){
		return permissionFactory.getPermission(String.valueOf(id));
	}


	public List<Permission> getPermissions(Permissionable permissionable) throws DotDataException {
		return permissionFactory.getPermissions(permissionable, false);
	}

	public List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
		return permissionFactory.getPermissions(permissionable, bitPermissions);
	}

	public List<Permission> getPermissions(Permissionable permissionable,
			boolean bitPermissions, boolean onlyIndividualPermissions) throws DotDataException {
		return permissionFactory.getPermissions(permissionable, bitPermissions, onlyIndividualPermissions);
	}


	public List<Permission> getPermissions(Permissionable permissionable,
			boolean bitPermissions, boolean onlyIndividualPermissions, boolean forceLoadFromDB) throws DotDataException {
		return permissionFactory.getPermissions(permissionable, bitPermissions, onlyIndividualPermissions, forceLoadFromDB);
	}

	/**
	 * @param Permission to save
	 * Saves passed in permission
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void save(Permission permission, Permissionable permissionable, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if(!doesUserHavePermission(permissionable, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user))
			throw new DotSecurityException("User id: " + user.getUserId() + " does not have permission to alter permissions on asset " + permissionable.getPermissionId());

		RoleAPI roleAPI = APILocator.getRoleAPI();

		Role role = roleAPI.loadRoleById(permission.getRoleId());
		if(!role.isEditPermissions())
			throw new DotSecurityException("Role id " + role.getId() + " is locked for editing permissions");
		try {
			List<Permission> currentIndividualPermissions = getPermissions(permissionable, true, true);
			if(currentIndividualPermissions.size() == 0) {
				//We need to ensure locked roles get saved as permissions too
				List<Permission> currentInheritedPermissions = getPermissions(permissionable, true);
				for(Permission currentPerm : currentInheritedPermissions) {
					Role permRole = roleAPI.loadRoleById(currentPerm.getRoleId());
					if(permRole.isLocked()) {
						Permission lockedPerm = new Permission(permissionable.getPermissionId(), currentPerm.getRoleId(), currentPerm.getPermission());
						permissionFactory.savePermission(lockedPerm, permissionable);
					}
				}

			}

			Permission p = permissionFactory.savePermission(permission, permissionable);
			if(p != null)
				permission.setId(p.getId());

		} catch (DataAccessException e) {
			Logger.error(getClass(), "save failed on daving the permission: " + permission.toString(), e);
			throw e;
		}

	}

	public void assignPermissions(List<Permission> permissions, Permissionable permissionable, User user, boolean respectFrontendRoles)
		throws DotDataException, DotSecurityException {

		if(!doesUserHavePermission(permissionable, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user))
			throw new DotSecurityException("User id: " + user.getUserId() + " does not have permission to alter permissions on asset " + permissionable.getPermissionId());

		if(permissions == null || permissions.size() == 0) {
			throw new DotDataException("This method is not intented to remove all permissionable permissions, instead use deletePermissions");
		}

		RoleAPI roleAPI = APILocator.getRoleAPI();

		List<Permission> currentPermissions = permissionFactory.getPermissions(permissionable, true);
		List<String> rolesIncluded = new ArrayList<String>();
		List<Permission> includingLockedRolePermissions = new ArrayList<Permission>();

		for(Permission current : currentPermissions) {
			Role role = roleAPI.loadRoleById(current.getRoleId());
			if(!role.isEditPermissions()) {
				Permission perm = new Permission(permissionable.getPermissionId(), current.getRoleId(), current.getPermission());
				includingLockedRolePermissions.add(perm);
				rolesIncluded.add(role.getId());
			}
		}

		for(Permission p : permissions) {
			Role role = roleAPI.loadRoleById(p.getRoleId());
			if(!role.isEditPermissions()) {
				continue;
			}
			if(!p.getInode().equals(permissionable.getPermissionId()))
				throw new DotDataException("Can't assign permissions to a diferent permissionable");
			rolesIncluded.add(role.getId());
			includingLockedRolePermissions.add(p);
		}

		for(Permission current : currentPermissions) {
			Role role = roleAPI.loadRoleById(current.getRoleId());
			if(role.isLocked() && !rolesIncluded.contains(role.getId())) {
				current.setInode(permissionable.getPermissionId());
				includingLockedRolePermissions.add(current);
				rolesIncluded.add(role.getId());
			}
		}


		permissionFactory.assignPermissions(includingLockedRolePermissions, permissionable);

		AdminLogger.log(PermissionBitAPIImpl.class, "assign Permissions Action", "Assigning permissions to :"+permissionable.getPermissionId(),user);
	}

	public Set<User> getReadUsers(Permissionable permissionable) throws DotDataException {
		Set<Role> roles = getReadRoles(permissionable);
		Set<User> users = new HashSet<User>();
		for (Role role : roles) {
			try {
				users.addAll(APILocator.getRoleAPI().findUsersForRole(role));
			} catch (NoSuchUserException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			} catch (DotDataException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			}
		}
		return users;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#getReadRoles(com.dotmarketing.beans.Inode)
	 */
	public Set<Role> getReadRoles(Permissionable permissionable) throws DotDataException {
		Set<Role> readPermissions = new HashSet<Role>();
		List<Permission> permissions = getPermissions(permissionable);
		List<Role> roles = new ArrayList<Role>();
		try{
			roles = loadRolesForPermission(permissions, PermissionAPI.PERMISSION_READ);
		}catch (NoSuchRoleException nsre) {
			Logger.error(this, "Error loading roles: ", nsre);
		}
		readPermissions.addAll(roles);
		return readPermissions;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#getPublishRoles(com.dotmarketing.beans.Inode)
	 */
	public Set<Role> getPublishRoles(Permissionable permissionable) throws DotDataException {
		Set<Role> publishPermissions = new HashSet<Role>();
		List<Permission> permissions = getPermissions(permissionable);
		List<Role> roles = new ArrayList<Role>();
		try{
			roles = loadRolesForPermission(permissions, PermissionAPI.PERMISSION_PUBLISH);
		}catch (NoSuchRoleException nsre) {
			Logger.error(this, "Error loading roles: ", nsre);
		}
		publishPermissions.addAll(roles);
		return publishPermissions;
	}

	public Set<User> getWriteUsers(Permissionable permissionable) throws DotDataException {
		Set<Role> roles = getWriteRoles(permissionable);
		Set<User> users = new HashSet<User>();
		for (Role role : roles) {
			try {
				List<User> roleUsers = APILocator.getRoleAPI().findUsersForRole(role);
				if(roleUsers != null)
					users.addAll(roleUsers);
			} catch (NoSuchUserException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			} catch (DotDataException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			}
		}
		return users;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#getWriteRoles(com.dotmarketing.beans.Inode)
	 */
	public Set<Role> getWriteRoles(Permissionable permissionable) throws DotDataException {
		Set<Role> writePermissions = new HashSet<Role>();
		List<Permission> permissions = getPermissions(permissionable);
		List<Role> roles = new ArrayList<Role>();
		try{
			roles = loadRolesForPermission(permissions, PermissionAPI.PERMISSION_WRITE);
		}catch (NoSuchRoleException nsre) {
			Logger.error(this, "Error loading roles: ", nsre);
		}
		writePermissions.addAll(roles);
		return writePermissions;
	}

	public Set<Role> getRolesWithPermission(Permissionable permissionable, int permission) throws DotDataException {

		Set<Role> roles = new HashSet<Role>();
		List<Permission> permissions = getPermissions(permissionable);
		try{
			roles.addAll(loadRolesForPermission(permissions, permission));
		}catch (NoSuchRoleException nsre) {
			Logger.error(this, "Error loading roles: ", nsre);
		}
		return roles;

	}

	public Set<User> getUsersWithPermission(Permissionable permissionable, int permission) throws DotDataException {
		Set<Role> roles = getRolesWithPermission(permissionable, permission);
		Set<User> users = new HashSet<User>();
		for (Role role : roles) {
			try {
				users.addAll(APILocator.getRoleAPI().findUsersForRole(role));
			} catch (NoSuchUserException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			} catch (DotDataException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitAPIImpl.class,e.getMessage(),e);
			}
		}
		return users;
	}



	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#doesUserOwn(com.dotmarketing.beans.Inode, com.liferay.portal.model.User)
	 */
	public boolean doesUserOwn(Inode inode, User user) throws DotDataException{
		if(user == null || inode == null){
			return false;
		}else if(inode instanceof WebAsset){
			return APILocator.getIdentifierAPI().find(inode).equals(user.getUserId());
		}else{
			return inode.getOwner() != null && inode.getOwner().equals(user.getUserId());
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#mapAllPermissions()
	 */
	// PERMISSION MAP METHODS!!!
	public void mapAllPermissions() throws DotDataException {

		Logger.debug(PermissionBitAPIImpl.class, "\n\nGoing to map all Permissions!!!!");

		if (Config.CONTEXT == null) {
			return;
		}

		java.util.List<HTMLPage> list = (java.util.List<HTMLPage>)HTMLPageFactory.getLiveHTMLPages();

		for(HTMLPage htmlPage : list) {
			permissionFactory.getPermissions(htmlPage, true);
		}

		Logger.debug(PermissionBitAPIImpl.class, "\n\nFinished mapping all Permissions!!!!");
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#getPermissionIdsFromRoles()
	 */
	public List<Integer> getPermissionIdsFromRoles(Permissionable permissionable, Role[] roles, User user)throws DotDataException {
		Set<Integer> permissions = new TreeSet<Integer>();
		List<Permission> assetsPermissions;

		for (int i = 0; i < roles.length; i++) {
			/*
			 * If the user is a CMS Admin Return full priv
			 */
			try {
				if (roles[i].getId().equals(APILocator.getRoleAPI().loadCMSAdminRole().getId())) {
					Collection<Integer> list = getPermissionTypes().values();
					for(int permissionType : list) {
						permissions.add(permissionType);
					}
					return new ArrayList<Integer>(permissions);
				}
			} catch (Exception e) {

			}
		}

		assetsPermissions = getPermissions(permissionable);

		//Adding asset permissions
		for (int i = 0; i < roles.length; i++) {
			Iterator<Permission> it = assetsPermissions.iterator();
			while (it.hasNext()) {
				Permission perm = it.next();

				if (perm.getRoleId().equals(roles[i].getId())) {
					if(!permissions.contains(perm.getPermission())){
						permissions.add(perm.getPermission());
					}
				}
			}
		}

		if(user == null){
			return new ArrayList<Integer>(permissions);
		}

		//add owners permission
		String identOwner = null;
		if(permissionable instanceof WebAsset){
			identOwner = permissionable.getOwner();
		}
		if ((identOwner != null && identOwner.equals(user.getUserId())) || (permissionable.getOwner() != null && permissionable.getOwner().equals(user.getUserId()))) {
			for (Entry<String, Integer> type : getPermissionTypes().entrySet()) {
				try {
					List<Role> rolesForType = loadRolesForPermission(getPermissions(permissionable), type.getValue());

					for (Role role : rolesForType) {
						if (role.getId().equals(APILocator.getRoleAPI().loadCMSOwnerRole().getId())) {
							permissions.add(type.getValue());
							break;
						}
					}

				} catch (NoSuchRoleException e) {
					Logger.error(this, e.getMessage(), e);
					throw new DotDataException(e.getMessage(), e);
				}

			}
		}


		return new ArrayList<Integer>(permissions);
	}

	public List<Integer> getPermissionIdsFromUser(Permissionable permissionable, User user) throws DotDataException {

		RoleAPI roleAPI = APILocator.getRoleAPI();

		List<Role> userRoles = roleAPI.loadRolesForUser(user.getUserId());
		return getPermissionIdsFromRoles(permissionable, userRoles.toArray(new Role[0]), user);

	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#getRoles(long, int, java.lang.String, int, int)
	 */
	public List<Role> getRoles(String inode, int permissionType, String filter, int start, int limit) {

		Inode inodeObj = null;
		List<Role> roleList = null;
		List<Permission> permissionList = null;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getRoles -> before loading inode object(%s)", inode) );
			inodeObj = InodeFactory.getInode(inode, Inode.class);
			permissionList = getPermissions(inodeObj, true);

			roleList = loadRolesForPermission(permissionList, permissionType, filter);

			if( start < roleList.size() ) {

				if (limit > -1 && start + limit < roleList.size() ) { // Valid ranges for pagination?
					roleList = roleList.subList(start, start + limit);
				}
				else {
					roleList = roleList.subList(start, roleList.size());
				}
			}

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
		finally {
			if( roleList == null ) {
				roleList = new ArrayList<Role>(0);
			}
		}

		return roleList;
	}


	public List<Role> getRoles(String inode, int permissionType,
			String filter, int start, int limit, boolean hideSystemRoles) {
		List<Role> roleList = getRoles(inode, permissionType, filter, start, limit);
		List<Role> roleListTemp = new ArrayList<Role>(roleList);
		if(hideSystemRoles)
			for(Role r : roleListTemp) {
				if(PortalUtil.isSystemRole(r))
					roleList.remove(r);
			}

		return roleList;
	}


	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#getRoleCount(long, int, java.lang.String)
	 */
	public int getRoleCount(String inode, int permissionType, String filter) {

		Inode inodeObj = null;
		List<Role> roleList = null;
		List<Permission> permissionList = null;
		int count = 0;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getRoleCount -> before loading inode object(%s)", inode) );
			inodeObj = InodeFactory.getInode(inode, Inode.class);
			permissionList = getPermissions(inodeObj, true);

			roleList = loadRolesForPermission(permissionList, permissionType, filter);

			count = roleList.size();

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		return count;
	}


	public int getRoleCount(String inode, int permissionType,
			String filter, boolean hideSystemRoles) {
		Inode inodeObj = null;
		List<Role> roleList = null;
		List<Permission> permissionList = null;
		int count = 0;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getRoleCount -> before loading inode object(%s)", inode) );
			inodeObj = InodeFactory.getInode(inode, Inode.class);
			permissionList = getPermissions(inodeObj, true);

			roleList = loadRolesForPermission(permissionList, permissionType, filter);

			List<Role> roleListTemp = new ArrayList<Role>(roleList);
			for(Role r : roleListTemp) {
				if(PortalUtil.isSystemRole(r))
					roleList.remove(r);
			}

			count = roleList.size();

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		return count;
	}


	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#getUsers(long, int, java.lang.String, int, int)
	 */
	public List<User> getUsers(String inode, int permissionType, String filter, int start, int limit) {

		Inode inodeObj = null;
		List<User> userList = null;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getUsers -> before loading inode object(%s)", inode) );
			inodeObj = InodeFactory.getInode(inode, Inode.class);

			userList = permissionFactory.getUsers(inodeObj, permissionType, filter, start, limit);

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
		finally {
			if( userList == null ) {
				userList = new ArrayList<User>(0);
			}
		}

		return userList;

	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#getUserCount(long, int, java.lang.String)
	 */
	public int getUserCount(String inode, int permissionType, String filter) {

		Inode inodeObj = null;
		int count = 0;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getUserCount -> before loading inode object(%s)", inode) );
			inodeObj = InodeFactory.getInode(inode, Inode.class);

			count = permissionFactory.getUserCount(inodeObj, permissionType, filter);

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		return count;
	}

	public void clearCache() {
		CacheLocator.getPermissionCache().clearCache();
	}

	public <P extends Permissionable> List<P> filterCollection(List<P> inputList, int requiredTypePermission,boolean respectFrontendRoles, User user) throws DotDataException, DotSecurityException {

		RoleAPI roleAPI = APILocator.getRoleAPI();

		if ((user != null) && roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole()))
			return inputList;

		List<P> permissionables = new ArrayList<P>(inputList);
		if(permissionables.isEmpty()){
			return permissionables;
		}

		Permissionable permissionable;
		int i = 0;

		while (i < permissionables.size()) {
			permissionable = permissionables.get(i);
			if(!doesUserHavePermission(permissionable, requiredTypePermission, user, respectFrontendRoles)){
				permissionables.remove(i);
			} else {
				++i;
			}
		}

		return permissionables;
	}

	public <P extends Permissionable> List<P> filterCollectionByDBPermissionReference(List<P> inputList, int requiredTypePermission,boolean respectFrontendRoles, User user) throws DotDataException, DotSecurityException {

		RoleAPI roleAPI = APILocator.getRoleAPI();

		if ((user != null) && roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole()))
			return inputList;

		List<P> permissionables = new ArrayList<P>(inputList);
		if(permissionables.isEmpty()){
			return permissionables;
		}

		return permissionFactory.filterCollectionByDBPermissionReference(permissionables, requiredTypePermission, respectFrontendRoles, user);
	}

	public void removePermissionsByRole(String roleId) {
		try {
			permissionFactory.removePermissionsByRole(roleId);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	public Map<String, Integer> getPermissionTypes() {
		return permissionFactory.getPermissionTypes();
	}

	public void updateOwner(Permissionable asset, String ownerId) throws DotDataException {
		permissionFactory.updateOwner(asset, ownerId);
	}

	public int maskOfAllPermissions () {
		return permissionFactory.maskOfAllPermissions();
	}

	public List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts)
			throws DotDataException {
		return getPermissionsByRole(role, onlyFoldersAndHosts, false);
	}

	public List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts, boolean bitPermissions)
		throws DotDataException {
		return permissionFactory.getPermissionsByRole(role, onlyFoldersAndHosts, bitPermissions);
	}

	public void resetPermissionsUnder(Permissionable parent) throws DotDataException {
		if(!parent.isParentPermissionable())
			return;
		permissionFactory.resetPermissionsUnder(parent);

	}

	public List<Permission> getInheritablePermissions(Permissionable permissionable) throws DotDataException {
		if(!permissionable.isParentPermissionable())
			return null;
		return permissionFactory.getInheritablePermissions(permissionable, false);
	}

	public List<Permission> getInheritablePermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
		if(!permissionable.isParentPermissionable())
			return null;
		return permissionFactory.getInheritablePermissions(permissionable, bitPermissions);
	}

	public void cascadePermissionUnder(Permissionable permissionable, Role role) throws DotDataException {
		permissionFactory.cascadePermissionUnder(permissionable, role);
	}

	public void resetPermissionReferences(Permissionable perm) throws DotDataException {
		permissionFactory.resetPermissionReferences(perm);

	}

	public void resetChildrenPermissionReferences(Structure structure) throws DotDataException {
		permissionFactory.resetChildrenPermissionReferences(structure);
	}

	public void resetAllPermissionReferences() throws DotDataException {
		permissionFactory.resetAllPermissionReferences();

	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#doesUserHavePermissions(com.dotmarketing.beans.Inode, String, com.liferay.portal.model.User)
	 */
	public boolean doesUserHavePermissions(Permissionable permissionable, String requiredPermissions, User user) throws DotDataException{
		return doesUserHavePermissions(permissionable, requiredPermissions, user, true);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionAPI#doesUserHavePermissions(com.dotmarketing.beans.Inode, String, com.liferay.portal.model.User, boolean)
	 */
    public boolean doesUserHavePermissions(Permissionable permissionable, String requiredPermissions, User user, boolean respectFrontendRoles) throws DotDataException{

		// if we have bad data
		if ((permissionable == null) || (!InodeUtils.isSet(permissionable.getPermissionId()))) {
			if(permissionable != null){
				Logger.debug(this, "Trying to get permissions on null inode of type :" + permissionable.getPermissionType()) ;
				Logger.debug(this, "Trying to get permissions on null inode of class :" + permissionable.getClass()) ;
			}
			return false;
		}

		if(user == null){
			return false;
		}

		if(user!=null && user.getUserId().equals(APILocator.getUserAPI().getSystemUser().getUserId())){
			return true;
		}

		Role adminRole;
		Role anonRole;
		Role frontEndUserRole;
		Role cmsOwnerRole;
		try {
			adminRole = APILocator.getRoleAPI().loadCMSAdminRole();
			anonRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
			frontEndUserRole = APILocator.getRoleAPI().loadLoggedinSiteRole();
			cmsOwnerRole = APILocator.getRoleAPI().loadCMSOwnerRole();
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}

		if(user != null && APILocator.getRoleAPI().doesUserHaveRole(user, adminRole))
			return true;

		List<Role> roles;
		try {
			roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}
		List<String> userRoleIds= new ArrayList<String>();
		for (Role role : roles) {
			try{
				String roleID = role.getId();
				userRoleIds.add(roleID);
				if(roleID.equals(adminRole.getId())){
					// if CMS Admin return true
					return true;
				}
			}catch (Exception e) {
				Logger.error(this, "Roleid should be a long : ",e);
			}
		}

		boolean isHost = false;
		boolean isFolder = false;
		Host host = null;
		Folder folder = null;
		if(permissionable instanceof Host){
			isHost = true;
			host = (Host)permissionable;
		}else if(permissionable instanceof Folder){
			isFolder = true;
			folder = (Folder)permissionable;
		}



		List<String> permissionIdsStr = new ArrayList<String>();
		String[] permissionIdArr = requiredPermissions.split(",");
		if(permissionIdArr.length>0){
			for(String perId : permissionIdArr){
				String[] perIdArr = perId.split(":");
			    permissionIdsStr.add(perIdArr[0].trim()+":"+perIdArr[1].trim());
			}
		}
		int perCount = 0;
		if(!permissionIdsStr.isEmpty()){
			List<Integer> permisssionIds = getPermissionIdsFromUser(permissionable, user);
			List<Permission> permissions = getPermissions(permissionable, true);

			boolean isInheriting = true;
			for(Permission p : permissions){
			    if(p.isIndividualPermission()){
			    	isInheriting = false;
			    	break;
			    }
			}

			if(permissionable.isParentPermissionable()){
				permissions.addAll(getInheritablePermissions(permissionable, true));
			}

			for(Role r : roles){
				permissions.addAll(getPermissionsByRole(r, true, true));
			}

 			if(!permisssionIds.isEmpty()){
				for(String permissionId : permissionIdsStr){
					String[] perId = permissionId.split(":");
					int requiredPermissionId = Integer.parseInt(perId[1].trim());
					String requiredPermissionType = perId[0].trim();
					if(requiredPermissionType.equalsIgnoreCase("PARENT")){
						if(permisssionIds.contains(requiredPermissionId)){
							perCount++;
						}
					}else{
						String perType = permissionTypes.get(requiredPermissionType.toUpperCase());
						if(UtilMethods.isSet(perType) && (isFolder || isHost)){
							for(Permission p : permissions){
								List<RelatedPermissionableGroup> permissionDependencies = permissionable.permissionDependencies(requiredPermissionId);
								try {
									if(respectFrontendRoles){
										// if we are anonymous
										if(p.getRoleId().equals(anonRole.getId()) && p.getType().equals(perType)
												&& p.matchesPermission(requiredPermissionId)
												&& (isInheriting  || (isHost && p.getInode().equals(host.getIdentifier())) ||
												   (isFolder && p.getInode().equals(folder.getInode())))){
											perCount++;
											break;
											//if logged in site user has permission
										}else if(user != null && p.getRoleId().equals(frontEndUserRole.getId())
												&& p.getType().equals(perType)
												&& p.matchesPermission(requiredPermissionId)
												&& (isInheriting || (isHost && p.getInode().equals(host.getIdentifier())) ||
														   (isFolder && p.getInode().equals(folder.getInode())))){
											perCount++;
											break;
										}
									}

									if(p.getRoleId().equals(cmsOwnerRole.getId()) &&
											permissionable.getOwner() != null && permissionable.getOwner().equals(user.getUserId()) &&
											checkRelatedPermissions(permissionDependencies, user)
											&& p.getType().equals(perType)
											&& p.matchesPermission(requiredPermissionId)
											&& (isInheriting  || (isHost && p.getInode().equals(host.getIdentifier())) ||
													   (isFolder && p.getInode().equals(folder.getInode())))){
										perCount++;
										break;
									}else if(p.getType().equals(perType) && p.matchesPermission(requiredPermissionId)
											&& userRoleIds.contains(p.getRoleId())
											&& (isInheriting || (isHost && (p.getInode().equals("SYSTEM_HOST") || p.getInode().equals(host.getIdentifier()))) ||
													   (isFolder && p.getInode().equals(folder.getInode())))){
										perCount++;
										break;
									}

								} catch (DotDataException e1) {
									Logger.error(this, e1.getMessage(), e1);
									throw new DotRuntimeException(e1.getMessage(), e1);
								}

							}
						}

					}

				}
			}
		}
		if(perCount==permissionIdsStr.size()){
			return true;
		}
		return false;
	}

    public boolean doesUserHavePermissions(PermissionableType permType, int permissionType, User user) throws DotDataException {
    	if(user==null) return false;

    	if(APILocator.getUserAPI().isCMSAdmin(user)) return true;

    	Boolean hasPerm = false;
    	RoleAPI roleAPI = APILocator.getRoleAPI();
		List<com.dotmarketing.business.Role> roles = roleAPI.loadRolesForUser(user.getUserId(), false);
		for(com.dotmarketing.business.Role r : roles) {
			List<Permission> perms = APILocator.getPermissionAPI().getPermissionsByRole(r, false);
			for (Permission p : perms) {
				if(p.getType().equals(permType.getCanonicalName())) {
					hasPerm = hasPerm | p.getPermission()>=permissionType;
				}
			}
		}

		return hasPerm;
    }

    public void permissionIndividually(Permissionable parent, Permissionable permissionable, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{
    	List<Permission> perList = new ArrayList<Permission>();
    	List<Permission> newSetOfPermissions = new ArrayList<Permission>();
    	HostAPI hostAPI = APILocator.getHostAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();

		if(!doesUserHavePermission(permissionable, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user))
			throw new DotSecurityException("User id: " + user.getUserId() + " does not have permission to alter permissions on asset " + permissionable.getPermissionId());

    	if(parent.isParentPermissionable()){

    		String type = permissionable.getPermissionType();
    		perList.addAll(permissionFactory.getInheritablePermissions(parent));
    		perList.addAll(permissionFactory.getPermissions(parent,true));
    		Host host = hostAPI.find(permissionable.getPermissionId(), systemUser, false);
			if(host != null) {
				type = Host.class.getCanonicalName();
			}
    		for(Permission p : perList){

    			if(type.equals(Folder.class.getCanonicalName())){
    				if(p.getType().equals(Template.class.getCanonicalName())
    						|| p.getType().equals(Container.class.getCanonicalName())
    						|| p.getType().equals(Category.class.getCanonicalName())
    						|| p.getType().equals(Host.class.getCanonicalName())){
    					continue;
    				}
    			}

    			if(type.equals(Host.class.getCanonicalName())){
    				if(p.getType().equals(Category.class.getCanonicalName())){
    					continue;
    				}
    			}

    			if(type.equals(p.getType()) || p.isIndividualPermission()){
    				Permission dupe = null;
    				List<Permission> dupes = new ArrayList<Permission>();
    				for(Permission newPerm : newSetOfPermissions){
    					if(newPerm.isIndividualPermission() && newPerm.getRoleId().equals(p.getRoleId()) && newPerm.getPermission()>p.getPermission()){
    						dupe = newPerm;
    						break;
    					}else if(newPerm.isIndividualPermission() && newPerm.getRoleId().equals(p.getRoleId())){
    						dupes.add(newPerm);
    					}
    				}
    				if(dupe==null){
    					newSetOfPermissions.removeAll(dupes);
    					if(p.isIndividualPermission()){
    	    				newSetOfPermissions.add(new Permission(p.getType(), permissionable.getPermissionId(), p.getRoleId(), p.getPermission(), true));
    	    				continue;
    					}else{
    						newSetOfPermissions.add(new Permission(permissionable.getPermissionId(), p.getRoleId(), p.getPermission(), true));
    					}
    				}
    				if(!p.isIndividualPermission()){
    				   newSetOfPermissions.add(new Permission(p.getType(), permissionable.getPermissionId(), p.getRoleId(), p.getPermission(), true));
    				}
    			}else{
    				newSetOfPermissions.add(new Permission(p.getType(), permissionable.getPermissionId(), p.getRoleId(), p.getPermission(), true));
    			}
    		}


    	    if(!newSetOfPermissions.isEmpty()){
    	    	permissionFactory.assignPermissions(newSetOfPermissions,permissionable);
    	    }
    	}
    }

    public Permissionable findParentPermissionable(Permissionable permissionable) throws DotDataException, DotSecurityException {
		Permissionable parentPermissionable=permissionable.getParentPermissionable();
		if(parentPermissionable!=null) {
			List<Permission> assetPermissions = getPermissions(permissionable, true);
			Map<String, Inode> inodeCache = new HashMap<String, Inode>();
    		for(Permission p : assetPermissions) {
    			if(!p.getInode().equals(permissionable.getPermissionId())) {
    				String assetInode = p.getInode();
					Inode inode = inodeCache.get(p.getInode());
					if(inode == null) {
						inode = InodeFactory.getInode(assetInode, Inode.class);
						inodeCache.put(inode.getInode(), inode);
					}
					if(inode instanceof Folder) {
						parentPermissionable = (Folder)inode;
					} else if (inode instanceof Structure) {
						parentPermissionable = (Structure)inode;
					} else if (inode instanceof Category) {
						parentPermissionable = (Category)inode;
					} else {
						Host host = APILocator.getHostAPI().find(assetInode, APILocator.getUserAPI().getSystemUser(), false);
						if(host != null) {
							parentPermissionable = host;
						}
					}
    			}
    		}
		}
		return parentPermissionable;
	}

	public boolean isInheritingPermissions(Permissionable permissionable) throws DotDataException {
		return permissionFactory.isInheritingPermissions(permissionable);
	}
}

