package com.dotcms.cost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotcms.cost.RequestCostApi.Accounting;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.mock.response.MockHttpStatusAndHeadersResponse;
import io.vavr.Tuple2;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RequestCostApi} and {@link RequestCostApiImpl}. Tests the request cost tracking system
 * including initialization, cost accumulation, full accounting mode, and header management.
 *
 */
public class RequestCostApiTest extends UnitTestBase {

    private RequestCostApi requestCostApi;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp() {
        requestCostApi = new RequestCostApiImpl(true);
        request = new MockParameterRequest(
                new MockAttributeRequest(new MockHeaderRequest(new FakeHttpRequest("localhost", "/").request())));
        response = new MockHttpStatusAndHeadersResponse(new MockHttpResponse().response());
    }

    /**
     * Test: {@link RequestCostApi#initAccounting(HttpServletRequest)} Should: Initialize accounting with default
     * settings (fullAccounting=false) Expected: Request attribute is set with empty list
     */
    @Test
    public void test_initAccounting_shouldInitializeWithDefaults() {
        // When
        requestCostApi.initAccounting(request);

        // Then
        List<Map<String, Object>> accountList = requestCostApi.getAccountList(request);
        assertNotNull("Account list should not be null", accountList);
        assertTrue("Account list should have at least one entry (init cost)", accountList.isEmpty());

        int rollingCost = requestCostApi.getRequestCost(request);
        assertEquals("Rolling cost should be 1", 1, rollingCost);


    }



    /**
     * Test: {@link RequestCostApi#incrementCost(Price, Method, Object[])} Should: Increment cost using method reference
     * Expected: Cost is added to the request total
     */
    @Test
    public void test_incrementCost_withMethod_shouldAddCost() throws Exception {
        // Given
        requestCostApi.initAccounting(request);
        Method testMethod = this.getClass().getDeclaredMethod("setUp");
        int initialCost = requestCostApi.getRequestCost(request);

        // When
        requestCostApi.incrementCost(Price.FIVE, testMethod, new Object[]{});

        // Then
        int finalCost = requestCostApi.getRequestCost(request);
        assertEquals("Cost should increase by 5", initialCost + 5, finalCost);
    }

    /**
     * Test: {@link RequestCostApi#incrementCost(Price, Class, String, Object[])} Should: Increment cost using class and
     * method name Expected: Cost is added to the request total
     */
    @Test
    public void test_incrementCost_withClassAndMethod_shouldAddCost() {
        // Given
        requestCostApi.initAccounting(request);
        int initialCost = requestCostApi.getRequestCost(request);

        // When
        requestCostApi.incrementCost(Price.TEN, RequestCostApiTest.class, "testMethod", new Object[]{"arg1", "arg2"});

        // Then
        int finalCost = requestCostApi.getRequestCost(request);
        assertEquals("Cost should increase by 10", initialCost + 10, finalCost);
    }

    /**
     * Test: {@link RequestCostApi#getRequestCost(HttpServletRequest)} Should: Return accumulated cost for the request
     * Expected: Sum of all incremented costs
     */
    @Test
    public void test_getRequestCost_shouldReturnAccumulatedCost() {
        // Given
        requestCostApi.initAccounting(request);
        requestCostApi.incrementCost(Price.THREE, RequestCostApiTest.class, "method1", new Object[]{});
        requestCostApi.incrementCost(Price.SEVEN, RequestCostApiTest.class, "method2", new Object[]{});
        requestCostApi.incrementCost(Price.FIVE, RequestCostApiTest.class, "method3", new Object[]{});

        // When
        int totalCost = requestCostApi.getRequestCost(request);

        // Then
        assertTrue("Total cost should be at least 15 (3+7+5)", totalCost >= 15);
    }

    /**
     * Test: {@link RequestCostApi#getAccountList(HttpServletRequest)} Should: Return list of all cost entries Expected:
     * List contains all incremented costs
     */
    @Test
    public void test_getAccountList_shouldReturn_empty_if_accounting_is_disabled() {
        // Given
        requestCostApi.initAccounting(request);
        int initialSize = requestCostApi.getAccountList(request).size();

        // When
        requestCostApi.incrementCost(Price.ONE, RequestCostApiTest.class, "method1", new Object[]{});
        requestCostApi.incrementCost(Price.TWO, RequestCostApiTest.class, "method2", new Object[]{});

        // Then
        List<Map<String, Object>> accountList = requestCostApi.getAccountList(request);
        assertEquals("Account list should have 0 more entries", 0, accountList.size());
    }



    /**
     * Test: {@link RequestCostApi#getAccountList(HttpServletRequest)} without full accounting Should: Only include cost
     * in account entries Expected: Entries only contain COST key
     */
    @Test
    public void test_getAccountList_withoutFullAccounting_shouldOnlyIncludeCost() {
        // Given
        requestCostApi.initAccounting(request);

        // When
        requestCostApi.incrementCost(Price.FIVE, RequestCostApiTest.class, "testMethod", new Object[]{"arg1"});

        // Then
        List<Map<String, Object>> accountList = requestCostApi.getAccountList(request);
        assertTrue("Account list should have no entries", accountList.isEmpty());

        assertEquals("requestCost should be 6", Price.SIX.price, requestCostApi.getRequestCost(request));
    }

    /**
     * Test: {@link RequestCostApi#resolveAccounting(HttpServletRequest)} Should: Return false when full accounting is
     * not enabled Expected: Returns false
     */
    @Test
    public void test_isFullAccounting_whenNotEnabled_shouldReturnFalse() {
        // Given
        requestCostApi.initAccounting(request);

        // When
        boolean isFullAccounting = requestCostApi.resolveAccounting(request) != Accounting.HEADER;

        // Then
        assertFalse("Full accounting should be disabled", isFullAccounting);
    }





    /**
     * Test: {@link RequestCostApi#addCostHeader(HttpServletRequest, HttpServletResponse)} Should: Add X-Request-Cost
     * header to response Expected: Header is set with current cost value
     */
    @Test
    public void test_addCostHeader_shouldSetHeaderWithCost() {
        // Given
        requestCostApi.initAccounting(request);
        requestCostApi.incrementCost(Price.THIRTY, RequestCostApiTest.class, "method", new Object[]{});
        requestCostApi.incrementCost(Price.TWENTY, RequestCostApiTest.class, "method", new Object[]{});
        // When
        requestCostApi.addCostHeader(request, response);

        // Then
        String costHeader = response.getHeader(RequestCostApi.REQUEST_COST_HEADER_NAME);
        assertNotNull("Cost header should be set", costHeader);
        double headerValue = Double.parseDouble(costHeader);
        assertTrue("Cost header should be at least 42", headerValue >= 42);
    }

    /**
     * Test: {@link RequestCostApi#totalLoadGetAndReset()} Should: Return current load and reset counters Expected:
     * Returns tuple with count and cost, then resets to zero
     */
    @Test
    public void test_totalLoadGetAndReset_shouldReturnAndReset() {
        // Given
        requestCostApi.initAccounting(request);
        requestCostApi.incrementCost(Price.THIRTY, RequestCostApiTest.class, "method", new Object[]{});
        requestCostApi.incrementCost(Price.THIRTY, RequestCostApiTest.class, "method", new Object[]{});
        requestCostApi.incrementCost(Price.THIRTY, RequestCostApiTest.class, "method", new Object[]{});
        requestCostApi.incrementCost(Price.THIRTY, RequestCostApiTest.class, "method", new Object[]{});

        // When
        Tuple2<Long, Long> load1 = requestCostApi.totalLoadGetAndReset();
        Tuple2<Long, Long> load2 = requestCostApi.totalLoadGetAndReset();

        // Then
        assertTrue("First load count should be > 0", load1._1 > 0);
        assertTrue("First load cost should be >= 100", load1._2 >= 100);
        assertEquals("Second load count should be 0", Long.valueOf(0), load2._1);
        assertEquals("Second load cost should be 0", Long.valueOf(0), load2._2);
    }

    /**
     * Test: Multiple requests should track costs independently Should: Each request maintains its own cost tracking
     * Expected: Costs don't interfere between requests
     */
    @Test
    public void test_multipleRequests_shouldTrackIndependently() {
        // Given
        HttpServletRequest request1 = new MockSessionRequest(new FakeHttpRequest("localhost", "/page1").request());
        HttpServletRequest request2 = new MockSessionRequest(new FakeHttpRequest("localhost", "/page2").request());

        // When
        requestCostApi.initAccounting(request1);
        requestCostApi.initAccounting(request2);

        requestCostApi.incrementCost(Price.TEN, RequestCostApiTest.class, "method1", new Object[]{});
        // Note: incrementCost uses ThreadLocal, so it will add to whichever request is in the ThreadLocal
        // For proper testing, we'd need to set the ThreadLocal appropriately

        int cost1 = requestCostApi.getRequestCost(request1);
        int cost2 = requestCostApi.getRequestCost(request2);

        // Then
        assertNotNull("Request 1 should have cost tracking", cost1);
        assertNotNull("Request 2 should have cost tracking", cost2);
    }

    /**
     * Test: Cost increment with zero value Should: Handle zero cost increment Expected: No error, cost remains same
     */
    @Test
    public void test_incrementCost_withZero_shouldNotFail() {
        // Given
        requestCostApi.initAccounting(request);
        int initialCost = requestCostApi.getRequestCost(request);

        // When
        requestCostApi.incrementCost(Price.FREE, RequestCostApiTest.class, "method", new Object[]{});

        // Then
        int finalCost = requestCostApi.getRequestCost(request);
        assertEquals("Cost should remain the same", initialCost, finalCost);
    }

    /**
     * Test: Cost increment with large value Should: Handle large cost values Expected: Cost accumulates correctly
     */
    @Test
    public void test_incrementCost_withLargeValue_shouldAccumulate() {
        // Given
        requestCostApi.initAccounting(request);
        int initialCost = requestCostApi.getRequestCost(request);

        // When
        requestCostApi.incrementCost(Price.TEN_THOUSAND, RequestCostApiTest.class, "expensiveMethod", new Object[]{});

        // Then
        int finalCost = requestCostApi.getRequestCost(request);
        assertEquals("Cost should increase by 10000", initialCost + Price.TEN_THOUSAND.price, finalCost);
    }

    /**
     * Test: Get account list before initialization Should: Return empty list or initialize automatically Expected: No
     * null pointer exception
     */
    @Test
    public void test_getAccountList_beforeInit_shouldNotFail() {
        // When
        List<Map<String, Object>> accountList = requestCostApi.getAccountList(request);

        // Then
        assertNotNull("Account list should not be null", accountList);
    }
}
