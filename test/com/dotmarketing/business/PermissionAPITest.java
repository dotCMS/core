package com.dotmarketing.business;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public class PermissionAPITest extends TestBase {
    
    Host host;
    
    @Before
    void createTestHost() throws Exception {
        final User sysuser=APILocator.getUserAPI().getSystemUser();
        host = new Host();
        host.setHostname("testhost.demo.dotcms.com");
        APILocator.getHostAPI().save(host, sysuser, false);
        
        Template tt=new Template();
        tt.setTitle("testtemplate");
        tt.setBody("<html><head></head><body>en empty template just for test</body></html>");
        APILocator.getTemplateAPI().saveTemplate(tt, host, sysuser, false);
        
        for(int w=1;w<=5;w++)
         for(int x=1;x<=5;x++)
          for(int y=1;y<=5;y++)
           for(int z=1;z<=5;z++) {
               String path="/f"+w+"/f"+x+"/f"+y+"/f"+z;
               Folder folder=APILocator.getFolderAPI().createFolders(path, host, sysuser, false);
               
               HTMLPage page=new HTMLPage();
               page.setPageUrl("testpage");
               page.setFriendlyName("testpage");
               page.setTitle("testpage");
               APILocator.getHTMLPageAPI().saveHTMLPage(page, tt, folder, sysuser, false);
           }
    }
    
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
