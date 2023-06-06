package com.dotcms.rest.api.v1.asset;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = FileUploadParamsJson.Builder.class)
public interface AbstractFileUploadParamsJson {

    String id();

}
