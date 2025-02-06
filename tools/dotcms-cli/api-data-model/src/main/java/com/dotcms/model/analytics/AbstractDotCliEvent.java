package com.dotcms.model.analytics;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = DotCliEvent.class)
public interface AbstractDotCliEvent {
    @JsonProperty("event_type")
    String eventType();
    @JsonProperty("event_source")
    String eventSource();
    String command();
    String user();
    String site();
}