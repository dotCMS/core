package com.dotmarketing.portlets.contentlet.ajax;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;

/**
 *
 * @author oswaldogallango
 *
 */
public class ContentletAjaxTest {
	
	private Language language;
	private Contentlet contentlet;
	private User systemUser = APILocator.systemUser();
	
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

	/**
	 * Test problem on "Content Search" when switching on one language 
	 * show all the contentlets 
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
	@Test
	public void issue5330() throws DotDataException, DotSecurityException{
		/*
		 * Creating language
		 */
		Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
		language = new LanguageDataGen().nextPersisted();

		/*
		 * Creating multilanguage contententlet
		 */
		Host host = APILocator.getHostAPI().findDefaultHost(systemUser, false);

		Structure structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent");
		contentlet = new Contentlet();
		contentlet.setContentTypeId(structure.getInode());
		contentlet.setHost(host.getIdentifier());
		contentlet.setLanguageId(defaultLang.getId());
		String title = "testIssue5330"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentlet.setStringProperty("title", title);
		contentlet.setStringProperty("body", "testIssue5330");
		contentlet.setHost(host.getIdentifier());
		contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);

		contentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser,false);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
		APILocator.getVersionableAPI().setLive(contentlet);
		APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(),true);
		
		String ident = contentlet.getIdentifier();
		contentlet = APILocator.getContentletAPI().findContentletByIdentifier(ident, true, defaultLang.getId(), systemUser, false);
		contentlet.setLanguageId(language.getId());
		contentlet.setStringProperty("body", "italianTestIssue5330");
		contentlet.setInode("");
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);

		contentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser,false);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
		APILocator.getVersionableAPI().setLive(contentlet);
		APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(),true);
		/*
		 * Validate that there are two contentlets associated to the same identifier wit different languages
		 */
		List<Contentlet> contList = APILocator.getContentletAPI().getSiblings(ident);
		Assert.assertEquals(2, contList.size());

		/*
		 * Get english version
		 */
		List<String> fieldsValues = new ArrayList<String>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		fieldsValues.add("languageId");
		fieldsValues.add(String.valueOf(defaultLang.getId()));
		List<String> categories = new ArrayList<String>();
		
		List<Object> results=new ContentletAjax().searchContentletsByUser(structure.getInode(), fieldsValues, categories, false, false, false, false,1, "modDate Desc", 10,systemUser, null, null, null);
		Map<String,Object> result = (Map<String,Object>)results.get(0);
		Assert.assertEquals((Long)result.get("total"), new Long(1));
		result = (Map<String,Object>)results.get(3);
		Assert.assertTrue(Long.parseLong(String.valueOf(result.get("languageId")))==defaultLang.getId());
		contentlet = APILocator.getContentletAPI().find(String.valueOf(result.get("inode")),systemUser,false);
		APILocator.getContentletAPI().archive(contentlet,systemUser,false);
		APILocator.getContentletAPI().delete(contentlet,systemUser,false);

		/*
		 * Get italian version
		 */
		fieldsValues = new ArrayList<String>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		fieldsValues.add("languageId");
		fieldsValues.add(String.valueOf(language.getId()));

		results=new ContentletAjax().searchContentletsByUser(structure.getInode(), fieldsValues, categories, false, false, false, false,1, "modDate Desc", 10,systemUser, null, null, null);
		result = (Map<String,Object>)results.get(0);
		Assert.assertEquals(new Long(1L), (Long)result.get("total"));
		result = (Map<String,Object>)results.get(3);
		Assert.assertTrue(Long.parseLong(String.valueOf(result.get("languageId")))==language.getId());
		contentlet = APILocator.getContentletAPI().find(String.valueOf(result.get("inode")),systemUser,false);
		APILocator.getContentletAPI().destroy(contentlet, systemUser, false);
	}

	@Test
	public void test_doSearchGlossaryTerm_ReturnsListLanguageVariables()
			throws Exception {
	    language = new LanguageDataGen().nextPersisted();
		Contentlet languageVariable1 = null;
		Contentlet languageVariable2 = null;
		Contentlet languageVariable3 = null;
		try {

			long time = System.currentTimeMillis();

			final ContentType languageVariableContentType = APILocator.getContentTypeAPI(systemUser)
					.find(LanguageVariableAPI.LANGUAGEVARIABLE);
			languageVariable1 = createTestKeyValueContent(
					"brought.you.by.IT"+time, "hello world", language.getId(),
					languageVariableContentType, systemUser);
			languageVariable2 = createTestKeyValueContent(
					"brought.you.by.Java"+time, "hello world", language.getId(),
					languageVariableContentType, systemUser);

			languageVariable3 = createTestKeyValueContent(
					"brought.you.by.Jay"+time, "hello world", language.getId(),
					languageVariableContentType, systemUser);

			final ContentletAjax contentletAjax = new ContentletAjax();
			List<String[]> languageVariablesList =
					contentletAjax.doSearchGlossaryTerm("brought.you.by.IT",String.valueOf(language.getId()));
			Assert.assertEquals("languageVariablesList: "
					+ languageVariablesList.toString(),1,languageVariablesList.size());
			languageVariablesList =
					contentletAjax.doSearchGlossaryTerm("brought.you.by.J",String.valueOf(language.getId()));
			Assert.assertEquals("languageVariablesList: "
					+ languageVariablesList.toString(),2,languageVariablesList.size());
			languageVariablesList =
					contentletAjax.doSearchGlossaryTerm("brought.you.by",String.valueOf(language.getId()));
			Assert.assertEquals("languageVariablesList: "
					+ languageVariablesList.toString(),3,languageVariablesList.size());

		}finally {
			if(languageVariable1 != null){
				deleteContentlets(systemUser,languageVariable1);
			}
			if(languageVariable2 != null){
				deleteContentlets(systemUser,languageVariable2);
			}
			if(languageVariable3 != null){
				deleteContentlets(systemUser,languageVariable3);
			}
		}

	}

}
