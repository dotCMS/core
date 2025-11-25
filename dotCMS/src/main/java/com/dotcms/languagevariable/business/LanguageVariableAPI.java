package com.dotcms.languagevariable.business;

import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides access to Language Variable objects in the system, which allow you to associate a key to
 * a specific value using specific a Content Type and language.
 * <p>
 * dotCMS allows easy and flexible management of multilingual content versions and multiple
 * websites, but a completely multi-lingual site will need to handle multi-lingual navigation menus,
 * buttons, etc, that may not exist as content. The use of global language variables helps to solve
 * this problem and allows multiple websites you manage to take advantage of the same variables for
 * re-use across your sites.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 27, 2017
 *
 */
public interface LanguageVariableAPI {
	
	String LANGUAGEVARIABLE_VAR_NAME = "Languagevariable";

    /**
     * Returns the Language Variable value associated to the specified key and language ID. This
     * method has a series of fallback mechanisms:
     * <ol>
     * <li>If the key doesn't exist for the specified language ID, then they key will be looked up
     * using the fallback language, i.e., looking up its value using ONLY the language code.</li>
     * <li>If the fallback value doesn't exist and the {@code MULTILINGUAGE_FALLBACK}
     * is {@code true}, then look up the key using the system's default language.</li>
     * <li>If none of the previous two appraoches work, then return {@code null}.</li>
     * </ol>
     * 
     * @param key - The key to the Language Variable.
     * @param languageId - The ID of the language that the variable was created for.
     * @param user - The user performing this action.
     * @param respectFrontendRoles - Set to {@code true} if this method requires that front-end
     *        roles are take in count for the search (which means this is being called from the
     *        front-end). Otherwise, set to {@code false}.
     * @return The value of the Language Variable that matched the search criteria, or {@code null}
     *         if it doesn't exist.
     */
    public String get(final String key, final long languageId, final User user, final boolean respectFrontendRoles);


    public String get(final String key, final long languageId, final User user, final boolean live, final boolean respectFrontendRoles);

    /**
     * Returns the Language Variable value associated to the specified key and language ID. This
     * method has a series of fallback mechanisms:
     * <ol>
     * <li>If the key doesn't exist for the specified language ID, then they key will be looked up
     * using the fallback language, i.e., looking up its value using ONLY the language code.</li>
     * <li>If the fallback value doesn't exist and the {@code MULTILINGUAGE_FALLBACK}
     * is {@code true}, then look up the key using the system's default language.</li>
     * <li>If none of the previous two appraoches work, then return {@code null}.</li>
     * </ol>
     *
     * This method is pretty much {@link #get(String, long, User, boolean)} with respectFrontendRoles in true.
     *
     * @param key - The key to the Language Variable.
     * @param languageId - The ID of the language that the variable was created for.
     * @param user - The user performing this action.

     * @return The value of the Language Variable that matched the search criteria, or {@code null}
     *         if it doesn't exist.
     */
    public default String getLanguageVariableRespectingFrontEndRoles(final String key, final long languageId, final User user) {

        return this.get(key, languageId, user, Boolean.TRUE);
    }

    /**
     * Returns the Language Variable value associated to the specified key and language ID. This
     * method has a series of fallback mechanisms:
     * <ol>
     * <li>If the key doesn't exist for the specified language ID, then they key will be looked up
     * using the fallback language, i.e., looking up its value using ONLY the language code.</li>
     * <li>If the fallback value doesn't exist and the {@code MULTILINGUAGE_FALLBACK}
     * is {@code true}, then look up the key using the system's default language.</li>
     * <li>If none of the previous two appraoches work, then return {@code null}.</li>
     * </ol>
     *
     * This method is pretty much {@link #get(String, long, User, boolean)} with respectFrontendRoles in false.
     *
     * @param key - The key to the Language Variable.
     * @param languageId - The ID of the language that the variable was created for.
     * @param user - The user performing this action.

     * @return The value of the Language Variable that matched the search criteria, or {@code null}
     *         if it doesn't exist.
     */
    public default String getLanguageVariable(final String key, final long languageId, final User user) {

        return this.getLanguageVariable(key, languageId, user, true, false);
    }

    default String getLanguageVariable(
            final String key,
            final long languageId,
            final User user,
            final boolean live,
            final boolean respectFrontendRoles) {

        return this.get(key, languageId, user, live, respectFrontendRoles);
    }

    /**
     *
     * Returns a list of {@link KeyValue} that the key starts with the specified key and languageId.
     *
     * @param key - The key to the Language Variable that starts with.
     * @param languageId - The ID of the language that the variable was created for.
     * @param user - The user performing this action.
     * @param limit - Size of the list.
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public List<KeyValue> getAllLanguageVariablesKeyStartsWith(final String key, final long languageId, final User user, final int limit)
            throws DotDataException, DotSecurityException;


    String ORDER_BY_DEFAULT = "contentlet.identifier";

    /**
     * Returns all the Language Variables in the system.
     * useful for cache building.
     * @return List of Language Variables.
     */
    Map<Language, List<LanguageVariable>> findAllVariables() ;

    /**
     * Returns all the Language Variables for the specified language.
     * @param langId - The ID of the language that the variable was created for.
     * @return List of Language Variables.
     * @throws DotDataException - If an error occurs while retrieving the Language Variables.
     */
    List<LanguageVariable> findVariables(final long langId) throws DotDataException;

    /**
     * Returns an Optional of {@link LanguageVariable} matching the specified language ID and key.
     * @param languageId - The ID of the language that the variable was created for.
     * @param key - The key to the Language Variable that starts with.
     * @return Optional of Language Variables.
     * @throws DotDataException - If an error occurs while retrieving the Language Variables.
     */
    Optional<LanguageVariable> findVariable(final long languageId, final String key) throws DotDataException;

    /**
     * Returns a list of {@link LanguageVariable} that the key starts with the specified key and
     *
     * @param offset  - The offset of the list.
     * @param limit   - Size of the list.
     * @param orderBy - The order by clause.
     * @return List of {@link LanguageVariable}
     * @throws DotDataException - If there is an error retrieving the list of Language Variables.
     */
    Map<String, List<LanguageVariableExt>> findVariablesGroupedByKey(int offset, int limit, String orderBy)
            throws DotDataException;

    /**
     * Count content Variables
     * @return the number of content variables unique by key
     */
    int countVariablesByKey();

    /**
     * Count content Variables
     * @param languageId - The ID of the language that the variable was created for.
     * @return the number of content variables unique by key
     */
    int countVariablesByKey(final long languageId);

    /**
     * Invalidate the cache for the Language Variables
     * @param contentlet - The contentlet that will be used to invalidate the cache.
     */
    void invalidateLanguageVariablesCache(Contentlet contentlet);
}
