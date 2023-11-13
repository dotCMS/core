package com.dotcms.analytics.metrics;

import com.dotcms.http.request.AbstractStringPayloadHttpRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = MetricsPayloadRequest.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractMetricsPayloadRequest extends AbstractStringPayloadHttpRequest {
}
