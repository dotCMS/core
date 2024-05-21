package com.dotcms.languagevariable.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.MultilinguableFallback;
import com.dotcms.keyvalue.business.KeyValueAPI;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCache;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation class for the {@link LanguageVariableAPI}.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 27, 2017
 *
 */
public class LanguageVariableAPIImpl implements LanguageVariableAPI {

  Lazy<ContentType> langVarContentType = Lazy.of(() -> {
      try {
        return APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
      } catch (DotDataException | DotSecurityException e) {
        throw new IllegalStateException("Can't seem to find a content-type for LangVars! ", e);
      }
  });

  private final KeyValueAPI keyValueAPI;
  private final LanguageAPI languageAPI;

  /**
   * Creates a new instance of the {@link LanguageVariableAPI}.
   */
  public LanguageVariableAPIImpl() {
    this(APILocator.getKeyValueAPI(), APILocator.getLanguageAPI());

  }

  @VisibleForTesting
  public LanguageVariableAPIImpl(final KeyValueAPI keyValueAPI, final LanguageAPI languageAPI) {

    this.keyValueAPI = keyValueAPI;
    this.languageAPI = languageAPI;
  }

  @Override
  public String get(final String key, final long languageId, final User user, final boolean respectFrontendRoles) {

    return this.get(key, languageId, user, true, respectFrontendRoles);
  }

  @Override
  public String get(final String key, final long languageId, final User user, final boolean live, final boolean respectFrontendRoles) {

    if (!UtilMethods.isSet(key)) {
      return null;
    }
    String languageValue = null;

    try {

      // get the content type LANGUAGEVARIABLE
      final ContentType languageVariableContentType = APILocator.getContentTypeAPI(user).find(LANGUAGEVARIABLE_VAR_NAME);

      languageValue = this.getValueFromUserLanguage(key, languageId, user, respectFrontendRoles, languageVariableContentType, live);

      if (null == languageValue) {

        languageValue = this.getValueFromUserFallbackLanguage(key, languageId, user, respectFrontendRoles, languageVariableContentType);

        if (null == languageValue && languageVariableContentType instanceof MultilinguableFallback
                && MultilinguableFallback.class.cast(languageVariableContentType).fallback()) {

          languageValue = this.getValueFromDefaultLanguage(key, user, respectFrontendRoles, languageVariableContentType);
        }
      }
    } catch (DotDataException | DotSecurityException e) {

      Logger.debug(this, () -> String.format("Could not retrieve Language Variable '%s': %s", key, e.getMessage(), e));
    }

    return (null != languageValue) ? languageValue : key;
  }

  @Override
  public List<KeyValue> getAllLanguageVariablesKeyStartsWith(final String key, final long languageId, final User user, final int limit)
      throws DotDataException, DotSecurityException {
    return this.keyValueAPI.getKeyValuesByKeyStartingWith(key, languageId, APILocator.getContentTypeAPI(user).find(LANGUAGEVARIABLE_VAR_NAME), user,
        false, limit);
  }

  private String getValueFromUserLanguage(final String key, long languageId, final User user, final boolean respectFrontendRoles,
                                             final ContentType languageVariableContentType, final boolean live) {

    final KeyValue keyValue = this.keyValueAPI.get(key, languageId, languageVariableContentType, user, live, respectFrontendRoles);
    return (null != keyValue) ? keyValue.getValue() : null;
  }

  private String getValueFromUserFallbackLanguage(final String key, long languageId, final User user, final boolean respectFrontendRoles,
      final ContentType languageVariableContentType) {

    KeyValue keyValue = null;
    final Language fallbackLanguage = this.languageAPI.getFallbackLanguage(this.languageAPI.getLanguage(languageId).getLanguageCode());

    if (null != fallbackLanguage) {

      keyValue = this.keyValueAPI.get(key, fallbackLanguage.getId(), languageVariableContentType, user, respectFrontendRoles);
    }

    return (null != keyValue) ? keyValue.getValue() : null;
  }

  private String getValueFromDefaultLanguage(final String key, final User user, final boolean respectFrontendRoles,
      final ContentType languageVariableContentType) {

    final KeyValue keyValue =
        this.keyValueAPI.get(key, this.languageAPI.getDefaultLanguage().getId(), languageVariableContentType, user, respectFrontendRoles);

    return (null != keyValue) ? keyValue.getValue() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Language, List<LanguageVariable>> findAllVariables() {
    final List<Language> languages = languageAPI.getLanguages();

    return languages.stream()
            .collect(Collectors.toMap(lang -> lang, lang -> {
              try {
                return findVariables(lang.getId());
              } catch (DotDataException e) {
                Logger.error(this, "Error finding language variables", e);
                return List.of();
              }
            }));
  }

  /**
   * {@inheritDoc}
   */
  @CloseDBIfOpened
  @Override
  public List<LanguageVariable> findVariables(final long langId) throws DotDataException {

    final ContentType contentType = langVarContentType.get();
    final LanguageCache languageCache = CacheLocator.getLanguageCache();

    return languageCache.ifPresentGetOrElseFetch(langId, ()->{
          //If the language is not in cache, fetch the variables from the database
          final LanguageVariableFactory factory = FactoryLocator.getLanguageVariableFactory();
          return factory.findVariables(contentType, langId, 0, 0, null);
      });
  }

  /**
   * {@inheritDoc}
   */
  @CloseDBIfOpened
  @Override
  public Optional<LanguageVariable> findVariable(final long languageId, final String key) throws DotDataException {
     final LanguageVariableFactory factory = FactoryLocator.getLanguageVariableFactory();
     final ContentType contentType = langVarContentType.get();
     final LanguageCache languageCache = CacheLocator.getLanguageCache();
     return languageCache.ifPresentGetOrElseFetch(languageId,  key, ()->{
         try {
             return factory.findVariables(contentType, languageId, 0, 0, null);
         } catch (DotDataException e) {
             Logger.error(this, "Error finding language variables", e);
             return List.of();
         }
     });
  }

  /**
   * {@inheritDoc}
   */
  @CloseDBIfOpened
  @Override
  public Map<String, List<LanguageVariableExt>> findVariablesGroupedByKey(final int offset, final int limit, final String orderBy) throws DotDataException {

    final LanguageVariableFactory factory = FactoryLocator.getLanguageVariableFactory();
    final ContentType contentType = langVarContentType.get();
    final List<LanguageVariableExt> variables = factory.findVariablesForPagination(contentType, offset, limit, orderBy);
    //Group by key including the language id
    return variables.stream().collect(Collectors.groupingBy(LanguageVariableExt::key));
  }

  /**
   * {@inheritDoc}
   */
  @CloseDBIfOpened
  @Override
  public int countVariablesByKey() {
    final LanguageVariableFactory factory = FactoryLocator.getLanguageVariableFactory();
    final ContentType contentType = langVarContentType.get();
    return factory.countVariablesByKey(contentType);
  }

  /**
   * {@inheritDoc}
   */
  public int countVariablesByKey(final long languageId) {
    final LanguageVariableFactory factory = FactoryLocator.getLanguageVariableFactory();
    final ContentType contentType = langVarContentType.get();
    return factory.countVariablesByKey(contentType, languageId);
  }

  /**
   * Invalidates the language variables cache for the given contentlet.
   * @param contentlet the contentlet to invalidate the cache for.
   */
  public void invalidateLanguageVariablesCache(final Contentlet contentlet) {
    final LanguageCache languageCache = CacheLocator.getLanguageCache();
    languageCache.clearVarsByLang(contentlet.getLanguageId());
  }

}
