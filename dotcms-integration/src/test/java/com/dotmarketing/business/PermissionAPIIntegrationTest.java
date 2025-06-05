package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This class tests the creation, copy, update, verification and setting of
 * permissions in dotCMS.
 * 
 * @author Jorge Urdaneta
 * @since May 14, 2012
 *
 */
public class PermissionAPIIntegrationTest extends IntegrationTestBase {

    private static PermissionAPI permissionAPI;
    private static RoleAPI roleAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static HostAPI hostAPI;
    private static FolderAPI folderAPI;
    private static UserAPI userAPI;

    private static Host site;
    private static User systemUser;

    @BeforeClass
    public static void createTestHost() throws Exception {

        IntegrationTestInitService.getInstance().init();
        permissionAPI =APILocator.getPermissionAPI();
        roleAPI = APILocator.getRoleAPI();
        systemUser=APILocator.getUserAPI().getSystemUser();
		contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
		hostAPI = APILocator.getHostAPI();
		folderAPI = APILocator.getFolderAPI();
		userAPI = APILocator.getUserAPI();

        site = new Host();
        site.setHostname(System.currentTimeMillis() + "testhost.demo.dotcms.com");
        site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        try{
            HibernateUtil.startTransaction();
            site = hostAPI.save(site, systemUser, false);
            HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
            HibernateUtil.rollbackTransaction();
            Logger.error(PermissionAPIIntegrationTest.class, e.getMessage());
            fail("Test Site could not be created upon test initialization: " + e.getMessage());
        }
        permissionAPI.permissionIndividually(site.getParentPermissionable(), site, systemUser);

        final Template template = new Template();
        template.setTitle("testtemplate");
        template.setBody("<html><head></head><body>en empty template just for test</body></html>");
        APILocator.getTemplateAPI().saveTemplate(template, site, systemUser, false);
        Config.setProperty("cache.permissionshortlived.size", 0);
        CacheLocator.getPermissionCache().flushShortTermCache();
    }

    @AfterClass
    public static void deleteTestHost() throws DotDataException {
        try{
            HibernateUtil.startTransaction();
            APILocator.getHostAPI().archive(site, userAPI.getSystemUser(), false);
            APILocator.getHostAPI().delete(site, userAPI.getSystemUser(), false);
            HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
            HibernateUtil.rollbackTransaction();
            Logger.error(PermissionAPIIntegrationTest.class, e.getMessage());
        }
        Config.setProperty("cache.permissionshortlived.size", 0);
        CacheLocator.getPermissionCache().flushShortTermCache();
    }
   
    @Test
    public void resetPermissionsUnder() throws DotStateException, DotDataException, DotSecurityException {
        folderAPI.createFolders("/f5/f1/f1/f1/", site, systemUser, false);
        final Folder f1 = folderAPI.findFolderByPath("/f5/", site, systemUser, false);
        final Folder f2 = folderAPI.findFolderByPath("/f5/f1", site, systemUser, false);
        final Folder f3 = folderAPI.findFolderByPath("/f5/f1/f1", site, systemUser, false);
        final Folder f4 = folderAPI.findFolderByPath("/f5/f1/f1/f1", site, systemUser, false);

        Structure s = new Structure();
        s.setHost(site.getIdentifier());
        s.setFolder(f4.getInode());
        s.setName("test_str_str_str");
        s.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        s.setOwner(systemUser.getUserId());
        s.setVelocityVarName("testtesttest"+System.currentTimeMillis());
        StructureFactory.saveStructure(s);
        CacheLocator.getContentTypeCache().add(s);

        Field field1 = new Field("testtext", Field.FieldType.TEXT, Field.DataType.TEXT, s,
                true, true, true, 3, "", "", "", true, false, true);
        field1.setVelocityVarName("testtext");
        field1.setListed(true);
        FieldFactory.saveField(field1);
        FieldsCache.addField(field1);

        Field field2 = new Field("f", Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, s,
                true, true, true, 4, "", "", "", true, false, true);
        field2.setVelocityVarName("f");
        FieldFactory.saveField(field2);
        FieldsCache.addField(field2);

        Contentlet cont1=new Contentlet();
        cont1.setStructureInode(s.getInode());
        cont1.setStringProperty("testtext", "a test value");
        cont1.setHost(site.getIdentifier());
        cont1.setFolder(f4.getInode());
        cont1.setIndexPolicy(IndexPolicy.FORCE);
        cont1=APILocator.getContentletAPI().checkin(cont1, systemUser, false);

        Contentlet cont2=new Contentlet();
        cont2.setStructureInode(s.getInode());
        cont2.setStringProperty("testtext", "another test value");
        cont2.setHost(site.getIdentifier());
        cont2.setFolder(f4.getInode());
        cont2.setIndexPolicy(IndexPolicy.FORCE);
        cont2=APILocator.getContentletAPI().checkin(cont2, systemUser, false);

        permissionAPI.permissionIndividually(site, cont1, systemUser);
        permissionAPI.permissionIndividually(site, cont2, systemUser);
        permissionAPI.permissionIndividually(site, f4, systemUser);
        permissionAPI.permissionIndividually(site, f3, systemUser);
        permissionAPI.permissionIndividually(site, f2, systemUser);
        permissionAPI.permissionIndividually(site, f1, systemUser);


        assertFalse(permissionAPI.isInheritingPermissions(f1));
        assertFalse(permissionAPI.isInheritingPermissions(f2));
        assertFalse(permissionAPI.isInheritingPermissions(f3));
        assertFalse(permissionAPI.isInheritingPermissions(f4));
        assertFalse(permissionAPI.isInheritingPermissions(cont1));
        assertFalse(permissionAPI.isInheritingPermissions(cont2));

        permissionAPI.resetPermissionsUnder(f1);

        assertTrue(permissionAPI.isInheritingPermissions(f2));
        assertTrue(permissionAPI.isInheritingPermissions(f3));
        assertTrue(permissionAPI.isInheritingPermissions(f4));
        assertTrue(permissionAPI.isInheritingPermissions(cont1));
        assertTrue(permissionAPI.isInheritingPermissions(cont2));
        try{
            HibernateUtil.startTransaction();
            APILocator.getContentletAPI().archive(cont1, systemUser, false);
            APILocator.getContentletAPI().archive(cont2, systemUser, false);
            APILocator.getContentletAPI().delete(cont1, systemUser, false);
            APILocator.getContentletAPI().delete(cont2, systemUser, false);

            FieldFactory.deleteField(field1);
            FieldFactory.deleteField(field2);
            StructureFactory.deleteStructure(s.getInode());
            HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
            HibernateUtil.rollbackTransaction();
            Logger.error(PermissionAPIIntegrationTest.class, e.getMessage());
        }
    }


    /**
     * https://github.com/dotCMS/core/issues/11850
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotHibernateException
     */
    @Test
    public void issue11850() throws DotSecurityException, DotDataException {

    	// Create test host
    	final Host host = new SiteDataGen().nextPersisted();

    	try {
    		long time = System.currentTimeMillis();

    		// Create test content-type under already-created test host
    		String name = "ContentTypePermissionsInheritanceTest" + time;
    		String description = "description" + time;
    		String variable = "velocityVarNameTesting" + time;

    		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.ordinal()))
    				.description(description).host(host.getIdentifier())
    				.name(name).owner("owner").variable(variable).build();

    		type = contentTypeAPI.save(type, null, null);

    		try {
    			//Should be inherited from system-host if no individual permissions had been added.
    			List<Permission> permissions = permissionAPI.getPermissions(type);
    			assertFalse(permissions.isEmpty());

    			// Assign 5 different permissions over test host (to be inherited to test content-type)
    			final Role role = roleAPI.loadCMSAnonymousRole();
    			int permission = PermissionAPI.PERMISSION_READ |
    					PermissionAPI.PERMISSION_WRITE |
    					PermissionAPI.PERMISSION_PUBLISH |
    					PermissionAPI.PERMISSION_EDIT_PERMISSIONS |
    					PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;

    			Permission inheritedPermission = new Permission(
    				Structure.class.getCanonicalName(), host.getPermissionId(), role.getId(), permission, true
    			);
    			permissionAPI.save(inheritedPermission, host, systemUser, true);

    			permissionAPI.resetPermissionsUnder(host);
    			
    			// Check the permissions for content-type are now inherited from test host
    			permissions = permissionAPI.getPermissions(type);
    			assertFalse(permissions.isEmpty());
    			assertEquals(5, permissions.size());

    		} finally {
    			// Remove test content-type
    			contentTypeAPI.delete(type);
    		}
    	} finally {
    		// Remove test host
            List<Host> hostsToDelete = Lists.newArrayList(host);
            cleanHosts(hostsToDelete);
        }
    }

    /**
     * https://github.com/dotCMS/core/issues/11962
     */
    @Test
    public void test_permissionIndividuallyByRole() {
        Host testSite = new Host();

        Folder goQuestFolder = new Folder();
        Folder applicationFolder = new Folder();

        Role parentRole = new Role();
        Role childRole = new Role();
        Role grandChildRole = new Role();

        User newUser = new User();

        try {
            // Create test Host.
            testSite.setHostname( System.currentTimeMillis() + "-permission.dotcms.com");
            testSite.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            testSite = hostAPI.save(testSite, systemUser, false);

            // Create the Folders needed.
            goQuestFolder = folderAPI.createFolders("/go-quest/", testSite, systemUser, false);
            applicationFolder = folderAPI.createFolders("/application/", testSite, systemUser, false);

            // Create the Roles.
            // Parent: Webmaster
            // Child: Product Publisher
            // Grandchild: Product Contributor
            // Create Parent Role.
            parentRole.setName(System.currentTimeMillis() + "-Webmaster-Test");
            parentRole.setEditUsers(true);
            parentRole.setEditPermissions(true);
            parentRole.setEditLayouts(true);
            parentRole = roleAPI.save(parentRole);

            // Create Child Role Role.
            childRole.setName(System.currentTimeMillis() + "-Product Contributor-Test");
            childRole.setEditUsers(true);
            childRole.setEditPermissions(true);
            childRole.setEditLayouts(true);
            childRole.setParent(parentRole.getId());
            childRole = roleAPI.save(childRole);

            // Create Grandchild Role Role.
            grandChildRole.setName(System.currentTimeMillis() + "-Product Contributor-Test");
            grandChildRole.setEditUsers(true);
            grandChildRole.setEditPermissions(true);
            grandChildRole.setEditLayouts(true);
            grandChildRole.setParent(childRole.getId());
            grandChildRole = roleAPI.save(grandChildRole);

            // Now lets create a user, assign the role and test basic permissions.
            newUser = userAPI.createUser("new.user@test.com", System.currentTimeMillis() + "-new.user@test.com");
            newUser.setFirstName("Test-11962");
            newUser.setLastName("User-11962");
            userAPI.save(newUser, systemUser, false);

            roleAPI.addRoleToUser(parentRole, newUser);

            assertFalse(permissionAPI
                    .doesUserHavePermission(goQuestFolder, permissionAPI.PERMISSION_EDIT, newUser));
            assertFalse(permissionAPI
                    .doesUserHavePermission(applicationFolder, permissionAPI.PERMISSION_EDIT,
                            newUser));

            ////////////////////////////////////////////////////////////////////////////////////////
            //
            // The Following logic will emulate the logic in RoleAjax.saveRolePermission() method.
            //
            ////////////////////////////////////////////////////////////////////////////////////////

            // Set up permissions for Product Contributor
            // Permission the role to view and add children on demo.dotcm.com
            if (permissionAPI.isInheritingPermissions(testSite)) {
                final Permissionable parentPermissionable = permissionAPI.findParentPermissionable(testSite);
                permissionAPI.permissionIndividuallyByRole(parentPermissionable, testSite, systemUser,
                        grandChildRole);
            }
            List<Permission> permissionsToSave = Lists
                    .newArrayList(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                            testSite.getPermissionId(), grandChildRole.getId(), 17, true));

            permissionAPI.assignPermissions(permissionsToSave, testSite, systemUser, false);

            // Set up permissions for Product Contributor
            // Give access to view and add/edit child objects on the "Go Quest" directory
            if (permissionAPI.isInheritingPermissions(goQuestFolder)) {
                final Permissionable parentPermissionable = permissionAPI
                        .findParentPermissionable(goQuestFolder);
                permissionAPI.permissionIndividuallyByRole(parentPermissionable, goQuestFolder,
                        systemUser, grandChildRole);
            }
            permissionsToSave = Lists.newArrayList(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                    goQuestFolder.getPermissionId(), grandChildRole.getId(), 19, true));

            permissionAPI.assignPermissions(permissionsToSave, goQuestFolder, systemUser, false);

            List<Permission> grandChildPermissions = permissionAPI
                    .getPermissionsByRole(grandChildRole, true);

            final int amountGrandChilPermissionsBefore = grandChildPermissions.size();

            // Set up permissions for Product Publisher
            // Give the role full publish permissions to the applications directory.
            if (permissionAPI.isInheritingPermissions(applicationFolder)) {
                final Permissionable parentPermissionable = permissionAPI
                        .findParentPermissionable(applicationFolder);
                permissionAPI.permissionIndividuallyByRole(parentPermissionable, applicationFolder,
                        systemUser, childRole);
            }
            permissionsToSave = Lists.newArrayList(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                    applicationFolder.getPermissionId(), childRole.getId(), 19, true));

            permissionAPI
                    .assignPermissions(permissionsToSave, applicationFolder, systemUser, false);

            grandChildPermissions = permissionAPI.getPermissionsByRole(grandChildRole, true);

            final int amountGrandChilPermissionsAfter = grandChildPermissions.size();

            // Make sure we didn't alter the permissions for grandchild Role.
            assertEquals(amountGrandChilPermissionsBefore, amountGrandChilPermissionsAfter);

            // Make sure "Reverse Permission Inheritance" works.
            assertTrue(permissionAPI
                    .doesUserHavePermission(goQuestFolder, permissionAPI.PERMISSION_EDIT, newUser));
            assertTrue(permissionAPI
                    .doesUserHavePermission(applicationFolder, permissionAPI.PERMISSION_EDIT,
                            newUser));

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            final List<User> usersToDelete = Lists.newArrayList(newUser);
            cleanUsers(usersToDelete);

            //Delete Roles.
            final List<Role> rolesToDelete = Lists.newArrayList(grandChildRole, childRole, parentRole);
            cleanRoles(rolesToDelete);

            final List<Folder> foldersToDelete = Lists.newArrayList(goQuestFolder, applicationFolder);
            cleanFolders(foldersToDelete);

            // Removing Host.
            List<Host> hostsToDelete = Lists.newArrayList(testSite);
            cleanHosts(hostsToDelete);
        }
    }

    /**
     * This tests {@link PermissionBitAPIImpl#permissionIndividually(Permissionable, Permissionable, User)}
     * Given Scenario : We have a site individually permissioned with 3 perms 1 individual and two inherited then we create a child folder.
     * Expected Result: We're testing that only when an inheritable Permission has a type of the Permissionable we're passing only then that role beocomes available to the permissionable requesting individual permission
     * For example:
     *     We have a group of  inheritable perms associated with a role. And a Folder requests their individual perms.
     *     Only if there's a Role associated with the type `com.dotmarketing.portlets.folders.model.Folder` those roles become available.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parent_Individual_Permissions_Should_not_Appear_On_getPermission_Over_Children_Assets()
            throws DotDataException, DotSecurityException {
        /*  Probably everyone knows this but..
           perms have one of the following types:
            "com.dotmarketing.portlets.structure.model.Structure"
            "com.dotmarketing.portlets.contentlet.model.Contentlet"
            "com.dotmarketing.portlets.files.model.File"
            "com.dotmarketing.portlets.folders.model.Folder"
            "com.dotmarketing.beans.Host"
            "com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage"
            "com.dotmarketing.portlets.links.model.Link"
           while
            Individual Permissions are of type "Individual"
            They are meant to be used for specific actions. Stuff that isn't necessarily mapped to a class
            Like a Workflow action. We use them to indicate weather or not we have access to a WF action
        */

        final int permission = PermissionAPI.PERMISSION_READ |
                PermissionAPI.PERMISSION_WRITE |
                PermissionAPI.PERMISSION_PUBLISH |
                PermissionAPI.PERMISSION_EDIT_PERMISSIONS |
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;

        //Create a site
        final Host site = new SiteDataGen().nextPersisted();

        //Now break inheritance.
        //We need to guarantee that our folder will be getting their permissions from the root site not system_host
        permissionAPI.permissionIndividually(site.getParentPermissionable(), site, systemUser);
        assertFalse(permissionAPI.isInheritingPermissions(site));

        final long timeMark = System.currentTimeMillis();
        //Now Lets create two roles
        //One Role associated with an inheritable permission
        final Role roleForInheritable1 = new RoleDataGen().name("roleForInheritable1-perm-associated-role_"+timeMark).nextPersisted();
        final Role roleForInheritable2 = new RoleDataGen().name("roleForInheritable2-perm-associated-role_"+timeMark).nextPersisted();

        //One Role associated with an inheritable permission
        final Role roleForIndividual = new RoleDataGen().name("roleForIndividual-perm-associated-role_"+timeMark).nextPersisted();

        //Now we're gonna create two permissions on the root site.
        //One is an roleForInheritable perm (The one we expect to show on the final list after having applied roleForIndividual perms on the child folder)
        final Permission inheritableStructurePerm = new Permission(Structure.class.getCanonicalName(), site.getPermissionId(), roleForInheritable1.getId(), permission, true);
        final Permission inheritableHostPerm = new Permission(Host.class.getCanonicalName(), site.getPermissionId(), roleForInheritable2.getId(), permission, true);

        //The other is an roleForIndividual permission. Which shouldn't make it into the final results.
        //This one is set on the site but should not make it into the list of perms after having applied roleForIndividual perms on the child folder
        final Permission individualPerm = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, site.getPermissionId(), roleForIndividual.getId(), permission, true);
        //Save both perms
        permissionAPI.save(inheritableStructurePerm, site, systemUser, true);
        permissionAPI.save(inheritableHostPerm, site, systemUser, true);
        permissionAPI.save(individualPerm, site, systemUser, true);

        //Now lets add a child folder.
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        //Verify the perms we're getting from the site.

        final List<Permission> assetPermissions = permissionAPI.getPermissions(folder, true);
        assertFalse(assetPermissions.isEmpty());

        final Role anonRole = roleAPI.loadCMSAnonymousRole();
        final Permission anon = assetPermissions.get(0);
        assertEquals(anon.getRoleId(),anonRole.getId());

        assertTrue(folder.isParentPermissionable());
        assertTrue(permissionAPI.getInheritablePermissions(folder, true).isEmpty());

        final Permissionable parentPermissionable = permissionAPI.findParentPermissionable(folder);
        assertTrue(parentPermissionable instanceof Host);
        assertEquals(((Host)parentPermissionable).getIdentifier(),site.getIdentifier());

        permissionAPI.permissionIndividually(parentPermissionable, folder, systemUser);

        final List<Permission> assetIndividualPermissions1 = permissionAPI.getPermissions(folder, true);

        assertTrue(assetIndividualPermissions1.stream().anyMatch(perm -> perm.getRoleId().equals(anon.getRoleId())));
        assertTrue(assetIndividualPermissions1.stream().anyMatch(perm -> perm.getRoleId().equals(roleForIndividual.getId())));
        //This is where the actual test begins
        //Here We simply test that Role `roleForInheritable` is missing from the permissions because they are associated with type different from the permissionable we're passing (Folder).
        //it is until we create inheritable Permission on the parent associated with a type Folder that it becomes available for the inheritors
        assertFalse(permissionAPI.isInheritingPermissions(folder));

        permissionAPI.resetPermissionsUnder(folder);

        final Role roleForInheritable3 = new RoleDataGen().name("roleForInheritable3-perm-associated-role_"+timeMark).nextPersisted();
        final Permission inheritableFolderPerm = new Permission(Folder.class.getCanonicalName(), site.getPermissionId(), roleForInheritable3.getId(), permission, true);
        permissionAPI.save(inheritableFolderPerm, site, systemUser, true);
        permissionAPI.permissionIndividually(parentPermissionable, folder, systemUser);
        final List<Permission> assetIndividualPermissions2 = permissionAPI.getPermissions(folder, true);

        assertTrue(assetIndividualPermissions2.stream().anyMatch(perm->perm.getRoleId().equals(roleForInheritable3.getId())));
    }
}
