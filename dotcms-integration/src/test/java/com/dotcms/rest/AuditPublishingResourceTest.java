package com.dotcms.rest;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Verifies that {@link AuditPublishingResource} enforces push-publish token
 * authentication on its endpoints. Both methods were previously reachable
 * anonymously; they now require a valid PP auth token.
 */
public class AuditPublishingResourceTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link AuditPublishingResource#get(String, HttpServletRequest)}
     * When: called with a request that carries no push-publish auth token
     * Should: return 401 Unauthorized.
     */
    @Test
    public void test_get_rejectsAnonymousRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = new AuditPublishingResource().get("any-bundle-id", request);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    /**
     * Method to test: {@link AuditPublishingResource#getAll(java.util.List, HttpServletRequest)}
     * When: called with a request that carries no push-publish auth token
     * Should: return 401 Unauthorized.
     */
    @Test
    public void test_getAll_rejectsAnonymousRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = new AuditPublishingResource()
                .getAll(Collections.singletonList("any-bundle-id"), request);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
}