package com.dotcms.rest.api.v1.a11yagent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Request body for {@code POST /api/v1/agent/a11y/stop}.
 *
 * <p>Carries the {@code runId} the client received from {@code /fix} (the report) or
 * {@code /fix/stream} (the {@code run} event). Stop is addressed by this id, NOT by the
 * caller's identity: the proxy mints a fresh short-lived token per request, so the JWT
 * {@code sub} differs between the /fix and /stop calls — the runId is the stable handle
 * the agent uses to find the in-flight run.
 */
@JsonDeserialize(builder = A11yAgentStopForm.Builder.class)
public class A11yAgentStopForm {

    private final String runId;

    private A11yAgentStopForm(final Builder builder) {
        this.runId = builder.runId;
    }

    public String getRunId() {
        return runId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder {

        @JsonProperty(value = "runId", required = true)
        private String runId;

        public Builder runId(final String runId) {
            this.runId = runId;
            return this;
        }

        public A11yAgentStopForm build() {
            return new A11yAgentStopForm(this);
        }
    }
}
