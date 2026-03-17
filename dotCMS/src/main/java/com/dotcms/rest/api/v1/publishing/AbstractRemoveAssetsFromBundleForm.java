package com.dotcms.rest.api.v1.publishing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Form for removing assets from a publishing bundle.
 * Contains the list of asset identifiers to remove.
 *
 * @author hassandotcms
 * @since Mar 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RemoveAssetsFromBundleForm.class)
@JsonDeserialize(as = RemoveAssetsFromBundleForm.class)
@Schema(description = "Request body for removing assets from a bundle")
public interface AbstractRemoveAssetsFromBundleForm {

    /**
     * List of asset identifiers to remove from the bundle.
     *
     * @return List of asset IDs
     */
    @Schema(
            description = "List of asset identifiers to remove from the bundle",
            example = "[\"asset-123\", \"asset-456\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> assetIds();

}
