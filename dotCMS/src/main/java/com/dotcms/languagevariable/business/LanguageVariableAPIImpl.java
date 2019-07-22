package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.MultilinguableFallback;
import com.dotcms.keyvalue.business.KeyValueAPI;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.List;

/**
 * Implementation class for the {@link LanguageVariableAPI}.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 27, 2017
 *
 */
public class LanguageVariableAPIImpl implements LanguageVariableAPI {

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
      final ContentType languageVariableContentType = APILocator.getContentTypeAPI(user).find(LANGUAGEVARIABLE);

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
    return this.keyValueAPI.getKeyValuesByKeyStartingWith(key, languageId, APILocator.getContentTypeAPI(user).find(LANGUAGEVARIABLE), user,
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

}
