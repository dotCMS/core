package com.dotcms.adminsite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.mock.response.MockHttpStatusAndHeadersResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AdminSiteRequestFilter}
 */
public class AdminSiteRequestFilterTest extends UnitTestBase {

    private AdminSiteAPI mockAdminSiteAPI;
    private AdminSiteRequestFilter filter;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private MockHttpStatusAndHeadersResponse captureResponse;
    private FilterChain mockFilterChain;

    @Before
    public void setUp() {
        mockAdminSiteAPI = mock(AdminSiteAPI.class);
        filter = new AdminSiteRequestFilter(mockAdminSiteAPI);
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
        captureResponse = new MockHttpStatusAndHeadersResponse(mockResponse);
        mockFilterChain = mock(FilterChain.class);
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Admin site functionality is disabled
     * ExpectedResult: Request passes through without any checks
     */
    @Test
    public void test_doFilter_passes_through_when_admin_site_disabled() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(false);

        // When
        filter.doFilter(mockRequest, captureResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(mockRequest, captureResponse);
        verify(mockAdminSiteAPI, never()).isAdminSiteUri(mockRequest);
        verify(mockAdminSiteAPI, never()).isAdminSite(mockRequest);
        assertEquals(HttpServletResponse.SC_OK, captureResponse.getStatus());
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Admin URI accessed from non-admin domain
     * ExpectedResult: Request is blocked with 404 and x-dot-admin-site header
     */
    @Test
    public void test_doFilter_blocks_admin_uri_from_non_admin_domain() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSiteUri(mockRequest)).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSite(mockRequest)).thenReturn(false);
        when(mockRequest.getRequestURI()).thenReturn("/dotadmin/");
        when(mockRequest.getHeader("host")).thenReturn("www.example.com");

        // When
        filter.doFilter(mockRequest, captureResponse, mockFilterChain);

        // Then
        verify(mockFilterChain, never()).doFilter(mockRequest, captureResponse);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, captureResponse.getStatus());
        assertEquals("true", captureResponse.getHeader("x-dot-admin-site"));
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Admin URI accessed from admin domain with
     * valid HTTPS ExpectedResult: Request passes through with admin headers added
     */
    @Test
    public void test_doFilter_allows_admin_uri_from_admin_domain() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSiteUri(mockRequest)).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSite(mockRequest)).thenReturn(true);
        when(mockAdminSiteAPI.allowInsecureRequests()).thenReturn(true);

        Map<String, String> headers = new HashMap<>();
        headers.put("x-robots-tag", "noindex, nofollow");
        when(mockAdminSiteAPI.getAdminSiteHeaders()).thenReturn(headers);

        // When
        filter.doFilter(mockRequest, captureResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(mockRequest, captureResponse);
        assertEquals(HttpServletResponse.SC_OK, captureResponse.getStatus());
        assertEquals("noindex, nofollow", captureResponse.getHeader("x-robots-tag"));
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Admin URI accessed without HTTPS when
     * insecure requests not allowed ExpectedResult: Request is blocked with 426 Upgrade Required
     */
    @Test
    public void test_doFilter_blocks_insecure_admin_request() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSiteUri(mockRequest)).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSite(mockRequest)).thenReturn(true);
        when(mockAdminSiteAPI.allowInsecureRequests()).thenReturn(false);
        when(mockRequest.isSecure()).thenReturn(false);

        // When - use the raw mock response to verify sendError is called with status and message
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Then
        verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
        verify(mockResponse).sendError(426, "Upgrade to HTTPS");
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Admin URI accessed with HTTPS when
     * insecure requests not allowed ExpectedResult: Request passes through
     */
    @Test
    public void test_doFilter_allows_secure_admin_request() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSiteUri(mockRequest)).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSite(mockRequest)).thenReturn(true);
        when(mockAdminSiteAPI.allowInsecureRequests()).thenReturn(false);
        when(mockRequest.isSecure()).thenReturn(true);
        when(mockAdminSiteAPI.getAdminSiteHeaders()).thenReturn(new HashMap<>());

        // When
        filter.doFilter(mockRequest, captureResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(mockRequest, captureResponse);
        assertEquals(HttpServletResponse.SC_OK, captureResponse.getStatus());
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Non-admin URI accessed from any domain
     * ExpectedResult: Request passes through without admin headers
     */
    @Test
    public void test_doFilter_allows_non_admin_uri() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSiteUri(mockRequest)).thenReturn(false);
        when(mockAdminSiteAPI.isAdminSite(mockRequest)).thenReturn(false);

        // When
        filter.doFilter(mockRequest, captureResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(mockRequest, captureResponse);
        assertEquals(HttpServletResponse.SC_OK, captureResponse.getStatus());
        assertNull(captureResponse.getHeader("x-robots-tag"));
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Non-admin URI accessed from admin domain
     * ExpectedResult: Request passes through with admin headers added
     */
    @Test
    public void test_doFilter_adds_headers_for_admin_site_non_admin_uri() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSiteUri(mockRequest)).thenReturn(false);
        when(mockAdminSiteAPI.isAdminSite(mockRequest)).thenReturn(true);

        Map<String, String> headers = new HashMap<>();
        headers.put("x-robots-tag", "noindex, nofollow");
        headers.put("x-custom-header", "custom-value");
        when(mockAdminSiteAPI.getAdminSiteHeaders()).thenReturn(headers);

        // When
        filter.doFilter(mockRequest, captureResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(mockRequest, captureResponse);
        assertEquals(HttpServletResponse.SC_OK, captureResponse.getStatus());
        assertEquals("noindex, nofollow", captureResponse.getHeader("x-robots-tag"));
        assertEquals("custom-value", captureResponse.getHeader("x-custom-header"));
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#isHTTPSOk} Given Scenario: Insecure requests are allowed
     * ExpectedResult: Returns true regardless of request secure status
     */
    @Test
    public void test_isHTTPSOk_returns_true_when_insecure_allowed() {
        // Given
        when(mockAdminSiteAPI.allowInsecureRequests()).thenReturn(true);
        when(mockRequest.isSecure()).thenReturn(false);

        // When/Then
        assertTrue(filter.isHTTPSOk(mockRequest));
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#isHTTPSOk} Given Scenario: Insecure requests not allowed, request
     * is secure ExpectedResult: Returns true
     */
    @Test
    public void test_isHTTPSOk_returns_true_when_request_is_secure() {
        // Given
        when(mockAdminSiteAPI.allowInsecureRequests()).thenReturn(false);
        when(mockRequest.isSecure()).thenReturn(true);

        // When/Then
        assertTrue(filter.isHTTPSOk(mockRequest));
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#isHTTPSOk} Given Scenario: Insecure requests not allowed, request
     * is not secure ExpectedResult: Returns false
     */
    @Test
    public void test_isHTTPSOk_returns_false_when_insecure_not_allowed_and_not_secure() {
        // Given
        when(mockAdminSiteAPI.allowInsecureRequests()).thenReturn(false);
        when(mockRequest.isSecure()).thenReturn(false);

        // When/Then
        assertFalse(filter.isHTTPSOk(mockRequest));
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Multiple admin headers configured
     * ExpectedResult: All headers are added to response
     */
    @Test
    public void test_doFilter_adds_multiple_headers() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSiteUri(mockRequest)).thenReturn(false);
        when(mockAdminSiteAPI.isAdminSite(mockRequest)).thenReturn(true);

        Map<String, String> headers = new HashMap<>();
        headers.put("x-robots-tag", "noindex, nofollow");
        headers.put("x-frame-options", "DENY");
        headers.put("x-content-type-options", "nosniff");
        when(mockAdminSiteAPI.getAdminSiteHeaders()).thenReturn(headers);

        // When
        filter.doFilter(mockRequest, captureResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(mockRequest, captureResponse);
        assertEquals("noindex, nofollow", captureResponse.getHeader("x-robots-tag"));
        assertEquals("DENY", captureResponse.getHeader("x-frame-options"));
        assertEquals("nosniff", captureResponse.getHeader("x-content-type-options"));
    }

    /**
     * Method to test: {@link AdminSiteRequestFilter#doFilter} Given Scenario: Admin URI from non-admin domain, should
     * log warning ExpectedResult: Request blocked with proper response even with logging
     */
    @Test
    public void test_doFilter_logs_blocked_request_info() throws IOException, ServletException {
        // Given
        when(mockAdminSiteAPI.isAdminSiteEnabled()).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSiteUri(mockRequest)).thenReturn(true);
        when(mockAdminSiteAPI.isAdminSite(mockRequest)).thenReturn(false);
        when(mockRequest.getRequestURI()).thenReturn("/html/portlet/ext/test.jsp");
        when(mockRequest.getHeader("host")).thenReturn("attacker.com");

        // When
        filter.doFilter(mockRequest, captureResponse, mockFilterChain);

        // Then
        verify(mockFilterChain, never()).doFilter(mockRequest, captureResponse);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, captureResponse.getStatus());
        assertEquals("true", captureResponse.getHeader("x-dot-admin-site"));
    }
}
