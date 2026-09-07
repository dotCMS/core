package com.dotcms.rest.api.v1.a11yagent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Request body for {@code POST /api/v1/agents/a11y/stop}.
 *
 * <p>Carries the {@code runId} the client received from {@code /fix} (the report) or
 * {@code /fix/stream} (the {@code run} event). Stop is addressed by this id, NOT by the
 * caller's identity: the proxy mints a fresh short-lived token per request, so the JWT
 * {@code sub} differs between the /fix and /stop calls — the runId is the stable handle
 * the agent uses to find the in-flight run.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*",
        additionalJsonAnnotations = JsonIgnoreProperties.class)
@Value.Immutable
@JsonSerialize(as = A11yAgentStopForm.class)
@JsonDeserialize(as = A11yAgentStopForm.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request body for stopping an in-flight a11y-fix agent run")
public interface AbstractA11yAgentStopForm {

    /**
     * Id of the run to stop. Declared nullable so a missing value surfaces as the resource's
     * {@code MISSING_RUN_ID} 400 rather than a deserialization failure.
     *
     * @return the run id returned by /fix or /fix/stream
     */
    @Nullable
    @Schema(
            description = "Run id returned by /fix (report) or /fix/stream (the `run` event)",
            example = "r_1f0c2b7d9a4e4c1fb0d5e6a7c8b9d0e1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String runId();
}
