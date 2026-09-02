package com.dotcms.rest.api.v1.maintenance;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Immutable view representing the result of a database-wide search and replace operation.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = SearchAndReplaceResultView.class)
@JsonDeserialize(as = SearchAndReplaceResultView.class)
@Schema(description = "Result of a database-wide search and replace operation")
public interface AbstractSearchAndReplaceResultView {

    @Schema(
            description = "Whether the operation completed without errors",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean success();

    @Schema(
            description = "Whether any table updates encountered errors. "
                    + "When true, some tables may not have been updated.",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean hasErrors();
}
