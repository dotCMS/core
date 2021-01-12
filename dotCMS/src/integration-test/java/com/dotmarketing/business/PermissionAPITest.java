package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.ajax.RoleAjax;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class tests the creation, copy, update, verification and setting of
 * permissions in dotCMS.
 * 
 * @author Jorge Urdaneta
 * @since May 14, 2012
 *
 */
public class PermissionAPITest extends IntegrationTestBase {

    private static PermissionAPI permissionAPI;
    private static Host host;
    private static User sysuser;
    private static Template template;

    @BeforeClass
    public static void createTestHost() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        
        permissionAPI =APILocator.getPermissionAPI();
        sysuser=APILocator.getUserAPI().getSystemUser();
        host = new Host();
        host.setHostname("testhost.demo.dotcms.com");
        try{
        	HibernateUtil.startTransaction();
            host=APILocator.getHostAPI().save(host, sysuser, false);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	host = APILocator.getHostAPI().findByName("testhost.demo.dotcms.com", sysuser, false);
        	Logger.error(PermissionAPITest.class, e.getMessage());
        } finally {
            HibernateUtil.closeSessionSilently();
        }
 
        try{
            permissionAPI.permissionIndividually(host.getParentPermissionable(), host, sysuser);
        }catch(DotDataException e){
            Logger.warn(PermissionAPITest.class, "Host Individual Permissions were already set. Reaplying permissions.");
            permissionAPI.removePermissions(host);
            permissionAPI.permissionIndividually(host.getParentPermissionable(), host, sysuser);
        }
        template =new Template();
        template.setTitle("testtemplate");
        template.setBody("<html><head></head><body>en empty template just for test</body></html>");
        APILocator.getTemplateAPI().saveTemplate(template, host, sysuser, false);
    }

    @AfterClass
    public static void deleteTestHost() throws DotContentletStateException, DotDataException, DotSecurityException {
        try{
        	HibernateUtil.startTransaction();
            APILocator.getHostAPI().archive(host, sysuser, false);
            APILocator.getHostAPI().delete(host, sysuser, false);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(PermissionAPITest.class, e.getMessage());
        }finally {
            HibernateUtil.closeSessionSilently();
        }
        
    }

    @Test
    public void doesRoleHavePermission() throws DotDataException, DotSecurityException {
        Role nrole=getRole("TestingRole");

        Permission p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_EDIT);
        p.setRoleId(nrole.getId());
        p.setInode(host.getIdentifier());
        permissionAPI.save(p, host, sysuser, false);

        assertTrue(permissionAPI.doesRoleHavePermission(host, PermissionAPI.PERMISSION_EDIT, nrole));
        assertFalse(permissionAPI.doesRoleHavePermission(host, PermissionAPI.PERMISSION_PUBLISH, nrole));
        assertFalse(permissionAPI.doesRoleHavePermission(host, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, nrole));
    }

    @Test
    public void doesUserHavePermission() throws DotDataException, DotSecurityException {
        Role nrole=getRole("TestingRole2");

        User user= UserTestUtil.getUser("useruser", false, true);

        if(!APILocator.getRoleAPI().doesUserHaveRole(user, nrole))
            APILocator.getRoleAPI().addRoleToUser(nrole, user);

        Permission p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_EDIT);
        p.setRoleId(nrole.getId());
        p.setInode(host.getIdentifier());
        permissionAPI.save(p, host, sysuser, false);

        assertTrue(permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT, user));
        assertFalse(permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_PUBLISH, user));
        assertFalse(permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user));

        /*should throw an error if the permissionable is null*/
        boolean throwException = false;
        try{
        	permissionAPI.doesUserHavePermission(null, PermissionAPI.PERMISSION_READ, user);
        }catch(NullPointerException e){
        	throwException=true;
        }
        assertTrue(throwException);
        
        throwException = false;

        try{
        	permissionAPI.doesUserHavePermission(null, PermissionAPI.PERMISSION_READ, user, false);
        }catch(NullPointerException e){
        	throwException=true;
        }
        assertTrue(throwException);
        
        throwException = false;
        try{
        	permissionAPI.doesUserHavePermissions(null, "HTMLPAGES", user, false) ;
        }catch(NullPointerException e){
        	throwException=true;
        }
        assertTrue(throwException);
        
    }

    /**
     * Method to test: {@link PermissionAPI#doesUserHavePermissions(String, PermissionableType, int, User)}
     * Given Scenario: Give a limited user permissions to edit Categories over the System Host, check permissions against System Host.
     * ExpectedResult: true since the user has permissions over the permissionable type and the asset.
     */
    @Test
    public void test_doesUserHavePermissions_checkSameAssetIdThatPermissionsWereGiven_returnTrue()
            throws DotDataException, DotSecurityException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();

        //Give Permissions Over the System Host
        final Permission permissions = new Permission(PermissionableType.CATEGORY.getCanonicalName(),
                APILocator.systemHost().getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true);
        permissionAPI.save(permissions, APILocator.systemHost(), sysuser, false);

        //Check Permissions
        final boolean doesUserHavePermissions =
                permissionAPI.doesUserHavePermissions(APILocator.systemHost().getIdentifier(),PermissionableType.CATEGORY,PermissionAPI.PERMISSION_EDIT,limitedUser);
        assertTrue(doesUserHavePermissions);
    }

    /**
     * Method to test: {@link PermissionAPI#doesUserHavePermissions(String, PermissionableType, int, User)}
     * Given Scenario: Create a category as asmin, Give a limited user permissions to edit the category, check permissions against System Host.
     * ExpectedResult: false since the user has permissions over the permissionable type but no the System Host.
     */
    @Test
    public void test_doesUserHavePermissions_checkDiffAssetIdThatPermissionsWereGiven_returnFalse()
            throws DotDataException, DotSecurityException {
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();

        //Create new top level Category as admin user
        final long currentTime = System.currentTimeMillis();
        final Category parentCategory = new CategoryDataGen()
                .setCategoryName("CT-Category-Parent"+currentTime)
                .setKey("parent"+currentTime)
                .setCategoryVelocityVarName("parent"+currentTime)
                .nextPersisted();

        //Give Permissions Over the Category
        final Permission permissions = new Permission(PermissionableType.CATEGORY.getCanonicalName(),
                parentCategory.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true);
        permissionAPI.save(permissions, parentCategory, sysuser, false);

        //Check Permissions
        final boolean doesUserHavePermissions =
                permissionAPI.doesUserHavePermissions(APILocator.systemHost().getIdentifier(),PermissionableType.CATEGORY,PermissionAPI.PERMISSION_EDIT,limitedUser);
        assertFalse(doesUserHavePermissions);
    }

    /**
     * Method to test:  doesUserHavePermission
     * Given Scenario:  when calls  doesUserHavePermission, to a contentlet with a diff language (not the default) with or not permission, the non-default language was throwing an exception
     * ExpectedResult: Now should work on and just return true or false instead of a non-working version exception
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void doesUserHavePermission_another_lang() throws DotDataException, DotSecurityException {

        final Role nrole = getRole("TestingRole2");
        final User user= UserTestUtil.getUser("useruser", false, true);

        if(!APILocator.getRoleAPI().doesUserHaveRole(user, nrole)) {

            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        }

        final Language defaultLanguage         = APILocator.getLanguageAPI().getDefaultLanguage();
        final ContentTypeAPI contentTypeAPI    = APILocator.getContentTypeAPI(APILocator.systemUser());
        final ContentType contentGenericType   = contentTypeAPI.find("webPageContent");
        final Contentlet contentletDefaultLang = new ContentletDataGen(contentGenericType.id())
                .setProperty("title", "TestContent")
                .setProperty("body",  "TestBody" ).languageId(defaultLanguage.getId()).nextPersisted();
        final Language newLanguage             = new LanguageDataGen().languageCode("es").country("MX").nextPersisted();

        assertFalse(permissionAPI.doesUserHavePermission(contentletDefaultLang, PermissionAPI.PERMISSION_USE, user, PageMode.get().respectAnonPerms));

        try {
            contentletDefaultLang.setLanguageId(newLanguage.getId());
            assertFalse(permissionAPI.doesUserHavePermission(contentletDefaultLang, PermissionAPI.PERMISSION_USE, user, PageMode.get().respectAnonPerms));
        } catch (DotStateException e) {
            fail("When asking for permission even if the contentlet is on a language without working version, shouldn't throw DotStateException");
        }
    }

    @Test
    public void removePermissions() throws DotDataException, DotSecurityException {

        final Host newHost = new SiteDataGen().nextPersisted();
        Role newRole = getRole("Role" + System.currentTimeMillis());

        // Adding permissions to the just created host
        Permission permission = new Permission();
        permission.setPermission(PermissionAPI.PERMISSION_EDIT);
        permission.setRoleId(newRole.getId());
        permission.setInode(newHost.getIdentifier());
        permissionAPI.save(permission, newHost, sysuser, false);

        APILocator.getFolderAPI().createFolders("/f1/", newHost, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f1/", newHost, sysuser, false);

        assertTrue(permissionAPI.isInheritingPermissions(f));
        assertEquals(f.getParentPermissionable().getPermissionId(), newHost.getPermissionId());

        permissionAPI.permissionIndividually(newHost, f, sysuser);
        assertFalse(permissionAPI.isInheritingPermissions(f));

        permissionAPI.removePermissions(f);

        assertTrue(permissionAPI.isInheritingPermissions(f));
        assertEquals(f.getParentPermissionable().getPermissionId(), newHost.getPermissionId());
    }

    @Test
    public void copyPermissions() throws DotDataException, DotSecurityException {
        APILocator.getFolderAPI().createFolders("/f1/", host, sysuser, false);
        APILocator.getFolderAPI().createFolders("/f2/", host, sysuser, false);
        Folder f1=APILocator.getFolderAPI().findFolderByPath("/f1/", host, sysuser, false);
        Folder f2=APILocator.getFolderAPI().findFolderByPath("/f2/", host, sysuser, false);

        Role nrole=getRole("TestingRole3");

        permissionAPI.permissionIndividually(host, f1, sysuser);
        permissionAPI.permissionIndividually(host, f2, sysuser);

        Permission p1=new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f1.getInode());
        permissionAPI.save(p1, f1, sysuser, false);

        Permission p2=new Permission();
        p2.setPermission(PermissionAPI.PERMISSION_WRITE);
        p2.setRoleId(nrole.getId());
        p2.setInode(f1.getInode());
        permissionAPI.save(p2, f1, sysuser, false);

        permissionAPI.copyPermissions(f1, f2);

        assertTrue(permissionAPI.doesRoleHavePermission(f2, PermissionAPI.PERMISSION_READ, nrole));
        assertTrue(permissionAPI.doesRoleHavePermission(f2, PermissionAPI.PERMISSION_WRITE, nrole));

        permissionAPI.removePermissions(f2);
        permissionAPI.removePermissions(f1);
    }

    @Test
    public void getPermissions() throws DotDataException, DotSecurityException {
        Role nrole=getRole("TestingRole4");

        APILocator.getFolderAPI().createFolders("/f1/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f1/", host, sysuser, false);
        permissionAPI.permissionIndividually(host, f, sysuser, false);

        Permission p1=new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        Permission p2=new Permission();
        p2.setPermission(PermissionAPI.PERMISSION_WRITE);
        p2.setRoleId(nrole.getId());
        p2.setInode(f.getInode());
        permissionAPI.save(p2, f, sysuser, false);

        int pp=0;
        for(Permission p : permissionAPI.getPermissions(f,true))
            if(p.getRoleId().equals(nrole.getId()))
                pp = pp | p.getPermission();
        assertTrue(pp==(PermissionAPI.PERMISSION_READ|PermissionAPI.PERMISSION_WRITE));

        permissionAPI.removePermissions(f);
    }

    @Test
    public void getRolesWithPermission() throws DotDataException, DotSecurityException {
        Role nrole=getRole("TestingRole6");

        APILocator.getFolderAPI().createFolders("/f2/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f2/", host, sysuser, false);
        permissionAPI.permissionIndividually(host, f, sysuser, false);

        Permission p1=new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        Permission p2=new Permission();
        p2.setPermission(PermissionAPI.PERMISSION_EDIT);
        p2.setRoleId(nrole.getId());
        p2.setInode(f.getInode());
        permissionAPI.save(p2, f, sysuser, false);

        assertTrue(permissionAPI.getRolesWithPermission(f, PermissionAPI.PERMISSION_READ).contains(nrole));
        assertTrue(permissionAPI.getRolesWithPermission(f, PermissionAPI.PERMISSION_EDIT).contains(nrole));

        permissionAPI.removePermissions(f);
    }

    @Test
    public void getUsersWithPermission() throws DotDataException, DotSecurityException {
        Role nrole=getRole("TestingRole5");

        User user= UserTestUtil.getUser("useruser", false, true);

        if(!APILocator.getRoleAPI().doesUserHaveRole(user, nrole))
            APILocator.getRoleAPI().addRoleToUser(nrole, user);

        APILocator.getFolderAPI().createFolders("/f3/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f3/", host, sysuser, false);
        permissionAPI.permissionIndividually(host, f, sysuser, false);

        Permission p1=new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        Permission p2=new Permission();
        p2.setPermission(PermissionAPI.PERMISSION_EDIT);
        p2.setRoleId(nrole.getId());
        p2.setInode(f.getInode());
        permissionAPI.save(p2, f, sysuser, false);

        assertTrue(permissionAPI.getUsersWithPermission(f, PermissionAPI.PERMISSION_READ).contains(user));
        assertTrue(permissionAPI.getUsersWithPermission(f, PermissionAPI.PERMISSION_EDIT).contains(user));

        permissionAPI.removePermissions(f);
    }

    @Test
    public void save() throws DotStateException, DotDataException, DotSecurityException {
        Role nrole=getRole("TestingRole7");

        APILocator.getFolderAPI().createFolders("/f4/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f4/", host, sysuser, false);
        permissionAPI.permissionIndividually(host, f, sysuser, false);

        ArrayList<Permission> permissions=new ArrayList<Permission>(permissionAPI.getPermissions(f));

        Permission p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_READ);
        p.setRoleId(nrole.getId());
        p.setInode(f.getInode());
        permissions.add(p);
        permissionAPI.save(p, f, sysuser, false);

        p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        p.setRoleId(nrole.getId());
        p.setInode(f.getInode());
        permissions.add(p);
        permissionAPI.save(p, f, sysuser, false);

        p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_EDIT);
        p.setRoleId(nrole.getId());
        p.setInode(f.getInode());
        permissions.add(p);
        permissionAPI.save(p, f, sysuser, false);

        p=new Permission();
        p.setPermission(PermissionAPI.PERMISSION_PUBLISH);
        p.setRoleId(nrole.getId());
        p.setInode(f.getInode());
        permissions.add(p);
        permissionAPI.save(p, f, sysuser, false);

        List<Permission> list= permissionAPI.getPermissions(f,true);
        int permV=PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT
                | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_READ;
        for(Permission x : list)
            if(x.getRoleId().equals(nrole.getId()))
               assertTrue(x.getPermission()==permV);

        permissionAPI.removePermissions(f);
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

        Role nrole=getRole("TestingRole7");

        try {
            Folder f1 = APILocator.getFolderAPI().createFolders("/f1/", hh, sysuser, false);
            Folder f2 = APILocator.getFolderAPI().createFolders("/f2/", hh, sysuser, false);
            Folder f3 = APILocator.getFolderAPI().createFolders("/f3/", hh, sysuser, false);
            Folder f4 = APILocator.getFolderAPI().createFolders("/f4/", hh, sysuser, false);

            CacheLocator.getPermissionCache().clearCache();

            // get them into cache
            permissionAPI.getPermissions(f1);
            permissionAPI.getPermissions(f2);

            Map<String,String> mm=new HashMap<String,String>();
            mm.put("individual",Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE));
            new RoleAjax().saveRolePermission(nrole.getId(), hh.getIdentifier(), mm, false);

            assertTrue(permissionAPI.findParentPermissionable(f4).equals(hh));
            assertTrue(permissionAPI.findParentPermissionable(f3).equals(hh));
            assertTrue(permissionAPI.findParentPermissionable(f2).equals(hh));
            assertTrue(permissionAPI.findParentPermissionable(f1).equals(hh));
        }
        finally {
            try{
            	HibernateUtil.startTransaction();
                APILocator.getHostAPI().archive(hh, sysuser, false);
                APILocator.getHostAPI().delete(hh, sysuser, false);
            	HibernateUtil.closeAndCommitTransaction();
            }catch(Exception e){
            	HibernateUtil.rollbackTransaction();
            	Logger.error(PermissionAPITest.class, e.getMessage());
            }

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
        Structure s=null;
        Host hh = new Host();
        hh.setHostname("issue847.demo.dotcms.com");
        hh=APILocator.getHostAPI().save(hh, sysuser, false);
        try {
            Folder f1 = APILocator.getFolderAPI().createFolders("/hh1/", hh, sysuser, false);
            Folder f2 = APILocator.getFolderAPI().createFolders("/hh1/hh2/", hh, sysuser, false);

            s = new Structure();
            s.setName("structure_issue847");
            s.setHost(hh.getIdentifier());
            s.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
            s.setOwner(sysuser.getUserId());
            s.setVelocityVarName("str847"+System.currentTimeMillis());
            StructureFactory.saveStructure(s);
            CacheLocator.getContentTypeCache().add(s);

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
            cont1.setIndexPolicy(IndexPolicy.FORCE);
            cont1=APILocator.getContentletAPI().checkin(cont1, sysuser, false);

            permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(f1), f1, sysuser);
            assertTrue(permissionAPI.findParentPermissionable(cont1).equals(f1));

            permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(f2), f2, sysuser);
            CacheLocator.getPermissionCache().clearCache();
            assertTrue(permissionAPI.findParentPermissionable(cont1).equals(f2));
        }
        finally {
            try{
            	HibernateUtil.startTransaction();
                APILocator.getHostAPI().archive(hh, sysuser, false);
                APILocator.getHostAPI().delete(hh, sysuser, false);
            	HibernateUtil.closeAndCommitTransaction();
            }catch(Exception e){
            	HibernateUtil.rollbackTransaction();
            	Logger.error(PermissionAPITest.class, e.getMessage());
            }
        }
    }

    /**
     * https://github.com/dotCMS/dotCMS/issues/886
     *
     * @throws Exception
     */
    @Test
    public void issue886() throws Exception {
        Host hh = new Host();
        try {
            hh.setHostname("issue886.demo.dotcms.com");
            hh = APILocator.getHostAPI().save(hh, sysuser, false);
        } catch(Exception e) {
            hh = APILocator.getHostAPI().findByName("issue886.demo.dotcms.com", sysuser, false);
        }

        try {
            Folder folderA = APILocator.getFolderAPI().createFolders("/ax/", hh, sysuser, false);
            Folder b = APILocator.getFolderAPI().createFolders("/ax/b/", hh, sysuser, false);
            Folder c = APILocator.getFolderAPI().createFolders("/ax/b/c/", hh, sysuser, false);

            permissionAPI.permissionIndividually(APILocator.getHostAPI().findSystemHost(), folderA, sysuser);

            String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
            
            HTMLPageAsset pageAssetFolderA = new HTMLPageDataGen(folderA, template).nextPersisted();
            HTMLPageAsset pb = new HTMLPageDataGen(b, template).nextPersisted();
            HTMLPageAsset pc = new HTMLPageDataGen(c, template).nextPersisted();

            java.io.File fdata=java.io.File.createTempFile("tmpfile", "data.txt");
            FileWriter fw=new FileWriter(fdata);
            fw.write("test file");
            fw.close();

            String FileAssetStInode=CacheLocator.getContentTypeCache().getStructureByVelocityVarName(
                    FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode();

            Contentlet ca=new Contentlet();
            ca.setStructureInode(FileAssetStInode);
            ca.setStringProperty(FileAssetAPI.TITLE_FIELD, "testfileasset.txt");
            ca.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "testfileasset.txt");
            java.io.File cadata=java.io.File.createTempFile("tmpfile", "cdata.txt");
            FileUtils.copyFile(fdata, cadata);
            ca.setBinary(FileAssetAPI.BINARY_FIELD, cadata);
            ca.setHost(hh.getIdentifier());
            ca.setFolder(folderA.getInode());
            ca.setIndexPolicy(IndexPolicy.FORCE);
            ca=APILocator.getContentletAPI().checkin(ca, sysuser, false);

            Contentlet cb=new Contentlet();
            cb.setStructureInode(FileAssetStInode);
            cb.setStringProperty(FileAssetAPI.TITLE_FIELD, "testfileasset.txt");
            cb.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "testfileasset.txt");
            java.io.File cbdata=java.io.File.createTempFile("tmpfile", "cdata.txt");
            FileUtils.copyFile(fdata, cbdata);
            cb.setBinary(FileAssetAPI.BINARY_FIELD, cbdata);
            cb.setHost(hh.getIdentifier());
            cb.setFolder(b.getInode());
            cb.setIndexPolicy(IndexPolicy.FORCE);
            cb=APILocator.getContentletAPI().checkin(cb, sysuser, false);

            Contentlet cc=new Contentlet();
            cc.setStructureInode(FileAssetStInode);
            cc.setStringProperty(FileAssetAPI.TITLE_FIELD, "testfileasset.txt");
            cc.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "testfileasset.txt");
            java.io.File ccdata=java.io.File.createTempFile("tmpfile", "cdata.txt");
            FileUtils.copyFile(fdata, ccdata);
            cc.setBinary(FileAssetAPI.BINARY_FIELD, ccdata);
            cc.setHost(hh.getIdentifier());
            cc.setFolder(c.getInode());
            cc.setIndexPolicy(IndexPolicy.FORCE);
            cc=APILocator.getContentletAPI().checkin(cc, sysuser, false);

            // get them into cache
            permissionAPI.getPermissions(folderA);   permissionAPI.getPermissions(ca);
            permissionAPI.getPermissions(b);   permissionAPI.getPermissions(cb);
            permissionAPI.getPermissions(c);   permissionAPI.getPermissions(cc);
            permissionAPI.getPermissions(pageAssetFolderA);
            permissionAPI.getPermissions(pb);
            permissionAPI.getPermissions(pc);

            // permission individually on folder a
            permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(folderA), folderA, sysuser);

            // everybody should be inheriting from a
            assertTrue(permissionAPI.findParentPermissionable(pageAssetFolderA).equals(folderA));
            assertTrue(permissionAPI.findParentPermissionable(ca).equals(folderA));
            assertTrue(permissionAPI.findParentPermissionable(b).equals(folderA));
            assertTrue(permissionAPI.findParentPermissionable(pb).equals(folderA));
            assertTrue(permissionAPI.findParentPermissionable(cb).equals(folderA));
            assertTrue(permissionAPI.findParentPermissionable(c).equals(folderA));
            assertTrue(permissionAPI.findParentPermissionable(pc).equals(folderA));
            assertTrue(permissionAPI.findParentPermissionable(cc).equals(folderA));
        }
        finally {
            try {
                APILocator.getHostAPI().archive(hh, sysuser, false);
                APILocator.getHostAPI().delete(hh, sysuser, false);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	/**
	 * Verifies that content inheriting from folder is displaying new roles
	 * added to folder (see <a
	 * href="https://github.com/dotCMS/core/issues/560">issue 560</a>).
	 * 
	 * @throws Exception
	 */
	@Test
    public void issue560() throws Exception {
        Host hh = new Host();
        hh.setHostname("issue560_"+System.currentTimeMillis()+".demo.dotcms.com");
        hh=APILocator.getHostAPI().save(hh, sysuser, false);

        Role nrole1 = getRole("TestingRole8");

        Role nrole2 = getRole("TestingRole9");

        Structure s=null;
        Contentlet cont1=null;
        try {
            Folder a = APILocator.getFolderAPI().createFolders("/a/", hh, sysuser, false);
            permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(a), a, sysuser);

            s = new Structure();
            s.setHost(hh.getIdentifier());
            s.setFolder(a.getInode());
            s.setName("issue560");
            s.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
            s.setOwner(sysuser.getUserId());
            s.setVelocityVarName("issue560"+System.currentTimeMillis());
            StructureFactory.saveStructure(s);
            CacheLocator.getContentTypeCache().add(s);

            Field field = new Field("testtext", Field.FieldType.TEXT, Field.DataType.TEXT, s,
                    true, true, true, 3, "", "", "", true, false, true);
            field.setVelocityVarName("testtext");
            field.setListed(true);
            FieldFactory.saveField(field);
            FieldsCache.addField(field);


            Map<String,String> mm=new HashMap<String,String>();
            mm.put("individual",Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN));
            mm.put("structures", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
            mm.put("content", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
            mm.put("pages", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
            mm.put("folders", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN));
            new RoleAjax().saveRolePermission(nrole1.getId(), a.getInode(), mm, false);

            cont1=new Contentlet();
            cont1.setStructureInode(s.getInode());
            cont1.setStringProperty("testtext", "a test value");
            cont1.setIndexPolicy(IndexPolicy.FORCE);
            cont1=APILocator.getContentletAPI().checkin(cont1, sysuser, false);

            permissionAPI.getPermissions(cont1); // to cache

            new RoleAjax().saveRolePermission(nrole2.getId(), a.getInode(), mm, false);

            boolean found1=false,found2=false;
            for(Permission p : permissionAPI.getPermissions(cont1)) {
                found1 = found1 || p.getRoleId().equals(nrole1.getId());
                found2 = found2 || p.getRoleId().equals(nrole2.getId());
            }

            assertTrue(found1);
            assertTrue(found2);
        }
        finally {
            try {

                if (cont1 != null) {

                    APILocator.getContentletAPI().destroy(cont1, sysuser, false);
                }
                if (s != null) {
                    APILocator.getStructureAPI().delete(s, sysuser);
                }
                APILocator.getHostAPI().archive(hh, sysuser, false);
                APILocator.getHostAPI().delete(hh, sysuser, false);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void issue1073() throws Exception {

    	Folder m1 = APILocator.getFolderAPI().createFolders("/m1/", host, sysuser, false);
    	Folder m2 = APILocator.getFolderAPI().createFolders("/m1/m2/", host, sysuser, false);
    	Folder m3 = APILocator.getFolderAPI().createFolders("/m1/m2/m3/", host, sysuser, false);

    	permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(m1), m1, sysuser, false);
    	permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(m2), m2, sysuser, false);
    	permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(m3), m3, sysuser, false);

    	Role nrole=getRole("TestingRole");

    	Permission p=new Permission(m1.getInode(),nrole.getId(),PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,false);
    	permissionAPI.save(p, m1, sysuser, false);

    	permissionAPI.cascadePermissionUnder(m1, nrole);

    }

    /**
     * Creates a host, a role and a map of permissions over the templateLayouts.
     * Creates a new template and check that the parentPermissionable of it is the host,
     * so it inherits the permissions of it.
     *
     */
    @Test
    public void test_templateLayout_parentPermissionableIsHost() throws Exception {

        Host hh = new Host();
        hh.setHostname("issue1112.demo.dotcms.com");
        hh=APILocator.getHostAPI().save(hh, sysuser, false);
        final Folder folderTheme = new FolderDataGen().site(hh).title("themeFolder"+System.currentTimeMillis()).nextPersisted();

        Role nrole=getRole("TestingRole10");

        Map<String,String> mm=new HashMap<String,String>();
        mm.put("templateLayouts", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
        RoleAjax roleAjax = new RoleAjax();
        roleAjax.saveRolePermission(nrole.getId(), hh.getIdentifier(), mm, false);
        PermissionAPI permAPI = APILocator.getPermissionAPI();
        List<Permission> perms = permAPI.getPermissionsByRole(nrole, true, true);

        for (Permission p : perms) {
            if(p!=null) {
                assertTrue(p.getType().equals(TemplateLayout.class.getCanonicalName()));
                assertTrue(p.getPermission()==(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
            }

        }


        try {
            Template t = new Template();
            t.setBody("\"<html>\\n <head>\\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"/html/css/template/reset-fonts-grids.css\" />\\n </head>\\n <body>\\n  <div id=\"doc3-template\" name=\"globalContainer\">\\n   <div id=\"hd-template\"></div>\\n   <div id=\"bd-template\">\\n    <div id=\"yui-main-template\">\\n     <div class=\"yui-b-template\" id=\"splitBody0\"></div>\\n    </div>\\n   </div>\\n   <div id=\"ft-template\"></div>\\n  </div>\\n </body>\\n</html>\"");
            t.setCountAddContainer(3);
            t.setCountContainers(0);
            t.setDrawed(true);
            t.setDrawedBody("\"<div id=\"doc3-template\" name=\"globalContainer\"><div id=\"hd-template\"><div class=\"addContainerSpan\"><a href=\"javascript: showAddContainerDialog('hd-template');\" title=\"Add Container\"><span class=\"plusBlueIcon\"></span>Add Container</a></div><h1>Header</h1></div><div id=\"bd-template\"><div id=\"yui-main-template\"><div class=\"yui-b-template\" id=\"splitBody0\"><div class=\"addContainerSpan\"><a href=\"javascript: showAddContainerDialog('splitBody0');\" title=\"Add Container\"><span class=\"plusBlueIcon\"></span>Add Container</a></div><h1>Body</h1></div></div></div><div id=\"ft-template\"><div class=\"addContainerSpan\"><a href=\"javascript: showAddContainerDialog('ft-template');\" title=\"Add Container\"><span class=\"plusBlueIcon\"></span>Add Container</a></div><h1>Footer</h1></div></div>\"");
            t.setiDate(new Date());
            t.setTitle("testTemplate");
            t.setTheme(folderTheme.getIdentifier());


            APILocator.getTemplateAPI().saveTemplate(t,hh, sysuser, false);


            assertTrue(permissionAPI.findParentPermissionable(t).equals(hh));
        }
        finally {
            try{
                HibernateUtil.startTransaction();
                APILocator.getHostAPI().archive(hh, sysuser, false);
                APILocator.getHostAPI().delete(hh, sysuser, false);
                HibernateUtil.closeAndCommitTransaction();
            }catch(Exception e){
                HibernateUtil.rollbackTransaction();
                Logger.error(PermissionAPITest.class, e.getMessage());
            }
        }

    }

    @Test
    public void testGetUsersWithoutFilter() throws DotDataException, DotSecurityException {

        Role nrole = getRole("TestingRole11");

        User user = UserTestUtil.getUser("useruser", false, true);

        if (!APILocator.getRoleAPI().doesUserHaveRole(user, nrole)) {
            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        }

        APILocator.getFolderAPI().createFolders("/f11/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f11/", host, sysuser, false);

        Permission p1 = new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        List<User> users = permissionAPI.getUsers(f.getInode(), PermissionAPI.PERMISSION_READ, null, -1, -1);

        assertNotNull(users);
        assertTrue(users.size() > 0);
        assertTrue(users.contains(user));

        APILocator.getFolderAPI().delete(f, sysuser, false);
    }

    @Test
    public void testGetUsersWithFilter() throws DotDataException, DotSecurityException {

        Role nrole = getRole("TestingRole11");

        User user = UserTestUtil.getUser("useruser", false, true);

        if (!APILocator.getRoleAPI().doesUserHaveRole(user, nrole)) {
            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        }

        APILocator.getFolderAPI().createFolders("/f11/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f11/", host, sysuser, false);

        Permission p1 = new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        List<User> users = permissionAPI.getUsers(f.getInode(), PermissionAPI.PERMISSION_READ, "useruser", -1, -1);

        assertNotNull(users);
        assertTrue(users.size() == 1);
        assertTrue(users.contains(user));

        APILocator.getFolderAPI().delete(f, sysuser, false);
    }

    @Test
    public void testGetUsersCountWithoutFilter() throws DotDataException, DotSecurityException {

        Role nrole = getRole("TestingRole11");

        User user = UserTestUtil.getUser("useruser", false, true);

        if (!APILocator.getRoleAPI().doesUserHaveRole(user, nrole)) {
            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        }

        APILocator.getFolderAPI().createFolders("/f11/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f11/", host, sysuser, false);

        Permission p1 = new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        int count = permissionAPI.getUserCount(f.getInode(), PermissionAPI.PERMISSION_READ, null);

        assertTrue(count > 0);

        APILocator.getFolderAPI().delete(f, sysuser, false);
    }

    @Test
    public void testGetUsersCountWithFilter() throws DotDataException, DotSecurityException {

        Role nrole = getRole("TestingRole11");

        User user = UserTestUtil.getUser("useruser", false, true);

        if (!APILocator.getRoleAPI().doesUserHaveRole(user, nrole)) {
            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        }

        APILocator.getFolderAPI().createFolders("/f11/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f11/", host, sysuser, false);

        Permission p1 = new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        int count = permissionAPI.getUserCount(f.getInode(), PermissionAPI.PERMISSION_READ, "useruser");

        assertTrue(count == 1);

        APILocator.getFolderAPI().delete(f, sysuser, false);
    }

    @Test
    public void testGetUsersCountDeleted() throws DotDataException, DotSecurityException {

        Role nrole = getRole("TestingRole11");

        User user = UserTestUtil.getUser("deletedUser", true, true);

        if (!APILocator.getRoleAPI().doesUserHaveRole(user, nrole)) {
            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        }

        APILocator.getFolderAPI().createFolders("/f11/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f11/", host, sysuser, false);

        Permission p1 = new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        int count = permissionAPI.getUserCount(f.getInode(), PermissionAPI.PERMISSION_READ, "deletedUser");

        assertTrue(count == 0);

        APILocator.getFolderAPI().delete(f, sysuser, false);
    }

    @Test
    public void testGetUsersDeleted() throws DotDataException, DotSecurityException {

        Role nrole = getRole("TestingRole11");

        User user = UserTestUtil.getUser("deletedUser", true, true);

        if (!APILocator.getRoleAPI().doesUserHaveRole(user, nrole)) {
            APILocator.getRoleAPI().addRoleToUser(nrole, user);
        }

        APILocator.getFolderAPI().createFolders("/f11/", host, sysuser, false);
        Folder f = APILocator.getFolderAPI().findFolderByPath("/f11/", host, sysuser, false);

        Permission p1 = new Permission();
        p1.setPermission(PermissionAPI.PERMISSION_READ);
        p1.setRoleId(nrole.getId());
        p1.setInode(f.getInode());
        permissionAPI.save(p1, f, sysuser, false);

        List<User> users = permissionAPI.getUsers(f.getInode(), PermissionAPI.PERMISSION_READ, "deletedUser", -1, -1);

        assertNotNull(users);
        assertTrue(users.size() == 0);

        APILocator.getFolderAPI().delete(f, sysuser, false);
    }

    /**
     * Tests that assigning permissions to a role on a folder does not also assign permissions on
     * the same folder to sibling roles who have individual permissions on the parent of the
     * mentioned folder.
     *
     * Reported on https://github.com/dotCMS/core/issues/16922
     */

    @Test
    public void testPermissionIndividuallyByRole() throws DotDataException, DotSecurityException {

        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final long time = System.currentTimeMillis();
        final User systemUser = APILocator.systemUser();
        Role parentRole = null;
        Role childRole = null;
        Role childSiblingRole = null;
        Host testSite = null;

        try {
            testSite = new SiteDataGen().nextPersisted();

            parentRole = new RoleDataGen().name("parent"+time).nextPersisted();

            // let's give view permissions to parent role on testSite
            assignPermissions(systemUser, parentRole, testSite);

            // create child role
            childRole = new RoleDataGen().name("child"+time).parent(parentRole.getId())
                    .nextPersisted();

            // give child role permissions on testSite
            assignPermissions(systemUser, childRole, testSite);

            // create test folder under test site
            final Folder testFolder = new FolderDataGen().site(testSite).nextPersisted();

            // create child-sibling role
            childSiblingRole = new RoleDataGen().name("childSibling"+time).parent(parentRole.getId())
                    .nextPersisted();

            // give child-sibling role permissions to testFolder
            assignPermissions(systemUser, childSiblingRole, testFolder);

            // assert parent has view permissions on testFolder
            List<Permission> parentRolePermissions = permissionAPI
                    .getPermissionsByRole(parentRole, false);

            final boolean doesParentHavePermissionOnTestFolder = parentRolePermissions.stream()
                    .anyMatch(permission -> permission.getInode().equals(
                            testFolder.getInode()));

            assertTrue(doesParentHavePermissionOnTestFolder);

            // assert that child does not have permissions on testFolder
            List<Permission> childRolePermissions = permissionAPI
                    .getPermissionsByRole(childRole, false);

            final boolean doesChildHavePermissionOnTestFolder = childRolePermissions.stream()
                    .noneMatch(permission -> permission.getInode().equals(
                            testFolder.getInode()));

            assertTrue(doesChildHavePermissionOnTestFolder);


        } finally {
            try {
                roleAPI.delete(childRole);
                roleAPI.delete(childSiblingRole);
                roleAPI.delete(parentRole);
                APILocator.getHostAPI().archive(testSite, systemUser, false);
                APILocator.getHostAPI().delete(testSite, systemUser, false);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testAnonymousUserCannotReadNonLiveContent() throws DotDataException {
        Contentlet content = TestDataUtils.getDocumentLikeContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(), null);

        try {

            //Anonymous user should not read non-live content
            assertFalse(permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_READ,
                    APILocator.getUserAPI().getAnonymousUser(), true));

            //Once the content is published, anonymous user can read it
            content = ContentletDataGen.publish(content);
            assertTrue(permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_READ,
                    APILocator.getUserAPI().getAnonymousUser(), true));
        } finally {
            ContentletDataGen.destroy(content);
        }

    }

    private void assignPermissions(User systemUser, Role parentRole, Permissionable permissionable)
            throws DotDataException, DotSecurityException {
        List<Permission> sitePermissionsForParentRole = CollectionsUtils.list(
                new Permission(permissionable.getPermissionId(), parentRole.getId(),
                        3, true));

        if (APILocator.getPermissionAPI().isInheritingPermissions(permissionable)) {
            Permissionable parentPermissionable = permissionAPI.
                    findParentPermissionable(permissionable);
            permissionAPI.permissionIndividuallyByRole(parentPermissionable,
                    permissionable, systemUser, parentRole);
        }

        permissionAPI.assignPermissions(sitePermissionsForParentRole, permissionable, systemUser,
                false);
    }

    /**
     * Generate a new role with the given name
     */
    private Role getRole(String roleName) throws DotDataException {
        Role nrole = APILocator.getRoleAPI().loadRoleByKey(roleName);
        if (!UtilMethods.isSet(nrole) || !UtilMethods.isSet(nrole.getId())) {
            nrole = new Role();
            nrole.setName(roleName);
            nrole.setRoleKey(roleName);
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription(roleName);
            nrole = APILocator.getRoleAPI().save(nrole);
        }
        return nrole;
    }

    /**
     * Given scenario: We create a hierarchy site/folder/page. Then we add page permission to the folder and verify the hierarchy picks the new changes
     * Expected Results:  The hierarchy should always be consistent in terms of permissions
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void Test_Inherited_Permissions() throws DotSecurityException, DotDataException {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final User systemUser = APILocator.systemUser();
        final Host site = new SiteDataGen().nextPersisted();

        assertNotNull(permissionAPI.getInheritablePermissions(site.getParentPermissionable()));

        assertTrue(permissionAPI.isInheritingPermissions(site));
        final PermissionCache permissionCache = CacheLocator.getPermissionCache();

        final Folder folder = new FolderDataGen()
                .name("about")
                .title("about")
                .site(site)
                .showOnMenu(true)
                .nextPersisted();

        assertTrue(permissionAPI.isInheritingPermissions(folder));

        //After calling get permission we should expect the permissions to be in cache.
        permissionAPI.getPermissions(folder, true);

        assertNotNull(permissionCache.getPermissionsFromCache(folder.getPermissionId()));

        final Contentlet page = new HTMLPageDataGen(folder, template)
                .friendlyName("index")
                .pageURL("index")
                .title("index")
                .nextPersisted();

        assertTrue(permissionAPI.isInheritingPermissions(page));
        final List<Permission> pagePermissionsInheritedFromSite = permissionAPI.getPermissions(page, true);
        final List<Permission> inheritedPermissionsFromCache = permissionCache
                .getPermissionsFromCache(page.getPermissionId());

        assertEquals(pagePermissionsInheritedFromSite, inheritedPermissionsFromCache);

        HTMLPageDataGen.publish(page);

        final Role role = new RoleDataGen()
                .name(String.format("role-%d", System.currentTimeMillis())).nextPersisted();
        //Assign permissions to the folder to simulate inherited permission on the page
        permissionAPI.save(
                new Permission(PermissionableType.HTMLPAGES.getCanonicalName(), folder.getPermissionId(),
                        role.getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT),
                folder, systemUser, false);

        assertTrue(permissionAPI.isInheritingPermissions(page));


        final List<Permission> pagePermissionsInheritedFromFolder = permissionAPI.getPermissions(page, true);
        //Here we verify the permissions are not the same since the page now inherits'em from the folder.
        assertNotEquals(pagePermissionsInheritedFromSite, pagePermissionsInheritedFromFolder);

        HTMLPageDataGen.unpublish(page);

        final List<Permission> pagePermissionsAfterUnpublish = permissionAPI.getPermissions(page, true);

        assertEquals(pagePermissionsAfterUnpublish, pagePermissionsInheritedFromFolder);

        permissionAPI.resetPermissionReferences(folder);

         //Resilience test
        final List<Permission> pagePermissionsAfterClearReference = permissionAPI.getPermissions(page, true);
        assertEquals(pagePermissionsAfterClearReference, pagePermissionsInheritedFromFolder);

        permissionAPI.removePermissions(folder);

        final List<Permission> pagePermissionsRestoredInheritance = permissionAPI.getPermissions(page, true);
        assertEquals(pagePermissionsRestoredInheritance, pagePermissionsInheritedFromSite);

    }

    /**
     * Given scenario: We remove permissions from the db to simulate a scenario
     * Expected Results:  Calling getPermissionsFromCache should give me null instead of an empty list
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void Test_Remove_All_Permissions_Then_Verify_Cache_Is_Null() throws DotSecurityException, DotDataException {
        final PermissionCache permissionCache = CacheLocator.getPermissionCache();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final User systemUser = APILocator.systemUser();
        final Host site = new SiteDataGen().nextPersisted();

        assertNotNull(permissionAPI.getInheritablePermissions(site.getParentPermissionable()));

        assertTrue(permissionAPI.isInheritingPermissions(site));

        final Folder folder = new FolderDataGen()
                .name("about")
                .title("about")
                .site(site)
                .showOnMenu(true)
                .nextPersisted();

        assertTrue(permissionAPI.isInheritingPermissions(folder));

        final Role role = new RoleDataGen()
                .name(String.format("role-%d", System.currentTimeMillis())).nextPersisted();
        //Assign permissions to the folder to simulate inherited permission on the page
        permissionAPI.save(
                new Permission(PermissionableType.HTMLPAGES.getCanonicalName(), folder.getPermissionId(),
                        role.getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT),
                folder, systemUser, false);

        //After calling get permission we should expect the permissions to be in cache.
        permissionAPI.getPermissions(folder, true);

        final DotConnect dotConnect = new DotConnect();

        dotConnect.setSQL("delete from permission where inode_id = ? ");
        dotConnect.addParam(folder.getPermissionId());
        dotConnect.loadResult();

        permissionAPI.resetPermissionReferences(folder);

        //Ww're testing here we're getting null here. instead of an empty list
        assertNull(permissionCache.getPermissionsFromCache(folder.getPermissionId()));

    }

}
