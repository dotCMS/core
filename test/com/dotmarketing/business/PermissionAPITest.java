package com.dotmarketing.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public class PermissionAPITest extends TestBase {
    
    private PermissionAPI perm;
    Host host;
    User sysuser;
    
    @BeforeClass
    void createTestHost() throws Exception {
        perm=APILocator.getPermissionAPI();
        sysuser=APILocator.getUserAPI().getSystemUser();
        host = new Host();
        host.setHostname("testhost.demo.dotcms.com");
        APILocator.getHostAPI().save(host, sysuser, false);
        
        perm.permissionIndividually(APILocator.getHostAPI().findSystemHost(), host, sysuser, false);
        
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
    
    @AfterClass
    public void deleteTestHost() throws DotContentletStateException, DotDataException, DotSecurityException {
        APILocator.getHostAPI().archive(host, sysuser, false);
        APILocator.getHostAPI().delete(host, sysuser, false);
    }
    
    @Test
    public void doesRoleHavePermission() throws DotDataException, DotSecurityException {
        Role nrole=new Role();
        nrole.setName("TestingRole");
        nrole.setRoleKey("TestingRole");
        nrole.setEditUsers(true);
        nrole.setEditPermissions(true);
        nrole.setEditLayouts(true);
        nrole.setDescription("Testing Role");
        APILocator.getRoleAPI().save(nrole);
        
        Permission p=new Permission();
        p.setBitPermission(true);
        p.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p.setPermission(PermissionAPI.PERMISSION_EDIT);
        p.setRoleId(nrole.getId());
        perm.save(p, host, sysuser, false);
        
        assertTrue(perm.doesRoleHavePermission(host, PermissionAPI.PERMISSION_EDIT, nrole));
        assertFalse(perm.doesRoleHavePermission(host, PermissionAPI.PERMISSION_PUBLISH, nrole));
        assertFalse(perm.doesRoleHavePermission(host, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, nrole));
    }
    
    @Test
    public void doesUserHavePermission() throws DotDataException, DotSecurityException {
        Role nrole=new Role();
        nrole.setName("TestingRole2");
        nrole.setRoleKey("TestingRole2");
        nrole.setEditUsers(true);
        nrole.setEditPermissions(true);
        nrole.setEditLayouts(true);
        nrole.setDescription("Testing Role 2");
        APILocator.getRoleAPI().save(nrole);
        
        User user=APILocator.getUserAPI().createUser("useruser", "user@fake.org");
        APILocator.getUserAPI().save(user, sysuser, false);
        
        APILocator.getRoleAPI().addRoleToUser(nrole, user);
        
        Permission p=new Permission();
        p.setBitPermission(true);
        p.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p.setPermission(PermissionAPI.PERMISSION_EDIT);
        p.setRoleId(nrole.getId());
        perm.save(p, host, sysuser, false);
        
        assertTrue(perm.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT, user));
        assertFalse(perm.doesUserHavePermission(host, PermissionAPI.PERMISSION_PUBLISH, user));
        assertFalse(perm.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user));
    }
    
    @Test
    public void removePermissions() throws DotDataException, DotSecurityException {
        Folder f=APILocator.getFolderAPI().findFolderByPath("/f1/", host, sysuser, false);
        
        assertTrue(perm.isInheritingPermissions(f));
        assertTrue(f.getParentPermissionable().equals(host));
        
        perm.permissionIndividually(host, f, sysuser, false);
        assertFalse(perm.isInheritingPermissions(f));
        
        perm.removePermissions(f);
        
        assertTrue(perm.isInheritingPermissions(f));
        assertTrue(f.getParentPermissionable().equals(host));
    }
    
    @Test
    public void copyPermissions() throws DotDataException, DotSecurityException {
        Folder f1=APILocator.getFolderAPI().findFolderByPath("/f1/", host, sysuser, false);
        Folder f2=APILocator.getFolderAPI().findFolderByPath("/f2/", host, sysuser, false);
        
        Role nrole=new Role();
        nrole.setName("TestingRole3");
        nrole.setRoleKey("TestingRole3");
        nrole.setEditUsers(true);
        nrole.setEditPermissions(true);
        nrole.setEditLayouts(true);
        nrole.setDescription("Testing Role 3");
        APILocator.getRoleAPI().save(nrole);
        
        perm.permissionIndividually(host, f1, sysuser, false);
        perm.permissionIndividually(host, f2, sysuser, false);
        
        Permission p1=new Permission();
        p1.setBitPermission(true);
        p1.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        perm.save(p1, f1, sysuser, false);
        
        Permission p2=new Permission();
        p2.setBitPermission(true);
        p2.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p2.setPermission(PermissionAPI.PERMISSION_WRITE);
        p2.setRoleId(nrole.getId());
        perm.save(p2, f1, sysuser, false);
        
        perm.copyPermissions(f1, f2);
        
        assertTrue(perm.doesRoleHavePermission(f2, PermissionAPI.PERMISSION_READ, nrole));
        assertTrue(perm.doesRoleHavePermission(f2, PermissionAPI.PERMISSION_WRITE, nrole));
        
        perm.removePermissions(f2);
        perm.removePermissions(f1);
    }
    
    private boolean samePermissions(List<Permission> l1, List<Permission> l2) {
        boolean same=true;
        for(Permission p1 : l1) {
            boolean found=false;
            for(Permission p2 : l2)
                found=found || (
                   p1.getRoleId().equalsIgnoreCase(p2.getRoleId()) &&
                   p1.getPermission()==p2.getPermission()
                );
            same=same && found;
        }
        return same;
    }
    
    private void assertSamePermissions(List<Permission> l1, List<Permission> l2) {
        assertTrue(samePermissions(l1, l2));
    }
    
    private void assertNotSamePermissions(List<Permission> l1, List<Permission> l2) {
        assertFalse(samePermissions(l1, l2));
    }
    
    @Test
    public void getPermissions() throws DotDataException, DotSecurityException {
        Role nrole=new Role();
        nrole.setName("TestingRole4");
        nrole.setRoleKey("TestingRole4");
        nrole.setEditUsers(true);
        nrole.setEditPermissions(true);
        nrole.setEditLayouts(true);
        nrole.setDescription("Testing Role 4");
        APILocator.getRoleAPI().save(nrole);
        
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f1/", host, sysuser, false);
        assertSamePermissions(perm.getPermissions(f),perm.getPermissions(host));
        perm.permissionIndividually(host, f, sysuser, false);
        assertSamePermissions(perm.getPermissions(f),perm.getPermissions(host));
        
        Permission p1=new Permission();
        p1.setBitPermission(true);
        p1.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        perm.save(p1, f, sysuser, false);
        
        Permission p2=new Permission();
        p2.setBitPermission(true);
        p2.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p2.setPermission(PermissionAPI.PERMISSION_WRITE);
        p2.setRoleId(nrole.getId());
        perm.save(p2, f, sysuser, false);
        
        assertNotSamePermissions(perm.getPermissions(f),perm.getPermissions(host));
        
        ArrayList<Permission> list=new ArrayList<Permission>(perm.getPermissions(host));
        list.add(p1); list.add(p2);
        assertSamePermissions(perm.getPermissions(f), list);
        
        perm.removePermissions(f);
    }
    
    @Test
    public void getRolesWithPermission() throws DotDataException, DotSecurityException {
        Role nrole=new Role();
        nrole.setName("TestingRole6");
        nrole.setRoleKey("TestingRole6");
        nrole.setEditUsers(true);
        nrole.setEditPermissions(true);
        nrole.setEditLayouts(true);
        nrole.setDescription("Testing Role 6");
        APILocator.getRoleAPI().save(nrole);
        
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f2/", host, sysuser, false);
        perm.permissionIndividually(host, f, sysuser, false);
        
        Permission p1=new Permission();
        p1.setBitPermission(true);
        p1.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        perm.save(p1, f, sysuser, false);
        
        Permission p2=new Permission();
        p2.setBitPermission(true);
        p2.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p2.setPermission(PermissionAPI.PERMISSION_EDIT);
        p2.setRoleId(nrole.getId());
        perm.save(p2, f, sysuser, false);
        
        assertTrue(perm.getRolesWithPermission(f, PermissionAPI.PERMISSION_READ).contains(nrole));
        assertTrue(perm.getRolesWithPermission(f, PermissionAPI.PERMISSION_EDIT).contains(nrole));
        
        perm.removePermissions(f);
    }
    
    @Test
    public void getUsersWithPermission() throws DotDataException, DotSecurityException {
        Role nrole=new Role();
        nrole.setName("TestingRole5");
        nrole.setRoleKey("TestingRole5");
        nrole.setEditUsers(true);
        nrole.setEditPermissions(true);
        nrole.setEditLayouts(true);
        nrole.setDescription("Testing Role 5");
        APILocator.getRoleAPI().save(nrole);
        
        User user=APILocator.getUserAPI().createUser("useruseruser", "useruseruser@fake.org");
        APILocator.getUserAPI().save(user, sysuser, false);
        APILocator.getRoleAPI().addRoleToUser(nrole, user);
        
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f3/", host, sysuser, false);
        perm.permissionIndividually(host, f, sysuser, false);
        
        Permission p1=new Permission();
        p1.setBitPermission(true);
        p1.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        perm.save(p1, f, sysuser, false);

        Permission p2=new Permission();
        p2.setBitPermission(true);
        p2.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        p2.setPermission(PermissionAPI.PERMISSION_EDIT);
        p2.setRoleId(nrole.getId());
        perm.save(p2, f, sysuser, false);
        
        assertTrue(perm.getUsersWithPermission(f, PermissionAPI.PERMISSION_READ).contains(user));
        assertTrue(perm.getUsersWithPermission(f, PermissionAPI.PERMISSION_EDIT).contains(user));
        
        perm.removePermissions(f);
    }
    
    @Test
    public void save() {
        
    }
    
    @Test
    public void resetPermissionsUnder() {
        
    }
    
    @Test
    public void cascadePermissionUnder() {
        
    }
    
    @Test
    public void resetPermissionReferences() {
        
    }
    
    @Test
    public void resetChildrenPermissionReferences() {
        
    }
    
    @Test
    public void permissionIndividually() {
        
    }
    
    @Test
    public void findParentPermissionable() {
        
    }
    
    @Test
    public void isInheritingPermissions() {
        
    }
}
