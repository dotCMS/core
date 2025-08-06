/*
 * Created on May 30, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.dotmarketing.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.List;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.util.Logger;
import java.util.Optional;
import javax.validation.constraints.NotNull;

/**
 *
 * @author salvador & david
 * @author Carlos Rivas
 * @author Jason Tesser
 */
public class PermissionCacheImpl extends PermissionCache {

	private DotCacheAdministrator cache;

	private final String primaryGroup = "PermissionCache";
	private final String shortLivedGroup = "PermissionShortLived";
	// region's name for the cache
    private final String[] groupNames = {primaryGroup,shortLivedGroup};

	protected PermissionCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#addToPermissionCache(java.lang.String, java.util.List)
	 */
	protected List<Permission> addToPermissionCache(final String key, List<Permission> permissions) {

        // Add the key to the cache
        cache.put(primaryGroup + key, permissions,primaryGroup);

        return permissions;
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#getPermissionsFromCache(java.lang.String)
	 */
    @SuppressWarnings("unchecked")
	protected List<Permission> getPermissionsFromCache(String key) {
    	key = primaryGroup + key;
    	List<Permission> perms = null;
    	try{
    		perms = (List<Permission>) cache.get(key, primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this,"Cache Entry not found", e);
		}
        return perms;
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
    public void clearCache() {
        // clear the cache
        cache.flushGroup(primaryGroup);
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    protected void remove(String key){
    	key = primaryGroup + key;
    	try{
	        cache.remove(key,primaryGroup);
    	}catch (Exception e) {
			Logger.debug(this,e.getMessage(), e);
		}

    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }

	@Override
	public Optional<Boolean> doesUserHavePermission(final Permissionable permissionable,
			@NotNull final String permissionType,
			@NotNull final User userIn,
			final boolean respectFrontendRoles,
			final Contentlet nullableContent) {

		if (DbConnectionFactory.inTransaction()) {
			return Optional.empty();
		}
		if (UtilMethods.isEmpty(permissionable::getPermissionId) ||
				UtilMethods.isEmpty(userIn::getUserId)
		) {
			return Optional.empty();
		}

		final Optional<String> key = shortLivedKey(permissionable, permissionType, userIn, respectFrontendRoles, nullableContent);
        return key.map(s -> (Boolean) cache.getNoThrow(s, shortLivedGroup));

    }

	@Override
	public void putUserHavePermission(@NotNull final Permissionable permissionable,
			@NotNull final String permissionType,
			@NotNull final User userIn,
			final boolean respectFrontendRoles,
			final Contentlet nullableContent, boolean hasPermission)  {


		final Optional<String> key = shortLivedKey(permissionable, permissionType, userIn, respectFrontendRoles, nullableContent);
        key.ifPresent(s -> cache.put(s, hasPermission, shortLivedGroup));

	}

	/**
	 * If no key is returned, it means that the object should not be cached.
	 * @param permissionable
	 * @param permissionType
	 * @param userIn
	 * @param respectFrontendRoles
	 * @param nullableContent
	 * @return
	 */
	private Optional<String> shortLivedKey(@NotNull final Permissionable permissionable,
			@NotNull final String permissionType,
			@NotNull final User userIn,
			final boolean respectFrontendRoles,
			final Contentlet nullableContent)  {

		if (DbConnectionFactory.inTransaction() ||
				UtilMethods.isEmpty(permissionable::getPermissionId) ||
				UtilMethods.isEmpty(userIn::getUserId)
		) {
			return Optional.empty();
		}

		String permissionableKey =  permissionable.getPermissionId();
		if(permissionable instanceof Contentlet && !(permissionable instanceof Host)){
			//We need a bit more dispersion for contentlets since they rely only on the identifier
			//There can be cases on which the same contentlet is being checked for different permissions like for live and working content
			//We need to be able to tell these cases apart
			//Change directly the contentlet permissionable-id on the contentlet implementation seems to be quite risky
			//Changing only the cache key here is safer see https://github.com/dotCMS/core/issues/30991
		    final Contentlet contentlet = (Contentlet)permissionable;
		    permissionableKey = contentlet.getPermissionId() + StringPool.COLON + contentlet.getInode();
		}

		return Optional.of(permissionableKey + permissionType + userIn.getUserId() + respectFrontendRoles + (
				nullableContent != null ? nullableContent.getIdentifier() : ""));


	}

	@Override
	public void flushShortTermCache() {
		cache.flushGroup(shortLivedGroup);
	}
}
