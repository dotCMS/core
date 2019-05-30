package com.dotmarketing.business;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

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
	
	
	
  /**
   * This test is for a language key that does not exists neither as a content or in the properties
   * file, so the key will be returned.
   */
  @Test
  public void getStringsAsMap_returnMap() throws Exception {
    final String BAD_KEY = "KEY-DOES-NOT-EXIST";
    final String CONTENTLET_KEY = "key-exists-as-contentlet";
    final String PROPERTYFILE_KEY = "key-exists-in-properties";
    final String SYSTEM_PROPERTYFILE_KEY = "contenttypes.action.cancel";
    final LanguageAPI lapi = APILocator.getLanguageAPI();
    ContentType langKeyType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("Languagevariable");

    Contentlet con = new ContentletDataGen(langKeyType.id()).setProperty("key", CONTENTLET_KEY).setProperty("value", CONTENTLET_KEY + "works")
        .nextPersisted();

    // Add Languague Variable to local properties
    Map<String, String> propertyKeys = ImmutableMap.of(PROPERTYFILE_KEY, PROPERTYFILE_KEY + "works");

    for (Language lang : APILocator.getLanguageAPI().getLanguages()) {
      lapi.saveLanguageKeys(lang, propertyKeys, ImmutableMap.of(), ImmutableSet.of());
    }

    
    List<String> keys = ImmutableList.of(BAD_KEY, CONTENTLET_KEY,PROPERTYFILE_KEY,SYSTEM_PROPERTYFILE_KEY );
    
    Locale locale = new Locale("en");
    
    Map<String, String> translatedMap = lapi.getStringsAsMap(locale, keys);
    assertEquals(translatedMap.get(BAD_KEY), BAD_KEY);
    assertEquals(translatedMap.get(CONTENTLET_KEY), CONTENTLET_KEY+"works");
    assertEquals(translatedMap.get(PROPERTYFILE_KEY), PROPERTYFILE_KEY+"works");
    assertEquals(translatedMap.get(SYSTEM_PROPERTYFILE_KEY), LanguageUtil.get( locale, SYSTEM_PROPERTYFILE_KEY ));
  }
  
	
	
	
	
	
	
}
