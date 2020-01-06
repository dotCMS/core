package com.dotmarketing.portlets.languagesmanager.business;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(DataProviderRunner.class)
public class LanguageAPITest {
	private static User systemUser;
	
	private static Language language;
	
	
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

		systemUser = APILocator.systemUser();
		
		language= new LanguageDataGen().nextPersisted();
		
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
    public void languageCache() throws Exception {

        int existingLanguagesCount = APILocator.getLanguageAPI().getLanguages().size();
        APILocator.getLanguageAPI().getLanguages();
        Assert.assertEquals(existingLanguagesCount, CacheLocator.getLanguageCache().getLanguages().size());



        Language lan = new LanguageDataGen().nextPersisted();

        existingLanguagesCount = APILocator.getLanguageAPI().getLanguages().size();
        CacheLocator.getLanguageCache().clearCache();
        APILocator.getLanguageAPI().getLanguages();
        Assert.assertEquals(existingLanguagesCount, CacheLocator.getLanguageCache().getLanguages().size());

        APILocator.getLanguageAPI().deleteLanguage(lan);
        Assert.assertEquals(existingLanguagesCount - 1, APILocator.getLanguageAPI().getLanguages().size());
        Assert.assertEquals(existingLanguagesCount - 1, CacheLocator.getLanguageCache().getLanguages().size());




        existingLanguagesCount = APILocator.getLanguageAPI().getLanguages().size();
        CacheLocator.getLanguageCache().clearCache();
        APILocator.getLanguageAPI().getLanguages();
        Assert.assertEquals(existingLanguagesCount, CacheLocator.getLanguageCache().getLanguages().size());
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
					KEY_1, VALUE_1, language.getId(),
					languageVariableContentType, systemUser);

			final String value = APILocator.getLanguageAPI().getStringKey(language,KEY_1);

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
			Assert.assertEquals("Email Address",value);

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
    
    final String uniq = UUIDGenerator.shorty();
    final String BAD_KEY = "KEY-DOES-NOT-EXIST";
    final String CONTENTLET_KEY = "key-exists-as-contentlet" + uniq;
    final String PROPERTYFILE_KEY = "key-exists-in-properties" + uniq;
    final String SYSTEM_PROPERTYFILE_KEY = "contenttypes.action.cancel";
    final LanguageAPIImpl languageAPi = new LanguageAPIImpl();
    
    final Language language  = new LanguageDataGen().nextPersisted();
    final LanguageAPIImpl lapi = Mockito.spy(languageAPi);
    Mockito.doReturn(APILocator.systemUser()).when(lapi).getUser();
    final ContentType langKeyType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("Languagevariable");

    Contentlet con = new ContentletDataGen(langKeyType.id())
        .setProperty("key", CONTENTLET_KEY)
        .setProperty("value", CONTENTLET_KEY + "works")
        .languageId(language.getId())
        .nextPersisted();
    APILocator.getContentletAPI().publish(con, APILocator.systemUser(), false);
    
    
    // Add Languague Variable to local properties
    lapi.saveLanguageKeys(language, ImmutableMap.of(PROPERTYFILE_KEY, PROPERTYFILE_KEY + "works"), new HashMap<>(), ImmutableSet.of());
    

    
    final List<String> keys = ImmutableList.of(BAD_KEY, CONTENTLET_KEY,PROPERTYFILE_KEY,SYSTEM_PROPERTYFILE_KEY );
    final Locale locale = language.asLocale();
    
    final Map<String, String> translatedMap = lapi.getStringsAsMap(locale, keys);
    
    // If key does not exist, we just return the key as the value
    assertEquals(translatedMap.get(BAD_KEY), BAD_KEY);
    
    // We should check content based keys - languagevariables - before .properties files
    assertEquals(CONTENTLET_KEY+"works", translatedMap.get(CONTENTLET_KEY));
    
    // then we should check properties files based on the locale
    assertEquals(PROPERTYFILE_KEY+"works",translatedMap.get(PROPERTYFILE_KEY));
    
    
    assertEquals(LanguageUtil.get( new Locale("en", "us"), SYSTEM_PROPERTYFILE_KEY ), translatedMap.get(SYSTEM_PROPERTYFILE_KEY) );
  }

	@DataProvider
	public static Object[] dataProviderSaveLanguage() {

		return new Language[]{
				null,
				new Language(0, "", null, null, null),
				new Language(0, "it", null, null, null),
				new Language(0, "", "IT", null, null),
		};
	}

	@Test(expected = IllegalArgumentException.class)
	@UseDataProvider("dataProviderSaveLanguage")
	public void test_saveLanguage_InvalidLanguage_ShouldThrowException(final Language language) {
  		APILocator.getLanguageAPI().saveLanguage(language);
	}

	@Test(expected = DotStateException.class)
	public void test_deleteLanguage_WithExistingContent_ShouldFail() {
		Language newLanguage = null;
  		ContentType testType = null;
  		try {
			newLanguage = new LanguageDataGen().nextPersisted();
			testType = new ContentTypeDataGen().nextPersisted();
			// We don't care about the reference to the content since deleting the type will take care of it
			new ContentletDataGen(testType.id())
					.languageId(newLanguage.getId())
					.nextPersisted();

			APILocator.getLanguageAPI().deleteLanguage(newLanguage);
		} finally {
			// clean up
			// new final var to be able to use Sneaky
			final ContentType typeToDelete = testType;
			if(testType!=null) {
				Sneaky.sneaked(()->
					APILocator.getContentTypeAPI(systemUser).delete(typeToDelete)
				);
			}

			if(language!=null) {
				APILocator.getLanguageAPI().deleteLanguage(newLanguage);
			}
		}
	}

}
