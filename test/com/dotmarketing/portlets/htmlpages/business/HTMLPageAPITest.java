package com.dotmarketing.portlets.htmlpages.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.dotcms.TestBase;
import com.dotcms.publisher.business.PublisherAPIImpl;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class HTMLPageAPITest extends TestBase {
    @Test
    public void saveHTMLPage() throws Exception {
        User sysuser=APILocator.getUserAPI().getSystemUser();
        Host host=APILocator.getHostAPI().findDefaultHost(sysuser, false);
        String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");

        Template template=new Template();
        template.setTitle("a template "+UUIDGenerator.generateUuid());
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, host, sysuser, false);

        Folder folder=APILocator.getFolderAPI().createFolders(
                "/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);

        HTMLPage page=new HTMLPage();
        page.setPageUrl("testpage"+ext);
        page.setFriendlyName("testpage"+ext);
        page.setTitle("testpage"+ext);
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, sysuser, false);
        assertTrue(page!=null);
        assertTrue(UtilMethods.isSet(page.getInode()));
        assertTrue(UtilMethods.isSet(page.getIdentifier()));

        List<HTMLPage> pages = APILocator.getHTMLPageAPI().findWorkingHTMLPages(folder);
        assertTrue(pages.size()==1);

        // now with existing inode/identifier
        String existingInode=UUIDGenerator.generateUuid();
        String existingIdentifier=UUIDGenerator.generateUuid();

        folder=APILocator.getFolderAPI().createFolders(
                "/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);
        page=new HTMLPage();
        page.setPageUrl("testpage"+ext);
        page.setFriendlyName("testpage"+ext);
        page.setTitle("testpage"+ext);
        page.setInode(existingInode);
        page.setIdentifier(existingIdentifier);
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, sysuser, false);
        assertEquals(existingInode,page.getInode());
        assertEquals(existingIdentifier,page.getIdentifier());

        pages = APILocator.getHTMLPageAPI().findWorkingHTMLPages(folder);
        assertTrue(pages.size()==1);
        page=pages.get(0);
        assertEquals(existingInode,page.getInode());
        assertEquals(existingIdentifier,page.getIdentifier());

        // now with existing inode but this time with an update
        HibernateUtil.getSession().clear();
        String newInode=UUIDGenerator.generateUuid();
        page.setInode(newInode);
        page.setTitle("other title");
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, sysuser, false);
        assertEquals(newInode,page.getInode());
        assertEquals(existingIdentifier,page.getIdentifier());
        assertEquals("other title",page.getTitle());
    }

    @Test
    public void delete() throws Exception {
        User sysuser=APILocator.getUserAPI().getSystemUser();
        Host host=APILocator.getHostAPI().findDefaultHost(sysuser, false);
        String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");

        // a container to use inside the template
        Container container = new Container();
        container.setFriendlyName("test container");
        container.setTitle("his is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");
        Structure st=StructureCache.getStructureByVelocityVarName("FileAsset");

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(st.getInode());
        cs.setCode("this is the code");
        csList.add(cs);

        container = APILocator.getContainerAPI().save(container, csList, host, sysuser, false);

        // a template for the page
        Template template=new Template();
        template.setTitle("a template "+UUIDGenerator.generateUuid());
        template.setBody("<html><body> #parseContainer('"+container.getIdentifier()+"') </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, host, sysuser, false);

        // folder where the page lives
        Folder folder=APILocator.getFolderAPI().createFolders(
                "/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);

        // the page
        HTMLPage page=new HTMLPage();
        page.setPageUrl("testpage"+ext);
        page.setFriendlyName("testpage"+ext);
        page.setTitle("testpage"+ext);
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, sysuser, false);

        // associate some contentlets with the page/container
        List<Contentlet> conns=APILocator.getContentletAPI().search(
                "+structureName:"+st.getVelocityVarName(), 5, 0, "moddate", sysuser, false);
        assertEquals(5, conns.size());

        for(Contentlet cc : conns) {
            MultiTreeFactory.saveMultiTree(
              new MultiTree(page.getIdentifier(),container.getIdentifier(),cc.getIdentifier()));
        }

        final String pageInode=page.getInode(),pageIdent=page.getIdentifier(),
                     templateInode=template.getInode(), templateIdent=template.getIdentifier(),
                     containerInode=container.getInode(), containerIdent=container.getIdentifier();

        // let's delete

        APILocator.getHTMLPageAPI().delete(page, sysuser, false);
        APILocator.getTemplateAPI().delete(template, sysuser, false);
        APILocator.getContainerAPI().delete(container, sysuser, false);

        // check everything is clean up

        AssetUtil.assertDeleted(pageInode, pageIdent, "htmlpage");
        AssetUtil.assertDeleted(templateInode, templateIdent, "template");
        AssetUtil.assertDeleted(containerInode, containerIdent, "containers");
    }

    @Test
    public void move() throws Exception {
    	User sysuser=null;
    	HTMLPage page=null;
    	Template template=null;
    	Container container=null;
    	Role role=null;
    	Folder folderWithPerms=null;
    	Folder folder=null;

    	try {
    		sysuser=APILocator.getUserAPI().getSystemUser();
    		Host host=APILocator.getHostAPI().findDefaultHost(sysuser, false);
    		String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");

    		// a container to use inside the template
    		container = new Container();
    		container.setFriendlyName("test container");
    		container.setTitle("his is the title");
    		container.setMaxContentlets(5);
    		container.setPreLoop("preloop code");
    		container.setPostLoop("postloop code");
    		Structure st=StructureCache.getStructureByVelocityVarName("FileAsset");
    		// commented by issue-2093

    		List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
            ContainerStructure cs = new ContainerStructure();
            cs.setStructureId(st.getInode());
            cs.setCode("this is the code");
            csList.add(cs);

    		container = APILocator.getContainerAPI().save(container, csList, host, sysuser, false);

    		// a template for the page
    		template=new Template();
    		template.setTitle("a template "+UUIDGenerator.generateUuid());
    		template.setBody("<html><body> #parseContainer('"+container.getIdentifier()+"') </body></html>");
    		template=APILocator.getTemplateAPI().saveTemplate(template, host, sysuser, false);

    		// folder where the page gets moved from
    		folder=APILocator.getFolderAPI().createFolders(
    				"/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);

    		// the page
    		page=new HTMLPage();
    		page.setPageUrl("testpage"+ext);
    		page.setFriendlyName("testpage"+ext);
    		page.setTitle("testpage"+ext);
    		page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, sysuser, false);

    		// folder with some perms, where the page gets moved to
    		folderWithPerms=APILocator.getFolderAPI().createFolders(
    				"/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);

    		// create new roles
    		RoleAPI roleAPI = APILocator.getRoleAPI();

    		role = new Role();
    		role.setName("testRole1");
    		role.setRoleKey("testKey1");
    		role.setEditUsers(true);
    		role.setEditPermissions(true);
    		role.setEditLayouts(true);
    		role.setDescription("testDesc1");
    		role = roleAPI.save(role);

    		List<Permission> newSetOfPermissions = new ArrayList<Permission>();
    		newSetOfPermissions.add(new Permission(HTMLPage.class.getCanonicalName(), folderWithPerms.getPermissionId(), role.getId(),
    				PermissionAPI.PERMISSION_READ, true));

    		PermissionAPI permAPI = APILocator.getPermissionAPI();
    		permAPI.assignPermissions(newSetOfPermissions, folderWithPerms, sysuser, false);

    		APILocator.getHTMLPageAPI().movePage(page, folderWithPerms, sysuser, false);

    		List<Permission> assetPermissions = permAPI.getPermissions(page, true);

    		assertNotNull(assetPermissions);
    		assertTrue(!assetPermissions.isEmpty());

    		Permission pageReadPerm = assetPermissions.get(0);

    		assertTrue(pageReadPerm.getType().equals(HTMLPage.class.getCanonicalName()) && pageReadPerm.getPermission()==1);

    	} catch (Exception e) {
    		try {
    			HibernateUtil.rollbackTransaction();
    		} catch (DotHibernateException e1) {
    			Logger.error(HTMLPageAPITest.class,e.getMessage(),e1);
    		}
    		Logger.error(HTMLPageAPITest.class,e.getMessage(),e);
    		throw e;
    	} finally {
    		if(page!=null)	APILocator.getHTMLPageAPI().delete(page, sysuser, false);
    		if(template!=null)	APILocator.getTemplateAPI().delete(template, sysuser, false);
    		if(container!=null)	APILocator.getContainerAPI().delete(container, sysuser, false);
    		if(folderWithPerms!=null)	APILocator.getFolderAPI().delete(folderWithPerms, sysuser, false);
    		if(folder!=null)	APILocator.getFolderAPI().delete(folder, sysuser, false);
    		if(role!=null)	APILocator.getRoleAPI().delete(role);
    	}

    }

}
