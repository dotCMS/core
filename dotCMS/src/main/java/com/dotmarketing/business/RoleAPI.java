package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.List;

public interface RoleAPI {

	public final String SYSTEM_ROOT_ROLE_KEY = "System";
	public final String USERS_ROOT_ROLE_KEY = "cms_users";
	public final String WORKFLOW_ADMIN_ROLE_KEY = "cms_workflow_admin";
	public final String DEFAULT_USER_ROLE_KEY = "dotcms.org.default";
	public final String WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY = "cms_workflow_any_who_can_view";
	public final String WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY = "cms_workflow_any_who_can_edit";
	public final String WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY = "cms_workflow_any_who_can_publish";
	public final String WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY = "cms_workflow_any_who_can_edit_permissions";

	/**
	 * Will retrieve all roles.
	 * @param showSystemRoles
	 * @throws DotDataException
	 * @return
	 */
	 List<Role> findAllAssignableRoles(boolean showSystemRoles) throws DotDataException;
	
	/**
	 * 
	 * @param roleId
	 * @return
	 * @throws DotDataException
	 */
	 Role loadRoleById(String roleId) throws DotDataException;
	
	/**
	 * Retrieves all roles assigned to the user including the roles implicitly inherited
	 * @param userId
	 * @return
	 * @throws DotDataException
	 */
	 List<Role> loadRolesForUser(String userId) throws DotDataException;
	
	
	/**
	 * Retrieves all roles assigned to the user, if includeImplicitRoles is passed false then it will only return
	 * the roles directly assigned to the user and not the implicitly inherited from the role hierarchy
	 * 
	 * @param userId
	 * @param includeImplicitRoles
	 * @return
	 * @throws DotDataException
	 */
	 List<Role> loadRolesForUser(String userId, boolean includeImplicitRoles) throws DotDataException;
	
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
	 List<Role> findRolesByNameFilter(String filter, int start,int limit) throws DotDataException;

	/**
	 * This method returns the requested role based on the role id passed
	 * @param rolename
	 * @param parent Can be null if it is a root role
	 * @return
	 * @throws DotDataException
	 */
	 Role findRoleByName(String rolename, Role parent) throws DotDataException;
	
	
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
     List<Role> findRolesByFilterLeftWildcard(String filter, int start,int limit) throws DotDataException ;
    	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	 void addRoleToUser(String roleId, User user) throws DotDataException, DotStateException;
	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	 void addRoleToUser(Role role, User user) throws DotDataException, DotStateException;
	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	 void removeRoleFromUser(Role role, User user) throws DotDataException, DotStateException;
	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	 void removeAllRolesFromUser(User user) throws DotDataException, DotStateException;
	
	/**
	 * 
	 * @param roleName
	 * @param parent Can be null if it is a root role
	 * @return
	 * @throws DotDataException
	 */
	 boolean roleExistsByName(String roleName, Role parent) throws DotDataException;
	
	/**
	 * Persist the given role in db
	 * @param Role
	 * @author Jason Tesser
	 * @throws DotDataException 
	 * @throws DotStateException
	 */
	 Role save(Role role) throws DotDataException, DotStateException;
	
	/**
	 * Persist the given role in db with the given id
	 * @param role Role
	 * @author Jason Tesser
	 * @throws DotDataException 
	 * @throws DotStateException
	 */
	 Role save(Role role, String existingId) throws DotDataException, DotStateException;
	
	/**
	 * Find all top level roles excluding the role key RoleAPI.USERS_ROOT_ROLE_KEY (cms_users). 
	 * The cms_users is the place holder for user roles and is not typically needed for the pull of root roles
	 * this method will hit the database
	 * @return
	 * @throws DotDataException
	 */
	 List<Role> findRootRoles() throws DotDataException;
	
	/**
	 * Will return whether a user has a specific role or not
	 * @param user
	 * @param role
	 * @return
	 * @throws DotDataException 
	 */
	 boolean doesUserHaveRole(User user, Role role) throws DotDataException;
	
	/**
	 * Will return whether a user has a specific role or not
	 * @param user
	 * @param roleId
	 * @return
	 * @throws DotDataException 
	 */
	 boolean doesUserHaveRole(User user, String roleId) throws DotDataException;

    /**
     * Removes a given Role.
     * <br>In order to remove a Role it is required first to remove the association
     * of this Role with any user under it, remove its permissions and finally remove its layouts.
     *
     * @param role
     * @throws DotDataException
     * @throws DotStateException
     */
     void delete ( Role role ) throws DotDataException, DotStateException;

    /**
	 * Loads the CMS Anonymous Role
	 * @return
	 * @throws DotDataException
	 */
	 Role loadCMSAnonymousRole() throws DotDataException;
	
	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	 Role loadLoggedinSiteRole() throws DotDataException;
	
	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	 Role loadCMSOwnerRole() throws DotDataException;
	
	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	 Role loadCMSAdminRole() throws DotDataException;

    /**
     * Returns deafult Role
     *
     * @return default role
     * @throws DotDataException
     */
     Role loadDefaultRole () throws DotDataException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 */
	 List<String> loadLayoutIdsForRole(Role role) throws DotDataException;
	
	/**
	 * Add a layout to a role
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	 void addLayoutToRole(Layout layout, Role role) throws DotDataException, DotStateException;

	/**
	 * Returns true if the layout is already related to the role, otherwise false
	 * @param layout {@link Layout}
	 * @param role  {@link Role}
	 * @return boolean
	 */
		 boolean roleHasLayout(Layout layout, Role role);
	
	/**
	 * Remove a layout from a role
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	 void removeLayoutFromRole(Layout layout, Role role) throws DotDataException, DotStateException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws NoSuchUserException 
	 */
	 List<User> findUsersForRole(Role role) throws DotDataException, NoSuchUserException, DotSecurityException;
	
	/**
	 * 
	 * @param role
	 * @param inherited users that have inherited this role
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws NoSuchUserException 
	 */
	 List<User> findUsersForRole(Role role, boolean inherited) throws DotDataException, NoSuchUserException, DotSecurityException;
	
	/**
	 * Returns the list of roles above this role in the role tree
	 * @param role
	 * @return
	 * @throws DotDataException
	 * @throws NoSuchUserException
	 * @throws DotSecurityException
	 */
	 List<Role> findRoleHierarchy(Role role) throws DotDataException, NoSuchUserException, DotSecurityException;

	
	/**
	 * 
	 * @param roleId
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws NoSuchUserException 
	 */
	 List<User> findUsersForRole(String roleId) throws DotDataException, NoSuchUserException, DotSecurityException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 */
	 List<String> findUserIdsForRole(Role role) throws DotDataException;
	
	/**
	 * 
	 * @param FQN
	 * @return
	 * @throws DotDataException
	 */
	 Role findRoleByFQN(String FQN) throws DotDataException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 */
	 void lock(Role role) throws DotDataException;
	
	/**
	 * 
	 * @param role
	 * @return
	 * @throws DotDataException
	 */
	 void unLock(Role role) throws DotDataException;
	
	/**
	 * 
	 * @param key
	 * @return
	 * @throws DotDataException
	 */
	 Role loadRoleByKey(String key) throws DotDataException;

	/**
	 * Retrieves the special role for user
	 * @param user
	 * @return
	 * @throws DotDataException 
	 */
	 Role getUserRole(User user) throws DotDataException;


	/**
	 * Verifies that a user is assigned to one of the specified role IDs. It is
	 * not guaranteed that this method will traverse the full list of roles.
	 * Once it finds a role that is associated to the user, it will return.
	 *
	 * @param userId
	 *            - The ID of the user going through role verification.
	 * @param roleIds
	 *            - A list of role IDs to check the user.
	 * @return If the user is associated to at least one role ID, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 */
	 boolean doesUserHaveRoles(String userId, List<String> roleIds);

    /**
     * Checks if the first role parameter is indeed a parent of the second role parameter.
     *
     * @return true is first parameter is parent of second.
     */
    boolean isParentRole(Role parent, Role child) throws DotSecurityException, DotDataException;

	/**
	 * Checks whether the first role parameter is indeed a sibling of the second role parameter, by
	 * checheking if they have the same role hierarchy.
	 *
	 * @return true is first parameter is sibling of second.
	 */
	boolean isSiblingRole(Role roleA, Role roleB)
			throws DotSecurityException, DotDataException;

	/**
	 * Get the list of roles exclusive for workflows
	 * (Any who can view, Any who can Edit, Any who can Publish and Any who canEdit Permissions)
	 * @return List of workflows
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	 List<Role> findWorkflowSpecialRoles() throws DotSecurityException, DotDataException;
	/**
	 * Returns the back end user role
	 * @return
	 * @throws DotDataException
	 */
	 Role loadBackEndUserRole() throws DotDataException;
  
  /**
   * Returns the front end user role
   * @return
   * @throws DotDataException
   */
	 Role loadFrontEndUserRole() throws DotDataException;

}
