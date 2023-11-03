package com.dotcms.model.pull;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = PullOptions.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractPullOptions {

    Optional<String> contentKey();

    String outputFormat();

    boolean isShortOutput();

}
