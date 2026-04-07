package com.dotcms.rest.api.v1.maintenance;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Immutable view representing the status of the clean orphan assets background process.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = CleanAssetsStatusView.class)
@JsonDeserialize(as = CleanAssetsStatusView.class)
@Schema(description = "Status of the clean orphan assets background process")
public interface AbstractCleanAssetsStatusView {

    @Schema(
            description = "Total number of asset directories to process",
            example = "45000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int totalFiles();

    @Schema(
            description = "Number of directories processed so far",
            example = "12500",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int currentFiles();

    @Schema(
            description = "Number of orphan directories deleted",
            example = "83",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int deleted();

    @Schema(
            description = "Whether the process is currently running",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean running();

    @Schema(
            description = "Current status description: Starting, Counting, Cleaning, Finished, or Error message",
            example = "Cleaning",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String status();
}
