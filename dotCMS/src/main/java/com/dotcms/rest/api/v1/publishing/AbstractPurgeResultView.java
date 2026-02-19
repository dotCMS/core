package com.dotcms.rest.api.v1.publishing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Represents the acknowledgment response for a purge operation.
 * Contains the confirmation message and the list of statuses being purged.
 *
 * @author hassandotcms
 * @since Jan 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = PurgeResultView.class)
@JsonDeserialize(as = PurgeResultView.class)
@Schema(description = "Purge operation acknowledgment with requested statuses")
public interface AbstractPurgeResultView {

    /**
     * Confirmation message indicating the purge operation has started.
     *
     * @return Acknowledgment message
     */
    @Schema(
            description = "Confirmation message indicating the purge operation has started",
            example = "Purge operation started. Results will be notified when complete.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message();

    /**
     * List of status names that are being purged.
     *
     * @return List of status names requested for purge
     */
    @Schema(
            description = "List of status names being purged",
            example = "[\"SUCCESS\", \"FAILED_TO_PUBLISH\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> statusesRequested();
}
