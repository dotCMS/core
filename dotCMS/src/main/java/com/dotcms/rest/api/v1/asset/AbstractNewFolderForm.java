package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = NewFolderForm.class)
@JsonDeserialize(as = NewFolderForm.class)
public interface AbstractNewFolderForm extends AbstractFolderForm <FolderDetail>{

}
