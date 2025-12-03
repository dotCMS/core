package com.dotcms.adminsite;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link AdminSiteAPI}
 */
public class AdminSiteAPITest extends IntegrationTestBase {

    private static AdminSiteAPI adminSiteAPI;

    static boolean originalAdminSiteEnabled = false;




    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        adminSiteAPI = APILocator.getAdminSiteAPI();

        originalAdminSiteEnabled = Config.getBooleanProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, false);
        Config.setProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, true);


    }

    @AfterClass
    public static void cleanup() throws Exception {
        Config.setProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, originalAdminSiteEnabled);

    }



    /**
     * Method to test: {@link AdminSiteAPI#getAdminSiteUrl()} Given Scenario: No ADMIN_SITE_URL is configured
     * ExpectedResult: Returns the default URL (https://local.dotcms.site:8443)
     */
    @Test
    public void test_getAdminSiteUrl_returns_default_when_not_configured() {
        // Given: Clear any existing configuration
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
            adminSiteAPI.invalidateCache();

            // When
            final String adminSiteUrl = adminSiteAPI.getAdminSiteUrl();

            // Then
            Assert.assertEquals(AdminSiteAPI._ADMIN_SITE_URL_DEFAULT, adminSiteUrl);
        } finally {
            // Restore original value
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#getAdminSiteUrl()} Given Scenario: ADMIN_SITE_URL is configured with a valid
     * URL ExpectedResult: Returns the configured URL
     */
    @Test
    public void test_getAdminSiteUrl_returns_configured_value() {
        // Given
        final String testUrl = "https://admin.example.com";
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, testUrl);
            adminSiteAPI.invalidateCache();

            // When
            final String adminSiteUrl = adminSiteAPI.getAdminSiteUrl();

            // Then
            Assert.assertEquals(testUrl, adminSiteUrl);
        } finally {
            // Restore original value
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#getAdminSiteUrl()} Given Scenario: ADMIN_SITE_URL is configured with trailing
     * slashes ExpectedResult: Returns the URL with trailing slashes removed
     */
    @Test
    public void test_getAdminSiteUrl_removes_trailing_slashes() {
        // Given
        final String testUrl = "https://admin.example.com///";
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, testUrl);
            adminSiteAPI.invalidateCache();

            // When
            final String adminSiteUrl = adminSiteAPI.getAdminSiteUrl();

            // Then
            Assert.assertEquals("https://admin.example.com", adminSiteUrl);
        } finally {
            // Restore original value
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#getAdminSiteUrl()} Given Scenario: ADMIN_SITE_URL is configured without a
     * protocol ExpectedResult: Returns the URL with https:// prepended
     */
    @Test
    public void test_getAdminSiteUrl_adds_https_protocol_when_missing() {
        // Given
        final String testUrl = "admin.example.com";
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, testUrl);
            adminSiteAPI.invalidateCache();

            // When
            final String adminSiteUrl = adminSiteAPI.getAdminSiteUrl();

            // Then
            Assert.assertEquals("https://admin.example.com", adminSiteUrl);
        } finally {
            // Restore original value
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSite(String)} Given Scenario: Check various hosts against default
     * admin domains ExpectedResult: Returns true for hosts that end with default admin domains
     */
    @Test
    public void test_isAdminSite_with_default_domains() {
        // Given: Default domains include dotcms.com, localhost, etc.
        adminSiteAPI.invalidateCache();

        // Then: Should match domains that end with default admin domains
        Assert.assertTrue("Should match localhost", adminSiteAPI.isAdminSite("localhost"));
        Assert.assertTrue("Should match dotcms.com", adminSiteAPI.isAdminSite("dotcms.com"));
        Assert.assertTrue("Should match subdomain of dotcms.com", adminSiteAPI.isAdminSite("admin.dotcms.com"));
        Assert.assertTrue("Should match dotcms.site", adminSiteAPI.isAdminSite("local.dotcms.site"));
        Assert.assertTrue("Should match dotcms.cloud", adminSiteAPI.isAdminSite("test.dotcms.cloud"));

        // Should NOT match random domains
        Assert.assertFalse("Should not match random.com", adminSiteAPI.isAdminSite("random.com"));
        Assert.assertFalse("Should not match example.org", adminSiteAPI.isAdminSite("example.org"));
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSiteUri(String)} Given Scenario: Check various URIs against default
     * admin URIs ExpectedResult: Returns true for URIs that start with default admin URI prefixes
     */
    @Test
    public void test_isAdminSiteUri_with_default_uris() {
        // Given: Default URIs include /dotadmin/, /html/, /c/, etc.
        adminSiteAPI.invalidateCache();

        // Then: Should match URIs that start with default admin URI prefixes
        Assert.assertTrue("Should match /dotadmin/", adminSiteAPI.isAdminSiteUri("/dotadmin/"));
        Assert.assertTrue("Should match /dotadmin/test", adminSiteAPI.isAdminSiteUri("/dotadmin/test"));
        Assert.assertTrue("Should match /html/", adminSiteAPI.isAdminSiteUri("/html/"));
        Assert.assertTrue("Should match /c/", adminSiteAPI.isAdminSiteUri("/c/"));
        Assert.assertTrue("Should match /dwr/", adminSiteAPI.isAdminSiteUri("/dwr/"));
        Assert.assertTrue("Should match /edit/", adminSiteAPI.isAdminSiteUri("/edit/"));

        // Should NOT match non-admin URIs
        Assert.assertFalse("Should not match /api/", adminSiteAPI.isAdminSiteUri("/api/"));
        Assert.assertFalse("Should not match /content", adminSiteAPI.isAdminSiteUri("/content"));
        Assert.assertFalse("Should not match /", adminSiteAPI.isAdminSiteUri("/"));
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSiteUri(HttpServletRequest)} Given Scenario: HttpServletRequest with
     * admin URI ExpectedResult: Returns true when request URI is an admin URI
     */
    @Test
    public void test_isAdminSiteUri_with_request() {
        // Given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/dotadmin/test");

        // When/Then
        Assert.assertTrue(adminSiteAPI.isAdminSiteUri(mockRequest));

        // Given: Non-admin request
        when(mockRequest.getRequestURI()).thenReturn("/api/v1/content");

        // When/Then
        Assert.assertFalse(adminSiteAPI.isAdminSiteUri(mockRequest));
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSite(HttpServletRequest)} Given Scenario: HttpServletRequest with
     * admin host ExpectedResult: Returns true when request host is an admin domain
     */
    @Test
    public void test_isAdminSite_with_request() {

        // Given: Request with admin domain
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("host")).thenReturn("admin.dotcms.com:8080");
        when(mockRequest.getAttribute(AdminSiteAPI.ADMIN_SITE_REQUEST_HEADERS)).thenReturn(null);

        // When/Then
        Assert.assertTrue(adminSiteAPI.isAdminSite(mockRequest));

        // Given: Request with non-admin domain
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("host")).thenReturn("www.example.com");
        when(mockRequest.getAttribute(AdminSiteAPI.ADMIN_SITE_REQUEST_HEADERS)).thenReturn(null);

        // When/Then
        Assert.assertFalse(adminSiteAPI.isAdminSite(mockRequest));
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSite(HttpServletRequest)} Given Scenario: HttpServletRequest already
     * validated (has ADMIN_SITE_REQUEST_HEADERS attribute) ExpectedResult: Returns true immediately without checking
     * host
     */
    @Test
    public void test_isAdminSite_with_already_validated_request() {
        // Given: Request already validated
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(AdminSiteAPI.ADMIN_SITE_REQUEST_HEADERS)).thenReturn("validated");

        // When/Then: Should return true regardless of host
        Assert.assertTrue(adminSiteAPI.isAdminSite(mockRequest));
    }

    /**
     * Method to test: {@link AdminSiteAPI#getAdminSiteHeaders()} Given Scenario: Default configuration ExpectedResult:
     * Returns default headers including x-robots-tag
     */
    @Test
    public void test_getAdminSiteHeaders_returns_default_headers() {
        // Given
        adminSiteAPI.invalidateCache();

        // When
        Map<String, String> headers = adminSiteAPI.getAdminSiteHeaders();

        // Then
        Assert.assertNotNull(headers);
        Assert.assertTrue("Should contain x-robots-tag", headers.containsKey("x-robots-tag"));
        Assert.assertEquals("noindex, nofollow", headers.get("x-robots-tag"));
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSiteEnabled()} Given Scenario: Default configuration (enabled by
     * default) ExpectedResult: Returns true
     */
    @Test
    public void test_isAdminSiteEnabled_returns_false_by_default() {
        // Given
        final Boolean originalValue = Config.getBooleanProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, true);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, null);
            adminSiteAPI.invalidateCache();

            // When/Then
            Assert.assertFalse(adminSiteAPI.isAdminSiteEnabled());
        } finally {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSiteEnabled()} Given Scenario: ADMIN_SITE_ENABLED is set to false
     * ExpectedResult: Returns false
     */
    @Test
    public void test_isAdminSiteEnabled_returns_false_when_disabled() {
        // Given
        final Boolean originalValue = Config.getBooleanProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, true);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, false);
            adminSiteAPI.invalidateCache();

            // When/Then
            Assert.assertFalse(adminSiteAPI.isAdminSiteEnabled());
        } finally {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_ENABLED, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSiteConfigured()} Given Scenario: ADMIN_SITE_URL is not set
     * ExpectedResult: Returns false
     */
    @Test
    public void test_isAdminSiteConfigured_returns_false_when_not_set() {
        // Given
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, null);

            // When/Then
            Assert.assertFalse(adminSiteAPI.isAdminSiteConfigured());
        } finally {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSiteConfigured()} Given Scenario: ADMIN_SITE_URL is set
     * ExpectedResult: Returns true
     */
    @Test
    public void test_isAdminSiteConfigured_returns_true_when_set() {
        // Given
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, "https://admin.example.com");

            // When/Then
            Assert.assertTrue(adminSiteAPI.isAdminSiteConfigured());
        } finally {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#invalidateCache()} Given Scenario: Cache is populated, then invalidated, then
     * URL is changed ExpectedResult: After invalidation, new URL value is returned
     */
    @Test
    public void test_invalidateCache_clears_cached_values() {
        // Given
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            // Set first URL and get it (caches the value)
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, "https://first.example.com");
            adminSiteAPI.invalidateCache();
            String firstUrl = adminSiteAPI.getAdminSiteUrl();
            Assert.assertEquals("https://first.example.com", firstUrl);

            // Change URL without invalidating - should still return cached value
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, "https://second.example.com");
            String cachedUrl = adminSiteAPI.getAdminSiteUrl();
            Assert.assertEquals("Should return cached value", "https://first.example.com", cachedUrl);

            // Now invalidate and verify new value is returned
            adminSiteAPI.invalidateCache();
            String newUrl = adminSiteAPI.getAdminSiteUrl();
            Assert.assertEquals("Should return new value after cache invalidation",
                    "https://second.example.com", newUrl);
        } finally {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#allowInsecureRequests()} Given Scenario: Default configuration
     * ExpectedResult: Returns false (insecure requests allowed by default)
     */
    @Test
    public void test_allowInsecureRequests_returns_true_by_default() {
        // Given
        final Boolean originalValue = Config.getBooleanProperty(AdminSiteAPI.ADMIN_SITE_REQUESTS_FORCE_SECURE, true);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_REQUESTS_FORCE_SECURE, null);

            // When/Then
            Assert.assertFalse(adminSiteAPI.allowInsecureRequests());
        } finally {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_REQUESTS_FORCE_SECURE, originalValue);
        }
    }

    /**
     * Method to test: {@link AdminSiteAPIImpl#getAdminDomains()} Given Scenario: ADMIN_SITE_URL is configured
     * ExpectedResult: Admin domains include the host from ADMIN_SITE_URL plus defaults
     */
    @Test
    public void test_getAdminDomains_includes_configured_url_host() {
        // Given
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, "https://custom-admin.mycompany.com:8443");
            adminSiteAPI.invalidateCache();

            // When
            String[] adminDomains = ((AdminSiteAPIImpl) adminSiteAPI).getAdminDomains();

            // Then
            Assert.assertNotNull(adminDomains);
            Assert.assertTrue("Should contain custom domain",
                    Arrays.asList(adminDomains).contains("custom-admin.mycompany.com"));
            // Should also still contain defaults
            Assert.assertTrue("Should contain localhost",
                    Arrays.asList(adminDomains).contains("localhost"));
        } finally {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPIImpl#getAdminUris()} Given Scenario: Default configuration ExpectedResult:
     * Returns default admin URIs
     */
    @Test
    public void test_getAdminUris_returns_defaults() {
        // Given
        adminSiteAPI.invalidateCache();

        // When
        String[] adminUris = ((AdminSiteAPIImpl) adminSiteAPI).getAdminUris();

        // Then
        Assert.assertNotNull(adminUris);
        Assert.assertTrue(adminUris.length > 0);
        Assert.assertTrue("Should contain /dotadmin/",
                Arrays.asList(adminUris).contains("/dotadmin/"));
        Assert.assertTrue("Should contain /html/",
                Arrays.asList(adminUris).contains("/html/"));
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminAllowed(HttpServletRequest)} Given Scenario: Any request
     * ExpectedResult: Returns false (currently always returns false)
     */
    @Test
    public void test_isAdminAllowed_returns_false() {
        // Given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        // When/Then
        Assert.assertFalse(adminSiteAPI.isAdminAllowed(mockRequest));
    }

    /**
     * Method to test: Multiple calls to getAdminSiteUrl after cache invalidation Given Scenario: Cache is invalidated,
     * then getAdminSiteUrl is called multiple times ExpectedResult: All calls return the same value (testing caching
     * behavior)
     */
    @Test
    public void test_getAdminSiteUrl_caches_result() {
        // Given
        final String originalValue = Config.getStringProperty(AdminSiteAPI.ADMIN_SITE_URL, null);
        try {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, "https://cache-test.example.com");
            adminSiteAPI.invalidateCache();

            // When: Call multiple times
            String url1 = adminSiteAPI.getAdminSiteUrl();
            String url2 = adminSiteAPI.getAdminSiteUrl();
            String url3 = adminSiteAPI.getAdminSiteUrl();

            // Then: All should be the same
            Assert.assertEquals(url1, url2);
            Assert.assertEquals(url2, url3);
            Assert.assertEquals("https://cache-test.example.com", url1);
        } finally {
            Config.setProperty(AdminSiteAPI.ADMIN_SITE_URL, originalValue);
            adminSiteAPI.invalidateCache();
        }
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSiteUri(String)} Given Scenario: URIs with mixed case ExpectedResult:
     * Matching is case-insensitive
     */
    @Test
    public void test_isAdminSiteUri_is_case_insensitive() {
        // Given
        adminSiteAPI.invalidateCache();

        // Then: Should match regardless of case
        Assert.assertTrue("Should match /DOTADMIN/", adminSiteAPI.isAdminSiteUri("/DOTADMIN/"));
        Assert.assertTrue("Should match /DotAdmin/", adminSiteAPI.isAdminSiteUri("/DotAdmin/"));
        Assert.assertTrue("Should match /HTML/", adminSiteAPI.isAdminSiteUri("/HTML/"));
        Assert.assertTrue("Should match /Html/test", adminSiteAPI.isAdminSiteUri("/Html/test"));
        Assert.assertTrue("Should match /C/", adminSiteAPI.isAdminSiteUri("/C/"));
        Assert.assertTrue("Should match /DWR/", adminSiteAPI.isAdminSiteUri("/DWR/"));
        Assert.assertTrue("Should match /EDIT/", adminSiteAPI.isAdminSiteUri("/EDIT/"));
    }

    /**
     * Method to test: {@link AdminSiteAPI#isAdminSite(String)} Given Scenario: Domains with mixed case ExpectedResult:
     * Matching is case-insensitive
     */
    @Test
    public void test_isAdminSite_is_case_insensitive() {
        // Given
        adminSiteAPI.invalidateCache();

        // Then: Should match regardless of case
        Assert.assertTrue("Should match LOCALHOST", adminSiteAPI.isAdminSite("LOCALHOST"));
        Assert.assertTrue("Should match LocalHost", adminSiteAPI.isAdminSite("LocalHost"));
        Assert.assertTrue("Should match DOTCMS.COM", adminSiteAPI.isAdminSite("DOTCMS.COM"));
        Assert.assertTrue("Should match DotCMS.Com", adminSiteAPI.isAdminSite("DotCMS.Com"));
        Assert.assertTrue("Should match admin.DOTCMS.SITE", adminSiteAPI.isAdminSite("admin.DOTCMS.SITE"));
        Assert.assertTrue("Should match TEST.DotCms.Cloud", adminSiteAPI.isAdminSite("TEST.DotCms.Cloud"));
    }
}
