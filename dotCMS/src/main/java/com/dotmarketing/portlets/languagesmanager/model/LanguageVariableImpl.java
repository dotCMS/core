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

public class LanguageVariableImpl extends DefaultKeyValue implements LanguageVariable {

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

    public LanguageVariableImpl(final String key, final String value, final long languageId) {
        super(key, value);
        this.setLanguageId(languageId);
        this.setContentTypeId(contentType.get().inode());
    }

    @Override
    public String getLanguageCode() {
        return language.get().getLanguageCode();
    }

    @Override
    public String getCountryCode() {
        return language.get().getCountryCode();
    }

    public static LanguageVariable fromContentlet(final Contentlet contentlet) throws DotSecurityException {
        if(contentlet instanceof LanguageVariable) {
            return (LanguageVariable) contentlet;
        }
        LanguageVariableImpl langVar = new LanguageVariableImpl(
                contentlet.getStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR),
                contentlet.getStringProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR),
                contentlet.getLanguageId()
        );
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        contentletAPI.copyProperties(langVar, contentlet.getMap());
        return langVar;
    }

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
        LanguageVariableImpl that = (LanguageVariableImpl) o;
        return Objects.equals(contentType.get().id(), that.contentType.get().id())
                && Objects.equals(language, that.language)
                && Objects.equals(getKey(), that.getKey())
                && Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contentType.get().id(),  getLanguageId(), getKey(), getValue());
    }
}
