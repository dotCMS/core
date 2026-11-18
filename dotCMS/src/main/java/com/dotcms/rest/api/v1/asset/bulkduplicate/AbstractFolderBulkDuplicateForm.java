package com.dotcms.rest.api.v1.asset.bulkduplicate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.immutables.value.Value;

/**
 * What {@code POST /v1/assets/folders/_bulkduplicate} accepts: the selected folders, and nothing
 * else (#37062, contract §1).
 * <p>
 * No destination and no options: every folder is duplicated in place, beside its original, under a
 * name the server derives. Shaped like {@code AbstractFolderBulkDeleteForm}, with no
 * self-validation; the empty-selection and over-the-maximum refusals need the configured ceiling
 * and a specific {@code errorCode}, so they live in {@link FolderBulkDuplicateHelper}.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = FolderBulkDuplicateForm.class)
@JsonDeserialize(as = FolderBulkDuplicateForm.class)
@Schema(description = "Request form for duplicating several folders in place in one background run")
public interface AbstractFolderBulkDuplicateForm {

    /**
     * The selected folders, as site-qualified paths.
     *
     * @return the folders to duplicate, in the order they were selected
     */
    @Schema(
        description = "The selected folders, as site-qualified paths. Each is duplicated beside "
                + "its original, in the same parent",
        example = "[\"//demo.dotcms.com/blogs/alpha/\"]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> assetPaths();
}
