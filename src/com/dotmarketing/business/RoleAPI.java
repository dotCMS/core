package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

public interface RoleAPI {

	public final String SYSTEM_ROOT_ROLE_KEY = "System";
	public final String USERS_ROOT_ROLE_KEY = "cms_users";
	public final String WORKFLOW_ADMIN_ROLE_KEY = "cms_workflow_admin";
	public final String DEFAULT_USER_ROLE_KEY = "dotcms.org.default";

	/**
	 * Will retrieve all roles.
	 * @param showSystemRoles
	 * @throws DotDataException
	 * @return
	 */
	public List<Role> findAllAssignableRoles(boolean showSystemRoles) throws DotDataException;
	
	/**
	 * 
	 * @param roleId
	 * @return
	 * @throws DotDataException
	 */
	public Role loadRoleById(String roleId) throws DotDataException;
	
	/**
	 * Retrieves all roles assigned to the user including the roles implicitly inherited
	 * @param userId
	 * @param user
	 * @param respectFrontEndRoles
	 * @return
	 * @throws DotDataException
	 */
	public List<Role> loadRolesForUser(String userId) throws DotDataException;
	
	
	/**
	 * Retrieves all roles assigned to the user, if includeImplicitRoles is passed false then it will only return
	 * the roles directly assigned to the user and not the implicitly inherited from the role hierarchy
	 * 
	 * @param userId
	 * @param includeImplicitRoles
	 * @return
	 * @throws DotDataException
	 */
	public List<Role> loadRolesForUser(String userId, boolean includeImplicitRoles) throws DotDataException;
	
	/**
	 * 
	 * This method returns a list of roles whose names are like the string passed in.
	 * @param filter compare string
	 * @param start First element to display. For a negative value, zero is assumed.
	 * @param limit Max number of elements to show. For a negative value, zero is assumed.
	 * @return List<Role> of Role entities.
	 * @throws DotRuntimeException, SystemException
	 * @version 1.9
	 * @since 1.9
	 * @author Jason Tesesr
	 * @throws DotDataException 
	 */
	public List<Role> findRolesByNameFilter(String filter, int start,int limit) throws DotDataException;

	/**
	 * This method returns the requested role based on the role id passed
	 * @param rolename
	 * @param parent Can be null if it is a root role
	 * @return
	 * @throws DotDataException
	 */
	public Role findRoleByName(String rolename, Role parent) throws DotDataException;
	
	
	/**
	 * 
	 * This method returns a list of roles whose names are like the string passed in.
	 * Left wildcarded, so "John S" would match "John Smith" but not "Mr. John Smith"
	 * @param filter compare string
	 * @param start First element to display. For a negative value, zero is assumed.
	 * @param limit Max number of elements to show. For a negative value, zero is assumed.
	 * @return List<Role> of Role entities.
	 * @throws DotRuntimeException, SystemException
	 * @version 1.9
	 * @since 1.9
	 * @author Jason Tesesr
	 * @throws DotDataException 
	 */
    public List<Role> findRolesByFilterLeftWildcard(String filter, int start,int limit) throws DotDataException ;
    	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public void addRoleToUser(String roleId, User user) throws DotDataException, DotStateException;
	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public void addRoleToUser(Role role, User user) throws DotDataException, DotStateException;
	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public void removeRoleFromUser(Role role, User user) throws DotDataException, DotStateException;
	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public void removeAllRolesFromUser(User user) throws DotDataException, DotStateException;
	
	/**
	 * 
	 * @param roleName
	 * @param parent Can be null if it is a root role
	 * @return
	 * @throws DotDataException
	 */
	public boolean roleExistsByName(String roleName, Role parent) throws DotDataException;
	
	/**
	 * Persist the given role in db
	 * @param Role
	 * @author Jason Tesser
	 * @throws DotDataException 
	 * @throws DotStateException
	 */
	public Role save(Role role) throws DotDataException, DotStateException;
	
	/**
	 * Find all top level roles. this method will hit the database
	 * @return
	 * @throws DotDataException
	 */
	public List<Role> findRootRoles() throws DotDataException;
	
	/**
	 * Will return whether a user has a specific role or not
	 * @param user
	 * @param role
	 * @return
	 * @throws DotDataException 
	 */
	public boolean doesUserHaveRole(User user, Role role) throws DotDataException;
	
	/**
	 * Will return whether a user has a specific role or not
	 * @param user
	 * @param roleId
	 * @return
	 * @throws DotDataException 
	 */
	public boolean doesUserHaveRole(User user, String roleId) throws DotDataException;
		
	
	/**
	 * 
	 * @param role
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException 
	 */
	public void delete(Role role) throws DotDataException, DotStateException, DotSecurityException;
		
	/**
	 * Loads the CMS Anonymous Role
	 * @return
	 * @throws DotDataException
	 */
	public Role loadCMSAnonymousRole() throws DotDataException;
	
	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public Role loadLoggedinSiteRole() throws DotDataException;
	
	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public Role loadCMSOwnerRole() throws DotDataException;
	
	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public Role loadCMSAdminRole() throws DotDataException;

    /**
     * Returns deafult Role
     *
     * @return default role
     * @throws DotDataException
     */
    public Role loadDefaultRole () throws DotDataException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 */
	public List<String> loadLayoutIdsForRole(Role role) throws DotDataException;
	
	/**
	 * Add a layout to a role
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public void addLayoutToRole(Layout layout, Role role) throws DotDataException, DotStateException;
	
	/**
	 * Remove a layout from a role
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public void removeLayoutFromRole(Layout layout, Role role) throws DotDataException, DotStateException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws NoSuchUserException 
	 */
	public List<User> findUsersForRole(Role role) throws DotDataException, NoSuchUserException, DotSecurityException;
	
	/**
	 * 
	 * @param role
	 * @param include users that have inherited this role
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws NoSuchUserException 
	 */
	public List<User> findUsersForRole(Role role, boolean inherited) throws DotDataException, NoSuchUserException, DotSecurityException;
	
	/**
	 * Returns the list of roles above this role in the role tree
	 * @param role
	 * @return
	 * @throws DotDataException
	 * @throws NoSuchUserException
	 * @throws DotSecurityException
	 */
	public List<Role> findRoleHierarchy(Role role) throws DotDataException, NoSuchUserException, DotSecurityException;

	
	/**
	 * 
	 * @param roleId
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws NoSuchUserException 
	 */
	public List<User> findUsersForRole(String roleId) throws DotDataException, NoSuchUserException, DotSecurityException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 */
	public List<String> findUserIdsForRole(Role role) throws DotDataException;
	
	/**
	 * 
	 * @param FQN
	 * @return
	 * @throws DotDataException
	 */
	public Role findRoleByFQN(String FQN) throws DotDataException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 */
	public void lock(Role role) throws DotDataException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 */
	public void unLock(Role role) throws DotDataException;
	
	/**
	 * 
	 * @param key
	 * @return
	 * @throws DotDataException
	 */
	public Role loadRoleByKey(String key) throws DotDataException;

	/**
	 * Retrieves the special role for user
	 * @param user
	 * @return
	 * @throws DotDataException 
	 */
	public Role getUserRole(User user) throws DotDataException;
	
}