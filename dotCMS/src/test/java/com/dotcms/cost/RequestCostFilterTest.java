package com.dotcms.cost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dotcms.UnitTestBase;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.mock.response.MockHttpStatusAndHeadersResponse;
import java.util.Collection;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RequestCostFilter}. Tests the filter's behavior in normal and full accounting modes, response
 * wrapper functionality, and header injection.
 *
 */
public class RequestCostFilterTest extends UnitTestBase {

    private RequestCostFilter filter;
    private RequestCostApi requestCostApi;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @Before
    public void setUp() {
        requestCostApi = new RequestCostApiImpl(true);

        filter = new RequestCostFilter(requestCostApi);
        requestCostApi = new RequestCostApiImpl(true);
        request = new MockParameterRequest(
                new MockAttributeRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest("localhost", "/test").request())));
        response = new MockHttpStatusAndHeadersResponse(new MockHttpResponse().response());
        filterChain = mock(FilterChain.class);
    }

    /**
     * Test: {@link RequestCostFilter#doFilter} in normal mode Should: Add cost header and pass through filter chain
     * Expected: Header is added, chain continues, no report generated
     */
    @Test
    public void test_doFilter_normalMode_shouldAddHeaderAndContinue() throws Exception {
        // Given
        requestCostApi.initAccounting(request);
        requestCostApi.incrementCost(Price.TEN, RequestCostFilterTest.class, "testMethod", new Object[]{});

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(eq(request), any(HttpServletResponse.class));
        String costHeader = response.getHeader(RequestCostApi.REQUEST_COST_HEADER_NAME);
        assertNotNull("Cost header should be set", costHeader);
        assertTrue("Cost should be at least 10",
                Double.parseDouble(costHeader) >= 10 / requestCostApi.getRequestCostDenominator());
    }



    /**
     * Test: {@link RequestCostFilter#init} Should: Initialize without error Expected: No exceptions thrown
     */
    @Test
    public void test_init_shouldNotFail() {
        // When/Then - should not throw
        filter.init(null);
    }

    /**
     * Test: {@link RequestCostFilter#destroy} Should: Cleanup without error Expected: No exceptions thrown
     */
    @Test
    public void test_destroy_shouldNotFail() {
        // When/Then - should not throw
        filter.destroy();
    }

    /**
     * Test: RequestCostResponseWrapper.containsHeader Should: Return true for cost header Expected: Always returns true
     * for X-Request-Cost
     */
    @Test
    public void test_responseWrapper_containsHeader_shouldReturnTrueForCostHeader() throws Exception {
        // Given
        requestCostApi.initAccounting(request);
        final boolean[] wrapperTested = {false};

        FilterChain testChain = (req, res) -> {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            // Test the wrapper
            assertTrue("Should contain cost header", response.containsHeader(RequestCostApi.REQUEST_COST_HEADER_NAME));
            assertTrue("Should contain cost header (case insensitive)", response.containsHeader("x-dotrequest-cost"));
            wrapperTested[0] = true;
        };

        // When
        filter.doFilter(request, response, testChain);

        // Then
        assertTrue("Wrapper should have been tested", wrapperTested[0]);
    }

    /**
     * Test: RequestCostResponseWrapper.getHeader Should: Return current cost for cost header Expected: Returns string
     * representation of current cost
     */
    @Test
    public void test_responseWrapper_getHeader_shouldReturnCurrentCost() throws Exception {
        // Given
        requestCostApi.initAccounting(request);
        requestCostApi.incrementCost(Price.TWENTY, RequestCostFilterTest.class, "method", new Object[]{});
        requestCostApi.incrementCost(Price.TWENTY, RequestCostFilterTest.class, "method", new Object[]{});
        requestCostApi.incrementCost(Price.THREE, RequestCostFilterTest.class, "method", new Object[]{});
        final boolean[] wrapperTested = {false};

        FilterChain testChain = (req, res) -> {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            String costHeader = response.getHeader(RequestCostApi.REQUEST_COST_HEADER_NAME);
            assertNotNull("Cost header should not be null", costHeader);
            double cost = Double.parseDouble(costHeader);
            assertTrue("Cost should be at least 42", cost >= 42 / requestCostApi.getRequestCostDenominator());
            wrapperTested[0] = true;
        };

        // When
        filter.doFilter(request, response, testChain);

        // Then
        assertTrue("Wrapper should have been tested", wrapperTested[0]);
    }

    /**
     * Test: RequestCostResponseWrapper.getHeaders Should: Return collection with cost value Expected: Collection
     * contains current cost
     */
    @Test
    public void test_responseWrapper_getHeaders_shouldReturnCostCollection() throws Exception {
        // Given
        requestCostApi.initAccounting(request);
        requestCostApi.incrementCost(Price.FIVE, RequestCostFilterTest.class, "method", new Object[]{});
        requestCostApi.incrementCost(Price.TWENTY, RequestCostFilterTest.class, "method", new Object[]{});
        final boolean[] wrapperTested = {false};

        FilterChain testChain = (req, res) -> {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            Collection<String> headers = response.getHeaders(RequestCostApi.REQUEST_COST_HEADER_NAME);
            assertNotNull("Headers collection should not be null", headers);
            assertEquals("Should have exactly one header value", 1, headers.size());
            String costValue = headers.iterator().next();
            double cost = Double.parseDouble(costValue);
            assertTrue("Cost should be at least 25", cost >= 25 / requestCostApi.getRequestCostDenominator());
            wrapperTested[0] = true;
        };

        // When
        filter.doFilter(request, response, testChain);

        // Then
        assertTrue("Wrapper should have been tested", wrapperTested[0]);
    }

    /**
     * Test: RequestCostResponseWrapper.getHeaderNames Should: Include cost header in names collection Expected:
     * Collection contains X-Request-Cost
     */
    @Test
    public void test_responseWrapper_getHeaderNames_shouldIncludeCostHeader() throws Exception {
        // Given
        requestCostApi.initAccounting(request);

        final boolean[] wrapperTested = {false};

        FilterChain testChain = (req, res) -> {
            HttpServletResponse response = (HttpServletResponse) res;
            Collection<String> headerNames = response.getHeaderNames();
            assertNotNull("Header names should not be null", headerNames);
            assertTrue("Should contain cost header",
                    headerNames.stream().anyMatch(name ->
                            RequestCostApi.REQUEST_COST_HEADER_NAME.equalsIgnoreCase(name)));
            wrapperTested[0] = true;
        };

        // When
        filter.doFilter(request, response, testChain);

        // Then
        assertTrue("Wrapper should have been tested", wrapperTested[0]);
    }

    /**
     * Test: Filter adds cost header before and after chain Should: Header is updated after chain execution Expected:
     * Cost increases during chain execution
     */
    @Test
    public void test_doFilter_shouldUpdateCostHeaderAfterChain() throws Exception {
        // Given
        requestCostApi.initAccounting(request);
        final double[] costBeforeChain = {0};
        final double[] costAfterChain = {0};

        FilterChain testChain = (req, res) -> {
            // Capture cost before chain completes
            HttpServletResponse response = (HttpServletResponse) res;
            String headerBefore = response.getHeader(RequestCostApi.REQUEST_COST_HEADER_NAME);
            costBeforeChain[0] = Double.parseDouble(headerBefore);

            // Add more cost during chain execution
            requestCostApi.incrementCost(Price.TWENTY, RequestCostFilterTest.class, "chainMethod", new Object[]{});
            requestCostApi.incrementCost(Price.THIRTY, RequestCostFilterTest.class, "chainMethod", new Object[]{});
        };

        // When
        filter.doFilter(request, response, testChain);

        // Then
        String headerAfter = response.getHeader(RequestCostApi.REQUEST_COST_HEADER_NAME);
        costAfterChain[0] = Double.parseDouble(headerAfter);

        assertTrue("Cost after chain should be greater than before",
                costAfterChain[0] > costBeforeChain[0]);
    }

    /**
     * Test: Filter with zero cost Should: Handle zero cost gracefully Expected: Header is set with value 0 or higher
     */
    @Test
    public void test_doFilter_withZeroCost_shouldNotFail() throws Exception {
        // Given
        requestCostApi.initAccounting(request);
        // Don't add any cost

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        String costHeader = response.getHeader(RequestCostApi.REQUEST_COST_HEADER_NAME);
        assertNotNull("Cost header should be set even with zero cost", costHeader);
        double cost = Double.parseDouble(costHeader);
        assertTrue("Cost should be non-negative", cost >= 0);
    }

    /**
     * Test: Filter with exception in chain Should: Still add cost header before exception propagates Expected: Header
     * is set, exception is propagated
     */
    @Test(expected = RuntimeException.class)
    public void test_doFilter_withExceptionInChain_shouldStillAddHeader() throws Exception {
        // Given
        requestCostApi.initAccounting(request);
        FilterChain throwingChain = (req, res) -> {
            throw new RuntimeException("Test exception");
        };

        // When/Then - should throw but header should be set first
        try {
            filter.doFilter(request, response, throwingChain);
        } finally {
            // Verify header was set before exception
            String costHeader = response.getHeader(RequestCostApi.REQUEST_COST_HEADER_NAME);
            assertNotNull("Cost header should be set even when exception occurs", costHeader);
        }
    }
}
