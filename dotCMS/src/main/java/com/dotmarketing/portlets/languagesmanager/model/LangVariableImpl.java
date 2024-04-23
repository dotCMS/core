package com.dotmarketing.portlets.languagesmanager.model;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.keyvalue.model.DefaultKeyValue;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import io.vavr.Lazy;
import java.util.Objects;

/**
 * Implementation of the LanguageVariable interface.
 * This is basically a contentlet with a specific content type Key Value and language.
 */
public class LangVariableImpl extends DefaultKeyValue implements LanguageVariable {

    Lazy<ContentType> contentType = Lazy.of(() -> {
        try {
            return APILocator.getContentTypeAPI(APILocator.systemUser()).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
        } catch (DotDataException | DotSecurityException e) {
            throw new IllegalStateException("Can't seem to find a content-type for LangVars! ",e);
        }
    });

    Lazy<Language> language = Lazy.of(() -> {
        final long languageId = getLanguageId();
        try {
            return APILocator.getLanguageAPI().getLanguage(languageId);
        } catch (Exception  e) {
            throw new IllegalStateException("Can't seem to find a language for id: " + languageId, e);
        }
    });

    /**
     * Default constructor
     * @param key  the key
     * @param value the value
     * @param languageId the language id
     */
    public LangVariableImpl(final String key, final String value, final long languageId) {
        super(key, value);
        this.setLanguageId(languageId);
        this.setContentTypeId(contentType.get().inode());
    }

    /**
     * Returns the language id
     * @return the language id
     */
    @Override
    public String getLanguageCode() {
        return language.get().getLanguageCode();
    }

    /**
     * Returns the country code
     * @return the country code
     */
    @Override
    public String getCountryCode() {
        return language.get().getCountryCode();
    }

    /**
     * Returns the language id
     * @return the language id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        LangVariableImpl that = (LangVariableImpl) o;
        return Objects.equals(contentType.get().id(), that.contentType.get().id())
                && Objects.equals(language, that.language)
                && Objects.equals(getKey(), that.getKey())
                && Objects.equals(getValue(), that.getValue());
    }

    /**
     * Returns the hash code
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contentType.get().id(),  getLanguageId(), getKey(), getValue());
    }

    /**
     * Returns the string representation of the object
     * @return the string representation of the object
     */
    @Override
    public String toString() {
        return "LangVariableImpl{key=" + getKey() + ", value= " + getValue() + ", language=" + getLanguageId() + '}';
    }

    /**
     * Returns the language id
     * @return the language id
     */
    public static LanguageVariable fromContentlet(final Contentlet contentlet) throws DotSecurityException {
        if(contentlet instanceof LanguageVariable) {
            return (LanguageVariable) contentlet;
        }
        LangVariableImpl langVar = new LangVariableImpl(
                contentlet.getStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR),
                contentlet.getStringProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR),
                contentlet.getLanguageId()
        );
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        contentletAPI.copyProperties(langVar, contentlet.getMap());
        return langVar;
    }
}
