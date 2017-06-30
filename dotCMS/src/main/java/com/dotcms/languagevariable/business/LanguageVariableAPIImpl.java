package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.keyvalue.business.KeyValueAPI;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

/**
 * Implementation class for the {@link LanguageVariableAPI}.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 27, 2017
 *
 */
public class LanguageVariableAPIImpl implements LanguageVariableAPI {

    private KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
    private LanguageAPI languageAPI = APILocator.getLanguageAPI();

    /**
     * Creates a new instance of the {@link LanguageVariableAPI}.
     */
    public LanguageVariableAPIImpl() {
        
    }

    @VisibleForTesting
    public LanguageVariableAPIImpl(KeyValueAPI keyValueAPI, LanguageAPI languageAPI) {
        this.keyValueAPI = keyValueAPI;
        this.languageAPI = languageAPI;
    }

    @Override
    public String get(final String key, final long languageId, final User user, final boolean respectFrontendRoles) {
        try {
            ContentType languageVariableCt = APILocator.getContentTypeAPI(user).find("Languagevariable");
            KeyValue keyValue = this.keyValueAPI.get(key, languageId, languageVariableCt, user, respectFrontendRoles);
            if (null != keyValue) {
                return keyValue.getValue();
            } else {
                Language language = this.languageAPI.getLanguage(languageId);
                Language fallbackLanguage = this.languageAPI.getFallbackLanguage(language.getLanguageCode());
                if (null != fallbackLanguage) {
                    keyValue = this.keyValueAPI.get(key, fallbackLanguage.getId(), languageVariableCt, user, respectFrontendRoles);
                    if (null != keyValue) {
                        return keyValue.getValue();
                    }
                }
                if (Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", Boolean.FALSE)) {
                    language = this.languageAPI.getDefaultLanguage();
                    keyValue = this.keyValueAPI.get(key, language.getId(), languageVariableCt, user, respectFrontendRoles);
                    if (null != keyValue) {
                        return keyValue.getValue();
                    }
                }
            }
        } catch (DotDataException | DotSecurityException e) {
            Logger.debug(this, String.format("Could not retrieve Language Variavle '%s': %s", key, e.getMessage()), e);
        }
        return null;
    }

}
