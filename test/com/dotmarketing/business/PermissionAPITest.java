package com.dotmarketing.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.ajax.RoleAjax;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class PermissionAPITest extends TestBase {
    
    private static PermissionAPI perm;
    private static Host host;
    private static User sysuser;
    
    @BeforeClass
    public static void createTestHost() throws Exception {
        perm=APILocator.getPermissionAPI();
        sysuser=APILocator.getUserAPI().getSystemUser();
        host = new Host();
        host.setHostname("testhost.demo.dotcms.com");
        host=APILocator.getHostAPI().save(host, sysuser, false);
        
        perm.permissionIndividually(host.getParentPermissionable(), host, sysuser, false);
        
        Template tt=new Template();
        tt.setTitle("testtemplate");
        tt.setBody("<html><head></head><body>en empty template just for test</body></html>");
        APILocator.getTemplateAPI().saveTemplate(tt, host, sysuser, false);
        
        /*for(int w=1;w<=5;w++)
         for(int x=1;x<=5;x++)
          for(int y=1;y<=5;y++)
           for(int z=1;z<=5;z++) {
               String path="/f"+w+"/f"+x+"/f"+y+"/f"+z;
               Folder folder=APILocator.getFolderAPI().createFolders(path, host, sysuser, false);
          */     
               // a page under the folder
               /*HTMLPage page=new HTMLPage();
               page.setPageUrl("testpage.html");
               page.setFriendlyName("testpage");
               page.setTitle("testpage");
               APILocator.getHTMLPageAPI().saveHTMLPage(page, tt, folder, sysuser, false);*/
               
               // a file under the folder
               /*File file=new File();
               file.setTitle("testfile.txt");
               file.setFileName("testfile.txt");
               java.io.File fdata=java.io.File.createTempFile("tmpfile", "data.txt");
               FileWriter fw=new FileWriter(fdata);
               fw.write("test file in path "+path);
               fw.close();
               APILocator.getFileAPI().saveFile(file, fdata, folder, sysuser, false);*/
               
    //       }
    }
    
    @AfterClass
    public static void deleteTestHost() throws DotContentletStateException, DotDataException, DotSecurityException {
        APILocator.getHostAPI().archive(host, sysuser, false);
        APILocator.getHostAPI().delete(host, sysuser, false);
    }
    
    @Test
    public void doesRoleHavePermission() throws DotDataException, DotSecurityException {
        Role nrole=APILocator.getRoleAPI().loadRoleByKey("TestingRole");
        if(nrole==null || !UtilMethods.isSet(nrole.getId())) {
            nrole=new Role();
            nrole.setName("TestingRole");
            nrole.setRoleKey("TestingRole");
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription("Testing Role");
            APILocator.getRoleAPI().save(nrole);
        }
        
        Permission p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_EDIT);
        p.setRoleId(nrole.getId());
        p.setInode(host.getIdentifier());
        perm.save(p, host, sysuser, false);
        
        assertTrue(perm.doesRoleHavePermission(host, PermissionAPI.PERMISSION_EDIT, nrole));
        assertFalse(perm.doesRoleHavePermission(host, PermissionAPI.PERMISSION_PUBLISH, nrole));
        assertFalse(perm.doesRoleHavePermission(host, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, nrole));
    }
    
    @Test
    public void doesUserHavePermission() throws DotDataException, DotSecurityException {
        Role nrole=APILocator.getRoleAPI().loadRoleByKey("TestingRole2");
        if(nrole==null || !UtilMethods.isSet(nrole.getId())) {
            nrole=new Role();
            nrole.setName("TestingRole2");
            nrole.setRoleKey("TestingRole2");
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription("Testing Role 2");
            APILocator.getRoleAPI().save(nrole);
        }
        
        User user=null;
        try {
            user=APILocator.getUserAPI().loadUserById("useruser", sysuser, false);
        }
        catch(Exception ex) {
            user=null;
        }
        finally {
            if(user==null || !UtilMethods.isSet(user.getUserId())) {
                user=APILocator.getUserAPI().createUser("useruser", "user@fake.org");
                APILocator.getUserAPI().save(user, sysuser, false);
                user=APILocator.getUserAPI().loadUserById("useruser", sysuser, false);
            }
        }
        
        if(!APILocator.getRoleAPI().doesUserHaveRole(user, nrole))
            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        
        Permission p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_EDIT);
        p.setRoleId(nrole.getId());
        p.setInode(host.getIdentifier());
        perm.save(p, host, sysuser, false);
        
        assertTrue(perm.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT, user));
        assertFalse(perm.doesUserHavePermission(host, PermissionAPI.PERMISSION_PUBLISH, user));
        assertFalse(perm.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user));
    }
    
    @Test
    public void removePermissions() throws DotDataException, DotSecurityException {
        APILocator.getFolderAPI().createFolders("/f1/", host, sysuser, false);
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
        APILocator.getFolderAPI().createFolders("/f1/", host, sysuser, false);
        APILocator.getFolderAPI().createFolders("/f2/", host, sysuser, false);
        Folder f1=APILocator.getFolderAPI().findFolderByPath("/f1/", host, sysuser, false);
        Folder f2=APILocator.getFolderAPI().findFolderByPath("/f2/", host, sysuser, false);
        
        Role nrole=APILocator.getRoleAPI().loadRoleByKey("TestingRole3");
        if(nrole==null || !UtilMethods.isSet(nrole.getId())) {
            nrole=new Role();
            nrole.setName("TestingRole3");
            nrole.setRoleKey("TestingRole3");
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription("Testing Role 3");
            APILocator.getRoleAPI().save(nrole);
        }
        
        perm.permissionIndividually(host, f1, sysuser, false);
        perm.permissionIndividually(host, f2, sysuser, false);
        
        Permission p1=new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f1.getInode());
        perm.save(p1, f1, sysuser, false);
        
        Permission p2=new Permission();
        p2.setPermission(PermissionAPI.PERMISSION_WRITE);
        p2.setRoleId(nrole.getId());
        p2.setInode(f1.getInode());
        perm.save(p2, f1, sysuser, false);
        
        perm.copyPermissions(f1, f2);
        
        assertTrue(perm.doesRoleHavePermission(f2, PermissionAPI.PERMISSION_READ, nrole));
        assertTrue(perm.doesRoleHavePermission(f2, PermissionAPI.PERMISSION_WRITE, nrole));
        
        perm.removePermissions(f2);
        perm.removePermissions(f1);
    }
    
    @Test
    public void getPermissions() throws DotDataException, DotSecurityException {
        Role nrole=APILocator.getRoleAPI().loadRoleByKey("TestingRole4");
        if(!UtilMethods.isSet(nrole) || !UtilMethods.isSet(nrole.getId())) {
            nrole=new Role();
            nrole.setName("TestingRole4");
            nrole.setRoleKey("TestingRole4");
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription("Testing Role 4");
            APILocator.getRoleAPI().save(nrole);
        }
        APILocator.getFolderAPI().createFolders("/f1/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f1/", host, sysuser, false);
        perm.permissionIndividually(host, f, sysuser, false);
        
        Permission p1=new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        perm.save(p1, f, sysuser, false);
        
        Permission p2=new Permission();
        p2.setPermission(PermissionAPI.PERMISSION_WRITE);
        p2.setRoleId(nrole.getId());
        p2.setInode(f.getInode());
        perm.save(p2, f, sysuser, false);
        
        int pp=0;
        for(Permission p : perm.getPermissions(f,true))
            if(p.getRoleId().equals(nrole.getId()))
                pp = pp | p.getPermission();
        assertTrue(pp==(PermissionAPI.PERMISSION_READ|PermissionAPI.PERMISSION_WRITE));
        
        perm.removePermissions(f);
    }
    
    @Test
    public void getRolesWithPermission() throws DotDataException, DotSecurityException {
        Role nrole=APILocator.getRoleAPI().loadRoleByKey("TestingRole6");
        if(nrole==null || !UtilMethods.isSet(nrole.getId())) {
            nrole=new Role();
            nrole.setName("TestingRole6");
            nrole.setRoleKey("TestingRole6");
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription("Testing Role 6");
            APILocator.getRoleAPI().save(nrole);
        }
        APILocator.getFolderAPI().createFolders("/f2/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f2/", host, sysuser, false);
        perm.permissionIndividually(host, f, sysuser, false);
        
        Permission p1=new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        perm.save(p1, f, sysuser, false);
        
        Permission p2=new Permission();
        p2.setPermission(PermissionAPI.PERMISSION_EDIT);
        p2.setRoleId(nrole.getId());
        p2.setInode(f.getInode());
        perm.save(p2, f, sysuser, false);
        
        assertTrue(perm.getRolesWithPermission(f, PermissionAPI.PERMISSION_READ).contains(nrole));
        assertTrue(perm.getRolesWithPermission(f, PermissionAPI.PERMISSION_EDIT).contains(nrole));
        
        perm.removePermissions(f);
    }
    
    @Test
    public void getUsersWithPermission() throws DotDataException, DotSecurityException {
        Role nrole=APILocator.getRoleAPI().loadRoleByKey("TestingRole5");
        if(nrole==null || !UtilMethods.isSet(nrole.getId())) {
            nrole=new Role();
            nrole.setName("TestingRole5");
            nrole.setRoleKey("TestingRole5");
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription("Testing Role 5");
            APILocator.getRoleAPI().save(nrole);
        }
        
        User user=null;
        try {
            user=APILocator.getUserAPI().loadUserById("useruser", sysuser, false);
        }
        catch(Exception ex) {
            user=null;
        }
        finally {
            if(user==null || !UtilMethods.isSet(user.getUserId())) {
                user=APILocator.getUserAPI().createUser("useruser", "user@fake.org");
                APILocator.getUserAPI().save(user, sysuser, false);
                user=APILocator.getUserAPI().loadUserById("useruser", sysuser, false);
            }
        }
        
        if(!APILocator.getRoleAPI().doesUserHaveRole(user, nrole))
            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        
        APILocator.getFolderAPI().createFolders("/f3/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f3/", host, sysuser, false);
        perm.permissionIndividually(host, f, sysuser, false);
        
        Permission p1=new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        perm.save(p1, f, sysuser, false);

        Permission p2=new Permission();
        p2.setPermission(PermissionAPI.PERMISSION_EDIT);
        p2.setRoleId(nrole.getId());
        p2.setInode(f.getInode());
        perm.save(p2, f, sysuser, false);
        
        assertTrue(perm.getUsersWithPermission(f, PermissionAPI.PERMISSION_READ).contains(user));
        assertTrue(perm.getUsersWithPermission(f, PermissionAPI.PERMISSION_EDIT).contains(user));
        
        perm.removePermissions(f);
    }
    
    @Test
    public void save() throws DotStateException, DotDataException, DotSecurityException {
        Role nrole=APILocator.getRoleAPI().loadRoleByKey("TestingRole7");
        if(nrole==null || !UtilMethods.isSet(nrole.getId())) {
            nrole=new Role();
            nrole.setName("TestingRole7");
            nrole.setRoleKey("TestingRole7");
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription("Testing Role 7");
            APILocator.getRoleAPI().save(nrole);
        }
        
        APILocator.getFolderAPI().createFolders("/f4/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f4/", host, sysuser, false);
        perm.permissionIndividually(host, f, sysuser, false);
        
        ArrayList<Permission> permissions=new ArrayList<Permission>(perm.getPermissions(f));
        
        Permission p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_READ);
        p.setRoleId(nrole.getId());
        p.setInode(f.getInode());
        permissions.add(p);
        perm.save(p, f, sysuser, false);
        
        p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        p.setRoleId(nrole.getId());
        p.setInode(f.getInode());
        permissions.add(p);
        perm.save(p, f, sysuser, false);
        
        p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_EDIT);
        p.setRoleId(nrole.getId());
        p.setInode(f.getInode());
        permissions.add(p);
        perm.save(p, f, sysuser, false);
        
        p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_PUBLISH);
        p.setRoleId(nrole.getId());
        p.setInode(f.getInode());
        permissions.add(p);
        perm.save(p, f, sysuser, false);
        
        List<Permission> list=perm.getPermissions(f,true);
        int permV=PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT 
                | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_READ;
        for(Permission x : list)
            if(x.getRoleId().equals(nrole.getId()))
               assertTrue(x.getPermission()==permV);
        
        perm.removePermissions(f);
    }
    
    @Test
    public void resetPermissionsUnder() throws DotStateException, DotDataException, DotSecurityException {
        APILocator.getFolderAPI().createFolders("/f5/f1/f1/f1/", host, sysuser, false);
        Folder f1 = APILocator.getFolderAPI().findFolderByPath("/f5/", host, sysuser, false);
        Folder f2 = APILocator.getFolderAPI().findFolderByPath("/f5/f1", host, sysuser, false);
        Folder f3 = APILocator.getFolderAPI().findFolderByPath("/f5/f1/f1", host, sysuser, false);
        Folder f4 = APILocator.getFolderAPI().findFolderByPath("/f5/f1/f1/f1", host, sysuser, false);
        
        Structure s = new Structure();
        s.setHost(host.getIdentifier());
        s.setFolder(f4.getInode());
        s.setName("test_str_str_str");
        s.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        s.setOwner(sysuser.getUserId());
        s.setVelocityVarName("testtesttest");
        StructureFactory.saveStructure(s);
        StructureCache.addStructure(s);
        
        Field field = new Field("testtext", Field.FieldType.TEXT, Field.DataType.TEXT, s, 
                true, true, true, 3, "", "", "", true, false, true);
        field.setVelocityVarName("testtext");
        field.setListed(true);
        FieldFactory.saveField(field);
        FieldsCache.addField(field);
        
        field = new Field("f", Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, s,
                true, true, true, 4, "", "", "", true, false, true);
        field.setVelocityVarName("f");
        FieldFactory.saveField(field);
        FieldsCache.addField(field);
        
        Contentlet cont1=new Contentlet();
        cont1.setStructureInode(s.getInode());
        cont1.setStringProperty("testtext", "a test value");
        cont1.setHost(host.getIdentifier());
        cont1.setFolder(f4.getInode());
        cont1=APILocator.getContentletAPI().checkin(cont1, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(cont1.getInode());
        
        Contentlet cont2=new Contentlet();
        cont2.setStructureInode(s.getInode());
        cont2.setStringProperty("testtext", "another test value");
        cont2.setHost(host.getIdentifier());
        cont2.setFolder(f4.getInode());
        cont2=APILocator.getContentletAPI().checkin(cont2, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(cont2.getInode());
        
        perm.permissionIndividually(host, cont1, sysuser, false);
        perm.permissionIndividually(host, cont2, sysuser, false);
        perm.permissionIndividually(host, f4, sysuser, false);
        perm.permissionIndividually(host, f3, sysuser, false);
        perm.permissionIndividually(host, f2, sysuser, false);
        perm.permissionIndividually(host, f1, sysuser, false);
        
        
        assertFalse(perm.isInheritingPermissions(f1));
        assertFalse(perm.isInheritingPermissions(f2));
        assertFalse(perm.isInheritingPermissions(f3));
        assertFalse(perm.isInheritingPermissions(f4));
        assertFalse(perm.isInheritingPermissions(cont1));
        assertFalse(perm.isInheritingPermissions(cont2));
        
        perm.resetPermissionsUnder(f1);
        
        assertTrue(perm.isInheritingPermissions(f2));
        assertTrue(perm.isInheritingPermissions(f3));
        assertTrue(perm.isInheritingPermissions(f4));
        assertTrue(perm.isInheritingPermissions(cont1));
        assertTrue(perm.isInheritingPermissions(cont2));
        
    }
    
    @Test
    public void permissionIndividually() throws DotStateException, DotDataException, DotSecurityException {
        
    }
    
    /** 
     * https://github.com/dotCMS/dotCMS/issues/781
     * @throws DotSecurityException 
     * @throws DotDataException 
     * @throws SystemException 
     * @throws PortalException 
     */
    @Test
    public void issue781() throws DotDataException, DotSecurityException, PortalException, SystemException {
        Host hh = new Host();
        hh.setHostname("issue781.demo.dotcms.com");
        hh=APILocator.getHostAPI().save(hh, sysuser, false);
        
        Role nrole=APILocator.getRoleAPI().loadRoleByKey("TestingRole7");
        if(nrole==null || !UtilMethods.isSet(nrole.getId())) {
            nrole=new Role();
            nrole.setName("TestingRole7");
            nrole.setRoleKey("TestingRole7");
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription("Testing Role 7");
            APILocator.getRoleAPI().save(nrole);
        }
        
        try {
            Folder f1 = APILocator.getFolderAPI().createFolders("/f1/", hh, sysuser, false);
            Folder f2 = APILocator.getFolderAPI().createFolders("/f2/", hh, sysuser, false);
            Folder f3 = APILocator.getFolderAPI().createFolders("/f3/", hh, sysuser, false);
            Folder f4 = APILocator.getFolderAPI().createFolders("/f4/", hh, sysuser, false);
            
            CacheLocator.getPermissionCache().clearCache();
            
            // get them into cache
            perm.getPermissions(f1);
            perm.getPermissions(f2);
            
            Map<String,String> mm=new HashMap<String,String>();
            mm.put("individual",Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE));
            new RoleAjax().saveRolePermission(nrole.getId(), hh.getIdentifier(), mm, false);
            
            assertTrue(perm.findParentPermissionable(f4).equals(hh));
            assertTrue(perm.findParentPermissionable(f3).equals(hh));
            assertTrue(perm.findParentPermissionable(f2).equals(hh));
            assertTrue(perm.findParentPermissionable(f1).equals(hh));
        }
        finally {
            APILocator.getHostAPI().archive(hh, sysuser, false);
            APILocator.getHostAPI().delete(hh, sysuser, false);
        }
    }
    
    /**
     * https://github.com/dotCMS/dotCMS/issues/847
     * @throws DotDataException 
     * @throws DotSecurityException 
     * @throws DotHibernateException 
     */
    @Test
    public void issue847() throws DotHibernateException, DotSecurityException, DotDataException {
        Host hh = new Host();
        hh.setHostname("issue847.demo.dotcms.com");
        hh=APILocator.getHostAPI().save(hh, sysuser, false);
        try {
            Folder f1 = APILocator.getFolderAPI().createFolders("/hh1/", hh, sysuser, false);
            Folder f2 = APILocator.getFolderAPI().createFolders("/hh1/hh2/", hh, sysuser, false);
            
            Structure s = new Structure();
            s.setName("structure_issue847");
            s.setHost(hh.getIdentifier());
            s.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
            s.setOwner(sysuser.getUserId());
            s.setVelocityVarName("str847");
            StructureFactory.saveStructure(s);
            StructureCache.addStructure(s);
            
            Field field = new Field("testtext", Field.FieldType.TEXT, Field.DataType.TEXT, s, 
                    true, true, true, 3, "", "", "", true, false, true);
            field.setVelocityVarName("testtext");
            field.setListed(true);
            FieldFactory.saveField(field);
            FieldsCache.addField(field);
            
            field = new Field("f", Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, s,
                    true, true, true, 4, "", "", "", true, false, true);
            field.setVelocityVarName("f");
            FieldFactory.saveField(field);
            FieldsCache.addField(field);
            
            Contentlet cont1=new Contentlet();
            cont1.setStructureInode(s.getInode());
            cont1.setStringProperty("testtext", "a test value");
            cont1.setHost(hh.getIdentifier());
            cont1.setFolder(f2.getInode());
            cont1=APILocator.getContentletAPI().checkin(cont1, sysuser, false);
            APILocator.getContentletAPI().isInodeIndexed(cont1.getInode());
            
            perm.permissionIndividually(perm.findParentPermissionable(f1), f1, sysuser, false);
            assertTrue(perm.findParentPermissionable(cont1).equals(f1));
            
            perm.permissionIndividually(perm.findParentPermissionable(f2), f2, sysuser, false);
            CacheLocator.getPermissionCache().clearCache();
            assertTrue(perm.findParentPermissionable(cont1).equals(f2));
        }
        finally {
            APILocator.getHostAPI().archive(hh, sysuser, false);
            APILocator.getHostAPI().delete(hh, sysuser, false);   
        }
    }
    
}
