package com.dotmarketing.portlets.languagesmanager.business;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;

/**
 * Provides data source access to information related to languages in the system. These languages
 * are added by the user as required and allow them to enter language-specific content for building
 * their sites.
 * 
 * @author david torres
 * @version N/A
 * @since Mar 22, 2017
 * 
 */
public abstract class LanguageFactory {

    /**
     * 
     * @param language
     */
    protected abstract void deleteLanguage(Language language);

    /**
     * 
     * @param languageCode
     * @param countryCode
     * @return
     */
    protected abstract Language getLanguage(String languageCode, String countryCode);

    /**
     * 
     * @param id
     * @return
     */
    protected abstract Language getLanguage(String id);

    /**
     * 
     * @return
     */
    protected abstract Language createDefaultLanguage();

    /**
     * 
     * @param id
     * @return
     */
    protected abstract Language getLanguage(long id);

    /**
     * 
     * @return
     */
    protected abstract List<Language> getLanguages();

    /**
     * 
     * @param language
     */
    protected abstract void saveLanguage(Language language);

    /**
     * 
     * @param id
     * @param langId
     * @return
     */
    protected abstract String getLanguageCodeAndCountry(long id, String langId);

    /**
     * This gets the default lang already loaded into cache
     * If not in cache it will load it from the db
     * If it doesnt exists already it create it.
     * @return
     */
    protected abstract Language getDefaultLanguage();

    /**
     * make a the given language the new default
     * @param languageId
     * @throws DotDataException
     */
    protected abstract void makeDefault(final Long languageId) throws DotDataException;


    /**
     * updates the default language on all assets
     * @param oldDefaultLanguage
     * @param newDefaultLanguage
     */
    protected abstract void transferAssets(final Long oldDefaultLanguage, final Long newDefaultLanguage)
            throws DotDataException;

    /**
     * 
     * @param id
     * @return
     */
    protected abstract boolean hasLanguage(String id);

    /**
     * Returns the language object for a specific language id.
     * @param id
     * @return true if has language
     */
    protected abstract boolean hasLanguage(long id);

    /**
     * 
     * @param languageCode
     * @param countryCode
     * @return
     */
    protected abstract boolean hasLanguage(String languageCode, String countryCode);

    /**
     * 
     * @param langCode
     * @return
     */
    protected abstract List<LanguageKey> getLanguageKeys(String langCode);

    /**
     * 
     * @param langCode
     * @param countryCode
     * @return
     */
	protected abstract List<LanguageKey> getLanguageKeys(String langCode, String countryCode);

	/**
	 * 
	 * @param lang
	 */
	protected abstract void createLanguageFiles(Language lang);

	/**
	 * 
	 * @param lang
	 * @param generalKeys
	 * @param specificKeys
	 * @param toDeleteKeys
	 * @throws DotDataException
	 */
	protected abstract void saveLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys, Set<String> toDeleteKeys) throws DotDataException;

    /**
     * Returns the fallback {@link Language} object for a specific language code.
     * <p>
     * When adding Language Variables, keys are usually looked up by language and country. However,
     * language-generic keys can be defined for a given language - without a country - so that keys
     * that are not found using a language-country combination can be looked up using the fallback
     * language instead.
     * 
     * @param languageCode - The ISO code of the fallback language.
     * @return The fallback {@link Language} object.
     */
    protected abstract Language getFallbackLanguage(final String languageCode);

    /**
     * Finds the first language with the language code if possible
     * @param languageCode String
     * @return Optional language
     */
    protected abstract Optional<Language> getFindFirstLanguageByCode(final String languageCode);

    /**
     * Deletes a language by id
     * @param id long
     */
    protected  abstract int deleteLanguageById(final Language fallbackLanguage) ;
}
