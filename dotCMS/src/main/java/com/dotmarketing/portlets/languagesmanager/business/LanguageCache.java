package com.dotmarketing.portlets.languagesmanager.business;

import com.dotcms.languagevariable.business.LanguageVariable;
import com.dotmarketing.exception.DotDataException;
import java.util.List;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * @author David
 */
public abstract class LanguageCache implements Cachable{

    protected abstract void addLanguage(Language l);

    protected abstract void add404Language(String languageCode, String countryCode);

    protected abstract Language getLanguageById(long id);

    protected abstract Language getLanguageById(String id);

    protected abstract Language getLanguageByCode(String languageCode, String countryCode);

    protected abstract boolean hasLanguage (String id);
    
    protected abstract boolean hasLanguage (long id);
    
    protected abstract boolean hasLanguage (String languageCode, String countryCode);
    
    protected abstract void removeLanguage(Language l);

    public abstract Language getDefaultLanguage();

    public abstract void setDefaultLanguage(Language defaultLanguage);

    public abstract void clearCache();
    
	public abstract String[] getGroups();
    
    public abstract String getPrimaryGroup();

    @Deprecated(since = "24.05", forRemoval = true)
    protected abstract List<LanguageKey> getLanguageKeys(String langCode, String countryCode) throws DotCacheException;

    @Deprecated(since = "24.05", forRemoval = true)
    protected abstract void removeLanguageKeys(String langCode, String countryCode);

    @Deprecated(since = "24.05", forRemoval = true)
    protected abstract void setLanguageKeys(String langCode, String countryCode, List<LanguageKey> keys);

    public abstract List<Language> getLanguages()  ;
    
    public abstract void putLanguages(List<Language> langs);

    /**
     * Removes all the languages stored under the key ALL_LANGUAGES_KEY but
     * will NOT clear the individual languages already in cache.
     */
    public abstract void clearLanguages();

    /**
     * Removes the default language stored under the key DEFAULT_LANGUAGE
     */
    public abstract void clearDefaultLanguage();

    /**
     * Removes the language stored under the key LANGUAGE_KEY_PREFIX + languageId
     * @param languageId the language id
     */
    public abstract void clearVarsByLang(final long languageId);

    /**
     * Removes all language variables stored in cache
     */
    public abstract void clearVariables();

    /**
     * if the there are language stored in cache for the given languageId this will return them
     *
     * @param languageId the language id
     * @param fetch
     * @return
     */
    public abstract List<LanguageVariable> ifPresentGetOrElseFetch(final long languageId,
            Callable<List<LanguageVariable>> fetchFunction) throws DotDataException;

    /**
     * Removes the language stored under the key LANGUAGE_KEY_PREFIX + languageId
     * @param languageId the language id
     */
    public abstract void putVars(long languageId, List<LanguageVariable> vars);

    /**
     * Removes the language stored under the key LANGUAGE_KEY_PREFIX + languageId
     * @param languageId the language id
     */
    public abstract List<LanguageVariable> getVars(final long languageId);

    /**
     * Removes the language stored under the key LANGUAGE_KEY_PREFIX + languageId
     * @param languageId the language id
     * @param key the key
     * @return the language variable
     */
    public abstract Optional<LanguageVariable> getVar(final long languageId, final String key);

    /**
     * Removes the language stored under the key LANGUAGE_KEY_PREFIX + languageId
     * @param languageId the language id
     * @param key  the key
     * @param fetchFunction the fetch function
     * @return the language variable
     * @throws DotDataException if an error occurs
     */
    public abstract Optional<LanguageVariable> ifPresentGetOrElseFetch(final long languageId, final String key,
            final Callable<List<LanguageVariable>> fetchFunction) throws DotDataException;
}
