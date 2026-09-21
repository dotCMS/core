package com.dotcms.rest.api.v1.asset.bulkdelete;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.immutables.value.Value;

/**
 * What {@code POST /v1/assets/folders/_bulkdelete} accepts — the selected folders, and nothing
 * else (#37063, contracts §1, Key Entities: "carries nothing else — no destination, no options").
 * <p>
 * An ordinary JSON body, not multipart (D-004): this operation sends paths, not content. Shaped as
 * an Immutables interface like the shipped single-folder delete's own
 * {@link com.dotcms.rest.api.v1.asset.AbstractFolderDeletionRequestForm}, rather than a
 * Bean-Validation-annotated class — this form has no self-validation to express; the
 * empty-selection and over-the-maximum refusals need the configured ceiling and a specific
 * {@code errorCode}, so they live in {@link FolderBulkDeleteHelper}.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = FolderBulkDeleteForm.class)
@JsonDeserialize(as = FolderBulkDeleteForm.class)
@Schema(description = "Request form for deleting several folders in one background run")
public interface AbstractFolderBulkDeleteForm {

    /** The selected folders, in the same site-qualified path form the single delete accepts. */
    @Schema(
        description = "The selected folders, in the same site-qualified path form the "
                + "single-folder delete accepts",
        example = "[\"//demo.dotcms.com/old-projects/\"]"
    )
    List<String> assetPaths();
}
