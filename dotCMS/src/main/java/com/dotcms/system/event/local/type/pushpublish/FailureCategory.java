package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishAuditStatus;

/**
 * High-level classification of a push-publishing endpoint failure.
 *
 * <p>Subscribers of {@link AllPushPublishEndpointsFailureEvent} and
 * {@link SinglePushPublishEndpointFailureEvent} can switch on this category to decide
 * how to react to a failure (alert, retry, request manual intervention, etc.) without
 * having to inspect HTTP status codes or audit-status integers.</p>
 *
 * <p>The {@link #isRetryable()} flag captures whether the underlying failure is the
 * kind that may auto-resolve on a subsequent attempt (transient network or server
 * errors) versus one that requires human intervention (auth, license, or bundle
 * problems).</p>
 */
public enum FailureCategory {

    /** HTTP 401 or missing/invalid auth key on the receiving endpoint. */
    AUTHENTICATION(false),

    /** HTTP 403 — typically a license issue on the receiving endpoint. */
    AUTHORIZATION(false),

    /** HTTP 4xx other than 401/403. */
    CLIENT_ERROR(false),

    /** HTTP 5xx — receiver-side issues that may auto-resolve on retry. */
    SERVER_ERROR(true),

    /** Connection timeout, DNS failure, endpoint unreachable. */
    NETWORK_ERROR(true),

    /** Bundle could not be built before sending. */
    BUNDLE_ERROR(false),

    /** Failure could not be classified. */
    UNKNOWN(false);

    private final boolean retryable;

    FailureCategory(final boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Maps a failure context to its category.
     *
     * @param httpStatusCode the HTTP status the receiver returned, or {@code null} when
     *                       no response was received
     * @param auditStatus    the {@link PublishAuditStatus.Status} the publisher recorded
     *                       for this attempt, or {@code null} when not yet known
     * @param throwable      the exception that prevented contacting the endpoint, or
     *                       {@code null} when the failure came from an HTTP response
     * @return the matching category; never {@code null}
     */
    public static FailureCategory from(final Integer httpStatusCode,
                                       final PublishAuditStatus.Status auditStatus,
                                       final Throwable throwable) {
        if (auditStatus == PublishAuditStatus.Status.INVALID_TOKEN) {
            return AUTHENTICATION;
        }
        if (auditStatus == PublishAuditStatus.Status.LICENSE_REQUIRED) {
            return AUTHORIZATION;
        }
        if (auditStatus == PublishAuditStatus.Status.FAILED_TO_BUNDLE) {
            return BUNDLE_ERROR;
        }
        if (httpStatusCode != null && httpStatusCode > 0) {
            if (httpStatusCode == 401) {
                return AUTHENTICATION;
            }
            if (httpStatusCode == 403) {
                return AUTHORIZATION;
            }
            if (httpStatusCode >= 500) {
                return SERVER_ERROR;
            }
            if (httpStatusCode >= 400) {
                return CLIENT_ERROR;
            }
        }
        if (throwable != null) {
            return NETWORK_ERROR;
        }
        return UNKNOWN;
    }
}
