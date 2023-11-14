package com.dotcms.http.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = StringPayloadHttpRequest.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractStringPayloadHttpRequest extends AbstractHttpRequest<String> {
}
