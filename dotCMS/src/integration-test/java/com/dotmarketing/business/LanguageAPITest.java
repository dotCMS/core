package com.dotmarketing.business;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.IntegrationTestInitService;

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
		
		CacheLocator.getLanguageCache().putLanguages(APILocator.getLanguageAPI().getLanguages());
		Assert.assertEquals(2,CacheLocator.getLanguageCache().getLanguages().size());		
		
		Language lan = APILocator.getLanguageAPI().getLanguage(102);
		lan = new Language();
		lan.setCountry("Italy");
		lan.setCountryCode("IT");
		lan.setLanguageCode("it");
		lan.setLanguage("Italian");
		APILocator.getLanguageAPI().saveLanguage(lan);
		
		CacheLocator.getLanguageCache().clearCache();
		CacheLocator.getLanguageCache().putLanguages(APILocator.getLanguageAPI().getLanguages());
		Assert.assertEquals(3, CacheLocator.getLanguageCache().getLanguages().size());
		
		APILocator.getLanguageAPI().deleteLanguage(lan);
		
		CacheLocator.getLanguageCache().clearCache();
		CacheLocator.getLanguageCache().putLanguages(APILocator.getLanguageAPI().getLanguages());
		Assert.assertEquals(2,CacheLocator.getLanguageCache().getLanguages().size());
		
		
	}
}
