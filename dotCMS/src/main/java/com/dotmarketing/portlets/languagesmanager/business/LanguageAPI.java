package com.dotmarketing.portlets.languagesmanager.business;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provides access to information related to the different languages that can be added to the
 * system.
 * <p>
 * Multiple languages are fully supported in dotCMS. Back-end users can contribute the same content
 * in multiple languages and front end users may choose to view content in the supported languages.
 * After saving the new language users will be able to open, search for, or create a piece of
 * content of any Content Type in the new language.
 * 
 * @author root
 * @version N/A
 * @since Mar 22, 2012
 *
 */
public interface LanguageAPI {

	String LOCALIZATION_ENHANCEMENTS_ENABLED = "LOCALIZATION_ENHANCEMENTS_ENABLED";
	Lazy<Boolean> localizationEnhancementsEnabled = Lazy.of(
			() -> Config.getBooleanProperty(LOCALIZATION_ENHANCEMENTS_ENABLED, true));

	static boolean isLocalizationEnhancementsEnabled() {
		//this system property is used to enable/disable the localization enhancements from any integration context
		// since the Config class is wrapped within a Lazy object and once it is loaded it is not possible to change the value
		final String enabled = System.getProperty(
				LOCALIZATION_ENHANCEMENTS_ENABLED);

		return enabled != null ? Boolean.parseBoolean(enabled) : localizationEnhancementsEnabled.get();
	}

    /**
     * 
     * @return
     */
	public Language createDefaultLanguage();

	/**
	 * 
	 * @param language
	 */
	public void deleteLanguage(Language language);

	/**
	 * Deletes a fallback Language, that you obtain with {@link #getFallbackLanguage(String)}
	 * @param fallbackLanguage Language
	 */
	void deleteFallbackLanguage(Language fallbackLanguage);

	/**
	 * 
	 * @param language
	 */
	public void saveLanguage(Language language);

	/**
	 * 
	 * @return
	 */
	public Language getDefaultLanguage();

	/**
	 * 
	 * @param languageCode
	 * @param countryCode
	 * @return
	 */
	public Language getLanguage(String languageCode, String countryCode);

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Language getLanguage(String id);

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Language getLanguage(long id);

	/**
	 * 
	 * @return
	 */
	public List<Language> getLanguages();

	/**
	 * 
	 * @param id
	 * @return
	 */
    public boolean hasLanguage (String id);

    /**
     * 
     * @param id
     * @return
     */
    public boolean hasLanguage (long id);

    /**
     * 
     * @param languageCode
     * @param countryCode
     * @return
     */
    public boolean hasLanguage (String languageCode, String countryCode);

    /**
     * 
     * @param id
     * @param langId
     * @return
     */
	public String getLanguageCodeAndCountry(long id, String langId);

	/**
	 * Retrieve the keys general for the language
	 * @param lang
	 * @return
	 */
	@Deprecated(since = "24.05", forRemoval = true)
	public List<LanguageKey> getLanguageKeys(Language lang);

	/**
	 * Retrieves the keys for the given language and specific for the given country
	 * @param langCode
	 * @return
	 */
	@Deprecated(since = "24.05", forRemoval = true)
	public List<LanguageKey> getLanguageKeys(String langCode);

	/**
	 * 
	 * @param langCode
	 * @param countryCode
	 * @return
	 */
	@Deprecated(since = "24.05", forRemoval = true)
	public List<LanguageKey> getLanguageKeys(String langCode, String countryCode);

	/**
	 * 
	 * @param lang
	 */
	@Deprecated(since = "24.05", forRemoval = true)
	public void createLanguageFiles(Language lang);

	/**
	 * 
	 * @param lang
	 * @param generalKeys
	 * @param specificKeys
	 * @param toDeleteKeys
	 * @throws DotDataException
	 */
	@Deprecated(since = "24.05", forRemoval = true)
	public void saveLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys, Set<String> toDeleteKeys) throws DotDataException;

    /**
     * Returns a internationalized value for a given key and language
     *
     * @param lang
     * @param key
     * @return
     */
    public String getStringKey(Language lang, String key);

    /**
     * 
     * @param lang
     * @param key
     * @return
     */
    public int getIntKey(Language lang, String key);

    /**
     * 
     * @param lang
     * @param key
     * @param defaultVal
     * @return
     */
    public int getIntKey(Language lang, String key, int defaultVal);

    /**
     * 
     * @param lang
     * @param key
     * @return
     */
    public float getFloatKey(Language lang, String key);

    /**
     * 
     * @param lang
     * @param key
     * @param defaultVal
     * @return
     */
    public float getFloatKey(Language lang, String key, float defaultVal);

    /**
     * 
     * @param lang
     * @param key
     * @return
     */
    public boolean getBooleanKey(Language lang, String key);

    /**
     * 
     * @param lang
     * @param key
     * @param defaultVal
     * @return
     */
    public boolean getBooleanKey(Language lang, String key, boolean defaultVal);
    
    /**
     * Clear the language cache
     *
     */
    public void clearCache();

    /**
     * Checks if the parameter is an asset type language 
     * @param id
     * @return true if its a language type, otherwise returns false
     */
    public boolean isAssetTypeLanguage(String id);

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
    public Language getFallbackLanguage(final String languageCode);

	/**
	 * Finds the first language with the language code
	 * @param languageCode String
	 * @return Optional language
	 */
	public Optional<Language> getFindFirstLanguageByCode(final String languageCode);

	/**
	 * Return all the languages for a specific contentletInode
	 *
	 * @param contentletInode
	 * @param user
	 * @return
	 * @throws DotSecurityException if the user dont have read permission in the contentlet
	 * @throws DotDataException
	 */
	public List<Language> getAvailableContentLanguages(final String contentletInode, final User user)
			throws DotSecurityException, DotDataException;

	/**
	 * Return if the MULTILINGUAGE_FALLBACK property is activated or not
	 * defaults to false
	 * @return boolean
	 */
    boolean canDefaultContentToDefaultLanguage();

	/**
	 * Return if the DEFAULT_WIDGET_TO_DEFAULT_LANGUAGE property is activated or not
	 * deaults to true
	 * @return boolean
	 */
    boolean canDefaultWidgetToDefaultLanguage();

	/**
	 * Return if the DEFAULT_PAGE_TO_DEFAULT_LANGUAGE property is activated or not, defaults to true
	 *
	 * @return boolean
	 */
    boolean canDefaultPageToDefaultLanguage();

	/**
	 * Return if the DEFAULT_FILE_TO_DEFAULT_LANGUAGE property is activated or not
	 * defaults to false
	 * @return boolean
	 */
    boolean canDefaultFileToDefaultLanguage();

    /**
     * Given a collection of Keys, return
     * a Map of translated values and if those are not found
     * then return the sent key as the value
     * @param locale
     * @param keys
     * @return
     */
    Map<String, String> getStringsAsMap(Locale locale, Collection<String> keys);

	/**
	 * Makes a language the new default
	 * The new default language is returned if the update operation succeeds.
	 * @param languageId
	 * @return
	 */
	Language makeDefault(final Long languageId, final User user)
			throws DotDataException, DotSecurityException;

	/**
	 * Once we change the default language this can transfer assets from the old default lang into the the new one.
	 * @param oldDefaultLanguage
	 * @param newDefaultLanguage
	 */
	void transferAssets(final Long oldDefaultLanguage, final Long newDefaultLanguage, final User user)
			throws DotDataException, DotIndexException, DotSecurityException;

}
