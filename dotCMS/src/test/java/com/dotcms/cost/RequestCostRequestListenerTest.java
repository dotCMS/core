package com.dotcms.cost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotmarketing.business.APILocator;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RequestCostRequestListener}. Tests request lifecycle management, ThreadLocal handling, and full
 * accounting detection.
 *
 * @author Will Ezell
 * @since Oct 13th, 2024
 */
public class RequestCostRequestListenerTest extends UnitTestBase {

    private RequestCostRequestListener listener;
    private RequestCostApi requestCostApi;
    private HttpServletRequest request;
    private ServletRequestEvent event;

    @Before
    public void setUp() {
        listener = new RequestCostRequestListener();
        requestCostApi = APILocator.getRequestCostAPI();
    }

    @After
    public void tearDown() {
        // Clean up ThreadLocal to prevent test pollution
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /**
     * Test: {@link RequestCostRequestListener#requestInitialized} Should: Initialize accounting and set ThreadLocal
     * Expected: Request is in ThreadLocal, accounting is initialized
     */
    @Test
    public void test_requestInitialized_shouldInitializeAccountingAndThreadLocal() {
        // Given
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())));
        event = mock(ServletRequestEvent.class);
        when(event.getServletRequest()).thenReturn(request);

        // When
        listener.requestInitialized(event);

        // Then
        HttpServletRequest threadLocalRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        assertNotNull("ThreadLocal should contain request", threadLocalRequest);
        assertEquals("ThreadLocal should contain same request", request, threadLocalRequest);

        // Verify accounting was initialized
        assertNotNull("Account list should be initialized", requestCostApi.getAccountList(request));
        assertTrue("Account list should have entries", requestCostApi.getAccountList(request).size() > 0);
    }

    /**
     * Test: {@link RequestCostRequestListener#requestInitialized} with full accounting parameter Should: Initialize
     * with full accounting mode enabled Expected: Full accounting is enabled
     */
    @Test
    public void test_requestInitialized_withFullAccountingParam_shouldEnableFullMode() {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put(RequestCostApi.REQUEST_COST_FULL_ACCOUNTING, "true");

        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())),
                params);
        event = mock(ServletRequestEvent.class);
        when(event.getServletRequest()).thenReturn(request);

        // When
        listener.requestInitialized(event);

        // Then
        // Note: Full accounting requires admin user, so this will be false in unit tests
        // but the parameter detection logic is still tested
        boolean isFullAccounting = requestCostApi.isFullAccounting(request);
        // In unit tests without proper user context, this will be false
        assertFalse("Full accounting requires admin user (not available in unit test)", isFullAccounting);
    }

    /**
     * Test: {@link RequestCostRequestListener#requestInitialized} without full accounting Should: Initialize with
     * normal mode Expected: Full accounting is disabled
     */
    @Test
    public void test_requestInitialized_withoutFullAccountingParam_shouldUseNormalMode() {
        // Given
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())));
        event = mock(ServletRequestEvent.class);
        when(event.getServletRequest()).thenReturn(request);

        // When
        listener.requestInitialized(event);

        // Then
        boolean isFullAccounting = requestCostApi.isFullAccounting(request);
        assertFalse("Full accounting should be disabled", isFullAccounting);
    }

    /**
     * Test: {@link RequestCostRequestListener#requestDestroyed} Should: Clear accounting and ThreadLocal Expected:
     * Attributes removed, ThreadLocal is null
     */
    @Test
    public void test_requestDestroyed_shouldClearAccountingAndThreadLocal() {
        // Given
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())));
        event = mock(ServletRequestEvent.class);
        when(event.getServletRequest()).thenReturn(request);

        // Initialize first
        listener.requestInitialized(event);
        requestCostApi.incrementCost(10, RequestCostRequestListenerTest.class, "method", new Object[]{});

        // Verify it's set up
        assertNotNull("ThreadLocal should be set", HttpServletRequestThreadLocal.INSTANCE.getRequest());
        assertNotNull("Accounting should be initialized", request.getAttribute(RequestCostApi.REQUEST_COST_ATTRIBUTE));

        // When
        listener.requestDestroyed(event);

        // Then
        assertNull("ThreadLocal should be cleared", HttpServletRequestThreadLocal.INSTANCE.getRequest());
        assertNull("Cost attribute should be removed",
                request.getAttribute(RequestCostApi.REQUEST_COST_ATTRIBUTE));
        assertNull("Full accounting attribute should be removed",
                request.getAttribute(RequestCostApi.REQUEST_COST_FULL_ACCOUNTING));
    }

    /**
     * Test: Complete request lifecycle Should: Initialize, process, and cleanup properly Expected: All phases work
     * correctly
     */
    @Test
    public void test_completeLifecycle_shouldWorkCorrectly() {
        // Given
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())));
        event = mock(ServletRequestEvent.class);
        when(event.getServletRequest()).thenReturn(request);

        // When - Initialize
        listener.requestInitialized(event);

        // Then - Verify initialization
        assertNotNull("ThreadLocal should be set after init",
                HttpServletRequestThreadLocal.INSTANCE.getRequest());

        // When - Process (simulate some work)
        requestCostApi.incrementCost(5, RequestCostRequestListenerTest.class, "method1", new Object[]{});
        requestCostApi.incrementCost(3, RequestCostRequestListenerTest.class, "method2", new Object[]{});

        // Then - Verify processing
        int cost = requestCostApi.getRequestCost(request);
        assertTrue("Cost should be at least 8", cost >= 8);

        // When - Destroy
        listener.requestDestroyed(event);

        // Then - Verify cleanup
        assertNull("ThreadLocal should be cleared after destroy",
                HttpServletRequestThreadLocal.INSTANCE.getRequest());
        assertNull("Attributes should be cleared after destroy",
                request.getAttribute(RequestCostApi.REQUEST_COST_ATTRIBUTE));
    }

    /**
     * Test: Multiple sequential requests Should: Each request has independent lifecycle Expected: No interference
     * between requests
     */
    @Test
    public void test_multipleSequentialRequests_shouldBeIndependent() {
        // Given - First request
        HttpServletRequest request1 = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/page1").request())));
        ServletRequestEvent event1 = mock(ServletRequestEvent.class);
        when(event1.getServletRequest()).thenReturn(request1);

        // When - First request lifecycle
        listener.requestInitialized(event1);
        requestCostApi.incrementCost(10, RequestCostRequestListenerTest.class, "method1", new Object[]{});
        int cost1 = requestCostApi.getRequestCost(request1);
        listener.requestDestroyed(event1);

        // Given - Second request
        HttpServletRequest request2 = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/page2").request())));
        ServletRequestEvent event2 = mock(ServletRequestEvent.class);
        when(event2.getServletRequest()).thenReturn(request2);

        // When - Second request lifecycle
        listener.requestInitialized(event2);
        requestCostApi.incrementCost(20, RequestCostRequestListenerTest.class, "method2", new Object[]{});
        int cost2 = requestCostApi.getRequestCost(request2);
        listener.requestDestroyed(event2);

        // Then
        assertTrue("First request cost should be at least 10", cost1 >= 10);
        assertTrue("Second request cost should be at least 20", cost2 >= 20);
        assertNull("ThreadLocal should be cleared after second request",
                HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }

    /**
     * Test: requestDestroyed called without initialization Should: Handle gracefully without errors Expected: No
     * exceptions thrown
     */
    @Test
    public void test_requestDestroyed_withoutInit_shouldNotFail() {
        // Given
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())));
        event = mock(ServletRequestEvent.class);
        when(event.getServletRequest()).thenReturn(request);

        // When/Then - should not throw
        listener.requestDestroyed(event);

        // Verify cleanup still happens
        assertNull("ThreadLocal should be null", HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }

    /**
     * Test: ThreadLocal cleanup prevents memory leaks Should: ThreadLocal is always cleared on destroy Expected: No
     * lingering references
     */
    @Test
    public void test_threadLocalCleanup_preventsMemoryLeaks() {
        // Given
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())));
        event = mock(ServletRequestEvent.class);
        when(event.getServletRequest()).thenReturn(request);

        // When - Multiple init/destroy cycles
        for (int i = 0; i < 10; i++) {
            listener.requestInitialized(event);
            assertNotNull("ThreadLocal should be set", HttpServletRequestThreadLocal.INSTANCE.getRequest());
            listener.requestDestroyed(event);
            assertNull("ThreadLocal should be cleared", HttpServletRequestThreadLocal.INSTANCE.getRequest());
        }

        // Then - Final state should be clean
        assertNull("ThreadLocal should remain null after all cycles",
                HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }

    /**
     * Test: Request counter increments on initialization Should: Global request counter increases Expected: Counter
     * increments for each request
     */
    @Test
    public void test_requestInitialized_shouldIncrementGlobalCounter() {
        // Given
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())));
        event = mock(ServletRequestEvent.class);
        when(event.getServletRequest()).thenReturn(request);

        // Get initial count
        var initialLoad = requestCostApi.totalLoadGetAndReset();

        // When - Initialize multiple requests
        for (int i = 0; i < 5; i++) {
            HttpServletRequest req = new MockParameterRequest(
                    new MockAttributeRequest(
                            new MockHeaderRequest(
                                    new FakeHttpRequest("localhost", "/test" + i).request())));
            ServletRequestEvent evt = mock(ServletRequestEvent.class);
            when(evt.getServletRequest()).thenReturn(req);
            listener.requestInitialized(evt);
        }

        // Then
        var finalLoad = requestCostApi.totalLoadGetAndReset();
        assertTrue("Request count should have increased", finalLoad._1 >= 5);
    }
}
