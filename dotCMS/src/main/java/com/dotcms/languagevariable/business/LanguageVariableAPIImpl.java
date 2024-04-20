package com.dotcms.languagevariable.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.MultilinguableFallback;
import com.dotcms.keyvalue.business.KeyValueAPI;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCache;
import com.dotmarketing.portlets.languagesmanager.model.LangVariableImpl;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageVariable;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
      return APILocator.getContentTypeAPI(APILocator.systemUser()).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
    } catch (DotDataException | DotSecurityException e) {
      throw new IllegalStateException("Can't seem to find a content-type for LangVars! ",e);
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

  @Override
  public List<LanguageVariable> findAllVariables() throws DotDataException{
       final List<Language> languages = languageAPI.getLanguages();
         return languages.stream()
                .map(Language::getId)
                .map(langId -> {
                     try {
                          return findVariables(langId);
                     } catch (DotDataException e) {
                          Logger.error(this, e.getMessage(), e);
                          return List.<LanguageVariable>of();
                     }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   *
   * @param langId - The ID of the language that the variable was created for.
   * @return
   * @throws DotDataException
   */
  @CloseDBIfOpened
  @Override
  public List<LanguageVariable> findVariables(final long langId)
            throws DotDataException {
      final ContentletFactory contentletFactory = FactoryLocator.getContentletFactory();
      final ContentType contentType = langVarContentType.get();
      final LanguageCache languageCache = CacheLocator.getLanguageCache();
      final List<LanguageVariable> cacheVars = languageCache.getVars(langId);
      if (cacheVars != null && !cacheVars.isEmpty()) {
          return cacheVars;
      }
    //We bring non-archived live contentlets
      final List<Contentlet> byContentTypeAndLanguage = contentletFactory.findByContentTypeAndLanguage(
              contentType, langId, 0, 0, LanguageVariableAPI.ORDER_BY_KEY, false);
      final ImmutableList<LanguageVariable> languageVariables = byContentTypeAndLanguage.stream()
              .map(fromContentlet()).filter(Objects::nonNull)
              .collect(CollectionsUtils.toImmutableList());

      languageCache.putVars(langId, languageVariables);
      return languageVariables;
    }

  @CloseDBIfOpened
  @Override
  public Map<String, List<LanguageVariable>> findVariablesForPagination(final int offset, final int limit,
          final String orderBy)
          throws DotDataException {
    final ContentletFactory contentletFactory = FactoryLocator.getContentletFactory();
    final ContentType contentType = langVarContentType.get();

    //We bring non-archived live contentlets
    final List<Contentlet> byContentTypeAndLanguage = contentletFactory.findByContentType(
            contentType, offset, limit, orderBy, false);

    final List<LanguageVariable> fetchedVariables = byContentTypeAndLanguage.stream()
            .map(fromContentlet()).filter(Objects::nonNull)
            .collect(CollectionsUtils.toImmutableList());

    return fetchedVariables.stream()
           .collect(Collectors.groupingBy(LanguageVariable::getKey, LinkedHashMap::new, Collectors.toList()));
  }

  @CloseDBIfOpened
  @Override
  public int countLiveVariables() {
      final ContentletFactory contentletFactory = FactoryLocator.getContentletFactory();
      final ContentType contentType = langVarContentType.get();
      return contentletFactory.countByTypeWorkingOrLive(contentType, false);
    }

   private Function<Contentlet, LanguageVariable> fromContentlet() {
      return contentlet -> {
        try {
          return LangVariableImpl.fromContentlet(contentlet);
        } catch (DotSecurityException e) {
          Logger.warn(this, e.getMessage(), e);
          return null;
        }
      };
    }

    public void invalidateLanguageVariablesCache(final Contentlet contentlet) {
      final LanguageCache languageCache = CacheLocator.getLanguageCache();
           languageCache.clearVarsByLang(contentlet.getLanguageId());
    }

}
