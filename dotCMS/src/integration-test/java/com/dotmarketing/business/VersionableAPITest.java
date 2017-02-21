package com.dotmarketing.business;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.repackage.com.ibm.icu.util.Calendar;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

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
	
	private HTMLPageAsset createHTMLPage() throws Exception{
		//Create HTMLPage
		String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
				
		Template template = new TemplateDataGen().nextPersisted();
		        
		Folder folder = APILocator.getFolderAPI().createFolders("/testingVersionable", host, user, false);
				
		/*HTMLPage page=new HTMLPage();
		page.setPageUrl("testpage"+ext);
		page.setFriendlyName("testpage"+ext);
		page.setTitle("testpage"+ext);
		page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, user, false);*/
		HTMLPageAsset page = new HTMLPageDataGen(folder,template).nextPersisted();
		
		return page;
	}
	
	private File createLegacyFile() throws Exception {
		// Create File
		String folderName = "/testOldFile" + UUIDGenerator.generateUuid();
		Folder folder = APILocator.getFolderAPI().createFolders(folderName, host, user, false);

		// file data in tmp folder
		java.io.File tmp = new java.io.File(
				APILocator.getFileAPI().getRealAssetPathTmpBinary() + java.io.File.separator + "testOldFile");
		if (!tmp.exists())
			tmp.mkdirs();
		java.io.File data = new java.io.File(tmp, "test-" + UUIDGenerator.generateUuid() + ".txt");

		FileWriter fw = new FileWriter(data, true);
		fw.write("file content");
		fw.close();

		// legacy file creation
		File file = new File();
		file.setFileName("legacy.txt");
		file.setFriendlyName("legacy.txt");
		file.setMimeType("text/plain");
		file.setTitle("legacy.txt");
		file.setSize((int) data.length());
		file.setModUser(user.getUserId());
		file.setModDate(Calendar.getInstance().getTime());
		file = APILocator.getFileAPI().saveFile(file, data, folder, user, false);
		return file;
	}

	@Test
	public void testFindWorkingVersionHTMLPage() throws Exception{
		HTMLPageAsset page = createHTMLPage();
        
        //Call Versionable
        Contentlet verAPI = APILocator.getContentletAPI().findContentletByIdentifier(page.getIdentifier(), false, page.getLanguageId(), user, false);
        
        //Check Same HTMLPage
        assertEquals(verAPI.getTitle(),page.getTitle());
        assertEquals(verAPI.getInode(),page.getInode());
        
        //Delete Template, Folder, HTMLPage
        //Folder folder = APILocator.getHTMLPageAPI().getParentFolder(page);
        Folder folder = APILocator.getHTMLPageAssetAPI().getParentFolder(page);
        //Template template = APILocator.getHTMLPageAPI().getTemplateForWorkingHTMLPage(page);
        Template template = APILocator.getHTMLPageAssetAPI().getTemplate(page, false);
        //APILocator.getHTMLPageAPI().delete(page, user, false);
        HTMLPageDataGen.remove(page);
        APILocator.getFolderAPI().delete(folder, user, false);
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
	
	@Test
	public void testFindWorkingVersionFile() throws Exception{
		File file = createLegacyFile();
	    
	    //Call Versionable
	    Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(file.getIdentifier(), user, false);
	    
	    //Check same File
        assertEquals(verAPI.getTitle(),file.getTitle());
        assertEquals(verAPI.getInode(),file.getInode());
        
        //Delete File
        Folder folder = APILocator.getFileAPI().getFileFolder(file, host, user, false);
        APILocator.getFileAPI().delete(file, user, false);
        APILocator.getFolderAPI().delete(folder, user, false);
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
		HTMLPageAsset page = createHTMLPage();
        
        APILocator.getVersionableAPI().setLive(page);
        
        //Call Versionable
        Contentlet verAPI = APILocator.getContentletAPI().findContentletByIdentifier(page.getIdentifier(), true, page.getLanguageId(), user, false);
        
        //Check Same HTMLPage
        assertEquals(verAPI.getTitle(),page.getTitle());
        assertEquals(verAPI.getInode(),page.getInode());
        
        //Delete Template, Folder, HTMLPage
        /*Folder folder = APILocator.getHTMLPageAPI().getParentFolder(page);
        Template template = APILocator.getHTMLPageAPI().getTemplateForWorkingHTMLPage(page);
        APILocator.getHTMLPageAPI().delete(page, user, false);*/
        Folder folder = APILocator.getHTMLPageAssetAPI().getParentFolder(page);
        Template template = APILocator.getHTMLPageAssetAPI().getTemplate(page, false);
        HTMLPageDataGen.remove(page);
        APILocator.getFolderAPI().delete(folder, user, false);
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
	
	@Test
	public void testFindLiveVersionFile() throws Exception{
		File file = createLegacyFile();
	    
	    APILocator.getFileAPI().publishFile(file, user, false);
	    
	    //Call Versionable
	    Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(file.getIdentifier(), user, false);
	    
	    //Check same File
        assertEquals(verAPI.getTitle(),file.getTitle());
        assertEquals(verAPI.getInode(),file.getInode());
        
        //Delete File
        Folder folder = APILocator.getFileAPI().getFileFolder(file, host, user, false);
        APILocator.getFileAPI().delete(file, user, false);
        APILocator.getFolderAPI().delete(folder, user, false);
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
