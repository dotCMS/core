package com.dotmarketing.business;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public interface PermissionAPI {

	// Default Permission keys for categories


	// Default Permission keys for assets
	final int PERMISSION_READ = 1;
	final int PERMISSION_USE = 1;
	final int PERMISSION_EDIT = 2;
	final int PERMISSION_WRITE = 2;
	final int PERMISSION_PUBLISH = 4;
	final int PERMISSION_EDIT_PERMISSIONS = 8;
	final int PERMISSION_CAN_ADD_CHILDREN = 16;
	final int PERMISSION_CREATE_VIRTUAL_LINKS = 32;


	final static String[] PERMISSION_TYPES={"PERMISSION_READ","PERMISSION_CREATE_VIRTUAL_LINKS","PERMISSION_WRITE","PERMISSION_PUBLISH","PERMISSION_USE","PERMISSION_CAN_ADD_CHILDREN","PERMISSION_EDIT","PERMISSION_EDIT_PERMISSIONS","PERMISSION_CREATE_VIRTUAL_LINKS"};



	//Permission types
	String INDIVIDUAL_PERMISSION_TYPE = "individual";

	static Map<String, String> permissionTypes= new HashMap<String, String>(){{
		put("HTMLPAGES", HTMLPage.class.getCanonicalName());
		put("CONTAINERS", Container.class.getCanonicalName());
		put("FILES", File.class.getCanonicalName());
		put("FOLDERS", Folder.class.getCanonicalName());
		put("LINKS", Link.class.getCanonicalName());
		put("TEMPLATES", Template.class.getCanonicalName());
		put("STRUCTURES", Structure.class.getCanonicalName());
		put("CONTENTLETS", Contentlet.class.getCanonicalName());
   }};

   public enum PermissionableType {
	   HTMLPAGES(HTMLPage.class.getCanonicalName()),
	   CONTAINERS(Container.class.getCanonicalName()),
	   FILES(File.class.getCanonicalName()),
	   FOLDERS(Folder.class.getCanonicalName()),
	   LINKS(Link.class.getCanonicalName()),
	   TEMPLATES(Template.class.getCanonicalName()),
	   STRUCTURES(Structure.class.getCanonicalName()),
	   CONTENTLETS(Contentlet.class.getCanonicalName());

	   private final String canonicalName;

	   PermissionableType(String canonicalName) {
		   this.canonicalName = canonicalName;
	   }

	   public String getCanonicalName() {
		   return canonicalName;
	   }

   }




	/**
	 * This method returns all the permission type masks configured in the system
	 * @return
	 * @version 1.8
	 * @since 1.8
	 */
	Map<String, Integer> getPermissionTypes();

	/**
	 * Return true if the role has permission over the permissionable the specified permission
	 * @param permissionable
	 * @param permissionType
	 * @param role
	 * @return
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.0
	 * @deprecated respectFrontendRoles parameter does not makes sense and its been totally ignored. @see doesRoleHavePermission(Permissionable,int,Role)
	 */
	boolean doesRoleHavePermission(Permissionable permissionable, int permissionType, Role role, boolean respectFrontendRoles) throws DotDataException;

	/**
	 * Return true if the role has permission over the permissionable the specified permission
	 * @param permissionable
	 * @param permissionType
	 * @param role
	 * @return
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.0
	 */
	boolean doesRoleHavePermission(Permissionable permissionable, int permissionType, Role role) throws DotDataException;

	/**
	 * Return true if the user have over the permissionable the specified
	 * permission This method is meant to be used by frontend call because
	 * assumes that frontend roles should respected
	 *
	 * @param o permissionable
	 * @param permissionId
	 * @param user
	 * @return boolean
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.0
	 */
	boolean doesUserHavePermission(Permissionable permissionable, int permissionType, User user) throws DotDataException;

	/**
	 * Return true if the user have over the permissionable the specified
	 * permission
	 *
	 * @param o permissionable
	 * @param permissionId
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.5
	 */
	boolean doesUserHavePermission(Permissionable permissionable, int permissionType, User user, boolean respectFrontendRoles) throws DotDataException;

	/**
	 * Remove all individual permissions attached to the asset
	 * @param o permissionable
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.0
	 */
	void removePermissions(Permissionable permissionable) throws DotDataException;

	/**
	 * Sets the to the CMS_ADMINISTRATOR_ROLE permission over the permissionable
	 * @param permissionable
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.0
	 */
	void setDefaultCMSAdminPermissions(Permissionable permissionable) throws DotDataException;

	/**
	 * Sets reads the to the CMS_ANONYMOUS_ROLE permission over the permissionable
	 * @param permissionable
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.8
	 */
	void setDefaultCMSAnonymousPermissions(Permissionable permissionable) throws DotDataException;

	/**
	 * copies permissions from one permissionable to another
	 * @param from permissionable
	 * @param to permissionable
	 ** @version 1.0
	 * @throws DotDataException
	 * @since 1.0
	 */
	void copyPermissions(Permissionable from, Permissionable to) throws DotDataException;

	/**
	 * Retrieves the list of permissions associated to the given permissionable, either searching from
	 * individual permissions directly associated or permissions inheriting from a parent
 	 * This method returns permissions in the old format
	 * and not the compressed bit permissions format.
	 *
	 * @param permissionable
	 * @return List
	 * @version 1.7
	 * @throws DotDataException
	 * @since 1.5.0.1
	 */
	List<Permission> getPermissions(Permissionable permissionable) throws DotDataException;

	/**
	 * Retrieves the list of permissions associated to the given permissionable, either searching from
	 * individual permissions directly associated or permissions inheriting from a parent
 	 * If bitPermissions is set true
	 * this methods returns the permissions in the new bit compressed format so multiple permissions for the same role
	 * will be returned in one permission entry. E.G. read and write permissions for role X will be returned in a single
	 * permission object having role = X permission = read | write. @see com.dotmarketing.beans.Permission.matchesPermissionType to
	 * identify the permission itself and @see com.dotmarketing.business.PermissionAPI for the different kind of permissions
	 * managed by the system
	 *
	 * @param permissionable
	 * @param bitPermissions if true returns the new compressed bit permissions format, where multiple permissions can be stored
	 * in a single permission object
	 * @return List
	 * @version 1.7
	 * @since 1.7
	 * @author David H Torres
	 * @throws DotDataException
	 */
	List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException;

	/**
	 *
	 * Retrieves the list of permissions associated to the given permissionable, either searching from
	 * individual permissions directly associated or permissions inheriting from a parent
	 *
	 * @param permissionable
	 * @param bitPermissions if true returns the new compressed bit permissions format, where multiple permissions can be stored
	 * in a single permission object
	 * @param onlyIndividualPermissions If true it will only look for individually set permissions in the asset and not try to go and
	 * 									search through the chain of permissions inheritance
	 * @return List
	 * @throws DotDataException
	 * @since 1.9
	 * @author David H Torres
	 */
	List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions) throws DotDataException;

	/**
	 *
	 * Retrieves the list of permissions associated to the given permissionable, either searching from
	 * individual permissions directly associated or permissions inheriting from a parent
	 *
	 * @param permissionable
	 * @param bitPermissions if true returns the new compressed bit permissions format, where multiple permissions can be stored
	 * in a single permission object
	 * @param onlyIndividualPermissions If true it will only look for individually set permissions in the asset and not try to go and
	 * 									search through the chain of permissions inheritance
	 * @param forceLoadFromDB Forces to load from DB
	 * @return List
	 * @throws DotDataException
	 * @since 1.9
	 * @author David H Torres
	 */
	List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions, boolean forceLoadFromDB) throws DotDataException;

	/**
	 * Retrieves the list of permission that could be inherited from the given parent permissionable,
	 * @param permissionable
	 * @return
	 * @throws DotDataException
	 */
	List<Permission> getInheritablePermissions(Permissionable permissionable) throws DotDataException;

	/**
	 * Retrieves the list of ALL permission that could be inherited from the given parent and the
	 * parent's parent and so on until it hits the system_host
	 *
	 * @param permissionable
	 * @return
	 * @throws DotDataException
	 */
	List<Permission> getInheritablePermissionsRecurse(Permissionable permissionable) throws DotDataException;




	/**
	 * Retrieves the list of permission that could be inherited from the given parent permissionable
	 * @param permissionable
	 * @param bitPermissions if true the compact bit version of permissions will be returned
	 * @return
	 * @throws DotDataException
	 */
	List<Permission> getInheritablePermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException;

	/**
	 * Returns a set of role names whose has read access over a specific asset.
	 * This method returns a set of string.
	 *
	 * @param o	permissionable to get read roles for
	 * @return	set of role names
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.5
	 * @deprecated {@link PermissionAPI.getRolesWithPermission}
	 */
	Set<Role> getReadRoles(Permissionable permissionable) throws DotDataException;

	/**
	 * Returns a set of role names whose has read access over a specific asset.
	 * This method returns a set of string.
	 *
	 * @param o	permissionable to get read roles for
	 * @return	set of User names
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.5
	 * @deprecated {@link PermissionAPI.getUsersWithPermission}
	 */
	Set<User> getReadUsers(Permissionable permissionable) throws DotDataException;

	/**
	 * Returns a set of role names whose has publish access over a specific asset.
	 * This method returns a set of string.
	 *
	 * @param o	permissionable to get read roles for
	 * @return	set of role names
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.5
	 * @deprecated {@link PermissionAPI.getRolesWithPermission}
	 */
	Set<Role> getPublishRoles(Permissionable permissionable) throws DotDataException;

	/**
	 * Returns a set of role names whose has write access over a specific asset.
	 * This method returns a set of string.
	 *
	 * @param o	permissionable to get read roles for
	 * @return	set of role names
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.5
	 * @deprecated {@link PermissionAPI.getRolesWithPermission}
	 */
	Set<Role> getWriteRoles(Permissionable permissionable) throws DotDataException;

	/**
	 * Returns a set of role names whose has write access over a specific asset.
	 * This method returns a set of string.
	 *
	 * @param o	permissionable to get read roles for
	 * @return	set of Users
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.5
	 * @deprecated {@link PermissionAPI.getUsersWithPermission}
	 */
	Set<User> getWriteUsers(Permissionable permissionable) throws DotDataException;

	/**
	 * Retrieves the list of roles that have the given permission granted
	 * on the permissionable
	 *
	 * @param permissionable
	 * @param permission
	 * @return
	 * @version 1.9
	 * @since 1.9
	 * @author David H Torres
	 * @throws DotDataException
	 */
	Set<Role> getRolesWithPermission(Permissionable permissionable, int permission) throws DotDataException;

	/**
	 * Retrieves the list of users that have the given permission granted
	 * on the permissionable
	 *
	 * @param permissionable
	 * @param permission
	 * @return
	 * @version 1.9
	 * @since 1.9
	 * @author David H Torres
	 * @throws DotDataException
	 */
	Set<User> getUsersWithPermission(Permissionable permissionable, int permission) throws DotDataException;

	/**
	 * @param inode The inode to look for
	 * @param user The user to check against
	 * @return true if the given user owns the passed inode, false otherwise
	 * @version 1.8
	 * @since 1.5
	 */
	boolean doesUserOwn(Inode inode, User user) throws DotDataException;

	/**
	 * Get a map of all the permission that live page have
	 * @version 1.8
	 * @throws DotDataException
	 * @since 1.5
	 *
	 */
	// PERMISSION MAP METHODS!!!
	void mapAllPermissions() throws DotDataException;

	/**
	 * Retrieves the granted permissions  for a given list of roles, E.G. it returns READ, WRITE if the given roles
	 * have permissions to READ and WRITE on the given permissionable
	 *
	 * @param permissionable
	 * @param roles
	 * @return List<String> of PermissionIds
	 * @throws DotDataException
	 * @version 1.9
	 * @since 1.5
	 */
	List<Integer> getPermissionIdsFromRoles(Permissionable permissionable, Role[] roles, User user) throws DotDataException;

	/**
	 * Retrieves the granted permissions  for a given user, E.G. it returns READ, WRITE if any of the user roles
	 * have permissions to READ and WRITE on the given permissionable
	 *
	 * @param permissionable
	 * @param roles
	 * @return List<String> of PermissionIds
	 * @throws DotDataException
	 * @version 1.9
	 * @since 1.5
	 */
	List<Integer> getPermissionIdsFromUser(Permissionable permissionable, User user) throws DotDataException;

	/**
	 * Retrieves the roles associated with the given permissionable that poses permissionType access
	 * and filters them by name
	 *
	 * This method is intended for backend usage is does not check for frontend specific roles
	 * like cms anon o logged in site user
	 *
	 * @param permissionable
	 * @param permissionType
	 * @param filter
	 * @param start
	 * @param limit
	 * @return
	 * @since 1.6
	 * @version 1.8
	 */
	List<Role> getRoles(String permissionable, int permissionType, String filter, int start, int limit);

	/**
	 * Retrieves the roles associated with the given permissionable that poses permissionType access
	 * and filters them by name
	 *
	 * This method is intended for backend usage is does not check for frontend specific roles
	 * like cms anon o logged in site user
	 *
	 * @param permissionable
	 * @param permissionType
	 * @param filter
	 * @param start
	 * @param limit
	 * @return
	 * @since 1.6
	 * @version 1.8
	 */
	List<Role> getRoles(String permissionable, int permissionType, String filter, int start, int limit, boolean hideSystemRoles);


	/**
	 * Retrieves the count of roles associated with the given permissionable that poses permissionType access
	 * and filters them by name
	 *
	 * This method is intended for backend usage is does not check for frontend specific roles
	 * like cms anon o logged in site user
	 *
	 * @param permissionable
	 * @param permissionType
	 * @param filter
	 * @return
	 * @since 1.6
	 * @version 1.8
	 */
	int getRoleCount(String permissionable, int permissionType, String filter);


	/**
	 * Retrieves the count of roles associated with the given permissionable that poses permissionType access
	 * and filters them by name
	 *
	 * This method is intended for backend usage is does not check for frontend specific roles
	 * like cms anon o logged in site user
	 *
	 * @param permissionable
	 * @param permissionType
	 * @param filter
	 * @return
	 * @since 1.8
	 * @version 1.8
	 */
	int getRoleCount(String permissionable, int permissionType, String filter, boolean hideSystemRoles);


	/**
	 * Retrieves the users with the given permissionType access to the given permissionable
	 * and filter the user names based on the given filter.
	 * This method is intended for backend usage is does not check for frontend specific roles
	 * like cms anon o logged in site user
	 *
	 * @param permissionable
	 * @param permissionType
	 * @param filter
	 * @param start
	 * @param limit
	 * @return
	 * @since 1.6
	 * @version 1.8
	 */
	List<User> getUsers(String permissionable, int permissionType, String filter, int start, int limit);

	/**
	 * Retrieves the count of users with the given permissionType access to the given permissionable
	 * and filter the user names based on the given filter
	 *
	 * This method is intended for backend usage is does not check for frontend specific roles
	 * like cms anon o logged in site user
	 *
	 * @param permissionable
	 * @param permissionType
	 * @param filter
	 * @return
	 * @since 1.6
	 * @version 1.8
	 */
	int getUserCount(String permissionable, int permissionType, String filter);

	/**
	 * Retrieves all permissions associated to a given a role, includes individual and inheritable permissions
	 * @param role
	 * @param onlyFoldersAndHosts filters for only permissions assigned to folders and hosts
	 *
	 * @return a bit permissions list
	 * @throws DotDataException
	 */
	List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts) throws DotDataException;

	/**
	 * Retrieves all permissions associated to a given a role, includes individual and inheritable permissions
	 * @param role
	 * @param onlyFoldersAndHosts filters for only permissions assigned to folders and hosts
	 *
	 * @return a bit permissions list
	 * @throws DotDataException
	 */
	List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts, boolean bitPermissions) throws DotDataException;

	/**
	 * Filters the given list of permissionables that meet the required permission mask
	 * @param <P> The type of permissionable given to the method
	 * @param permissionables
	 * @param requiredPermission
	 * @param respectFrontendRoles
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @since 1.6
	 * @version 1.8
	 */
	<P extends Permissionable> List<P> filterCollection(List<P> permissionables, int requiredPermission, boolean respectFrontendRoles, User user)
		throws DotDataException, DotSecurityException;

	/**
	 * Filters the given list of permissionables that meet the required permission mask
	 * using the permission reference table instead of hitting the cache
	 * This method can be slow if you pass in many objects or there are lots of objects in the DBs.
	 * @param <P> The type of permissionable given to the method
	 * @param permissionables
	 * @param requiredPermission
	 * @param respectFrontendRoles
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @since 1.9.1.4
	 * @version 1.9.1.4
	 */
	<P extends Permissionable> List<P> filterCollectionByDBPermissionReference(List<P> permissionables, int requiredPermission, boolean respectFrontendRoles, User user)
		throws DotDataException, DotSecurityException;


	/**
	 * Remove all the permissions given the roleId
	 * @param roleId
	 * @since 1.6
	 * @version 1.8
	 */
	void removePermissionsByRole(String roleId);

	/**
	 * Saves an individual permission of a given permissionable
	 * @param permission
	 * @param permissionable
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void save(Permission permission, Permissionable permissionable, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 *
	 * Assigns a set of permissions to a given asset, any permissions already assigned to the asset are either updated or removed to match the provided list
	 *
	 * @param permission
	 * @param permissionable
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void assignPermissions(List<Permission> permission, Permissionable permissionable, User user, boolean respectFrontendRoles)
		throws DotDataException, DotSecurityException;


	/**
	 * This method is to clear the permissions cache.  Should only need to be
	 * called for maintenance or debug reasons
	 * @since 1.6
	 * @version 1.8
	 */
	void clearCache();

	/**
	 * This method updates the given permissionable owner with the given user id
	 * @param asset
	 * @param ownerId
	 * @throws DotDataException
	 * @author David H Torres
	 */
	void updateOwner(Permissionable asset, String ownerId) throws DotDataException;

	/**
	 * Returns the bit mask of all system permissions
	 * @return
	 */
	int maskOfAllPermissions();

	/**
	 * Recursively removes all individual and inheritable permissions of children of the given permissionable
	 * @param parent
	 * @throws DotDataException
	 */
	void resetPermissionsUnder(Permissionable parent) throws DotDataException;

	/**
	 *
	 * Navigates through the given permissionable children making sure all individual permissioned assets also have the same
	 * permission rules for the given role as the parent permissionable has.
	 *
	 * Therefore if a child that is individually permissioned (not inheriting) does not have an individual
	 * permission for the given role then the permission will be created, if the child asset already have a permission for the given role
	 * then the permission is updated, and at last if the asset has an individual permission for the role but the parent doesn't then the
	 * individual permission is removed from the child.
	 *
	 * @param permissionable
	 * @param role
	 * @throws DotDataException
	 */
	void cascadePermissionUnder(Permissionable permissionable, Role role) throws DotDataException;

	/**
	 * Removes the given permissionable permission references forcing the api to recalculate the reference
	 * @param perm
	 * @throws DotDataException
	 */
	void resetPermissionReferences(Permissionable perm) throws DotDataException;

	/**
	 * Removes the permission references of all content children of the given structure
	 * @param perm
	 * @throws DotDataException
	 */
	public void resetChildrenPermissionReferences(Structure structure) throws DotDataException;


	/**
	 * Removes all permission references
	 * @throws DotDataException
	 */
	public void resetAllPermissionReferences() throws DotDataException;


	/**
	 * Returns if a user has proper permissions to an asset - even if not created yet
	 * based on the Permissionable parent.  Use case:  A user is trying to save and publish
	 * a new file (no permissions yet) in a folder.  We need to look at the fodler to see
	 * if there are any File+Publish inheritable permissions for the user - even before we
	 * allow the user to save the file.
	 * @param parent
	 * @param type
	 * @param requiredPermissions
	 * @param user
	 * @return
	 * @throws DotDataException
	 */

	boolean doesUserHaveInheriablePermissions(Permissionable parent, String type, int requiredPermissions, User user) throws DotDataException;


	/**
	 * Returns true if the user has the requiredPermissions over the specified permissionable
	 * this method is meant to be used to check multiple parent & children permissions on Hosts/Folders.
	 * @param permissionable
	 * @param requiredPermissions a comma separated list of permissions of the form TYPE:PERMISSION, where TYPE is the permission type
	 * to match the permission against the permissions under the given permissionable host or folder.If no children permissionable are given
	 * the TYPE should be set to PARENT i.e : PARENT:1, this will check for READ permissions on the given permissionable while a value of
	 * PARENT:1, STRUCTURES:4 will check for READ permissions on the given permissionable AND publish permission on structures under the
	 * given permissionable.
	 * @param user
	 * @return
	 * @throws DotDataException
	 */
	boolean doesUserHavePermissions(Permissionable permissionable, String requiredPermissions, User user) throws DotDataException;

	/**
	 * Returns true if the user has the requiredPermissions over the specified permissionable
	 * this method is meant to be used to check multiple parent & children permissions on Hosts/Folders.
	 * @param permissionable
	 * @param requiredPermissions a comma separated list of permissions of the form TYPE:PERMISSION, where TYPE is the permission type
	 * to match the permission against the permissions under the given permissionable host or folder.If no children permissionable are given
	 * the TYPE should be set to PARENT i.e : PARENT:1, this will check for READ permissions on the given permissionable while a value of
	 * PARENT:1, STRUCTURES:4 will check for READ permissions on the given permissionable AND publish permission on structures under the
	 * given permissionable.
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 */
	boolean doesUserHavePermissions(Permissionable permissionable, String requiredPermissions, User user, boolean respectFrontendRoles) throws DotDataException;

	/**
	 * Returns true if the user has the requiredPermissions over the specified objectType
	 * this method is meant to be used to check multiple parent & children permissions on Hosts/Folders.
	 * @param objectyType
	 * @param requiredPermissions a comma separated list of permissions of the form TYPE:PERMISSION, where TYPE is the permission type
	 * to match the permission against the permissions under the given permissionable host or folder.If no children permissionable are given
	 * the TYPE should be set to PARENT i.e : PARENT:1, this will check for READ permissions on the given permissionable while a value of
	 * PARENT:1, STRUCTURES:4 will check for READ permissions on the given permissionable AND publish permission on structures under the
	 * given permissionable.
	 * @param user
	 * @return
	 * @throws DotDataException
	 */
	boolean doesUserHavePermissions(PermissionableType permType, int permissionType, User user) throws DotDataException;

	/**
	 *
	 * @param parent
	 * @param permissionable
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void permissionIndividually(Permissionable parent, Permissionable permissionable, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
     * Finds the permissionable instance from where <code>permissionable</code> is inheriting its permissions.
     * It is usefull before call permissionIndividually as it requires both the permissionable and the parent
     * from where it inherits its permissions
     *
     * http://jira.dotmarketing.net/browse/DOTCMS-6316
     *
     * @param permissionable
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Permissionable findParentPermissionable(Permissionable permissionable) throws DotDataException, DotSecurityException;

    /**
     * Returns wherever a permissionable is inheriting its permissions or
     * have individual permissions
     *
     * @param permissionable
     * @return true - is inheriting permissions / false - have individual permissions
     * @throws DotDataException
     */
    boolean isInheritingPermissions(Permissionable permissionable) throws DotDataException;
}
