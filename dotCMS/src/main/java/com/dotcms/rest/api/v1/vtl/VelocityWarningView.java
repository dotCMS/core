package com.dotcms.rest.api.v1.vtl;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A non-fatal Velocity warning surfaced by the {@code /api/vtl/dynamic} endpoints.
 *
 * <p>Because dotCMS evaluates Velocity in non-strict mode (an undefined reference renders as its
 * literal text and a method call that returns {@code null} produces no output), typos such as
 * {@code $noSuchVar} or {@code $obj.noSuchMethod()} would otherwise fail silently. The dynamic
 * endpoints attach a per-evaluation handler that collects these as warnings so a caller (typically
 * an automated agent) can spot the mistake — the script still runs and its output is returned.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A non-fatal Velocity warning, such as an undefined reference or a method call that "
        + "returned null. The script still evaluated; warnings flag likely typos in non-strict mode.")
public class VelocityWarningView {

    @Schema(description = "The kind of warning.",
            allowableValues = {"UNDEFINED_REFERENCE", "NULL_METHOD_RESULT", "INVALID_METHOD", "NULL_SET"},
            example = "UNDEFINED_REFERENCE")
    private final String type;

    @Schema(description = "Human-readable description of the warning.",
            example = "Undefined reference '$noSuchVar' — renders as literal text in non-strict mode")
    private final String message;

    @Schema(description = "The reference or method expression that triggered the warning, when known.",
            example = "$noSuchVar")
    private final String reference;

    @Schema(description = "1-based line number where the reference appears, when Velocity reports it. "
            + "Omitted when unavailable.",
            example = "3")
    private final Integer line;

    @Schema(description = "1-based column number where the reference appears, when Velocity reports it. "
            + "Omitted when unavailable.",
            example = "1")
    private final Integer column;

    public VelocityWarningView(final String type, final String message, final String reference,
                               final Integer line, final Integer column) {
        this.type = type;
        this.message = message;
        this.reference = reference;
        this.line = line;
        this.column = column;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getReference() {
        return reference;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }
}
