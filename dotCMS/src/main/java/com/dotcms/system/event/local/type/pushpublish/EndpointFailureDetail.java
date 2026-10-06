package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishAuditStatus;

import java.io.Serializable;

/**
 * Per-endpoint failure information attached to push-publishing failure events.
 *
 * <p>Carries everything a subscriber needs to decide what to do about a failed
 * delivery to one specific endpoint: which endpoint and environment, the high-level
 * {@link FailureCategory}, the original {@link PublishAuditStatus.Status} the
 * publisher recorded, the HTTP status code (when one was returned), the human
 * readable message, and a {@code retryable} hint.</p>
 *
 * <p>Instances are immutable and built via {@link #builder()}.</p>
 */
public final class EndpointFailureDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String endpointId;
    private final String endpointName;
    private final String address;
    private final String environmentId;
    private final String environmentName;
    private final FailureCategory failureCategory;
    private final PublishAuditStatus.Status auditStatus;
    private final Integer httpStatusCode;
    private final String message;
    private final boolean retryable;
    private final String exceptionClass;

    private EndpointFailureDetail(final Builder b) {
        this.endpointId = b.endpointId;
        this.endpointName = b.endpointName;
        this.address = b.address;
        this.environmentId = b.environmentId;
        this.environmentName = b.environmentName;
        this.failureCategory = b.failureCategory != null ? b.failureCategory : FailureCategory.UNKNOWN;
        this.auditStatus = b.auditStatus;
        this.httpStatusCode = b.httpStatusCode;
        this.message = b.message;
        this.retryable = b.retryable != null ? b.retryable : this.failureCategory.isRetryable();
        this.exceptionClass = b.exceptionClass;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    /** Host plus formatted port, or {@code null} when not known. */
    public String getAddress() {
        return address;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public FailureCategory getFailureCategory() {
        return failureCategory;
    }

    /** May be {@code null} when no audit status was set for this attempt. */
    public PublishAuditStatus.Status getAuditStatus() {
        return auditStatus;
    }

    /** May be {@code null} when no HTTP response was received (e.g. network errors). */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /** May be {@code null} when the failure did not stem from an exception. */
    public String getExceptionClass() {
        return exceptionClass;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String endpointId;
        private String endpointName;
        private String address;
        private String environmentId;
        private String environmentName;
        private FailureCategory failureCategory;
        private PublishAuditStatus.Status auditStatus;
        private Integer httpStatusCode;
        private String message;
        private Boolean retryable;
        private String exceptionClass;

        public Builder endpointId(final String v) {
            this.endpointId = v;
            return this;
        }

        public Builder endpointName(final String v) {
            this.endpointName = v;
            return this;
        }

        public Builder address(final String v) {
            this.address = v;
            return this;
        }

        public Builder environmentId(final String v) {
            this.environmentId = v;
            return this;
        }

        public Builder environmentName(final String v) {
            this.environmentName = v;
            return this;
        }

        public Builder failureCategory(final FailureCategory v) {
            this.failureCategory = v;
            return this;
        }

        public Builder auditStatus(final PublishAuditStatus.Status v) {
            this.auditStatus = v;
            return this;
        }

        public Builder httpStatusCode(final Integer v) {
            this.httpStatusCode = v;
            return this;
        }

        public Builder message(final String v) {
            this.message = v;
            return this;
        }

        public Builder retryable(final Boolean v) {
            this.retryable = v;
            return this;
        }

        public Builder exceptionClass(final String v) {
            this.exceptionClass = v;
            return this;
        }

        public EndpointFailureDetail build() {
            return new EndpointFailureDetail(this);
        }
    }
}
