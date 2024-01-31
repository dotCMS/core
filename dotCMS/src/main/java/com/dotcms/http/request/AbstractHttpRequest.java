package com.dotcms.http.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = HttpRequest.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractHttpRequest<P extends Serializable> {

    @JsonProperty("url")
    String url();

    @Nullable
    @JsonProperty("headers")
    Map<String, String> headers();

    @Nullable
    @JsonProperty("payload")
    P payload();

    @Nullable
    @JsonProperty("queryParams")
    Map<String, String> queryParams();

}
