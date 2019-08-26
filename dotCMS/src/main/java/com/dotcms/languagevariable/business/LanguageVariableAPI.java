package com.dotcms.languagevariable.business;

import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.List;

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
	
	public static final String LANGUAGEVARIABLE = "Languagevariable";

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

}
