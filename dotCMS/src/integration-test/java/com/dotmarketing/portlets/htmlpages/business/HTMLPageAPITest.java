package com.dotmarketing.portlets.htmlpages.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.*;
import com.dotmarketing.business.*;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(DataProviderRunner.class)
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
        page.setIndexPolicy(IndexPolicy.FORCE);
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
		HTMLPageAsset page2 = new HTMLPageDataGen(folder, template).inode(existingInode).identifier(existingIdentifier).nextPersisted();
        APILocator.getContentletIndexAPI().addContentToIndex(page2, true, true);
        isIndexed = APILocator.getContentletAPI().isInodeIndexed( page2.getInode() );
        assertTrue(isIndexed);
        assertEquals(existingInode,page2.getInode());
        assertEquals(existingIdentifier,page2.getIdentifier());

        pages = APILocator.getHTMLPageAssetAPI().getWorkingHTMLPages(folder, sysuser, false);
        assertTrue(pages.size()==1);
        page2=(HTMLPageAsset) pages.get(0);
        assertEquals(existingInode,page2.getInode());
        assertEquals(existingIdentifier,page2.getIdentifier());

        // now with existing inode but this time with an update
        HibernateUtil.getSession().clear();
        String newInode=UUIDGenerator.generateUuid();
        page2.setInode(newInode);
        page2.setTitle("other title");
        Contentlet pageContentlet = APILocator.getContentletAPI().checkin(page2, sysuser, false);
        HibernateUtil.closeAndCommitTransaction();
        assertEquals(newInode,pageContentlet.getInode());
        assertEquals(existingIdentifier,pageContentlet.getIdentifier());
        assertEquals("other title",pageContentlet.getTitle());

		HTMLPageDataGen.remove(page);
        HTMLPageDataGen.remove(pageContentlet);
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

	@DataProvider
	public static Object[] testCasesFindByIdLanguageFallback() {
		return new Object[] {
			new TestCaseFindByIdLanguageFallback(1, 1, 1, null, false, new User()),
			new TestCaseFindByIdLanguageFallback(2,1, 1, null, true, new User()),
			new TestCaseFindByIdLanguageFallback(1,-1, 2, ResourceNotFoundException.class, false, new User()),
			new TestCaseFindByIdLanguageFallback(1,-1, 2, ResourceNotFoundException.class, false, new User()),
			new TestCaseFindByIdLanguageFallback(1, 1, 1, DotSecurityException.class, false, null),
		};
	}

	private static class TestCaseFindByIdLanguageFallback {
		long requestedLanguage;
		long expectedLanguage;
		long pageLanguage;
		Class<? extends Exception> expectedException;
		boolean defaultPageToDefaultLang;
		User user;

		TestCaseFindByIdLanguageFallback(final long requestedLanguage, final long expectedLanguage,
										 final long pageLanguage, final Class<? extends Exception> expectedException,
										 final boolean defaultPageToDefaultLang, final User user) {
			this.requestedLanguage = requestedLanguage;
			this.expectedLanguage = expectedLanguage;
			this.pageLanguage = pageLanguage;
			this.expectedException = expectedException;
			this.defaultPageToDefaultLang = defaultPageToDefaultLang;
			this.user = user;
		}
	}

	@Test
	@UseDataProvider("testCasesFindByIdLanguageFallback")
	public void testFindByIdLanguageFallback(final TestCaseFindByIdLanguageFallback testCase) throws DotDataException, DotSecurityException {
		final User sysuser=APILocator.getUserAPI().getSystemUser();
		final Template template = new TemplateDataGen().nextPersisted();
		final Folder folder = new FolderDataGen().nextPersisted();
		final HTMLPageDataGen pageDataGen = new HTMLPageDataGen(folder, template);
		final HTMLPageAsset pageAsset = pageDataGen.languageId(testCase.pageLanguage)
				.nextPersisted();

		final boolean originalValue = Config.getBooleanProperty( "DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", true );
		Config.setProperty( "DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", testCase.defaultPageToDefaultLang );

		final HTMLPageAssetAPI pageAssetAPI = APILocator.getHTMLPageAssetAPI();

		try {

			final User user = testCase.user!=null?sysuser:testCase.user;

			final IHTMLPage returnedPage = pageAssetAPI.findByIdLanguageFallback(pageAsset.getIdentifier(),
					testCase.requestedLanguage, false, user, false);

			assertEquals(testCase.expectedLanguage, returnedPage.getLanguageId());

		} catch(Exception e) {
			assertEquals(testCase.expectedException.getName(),e.getClass().getName());
		} finally {
			// restore original value
			Config.setProperty( "DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", originalValue );
			if(pageAsset!=null) {
				ContentletDataGen.remove(pageAsset);
			}

			if(template!=null) {
				TemplateDataGen.remove(template);
			}

			FolderDataGen.remove(folder);
		}


	}



}
