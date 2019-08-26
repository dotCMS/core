package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
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
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
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
public class PermissionAPIIntegrationTest extends IntegrationTestBase {

    private static PermissionAPI permissionAPI;
    private static RoleAPI roleAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static HostAPI hostAPI;
    private static FolderAPI folderAPI;
    private static UserAPI userAPI;

    private static Host host;
    private static User systemUser;
    private static Template template;

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

        host = new Host();
        host.setHostname("testhost.demo.dotcms.com");
        try{
            HibernateUtil.startTransaction();
            host=hostAPI.save(host, systemUser, false);
            HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
            HibernateUtil.rollbackTransaction();
            Logger.error(PermissionAPIIntegrationTest.class, e.getMessage());
        }
 

        permissionAPI.permissionIndividually(host.getParentPermissionable(), host, systemUser);

        template =new Template();
        template.setTitle("testtemplate");
        template.setBody("<html><head></head><body>en empty template just for test</body></html>");
        APILocator.getTemplateAPI().saveTemplate(template, host, systemUser, false);
        Map<String, Object> sessionAttrs = new HashMap<String, Object>();
        sessionAttrs.put("USER_ID", "dotcms.org.1");
    }

    @AfterClass
    public static void deleteTestHost() throws DotDataException {
        try{
            HibernateUtil.startTransaction();
            APILocator.getHostAPI().archive(host, userAPI.getSystemUser(), false);
            APILocator.getHostAPI().delete(host, userAPI.getSystemUser(), false);
            HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
            HibernateUtil.rollbackTransaction();
            Logger.error(PermissionAPIIntegrationTest.class, e.getMessage());
        }
    }
   
    @Test
    public void resetPermissionsUnder() throws DotStateException, DotDataException, DotSecurityException {
        folderAPI.createFolders("/f5/f1/f1/f1/", host, systemUser, false);
        final Folder f1 = folderAPI.findFolderByPath("/f5/", host, systemUser, false);
        final Folder f2 = folderAPI.findFolderByPath("/f5/f1", host, systemUser, false);
        final Folder f3 = folderAPI.findFolderByPath("/f5/f1/f1", host, systemUser, false);
        final Folder f4 = folderAPI.findFolderByPath("/f5/f1/f1/f1", host, systemUser, false);

        Structure s = new Structure();
        s.setHost(host.getIdentifier());
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
        cont1.setHost(host.getIdentifier());
        cont1.setFolder(f4.getInode());
        cont1.setIndexPolicy(IndexPolicy.FORCE);
        cont1=APILocator.getContentletAPI().checkin(cont1, systemUser, false);

        Contentlet cont2=new Contentlet();
        cont2.setStructureInode(s.getInode());
        cont2.setStringProperty("testtext", "another test value");
        cont2.setHost(host.getIdentifier());
        cont2.setFolder(f4.getInode());
        cont2.setIndexPolicy(IndexPolicy.FORCE);
        cont2=APILocator.getContentletAPI().checkin(cont2, systemUser, false);

        permissionAPI.permissionIndividually(host, cont1, systemUser);
        permissionAPI.permissionIndividually(host, cont2, systemUser);
        permissionAPI.permissionIndividually(host, f4, systemUser);
        permissionAPI.permissionIndividually(host, f3, systemUser);
        permissionAPI.permissionIndividually(host, f2, systemUser);
        permissionAPI.permissionIndividually(host, f1, systemUser);


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

        Host host = new Host();

        Folder goQuestFolder = new Folder();
        Folder applicationFolder = new Folder();

        Role parentRole = new Role();
        Role childRole = new Role();
        Role grandChildRole = new Role();

        User newUser = new User();

        try {
            // Create test Host.
            host.setHostname("permission.dotcms.com");
            host = hostAPI.save(host, systemUser, false);

            // Create the Folders needed.
            goQuestFolder = folderAPI.createFolders("/go-quest/", host, systemUser, false);
            applicationFolder = folderAPI.createFolders("/application/", host, systemUser, false);

            // Create the Roles.
            // Parent: Webmaster
            // Child: Product Publisher
            // Grandchild: Product Contributor
            // Create Parent Role.
            parentRole.setName("Webmaster-Test");
            parentRole.setEditUsers(true);
            parentRole.setEditPermissions(true);
            parentRole.setEditLayouts(true);
            parentRole = roleAPI.save(parentRole);

            // Create Child Role Role.
            childRole.setName("Product Contributor-Test");
            childRole.setEditUsers(true);
            childRole.setEditPermissions(true);
            childRole.setEditLayouts(true);
            childRole.setParent(parentRole.getId());
            childRole = roleAPI.save(childRole);

            // Create Grandchild Role Role.
            grandChildRole.setName("Product Contributor-Test");
            grandChildRole.setEditUsers(true);
            grandChildRole.setEditPermissions(true);
            grandChildRole.setEditLayouts(true);
            grandChildRole.setParent(childRole.getId());
            grandChildRole = roleAPI.save(grandChildRole);

            // Now lets create a user, assign the role and test basic permissions.
            newUser = userAPI.createUser("new.user@test.com", "new.user@test.com");
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
            if (permissionAPI.isInheritingPermissions(host)) {
                final Permissionable parentPermissionable = permissionAPI.findParentPermissionable(host);
                permissionAPI.permissionIndividuallyByRole(parentPermissionable, host, systemUser,
                        grandChildRole);
            }
            List<Permission> permissionsToSave = Lists
                    .newArrayList(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                            host.getPermissionId(), grandChildRole.getId(), 17, true));

            permissionAPI.assignPermissions(permissionsToSave, host, systemUser, false);

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
            List<Host> hostsToDelete = Lists.newArrayList(host);
            cleanHosts(hostsToDelete);
        }
    }
}
