package com.dotmarketing.portlets.contentlet.ajax;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

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
		language = APILocator.getLanguageAPI().getLanguage(102);
		if(!UtilMethods.isSet(language) || language.getId() == 0){
			if(DbConnectionFactory.getDBType().equals("Microsoft SQL Server")){
				DotConnect dc = new DotConnect();
				String sql = "set identity_insert language ON;insert into language(id,language_code,country_code,language,country) values(102,'it','IT','Italian','Italy');set identity_insert language OFF;";
				dc.setSQL(sql);
				dc.loadResult();
			}else{
				language = new Language();
				language.setCountry("Italy");
				language.setCountryCode("IT");
				language.setLanguageCode("it");
				language.setLanguage("Italian");
				APILocator.getLanguageAPI().saveLanguage(language);
				/*
				 * changing id to recreate possible match between languages
				 */
				DotConnect dc = new DotConnect();
				dc.setSQL("update language set id="+defaultLang.getId()+"02 where language_code='it'");
				dc.loadResult();
			}
			language = APILocator.getLanguageAPI().getLanguage("it", "IT");
		}

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

		contentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser,false);
		APILocator.getVersionableAPI().setLive(contentlet);
		APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(),true);
		
		String ident = contentlet.getIdentifier();
		contentlet = APILocator.getContentletAPI().findContentletByIdentifier(ident, true, defaultLang.getId(), systemUser, false);
		contentlet.setLanguageId(language.getId());
		contentlet.setStringProperty("body", "italianTestIssue5330");
		contentlet.setInode("");

		contentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser,false);
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
		Assert.assertTrue((Long)result.get("total")==1);
		result = (Map<String,Object>)results.get(3);
		Assert.assertTrue(Long.parseLong(String.valueOf(result.get("languageId")))==defaultLang.getId());

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
		Assert.assertTrue((Long)result.get("total")==1);
		result = (Map<String,Object>)results.get(3);
		Assert.assertTrue(Long.parseLong(String.valueOf(result.get("languageId")))==language.getId());
	}
	
	@After
    public void deletes() throws Exception {
		APILocator.getContentletAPI().unpublish(contentlet, systemUser, false);
		APILocator.getContentletAPI().archive(contentlet, systemUser, false);
		APILocator.getContentletAPI().delete(contentlet, systemUser, false);
		APILocator.getLanguageAPI().deleteLanguage(language);
    }

}
