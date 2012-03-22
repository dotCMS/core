package com.dotmarketing.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public abstract class PermissionFactory {

	/**
	 * This method returns all the permission type masks configured in the system
	 * @return 
	 * @version 1.7
	 * @since 1.7
	 */
	abstract protected Map<String, Integer> getPermissionTypes();
	
	/**
	 * This method returns the permission by id
	 * @param x permision id
	 * @return Permision
	 * @version 1.0
	 * @since 1.0
	 */
	abstract protected Permission getPermission(String x);

	/**
	 * This method returns a list of all permission that could be propagated 
	 * from this permissionable to its children, returns the list in the compress
	 * bit format
	 * 
	 * @param permissionableId The object id from where we going to take the permissions
	 * @return List<Permission>
	 * @version 1.9
	 * @since 1.9
	 * @author David H Torres
	 * @throws DotDataException 
	 */
	abstract protected List<Permission> getInheritablePermissions(Permissionable permissionable) throws DotDataException;

	/**
	 * This method returns a list of all permission that could be propagated 
	 * from this permissionable to its children, returns the list in the old
	 * one entry per permission or in the compress bit format depending
	 * on the bitPermissions parameter. Returns null or empty if the given 
	 * permissionable is not a parent permissionable that other can inherit from
	 * 
	 * @param permissionableId The object id from where we going to take the permissions
	 * @param bitPermissions if true returns the compressed bit version of permissions
	 * @return List<Permission>
	 * @version 1.9
	 * @since 1.9
	 * @author David H Torres
	 */
	abstract protected List<Permission> getInheritablePermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException;

	/**
	 * This method returns a list of all permission that could be propagated 
	 * from this permissionable to its children. 
	 * 
	 * Returns null or empty if the given permissionable is not a parent permissionable that others can inherit from
	 * 
	 * Returns the bit representation of permissions
	 * 
	 * @param permissionableId The object id from where we going to take the permissions
	 * @param bitPermissions if true returns the compressed bit version of permissions
	 * @return List<Permission>
	 * @version 1.9
	 * @since 1.9
	 * @author David H Torres
	 */
	abstract protected List<Permission> getInheritablePermissions(Permissionable permissionable, String type) throws DotDataException;
	
	/**
	 * This method returns a list of all the permission the object have associated,
	 * it returns the compressed bit version of permissions, this method looks for
	 * individually assigned permissions if none are found it tries to look
	 * up in the chain for inherited permissions
	 * 
	 * @param permissionableId The object id from where we going to take the permissions
	 * @return List<Permission>
	 * @version 1.7
	 * @throws DotDataException 
	 * @since 1.0
	 */
	abstract protected List<Permission> getPermissions(Permissionable permissionable) throws DotDataException;

	/**
	 * This method returns a list of all the permission the object have associated, this
	 * method looks for permission associated directly to the asset if none then it looks
	 * for parent inherited permissions
	 * 
	 * @param permissionableId
	 * @param bitPermissions if true returns the compressed bit version of permissions
	 * @return
	 * @version 1.7
	 * @since 1.7
	 */
	abstract protected List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException;
	
	/**
	 * This method returns a list of all the permission the permissionable have associated, this
	 * method looks for permission associated directly to the asset if none then it looks
	 * for parent inherited permissions if the onlyIndividualPermissions is set to false
	 * 
	 * @param permissionableId
	 * @param bitPermissions
	 * @param onlyIndividualPermissions if true it only returns the list of individually assigned permission
	 * @return
	 * @throws DotDataException 
	 * @throws DotDataException
	 * @version 1.9
	 * @since 1.9
	 * @author David H Torres
	 */
	abstract List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions) throws DotDataException;
	
	/**
	 * This method returns a list permission of all the permissionables passed as parameter,
	 * it returns the compressed bit version of permissions, this method returns either the 
	 * individually assigned permissions if none then it looks for any inherited permissions
	 * 
	 * @param permissionableIds
	 * @return A map of permisionable vs. permissions list
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @version 1.7
	 * @since 1.0
	 */
	abstract  protected Map<Permissionable, List<Permission>> getPermissions(List<Permissionable> permissionables)
		throws DotDataException, DotSecurityException;

	/**
	 * This method returns a list permission of all the permissionables passed as parameter,
	 * this method returns either the 
	 * individually assigned permissions if none then it looks for any inherited permissions
	 * 
	 * @param permissionables
	 * @param bitPermission if true it retrieves the compressed bit permissions version
	 * @return A map of permisionable vs. permissions list
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @version 1.7
	 * @since 1.7
	 */
	abstract protected Map<Permissionable, List<Permission>> getPermissions(List<Permissionable> permissionables, boolean bitPermission) 
		throws DotDataException, DotSecurityException;

	/**
	 * This method returns a list of all the permission the permissionable have associated, this
	 * method looks for permission associated directly to the asset if none then it looks
	 * for parent inherited permissions if the onlyIndividualPermissions is set to false
	 * 
	 * @param permissionableId
	 * @param bitPermissions
	 * @param onlyIndividualPermissions if true it only returns the list of individually assigned permission
	 * @param forceLoadFromDB if true forces the load of permissions from DB
	 * @return
	 * @throws DotDataException
	 * @version 1.9
	 * @since 1.9
	 * @author David H Torres
	 */
	abstract List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions, boolean forceLoadFromDB) throws DotDataException;

	/**
	 * This method saves or update the permission object in db
	 * @param p permission
	 * @version 1.7
	 * @throws DotDataException 
	 * @since 1.0
	 */
	abstract protected Permission savePermission(Permission p, Permissionable permissionable) throws DotDataException;

	/**
	 * Assigns a set of permissions to a given asset, any permissions already assigned to the asset are either updated or removed to match the provided list
	 * @param permissions
	 * @param permissionable
	 * @throws DotDataException
	 */
	abstract void assignPermissions(List<Permission> permissions, Permissionable permissionable) throws DotDataException;
	
	/**
	 * Gets a list of users that have the specified permission over the permissionable object
	 * Use this method with caution it always hits database
	 * 
	 * @param permissionableId
	 * @param permissionType type of permission (READ, WRITE OR PUBLISH)
	 * @param filter compare string
	 * @param start first element to show
	 * @param limit max number of element to show
	 * @return List<User>
	 * @version 1.9
	 * @since 1.5.1
	 */
	abstract protected List<User> getUsers(Permissionable permissionable, int permissionType, String filter, int start, int limit);

	/**
	 * Counts how many ocurrences of users that have the specified permission over the inode object.
	 * @param permissionableId
	 * @param permissionType type of permission (READ, WRITE OR PUBLISH)
	 * @param filter compare string
	 * @return An integer indicating the number of users.
	 * @version 1.7
	 * @since 1.5.1
	 */
	abstract protected int getUserCount(Permissionable permissionable, int permissionType, String filter);
	
	/**
	 * This method returns a list of all the permission the object have associated from the cache.
	 * This method will not goto the database.  The result will be null if not in the cache
	 * @param permissionableId The object id from where we going to take the permissions
	 * @return List<Permission>
	 * @version 1.6
	 * @since 1.6
	 */
	abstract protected List<Permission> getPermissionsFromCache(String permissionableId);

	/**
	 * Removes all individual permissions associated to the given permissionable
	 * it also removes all inheritable permissions if the permissionable is instance of a 
	 * ParentPermissionable
	 * 
	 * @param roleId
	 * @version 1.0
	 * @throws DotDataException 
	 * @since 1.0
	 */
	abstract void removePermissions(Permissionable permissionable) throws DotDataException;

	/**
	 * Removes all individual permissions associated to the given permissionable
	 * it also removes all inheritable permissions if includeInheritablePermissions is
	 * set to true
	 * 
	 * @param roleId
	 * @version 1.0
	 * @throws DotDataException 
	 * @since 1.0
	 */
	abstract void removePermissions(Permissionable permissionable, boolean includeInheritablePermissions) throws DotDataException;

	/**
	 * Removes all permissions associated to the given role
	 * @param roleId
	 * @version 1.0
	 * @since 1.0
	 */
	abstract void removePermissionsByRole(String roleId);

	/**
	 * This method updates the given permissionable owner with the given user id
	 * @param asset
	 * @param ownerId
	 * @throws DotDataException 
	 * @author David H Torres
	 */
	abstract void updateOwner(Permissionable asset, String ownerId) throws DotDataException;

	/**
	 * Returns the binary combination of all permissions
	 */
	abstract int maskOfAllPermissions();

	/**
	 * Retrieves all permissions associated to a given role
	 * @param role
	 * @param onlyFoldersAndHosts 
	 * @return
	 * @throws DotDataException 
	 */
	abstract List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts, boolean bitPermission) throws DotDataException;

	/**
	 * Recursively removes any individual or inheritable permissions on children of the given permissionable 
	 * @param parent
	 * @throws DotDataException 
	 */
	abstract void resetPermissionsUnder(Permissionable parent) throws DotDataException;

	/**
	 * 
	 * @param permissionable
	 * @param role
	 * @throws DotDataException
	 */
	abstract void cascadePermissionUnder(Permissionable permissionable, Role role) throws DotDataException;

	/**
	 * Removes the given permissionable permission references forcing the api to recalculate the reference
	 * @param perm
	 * @throws DotDataException 
	 * @throws DotDataException
	 */
	abstract void resetPermissionReferences(Permissionable perm) throws DotDataException;

	
	/**
	 * Removes the permission references of all content children of the given structure
	 * @param perm
	 * @throws DotDataException 
	 * @throws DotDataException
	 */
	
	abstract void resetChildrenPermissionReferences(Structure structure) throws DotDataException;

	
	/**
	 * Removes all permission references
	 * @throws DotDataException
	 */
	abstract void resetAllPermissionReferences() throws DotDataException;
	
	
	 /**
     * 
     * @param <P>
     * @param permissionables
     * @param requiredTypePermission
     * @param respectFrontendRoles
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
	abstract <P extends Permissionable> List<P> filterCollectionByDBPermissionReference(List<P> permissionables, int requiredTypePermission,boolean respectFrontendRoles, User user) throws DotDataException, DotSecurityException;

	abstract boolean isInheritingPermissions(Permissionable permissionable) throws DotDataException;

}
