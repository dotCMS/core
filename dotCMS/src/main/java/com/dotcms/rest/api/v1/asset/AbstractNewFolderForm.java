package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = NewFolderForm.class)
@JsonDeserialize(as = NewFolderForm.class)
@Schema(description = "New folder creation form with asset path and folder configuration details")
public interface AbstractNewFolderForm extends AbstractFolderForm <FolderDetail>{

}
