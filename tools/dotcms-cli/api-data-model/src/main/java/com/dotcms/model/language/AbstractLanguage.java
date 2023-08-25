package com.dotcms.model.language;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.views.CommonViews;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Language.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractLanguage {

    String TYPE = "Language";

    @JsonView(CommonViews.InternalView.class)
    @Value.Derived
    default String dotCMSObjectType() {
        return TYPE;
    }

    Optional<Long> id();

    Optional<String> languageCode();

    Optional<String> countryCode();

    Optional<String> language();

    Optional<String> country();

    @JsonView(CommonViews.InternalView.class)
    Optional<Boolean> defaultLanguage();

    String isoCode();

}
