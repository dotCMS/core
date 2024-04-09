package com.dotcms.model.language;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.views.CommonViews.LanguageExternalView;
import com.dotcms.model.views.CommonViews.LanguageFileView;
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

    @JsonView(LanguageExternalView.class)
    Optional<Long> id();

    @JsonView(LanguageExternalView.class)
    Optional<String> languageCode();

    @JsonView(LanguageExternalView.class)
    Optional<String> countryCode();

    @JsonView({LanguageFileView.class, LanguageExternalView.class})
    Optional<String> language();

    @JsonView(LanguageExternalView.class)
    Optional<String> country();

    @JsonView(LanguageFileView.class)
    Optional<Boolean> defaultLanguage();

    @JsonView({LanguageFileView.class, LanguageExternalView.class})
    String isoCode();

}
