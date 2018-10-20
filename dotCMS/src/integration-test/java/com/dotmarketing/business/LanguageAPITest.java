package com.dotmarketing.business;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LanguageAPITest {
	
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
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

	@Test
	public void test_language_equals_happy_path() throws Exception{

		final Language language1 = new Language();
		language1.setCountryCode("CR");
		language1.setLanguageCode("es");

		final Language language2 = new Language();
		language2.setCountryCode("CR");
		language2.setLanguageCode("es");

        Assert.assertTrue(Language.equals(language1,language2));
	}

	@Test
	public void test_language_equals_case_differences() throws Exception{

		final Language language1 = new Language();
		language1.setCountryCode("CR");
		language1.setLanguageCode("es");

		final Language language2 = new Language();
		language2.setCountryCode("Cr");
		language2.setLanguageCode("Es");

		Assert.assertTrue(Language.equals(language1,language2));
	}

	@Test
	public void test_language_equals_nulls() throws Exception{

		final Language language1 = new Language();
		language1.setCountryCode("CR");
		language1.setLanguageCode(null);

		final Language language2 = new Language();
		language2.setCountryCode("Cr");
		language2.setLanguageCode(null);

		Assert.assertTrue(Language.equals(language1,language2));
	}

	@Test
	public void test_language_equals_case_nulls() throws Exception{

		final Language language1 = new Language();
		language1.setCountryCode("CRC");
		language1.setLanguageCode(null);

		final Language language2 = new Language();
		language2.setCountryCode("Cr");
		language2.setLanguageCode(null);

		Assert.assertFalse(Language.equals(language1,language2));
	}

}
