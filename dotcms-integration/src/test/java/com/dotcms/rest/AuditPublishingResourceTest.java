package com.dotcms.rest;

import com.dotcms.IntegrationTestBase;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.mockito.Mockito.mock;

/**
 * Verifies that {@link AuditPublishingResource} enforces backend-user
 * authentication on its endpoints. Both methods previously skipped
 * {@link WebResource#init} entirely and were reachable anonymously.
 */
public class AuditPublishingResourceTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link AuditPublishingResource#get(String, HttpServletRequest, HttpServletResponse)}
     * When: called with a request that carries no authenticated user
     * Should: reject the call with a SecurityException. The resource is now
     *         backend-only and `rejectWhenNoUser(true)` is configured.
     */
    @Test(expected = SecurityException.class)
    public void test_get_rejectsAnonymousRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        new AuditPublishingResource().get("any-bundle-id", request, response);
    }

    /**
     * Method to test: {@link AuditPublishingResource#getAll(java.util.List, HttpServletRequest, HttpServletResponse)}
     * When: called with a request that carries no authenticated user
     * Should: reject the call with a SecurityException.
     */
    @Test(expected = SecurityException.class)
    public void test_getAll_rejectsAnonymousRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        new AuditPublishingResource().getAll(Collections.singletonList("any-bundle-id"),
                request, response);
    }
}
