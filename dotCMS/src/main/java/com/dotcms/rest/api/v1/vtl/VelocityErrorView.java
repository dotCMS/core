package com.dotcms.rest.api.v1.vtl;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Structured description of a single Velocity evaluation error surfaced by the
 * {@code /api/vtl/dynamic} endpoints. It is designed so an automated caller (for example, an AI
 * agent that generated the VTL) can locate and fix the offending code without having to parse a
 * free-form stack trace.
 *
 * <p>The {@code line} and {@code column} fields are only populated when the underlying Velocity
 * exception reports a position (parse and method-invocation errors do; resource-not-found errors
 * usually do not). A value of {@code 0} means "not available" and is omitted from the JSON.</p>
 *
 * <p>{@code message} is a concise, single-line summary; the full engine output (for parse errors,
 * the exhaustive "was expecting one of ..." token list) is preserved in {@code detail} for human
 * display.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A single Velocity evaluation error, structured so an automated caller can locate and fix "
        + "the offending code without parsing a stack trace.")
public class VelocityErrorView {

    @Schema(description = "Concise, single-line Velocity error message including the offending token and position "
            + "when available. See `detail` for the full engine output.",
            example = "Encountered \"<EOF>\" at line 6, column 39")
    private final String message;

    @Schema(description = "Simple class name of the underlying Velocity error, normalized to the public Velocity "
            + "type (e.g. ParseErrorException, MethodInvocationException, ResourceNotFoundException) rather than a "
            + "dotCMS-internal subclass. Lets the caller distinguish a syntax error from a runtime error.",
            example = "ParseErrorException")
    private final String errorType;

    @Schema(description = "Name Velocity associated with the evaluated template. For dynamic requests this is a "
            + "synthetic name identifying the submitted script.",
            example = "dynamic velocity")
    private final String templateName;

    @Schema(description = "1-based line number in the submitted velocity where the error occurred, when Velocity "
            + "reports it. Omitted when unavailable.",
            example = "6")
    private final Integer line;

    @Schema(description = "1-based column number in the submitted velocity where the error occurred, when Velocity "
            + "reports it. Omitted when unavailable.",
            example = "39")
    private final Integer column;

    @Schema(description = "Full, multi-line engine output for the error, including the complete grammar-token list "
            + "for parse errors. Intended for human display; omitted when it adds nothing beyond `message`.",
            example = "Encountered \"<EOF>\" at line 6, column 39\nWas expecting one of:\n    \"[\" ...\n    \"(\" ...")
    private final String detail;

    public VelocityErrorView(final String message, final String errorType, final String templateName,
                             final Integer line, final Integer column, final String detail) {
        this.message = message;
        this.errorType = errorType;
        this.templateName = templateName;
        this.line = line;
        this.column = column;
        this.detail = detail;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }

    public String getDetail() {
        return detail;
    }
}
