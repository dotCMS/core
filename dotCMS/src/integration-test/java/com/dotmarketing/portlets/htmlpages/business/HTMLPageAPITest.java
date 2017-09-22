package com.dotmarketing.portlets.htmlpages.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.IntegrationTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

public class HTMLPageAPITest extends IntegrationTestBase {
	
    @BeforeClass
    public static void prepare () throws Exception {
    	
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }
    
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

        HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
        APILocator.getContentletIndexAPI().addContentToIndex(page, true, true);
        boolean isIndexed = APILocator.getContentletAPI().isInodeIndexed( page.getInode() );
        assertTrue(isIndexed);

        List<IHTMLPage> pages = APILocator.getHTMLPageAssetAPI().getWorkingHTMLPages(folder, sysuser, true);
        assertTrue(pages.size()==1);

        // now with existing inode/identifier
        String existingInode=UUIDGenerator.generateUuid();
        String existingIdentifier=UUIDGenerator.generateUuid();

        folder=APILocator.getFolderAPI().createFolders(
                "/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);
        page = new HTMLPageDataGen(folder, template).inode(existingInode).identifier(existingIdentifier).nextPersisted();
        APILocator.getContentletIndexAPI().addContentToIndex(page, true, true);
        isIndexed = APILocator.getContentletAPI().isInodeIndexed( page.getInode() );
        assertTrue(isIndexed);
        assertEquals(existingInode,page.getInode());
        assertEquals(existingIdentifier,page.getIdentifier());

        pages = APILocator.getHTMLPageAssetAPI().getWorkingHTMLPages(folder, sysuser, false);
        assertTrue(pages.size()==1);
        page=(HTMLPageAsset) pages.get(0);
        assertEquals(existingInode,page.getInode());
        assertEquals(existingIdentifier,page.getIdentifier());

        // now with existing inode but this time with an update
        HibernateUtil.getSession().clear();
        String newInode=UUIDGenerator.generateUuid();
        page.setInode(newInode);
        page.setTitle("other title");
        Contentlet pageContentlet = APILocator.getContentletAPI().checkin(page, sysuser, false);
        HibernateUtil.closeAndCommitTransaction();
        assertEquals(newInode,pageContentlet.getInode());
        assertEquals(existingIdentifier,pageContentlet.getIdentifier());
        assertEquals("other title",pageContentlet.getTitle());
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
        HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();

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
        	HTMLPageDataGen.remove(page);
            APILocator.getTemplateAPI().delete(template, sysuser, false);
            APILocator.getContainerAPI().delete(container, sysuser, false);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(HTMLPageAPITest.class, e.getMessage());
        }

        // check everything is clean up

        AssetUtil.assertDeleted(pageInode, pageIdent, "contentlet");
        AssetUtil.assertDeleted(templateInode, templateIdent, "template");
        AssetUtil.assertDeleted(containerInode, containerIdent, Inode.Type.CONTAINERS.getValue());
    }

    @Test
    public void move() throws Exception {
    	User sysuser=null;
    	HTMLPageAsset page=null;
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
    		page = new HTMLPageDataGen(folder, template).nextPersisted();

    		// folder with some perms, where the page gets moved to
    		folderWithPerms=APILocator.getFolderAPI().createFolders(
    				"/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);

    		// create new roles
    		RoleAPI roleAPI = APILocator.getRoleAPI();

    		long time = System.currentTimeMillis();
    		
    		role = new Role();
    		role.setName("testRole1"+time);
    		role.setRoleKey("testKey1"+time);
    		role.setEditUsers(true);
    		role.setEditPermissions(true);
    		role.setEditLayouts(true);
    		role.setDescription("testDesc1");
    		role = roleAPI.save(role);

    		List<Permission> newSetOfPermissions = new ArrayList<Permission>();
    		newSetOfPermissions.add(new Permission(IHTMLPage.class.getCanonicalName(), folderWithPerms.getPermissionId(), role.getId(),
    				PermissionAPI.PERMISSION_READ, true));

    		PermissionAPI permAPI = APILocator.getPermissionAPI();

    		// NOTE: Method "assignPermissions" is deprecated in favor of "save", which has subtle functional differences. Please take these differences into consideration if planning to replace this method with the "save"
    		permAPI.assignPermissions(newSetOfPermissions, folderWithPerms, sysuser, false);

    		APILocator.getHTMLPageAssetAPI().move(page, folderWithPerms, sysuser);

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
    		if(page!=null)	HTMLPageDataGen.remove(page);
    		if(template!=null)	APILocator.getTemplateAPI().delete(template, sysuser, false);
    		if(container!=null)	APILocator.getContainerAPI().delete(container, sysuser, false);
    		if(folderWithPerms!=null)	APILocator.getFolderAPI().delete(folderWithPerms, sysuser, false);
    		if(folder!=null)	APILocator.getFolderAPI().delete(folder, sysuser, false);
    		if(role!=null)	APILocator.getRoleAPI().delete(role);
    	}

    }

	/**
	 * Tests the deletion of a multi-lang page in only one language.
	 * <p>
	 * This should work since it is now possible to delete only one language of a multi-lang page.
	 */
	@Test
	public void removeOneLanguageOfHtmlAsset() throws Exception  {
		int english = 1;
		int spanish = 2;

			Template template = new TemplateDataGen().nextPersisted();
			Folder folder = new FolderDataGen().nextPersisted();
			HTMLPageAsset multiLangPageEnglishVersion = new HTMLPageDataGen(folder, template).languageId(english)
				.nextPersisted();
			Contentlet multiLangPageSpanishVersion = HTMLPageDataGen.checkout(multiLangPageEnglishVersion);
			multiLangPageSpanishVersion.setLanguageId(spanish);
			multiLangPageSpanishVersion = HTMLPageDataGen.checkin(multiLangPageSpanishVersion);

			// this shouldn't throw error
			HTMLPageDataGen.remove(multiLangPageSpanishVersion);
			// now let's remove the english version
			HTMLPageDataGen.remove(multiLangPageEnglishVersion);
			// dispose other objects
			FolderDataGen.remove(folder);
			TemplateDataGen.remove(template);

	}

}
