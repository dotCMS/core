package com.dotcms.model.language;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.views.CommonViews.LanguageFileView;
import com.dotcms.model.views.CommonViews.LanguageReadView;
import com.dotcms.model.views.CommonViews.LanguageWriteView;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Language.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public interface AbstractLanguage {

    String TYPE = "Language";

    @JsonView(LanguageFileView.class)
    @Value.Derived
    default String dotCMSObjectType() {
        return TYPE;
    }

    @JsonView({LanguageReadView.class, LanguageWriteView.class})
    Optional<Long> id();

    @JsonView({LanguageReadView.class, LanguageWriteView.class})
    Optional<String> languageCode();

    @JsonView({LanguageReadView.class, LanguageWriteView.class})
    Optional<String> countryCode();

    @JsonView({LanguageReadView.class, LanguageWriteView.class})
    Optional<String> language();

    @JsonView({LanguageReadView.class, LanguageWriteView.class})
    Optional<String> country();

    @JsonView(LanguageReadView.class)
    Optional<Boolean> defaultLanguage();

    @JsonView({LanguageFileView.class, LanguageReadView.class, LanguageWriteView.class})
    String isoCode();

}
