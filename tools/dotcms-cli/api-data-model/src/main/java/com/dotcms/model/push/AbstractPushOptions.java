package com.dotcms.model.push;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = PushOptions.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractPushOptions {

    boolean allowRemove();

    boolean failFast();

    boolean dryRun();

    boolean disableAutoUpdate();

    int maxRetryAttempts();
}
