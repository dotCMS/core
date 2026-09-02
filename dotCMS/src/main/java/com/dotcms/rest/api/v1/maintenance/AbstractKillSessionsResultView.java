package com.dotcms.rest.api.v1.maintenance;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Immutable view representing the result of bulk session invalidation.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = KillSessionsResultView.class)
@JsonDeserialize(as = KillSessionsResultView.class)
@Schema(description = "Result of invalidating all sessions except the caller's")
public interface AbstractKillSessionsResultView {

    @Schema(
            description = "Number of sessions invalidated (the caller's own session is always skipped).",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int killedCount();
}
