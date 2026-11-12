package com.dotcms.util.network;

import static org.junit.Assert.*;
import com.dotcms.util.network.IPUtils.AddressPolicy;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
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
     * equivalent in the IPv4-only built-in ranges, so the classification cannot regress to
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
     * With no configured lists, the built-in checks apply and cover both address families.
     */
    @Test
    public void test_builtin_checks_apply_when_no_lists_are_configured() throws Exception {
        final AddressPolicy noLists = policy(null, null, true);

        for (final String internal : new String[]{"10.0.0.1", "127.0.0.1", "169.254.169.254", "::1", "fc00::1"}) {
            assertTrue(internal + " must be internal when no list is configured",
                    IPUtils.isInternalAddress(address(internal), noLists));
        }
        for (final String publicAddress : new String[]{"2.2.2.2", "2606:4700:4700::1111"}) {
            assertFalse(publicAddress + " must stay public",
                    IPUtils.isInternalAddress(address(publicAddress), noLists));
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
    
    public void test_ip_public_subnets() {
        for(String testCase : ipsOnPublicSubnets) {
            assertFalse( IPUtils.isIpPrivateSubnet(testCase));
        }
    }

    // ---- Configured lists and the IPv6 switch ----

    private static AddressPolicy policy(final String[] denied, final String[] allowed, final boolean ipv6Allowed) {
        return AddressPolicy.of(denied, allowed, ipv6Allowed);
    }

    private static InetAddress address(final String literal) throws Exception {
        return InetAddress.getByName(literal);
    }

    private static void assertInternal(final AddressPolicy policy, final String... literals) throws Exception {
        for (final String literal : literals) {
            assertTrue(literal + " must be internal", IPUtils.isInternalAddress(address(literal), policy));
        }
    }

    private static void assertPublic(final AddressPolicy policy, final String... literals) throws Exception {
        for (final String literal : literals) {
            assertFalse(literal + " must be public", IPUtils.isInternalAddress(address(literal), policy));
        }
    }

    /**
     * A configured REMOTE_CALL_SUBNET_BLACKLIST adds ranges to the built-in checks: the
     * built-in ranges stay internal whatever the list says.
     */
    @Test
    public void test_configured_blacklist_adds_to_builtin_checks() throws Exception {
        final AddressPolicy oneRange = policy(new String[]{"203.0.113.0/24"}, null, true);

        assertInternal(oneRange, "10.0.0.1", "::1", "127.0.0.2", "0.0.0.0", "169.254.169.254", "192.168.1.1");
    }

    /**
     * A range on the configured list is refused even when it is public by category.
     */
    @Test
    public void test_configured_blacklist_range_is_refused() throws Exception {
        final AddressPolicy oneRange = policy(new String[]{"203.0.113.0/24"}, null, true);

        assertInternal(oneRange, "203.0.113.5");
        assertPublic(oneRange, "203.0.114.1");
    }

    /**
     * The allow list opens exactly the ranges it names.
     */
    @Test
    public void test_allowlist_opens_only_the_listed_range() throws Exception {
        final AddressPolicy oneRange = policy(null, new String[]{"10.1.0.0/16"}, true);

        assertPublic(oneRange, "10.1.2.3", "10.1.0.0", "10.1.255.255");
        assertInternal(oneRange, "10.2.0.1", "10.0.255.255", "192.168.1.1");
    }

    /**
     * Loopback, unspecified, "this network", link-local and metadata addresses stay internal
     * whatever the allow list names, while the same list does open other ranges.
     */
    @Test
    public void test_allowlist_never_opens_loopback_unspecified_or_metadata() throws Exception {
        final AddressPolicy everything = policy(null,
                new String[]{"127.0.0.0/8", "0.0.0.0/0", "169.254.0.0/16", "::/0", "fd00:ec2::/64", "fe80::/10"}, true);

        assertInternal(everything, "127.0.0.1", "127.0.0.2", "0.0.0.0", "0.1.2.3", "::1", "::",
                "169.254.169.254", "169.254.170.2", "169.254.170.23", "169.254.1.1", "fe80::1", "fd00:ec2::254");
        assertPublic(everything, "10.0.0.1", "fd00:ec2::1", "fc00::1");
    }

    /**
     * An IPv4 catch-all on the allow list opens IPv4 addresses only: the IPv6 space is untouched.
     */
    @Test
    public void test_ipv4_catch_all_on_the_allowlist_does_not_open_ipv6() throws Exception {
        final AddressPolicy ipv4CatchAll = policy(null, new String[]{"0.0.0.0/0"}, true);

        assertPublic(ipv4CatchAll, "10.0.0.1", "192.168.1.1");
        assertInternal(ipv4CatchAll, "fc00::1", "fe80::1", "64:ff9b::808:808");
        // the helper itself keeps the legacy "every address" meaning of 0.0.0.0/0 for other callers
        assertTrue(IPUtils.isIpInCIDR("fc00::1", "0.0.0.0/0"));
        assertFalse(IPUtils.isIpInCIDR("2606:4700:4700::1111", "10.0.0.0/8"));
        assertFalse(IPUtils.isIpInCIDR("10.0.0.1", "fc00::/7"));
    }

    /**
     * An explicit deny entry always refuses, even inside an allowed range: the allow list only
     * opens the built-in ranges.
     */
    @Test
    public void test_denylist_wins_over_allowlist_outside_the_floor() throws Exception {
        final AddressPolicy both = policy(new String[]{"10.1.0.0/16"}, new String[]{"10.1.0.0/16"}, true);
        assertInternal(both, "10.1.0.1");

        final AddressPolicy narrowed = policy(new String[]{"10.0.0.5/32"}, new String[]{"10.0.0.0/8"}, true);
        assertInternal(narrowed, "10.0.0.5");
        assertPublic(narrowed, "10.0.0.4", "10.0.0.6");
    }

    /**
     * An IPv6 allow entry does not reach IPv6 addresses while IPv6 is off.
     */
    @Test
    public void test_ipv6_allowlist_entries_have_no_effect_while_ipv6_is_off() throws Exception {
        final AddressPolicy ipv6Off = policy(null, new String[]{"::/0", "2606:4700::/32"}, false);

        assertInternal(ipv6Off, "2606:4700:4700::1111", "::1", "fc00::1");
    }

    /**
     * Shared address space, benchmarking, protocol-assignment, reserved and "this network"
     * ranges are internal by default.
     */
    @Test
    public void test_builtin_checks_cover_shared_benchmark_and_reserved_ranges() throws Exception {
        final AddressPolicy noLists = policy(null, null, true);

        assertInternal(noLists, "100.64.0.1", "198.18.0.1", "192.0.0.1", "240.0.0.1", "64:ff9b::808:808", "0.1.2.3");
    }

    /**
     * Boundaries of the built-in ranges that are not covered by a JDK category.
     */
    @Test
    public void test_builtin_range_boundaries() throws Exception {
        final AddressPolicy noLists = policy(null, null, true);

        assertPublic(noLists, "100.63.255.255", "100.128.0.0", "198.17.255.255", "198.20.0.0",
                "191.255.255.255", "192.0.1.0", "223.255.255.255", "172.15.255.255", "172.32.0.0",
                "192.167.255.255", "192.169.0.0", "1.0.0.0");
        assertInternal(noLists, "100.64.0.0", "100.127.255.255", "198.18.0.0", "198.19.255.255",
                "192.0.0.0", "192.0.0.255", "240.0.0.0", "255.255.255.255", "172.16.0.0", "172.31.255.255",
                "192.168.0.0", "192.168.255.255", "0.255.255.255");
    }

    /**
     * A missing address, or a missing entry among a host's addresses, is refused: the check
     * fails closed rather than guessing.
     */
    @Test
    public void test_missing_addresses_are_refused() throws Exception {
        final AddressPolicy open = policy(null, null, true);

        assertTrue(IPUtils.isInternalAddress(null, open));
        assertEquals(0, IPUtils.permittedAddresses(null, open).length);
        assertEquals(0, IPUtils.permittedAddresses(new InetAddress[]{address("1.1.1.1"), null}, open).length);
    }

    /**
     * Entries that do not name an address or a range are dropped; the rest are kept in order,
     * a bare address as a single-address range.
     */
    @Test
    public void test_invalid_list_entries_are_skipped() throws Exception {
        final AddressPolicy denied = policy(
                new String[]{"not-a-cidr", " 10.0.0.0/8 ", "203.0.113.7", "1.2.3.4/40", "", "5.6.7.8/-1", null},
                new String[]{"garbage", "2001:db8::/129", "198.51.100.0/0", "2001:db8::7", "2001:db8::/32", "abc/24"}, true);

        assertEquals(List.of("10.0.0.0/8", "203.0.113.7/32"), denied.denied());
        assertEquals(List.of("0.0.0.0/0", "2001:db8::7/128", "2001:db8::/32"), denied.allowed());
        assertInternal(denied, "10.0.0.1", "203.0.113.7");
        assertPublic(denied, "8.8.8.8", "203.0.113.8");

        final AddressPolicy fromNulls = new AddressPolicy(null, null, true);
        assertEquals(List.of(), fromNulls.denied());
        assertEquals(List.of(), fromNulls.allowed());
    }

    /**
     * A scoped link-local address is matched without its interface suffix.
     */
    @Test
    public void test_scoped_ipv6_addresses_are_classified_without_their_scope() throws Exception {
        final byte[] linkLocal = address("fe80::1").getAddress();
        final InetAddress scoped = Inet6Address.getByAddress("fe80::1%7", linkLocal, 7);
        final AddressPolicy ipv6On = policy(null, null, true);

        assertTrue(scoped.getHostAddress().contains("%"));
        assertInternal(ipv6On, scoped.getHostAddress());
        assertTrue(IPUtils.isInternalAddress(scoped, ipv6On));
    }

    /**
     * IPv6 addresses are not reachable unless the IPv6 setting is on, and it is off by default.
     */
    @Test
    public void test_ipv6_addresses_are_refused_by_default() throws Exception {
        assertInternal(policy(null, null, false), ipv6AddressesOnPublicSubnets);
        assertInternal(policy(null, null, false), "::1", "fc00::1");
    }

    /**
     * With the IPv6 setting on, IPv6 addresses follow the same policy as IPv4.
     */
    @Test
    public void test_ipv6_addresses_follow_the_policy_when_allowed() throws Exception {
        final AddressPolicy ipv6On = policy(null, null, true);

        assertPublic(ipv6On, ipv6AddressesOnPublicSubnets);
        assertInternal(ipv6On, "::1", "::", "fd00:ec2::254", "fc00::1", "fe80::1", "64:ff9b::808:808");
    }

    /**
     * A host with addresses in both families keeps working over IPv4 while IPv6 is off.
     */
    @Test
    public void test_dual_stack_hosts_keep_their_ipv4_addresses_when_ipv6_is_off() throws Exception {
        final InetAddress[] resolved = {address("2606:4700:4700::1111"), address("1.1.1.1")};

        final InetAddress[] permitted = IPUtils.permittedAddresses(resolved, policy(null, null, false));

        assertEquals(1, permitted.length);
        assertEquals("1.1.1.1", permitted[0].getHostAddress());

        final InetAddress[] both = IPUtils.permittedAddresses(resolved, policy(null, null, true));
        assertEquals(2, both.length);
    }

    /**
     * A host with IPv6 addresses only is refused while IPv6 is off, and a host with one
     * internal IPv4 address is refused as a whole.
     */
    @Test
    public void test_ipv6_only_hosts_are_refused_when_ipv6_is_off() throws Exception {
        final AddressPolicy ipv6Off = policy(null, null, false);

        assertEquals(0, IPUtils.permittedAddresses(new InetAddress[]{address("2606:4700:4700::1111")}, ipv6Off).length);
        assertEquals(0, IPUtils.permittedAddresses(new InetAddress[]{address("1.1.1.1"), address("10.0.0.1")}, ipv6Off).length);
        assertEquals(0, IPUtils.permittedAddresses(new InetAddress[0], ipv6Off).length);
        assertEquals(0, IPUtils.permittedAddresses(null, ipv6Off).length);
    }

    /**
     * With every check switched off, the resolved set is handed back unfiltered, and a missing
     * set becomes an empty one.
     */
    @Test
    public void test_switching_the_checks_off_permits_every_resolved_address() throws Exception {
        final InetAddress[] resolved = {address("127.0.0.1"), address("::1")};
        IPUtils.disabledIpPrivateSubnet(true);
        try {
            assertEquals(2, IPUtils.permittedAddresses(resolved).length);
            assertEquals(0, IPUtils.permittedAddresses(null).length);
            assertFalse(IPUtils.isInternalAddress(address("127.0.0.1")));
        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
        }
        assertEquals(0, IPUtils.permittedAddresses(resolved).length);
        assertTrue(IPUtils.isInternalAddress(address("127.0.0.1")));
    }

}
