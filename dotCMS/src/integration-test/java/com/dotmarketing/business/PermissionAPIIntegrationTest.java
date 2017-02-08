package com.dotmarketing.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
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
    private static Host host;
    private static User sysuser;
    private static Template tt;

    @BeforeClass
    public static void createTestHost() throws Exception {

        IntegrationTestInitService.getInstance().init();
        perm=APILocator.getPermissionAPI();
        sysuser=APILocator.getUserAPI().getSystemUser();
        host = new Host();
        host.setHostname("testhost.demo.dotcms.com");
        try{
            HibernateUtil.startTransaction();
            host=APILocator.getHostAPI().save(host, sysuser, false);
            HibernateUtil.commitTransaction();
        }catch(Exception e){
            HibernateUtil.rollbackTransaction();
            Logger.error(PermissionAPITest.class, e.getMessage());
        }
 

        perm.permissionIndividually(host.getParentPermissionable(), host, sysuser, false);

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
            Logger.error(PermissionAPITest.class, e.getMessage());
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
            Logger.error(PermissionAPITest.class, e.getMessage());
        }
    }
}