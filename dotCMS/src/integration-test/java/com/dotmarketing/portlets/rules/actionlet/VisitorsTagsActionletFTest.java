package com.dotmarketing.portlets.rules.actionlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.LicenseTestUtil;
import com.dotcms.TestBase;
import com.dotcms.repackage.com.google.common.collect.Lists;
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
import com.dotmarketing.portlets.rules.ParameterDataGen;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Testing actionlet to add tags to the visitor object
 *
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class VisitorsTagsActionletFTest extends TestBase {

	private final Random random = new Random();
	private RuleDataGen ruleDataGen;
	private List<Rule> rulesToRemove = Lists.newArrayList();

	private static FolderAPI folderAPI = APILocator.getFolderAPI();
	private static TemplateAPI templateAPI = APILocator.getTemplateAPI();
	private static ContainerAPI containerAPI = APILocator.getContainerAPI();
	private static ContentletAPI contentletAPI = APILocator.getContentletAPI();
	private static LanguageAPI languageAPI = APILocator.getLanguageAPI();

	private static HostAPI hostAPI = APILocator.getHostAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static User sysuser=null;

	private HttpServletRequest request=ServletTestRunner.localRequest.get();
	private final String baseUrl = "http://" + request.getServerName() + ":" + request.getServerPort();

	private final String BLANK_TEMPLATE_INODE = "54b2ca77-4c91-4de5-bcc7-ccd4ce0ecd50";

	@BeforeClass
	public static void prepare () throws Exception {
		LicenseTestUtil.getLicense();
	}

	/**
	 * Test adding tags to visitor object
	 * @throws Exception
	 */
	@Test
	public void addTag1() throws Exception {

		sysuser=userAPI.getSystemUser();
		Host host=hostAPI.findDefaultHost(sysuser, false);

		// setting up the requirements for testing
		// a new cotent to create the persona linked to the visitor
		Contentlet personaContentlet= new Contentlet();
		personaContentlet.setStructureInode(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		personaContentlet.setHost(host.getIdentifier());
		personaContentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
		String name="persona1"+UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		personaContentlet.setProperty(PersonaAPI.NAME_FIELD, name);
		personaContentlet.setProperty(PersonaAPI.KEY_TAG_FIELD, name);
		personaContentlet.setProperty(PersonaAPI.TAGS_FIELD, "persona");
		personaContentlet.setProperty(PersonaAPI.DESCRIPTION_FIELD,"test to delete");
		personaContentlet =contentletAPI.checkin(personaContentlet, sysuser, false);
		contentletAPI.publish(personaContentlet, sysuser, false);

		Persona persona = new Persona(personaContentlet);

		// 4 test rules
		// test 1 : using a single tag
		createTagActionlet("testing");

		// test 2 : using a multiple tags
		createTagActionlet("persona,asia,china,nigeria");

		// test 3 : using a new tag (not from the tag manager)
		createTagActionlet("newtag");

		// test 4 : using multiple a new tags (not from the tag manager)
		createTagActionlet("new2tag,new3tag,new4tag");

		// environment setup
		// FOLDER
		Folder folder = folderAPI.createFolders("/tags-actionlet-"+System.currentTimeMillis(), host, sysuser, false);
		// TEMPLATE
		Template template = templateAPI.find(BLANK_TEMPLATE_INODE, sysuser, false);
		// PAGE
		String page ="tags-page-"+UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		Contentlet pageContentlet = new Contentlet();
		pageContentlet.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		pageContentlet.setHost(host.getIdentifier());
		pageContentlet.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page);
		pageContentlet.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page);
		pageContentlet.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page);
		pageContentlet.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		pageContentlet.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		pageContentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
		pageContentlet.setFolder(folder.getInode());
		pageContentlet=contentletAPI.checkin(pageContentlet, sysuser, false);
		contentletAPI.publish(pageContentlet, sysuser, false);

		// Widget to show the tag
		String title="personawidget"+UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
		String body="<p>$visitor.accruedTags</p>";
		Contentlet widgetContentlet = new Contentlet();
		Structure contentst = StructureFactory.getStructureByVelocityVarName("webPageContent");
		Structure widgetst = StructureFactory.getStructureByVelocityVarName("SimpleWidget");
		widgetContentlet.setStructureInode(widgetst.getInode());
		widgetContentlet.setHost(host.getIdentifier());
		widgetContentlet.setProperty("widgetTitle", title);
		widgetContentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
		widgetContentlet.setProperty("code", body);
		widgetContentlet.setFolder(folder.getInode());
		widgetContentlet=contentletAPI.checkin(widgetContentlet, sysuser, false);
		contentletAPI.publish(widgetContentlet, sysuser, false);

		Container container = null;
		List<Container> containers = containerAPI.findContainersForStructure(contentst.getInode());
		for(Container c : containers){
			if(c.getTitle().equals("Blank Container")){
				container=c;
				break;
			}
		}

		MultiTree m = new MultiTree(pageContentlet.getIdentifier(), container.getIdentifier(), widgetContentlet.getIdentifier());
		MultiTreeFactory.saveMultiTree(m);

		// login
		CookieHandler.setDefault(new CookieManager());
		URL testUrl = new URL(baseUrl+"/c/portal_public/login?my_account_cmd=auth&my_account_login=admin@dotcms.com&password=admin&my_account_r_m=true");
		IOUtils.toString(testUrl.openStream(),"UTF-8");

		String url = baseUrl + folder.getPath() + page + "?mainFrame=true&livePage=0com.dotmarketing.htmlpage.language=1&host_id=" + host.getIdentifier() + "&com.dotmarketing.persona.id=" + persona.getIdentifier() + "&previewPage=2";
		testUrl = new URL(url);
		StringBuilder result = new StringBuilder();
		InputStreamReader isr = new InputStreamReader(testUrl.openStream());
		BufferedReader br = new BufferedReader(isr);
		int byteRead;
		while ( (byteRead = br.read()) != -1){
			result.append((char) byteRead);
		}
		br.close();

		// testing
		Assert.assertTrue("Expected tag 'testing' not found",result.toString().contains("testing"));

		Assert.assertTrue("Expected tag 'persona' not found",result.toString().contains("persona"));
		Assert.assertTrue("Expected tag 'asia' not found",result.toString().contains("asia"));
		Assert.assertTrue("Expected tag 'china' not found",result.toString().contains("china"));
		Assert.assertTrue("Expected tag 'nigeria' not found",result.toString().contains("nigeria"));

		Assert.assertTrue("Expected tag 'newtag' not found",result.toString().contains("newtag"));

		Assert.assertTrue("Expected tag 'new2tag' not found",result.toString().contains("new2tag"));
		Assert.assertTrue("Expected tag 'new3tag' not found",result.toString().contains("new3tag"));
		Assert.assertTrue("Expected tag 'new4tag' not found",result.toString().contains("new4tag"));

		Assert.assertFalse("Unexpected tag 'dollar' found",result.toString().contains("dollar"));

		// clean up
		contentletAPI.unpublish(personaContentlet, sysuser, false);
		contentletAPI.unpublish(widgetContentlet, sysuser, false);
		contentletAPI.unpublish(pageContentlet, sysuser, false);
		contentletAPI.archive(personaContentlet, sysuser, false);
		contentletAPI.archive(widgetContentlet, sysuser, false);
		contentletAPI.archive(pageContentlet, sysuser, false);
		contentletAPI.delete(personaContentlet, sysuser, false);
		contentletAPI.delete(widgetContentlet, sysuser, false);
		contentletAPI.delete(pageContentlet, sysuser, false);

		folderAPI.delete(folder, sysuser, false);
	}

	private Rule createTagActionlet(String value) {
      ruleDataGen =
          new RuleDataGen(Rule.FireOn.EVERY_REQUEST).name(String.format("VisitorsTagsActionlet - fireOnEveryRequest %s", random.nextInt()));
      Rule rule = ruleDataGen.nextPersisted();
      rulesToRemove.add(rule);

      RuleActionDataGen actionDataGen = new RuleActionDataGen().ruleId(rule.getId());
      RuleAction action = actionDataGen.actionlet(VisitorTagsActionlet.class).priority(random.nextInt(100) + 1).next();

      ParameterDataGen pDataGen = new ParameterDataGen().ownerId(action.getId());
      action.addParameter(pDataGen.key(VisitorTagsActionlet.TAGS_KEY).value(value).next());

      actionDataGen.persist(action);
      return rule;
  }

	@After
	public void tearDown () throws Exception {
		URL logoutUrl = new URL(baseUrl + "/destroy.jsp");
		URLConnection con = logoutUrl.openConnection();

		con.connect();
		con.getInputStream();

		for (Rule rule : rulesToRemove) {
            ruleDataGen.remove(rule);
        }
        rulesToRemove.clear();
	}
}
