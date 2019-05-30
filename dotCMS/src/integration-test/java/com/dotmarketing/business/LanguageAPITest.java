package com.dotmarketing.business;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Date;
import org.junit.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class LanguageAPITest {
	private static User systemUser;
	
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

		systemUser = APILocator.systemUser();
	}

	/*
	 * 1- Put things in cache
	 * 2- Add a language
	 * 3- Make sure cache have all the languages
	 * 4- Remove a language
	 * 5- Make sure cache remove the language
	 * 6- Clear cache
	 * 
	 */
	@Test
	public void languageCache() throws Exception{

		int existingLanguagesCount = APILocator.getLanguageAPI().getLanguages().size();
		APILocator.getLanguageAPI().getLanguages();
		Assert.assertEquals(existingLanguagesCount,CacheLocator.getLanguageCache().getLanguages().size());
		
		Language lan = new Language();
		lan.setCountry("Italy");
		lan.setCountryCode("IT");
		lan.setLanguageCode("it");
		lan.setLanguage("Italian");
		APILocator.getLanguageAPI().saveLanguage(lan);
		lan = APILocator.getLanguageAPI().getLanguage("it", "IT");
		
		existingLanguagesCount = APILocator.getLanguageAPI().getLanguages().size();
		CacheLocator.getLanguageCache().clearCache();
		APILocator.getLanguageAPI().getLanguages();
		Assert.assertEquals(existingLanguagesCount, CacheLocator.getLanguageCache().getLanguages().size());
		
		APILocator.getLanguageAPI().deleteLanguage(lan);
		
		existingLanguagesCount = APILocator.getLanguageAPI().getLanguages().size();
		CacheLocator.getLanguageCache().clearCache();
		APILocator.getLanguageAPI().getLanguages();
		Assert.assertEquals(existingLanguagesCount,CacheLocator.getLanguageCache().getLanguages().size());		
	}

	/**
	 * This test is for a language key that exist as a content,
	 * so it's returned
	 */
	@Test
	public void getStringKey_returnLanguageVariableContent() throws Exception{
		final String KEY_1 = "KEY_"+System.currentTimeMillis();
		final String VALUE_1 = "VALUE_"+System.currentTimeMillis();
		Contentlet contentletEnglish = null;
		try{
			// Using the provided Language Variable Content Type
			final ContentType languageVariableContentType = APILocator.getContentTypeAPI(systemUser)
					.find(LanguageVariableAPI.LANGUAGEVARIABLE);
			contentletEnglish = createTestKeyValueContent(
					KEY_1, VALUE_1, 1,
					languageVariableContentType, systemUser);

			final String value = APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(),KEY_1);

			Assert.assertEquals(VALUE_1,value);

		}finally {
			//Clean up
			if (null != contentletEnglish) {
				deleteContentlets(systemUser, contentletEnglish);
			}
		}
	}


	/**
	 * This test is for a language key that exist in the properties file,
	 * but afterwards is created as a content, so that
	 * language variable gets the priority so the value changes.
	 */
	@Test
	public void getStringKey_KeyExistsPropertiesFileAndAsAContent_returnsLanguageVariableContent() throws Exception{
		final String KEY_1 = "email-address";
		final String VALUE_1 = "Electronic Mail Address";
		Contentlet contentletEnglish = null;
		try{

			String value = APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(),KEY_1);
			Assert.assertEquals("email-address",value);

			// Using the provided Language Variable Content Type
			final ContentType languageVariableContentType = APILocator.getContentTypeAPI(systemUser)
					.find(LanguageVariableAPI.LANGUAGEVARIABLE);
			contentletEnglish = createTestKeyValueContent(
					KEY_1, VALUE_1, 1,
					languageVariableContentType, systemUser);

			value = APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(),KEY_1);

			Assert.assertEquals(VALUE_1,value);

		}finally {
			//Clean up
			if (null != contentletEnglish) {
				deleteContentlets(systemUser, contentletEnglish);
			}
		}
	}

	/**
	 * This test is for a language key that does not exists neither as a content or
	 * in the properties file, so the key will be returned.
	 */
	@Test
	public void getStringKey_returnKey(){
		final String KEY_1 = "KEY DOES NOT EXISTS";
		final String value = APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(),KEY_1);
		Assert.assertEquals(KEY_1,value);
	}
	
	
	
	
	
	
	
	
	
	
	
}
