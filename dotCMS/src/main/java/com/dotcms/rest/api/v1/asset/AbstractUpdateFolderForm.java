package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = UpdateFolderForm.class)
@JsonDeserialize(as = UpdateFolderForm.class)
@Schema(description = "Folder update form with asset path and update details including optional rename")
public interface AbstractUpdateFolderForm extends AbstractFolderForm <UpdateFolderDetail> {

}
