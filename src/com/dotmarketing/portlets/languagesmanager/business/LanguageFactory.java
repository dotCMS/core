package com.dotmarketing.portlets.languagesmanager.business;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;


/**
 *
 * @author  david torres 
 * 
 */
public abstract class LanguageFactory {

    protected abstract void deleteLanguage(Language language);

    protected abstract Language getLanguage(String languageCode, String countryCode);

    protected abstract Language getLanguage(String id);

    protected abstract Language createDefaultLanguage();

    protected abstract Language getLanguage(long id);

    protected abstract List<Language> getLanguages();

    protected abstract void saveLanguage(Language o);

    protected abstract String getLanguageCodeAndCountry(long id, String langId);
    
    protected abstract Language getDefaultLanguage ();

    protected abstract boolean hasLanguage(String id);
    
    protected abstract boolean hasLanguage(long id);

    protected abstract boolean hasLanguage(String languageCode, String countryCode);

    protected abstract List<LanguageKey> getLanguageKeys(String langCode);

	protected abstract List<LanguageKey> getLanguageKeys(String langCode, String countryCode);

	protected abstract void createLanguageFiles(Language lang);

	protected abstract void saveLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys, Set<String> toDeleteKeys) throws DotDataException;
    
}
