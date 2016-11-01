package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.LicenseTestUtil;
import com.dotcms.TestBase;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

/**
 * jUnit test used to verify the results of calling the personas actionlets provided
 * out of the box in dotCMS.
 *
 * @author Oswaldo Gallango
 * @version 1.0
 * @since 01-07-2016
 *
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class PersonaActionletFTest extends TestBase {

	@BeforeClass
	public static void prepare () throws Exception {
		LicenseTestUtil.getLicense();
	}

	private static FolderAPI folderAPI = APILocator.getFolderAPI();
	private static TemplateAPI templateAPI = APILocator.getTemplateAPI();
	private static ContainerAPI containerAPI = APILocator.getContainerAPI();
	private static ContentletAPI contentletAPI = APILocator.getContentletAPI();
	private static PersonaAPI personaAPI = APILocator.getPersonaAPI();
	private static LanguageAPI languageAPI = APILocator.getLanguageAPI();

	private static HostAPI hostAPI = APILocator.getHostAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static User sysuser=null;

	private HttpServletRequest request=ServletTestRunner.localRequest.get();
	private final String baseUrl = "http://" + request.getServerName() + ":" + request.getServerPort();

	/**
	 * Test the creation of a persona in the backend and if the persona object 
	 * change in the $visitor variable for the preview as persona functionality card584
	 * @throws Exception
	 */
	@Test
	public void addPersona() throws Exception {

		sysuser=userAPI.getSystemUser();
		Host host=hostAPI.findDefaultHost(sysuser, false);
		Host systemHost=hostAPI.findSystemHost();

		/*
		 * Create personas for test
		 */
		//Create a Persona related to Single host
		Contentlet persona = new Contentlet();
		persona.setStructureInode(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		persona.setHost(host.getIdentifier());
		persona.setLanguageId(languageAPI.getDefaultLanguage().getId());
		String name="persona1"+UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		persona.setProperty(PersonaAPI.NAME_FIELD, name);
		persona.setProperty(PersonaAPI.KEY_TAG_FIELD, name);
		persona.setProperty(PersonaAPI.TAGS_FIELD, "persona");
		persona.setProperty(PersonaAPI.DESCRIPTION_FIELD,"test to delete");
		persona =contentletAPI.checkin(persona, sysuser, false);
		contentletAPI.publish(persona, sysuser, false);

		Persona personaA = new Persona(persona);
		boolean isPersonaAIndexed = contentletAPI.isInodeIndexed(persona.getInode(), 500);
		Assert.assertTrue(isPersonaAIndexed);

		//Create a Persona related to System Host
		Contentlet persona2 = new Contentlet();
		persona2.setStructureInode(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		persona2.setHost(systemHost.getIdentifier());
		persona2.setLanguageId(languageAPI.getDefaultLanguage().getId());
		String name2="persona2"+UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		persona2.setProperty(PersonaAPI.NAME_FIELD, name2);
		persona2.setProperty(PersonaAPI.KEY_TAG_FIELD, name2);
		persona2.setProperty(PersonaAPI.TAGS_FIELD, "persona");
		persona2.setProperty(PersonaAPI.DESCRIPTION_FIELD,"test to delete");
		persona2 =contentletAPI.checkin(persona2, sysuser, false);
		contentletAPI.publish(persona2, sysuser, false);

		Persona personaB = new Persona(persona2);
		boolean isPersonaBIndexed = contentletAPI.isInodeIndexed(persona2.getInode(), 500);
		Assert.assertTrue(isPersonaBIndexed);

		Assert.assertTrue("PersonaA was not created succesfully",
				UtilMethods.isSet(personaAPI.find(personaA.getIdentifier(), sysuser, false)));
		Assert.assertTrue("PersonaB was not created succesfully",
				UtilMethods.isSet(personaAPI.find(personaB.getIdentifier(), sysuser, false)));

		/*
		 * Test 1:
		 * Create a test page to see if the personas object 
		 * in the $visitor variable change
		 */
		Folder ftest = folderAPI.createFolders("/personafoldertest"+System.currentTimeMillis(), host, sysuser, false);
		//adding page
		String pageStr ="persona-test-page"+UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		List<Template> templates = templateAPI.findTemplatesAssignedTo(host);
		Template template =null;
		for(Template temp: templates){
			if(temp.getTitle().equals("Blank")){
				template=temp;
				break;
			}
		}
		Contentlet contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(host.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, pageStr);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(languageAPI.getDefaultLanguage().getId());
		contentAsset.setFolder(ftest.getInode());
		contentAsset=contentletAPI.checkin(contentAsset, sysuser, false);
		contentletAPI.publish(contentAsset, sysuser, false);

		/*Adding simple widget to show the current persona keytag*/
		String title="personawidget"+UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		String body="<p>#if(\"$visitor.persona.keyTag\" == \""+personaA.getKeyTag()+"\")<h1>showing "+personaA.getKeyTag()+"</h1> #elseif(\"$visitor.persona.keyTag\" == \""+personaB.getKeyTag()+"\") <h1>showing "+personaB.getKeyTag()+"</h1> #else showing default persona #end</p>";
		Contentlet contentAsset2=new Contentlet();
		Structure contentst = StructureFactory.getStructureByVelocityVarName("webPageContent");
		Structure widgetst = StructureFactory.getStructureByVelocityVarName("SimpleWidget");
		contentAsset2.setStructureInode(widgetst.getInode());
		contentAsset2.setHost(host.getIdentifier());
		contentAsset2.setProperty("widgetTitle", title);
		contentAsset2.setLanguageId(languageAPI.getDefaultLanguage().getId());
		contentAsset2.setProperty("code", body);
		contentAsset2.setFolder(ftest.getInode());
		contentAsset2=contentletAPI.checkin(contentAsset2, sysuser, false);
		contentletAPI.publish(contentAsset2, sysuser, false);

		Container container =null;
		List<Container> containers = containerAPI.findContainersForStructure(contentst.getInode());
		for(Container c : containers){
			if(c.getTitle().equals("Blank Container")){
				container=c;
				break;
			}
		}

		/*Relate widget to page*/
		MultiTree m = new MultiTree(contentAsset.getIdentifier(), container.getIdentifier(), contentAsset2.getIdentifier());
		MultiTreeFactory.saveMultiTree(m);


		//Call page to see if the persona functionality is working
		CookieHandler.setDefault(new CookieManager());
		URL testUrl = new URL(baseUrl+"/c/portal_public/login?my_account_cmd=auth&my_account_login=admin@dotcms.com&password=admin&my_account_r_m=true");
		IOUtils.toString(testUrl.openStream(),"UTF-8");

		String urlpersonaA=baseUrl+ftest.getPath()+pageStr+"?mainFrame=true&livePage=0com.dotmarketing.htmlpage.language=1&host_id="+host.getIdentifier()+"&com.dotmarketing.persona.id="+personaA.getIdentifier()+"&previewPage=2";
		testUrl = new URL(urlpersonaA);
		StringBuilder result = new StringBuilder();
		InputStreamReader isr = new InputStreamReader(testUrl.openStream());
		BufferedReader br = new BufferedReader(isr);
		int byteRead;
		while ( (byteRead = br.read()) != -1){
			result.append((char) byteRead);
		}
		br.close();
		Assert.assertTrue("Error the page is not showing the Persona expected",result.toString().contains("showing "+personaA.getKeyTag()));

		String urlpersonaB=baseUrl+ftest.getPath()+pageStr+"?mainFrame=true&livePage=0com.dotmarketing.htmlpage.language=1&host_id="+host.getIdentifier()+"&com.dotmarketing.persona.id="+personaB.getIdentifier()+"&previewPage=2";
		testUrl = new URL(urlpersonaB);
		result = new StringBuilder();
		isr = new InputStreamReader(testUrl.openStream());
		br = new BufferedReader(isr);
		while ( (byteRead = br.read()) != -1){
			result.append((char) byteRead);
		}
		br.close();
		Assert.assertTrue("Error the page is not showing the Persona expected",result.toString().contains("showing "+personaB.getKeyTag()));


		//remove personas, content, page and folder created for this test
		contentletAPI.unpublish(persona, sysuser, false);
		contentletAPI.unpublish(persona2, sysuser, false);

		contentletAPI.unpublish(contentAsset2, sysuser, false);
		contentletAPI.unpublish(contentAsset, sysuser, false);

		contentletAPI.archive(persona, sysuser, false);
		contentletAPI.archive(persona2, sysuser, false);

		contentletAPI.archive(contentAsset2, sysuser, false);
		contentletAPI.archive(contentAsset, sysuser, false);

		contentletAPI.delete(persona, sysuser, false);
		contentletAPI.delete(persona2, sysuser, false);

		contentletAPI.delete(contentAsset2, sysuser, false);
		contentletAPI.delete(contentAsset, sysuser, false);

		folderAPI.delete(ftest, sysuser, false);
	}

	@After
	public void tearDown () throws Exception {
		URL logoutUrl = new URL(baseUrl + "/destroy.jsp");
		URLConnection con = logoutUrl.openConnection();

		con.connect();
		con.getInputStream();
	}

}
