package com.dotmarketing.portlets.htmlpages.business;

import static com.dotcms.repackage.org.junit.Assert.assertEquals;
import static com.dotcms.repackage.org.junit.Assert.assertNotNull;
import static com.dotcms.repackage.org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.TestBase;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
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
        
        HibernateUtil.startTransaction();
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
        HibernateUtil.commitTransaction();
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
        Structure st=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("FileAsset");

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
        try{
        	HibernateUtil.startTransaction();
            APILocator.getHTMLPageAPI().delete(page, sysuser, false);
            APILocator.getTemplateAPI().delete(template, sysuser, false);
            APILocator.getContainerAPI().delete(container, sysuser, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(HTMLPageAPITest.class, e.getMessage());
        }

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
    		Structure st=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("FileAsset");
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
    		newSetOfPermissions.add(new Permission(IHTMLPage.class.getCanonicalName(), folderWithPerms.getPermissionId(), role.getId(),
    				PermissionAPI.PERMISSION_READ, true));

    		PermissionAPI permAPI = APILocator.getPermissionAPI();
    		permAPI.assignPermissions(newSetOfPermissions, folderWithPerms, sysuser, false);

    		APILocator.getHTMLPageAPI().movePage(page, folderWithPerms, sysuser, false);

    		List<Permission> assetPermissions = permAPI.getPermissions(page, true);

    		assertNotNull(assetPermissions);
    		assertTrue(!assetPermissions.isEmpty());

    		Permission pageReadPerm = assetPermissions.get(0);

    		assertTrue(pageReadPerm.getType().equals(IHTMLPage.class.getCanonicalName()) && pageReadPerm.getPermission()==1);

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

	/**
	 * Tries to remove a single language version of a page, which must fail
	 * because it's not possible to delete only 1 language of a page. The page
	 * MUST be completely removed.
	 */
	@Test
	public void removeOneLanguageOfHtmlAsset() throws DotDataException,
			DotSecurityException {
		User systemUser = APILocator.getUserAPI().getSystemUser();
		Host host = APILocator.getHostAPI().findDefaultHost(systemUser, false);
		ContentletAPI contentletAPI = APILocator.getContentletAPI();
		IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
		FolderAPI folderAPI = APILocator.getFolderAPI();
		try {
			HibernateUtil.startTransaction();
			// Creating testing folder
			String folderPath = "/testfolder" + UUIDGenerator.generateUuid();
			Folder folder = folderAPI.createFolders(folderPath, host,
					systemUser, true);

			// Creating test page in English
			Template template = APILocator.getTemplateAPI().findLiveTemplate(
					"9396ac6a-d32c-4539-966e-c776e7562cfb", systemUser, false);
			Contentlet englishPage = new Contentlet();
			englishPage
					.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
			englishPage.setHost(host.getIdentifier());
			englishPage.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD,
					"page english");
			englishPage
					.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, "test-page");
			englishPage.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD,
					"page english");
			englishPage.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
			englishPage.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD,
					template.getIdentifier());
			englishPage.setFolder(folder.getInode());
			englishPage = contentletAPI.checkin(englishPage, systemUser, false);

			// Creating test page in Spanish
			Contentlet spanishPage = APILocator.getContentletAPI().checkout(
					englishPage.getInode(), systemUser, false);
			spanishPage.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD,
					"page spanish");
			spanishPage.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD,
					"page spanish");
			spanishPage.setLanguageId(2);
			spanishPage = contentletAPI.checkin(spanishPage, systemUser, false);

			// Archive and delete the first page. Cannot delete pages that have
			// versions in other languages, which is expected and correct. The
			// call to the delete method MUST return false
			contentletAPI.archive(englishPage, systemUser, false);
			boolean deleteSuccessful = false;
			try {
				deleteSuccessful = contentletAPI.delete(englishPage,
						systemUser, false);
			} catch (Exception e) {
				// Ignore
			}
			assertTrue("ERROR: Another page in another language still exists.",
					!deleteSuccessful);

			// Now, archive and delete the second page. Now that both pages are
			// archived and deleted, the page can be successfully removed.
			contentletAPI.archive(spanishPage, systemUser, false);
			contentletAPI.delete(spanishPage, systemUser, false);
			Identifier id = identifierAPI.find(englishPage.getIdentifier());
			assertTrue("ERROR: The page is still present",
					!UtilMethods.isSet(id.getId()));
			folderAPI.delete(folder, systemUser, false);
			HibernateUtil.commitTransaction();
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Logger.error(HTMLPageAPITest.class, e.getMessage());
		}
	}

}
