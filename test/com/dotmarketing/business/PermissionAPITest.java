package com.dotmarketing.business;

import static org.junit.Assert.*;

import org.junit.Test;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;

public class PermissionAPITest {
    
    @Test
    public void doesRoleHavePermission(Permissionable permissionable, int permissionType, Role role, boolean respectFrontendRoles) throws DotDataException {
        
    }
    
    @Test
    public void doesRoleHavePermission(Permissionable permissionable, int permissionType, Role role) throws DotDataException {
        
    }
    
    @Test
    public void doesUserHavePermission(Permissionable permissionable, int permissionType, User user) throws DotDataException {
        
    }
    
    @Test
    public void doesUserHavePermission(Permissionable permissionable, int permissionType, User user, boolean respectFrontendRoles) throws DotDataException {
        
    }
    
    @Test
    public void removePermissions(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void setDefaultCMSAdminPermissions(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void setDefaultCMSAnonymousPermissions(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void copyPermissions(Permissionable from, Permissionable to) throws DotDataException {
        
    }
    
    @Test
    public void getPermissions(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void getPermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
        
    }
    
    @Test
    public void getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions) throws DotDataException {
        
    }
    
    @Test
    public void getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions, boolean forceLoadFromDB) throws DotDataException {
        
    }
    
    @Test
    public void getInheritablePermissions(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void getInheritablePermissionsRecurse(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void getInheritablePermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
        
    }
    
    @Test
    public void getReadRoles(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void getReadUsers(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void getPublishRoles(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void getWriteRoles(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void getWriteUsers(Permissionable permissionable) throws DotDataException {
        
    }
    
    @Test
    public void getRolesWithPermission(Permissionable permissionable, int permission) throws DotDataException {
        
    }
    
    @Test
    public void getUsersWithPermission(Permissionable permissionable, int permission) throws DotDataException {
        
    }
    
    @Test
    public void doesUserOwn(Inode inode, User user) throws DotDataException {
        
    }
    
    @Test
    public void mapAllPermissions() throws DotDataException {
        
    }
    
    @Test
    public void getPermissionIdsFromRoles(Permissionable permissionable, Role[] roles, User user) throws DotDataException {
        
    }
    
    @Test
    public void getPermissionIdsFromUser(Permissionable permissionable, User user) throws DotDataException {
        
    }
    
    @Test
    public void getRoles(String permissionable, int permissionType, String filter, int start, int limit) {
        
    }
    
    @Test
    public void getRoles(String permissionable, int permissionType, String filter, int start, int limit, boolean hideSystemRoles) {
        
    }
}
