package com.dotmarketing.business;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Erick Gonzalez
 */
public class VersionableAPITest {
	
	private static User user;
	private static Host host;
	
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        
        user = APILocator.getUserAPI().getSystemUser();
        host = APILocator.getHostAPI().findDefaultHost(user, false);
	}
	
	private HTMLPage createHTMLPage() throws Exception{
		//Create HTMLPage
		String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
				
		Template template = new TemplateDataGen().nextPersisted();
		        
		Folder folder = APILocator.getFolderAPI().createFolders("/testingVersionable", host, user, false);
				
		HTMLPage page=new HTMLPage();
		page.setPageUrl("testpage"+ext);
		page.setFriendlyName("testpage"+ext);
		page.setTitle("testpage"+ext);
		page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, user, false);
		
		return page;
	}

	@Test
	public void testFindWorkingVersionHTMLPage() throws Exception{
		HTMLPage page = createHTMLPage();
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(page.getIdentifier(), user, false);
        
        //Check Same HTMLPage
        assertEquals(verAPI.getTitle(),page.getTitle());
        assertEquals(verAPI.getInode(),page.getInode());
        
        //Delete Template, Folder, HTMLPage
        Folder folder = APILocator.getHTMLPageAPI().getParentFolder(page);
        Template template = APILocator.getHTMLPageAPI().getTemplateForWorkingHTMLPage(page);
        APILocator.getHTMLPageAPI().delete(page, user, false);
        APILocator.getFolderAPI().delete(folder, user, false);
        APILocator.getTemplateAPI().delete(template, user, false);
	}
	
	@Test
	public void testFindWorkingVersionContainer() throws Exception{
		//Create Container
		Structure structure = new StructureDataGen().nextPersisted();
		Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(container.getIdentifier(), user, false);
        
        //Check Same Container
        assertEquals(verAPI.getTitle(),container.getTitle());
        assertEquals(verAPI.getInode(),container.getInode());
        
        //Delete Container
        APILocator.getContainerAPI().delete(container, user, false);
        APILocator.getStructureAPI().delete(structure, user);
	}
	
	@Test
	public void testFindWorkingVersionTemplate() throws Exception{
		//Create Template
		Template template = new TemplateDataGen().nextPersisted();
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(template.getIdentifier(), user, false);
        
        //Check same Template
        assertEquals(verAPI.getTitle(),template.getTitle());
        assertEquals(verAPI.getInode(),template.getInode());
        
        //Delete Template
        APILocator.getTemplateAPI().delete(template, user, false);
	}
	
	@Test(expected = DotDataException.class)
	public void testFindWorkingVersionContentlet() throws Exception{
		//Create Contentlet
		Structure structure = new StructureDataGen().nextPersisted();
		Contentlet contentlet = new ContentletDataGen(structure.getInode()).nextPersisted();
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(contentlet.getIdentifier(), user, false);
	}
	
	@Test
	public void testFindLiveVersionHTMLPage() throws Exception{
		HTMLPage page = createHTMLPage();
        
        APILocator.getVersionableAPI().setLive(page);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(page.getIdentifier(), user, false);
        
        //Check Same HTMLPage
        assertEquals(verAPI.getTitle(),page.getTitle());
        assertEquals(verAPI.getInode(),page.getInode());
        
        //Delete Template, Folder, HTMLPage
        Folder folder = APILocator.getHTMLPageAPI().getParentFolder(page);
        Template template = APILocator.getHTMLPageAPI().getTemplateForWorkingHTMLPage(page);
        APILocator.getHTMLPageAPI().delete(page, user, false);
        APILocator.getFolderAPI().delete(folder, user, false);
        APILocator.getTemplateAPI().delete(template, user, false);
	}
	
	@Test
	public void testFindLiveVersionContainer() throws Exception{
		//Create Container
		Structure structure = new StructureDataGen().nextPersisted();
		Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
        
        APILocator.getVersionableAPI().setLive(container);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(container.getIdentifier(), user, false);
        
        //Check Same Container
        assertEquals(verAPI.getTitle(),container.getTitle());
        assertEquals(verAPI.getInode(),container.getInode());
        
        //Delete Container
        APILocator.getContainerAPI().delete(container, user, false);
        APILocator.getStructureAPI().delete(structure, user);
	}
	
	@Test
	public void testFindLiveVersionTemplate() throws Exception{
		//Create Template
		Template template = new TemplateDataGen().nextPersisted();
        APILocator.getVersionableAPI().setLive(template);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(template.getIdentifier(), user, false);
        
        //Check same Template
        assertEquals(verAPI.getTitle(),template.getTitle());
        assertEquals(verAPI.getInode(),template.getInode());
        
        //Delete Template
        APILocator.getTemplateAPI().delete(template, user, false);
	}
	
	@Test(expected = DotDataException.class)
	public void testFindLiveVersionContentlet() throws Exception{
		//Create Contentlet
		Structure structure = new StructureDataGen().nextPersisted();
		Contentlet contentlet = new ContentletDataGen(structure.getInode()).nextPersisted();
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(contentlet.getIdentifier(), user, false);
	}

}
