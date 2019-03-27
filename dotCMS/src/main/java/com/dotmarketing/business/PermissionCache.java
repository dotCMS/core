package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.beans.Permission;

//This interface should have default package access
public abstract class PermissionCache implements Cachable{

	abstract protected List<Permission> addToPermissionCache(String key,
			List<Permission> permissions);

	abstract protected List<Permission> getPermissionsFromCache(String key);

	abstract public void clearCache();

	abstract protected void remove(String key);

}