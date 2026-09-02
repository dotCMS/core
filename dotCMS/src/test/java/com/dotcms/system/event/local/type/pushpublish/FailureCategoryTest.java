package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishAuditStatus;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FailureCategoryTest {

    @Test
    public void invalidTokenAuditStatus_mapsToAuthentication() {
        assertEquals(FailureCategory.AUTHENTICATION,
                FailureCategory.from(null, PublishAuditStatus.Status.INVALID_TOKEN, null));
    }

    @Test
    public void licenseRequiredAuditStatus_mapsToAuthorization() {
        assertEquals(FailureCategory.AUTHORIZATION,
                FailureCategory.from(null, PublishAuditStatus.Status.LICENSE_REQUIRED, null));
    }

    @Test
    public void failedToBundleAuditStatus_mapsToBundleError() {
        assertEquals(FailureCategory.BUNDLE_ERROR,
                FailureCategory.from(null, PublishAuditStatus.Status.FAILED_TO_BUNDLE, null));
    }

    @Test
    public void http401_mapsToAuthentication() {
        assertEquals(FailureCategory.AUTHENTICATION,
                FailureCategory.from(401, null, null));
    }

    @Test
    public void http403_mapsToAuthorization() {
        assertEquals(FailureCategory.AUTHORIZATION,
                FailureCategory.from(403, null, null));
    }

    @Test
    public void http404_mapsToClientError() {
        assertEquals(FailureCategory.CLIENT_ERROR,
                FailureCategory.from(404, null, null));
    }

    @Test
    public void http500_mapsToServerError() {
        assertEquals(FailureCategory.SERVER_ERROR,
                FailureCategory.from(500, null, null));
    }

    @Test
    public void http503_mapsToServerError() {
        assertEquals(FailureCategory.SERVER_ERROR,
                FailureCategory.from(503, null, null));
    }

    @Test
    public void exceptionWithoutHttpStatus_mapsToNetworkError() {
        assertEquals(FailureCategory.NETWORK_ERROR,
                FailureCategory.from(null, null, new SocketTimeoutException("connect timed out")));
    }

    @Test
    public void zeroHttpStatusWithIoException_mapsToNetworkError() {
        assertEquals(FailureCategory.NETWORK_ERROR,
                FailureCategory.from(0, null, new IOException("DNS failure")));
    }

    @Test
    public void noSignals_mapsToUnknown() {
        assertEquals(FailureCategory.UNKNOWN, FailureCategory.from(null, null, null));
    }

    @Test
    public void auditStatusTakesPrecedenceOverHttpStatus() {
        // Receiver returned 200 but the publisher decided the token was invalid.
        assertEquals(FailureCategory.AUTHENTICATION,
                FailureCategory.from(200, PublishAuditStatus.Status.INVALID_TOKEN, null));
    }

    @Test
    public void retryable_serverAndNetwork_arRetryable_others_areNot() {
        assertTrue(FailureCategory.SERVER_ERROR.isRetryable());
        assertTrue(FailureCategory.NETWORK_ERROR.isRetryable());
        assertFalse(FailureCategory.AUTHENTICATION.isRetryable());
        assertFalse(FailureCategory.AUTHORIZATION.isRetryable());
        assertFalse(FailureCategory.CLIENT_ERROR.isRetryable());
        assertFalse(FailureCategory.BUNDLE_ERROR.isRetryable());
        assertFalse(FailureCategory.UNKNOWN.isRetryable());
    }
}
