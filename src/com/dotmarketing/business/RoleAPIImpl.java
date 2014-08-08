package com.dotmarketing.business;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jason Tesser
 * RoleAPI is an API intended to be a helper class for class to get Role entities.  Classes within the dotCMS
 * should use this API for role management. 
 */
public class RoleAPIImpl implements RoleAPI {

	private RoleFactory rf = FactoryLocator.getRoleFactory();
	private Role CMS_ADMIN = null;
	private Role CMS_ANON = null;
	private Role CMS_OWNER = null;
	private Role LOGGEDIN_SITE_USER = null;
	
	public RoleAPIImpl()  {
		
	}

	public List<Role> findAllAssignableRoles(boolean showSystemRoles) throws DotDataException {
		return rf.findAllAssignableRoles(showSystemRoles);
	}
	
	public Role loadRoleById(String roleId) throws DotDataException {
		return rf.getRoleById(roleId);
	}
	
	public List<Role> loadRolesForUser(String userId) throws DotDataException {
		return loadRolesForUser(userId, true);
	}
	

	public List<Role> loadRolesForUser(String userId, boolean includeImplicitRoles)
			throws DotDataException {
		return rf.loadRolesForUser(userId,includeImplicitRoles);
	}
	
    /* (non-Javadoc)
	 * @see com.dotmarketing.business.RoleAPI#getRolesByName(java.lang.String, int, int)
	 */
    public List<Role> findRolesByNameFilter(String filter, int start,int limit) throws DotDataException {
    	return rf.getRolesByName(filter, start, limit);
    }

	public List<User> findUsersForRole(Role role, boolean inherited) throws DotDataException, NoSuchUserException, DotSecurityException{
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
    public List<Role> findRolesByFilterLeftWildcard(String filter, int start,int limit) throws DotDataException {
    	
		if(filter !=null)
			filter = filter.toLowerCase() + "%";

    	return rf.getRolesByNameFiltered(filter, start, limit);
    }
    
    public Role findRoleByName(String rolename, Role parent) throws DotDataException {
    	return rf.findRoleByName(rolename, parent);
    }

    public void delete ( Role role ) throws DotDataException, DotStateException {

        Role r = loadRoleById( role.getId() );

        for ( String uid : rf.findUserIdsForRole( role, true ) ) {
            CacheLocator.getRoleCache().remove( uid );
        }

        if ( r.isLocked() ) {
            throw new DotStateException( "Cannot delete locked role" );
        }
        if ( r.isSystem() ) {
            throw new DotStateException( "Cannot edit a system role" );
        }

        boolean local = false;
        try {
            try {
                local = HibernateUtil.startLocalTransactionIfNeeded();
            } catch ( DotDataException e ) {
                Logger.error( this.getClass(), e.getMessage(), e );
                throw new DotHibernateException( "Unable to start a local transaction " + e.getMessage(), e );
            }

            r.setEditPermissions( true );
            r.setEditLayouts( true );
            r.setEditUsers( true );
            rf.save( r );

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
            rf.delete( role );
            
            if ( local ) {
                HibernateUtil.commitTransaction();
            }
        } catch ( Exception e ) {
            if ( local ) {
                HibernateUtil.rollbackTransaction();
            }
            if ( role != null ) {
                Logger.error( this.getClass(), "Error deleting Role: " + role.getName(), e );
            } else {
                Logger.error( this.getClass(), "Error deleting Role", e );
            }
            throw new DotDataException( e.getMessage() );
        }
    }

	public boolean roleExistsByName(String roleName, Role parent) throws DotDataException {
		Role r = rf.findRoleByName(roleName, parent);
		if(r == null){
			return false;
		}
		return true;
	}
	
	public void addRoleToUser(Role role, User user) throws DotDataException, DotStateException {
		Role r = loadRoleById(role.getId());
		if(!r.isEditUsers()){
			throw new DotStateException("Cannot alter users on this role.  Name:" + role.getName() + ", id:" + role.getId());
		}
		rf.addRoleToUser(role, user);
	}
	
	public void addRoleToUser(String roleId, User user)	throws DotDataException, DotStateException {
		Role r = loadRoleById(roleId);
		addRoleToUser(r, user);
	}

	public Role save(Role role) throws DotDataException, DotStateException {
		if(InodeUtils.isSet(role.getId())) {
			Role r = loadRoleById(role.getId());
			if(r.isSystem() || r.isLocked() || role.isSystem()){
				throw new DotStateException("Cannot save locked or system role");
			}
		} else {
			Logger.debug(this, "assuming is a new role checking if locked or system");
			if(role.isSystem() || role.isLocked()){
				throw new DotStateException("Cannot save locked or system role");
			}
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
			roleParent = rf.getRoleById(role.getParent());
		}
		Role dupRole = rf.findRoleByName(role.getName(), roleParent);
		if(dupRole != null && !dupRole.getId().equals(role.getId()))
			throw new DuplicateRoleException("A role with id = " + dupRole.getId() + " and name = " + dupRole.getName());
			
		
		return rf.save(role);
	}

	public List<Role> findRootRoles() throws DotDataException {
		return rf.findRootRoles();
	}
	
	public boolean doesUserHaveRole(User user, Role role) throws DotDataException {
		return rf.doesUserHaveRole(user, role);
	}
	
	public boolean doesUserHaveRole(User user, String roleId) throws DotDataException {
		Role role = rf.getRoleById(roleId);
		return doesUserHaveRole(user, role);
	}
	
	public Role loadCMSAnonymousRole() throws DotDataException {
		if(CMS_ANON == null){
			CMS_ANON =  rf.loadRoleByKey(Config.getStringProperty("CMS_ANONYMOUS_ROLE"));
		}
		return CMS_ANON;
	}
	
	public Role loadCMSOwnerRole() throws DotDataException {
		if(CMS_OWNER == null){
			CMS_OWNER =  rf.loadRoleByKey(Config.getStringProperty("CMS_OWNER_ROLE"));
		}
		return CMS_OWNER;
	}
	
	public Role loadLoggedinSiteRole() throws DotDataException {
		if(LOGGEDIN_SITE_USER == null){
			LOGGEDIN_SITE_USER =  rf.loadRoleByKey(Config.getStringProperty("CMS_LOGGED_IN_SITE_USER_ROLE"));
		}
		return LOGGEDIN_SITE_USER;
	}
	
	public Role loadCMSAdminRole() throws DotDataException {
		if(CMS_ADMIN == null){
			CMS_ADMIN = rf.loadRoleByKey(Config.getStringProperty("CMS_ADMINISTRATOR_ROLE"));
		}
		return CMS_ADMIN;		
	}

    public Role loadDefaultRole () throws DotDataException {
        return rf.loadRoleByKey( DEFAULT_USER_ROLE_KEY );
    }
	
	public List<String> findUserIdsForRole(Role role) throws DotDataException {
		return rf.findUserIdsForRole(role);
	}
	
	public List<User> findUsersForRole(Role role) throws DotDataException, NoSuchUserException, DotSecurityException {
		List<String> uids = findUserIdsForRole(role);
		List<User> users = new ArrayList<User>();
		for (String uid : uids)
			users.add(APILocator.getUserAPI().loadUserById(uid, APILocator.getUserAPI().getSystemUser(), true));
		return users;
	}
	
	public List<User> findUsersForRole(String roleId) throws DotDataException, NoSuchUserException, DotSecurityException {
		Role role = rf.getRoleById(roleId);
		return findUsersForRole(role);
	}
	
	public List<String> loadLayoutIdsForRole(Role role) throws DotDataException {
		return rf.loadLayoutIdsForRole(role);
	}
	
	public void addLayoutToRole(Layout layout, Role role) throws DotDataException, DotStateException {
		Role r = loadRoleById(role.getId());
		if(!r.isEditLayouts()){
			throw new DotStateException("Cannot alter layouts on this role");
		}
		rf.addLayoutToRole(layout, role);
	}
	
	public void removeLayoutFromRole(Layout layout, Role role) throws DotDataException, DotStateException {
		Role r = loadRoleById(role.getId());
		if(!r.isEditLayouts()){
			throw new DotStateException("Cannot alter layouts on this role");
		}
		rf.removeLayoutFromRole(layout, role);	
	}
	
	public Role findRoleByFQN(String FQN) throws DotDataException {
		return rf.findRoleByFQN(FQN);
	}
	public void removeRoleFromUser(Role role, User user) throws DotDataException, DotStateException {
		Role r = loadRoleById(role.getId());
		rf.removeRoleFromUser(role, user);
	}
	

	public void removeAllRolesFromUser(User user) throws DotDataException,
			DotStateException {
		List<Role> roles = loadRolesForUser(user.getUserId(), false);
		for(Role role : roles) {
			removeRoleFromUser(role, user);
		}
	}	
	
	public void lock(Role role) throws DotDataException {
		Role r = loadRoleById(role.getId());
		//if(r.isSystem())
		//	throw new DotStateException("Cannot lock a system role");
		r.setLocked(true);
		rf.save(role);
	}
	
	public void unLock(Role role) throws DotDataException {
		Role r = loadRoleById(role.getId());
		//if(r.isSystem())
		//	throw new DotStateException("Cannot unlock a system role");
		r.setLocked(false);
		rf.save(r);
	}
	
	public Role loadRoleByKey(String key) throws DotDataException {
		return rf.loadRoleByKey(key);
	}

	public Role getUserRole(User user) throws DotDataException {
		Role role = loadRoleByKey(user.getUserId());
		if(role == null) {
			role = rf.addUserRole(user);
		} else if(!role.getName().equals(user.getFullName()) && !role.getName().equalsIgnoreCase("System")) {
			role.setName(user.getFullName());
			rf.save(role);
		}
		if(!APILocator.getRoleAPI().doesUserHaveRole(user, role)){
			rf.addRoleToUser(role, user);
		}
		return role;
	}


	
}
