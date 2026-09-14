package com.dotcms.inference.rest.view;

import com.dotcms.inference.model.InferenceError;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A refusal, in the standard error shape clients already parse.
 *
 * <p>Deliberately not wrapped in the dotCMS {@code ResponseEntityView} envelope. Byte
 * compatibility with the external standard is the whole point of this endpoint family, and a
 * dotCMS wrapper would stop the payload deserializing into the error types a client library
 * already has.</p>
 *
 * <p>Note what is absent: there is no retryable field. The standard error shape has none, and
 * inventing one would break that same compatibility — retryability is carried by the HTTP status,
 * which is what a standard client's back-off actually keys off.</p>
 */
@Schema(description = "Error response in the OpenAI-compatible error shape")
public record InferenceErrorView(@JsonProperty("error") @Schema(description = "The error") Body error) {

    /**
     * The error itself.
     *
     * @param message a safe description; never the upstream provider's raw envelope
     * @param type    the error family, e.g. {@code invalid_request_error}
     * @param param   the offending request field, when one can be named
     * @param code    a machine-readable code; null unless the provider supplied one
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Error detail")
    public record Body(
            @JsonProperty("message") @Schema(description = "Human-readable description of the failure",
                    example = "The model 'gpt-4o' is not configured for this site") String message,
            @JsonProperty("type") @Schema(description = "Error family",
                    example = "invalid_request_error") String type,
            @JsonProperty("param") @Schema(description = "Offending request field, when applicable",
                    example = "model") String param,
            @JsonProperty("code") @Schema(description = "Machine-readable code, when the provider supplies one")
            String code) {
    }

    /**
     * @param error the internal error
     * @return the same error in the wire shape, without its HTTP status, which the response carries
     */
    public static InferenceErrorView of(final InferenceError error) {
        return new InferenceErrorView(
                new Body(error.message(), error.type(), error.param(), null));
    }
}
