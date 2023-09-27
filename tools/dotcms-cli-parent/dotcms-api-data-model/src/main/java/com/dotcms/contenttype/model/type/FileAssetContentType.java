package com.dotcms.contenttype.model.type;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableFileAssetContentType.class)
@JsonDeserialize(as = ImmutableFileAssetContentType.class)
@Value.Immutable
public abstract class FileAssetContentType extends ContentType {

}
