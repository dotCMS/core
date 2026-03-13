package com.dotcms.health.checks;

import org.apache.catalina.util.ServerInfo;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Security regression test for CVE-2025-66614.
 *
 * Verifies that the Apache Tomcat version in use meets the minimum required
 * version (9.0.113+) that fixes the SNI/Host header mismatch vulnerability
 * which allows bypassing client certificate authentication in multi-virtual-host
 * configurations.
 *
 * @see <a href="https://app.opencve.io/cve/CVE-2025-66614">CVE-2025-66614</a>
 */
public class TomcatVersionSecurityTest {

    private static final int[] MINIMUM_SAFE_VERSION = {9, 0, 113};

    @Test
    public void testTomcatVersion_MeetsMinimumForCVE_2025_66614() {
        String serverNumber = ServerInfo.getServerNumber();
        assertNotNull("Tomcat ServerInfo should be available on the classpath", serverNumber);

        int[] actual = parseVersion(serverNumber);
        assertTrue(
            "Tomcat " + serverNumber + " is vulnerable to CVE-2025-66614. " +
            "Minimum safe version is 9.0.113. Upgrade tomcat.version in parent/pom.xml.",
            isVersionAtLeast(actual, MINIMUM_SAFE_VERSION)
        );
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.trim().split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9].*", ""));
        }
        return result;
    }

    private static boolean isVersionAtLeast(int[] actual, int[] minimum) {
        int len = Math.min(actual.length, minimum.length);
        for (int i = 0; i < len; i++) {
            if (actual[i] > minimum[i]) return true;
            if (actual[i] < minimum[i]) return false;
        }
        return actual.length >= minimum.length;
    }
}
