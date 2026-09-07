package com.dotcms.rest.api.v1.vtl;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Error payload returned with an HTTP 400 by the {@code /api/vtl/dynamic} endpoints when the
 * submitted Velocity code fails to parse or evaluate. Mirrors the {@code {"errors": [...]}} shape
 * that VTL authors already produce via {@code $dotJSON.put("errors", ...)}, so consumers can handle
 * both application-level and engine-level errors uniformly.
 *
 * <p>{@code warnings} carries any non-fatal issues (undefined references, null method results)
 * observed before the fatal error — useful context when a typo cascades into a hard failure. It is
 * omitted when empty.</p>
 */
public class VelocityErrorResponseView {

    @Schema(description = "List of Velocity errors detected while evaluating the submitted code.")
    private final List<VelocityErrorView> errors;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "Non-fatal warnings (undefined references, null method results) observed before the "
            + "error. Omitted when there are none.")
    private final List<VelocityWarningView> warnings;

    public VelocityErrorResponseView(final List<VelocityErrorView> errors,
                                     final List<VelocityWarningView> warnings) {
        this.errors = errors;
        this.warnings = warnings;
    }

    public List<VelocityErrorView> getErrors() {
        return errors;
    }

    public List<VelocityWarningView> getWarnings() {
        return warnings;
    }
}
