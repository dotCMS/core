package com.dotcms.util.network;

import static org.junit.Assert.*;
import java.net.InetAddress;
import org.junit.BeforeClass;
import org.junit.Test;

public class IPUtilsTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}
    final static String[][] trueCases = {
            {"0.0.0.0/0", "127.0.0.1"},
            {"0.0.0.0/0", "244.244.244.1"},
            {"0.0.0.0/0", "0:0:0:0:0:0:0:1"},
            {"192.168.1.0/24", "192.168.1.1"},
            {"192.168.1.0/24", "192.168.1.0"},
            {"192.168.1.0/24", "192.168.1.255"},
            {"192.168.0.0/16", "192.168.1.255"},
            {"192.0.0.0/8", "192.168.1.255"},
            // IPv6 CIDRs
            {"fc00::/7", "fc00::1"},
            {"fc00::/7", "fd00:ec2::254"},
            {"::1/128", "::1"},
            {"2001:db8::/32", "2001:db8:0:0:0:0:0:1"},
            {"fe80::/10", "fe80::1"},

    };


    
    final static String[][] falseCases = {
            {"255.0.0.1/24", "255.0.1.2"},
            {"244.244.244.2/32", "244.244.244.1"},
            {"192.168.2.0/24", "192.168.1.1"},
            {"192.168.2.0/24", "192.168.1.255"},
            {"192.169.1.0/24", "192.168.1.255"},
            {"192.169.0.0/16", "192.168.1.255"},
            {"192.0.0.0/8", "193.168.1.255"},
            {"2001:db8::/32", "2001:db9:0:0:0:0:0:1"},
            {"fc00::/7", "2001:4860:4860:0:0:0:0:8888"},
            // cross-family comparisons must not match
            {"10.0.0.0/8", "fc00::1"},
            {"fc00::/7", "10.0.0.1"},

    };



    
    
    @Test
    public void test_true_cases() {
        for(String[] testCase : trueCases) {
            assertTrue( testCase[1] + " is in " + testCase[0], IPUtils.isIpInCIDR(testCase[1], testCase[0]));
            
            
        }

    }
    
    @Test
    public void test_false_cases() {
        for(String[] testCase : falseCases) {
            assertTrue( testCase[1] + " is NOT in " + testCase[0], !IPUtils.isIpInCIDR(testCase[1], testCase[0]));
            
            
        }

    }
    
    
    final static String[] ipsOnPrivateSubnets= {
            "192.168.1.255",
            "10.0.0.4",
            "127.0.0.1",
            "172.16.3.5",
            "172.16.3.0",
            "localhost",
            "127.0.0.1"
    };
    
    /**
     * Addresses that must be classified as internal. Covers the IPv6 ranges that carry no
     * equivalent in the IPv4-only default blacklist, so the classification cannot regress to
     * depending on that list alone.
     */
    final static String[] ipv6AddressesOnPrivateSubnets = {
            "::1",                      // IPv6 loopback
            "[::1]",                    // as URL.getHost() hands it over, brackets included
            "0:0:0:0:0:0:0:1",          // expanded loopback
            "fc00::1",                  // unique-local, not covered by the JDK helpers
            "fd00::1",                  // unique-local, upper half of fc00::/7
            "fd00:ec2::254",            // IPv6 instance metadata
            "fe80::1",                  // link-local
            "::ffff:127.0.0.1",         // IPv4-mapped loopback
            "::ffff:10.0.0.1",          // IPv4-mapped private
            "::",                       // unspecified
    };

    final static String[] ipv6AddressesOnPublicSubnets = {
            "2001:4860:4860::8888",
            "2606:4700:4700::1111",
    };

    final static String[] ipsOnPublicSubnets= {
            "2.2.2.2",
            "3.22.136.122",
            "142.251.32.110",
            "74.6.231.21",
            "dotcms.com",
            "193.252.133.20"
    };
    
    @Test
    public void test_ip_private_subnets() {

        for(String testCase : ipsOnPrivateSubnets) {
            assertTrue( "Must be a private subnets: " + testCase, IPUtils.isIpPrivateSubnet(testCase));
        }
    }
    /**
     * An explicitly configured REMOTE_CALL_SUBNET_BLACKLIST defines the policy on its own. A
     * deployment that narrows the list to permit specific internal ranges must keep working, so
     * the built-in address checks must not be applied on top of a configured list.
     */
    @Test
    public void test_configured_blacklist_is_not_overridden_by_builtin_checks() throws Exception {
        final String[] onlyMetadata = {"169.254.169.254/32"};

        assertTrue("the configured range must still match",
                IPUtils.isInternalAddress(InetAddress.getByName("169.254.169.254"), onlyMetadata));

        for (final String permitted : new String[]{"10.0.0.1", "192.168.1.1", "172.16.3.5", "127.0.0.1"}) {
            assertFalse(permitted + " is outside the configured list, so it must stay permitted",
                    IPUtils.isInternalAddress(InetAddress.getByName(permitted), onlyMetadata));
        }
    }

    /**
     * With no configured list, the built-in checks apply and cover both address families.
     */
    @Test
    public void test_builtin_checks_apply_when_no_blacklist_configured() throws Exception {
        for (final String internal : new String[]{"10.0.0.1", "127.0.0.1", "169.254.169.254", "::1", "fc00::1"}) {
            assertTrue(internal + " must be internal when no list is configured",
                    IPUtils.isInternalAddress(InetAddress.getByName(internal), null));
        }
        for (final String publicAddress : new String[]{"2.2.2.2", "2606:4700:4700::1111"}) {
            assertFalse(publicAddress + " must stay public",
                    IPUtils.isInternalAddress(InetAddress.getByName(publicAddress), null));
        }
    }

    @Test
    public void test_ipv6_private_subnets() {
        for (final String testCase : ipv6AddressesOnPrivateSubnets) {
            assertTrue("Must be treated as a private subnet: " + testCase,
                    IPUtils.isIpPrivateSubnet(testCase));
        }
    }

    @Test
    public void test_ipv6_public_subnets() {
        for (final String testCase : ipv6AddressesOnPublicSubnets) {
            assertFalse("Must NOT be treated as a private subnet: " + testCase,
                    IPUtils.isIpPrivateSubnet(testCase));
        }
    }

    @Test
    
    public void test_ip_public_subnets() {
        for(String testCase : ipsOnPublicSubnets) {
            assertFalse( IPUtils.isIpPrivateSubnet(testCase));
        }
    }
    
    
}
