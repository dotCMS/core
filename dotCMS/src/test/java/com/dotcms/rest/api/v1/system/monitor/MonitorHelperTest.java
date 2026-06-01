package com.dotcms.rest.api.v1.system.monitor;

import com.dotmarketing.util.WebKeys;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MonitorHelper} focusing on IP-based access control,
 * including IPv4, IPv6, and Docker gateway IP scenarios.
 */
public class MonitorHelperTest {

    private HttpServletRequest request;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
    }

    /**
     * Test that IPv4 localhost (127.0.0.1) is properly recognized and granted access
     */
    @Test
    public void test_isAccessGranted_ipv4_localhost() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("IPv4 localhost should be granted access", helper.accessGranted);
    }

    /**
     * Test that IPv6 localhost in collapsed form (::1) is properly recognized and granted access
     * This is the most common form used by modern systems and the root cause of the original bug
     */
    @Test
    public void test_isAccessGranted_ipv6_localhost_collapsed_form() {
        when(request.getRemoteAddr()).thenReturn("::1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("IPv6 localhost in collapsed form (::1) should be granted access", 
                   helper.accessGranted);
    }

    /**
     * Test that IPv6 localhost in expanded form (0:0:0:0:0:0:0:1) is properly recognized
     */
    @Test
    public void test_isAccessGranted_ipv6_localhost_expanded_form() {
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("IPv6 localhost in expanded form should be granted access", 
                   helper.accessGranted);
    }

    /**
     * Test that IPv6 localhost in partially collapsed form is properly recognized
     */
    @Test
    public void test_isAccessGranted_ipv6_localhost_partial_form() {
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("IPv6 localhost in any valid form should be granted access", 
                   helper.accessGranted);
    }

    /**
     * Test that Docker gateway IP (172.17.0.1) is granted access when in default ACL
     * This IP is within the 172.16.0.0/12 CIDR range that should be allowed
     */
    @Test
    public void test_isAccessGranted_docker_gateway_ip() {
        when(request.getRemoteAddr()).thenReturn("172.17.0.1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("Docker gateway IP 172.17.0.1 should be granted access (within 172.16.0.0/12)", 
                   helper.accessGranted);
    }

    /**
     * Test that another Docker gateway IP (172.18.0.1) is granted access
     */
    @Test
    public void test_isAccessGranted_docker_gateway_ip_172_18() {
        when(request.getRemoteAddr()).thenReturn("172.18.0.1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("Docker gateway IP 172.18.0.1 should be granted access (within 172.16.0.0/12)", 
                   helper.accessGranted);
    }

    /**
     * Test that private network IP (10.0.0.1) is granted access when in default ACL
     */
    @Test
    public void test_isAccessGranted_private_network_10_0_0_1() {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("Private network IP 10.0.0.1 should be granted access (within 10.0.0.0/8)", 
                   helper.accessGranted);
    }

    /**
     * Test that private network IP (192.168.1.1) is granted access when in default ACL
     */
    @Test
    public void test_isAccessGranted_private_network_192_168() {
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("Private network IP 192.168.1.1 should be granted access (within 192.168.0.0/16)", 
                   helper.accessGranted);
    }

    /**
     * Test that public IP (8.8.8.8) is denied access (not in ACL)
     */
    @Test
    public void test_isAccessGranted_public_ip_denied() {
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertFalse("Public IP 8.8.8.8 should be denied access", 
                    helper.accessGranted);
    }

    /**
     * Test that external IP (1.1.1.1) is denied access (not in ACL)
     */
    @Test
    public void test_isAccessGranted_external_ip_denied() {
        when(request.getRemoteAddr()).thenReturn("1.1.1.1");
        
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertFalse("External IP 1.1.1.1 should be denied access", 
                    helper.accessGranted);
    }

    /**
     * Test isLocalhostAddress method directly with IPv4 localhost
     */
    @Test
    public void test_isLocalhostAddress_ipv4() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("127.0.0.1 should be recognized as localhost", 
                   helper.isLocalhostAddress("127.0.0.1"));
    }

    /**
     * Test isLocalhostAddress method directly with IPv6 collapsed localhost
     */
    @Test
    public void test_isLocalhostAddress_ipv6_collapsed() {
        when(request.getRemoteAddr()).thenReturn("::1");
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("::1 should be recognized as localhost", 
                   helper.isLocalhostAddress("::1"));
    }

    /**
     * Test isLocalhostAddress method directly with IPv6 expanded localhost
     */
    @Test
    public void test_isLocalhostAddress_ipv6_expanded() {
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertTrue("0:0:0:0:0:0:0:1 should be recognized as localhost", 
                   helper.isLocalhostAddress("0:0:0:0:0:0:0:1"));
    }

    /**
     * Test isLocalhostAddress method with non-localhost IP
     */
    @Test
    public void test_isLocalhostAddress_non_localhost() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertFalse("8.8.8.8 should not be recognized as localhost", 
                    helper.isLocalhostAddress("8.8.8.8"));
    }

    /**
     * Test isLocalhostAddress method with null address
     */
    @Test
    public void test_isLocalhostAddress_null() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertFalse("null should not be recognized as localhost", 
                    helper.isLocalhostAddress(null));
    }

    /**
     * Test isLocalhostAddress method with empty string
     */
    @Test
    public void test_isLocalhostAddress_empty_string() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final MonitorHelper helper = new MonitorHelper(request, false);
        
        assertFalse("Empty string should not be recognized as localhost", 
                    helper.isLocalhostAddress(""));
    }

    /**
     * Test isLocalhostAddress method with invalid IP address
     */
    @Test
    public void test_isLocalhostAddress_invalid_ip() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final MonitorHelper helper = new MonitorHelper(request, false);

        assertFalse("Invalid IP should not be recognized as localhost",
                    helper.isLocalhostAddress("not.an.ip.address"));
    }

    /**
     * Test that any IP from 127.0.0.0/8 range is recognized as localhost
     */
    @Test
    public void test_isLocalhostAddress_ipv4_loopback_range() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final MonitorHelper helper = new MonitorHelper(request, false);

        assertTrue("127.0.0.2 should be recognized as localhost (loopback range)",
                   helper.isLocalhostAddress("127.0.0.2"));
        assertTrue("127.1.1.1 should be recognized as localhost (loopback range)",
                   helper.isLocalhostAddress("127.1.1.1"));
        assertTrue("127.255.255.255 should be recognized as localhost (loopback range)",
                   helper.isLocalhostAddress("127.255.255.255"));
    }

    // -------------------------------------------------------------------------
    // isStartedUp
    // -------------------------------------------------------------------------

    /**
     * isStartedUp() must return false when the {@code dotcms.started.up} system property
     * has not been set.
     */
    @Test
    public void test_isStartedUp_false_when_property_absent() {
        System.clearProperty(WebKeys.DOTCMS_STARTED_UP);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertFalse(new MonitorHelper(request, false).isStartedUp());
    }

    /**
     * isStartedUp() must return true as long as the {@code dotcms.started.up} system property
     * is present, regardless of its value.
     */
    @Test
    public void test_isStartedUp_true_when_property_present() {
        System.setProperty(WebKeys.DOTCMS_STARTED_UP, "true");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        try {
            assertTrue(new MonitorHelper(request, false).isStartedUp());
        } finally {
            System.clearProperty(WebKeys.DOTCMS_STARTED_UP);
        }
    }

    // -------------------------------------------------------------------------
    // FileSystemTest inner class
    // -------------------------------------------------------------------------

    /**
     * FileSystemTest must return true when given a writable temporary directory.
     */
    @Test
    public void test_fileSystemTest_returns_true_for_writable_path() throws Exception {
        final Path tempDir = Files.createTempDirectory("monitor-test-");
        try {
            assertTrue(new MonitorHelper.FileSystemTest(tempDir.toString()).call());
        } finally {
            deleteRecursively(tempDir.toFile());
        }
    }

    /**
     * FileSystemTest must handle a base path that already ends with the file separator
     * without producing a double-separator in the internal path.
     */
    @Test
    public void test_fileSystemTest_handles_trailing_separator() throws Exception {
        final Path tempDir = Files.createTempDirectory("monitor-test-");
        try {
            assertTrue(new MonitorHelper.FileSystemTest(tempDir + File.separator).call());
        } finally {
            deleteRecursively(tempDir.toFile());
        }
    }

    /**
     * FileSystemTest must return false when the base path is an existing regular file
     * (not a directory), making mkdirs() impossible.
     */
    @Test
    public void test_fileSystemTest_returns_false_when_base_path_is_a_file() throws Exception {
        final File tempFile = File.createTempFile("monitor-test-", ".tmp");
        try {
            assertFalse(new MonitorHelper.FileSystemTest(tempFile.getAbsolutePath()).call());
        } finally {
            tempFile.delete();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void deleteRecursively(final File f) {
        if (f.isDirectory()) {
            final File[] children = f.listFiles();
            if (children != null) {
                for (final File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        f.delete();
    }
}
