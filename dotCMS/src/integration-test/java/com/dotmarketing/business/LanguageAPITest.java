package com.dotmarketing.business;

import org.junit.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.languagesmanager.model.Language;

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
}
