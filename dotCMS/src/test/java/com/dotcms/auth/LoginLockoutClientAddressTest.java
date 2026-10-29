package com.dotcms.auth;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoginLockout#clientAddress(HttpServletRequest)}, which turns a request's remote
 * address into the source that login attempts are counted under. No database is involved.
 */
public class LoginLockoutClientAddressTest {

    private static final String ADDRESS = "192.0.2.10";

    private static HttpServletRequest requestFrom(final String address) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(address);
        return request;
    }

    /**
     * IPv6 addresses count under their /64 network, which one host can usually pick any address from; an
     * IPv4-mapped address counts as the IPv4 address.
     */
    @Test
    public void test_ipv6_addresses_are_grouped_by_64() {
        assertEquals("2001:db8:1:2::/64", LoginLockout.clientAddress(requestFrom("2001:db8:1:2:aaaa:bbbb:cccc:dddd")));
        assertEquals("2001:db8:1:2::/64", LoginLockout.clientAddress(requestFrom("2001:db8:1:2::1")));
        assertNotEquals("2001:db8:1:2::/64", LoginLockout.clientAddress(requestFrom("2001:db8:1:3::1")));
        assertEquals("fe80::/64", LoginLockout.clientAddress(requestFrom("fe80::1%eth0")));
        assertEquals("192.0.2.30", LoginLockout.clientAddress(requestFrom("::ffff:192.0.2.30")));
        assertEquals("not-an-address:x", LoginLockout.clientAddress(requestFrom("not-an-address:x")));
    }

    /**
     * The client address falls back to a fixed source outside a request, and is cut to the column length.
     */
    @Test
    public void test_client_address() {
        assertEquals(LoginLockout.UNKNOWN_SOURCE, LoginLockout.clientAddress((HttpServletRequest) null));
        assertEquals(ADDRESS, LoginLockout.clientAddress(requestFrom(ADDRESS)));
        assertEquals(64, LoginLockout.clientAddress(requestFrom("f".repeat(100))).length());
    }
}
