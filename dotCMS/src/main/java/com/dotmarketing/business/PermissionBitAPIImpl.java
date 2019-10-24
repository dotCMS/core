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

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Sets;
import com.liferay.portal.NoSuchRoleException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.rainerhahnekamp.sneakythrow.Sneaky;

/**
 * PermissionAPI is an API intended to be a helper class for class to get Permissions.  Classes within the dotCMS
 * should use this API for permissions.  The PermissionAPI will goto the PermissionCache for you so you can use the PermissionAPI
 * trusting that you will not be continually hitting the database.
 *
 * @author David Torres (2009)
 */
public class PermissionBitAPIImpl implements PermissionAPI {


	PermissionFactory permissionFactory;
	private SystemEventsAPI systemEventsAPI;

	public PermissionBitAPIImpl() {
		this(FactoryLocator.getPermissionFactory(),APILocator.getSystemEventsAPI());
	}
	
	/**
	 * Builds a PermissionAPI initialized with its dependent objects.
	 * @return PermissionFactory service reference
	 */
	@VisibleForTesting
	public PermissionBitAPIImpl(PermissionFactory serviceRef, SystemEventsAPI systemEventsAPI) {
		setPermissionFactory(serviceRef);
		this.systemEventsAPI = systemEventsAPI;
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

	@Override
	public boolean doesRoleHavePermission(Permissionable permissionable, int permissionType, Role role, boolean respectFrontendRoles) throws DotDataException {
		return doesRoleHavePermission(permissionable, permissionType, role);
	}

	@Override
	public boolean doesRoleHavePermission(Permissionable permissionable, int permissionType, Role role) throws DotDataException {

		// if we have bad data
		if (permissionable != null && (!InodeUtils.isSet(permissionable.getPermissionId())) || (role == null)) {
			return false;
		}
		
		// Folders do not have PUBLISH, use EDIT instead
		if(PermissionableType.FOLDERS.getCanonicalName().equals(permissionable.getPermissionType()) && permissionType == PERMISSION_PUBLISH){
			permissionType=PERMISSION_EDIT;
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

	@Override
	public List<Permission> getInheritablePermissionsRecurse(Permissionable permissionable) throws DotDataException {
		List<Permission> fPerms = getInheritablePermissions(permissionable, false);
		Permissionable parent = permissionable.getParentPermissionable();
		while(parent != null){
			fPerms.addAll(getInheritablePermissions(parent, false));
			parent = parent.getParentPermissionable();
		}

		return fPerms;
	}

	@Override
	public boolean doesUserHaveInheriablePermissions(Permissionable parentPermissionable, String type, int requiredPermissions, User user) throws DotDataException {

		if(parentPermissionable == null){
			Logger.error(this, "Parent permissionable is null");
			throw new NullPointerException("Parent permissionable is null");
		}
		// Folders do not have PUBLISH, use EDIT instead
		if(PermissionableType.FOLDERS.getCanonicalName().equals(type) && requiredPermissions == PERMISSION_PUBLISH){
			requiredPermissions=PERMISSION_EDIT;
		}
		
		
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

	@Override
	public void  checkPermission(Permissionable permissionable, PermissionLevel level, User user) throws DotSecurityException{
		try{
			if(!doesUserHavePermission(permissionable, level.type, user, true)){
				throw new DotSecurityException("User:" + user +" does not have permissions " + level + " for object " + permissionable + " of type " + permissionable.getPermissionType());
			}
		}
		catch(DotDataException e){
			throw new DotStateException(e);
		}
	}

	@Override
	public boolean doesUserHavePermission(Permissionable permissionable, int permissionType, User user) throws DotDataException {
		return doesUserHavePermission(permissionable, permissionType, user, true);
	}

	@CloseDBIfOpened
	@Override
	public boolean doesUserHavePermission(final Permissionable permissionable, int permissionType, final User userIn, final boolean respectFrontendRoles) throws DotDataException {
	    
	    
	    final User user = (userIn==null || userIn.getUserId()==null) ? APILocator.getUserAPI().getAnonymousUser() : userIn;
	    
	    
	    
	    
        if(user.getUserId().equals(APILocator.getUserAPI().getSystemUser().getUserId())){
            return true;
        }
        
		// if we have bad data
		if ((permissionable == null) || (!InodeUtils.isSet(permissionable.getPermissionId()))) {
			if(permissionable != null){
				Logger.debug(this.getClass(), "Trying to get permissions on null inode of type :" + permissionable.getPermissionType()) ;
				Logger.debug(this.getClass(), "Trying to get permissions on null inode of class :" + permissionable.getClass()) ;
			}
			if(permissionable == null){
				Logger.error(this, "Permissionable object is null");
				throw new NullPointerException("Permissionable object is null");
			}
			return false;
		}


		
		// Folders do not have PUBLISH, use EDIT instead
		if(PermissionableType.FOLDERS.getCanonicalName().equals(permissionable.getPermissionType()) && permissionType == PERMISSION_PUBLISH){
			permissionType=PERMISSION_EDIT;
		}

		Role adminRole;
		Role anonRole;
		Role frontEndUserRole;
		Role cmsOwnerRole;
		User anonUser;
		try {
			adminRole = APILocator.getRoleAPI().loadCMSAdminRole();
			anonRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
			frontEndUserRole = APILocator.getRoleAPI().loadLoggedinSiteRole();
			cmsOwnerRole = APILocator.getRoleAPI().loadCMSOwnerRole();
			anonUser=APILocator.getUserAPI().getAnonymousUser();
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}

		if(APILocator.getRoleAPI().doesUserHaveRole(user, adminRole)) {
			return true;
		}

		List<RelatedPermissionableGroup> permissionDependencies = permissionable.permissionDependencies(permissionType);


		List<Permission> perms =  getPermissions(permissionable, true);

		for(Permission p : perms){
			if(p.matchesPermission(permissionType)){
				if(respectFrontendRoles){

                        //anonymous role should not be able to access non-live contentlet
                        boolean isContentlet = permissionable instanceof Contentlet;
                        if (p.getRoleId().equals(anonRole.getId()) && (!isContentlet
                                || isLiveContentlet(permissionable))) {
                            return true;
                            //if logged in site user has permission
                        }else if(!anonUser.getUserId().equals(user.getUserId()) && p.getRoleId().equals(frontEndUserRole.getId())){
                            return true;
                        }
                   
                }
				// if owner and owner has required permission return true
				try {
					if(p.getRoleId().equals(cmsOwnerRole.getId()) && permissionable.getOwner() != null && permissionable.getOwner().equals(user.getUserId()) &&
							checkRelatedPermissions(permissionDependencies, user)){
						return true;
					}
				} catch (DotDataException e1) {
					Logger.error(this, e1.getMessage(), e1);
					throw new DotRuntimeException(e1.getMessage(), e1);
				}
			}
		}



		List<Role> roles = Sneaky.sneak(()->APILocator.getRoleAPI().loadRolesForUser(user.getUserId()));
        // remove front end user access for anon user (e.g, /intranet)
		if(user.isAnonymousUser()) {
		    
		    if(!isLiveContentlet(permissionable)) {
		        return false;
		    }
		    
		    
            roles.removeIf(r->r.equals(frontEndUserRole));
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
        
        if(!respectFrontendRoles) {
			List<String> frontEndRoles = new ArrayList<String>(3);
	
			try {
				frontEndRoles.add(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
				frontEndRoles.add(APILocator.getRoleAPI().loadLoggedinSiteRole().getId());
				frontEndRoles.add(APILocator.getRoleAPI().loadRoleByKey("anonymous").getId());
			} catch (DotDataException e1) {
				Logger.error(this, e1.getMessage(), e1);
				throw new DotRuntimeException(e1.getMessage(), e1);
			}
			
			if(frontEndRoles.containsAll(userRoleIds)) {
				return false; // The user roles are ALL frontEnd roles AND respectFrontEndRoles is false, so return false
			}
		}
        
		return doRolesHavePermission(userRoleIds,getPermissions(permissionable, true),permissionType);
	}

    /**
     *
     * @param permissionable
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private boolean isLiveContentlet(Permissionable permissionable) {
        return permissionable!=null && permissionable instanceof Contentlet
                && Sneaky.sneak(()->((Contentlet) permissionable).isLive());
    }

    @WrapInTransaction
	@Override
	public void removePermissions(Permissionable permissionable) throws DotDataException {

		permissionFactory.removePermissions(permissionable);
		
		if(permissionable instanceof Host){	
			//Send a websocket event to notificate a site permission change  
			systemEventsAPI.pushAsync(SystemEventType.UPDATE_SITE_PERMISSIONS,
					new Payload(permissionable, Visibility.GLOBAL,	(String) null));
		}
	}

	//This method can be used later
	@WrapInTransaction
	@Override
	public void setDefaultCMSAnonymousPermissions(Permissionable permissionable) throws DotDataException{
		Role cmsAnonymousRole;
		try {
			cmsAnonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
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

			//Default links permissions
			p = new Permission(Link.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

			//Default pages permissions
			p = new Permission(IHTMLPage.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

			//Default content permissions
			p = new Permission(Contentlet.class.getCanonicalName(), permissionable.getPermissionId(), cmsAnonymousRole.getId(), PermissionAPI.PERMISSION_READ, true);
			permissionFactory.savePermission(p, permissionable);

		}
	}

	@WrapInTransaction
	@Override
	public void setDefaultCMSAdminPermissions (Permissionable permissionable) throws DotDataException {
		Role cmsAdminRole;
		try {
			cmsAdminRole = APILocator.getRoleAPI().loadCMSAdminRole();
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

	@WrapInTransaction
	@Override
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
	@CloseDBIfOpened
	public  Permission find(String id){
		return permissionFactory.getPermission(String.valueOf(id));
	}

	@CloseDBIfOpened
	@Override
	public List<Permission> getPermissions(Permissionable permissionable) throws DotDataException {
		return permissionFactory.getPermissions(permissionable, false);
	}

	@CloseDBIfOpened
	@Override
	public List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
		return permissionFactory.getPermissions(permissionable, bitPermissions);
	}

	@CloseDBIfOpened
	@Override
	public List<Permission> getPermissions(Permissionable permissionable,
			boolean bitPermissions, boolean onlyIndividualPermissions) throws DotDataException {
		return permissionFactory.getPermissions(permissionable, bitPermissions, onlyIndividualPermissions);
	}

	@CloseDBIfOpened
	@Override
	public List<Permission> getPermissions(Permissionable permissionable,
			boolean bitPermissions, boolean onlyIndividualPermissions, boolean forceLoadFromDB) throws DotDataException {
		return permissionFactory.getPermissions(permissionable, bitPermissions, onlyIndividualPermissions, forceLoadFromDB);
	}

	@CloseDBIfOpened
	@Override
    public void addPermissionsToCache ( Permissionable permissionable ) throws DotDataException {
        permissionFactory.addPermissionsToCache( permissionable );
    }

	@Override
    // todo: should be this a transaction (all of nothing on save several permissions)???
    public void save(Collection<Permission> permissions, Permissionable permissionable, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        
    	for (Permission permission: permissions ) {
            save(permission, permissionable, user, respectFrontendRoles, false);
        }

        if(permissionable instanceof Host){	
			//Send a websocket event to notificate a site permission change  
			systemEventsAPI.pushAsync(SystemEventType.UPDATE_SITE_PERMISSIONS,
					new Payload(permissionable, Visibility.GLOBAL,	(String) null));
		}
    }

    /**
     * Saves passed in permission
	 * @param Permission to save
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
    @Override
	public void save(Permission permission, Permissionable permissionable, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		save(permission, permissionable, user, respectFrontendRoles, true);
	}
	
	/**
	 * Saves passed  permission and send a system notification if the 
	 * create event parameter is set true
	 * 
	 * @param permission A list of permissions to apply
	 * @param permissionable The object where the permsiions will be applied
	 * @param user current user
     * @param respectFrontendRoles indicates if should be respected front end roles
	 * @param createEvent indicate if a system event should be notified
	 * 
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@WrapInTransaction
	private void save(Permission permission, Permissionable permissionable, User user, boolean respectFrontendRoles, boolean createEvent) throws DotDataException, DotSecurityException {
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
		
		if(createEvent){
			if(permissionable instanceof Host){	
				//Send a websocket event to notificate a site permission change  
				systemEventsAPI.pushAsync(SystemEventType.UPDATE_SITE_PERMISSIONS,
						new Payload(permissionable, Visibility.GLOBAL,	(String) null));
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#assignPermissions
	 * @deprecated Use save(permission) instead.
	 */
	@WrapInTransaction
	@Override
    @Deprecated
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

		// NOTE: Method "assignPermissions" is deprecated in favor of "savePermission", which has subtle functional differences. Please take these differences into consideration if planning to replace this method with the "savePermission"
		permissionFactory.assignPermissions(includingLockedRolePermissions, permissionable);

		if(permissionable instanceof Host){	
			//Send a websocket event to notificate a site permission change  
			systemEventsAPI.pushAsync(SystemEventType.UPDATE_SITE_PERMISSIONS,
					new Payload(permissionable, Visibility.GLOBAL,	(String) null));
		}

		AdminLogger.log(PermissionBitAPIImpl.class, "assign Permissions Action", "Assigning permissions to :"+permissionable.getPermissionId(),user);
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@CloseDBIfOpened
	@Override
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

	@Override
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

	@Override
	public boolean doesUserOwn(Inode inode, User user) throws DotDataException{
		if(user == null || inode == null){
			return false;
		}else if(inode instanceof WebAsset){
			return APILocator.getIdentifierAPI().find(inode).equals(user.getUserId());
		}else{
			return inode.getOwner() != null && inode.getOwner().equals(user.getUserId());
		}
	}

	// PERMISSION MAP METHODS!!!
	@Override
	public void mapAllPermissions() throws DotDataException {

		Logger.debug(PermissionBitAPIImpl.class, "\n\nGoing to map all Permissions!!!!");

		if (Config.CONTEXT == null) {
			return;
		}

		Logger.debug(PermissionBitAPIImpl.class, "\n\nFinished mapping all Permissions!!!!");
	}

	@CloseDBIfOpened
	@Override
	public List<Integer> getPermissionIdsFromRoles(final Permissionable permissionable, final Role[] roles,
												   final User user) throws DotDataException {
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

	@Override
	public List<Integer> getPermissionIdsFromUser(Permissionable permissionable, User user) throws DotDataException {

		RoleAPI roleAPI = APILocator.getRoleAPI();

		List<Role> userRoles = roleAPI.loadRolesForUser(user.getUserId());
		return getPermissionIdsFromRoles(permissionable, userRoles.toArray(new Role[0]), user);

	}

	@Override
	public List<Role> getRoles(String inode, int permissionType, String filter, int start, int limit) {

		Inode inodeObj = null;
		List<Role> roleList = null;
		List<Permission> permissionList = null;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getRoles -> before loading inode object(%s)", inode) );
			
			inodeObj = InodeUtils.getInode(inode);
			
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

	@Override
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

	@Override
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

	@Override
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

	@CloseDBIfOpened
	@Override
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

	@CloseDBIfOpened
	@Override
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

	@Override
	public void clearCache() {
		CacheLocator.getPermissionCache().clearCache();
	}

	@Override
    public void removePermissionableFromCache(String permissionableId) {
        CacheLocator.getPermissionCache().remove(permissionableId);
    }

    @CloseDBIfOpened
    @Override
	public <P extends Permissionable> List<P> filterCollection(final List<P> inputList,
															   final int requiredTypePermission,
															   final boolean respectFrontendRoles, User user) throws DotDataException, DotSecurityException {

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

	@CloseDBIfOpened
	@Override
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

	@WrapInTransaction
	@Override
	public void removePermissionsByRole(String roleId) {
		try {
			permissionFactory.removePermissionsByRole(roleId);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	@CloseDBIfOpened
	@Override
	public Map<String, Integer> getPermissionTypes() {
		return permissionFactory.getPermissionTypes();
	}

	@WrapInTransaction
	@Override
	public void updateOwner(Permissionable asset, String ownerId) throws DotDataException {
		permissionFactory.updateOwner(asset, ownerId);
	}

	@Override
	public int maskOfAllPermissions () {
		return permissionFactory.maskOfAllPermissions();
	}

	@Override
	public List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts)
			throws DotDataException {
		return getPermissionsByRole(role, onlyFoldersAndHosts, false);
	}

	@CloseDBIfOpened
	@Override
	public List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts, boolean bitPermissions)
		throws DotDataException {
		return permissionFactory.getPermissionsByRole(role, onlyFoldersAndHosts, bitPermissions);
	}

	@WrapInTransaction
	@Override
	public void resetPermissionsUnder(Permissionable parent) throws DotDataException {
		if(!parent.isParentPermissionable())
			return;
		permissionFactory.resetPermissionsUnder(parent);

	}

	@CloseDBIfOpened
	@Override
	public List<Permission> getInheritablePermissions(Permissionable permissionable) throws DotDataException {
		if(!permissionable.isParentPermissionable())
			return null;
		return permissionFactory.getInheritablePermissions(permissionable, false);
	}

	@CloseDBIfOpened
	@Override
	public List<Permission> getInheritablePermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
		if(!permissionable.isParentPermissionable())
			return null;
		return permissionFactory.getInheritablePermissions(permissionable, bitPermissions);
	}

	@WrapInTransaction
	@Override
	public void cascadePermissionUnder(Permissionable permissionable, Role role) throws DotDataException {
		permissionFactory.cascadePermissionUnder(permissionable, role);
	}

	@WrapInTransaction
	@Override
	public void resetPermissionReferences(Permissionable perm) throws DotDataException {
		permissionFactory.resetPermissionReferences(perm);

	}

	@WrapInTransaction
	@Override
	public void resetChildrenPermissionReferences(Structure structure) throws DotDataException {
		permissionFactory.resetChildrenPermissionReferences(structure);
	}

	@WrapInTransaction
	@Override
	public void resetAllPermissionReferences() throws DotDataException {
		permissionFactory.resetAllPermissionReferences();

	}

	@Override
	public boolean doesUserHavePermissions(Permissionable permissionable, String requiredPermissions, User user) throws DotDataException{
		return doesUserHavePermissions(permissionable, requiredPermissions, user, true);
	}

	@CloseDBIfOpened
	@Override
    public boolean doesUserHavePermissions(Permissionable permissionable, String requiredPermissions, User user, boolean respectFrontendRoles) throws DotDataException{

		// if we have bad data
		if ((permissionable == null) || (!InodeUtils.isSet(permissionable.getPermissionId()))) {
			if(permissionable != null){
				Logger.debug(this, "Trying to get permissions on null inode of type :" + permissionable.getPermissionType()) ;
				Logger.debug(this, "Trying to get permissions on null inode of class :" + permissionable.getClass()) ;
			}
			if(permissionable == null){
				Logger.error(this, "Permissionable object is null");
				throw new NullPointerException("Permissionable object is null");
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
		User anonUser;
		try {
			adminRole = APILocator.getRoleAPI().loadCMSAdminRole();
			anonRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
			frontEndUserRole = APILocator.getRoleAPI().loadLoggedinSiteRole();
			cmsOwnerRole = APILocator.getRoleAPI().loadCMSOwnerRole();
			 anonUser=APILocator.getUserAPI().getAnonymousUser();
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
		try {

			if(this.isHost(permissionable)){

				isHost = true;
				host = (permissionable instanceof PermissionableProxy)?
						APILocator.getHostAPI()
							.find(permissionable.getPermissionId(), APILocator.systemUser(),false):
						(Host) permissionable;

			} else if(this.isFolder(permissionable)){

				isFolder = true;
				folder = (permissionable instanceof PermissionableProxy)?
						APILocator.getFolderAPI()
								.find(permissionable.getPermissionId(), APILocator.systemUser(), false):
						(Folder) permissionable;
			}
		} catch (DotSecurityException e) {

			throw new DotDataException(e);
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
										}else if(user != null && ! user.getUserId().equals(anonUser.getUserId()) && p.getRoleId().equals(frontEndUserRole.getId())
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

	private boolean isFolder(final Permissionable permissionable) {
		
		return permissionable instanceof Folder ||
				(null != permissionable && permissionable instanceof PermissionableProxy
						&& Folder.class.getName().equals(PermissionableProxy.class.cast(permissionable).getType()));
	}

	private boolean isHost(final Permissionable permissionable) {

		return permissionable instanceof Host ||
				(null != permissionable && permissionable instanceof PermissionableProxy
						&& Host.class.getName().equals(PermissionableProxy.class.cast(permissionable).getType()));
	}

	@Override
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

    /**
     * @Deprecated: use permissionIndividually(Permissionable parent, Permissionable permissionable,
     * User user) instead.
     */
    @Override
    @Deprecated
    public void permissionIndividually(Permissionable parent, Permissionable permissionable,
            User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        permissionIndividually(parent, permissionable, user);
    }

    @WrapInTransaction
    @Override
    public void permissionIndividually(Permissionable parent, Permissionable permissionable,
            User user) throws DotDataException, DotSecurityException {

        List<Permission> newSetOfPermissions = getNewPermissions(parent, permissionable, user);

        if (!newSetOfPermissions.isEmpty()) {
            // NOTE: Method "assignPermissions" is deprecated in favor of "savePermission",
            // which has subtle functional differences. Please take these differences into
            // consideration if planning to replace this method with the "savePermission"
            permissionFactory.assignPermissions(newSetOfPermissions, permissionable);
        }
    }

    @WrapInTransaction
    @Override
    public void permissionIndividuallyByRole(Permissionable parent, Permissionable permissionable,
            User user, Role role) throws DotDataException, DotSecurityException {

        List<Permission> newSetOfPermissions = getNewPermissions(parent, permissionable, user);
        ImmutableList.Builder<Permission> immutablePermissionsFiltered = new Builder<>();

        // We need to make sure that newSetOfPermissions doesn't contain
        // a child or sibling of the role we are assigning permissions.
        for (Permission newPermission : newSetOfPermissions) {
            Role newPermissionRole = APILocator.getRoleAPI().loadRoleById(newPermission.getRoleId());

            if (!APILocator.getRoleAPI().isParentRole(role, newPermissionRole)
				&& !APILocator.getRoleAPI().isSiblingRole(role, newPermissionRole)) {
                immutablePermissionsFiltered.add(newPermission);
            }
        }

        final List<Permission> permissionsFiltered = immutablePermissionsFiltered.build();
        if (!permissionsFiltered.isEmpty()) {
            // NOTE: Method "assignPermissions" is deprecated in favor of "savePermission",
            // which has subtle functional differences. Please take these differences into
            // consideration if planning to replace this method with the "savePermission"
            permissionFactory.assignPermissions(permissionsFiltered, permissionable);
        }
    }

    /**
     * Retrieves all the parent permissions in order to be applied to the permissionable.
     */
    private List<Permission> getNewPermissions(Permissionable parent, Permissionable permissionable,
            User user) throws DotDataException, DotSecurityException {

        ImmutableList.Builder<Permission> immutablePermissionList = new Builder<>();
        List<Permission> newSetOfPermissions = new ArrayList<>();

        if (!doesUserHavePermission(permissionable, PermissionAPI.PERMISSION_EDIT_PERMISSIONS,
                user)) {
            throw new DotSecurityException("User id: " + user.getUserId()
                    + " does not have permission to alter permissions on asset " + permissionable
                    .getPermissionId());
        }

        if (parent.isParentPermissionable()) {

            String type = permissionable.getPermissionType();
            immutablePermissionList.addAll(permissionFactory.getInheritablePermissions(parent));
            immutablePermissionList.addAll(permissionFactory.getPermissions(parent, true));
            List<Permission> permissionList = immutablePermissionList.build();

            Host host = APILocator.getHostAPI()
                    .find(permissionable.getPermissionId(), APILocator.getUserAPI().getSystemUser(),
                            false);
            if (host != null) {
                type = Host.class.getCanonicalName();
            }

            final Set<String> classesToIgnoreFolder = Sets
                    .newHashSet(Template.class.getCanonicalName(),
                            Container.class.getCanonicalName(),
                            Category.class.getCanonicalName(),
                            Host.class.getCanonicalName());

            final Set<String> classesToIgnoreHost = Sets
                    .newHashSet(Category.class.getCanonicalName());

            for (Permission permission : permissionList) {

                if (type.equals(Folder.class.getCanonicalName()) && classesToIgnoreFolder
                        .contains(permission.getType())) {
                    continue;
                }

                if (type.equals(Host.class.getCanonicalName()) && classesToIgnoreHost
                        .contains(permission.getType())) {
                    continue;
                }

                if (type.equals(permission.getType()) || permission.isIndividualPermission()) {
                    Permission duplicatedPermission = null;
                    ImmutableList.Builder<Permission> immutableDuplicatedList = new Builder<>();

                    for (Permission newPermission : newSetOfPermissions) {
                        if (newPermission.isIndividualPermission() && newPermission.getRoleId()
                                .equals(permission.getRoleId())
                                && newPermission.getPermission() > permission
                                .getPermission()) {
                            duplicatedPermission = newPermission;
                            break;
                        } else if (newPermission.isIndividualPermission() && newPermission
                                .getRoleId()
                                .equals(permission.getRoleId())) {
                            immutableDuplicatedList.add(newPermission);
                        }
                    }
                    List<Permission> duplicatedPermissionList = immutableDuplicatedList.build();
                    if (duplicatedPermission == null) {
                        newSetOfPermissions.removeAll(duplicatedPermissionList);
                        if (permission.isIndividualPermission()) {
                            newSetOfPermissions.add(new Permission(permission.getType(),
                                    permissionable.getPermissionId(), permission.getRoleId(),
                                    permission.getPermission(), true));
                            continue;
                        } else {
                            newSetOfPermissions.add(new Permission(permissionable.getPermissionId(),
                                    permission.getRoleId(), permission.getPermission(), true));
                        }
                    }
                    if (!permission.isIndividualPermission()) {
                        newSetOfPermissions
                                .add(new Permission(permission.getType(),
                                        permissionable.getPermissionId(),
                                        permission.getRoleId(), permission.getPermission(), true));
                    }
                } else {
                    newSetOfPermissions
                            .add(new Permission(permission.getType(),
                                    permissionable.getPermissionId(),
                                    permission.getRoleId(), permission.getPermission(), true));
                }
            }


        }
        return newSetOfPermissions;
    }

    @CloseDBIfOpened
    @Override
    public Permissionable findParentPermissionable(final Permissionable permissionable) throws DotDataException, DotSecurityException {
		Permissionable parentPermissionable=permissionable.getParentPermissionable();
		if(parentPermissionable!=null) {
			final List<Permission> assetPermissions = getPermissions(permissionable, true);
			final Map<String, Inode> inodeCache = new HashMap<String, Inode>();
    		for(Permission p : assetPermissions) {
    			if(!p.getInode().equals(permissionable.getPermissionId())) {
    				final String assetInode = p.getInode();
                    Inode inode = inodeCache.get(p.getInode());
                    if (null == inode) {
                        // Both Structure and ContentType classes are handled properly here
                        inode = InodeUtils.getInode(assetInode);
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

	@CloseDBIfOpened
	public boolean isInheritingPermissions(Permissionable permissionable) throws DotDataException {
		return permissionFactory.isInheritingPermissions(permissionable);
	}

}
