/**
 * 
 */
package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 *
 */
public abstract class RoleFactory {

	/**
	 * Will retrieve all roles.
	 * @param showSystemRoles
	 * @throws DotDataException
	 * @return
	 */
	protected abstract List<Role> findAllAssignableRoles(boolean showSystemRoles) throws DotDataException;
	
	/**
	 * 
	 * @param userId
	 * @param user
	 * @param respectFrontEndRoles
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<Role> loadRolesForUser(String userId) throws DotDataException;
	
	protected abstract Role getRoleById(String roleId) throws DotDataException;
	
	protected abstract List<Role> getRolesByName(String filter, int start,int limit) throws DotDataException;
	
	protected abstract void delete(Role object) throws DotDataException;
	
	protected abstract Role findRoleByName(String rolename, Role parent) throws DotDataException;
	
	protected abstract void addRoleToUser(Role role, User user) throws DotDataException;
	
	protected abstract Role save(Role role) throws DotDataException;
	
	protected abstract List<Role> getRolesByNameFiltered(String filter, int start, int limit) throws DotDataException;

	/**
	 * Find all top level roles. Find all top level roles. this method will hit the database
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<Role> findRootRoles() throws DotDataException;
	
	/**
	 * Will return whether a user has a specific role or not
	 * @param user
	 * @param role
	 * @return
	 * @throws DotDataException 
	 */
	protected abstract boolean doesUserHaveRole(User user, Role role) throws DotDataException;
	
	protected abstract List<String> findUserIdsForRole(Role role) throws DotDataException;
	
	protected abstract List<String> loadLayoutIdsForRole(Role role) throws DotDataException;
	
	protected abstract void addLayoutToRole(Layout layout, Role role) throws DotDataException;
	
	protected abstract void removeLayoutFromRole(Layout layout, Role role) throws DotDataException;
	
	protected abstract Role findRoleByFQN(String FQN) throws DotDataException;
	
	/**
	 * Add a role to a user
	 * @param role
	 * @param user
	 * @throws DotDataException
	 */
	protected abstract void removeRoleFromUser(Role role, User user) throws DotDataException;
	
	/**
	 * 
	 * @param key
	 * @return
	 * @throws DotDataException
	 */
	protected abstract Role loadRoleByKey(String key) throws DotDataException;

	protected abstract  Role addUserRole(User user) throws DotDataException;
}