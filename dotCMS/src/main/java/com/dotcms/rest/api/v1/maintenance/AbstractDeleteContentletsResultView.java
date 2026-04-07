package com.dotcms.rest.api.v1.maintenance;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Immutable view representing the result of a bulk contentlet deletion.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = DeleteContentletsResultView.class)
@JsonDeserialize(as = DeleteContentletsResultView.class)
@Schema(description = "Result of bulk contentlet deletion with count and error details")
public interface AbstractDeleteContentletsResultView {

    @Schema(
            description = "Number of contentlets successfully destroyed",
            example = "6",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int deleted();

    @Schema(
            description = "Identifiers of contentlets that failed to be destroyed",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> errors();
}
