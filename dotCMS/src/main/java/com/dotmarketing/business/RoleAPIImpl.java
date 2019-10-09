package com.dotmarketing.business;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.RoleNameException;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.util.*;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.GetterUtil;
import com.liferay.util.SystemProperties;

import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jason Tesser
 * RoleAPI is an API intended to be a helper class for class to get Role entities.  Classes within the dotCMS
 * should use this API for role management. 
 */
public class RoleAPIImpl implements RoleAPI {

	private final String ROLENAME_REGEXP_PATTERN = GetterUtil.getString( SystemProperties.get( "RoleName.regexp.pattern" ) );
	
	private Role CMS_ADMIN = null;
	private Role CMS_ANON = null;
	private Role CMS_OWNER = null;
	private Role LOGGEDIN_SITE_USER = null;

	private final UserAPI userAPI;
	private final RoleFactory roleFactory;

	public RoleAPIImpl()  {
		this(FactoryLocator.getRoleFactory(), APILocator.getUserAPI());
	}

	@VisibleForTesting
	public RoleAPIImpl(RoleFactory roleFactory, UserAPI userAPI) {
		this.roleFactory = roleFactory;
		this.userAPI = userAPI;
	}

	@CloseDBIfOpened
	@Override
	public List<Role> findAllAssignableRoles(final boolean showSystemRoles) throws DotDataException {
		return roleFactory.findAllAssignableRoles(showSystemRoles);
	}

	@CloseDBIfOpened
	@Override
	public Role loadRoleById(final String roleId) throws DotDataException {
		return roleFactory.getRoleById(roleId);
	}

	@Override
	public List<Role> loadRolesForUser(final String userId) throws DotDataException {
		return loadRolesForUser(userId, true);
	}
	

	@CloseDBIfOpened
	@Override
	public List<Role> loadRolesForUser(final String userId, final boolean includeImplicitRoles)
			throws DotDataException {
		return roleFactory.loadRolesForUser(userId,includeImplicitRoles);
	}
	
    /* (non-Javadoc)
	 * @see com.dotmarketing.business.RoleAPI#getRolesByName(java.lang.String, int, int)
	 */
    @CloseDBIfOpened
	@Override
    public List<Role> findRolesByNameFilter(final String filter, final int start, final int limit) throws DotDataException {
    	return roleFactory.getRolesByName(filter, start, limit);
    }

    @CloseDBIfOpened
	@Override
	public List<User> findUsersForRole(final Role role, final boolean inherited) throws DotDataException, NoSuchUserException, DotSecurityException {

		if(!inherited){
			return findUsersForRole(role);
		}
		
		List<Role> roles =  findRoleHierarchy(role);
        List<User> users = new ArrayList<User>();
        for(Role x : roles){
        	List<User> ul = findUsersForRole(x);
        	if(ul!=null){
        		users.addAll(findUsersForRole(x));
        	}
        }
        return users;
	}

	@Override
	public List<Role> findRoleHierarchy(Role role) throws DotDataException, NoSuchUserException, DotSecurityException{

		
        List<Role> roles = new ArrayList<Role>();
        roles.add(role);
        int i=0;
        while(!role.getParent().equals(role.getId()) && i< 100){
        	role = loadRoleById(role.getParent());
        	roles.add(0,role);
        	i++;
        }
        
        return roles;
	}
	
	
	
    /* (non-Javadoc)
	 * @see com.dotmarketing.business.RoleAPI#getRolesByName(java.lang.String, int, int)
	 */
	@Override
    public List<Role> findRolesByFilterLeftWildcard(String filter, final int start, final int limit) throws DotDataException {
    	
		if(filter !=null)
			filter = filter.toLowerCase() + "%";

    	return roleFactory.getRolesByNameFiltered(filter, start, limit);
    }

    @Override
    public Role findRoleByName(final String rolename, final Role parent) throws DotDataException {
    	return roleFactory.findRoleByName(rolename, parent);
    }

    @WrapInTransaction
	@Override
    public void delete (final Role role ) throws DotDataException, DotStateException {

        Role r = loadRoleById( role.getId() );

        for ( String uid : roleFactory.findUserIdsForRole( role, true ) ) {
            CacheLocator.getRoleCache().remove( uid );
        }

        if ( r.isLocked() ) {
            throw new DotStateException( "Cannot delete locked role" );
        }
        if ( r.isSystem() ) {
            throw new DotStateException( "Cannot edit a system role" );
        }

        try {

            r.setEditPermissions( true );
            r.setEditLayouts( true );
            r.setEditUsers( true );
            roleFactory.save( r );

            List<User> users = findUsersForRole( r.getId() );
            if ( users != null ) {
                for ( User u : users ) {
                    removeRoleFromUser( r, u );
                }
            }

            PermissionAPI permAPI = APILocator.getPermissionAPI();
            permAPI.removePermissionsByRole( role.getId() );
            LayoutAPI layoutAPI = APILocator.getLayoutAPI();
            for ( Layout l : layoutAPI.loadLayoutsForRole( role ) ) {
                removeLayoutFromRole( l, role );
            }
            SecurityLogger.logInfo(this.getClass(), "Deleting role:'" + role.getName() + "' " + role);
            roleFactory.delete( role );

        } catch ( Exception e ) {

            if ( role != null ) {
                Logger.error( this.getClass(), "Error deleting Role: " + role.getName(), e );
            } else {
                Logger.error( this.getClass(), "Error deleting Role", e );
            }
            throw new DotDataException( e.getMessage() );
        }
    }

    @WrapInTransaction
	@Override
	public boolean roleExistsByName(final String roleName, final Role parent) throws DotDataException {
		Role r = roleFactory.findRoleByName(roleName, parent);
		return (r != null);
	}

	@WrapInTransaction
	@Override
	public void addRoleToUser(final Role role, final User user) throws DotDataException, DotStateException {
		if(role==null || user==null)return;
	  final Role currentRole = loadRoleById(role.getId());
	  if(!roleFactory.doesUserHaveRole(user, currentRole)) {
		if(!currentRole.isEditUsers()){
			throw new DotStateException("Cannot alter users on this role.  Name:" + role.getName() + ", id:" + role.getId());
		}
		SecurityLogger.logInfo(this.getClass(), "Adding role:'" + role.getName() + "' to user:" + user.getUserId() + " email:" + user.getEmailAddress());
		roleFactory.addRoleToUser(role, user);
	  }
	}
	
	public void addRoleToUser(String roleId, User user)	throws DotDataException, DotStateException {
		Role r = loadRoleById(roleId);
		addRoleToUser(r, user);
	}

	@WrapInTransaction
	@Override
	public Role save(final Role role) throws DotDataException, DotStateException {
		if(InodeUtils.isSet(role.getId())) {
			if(role.isLocked() || role.isSystem()){
				throw new DotStateException("Cannot save locked or system role");
			}
		}
		if(!UtilMethods.isSet(role.getName())  || !RegEX.contains( role.getName(), ROLENAME_REGEXP_PATTERN ) || role.getName().length()>100) {
			throw new RoleNameException();
		}
		
		//Checking the role key does not already exist
		if(role.getRoleKey() != null && !role.getRoleKey().equals("")) {
			Role dupRole = this.loadRoleByKey(role.getRoleKey());
			if(dupRole != null && !dupRole.getId().equals(role.getId()))
				throw new DuplicateRoleKeyException("A role with id = " + dupRole.getId() + " and key = " + role.getRoleKey() + " already exists");
		}
		
		//Checking if role with the same path/fqn already exists
		Role roleParent = null;
		if(role.getParent() != null) {
			roleParent = roleFactory.getRoleById(role.getParent());
		}
		Role dupRole = roleFactory.findRoleByName(role.getName(), roleParent);
		if(dupRole != null && !dupRole.getId().equals(role.getId()))
			throw new DuplicateRoleException("A role with id = " + dupRole.getId() + " and name = " + dupRole.getName());
			
		SecurityLogger.logInfo(this.getClass(), "Saving role:'" + role.getName() + "' " + role);
    return roleFactory.save(role);
	}

	@WrapInTransaction
	@Override
	public Role save(final Role role, final String existingId) throws DotDataException, DotStateException {
		if(role==null) return null;
		
		return roleFactory.save(role, existingId);
	}

	@CloseDBIfOpened
	@Override
	public List<Role> findRootRoles() throws DotDataException {
		return roleFactory.findRootRoles();
	}

	@CloseDBIfOpened
	@Override
	public boolean doesUserHaveRole(final User user, final Role role) throws DotDataException {
		return roleFactory.doesUserHaveRole(user, role);
	}

	@CloseDBIfOpened
	@Override
	public boolean doesUserHaveRole(final User user, final String roleId) throws DotDataException {
		final Role role = roleFactory.getRoleById(roleId);
		return doesUserHaveRole(user, role);
	}

	@CloseDBIfOpened
	@Override
	public Role loadCMSAnonymousRole() throws DotDataException {
		if(CMS_ANON == null){
			CMS_ANON =  roleFactory.loadRoleByKey(Role.CMS_ANONYMOUS_ROLE);
		}
		return CMS_ANON;
	}

	@CloseDBIfOpened
	@Override
	public Role loadCMSOwnerRole() throws DotDataException {
		if(CMS_OWNER == null){
			CMS_OWNER =  roleFactory.loadRoleByKey(Role.CMS_OWNER_ROLE);
		}
		return CMS_OWNER;
	}

	@CloseDBIfOpened
	@Override
	public Role loadLoggedinSiteRole() throws DotDataException {
		if(LOGGEDIN_SITE_USER == null){
			LOGGEDIN_SITE_USER =  roleFactory.loadRoleByKey(Role.DOTCMS_FRONT_END_USER);
		}
		return LOGGEDIN_SITE_USER;
	}
	
  @CloseDBIfOpened
  @Override
  public Role loadFrontEndUserRole() throws DotDataException {

    return this.loadLoggedinSiteRole() ;
  }
	
	
  @CloseDBIfOpened
  @Override
  public Role loadBackEndUserRole() throws DotDataException {
    return roleFactory.loadRoleByKey(Role.DOTCMS_BACK_END_USER);

  }
  
	@CloseDBIfOpened
	@Override
	public Role loadCMSAdminRole() throws DotDataException {
		if(CMS_ADMIN == null){
			CMS_ADMIN = roleFactory.loadRoleByKey(Role.CMS_ADMINISTRATOR_ROLE);
		}
		return CMS_ADMIN;		
	}

	@CloseDBIfOpened
	@Override
    public Role loadDefaultRole () throws DotDataException {
        return roleFactory.loadRoleByKey( DEFAULT_USER_ROLE_KEY );
    }

    @CloseDBIfOpened
	@Override
	public List<String> findUserIdsForRole(final Role role) throws DotDataException {
		return roleFactory.findUserIdsForRole(role);
	}

	@Override
	public List<User> findUsersForRole(final Role role) throws DotDataException, NoSuchUserException, DotSecurityException {
		List<String> uids = findUserIdsForRole(role);
		List<User> users = new ArrayList<User>();
		for (String uid : uids)
			users.add(APILocator.getUserAPI().loadUserById(uid, APILocator.getUserAPI().getSystemUser(), true));
		return users;
	}

	@CloseDBIfOpened
	@Override
	public List<User> findUsersForRole(final String roleId) throws DotDataException, NoSuchUserException, DotSecurityException {
		Role role = roleFactory.getRoleById(roleId);
		return findUsersForRole(role);
	}

	@CloseDBIfOpened
	@Override
	public List<String> loadLayoutIdsForRole(final Role role) throws DotDataException {
		return roleFactory.loadLayoutIdsForRole(role);
	}

	@WrapInTransaction
	@Override
	public void addLayoutToRole(final Layout layout, final Role role) throws DotDataException, DotStateException {
		Role r = loadRoleById(role.getId());
		if(!r.isEditLayouts()){
			throw new DotStateException("Cannot alter layouts on this role");
		}
		roleFactory.addLayoutToRole(layout, role);
	}

	@WrapInTransaction
	@Override
	public void removeLayoutFromRole(final Layout layout, final Role role) throws DotDataException, DotStateException {
		Role r = loadRoleById(role.getId());
		if(!r.isEditLayouts()){
			throw new DotStateException("Cannot alter layouts on this role");
		}
		roleFactory.removeLayoutFromRole(layout, role);
	}

	@CloseDBIfOpened
	@Override
	public Role findRoleByFQN(final String FQN) throws DotDataException {
		return roleFactory.findRoleByFQN(FQN);
	}

	@WrapInTransaction
	@Override
	public void removeRoleFromUser(final Role role, final User user) throws DotDataException, DotStateException {
		final Role roleFromDb = loadRoleById(role.getId());
		roleFactory.removeRoleFromUser(roleFromDb, user);
	}

	@Override
	public void removeAllRolesFromUser(final User user) throws DotDataException,
			DotStateException {
		final List<Role> roles = loadRolesForUser(user.getUserId(), false);
		for(Role role : roles) {
			removeRoleFromUser(role, user);
		}
	}

	@WrapInTransaction
	@Override
	public void lock(final Role role) throws DotDataException {
		final Role roleDb = loadRoleById(role.getId());
		//if(r.isSystem())
		//	throw new DotStateException("Cannot lock a system role");
		roleDb.setLocked(true);
		roleFactory.save(roleDb);
	}

	@WrapInTransaction
	@Override
	public void unLock(final Role role) throws DotDataException {
		final Role roleDb = loadRoleById(role.getId());
		//if(r.isSystem())
		//	throw new DotStateException("Cannot unlock a system role");
		roleDb.setLocked(false);
		roleFactory.save(roleDb);
	}

	@CloseDBIfOpened
	@Override
	public Role loadRoleByKey(final String key) throws DotDataException {
		return roleFactory.loadRoleByKey(key);
	}

	@WrapInTransaction
	@Override
	public Role getUserRole(final User user) throws DotDataException {

		Role role = loadRoleByKey(user.getUserId());
		if(role == null) {
			role = roleFactory.addUserRole(user);
		} else if(!role.getName().equals(user.getFullName()) && !role.getName().equalsIgnoreCase("System")) {
			role.setName(user.getFullName());
			roleFactory.save(role);
		}
		if(!APILocator.getRoleAPI().doesUserHaveRole(user, role)){
			roleFactory.addRoleToUser(role, user);
		}
		return role;
	}

	public boolean doesUserHaveRoles(final String userId, final List<String> roleIds) {

		if (!UtilMethods.isSet(userId) || roleIds == null || roleIds.size() == 0) {
			return false;
		}
		User user;
		try {
			user = this.userAPI.loadUserById(userId, this.userAPI.getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(this, "An error occurred when retrieving information of user ID [" + userId + "]", e);
			return false;
		}
		String currentRoleId = null;
		for (String roleId : roleIds) {
			if (UtilMethods.isSet(roleId.trim())) {
				currentRoleId = roleId;
				try {
					if (this.doesUserHaveRole(user, roleId)) {
						return true;
					}
				} catch (DotDataException e) {
					Logger.error(UserAjax.class, "An error occurred when checking role [" + currentRoleId + "] on user ID ["
							+ userId + "]", e);
					return false;
				}
			}
		}
		return false;
	}

	@CloseDBIfOpened
    @Override
    public boolean isParentRole(Role parent, Role child)
            throws DotSecurityException, DotDataException {

        final List<Role> roleHierarchy = findRoleHierarchy(child);

        // findRoleHierarchy return the hierarchy INCLUDING same role
        // so we need to remove it from the list before checking.
        roleHierarchy.remove(child);

        for (Role role : roleHierarchy) {
            if (role.getId().equals(parent.getId())) {
                return true;
            }
        }

        return false;
    }

	@CloseDBIfOpened
	@Override
	public boolean isSiblingRole(Role roleA, Role roleB)
			throws DotSecurityException, DotDataException {

		// findRoleHierarchy return the hierarchy INCLUDING same role
		// so we need to remove it from the list before checking.

		final List<Role> roleAHierarchy = findRoleHierarchy(roleA);
		roleAHierarchy.remove(roleA);

		final List<Role> roleBHierarchy = findRoleHierarchy(roleB);
		roleBHierarchy.remove(roleB);

		return roleAHierarchy.equals(roleBHierarchy);
	}

	@Override
	public List<Role> findWorkflowSpecialRoles() throws DotSecurityException, DotDataException {
		ImmutableList.Builder<Role> roleList = new ImmutableList.Builder<>();

		final Role anyWhoCanView = APILocator.getRoleAPI().loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY);
		if (null != anyWhoCanView) {
			roleList.add(anyWhoCanView);
		}

		final Role anyWhoCanEdit = APILocator.getRoleAPI().loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
		if (null != anyWhoCanEdit) {
			roleList.add(anyWhoCanEdit);
		}

		final Role anyWhoCanPublish = APILocator.getRoleAPI().loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
		if (null != anyWhoCanPublish) {
			roleList.add(anyWhoCanPublish);
		}

		final Role anyWhoCanEditPermissions = APILocator.getRoleAPI().loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY);
		if (null != anyWhoCanEditPermissions) {
			roleList.add(anyWhoCanEditPermissions);
		}

		return roleList.build();
	}

}
