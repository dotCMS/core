package com.dotmarketing.business;

import java.util.List;
import java.util.Optional;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import javax.validation.constraints.NotNull;

//This interface should have default package access
public abstract class PermissionCache implements Cachable{

	abstract protected List<Permission> addToPermissionCache(String key,
			List<Permission> permissions);

	abstract protected List<Permission> getPermissionsFromCache(String key);

	abstract public void clearCache();

	abstract protected void remove(String key);

	public abstract Optional<Boolean> doesUserHavePermission(Permissionable permissionable,
															 String permissionType,
															 User userIn,
															 boolean respectFrontendRoles,
															 Contentlet nullableContent) ;

	public abstract void putUserHavePermission(@NotNull Permissionable permissionable,
											   String permissionType,
											   @NotNull User userIn,
											   boolean respectFrontendRoles,
											   @NotNull Contentlet nullableContent,
											   boolean hasPermission) ;



	public abstract void flushShortTermCache() ;
}