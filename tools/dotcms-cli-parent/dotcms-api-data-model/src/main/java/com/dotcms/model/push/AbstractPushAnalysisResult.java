package com.dotcms.model.push;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.File;
import java.util.Optional;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = PushAnalysisResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractPushAnalysisResult<T> {

    PushAction action();

    Optional<T> serverContent();

    Optional<File> localFile();

}
