package com.dotmarketing.portlets.contentlet.business;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.RestClientBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Config;
import com.liferay.util.FileUtil;

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class FileAssetTest extends ContentletBaseTest {
	
	Client client;
	WebTarget webTarget;
	
    @Before
    public void before() throws Exception{
    	    	
        LicenseTestUtil.getLicense();

        client=RestClientBuilder.newClient();
        HttpServletRequest request = ServletTestRunner.localRequest.get();
        String serverName = request.getServerName();
        long serverPort = request.getServerPort();
        webTarget = client.target("http://" + serverName + ":" + serverPort + "/");
    }
    
	@Test
	public void fileAssetLanguageDifferentThanDefault()throws DotSecurityException, DotDataException, IOException{
		Config.setProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false);
  	  	int spanish = 2;
  	  	Folder folder = APILocator.getFolderAPI().findSystemFolder();
  	  	java.io.File file = java.io.File.createTempFile("texto", ".txt");
		FileUtil.write(file, "helloworld");
        
  	  	FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
  	  	Contentlet fileInSpanish = fileAssetDataGen.languageId(spanish).nextPersisted();
  	  	Contentlet result = contentletAPI.findContentletByIdentifier(fileInSpanish.getIdentifier(), false, spanish, user, false);
  	  	contentletAPI.publish(result, user, false);
  	  
  	  	//Request by Resource Link (SpeedyAssetServlet)
  	  	Response response = webTarget.path(result.getTitle()).queryParam("language_id", result.getLanguageId()).request().get();
      	Assert.assertEquals(HttpStatus.SC_OK, response.getStatus());
  	  	
      	//Request by Identifier (BinaryExporterServlet)
      	Response responseI = webTarget.path("contentAsset/raw-data/"+result.getIdentifier()+"/fileAsset").queryParam("language_id", result.getLanguageId()).request().get();
      	Assert.assertEquals(HttpStatus.SC_OK, responseI.getStatus());
      	
      	fileAssetDataGen.remove(fileInSpanish);
	}
	
	/*
	 * Test Disabled because is failing sporadically in all DB's
	 * 
	@Test
	public void fileAssetNonExistingLanguageDefaultFilesTrue()throws DotSecurityException, DotDataException, IOException{
		Config.setProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", true);
  	  	int english = 1;
  	  	int spanish = 2;
  	  	Folder folder = APILocator.getFolderAPI().findSystemFolder();
  	  	java.io.File file = java.io.File.createTempFile("texto", ".txt");
		FileUtil.write(file, "helloworld");
        
  	  	FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
  	  	Contentlet fileInEnglish = fileAssetDataGen.languageId(english).nextPersisted();
  	  	Contentlet result = contentletAPI.findContentletByIdentifier(fileInEnglish.getIdentifier(), false, english, user, false);
  	  	contentletAPI.publish(result, user, false);
  	  	contentletAPI.isInodeIndexed(result.getInode());
  	  
  	  	//Request by Resource Link (SpeedyAssetServlet)
  	  	Response response = webTarget.path(result.getTitle()).queryParam("language_id", spanish).request().get();
      	Assert.assertEquals(HttpStatus.SC_OK, response.getStatus());
  	  	
      	//Request by Identifier (BinaryExporterServlet)
      	Response responseI = webTarget.path("contentAsset/raw-data/"+result.getIdentifier()+"/fileAsset").queryParam("language_id", spanish).request().get();
      	Assert.assertEquals(HttpStatus.SC_OK, responseI.getStatus());
  	   
      	fileAssetDataGen.remove(fileInEnglish);
	}
	*/
	
	@Test
	public void fileAssetNonExistingLanguageDefaultFilesFalse()throws DotSecurityException, DotDataException, IOException{
		Config.setProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false);
  	  	int english = 1;
  	  	int spanish = 2;
  	  	Folder folder = APILocator.getFolderAPI().findSystemFolder();
  	  	java.io.File file = java.io.File.createTempFile("texto", ".txt");
		FileUtil.write(file, "helloworld");
        
  	  	FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
  	  	Contentlet fileInEnglish = fileAssetDataGen.languageId(english).nextPersisted();
  	  	Contentlet result = contentletAPI.findContentletByIdentifier(fileInEnglish.getIdentifier(), false, english, user, false);
  	  	contentletAPI.publish(result, user, false);
  	  
  	  	//Request by Resource Link (SpeedyAssetServlet)
  	  	Response response = webTarget.path(result.getTitle()).queryParam("language_id", spanish).request().get();
  	  	Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
  	  	
      	//Request by Identifier (BinaryExporterServlet)
      	Response responseI = webTarget.path("contentAsset/raw-data/"+result.getIdentifier()+"/fileAsset").queryParam("language_id", spanish).request().get();
      	Assert.assertEquals(HttpStatus.SC_NOT_FOUND, responseI.getStatus());
  	  	 
  	  	fileAssetDataGen.remove(fileInEnglish);
	}
	
}
