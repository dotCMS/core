package com.dotmarketing.business;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.beans.WebAsset;
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
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
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
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

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
	 * @param permissionFactory service reference
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
		ArrayList<Role> roles = new ArrayList<>();
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
		SortedSet<Role> roles = new TreeSet<>();

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
		return new ArrayList<>(roles);
	}


	/**
	 * This is not intended to be used to check permission because it doesn't check for cms administrator privileges
	 * @param userRoleIDs
	 * @param permissions
	 * @param requiredPermissionType
	 * @return If the user has the required permission for the collection of permissions passed in
	 */
	private boolean doRolesHavePermission(Collection<String> userRoleIDs, List<Permission> permissions, int requiredPermissionType){
		
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
		if((PermissionableType.FOLDERS.getCanonicalName().equals(permissionable.getPermissionType()) ||
				permissionable instanceof Identifier && ((Identifier) permissionable).getAssetType().equals("folder"))
				&& permissionType == PERMISSION_PUBLISH){
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

	private boolean userPermissions(final UserProxy userProxy, final User userIn) {

		if(userProxy.getPermissionId().equals("user:"+userIn.getUserId())) {
			return true;
		}
		return Try.of(()-> APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("user", userIn)).getOrElse(false);
	}

	@Override
	public boolean doesUserHavePermission(Permissionable permissionable, int permissionType, User user) throws DotDataException {
		return doesUserHavePermission(permissionable, permissionType, user, true);
	}

	@CloseDBIfOpened
	@Override
	public boolean doesUserHavePermission(final Permissionable permissionable,
										  final int permissionType,
										  final User userIn,
										  final boolean respectFrontendRoles) throws DotDataException {
		return doesUserHavePermission(permissionable, permissionType, userIn, respectFrontendRoles, null);
	}


	@CloseDBIfOpened
	@Override
	public boolean doesUserHavePermission(final Permissionable permissionable,
			final int permissionType,
			final User userIn,
			final boolean respectFrontendRoles,
			final Contentlet contentlet) throws DotDataException {



		final User user = (userIn==null || userIn.getUserId()==null) ? APILocator.getUserAPI().getAnonymousUser() : userIn;
		if (user.getUserId().equals(APILocator.systemUser().getUserId()) || user.isAdmin()){
			return true;
		}


		if (permissionable == null) {
			Logger.warn(this, "Permissionable object is null");
			throw new NullPointerException("Permissionable object is null");
		}

		if (UtilMethods.isEmpty(permissionable.getPermissionId())) {
			Logger.debug(
					this.getClass(),
					"Trying to get permissions on null inode of type :" + permissionable.getPermissionType()) ;
			Logger.debug(
					this.getClass(),
					"Trying to get permissions on null inode of class :" + permissionable.getClass()) ;
			return false;
		}

		Optional<Boolean> cachedPermission = CacheLocator.getPermissionCache().doesUserHavePermission(permissionable, String.valueOf(permissionType), user, respectFrontendRoles, contentlet);
		if (cachedPermission.isPresent() ) {
			return cachedPermission.get();
		}

		boolean hasPermission = doesUserHavePermissionInternal(permissionable, permissionType, user, respectFrontendRoles, contentlet);

		CacheLocator.getPermissionCache().putUserHavePermission(permissionable, String.valueOf(permissionType), user, respectFrontendRoles, contentlet, hasPermission);
		return hasPermission;

	}

	private final Lazy<Role> anonRole = Lazy.of(()->Try.of(()->APILocator.getRoleAPI().loadCMSAnonymousRole()).getOrElseThrow(DotRuntimeException::new));
	private final Lazy<Role> adminRole = Lazy.of(()->Try.of(()->APILocator.getRoleAPI().loadCMSAdminRole()).getOrElseThrow(DotRuntimeException::new));
	private final Lazy<Role> ownerRole = Lazy.of(()->Try.of(()->APILocator.getRoleAPI().loadCMSOwnerRole()).getOrElseThrow(DotRuntimeException::new));



	@CloseDBIfOpened
	public boolean doesUserHavePermissionInternal(final Permissionable permissionable,
										  final int permissionType,
										  @NotNull final User userIn,
										  final boolean respectFrontendRoles,
										  final Contentlet contentlet) throws DotDataException {

		final User user = (userIn==null || userIn.getUserId()==null) ? APILocator.getUserAPI().getAnonymousUser() : userIn;
		if (user.getUserId().equals(APILocator.systemUser().getUserId())){
			return true;
		}

		if (user.isAdmin()) {
			return true;
		}

		if (permissionable == null) {
			Logger.warn(this, "Permissionable object is null");
			throw new NullPointerException("Permissionable object is null");
		}

		if (UtilMethods.isEmpty(permissionable.getPermissionId())) {
			Logger.debug(
					this.getClass(),
					"Trying to get permissions on null inode of type :" + permissionable.getPermissionType()) ;
			Logger.debug(
					this.getClass(),
					"Trying to get permissions on null inode of class :" + permissionable.getClass()) ;
			return false;
		}

		// short circuit for UserProxy
		if (permissionable instanceof UserProxy) {
			return userPermissions((UserProxy) permissionable, user);
		}

		// Folders do not have PUBLISH, use EDIT instead
		final int expecterPermissionType = (PermissionableType
				.FOLDERS
				.getCanonicalName()
				.equals(permissionable.getPermissionType()) ||
				permissionable instanceof Identifier && ((Identifier) permissionable).getAssetType().equals("folder"))
				&& permissionType == PERMISSION_PUBLISH
				? PERMISSION_EDIT
				: permissionType;


		if (APILocator.getRoleAPI().doesUserHaveRole(user, adminRole.get())) {
			return true;
		}

		final List<Permission> perms = getPermissions(permissionable, true)
				.stream()
				.filter(p-> p.matchesPermission(expecterPermissionType))
				.collect(Collectors.toList());
		final boolean isContentlet = permissionable instanceof Contentlet;
		for(final Permission p : perms){
			if (respectFrontendRoles) {
				//anonymous role should not be able to access non-live contentlet
				if (p.getRoleId().equals(anonRole.get().getId()) && (!isContentlet || isLiveContentlet(permissionable))) {
					return true;
					//if logged in site user has permission
				}
			}
			// if owner and owner has required permission return true
			try {
				if (p.getRoleId().equals(ownerRole.get().getId())
						&& permissionable.getOwner() != null
						&& permissionable.getOwner().equals(user.getUserId())
						&& checkRelatedPermissions(permissionable.permissionDependencies(expecterPermissionType), user)) {
					return true;
				}
			} catch (DotDataException e1) {
				Logger.error(this, e1.getMessage(), e1);
				throw new DotRuntimeException(e1.getMessage(), e1);
			}

			if (permissionable instanceof WorkflowAction
					&& workflowActionHasPermission(expecterPermissionType, p, user, contentlet)) {
				return true;
			}
		}

		// front end users cannot read content that is not live
		if (!user.isBackendUser()
				&& isContentlet
				&& !isLiveContentlet(permissionable)
				&& expecterPermissionType == PERMISSION_READ) {
			Logger.warn(this, String.format("User '%s' cannot verify READ permissions on Contentlet '%s' because it " +
					"is not live.", user.getUserId(), permissionable.getPermissionId()));
			return false;
		}

        if (isContentlet && !hasCategoryPermission(((Contentlet) permissionable), expecterPermissionType, user,
                respectFrontendRoles)) {
            return false;
        }

		final Set<String> userRoleIds = filterUserRoles(user, respectFrontendRoles).stream().map(Role::getId).collect(Collectors.toSet());

		return doRolesHavePermission(userRoleIds, getPermissions(permissionable, true), expecterPermissionType);

	}


    /**
     * Checks if the specified user has permissions to the categories used on the content. The algo is this: To access a
     * piece of content, the user has to have permissions to at least one category that is set on EVERY category field
     * on the content (or the category field is empty). If the user does not have access to ANY of the categories set on
     * any category field of the content then the user is not allowed to access the content.
     *
     * @param contentlet           the contentlet for which category permissions are being checked. This parameter must
     *                             not be null.
     * @param permissionType       the type of permission being checked (e.g., read, write, etc.).
     * @param user                 the user whose permissions are being verified. This parameter must not be null.
     * @param respectFrontendRoles a flag indicating whether to respect frontend roles during permission checks.
     * @return true if the user has the specified permission type for all categories associated with the contentlet;
     * false otherwise.
     * @throws DotDataException if an error occurs during the permission check process.
     */

    boolean hasCategoryPermission(@NotNull final Contentlet contentlet, final int permissionType,
            @NotNull final User user, boolean respectFrontendRoles)
            throws DotDataException {

        if (!Config.getBooleanProperty("PERMISSION_CONTENT_RESPECT_CATEGORY_PERMISSION", true)) {
            return true;
        }

        // List of fields which have secondaryPermissionCheck=true
        List<Field> permissionedCategories = contentlet.getContentType()
                .fields(CategoryField.class)
                .stream()
                .filter(f -> f.fieldVariables()
                        .stream()
                        .anyMatch(
                                fv -> "secondaryPermissionCheck".equalsIgnoreCase(fv.key()) && "true".equalsIgnoreCase(
                                        fv.value())))
                .collect(Collectors.toList());

        if (permissionedCategories.isEmpty()) {
            return true;
        }

        for (Field field : permissionedCategories) {

            // categories selected
            List<Category> allCats = Try.of(() -> (List<Category>) APILocator.getContentletAPI()
                            .getFieldValue(contentlet, field, APILocator.systemUser(), false))
                    .getOrElse(List.of());

            // nothing selected, allow access
            if (allCats.isEmpty()) {
                continue;
            }

            //categories I have access to
            List<Category> myCats = Try.of(
                            () -> (List<Category>) APILocator.getContentletAPI().getFieldValue(contentlet, field, user, false))
                    .getOrElse(List.of());

            // if I have no access, kaput!
            if (myCats.isEmpty()) {
                return false;
            }
        }

        return true;


    }


	/**
	 * Given an expected permissionType to be satisfied, a resolved "Anyone who can..." style permission, a {@link User}
	 * and a {@link Contentlet}, evaluates if the contentlet is new. Then verify that the provided permission is the
	 * actual mentioned style and if it does then finally evaluate the contentlet's content type permissions as a
	 * fallback.
	 *
	 * @param permissionType provided permission type
	 * @param permission actual permission
	 * @param user provided user
	 * @param contentlet contentlet
	 * @return true if the explained logic takes place, otherwise false
	 */
	private boolean workflowActionHasPermission(final int permissionType,
												final Permission permission,
												final User user,
												final Contentlet contentlet) throws DotDataException {
		if (Objects.nonNull(contentlet) && StringUtils.isBlank(contentlet.getInode())) {
			final Optional<Role> permRole = Optional.ofNullable(
				Try
					.of(() -> APILocator.getRoleAPI().loadRoleById(permission.getRoleId()))
					.getOrElse((Role) null))
				.filter(role -> role.getRoleKey().startsWith("cms_workflow_any_who_can"));
			if (permRole.isPresent())  {
				return doesUserHavePermission(contentlet.getContentType(), permissionType, user);
			}
		}
		return false;
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
                && Try.of(()->((Contentlet) permissionable).isLive()).getOrElse(false);
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
		Role cmsAnonymousRole= anonRole.get();

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
	 * @param permission to save
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


		checkCopyPermissions(permissionable, user);

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

	private void checkCopyPermissions(Permissionable permissionable, User user) throws DotDataException, DotSecurityException {

		if (!isCopy(permissionable)){
			if (!doesUserHavePermission(permissionable, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {

				if (!checkIfContentletTypeHasEditPermissions(permissionable, user)) {
					throw new DotSecurityException("User id: " + user.getUserId() + " does not have permission to alter permissions on asset " + permissionable.getPermissionId());
				}
			}
		}
	}

	/**
	 * In case the permissionable is a contentlet, we try to check if the content type has edit permissions
	 * This is applies when the doesUserHavePermission(permissionable, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) was called previously and has fail.
	 *
	 * @param permissionable
	 * @param user
	 * @return boolean
	 * @throws DotDataException
	 */
	public boolean checkIfContentletTypeHasEditPermissions(final Permissionable permissionable, final User user) throws DotDataException {

		return permissionable instanceof Contentlet? // we can check if the content type has edit permissions
				doesUserHavePermission(Contentlet.class.cast(permissionable).getContentType(), PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user):false;
	}


	@Override
	public boolean doesUserHavePermission(final ContentType permissionable, final int permissionType, final User user) throws DotDataException {

		return doesUserHavePermission(permissionable, permissionType, user, true);
	}

	@Override
	public boolean doesUserHavePermission(final ContentType type, final int permissionType,
										  final User user, final boolean respectFrontendRoles) throws DotDataException {

		    // try the legacy way
			final boolean hasPermission = this.doesUserHavePermission((Permissionable) type, permissionType, user, respectFrontendRoles);

			// if the user does not have permission, check if the type allows CMS owner
			if (!hasPermission) {

				final Role cmsOwnerRole = Try.of(() -> APILocator.getRoleAPI().loadCMSOwnerRole())
						.getOrElseThrow(e -> new DotRuntimeException(e.getMessage(), e));

				final List<Permission> contentTypePermissions = getPermissions(type, true);
				for(final Permission contentTypePermission : contentTypePermissions) {
					if (user.isBackendUser() && contentTypePermission.getRoleId().equals(cmsOwnerRole.getId())) {

						if (type.fields(HostFolderField.class).isEmpty() && Host.SYSTEM_HOST.equals(type.host())) {
							return true;
						}
					}
				}
			}

			return hasPermission;
	} // doesUserHavePermission

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
		List<String> rolesIncluded = new ArrayList<>();
		List<Permission> includingLockedRolePermissions = new ArrayList<>();

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
		Set<User> users = new HashSet<>();
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
		Set<Role> readPermissions = new HashSet<>();
		List<Permission> permissions = getPermissions(permissionable);
		List<Role> roles = new ArrayList<>();
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
		Set<Role> publishPermissions = new HashSet<>();
		List<Permission> permissions = getPermissions(permissionable);
		List<Role> roles = new ArrayList<>();
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
		Set<User> users = new HashSet<>();
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
		Set<Role> writePermissions = new HashSet<>();
		List<Permission> permissions = getPermissions(permissionable);
		List<Role> roles = new ArrayList<>();
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

		Set<Role> roles = new HashSet<>();
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
		Set<User> users = new HashSet<>();
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
		Set<Integer> permissions = new TreeSet<>();
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
					return new ArrayList<>(permissions);
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
			return new ArrayList<>(permissions);
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


		return new ArrayList<>(permissions);
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
				roleList = new ArrayList<>(0);
			}
		}

		return roleList;
	}

	@Override
	public List<Role> getRoles(String inode, int permissionType,
			String filter, int start, int limit, boolean hideSystemRoles) {
		List<Role> roleList = getRoles(inode, permissionType, filter, start, limit);
		List<Role> roleListTemp = new ArrayList<>(roleList);
		if(hideSystemRoles)
			for(Role r : roleListTemp) {
				if(PortalUtil.isSystemRole(r))
					roleList.remove(r);
			}

		return roleList;
	}

	@Override
	public int getRoleCount(String inode, int permissionType, String filter) {

		List<Role> roleList = null;
		List<Permission> permissionList = null;
		int count = 0;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getRoleCount -> before loading inode object(%s)", inode) );
			final Optional<Permissionable> permissionable = getPermissionableFromInode(inode);
			if (permissionable.isPresent()) {
				permissionList = getPermissions(permissionable.get(), true);
			}

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
		List<Role> roleList = null;
		List<Permission> permissionList = null;
		int count = 0;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getRoleCount -> before loading inode object(%s)", inode) );

			final Optional<Permissionable> permissionable = getPermissionableFromInode(inode);

			if (permissionable.isPresent()) {
				permissionList = getPermissions(permissionable.get(), true);

			}
			roleList = loadRolesForPermission(permissionList, permissionType, filter);

			List<Role> roleListTemp = new ArrayList<>(roleList);
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

		List<User> userList = null;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getUsers -> before loading inode object(%s)", inode) );

			final Optional<Permissionable> permissionable = getPermissionableFromInode(inode);
			if (permissionable.isPresent()) {
				userList = permissionFactory.getUsers(permissionable.get(), permissionType, filter, start,
						limit);
			}

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
		finally {
			if( userList == null ) {
				userList = new ArrayList<>(0);
			}
		}

		return userList;

	}

	/**
	 * Returns the Permissionable object based on its Inode.
	 *
	 * @param inode The Inode of the Permissionable object.
	 *
	 * @return The {@link Permissionable} object matching the specified Inode.
	 *
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException The User accessing this information does not hace the required permissions to do
	 * so.
	 */
	private Optional<Permissionable> getPermissionableFromInode(final String inode) throws DotDataException,
																								   DotSecurityException {
		final Inode inodeObj = InodeFactory.getInode(inode, Inode.class);
		final Class<?> clazz = Inode.Type.CONTENTLET.getTableName().equalsIgnoreCase(inodeObj.getType()) ?
									   Contentlet.class : InodeUtils.getClassByDBType(inodeObj.getType());
		Permissionable perm = null;
		if (Contentlet.class.equals(clazz)) {
			perm = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
		} else if (Folder.class.equals(clazz)) {
			perm = APILocator.getFolderAPI().find(inode, APILocator.systemUser(), false);
		} else if (Container.class.equals(clazz)) {
			perm = APILocator.getContainerAPI().find(inode, APILocator.systemUser(), false);
		} else if (Template.class.equals(clazz)) {
			perm = APILocator.getTemplateAPI().find(inode, APILocator.systemUser(), false);
		} else if (Category.class.equals(clazz)) {
			perm = APILocator.getCategoryAPI().find(inode, APILocator.systemUser(), false);
		} else if (Link.class.equals(clazz)) {
			perm = APILocator.getMenuLinkAPI().find(inode, APILocator.systemUser(), false);
		} else {
			// In the case an inode isn't found, we should check if it is a folder and return it
			final Folder folder = APILocator.getFolderAPI().find(inode, APILocator.systemUser(), false);
			if (null != folder && null != folder.getInode()) {
				perm = folder;
			}
		}
		return null != perm && UtilMethods.isSet(perm.getPermissionId()) ? Optional.of(perm) : Optional.empty();
	}

	@CloseDBIfOpened
	@Override
	public int getUserCount(String inode, int permissionType, String filter) {

		int count = 0;

		try {

			Logger.debug( PermissionAPI.class, String.format("::getUserCount -> before loading inode object(%s)", inode) );

			final Optional<Permissionable> permissionable = getPermissionableFromInode(inode);
			if (permissionable.isPresent()) {
				count = permissionFactory.getUserCount(permissionable.get(), permissionType, filter);
			}

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
															   final boolean respectFrontendRoles,
															   final User user,
															   final Contentlet contentlet)
		throws DotDataException, DotSecurityException {

		final RoleAPI roleAPI = APILocator.getRoleAPI();
		if ((user != null) && roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole())) {
			return inputList;
		}

		final List<P> permissionables = new ArrayList<>(inputList);
		if (permissionables.isEmpty()) {
			return permissionables;
		}

		Permissionable permissionable;
		int i = 0;

		while (i < permissionables.size()) {
			permissionable = permissionables.get(i);
			if (!doesUserHavePermission(
				permissionable,
				requiredTypePermission,
				user,
				respectFrontendRoles,
				contentlet)) {
				permissionables.remove(i);
			} else {
				++i;
			}
		}

		return permissionables;
	}

    @CloseDBIfOpened
    @Override
	public <P extends Permissionable> List<P> filterCollection(final List<P> inputList,
															   final int requiredTypePermission,
															   final boolean respectFrontendRoles,
															   final User user)
		throws DotDataException, DotSecurityException {

		return filterCollection(inputList, requiredTypePermission, respectFrontendRoles, user, null);
	}

	@CloseDBIfOpened
	@Override
	public <P extends Permissionable> List<P> filterCollectionByDBPermissionReference(List<P> inputList, int requiredTypePermission,boolean respectFrontendRoles, User user) throws DotDataException, DotSecurityException {

		RoleAPI roleAPI = APILocator.getRoleAPI();

		if ((user != null) && roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole()))
			return inputList;

		List<P> permissionables = new ArrayList<>(inputList);
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
		List<String> userRoleIds= new ArrayList<>();
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
				host = !(permissionable instanceof Host)?
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



		List<String> permissionIdsStr = new ArrayList<>();
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
				(permissionable instanceof Contentlet && ((Contentlet) permissionable).isHost()) ||
				(null != permissionable && permissionable instanceof PermissionableProxy
						&& Host.class.getName().equals(PermissionableProxy.class.cast(permissionable).getType()));
	}

	@Override
    public boolean doesUserHavePermissions(PermissionableType permType, int permissionType, User user) throws DotDataException {
    	return doesUserHavePermissions(null,permType,permissionType,user);
    }

	@Override
	public boolean doesUserHavePermissions(final String assetId, final PermissionableType permType, final int permissionType, final User user) throws DotDataException {
		if(user==null) {
			return false;
		}

		if(APILocator.getUserAPI().isCMSAdmin(user)){
			return true;
		}

		Boolean hasPerm = false;
		final RoleAPI roleAPI = APILocator.getRoleAPI();
		final List<com.dotmarketing.business.Role> roles = roleAPI.loadRolesForUser(user.getUserId(), false);
		for(final com.dotmarketing.business.Role role : roles) {
			List<Permission> perms = APILocator.getPermissionAPI().getPermissionsByRole(role, false);
			if(UtilMethods.isSet(assetId)) {
				perms = perms.stream()
						.filter(permission -> permission.getInode().equalsIgnoreCase(assetId))
						.collect(
								Collectors.toList());
			}
			for (final Permission permission : perms) {
				if(permission.getType().equals(permType.getCanonicalName())) {
					hasPerm = hasPerm | permission.getPermission()>=permissionType;
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

		final ImmutableList.Builder<Permission> builder = new Builder<>();
		final List<Permission> newSetOfPermissions = new ArrayList<>();

		if (!doesUserHavePermission(permissionable, PermissionAPI.PERMISSION_EDIT_PERMISSIONS,
				user)) {
			throw new DotSecurityException("User id: " + user.getUserId()
					+ " does not have permission to alter permissions on asset " + permissionable
					.getPermissionId());
		}

		if (parent.isParentPermissionable()) {

			builder.addAll(permissionFactory.getInheritablePermissions(parent))
					.addAll(permissionFactory.getPermissions(parent, true));

			final List<Permission> permissionList = builder.build();

			String permissionableType = permissionable.getPermissionType();

			final Host host = APILocator.getHostAPI()
					.find(permissionable.getPermissionId(), APILocator.systemUser(), false);
			if (host != null) {
				permissionableType = Host.class.getCanonicalName();
			}

			final Set<String> classesToIgnoreFolder = Sets.newHashSet(
					Template.class.getCanonicalName(),
					Container.class.getCanonicalName(),
					Category.class.getCanonicalName(),
					Host.class.getCanonicalName()
			);

            final Set<String> ContentTypeInheritableClasses = Sets.newHashSet(
                            Contentlet.class.getCanonicalName()
                    );
			
			
			final Set<String> classesToIgnoreHost = Sets
					.newHashSet(Category.class.getCanonicalName());

            //Organize permission by role
			final Map<String, List<Permission>> permissionsByRole = permissionList.stream()
					.collect(Collectors.groupingBy(Permission::getRoleId));

			final String finalPermissionableType = permissionableType;
			permissionsByRole.forEach((roleId, permissions) -> {

                //Then find out if the group of role permissions we're looking at least has one perm type matching the permissionable type that has been passed.
				final Optional<Permission> hasRolesMatchingParentPermissionable = permissions
						.stream()
						.filter(permission -> finalPermissionableType
								.equals(permission.getType())).findAny();

				final boolean onlyHasIndividualPermissions = permissions.stream().allMatch(
						permission -> INDIVIDUAL_PERMISSION_TYPE.equals(permission.getType()));

				if (hasRolesMatchingParentPermissionable.isPresent() || onlyHasIndividualPermissions) {

					for (final Permission permission : permissions) {

						if (finalPermissionableType.equals(Structure.class.getCanonicalName())
								&& !ContentTypeInheritableClasses.contains(permission.getType())) {
							continue;
						}
                        if (finalPermissionableType.equals(Folder.class.getCanonicalName())
                                && classesToIgnoreFolder.contains(permission.getType())) {
                            continue;
                        }
						if (finalPermissionableType.equals(Host.class.getCanonicalName())
								&& classesToIgnoreHost.contains(permission.getType())) {
							continue;
						}

						if (finalPermissionableType.equals(permission.getType()) || permission
								.isIndividualPermission()) {
							Permission duplicatedPermission = null;
							ImmutableList.Builder<Permission> dupesListBuilder = new Builder<>();

							for (final Permission newPermission : newSetOfPermissions) {
								if (newPermission.isIndividualPermission() && newPermission
										.getRoleId().equals(permission.getRoleId())
										&& newPermission.getPermission() > permission
										.getPermission()) {
									duplicatedPermission = newPermission;
									break;
								} else if (newPermission.isIndividualPermission() && newPermission
										.getRoleId().equals(permission.getRoleId())) {
									dupesListBuilder.add(newPermission);
								}
							}

							final List<Permission> dupesPermissionList = dupesListBuilder
									.build();
							if (duplicatedPermission == null) {
								newSetOfPermissions.removeAll(dupesPermissionList);
								if (permission.isIndividualPermission()) {
									newSetOfPermissions.add(new Permission(permission.getType(),
											permissionable.getPermissionId(),
											permission.getRoleId(), permission.getPermission(),
											true));
									continue;
								} else {
									//inheritable non-dupe perm added as individual perm
									newSetOfPermissions
											.add(new Permission(permissionable.getPermissionId(),
													permission.getRoleId(),
													permission.getPermission(), true));
								}
							}

							if (!permission.isIndividualPermission()) {
								//inheritable non-dupe perm
								newSetOfPermissions.add(new Permission(permission.getType(),
										permissionable.getPermissionId(), permission.getRoleId(),
										permission.getPermission(), true));
							}

						} else {
							//Any other inheritable permission different from the permissionable type ends here.
							newSetOfPermissions.add(new Permission(permission.getType(),
									permissionable.getPermissionId(), permission.getRoleId(),
									permission.getPermission(), true));
						}
					}


				}


			});


		}
		return newSetOfPermissions;
	}

    @CloseDBIfOpened
    @Override
    public Permissionable findParentPermissionable(final Permissionable permissionable) throws DotDataException, DotSecurityException {
		Permissionable parentPermissionable=permissionable.getParentPermissionable();
		if(parentPermissionable!=null) {
			final List<Permission> assetPermissions = getPermissions(permissionable, true);
			final Map<String, Inode> inodeCache = new HashMap<>();
    		for(Permission p : assetPermissions) {
    			if(!p.getInode().equals(permissionable.getPermissionId())) {
    				final String assetInode = p.getInode();
                    Inode inode = inodeCache.get(p.getInode());
                    if (null == inode) {
                        // Both Structure and ContentType classes are handled properly here
                        inode = InodeUtils.getInode(assetInode);
                        inodeCache.put(inode.getInode(), inode);
                    }
					if (inode instanceof Structure) {
						parentPermissionable = (Structure)inode;
					} else if (inode instanceof Category) {
						parentPermissionable = (Category)inode;
					} else {
						//it can be a host or a folder
						final Host host = APILocator.getHostAPI()
								.find(assetInode, APILocator.getUserAPI().getSystemUser(), false);
						if (host != null) {
							parentPermissionable = host;
						}

						final Folder folder = APILocator.getFolderAPI()
								.find(assetInode, APILocator.getUserAPI().getSystemUser(), false);
						if (folder != null) {
							parentPermissionable = folder;
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

	/**
	 * Retrieves a filtered list of roles by removing front-end roles when unnecessary.
	 **/
	private Set<Role> filterUserRoles(final User user, final boolean respectFrontendRoles) throws DotDataException {
		final Role anonymousRoleRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
		final Role frontEndUserRole = APILocator.getRoleAPI().loadLoggedinSiteRole();

		final Set<Role> roles = new HashSet<>(Try
				.of(() -> APILocator.getRoleAPI().loadRolesForUser(user.getUserId()))
				.getOrElse(List.of()));

		// remove front end user access for anon user (e.g, /intranet)
		// Note to selves: it was a mistake to add this role to the Anon user in 5.2.0
		if (user.isAnonymousUser()) {
			roles.remove(frontEndUserRole);
		}
		if (!respectFrontendRoles) {
			roles.remove(frontEndUserRole);
			roles.remove(anonymousRoleRole);
			roles.remove(APILocator.getRoleAPI().loadRoleByKey("anonymous"));
		}

		return roles;
	}

	@Override
	@CloseDBIfOpened
	public boolean doesSystemHostHavePermissions(final Permissionable systemHost, final User user, final boolean respectFrontendRoles, final String expectedPermissionType) throws DotDataException {
		final List<Permission> systemHostPermissions = getInheritablePermissions(systemHost);

		final Set<String> userRoleIds = filterUserRoles(user, respectFrontendRoles).stream().map(Role::getId).collect(Collectors.toSet());

		for(final Permission perm : systemHostPermissions){
			if(perm.getType().equals(expectedPermissionType) && userRoleIds.contains(perm.getRoleId()) ){
				return true;
			}
		}

		return false;
	}

	public boolean isCopy(Permissionable permissionable){
		boolean isCopy = false;
		if (permissionable instanceof Contentlet){
			Contentlet contentlet = (Contentlet) permissionable;
			isCopy = contentlet.getBoolProperty(Contentlet.IS_COPY);
		}
		return isCopy;
	}

}
