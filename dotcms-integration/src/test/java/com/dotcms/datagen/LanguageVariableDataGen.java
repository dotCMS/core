package com.dotcms.datagen;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import io.vavr.Lazy;

/**
 * Data generator for Language Variables
 */
public class LanguageVariableDataGen extends ContentletDataGen {

    /**
     * Lazy loaded content type for Language Variables
     */
    public static Lazy<ContentType> langVarContentType = Lazy.of(() -> {
        try {
            return APILocator.getContentTypeAPI(APILocator.systemUser()).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
        } catch (DotDataException | DotSecurityException e) {
            throw new IllegalStateException("Can't seem to find a content-type for LangVars! ",e);
        }
    });

    /**
     * Default constructor
     */
    public LanguageVariableDataGen() {
        super(langVarContentType.get());
    }

    /**
     * Constructor with language id
     * @param key the key
     */
    public LanguageVariableDataGen key(final String key) {
        setProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR, key);
        return this;
    }

    /**
     * Constructor with language id
     * @param value the value
     */
    public LanguageVariableDataGen value(final String value) {
        setProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR, value);
        return this;
    }

    /**
     * Constructor with language id
     * @param languageId the language id
     */
    public LanguageVariableDataGen languageId(long languageId){
        super.languageId(languageId);
        return this;
    }

}