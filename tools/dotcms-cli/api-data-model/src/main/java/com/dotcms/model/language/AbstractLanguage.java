package com.dotcms.model.language;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Language.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractLanguage {

    Optional<Long> id();

    String languageCode();

    String countryCode();

    String language();

    String country();
}
