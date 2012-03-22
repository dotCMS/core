package com.dotmarketing.portlets.languagesmanager.business;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.liferay.portal.language.LanguageException;

public interface LanguageAPI {

	public Language createDefaultLanguage();

	public void deleteLanguage(Language language);

	public void saveLanguage(Language o);

	public Language getDefaultLanguage();

	public Language getLanguage(String languageCode, String countryCode);

	public Language getLanguage(String id);

	public Language getLanguage(long id);

	public List<Language> getLanguages();
	
    public boolean hasLanguage (String id);
    
    public boolean hasLanguage (long id);
    
    public boolean hasLanguage (String languageCode, String countryCode);

	public String getLanguageCodeAndCountry(long id, String langId);

	/**
	 * Retrieve the keys general for the language
	 * @param lang
	 * @return
	 */
	public List<LanguageKey> getLanguageKeys(Language lang);

	/**
	 * Retrieves the keys for the given language and specific for the given country
	 * @param langCode
	 * @param countryCode
	 * @return
	 */
	public List<LanguageKey> getLanguageKeys(String langCode);

	public List<LanguageKey> getLanguageKeys(String langCode, String countryCode);

	public void createLanguageFiles(Language lang);

	public void saveLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys, Set<String> toDeleteKeys) throws DotDataException;
	
    public String getStringKey(Language lang, String key);

    public int getIntKey(Language lang, String key);

    public int getIntKey(Language lang, String key, int defaultVal);

    public float getFloatKey(Language lang, String key);

    public float getFloatKey(Language lang, String key, float defaultVal);

    public boolean getBooleanKey(Language lang, String key);

    public boolean getBooleanKey(Language lang, String key, boolean defaultVal);
    
    /**
     * Clear the language cache
     *
     */
    public void clearCache();
    
	public void addLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys) throws DotDataException, LanguageException;
	
	public void updateLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys) throws DotDataException, LanguageException;
	
	public void deleteLanguageKeys(Language lang,Set<String> toDeleteKeys) throws DotDataException, LanguageException;
	
}
