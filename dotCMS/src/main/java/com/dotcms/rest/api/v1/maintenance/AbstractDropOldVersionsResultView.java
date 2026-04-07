package com.dotcms.rest.api.v1.maintenance;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Immutable view representing the result of dropping old asset versions.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = DropOldVersionsResultView.class)
@JsonDeserialize(as = DropOldVersionsResultView.class)
@Schema(description = "Result of dropping old asset versions older than a specified date")
public interface AbstractDropOldVersionsResultView {

    @Schema(
            description = "Number of asset versions deleted. Returns -1 if an error occurred.",
            example = "1523",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int deletedCount();

    @Schema(
            description = "Whether the operation completed successfully (deletedCount >= 0)",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean success();
}
