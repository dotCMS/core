package com.dotcms.system.event.local.type.pushpublish;

import com.dotcms.publisher.business.PublishAuditStatus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EndpointFailureDetailTest {

    @Test
    public void builder_setsAllFields() {
        final EndpointFailureDetail detail = EndpointFailureDetail.builder()
                .endpointId("endpoint-1")
                .endpointName("server-a")
                .address("server-a.example.com:8080")
                .environmentId("env-1")
                .environmentName("Staging")
                .failureCategory(FailureCategory.SERVER_ERROR)
                .auditStatus(PublishAuditStatus.Status.FAILED_TO_SENT)
                .httpStatusCode(503)
                .message("Service unavailable")
                .exceptionClass("java.io.IOException")
                .build();

        assertEquals("endpoint-1", detail.getEndpointId());
        assertEquals("server-a", detail.getEndpointName());
        assertEquals("server-a.example.com:8080", detail.getAddress());
        assertEquals("env-1", detail.getEnvironmentId());
        assertEquals("Staging", detail.getEnvironmentName());
        assertEquals(FailureCategory.SERVER_ERROR, detail.getFailureCategory());
        assertEquals(PublishAuditStatus.Status.FAILED_TO_SENT, detail.getAuditStatus());
        assertEquals(Integer.valueOf(503), detail.getHttpStatusCode());
        assertEquals("Service unavailable", detail.getMessage());
        assertEquals("java.io.IOException", detail.getExceptionClass());
    }

    @Test
    public void retryable_defaultsToCategoryRetryable_whenNotExplicitlySet() {
        final EndpointFailureDetail serverError = EndpointFailureDetail.builder()
                .failureCategory(FailureCategory.SERVER_ERROR)
                .build();
        assertTrue(serverError.isRetryable());

        final EndpointFailureDetail authError = EndpointFailureDetail.builder()
                .failureCategory(FailureCategory.AUTHENTICATION)
                .build();
        assertFalse(authError.isRetryable());
    }

    @Test
    public void retryable_overrideTakesPrecedenceOverCategoryDefault() {
        final EndpointFailureDetail forced = EndpointFailureDetail.builder()
                .failureCategory(FailureCategory.AUTHENTICATION)
                .retryable(true)
                .build();
        assertTrue(forced.isRetryable());
    }

    @Test
    public void missingFailureCategory_defaultsToUnknown() {
        final EndpointFailureDetail detail = EndpointFailureDetail.builder().build();
        assertEquals(FailureCategory.UNKNOWN, detail.getFailureCategory());
        assertFalse(detail.isRetryable());
        assertNull(detail.getHttpStatusCode());
        assertNull(detail.getAuditStatus());
        assertNull(detail.getExceptionClass());
    }
}
