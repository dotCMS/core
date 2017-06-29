package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
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
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * This class tests the creation, copy, update, verification and setting of
 * permissions in dotCMS.
 * 
 * @author Jorge Urdaneta
 * @since May 14, 2012
 *
 */
public class PermissionAPIIntegrationTest extends IntegrationTestBase {

    private static PermissionAPI perm;
    private static RoleAPI roleApi;
    private static ContentTypeAPI contentTypeApi;
    private static Host host;
    private static User sysuser;
    private static Template tt;

    @BeforeClass
    public static void createTestHost() throws Exception {

        IntegrationTestInitService.getInstance().init();
        perm=APILocator.getPermissionAPI();
        roleApi = APILocator.getRoleAPI();
        sysuser=APILocator.getUserAPI().getSystemUser();
		contentTypeApi = APILocator.getContentTypeAPI(sysuser);
        host = new Host();
        host.setHostname("testhost.demo.dotcms.com");
        try{
            HibernateUtil.startTransaction();
            host=APILocator.getHostAPI().save(host, sysuser, false);
            HibernateUtil.commitTransaction();
        }catch(Exception e){
            HibernateUtil.rollbackTransaction();
            Logger.error(PermissionAPIIntegrationTest.class, e.getMessage());
        }
 

        perm.permissionIndividually(host.getParentPermissionable(), host, sysuser);

        tt=new Template();
        tt.setTitle("testtemplate");
        tt.setBody("<html><head></head><body>en empty template just for test</body></html>");
        APILocator.getTemplateAPI().saveTemplate(tt, host, sysuser, false);
        Map<String, Object> sessionAttrs = new HashMap<String, Object>();
        sessionAttrs.put("USER_ID", "dotcms.org.1");
    }

    @AfterClass
    public static void deleteTestHost() throws DotContentletStateException, DotDataException, DotSecurityException {
        try{
            HibernateUtil.startTransaction();
            APILocator.getHostAPI().archive(host, sysuser, false);
            APILocator.getHostAPI().delete(host, sysuser, false);
            HibernateUtil.commitTransaction();
        }catch(Exception e){
            HibernateUtil.rollbackTransaction();
            Logger.error(PermissionAPIIntegrationTest.class, e.getMessage());
        }       
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
        cont1=APILocator.getContentletAPI().checkin(cont1, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(cont1.getInode());

        Contentlet cont2=new Contentlet();
        cont2.setStructureInode(s.getInode());
        cont2.setStringProperty("testtext", "another test value");
        cont2.setHost(host.getIdentifier());
        cont2.setFolder(f4.getInode());
        cont2=APILocator.getContentletAPI().checkin(cont2, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(cont2.getInode());

        perm.permissionIndividually(host, cont1, sysuser);
        perm.permissionIndividually(host, cont2, sysuser);
        perm.permissionIndividually(host, f4, sysuser);
        perm.permissionIndividually(host, f3, sysuser);
        perm.permissionIndividually(host, f2, sysuser);
        perm.permissionIndividually(host, f1, sysuser);


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
        try{
            HibernateUtil.startTransaction();
            APILocator.getContentletAPI().archive(cont1, sysuser, false);
            APILocator.getContentletAPI().archive(cont2, sysuser, false);
            APILocator.getContentletAPI().delete(cont1, sysuser, false);
            APILocator.getContentletAPI().delete(cont2, sysuser, false);

            FieldFactory.deleteField(field1);
            FieldFactory.deleteField(field2);
            StructureFactory.deleteStructure(s.getInode());
            HibernateUtil.commitTransaction();
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
    public void issue11850() throws DotHibernateException, DotSecurityException, DotDataException {

    	// Create test host
    	Host host = new Host();
    	host.setHostname("issue11850.demo.dotcms.com");
    	host=APILocator.getHostAPI().save(host, sysuser, false);
    	try {
    		long time = System.currentTimeMillis();

    		// Create test content-type under already-created test host
    		String name = "ContentTypePermissionsInheritanceTest" + time;
    		String description = "description" + time;
    		String variable = "velocityVarNameTesting" + time;

    		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.ordinal()))
    				.description(description).host(host.getIdentifier())
    				.name(name).owner("owner").variable(variable).build();

    		type = contentTypeApi.save(type, null, null);

    		try {
    			// Check no permissions exists over test content-type
    			List<Permission> permissions = perm.getPermissions(type);
    			assertTrue(permissions.isEmpty());

    			// Assign 5 different permissions over test host (to be inherited to test content-type)
    			Role role = roleApi.loadCMSAnonymousRole();
    			int permission = PermissionAPI.PERMISSION_READ |
    					PermissionAPI.PERMISSION_WRITE |
    					PermissionAPI.PERMISSION_PUBLISH |
    					PermissionAPI.PERMISSION_EDIT_PERMISSIONS |
    					PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;

    			Permission inheritedPermission = new Permission(
    				Structure.class.getCanonicalName(), host.getPermissionId(), role.getId(), permission, true
    			);
    			perm.save(inheritedPermission, host, sysuser, true);

    			// Check the permissions for content-type are now inherited from test host
    			permissions = perm.getPermissions(type);
    			assertFalse(permissions.isEmpty());
    			assertEquals(5, permissions.size());

    		} finally {
    			// Remove test content-type
    			contentTypeApi.delete(type);
    		}
    	} finally {
    		// Remove test host
    		try{
    			HibernateUtil.startTransaction();
    			APILocator.getHostAPI().archive(host, sysuser, false);
    			APILocator.getHostAPI().delete(host, sysuser, false);
    			HibernateUtil.commitTransaction();
    		}catch(Exception e){
    			HibernateUtil.rollbackTransaction();
    			Logger.error(PermissionAPIIntegrationTest.class, e.getMessage());
    		}
    	}
    }
}