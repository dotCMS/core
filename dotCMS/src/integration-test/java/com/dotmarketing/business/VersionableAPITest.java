package com.dotmarketing.business;

import com.dotcms.repackage.com.ibm.icu.util.Calendar;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.IntegrationTestInitService;
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

	@Test
	public void testFindWorkingVersionHTMLPage() throws Exception{
		//Create HTMLPage
		String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
		
		Template template=new Template();
        template.setTitle("TEST TEMPLATE VERSIONABLE");
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        
        Folder folder = APILocator.getFolderAPI().createFolders("/testingVersionable", host, user, false);
		
		HTMLPage page=new HTMLPage();
        page.setPageUrl("testpage"+ext);
        page.setFriendlyName("testpage"+ext);
        page.setTitle("testpage"+ext);
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, user, false);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(page.getIdentifier(), user, false);
        
        //Check Same HTMLPage
        assertEquals(verAPI.getTitle(),page.getTitle());
        assertEquals(verAPI.getInode(),page.getInode());
        
        //Delete Template, Folder, HTMLPage
        APILocator.getHTMLPageAPI().delete(page, user, false);
        APILocator.getFolderAPI().delete(folder, user, false);
        APILocator.getTemplateAPI().delete(template, user, false);
	}
	
	@Test
	public void testFindWorkingVersionContainer() throws Exception{
		//Create Container
		Container c = new Container();
        c.setFriendlyName("Versionable Container Working");
        c.setTitle("Versionable Container Working");
        c.setMaxContentlets(2);
        c.setPreLoop("preloop code");
        c.setPostLoop("postloop code");

        Structure st=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("host");
        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(st.getInode());
        cs.setCode("this is the code");
        csList.add(cs);
        
        c = APILocator.getContainerAPI().save(c, csList, host, user, false);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(c.getIdentifier(), user, false);
        
        //Check Same Container
        assertEquals(verAPI.getTitle(),c.getTitle());
        assertEquals(verAPI.getInode(),c.getInode());
        
        //Delete Container
        APILocator.getContainerAPI().delete(c, user, false);
	}
	
	@Test
	public void testFindWorkingVersionTemplate() throws Exception{
		//Create Template
		Template template=new Template();
        template.setTitle("TEST TEMPLATE VERSIONABLE");
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        
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
		//Create File
		String folderName = "/testOldFile"+UUIDGenerator.generateUuid();
	    Folder ff = APILocator.getFolderAPI().createFolders(folderName, host, user, false);
	    
	    // file data in tmp folder
	    java.io.File tmp=new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary()+java.io.File.separator+"testOldFile");
	    if(!tmp.exists()) tmp.mkdirs();
	    java.io.File data=new java.io.File(tmp,
	            "test-"+UUIDGenerator.generateUuid()+".txt");
	    
	    FileWriter fw=new FileWriter(data,true);
	    fw.write("file content"); fw.close();
	    
	    // legacy file creation
	    File file = new File();
	    file.setFileName("legacy.txt");
	    file.setFriendlyName("legacy.txt");
	    file.setMimeType("text/plain");
	    file.setTitle("legacy.txt");
	    file.setSize((int)data.length());
	    file.setModUser(user.getUserId());
	    file.setModDate(Calendar.getInstance().getTime());
	    file = APILocator.getFileAPI().saveFile(file, data, ff, user, false);
	    
	    //Call Versionable
	    Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(file.getIdentifier(), user, false);
	    
	    //Check same File
        assertEquals(verAPI.getTitle(),file.getTitle());
        assertEquals(verAPI.getInode(),file.getInode());
        
        //Delete File
        APILocator.getFileAPI().delete(file, user, false);
        APILocator.getFolderAPI().delete(ff, user, false);
	}
	
	@Test(expected = DotDataException.class)
	public void testFindWorkingVersionContentlet() throws Exception{
		//Create Contentlet
		Structure testStructure = APILocator.getStructureAPI().findByVarName("webPageContent", user);

        Contentlet cont=new Contentlet();
        cont.setStructureInode(testStructure.getInode());
        cont.setStringProperty("title", "Testing Working");
        cont.setStringProperty("body", "TESTING");
        cont.setStringProperty("contentHost", host.getIdentifier());
        cont.setReviewInterval( "1m" );
        cont.setStructureInode( testStructure.getInode() );
        cont.setHost( host.getIdentifier() );
        cont = APILocator.getContentletAPI().checkin(cont, user, false);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findWorkingVersion(cont.getIdentifier(), user, false);
	}
	
	@Test
	public void testFindLiveVersionHTMLPage() throws Exception{
		//Create HTMLPage
		String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
		
		Template template=new Template();
        template.setTitle("TEST TEMPLATE VERSIONABLE");
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        
        Folder folder = APILocator.getFolderAPI().createFolders("/testingVersionable", host, user, false);
		
		HTMLPage page=new HTMLPage();
        page.setPageUrl("testpage"+ext);
        page.setFriendlyName("testpage"+ext);
        page.setTitle("testpage"+ext);
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, user, false);
        
        APILocator.getVersionableAPI().setLive(page);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(page.getIdentifier(), user, false);
        
        //Check Same HTMLPage
        assertEquals(verAPI.getTitle(),page.getTitle());
        assertEquals(verAPI.getInode(),page.getInode());
        
        //Delete Template, Folder, HTMLPage
        APILocator.getHTMLPageAPI().delete(page, user, false);
        APILocator.getFolderAPI().delete(folder, user, false);
        APILocator.getTemplateAPI().delete(template, user, false);
	}
	
	@Test
	public void testFindLiveVersionContainer() throws Exception{
		//Create Container
		Container c = new Container();
        c.setFriendlyName("Versionable Container Working");
        c.setTitle("Versionable Container Working");
        c.setMaxContentlets(2);
        c.setPreLoop("preloop code");
        c.setPostLoop("postloop code");

        Structure st=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("host");
        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(st.getInode());
        cs.setCode("this is the code");
        csList.add(cs);
        
        c = APILocator.getContainerAPI().save(c, csList, host, user, false);
        
        APILocator.getVersionableAPI().setLive(c);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(c.getIdentifier(), user, false);
        
        //Check Same Container
        assertEquals(verAPI.getTitle(),c.getTitle());
        assertEquals(verAPI.getInode(),c.getInode());
        
        //Delete Container
        APILocator.getContainerAPI().delete(c, user, false);
	}
	
	@Test
	public void testFindLiveVersionTemplate() throws Exception{
		//Create Template
		Template template=new Template();
        template.setTitle("TEST TEMPLATE VERSIONABLE");
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
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
		//Create File
		String folderName = "/testOldFile"+UUIDGenerator.generateUuid();
	    Folder ff = APILocator.getFolderAPI().createFolders(folderName, host, user, false);
	    
	    // file data in tmp folder
	    java.io.File tmp=new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary()+java.io.File.separator+"testOldFile");
	    if(!tmp.exists()) tmp.mkdirs();
	    java.io.File data=new java.io.File(tmp,
	            "test-"+UUIDGenerator.generateUuid()+".txt");
	    
	    FileWriter fw=new FileWriter(data,true);
	    fw.write("file content"); fw.close();
	    
	    // legacy file creation
	    File file = new File();
	    file.setFileName("legacy.txt");
	    file.setFriendlyName("legacy.txt");
	    file.setMimeType("text/plain");
	    file.setTitle("legacy.txt");
	    file.setSize((int)data.length());
	    file.setModUser(user.getUserId());
	    file.setModDate(Calendar.getInstance().getTime());
	    file = APILocator.getFileAPI().saveFile(file, data, ff, user, false);
	    
	    APILocator.getFileAPI().publishFile(file, user, false);
	    
	    //Call Versionable
	    Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(file.getIdentifier(), user, false);
	    
	    //Check same File
        assertEquals(verAPI.getTitle(),file.getTitle());
        assertEquals(verAPI.getInode(),file.getInode());
        
        //Delete File
        APILocator.getFileAPI().delete(file, user, false);
        APILocator.getFolderAPI().delete(ff, user, false);
	}
	
	@Test(expected = DotDataException.class)
	public void testFindLiveVersionContentlet() throws Exception{
		//Create Contentlet
		Structure testStructure = APILocator.getStructureAPI().findByVarName("webPageContent", user);

        Contentlet cont=new Contentlet();
        cont.setStructureInode(testStructure.getInode());
        cont.setStringProperty("title", "Testing Working");
        cont.setStringProperty("body", "TESTING");
        cont.setStringProperty("contentHost", host.getIdentifier());
        cont.setReviewInterval( "1m" );
        cont.setStructureInode( testStructure.getInode() );
        cont.setHost( host.getIdentifier() );
        cont = APILocator.getContentletAPI().checkin(cont, user, false);
        
        //Call Versionable
        Versionable verAPI = APILocator.getVersionableAPI().findLiveVersion(cont.getIdentifier(), user, false);
	}

}
