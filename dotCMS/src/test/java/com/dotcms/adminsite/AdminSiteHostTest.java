package com.dotcms.adminsite;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Exercises admin-host matching for IPv6 literals, IPv4 addresses and DNS names.
 * Configuration accessors are stubbed; host parsing and request eligibility use
 * the real implementation without database or external-service dependencies.
 */
@RunWith(Parameterized.class)
public class AdminSiteHostTest {

    private final String description;
    private final String adminUrl;
    private final String[] adminDomains;
    private final String host;
    private final boolean expected;
    private AdminSiteAPIImpl api;

    /**
     * Defines allowed and rejected hosts without adding IPv6 to the trusted defaults.
     * Each case is checked through both the string and HTTP-request entry points.
     *
     * @return cases containing a description, canonical URL, trusted domains, host and expected result
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> hosts() {
        final String[] noDomains = new String[0];
        final String[] defaults = AdminSiteAPI._ADMIN_SITE_REQUEST_DOMAINS_DEFAULT.toArray(new String[0]);
        return Arrays.asList(new Object[][] {
                {"canonical IPv6 without port", "https://[::1]:8443", noDomains, "[::1]", true},
                {"canonical IPv6 with port", "https://[::1]:8443", noDomains, "[::1]:8443", true},
                {"canonical IPv6 with different port", "https://[::1]:8443", noDomains, "[::1]:8080", true},
                {"bare IPv6 literal", "https://[::1]:8443", noDomains, "::1", true},
                {"canonical non-loopback IPv6", "https://[2001:db8::1]:8443", noDomains,
                        "[2001:db8::1]:8080", true},
                {"case-insensitive IPv6", "https://[2001:db8::a]:8443", noDomains,
                        "[2001:DB8::A]:8443", true},
                {"bare IPv6 trusted domain", "https://admin.example.com", new String[] {"2001:db8::1"},
                        "[2001:db8::1]:8443", true},
                {"bracketed IPv6 trusted domain", "https://admin.example.com", new String[] {"[2001:db8::1]"},
                        "[2001:db8::1]:8443", true},
                {"bracketed domain with bare IPv6 input", "https://admin.example.com",
                        new String[] {"[2001:db8::1]"}, "2001:db8::1", true},
                {"different canonical IPv6", "https://[2001:db8::1]:8443", noDomains,
                        "[2001:db8::2]:8443", false},
                {"IPv6 prefix is not an exact match", "https://[2001:db8::1]:8443", noDomains,
                        "[2001:db8::10]:8443", false},
                {"different trusted IPv6", "https://admin.example.com", new String[] {"2001:db8::1"},
                        "[2001:db8::2]:8443", false},
                {"IPv6 does not support DNS subdomain matching", "https://admin.example.com",
                        new String[] {"::1"}, "attacker.::1", false},
                {"IPv6 loopback is not implicitly trusted", "https://admin.example.com", defaults,
                        "[::1]:8443", false},
                {"missing closing bracket", "https://[::1]:8443", noDomains, "[::1", false},
                {"trailing bracket garbage", "https://[::1]:8443", noDomains, "[::1]garbage", false},
                {"empty bracketed host", "https://[::1]:8443", noDomains, "[]:8443", false},
                {"empty IPv6 port", "https://[::1]:8443", noDomains, "[::1]:", false},
                {"nonnumeric IPv6 port", "https://[::1]:8443", noDomains, "[::1]:invalid", false},
                {"extra port delimiter", "https://[::1]:8443", noDomains, "[::1]:8443:80", false},
                {"canonical IPv4 without port", "https://192.0.2.1:8443", noDomains, "192.0.2.1", true},
                {"canonical IPv4 with port", "https://192.0.2.1:8443", noDomains, "192.0.2.1:8080", true},
                {"different IPv4", "https://192.0.2.1:8443", noDomains, "192.0.2.2:8443", false},
                {"canonical DNS without port", "https://admin.example.com", noDomains, "admin.example.com", true},
                {"canonical DNS with port", "https://admin.example.com", noDomains, "admin.example.com:8443", true},
                {"case-insensitive DNS", "https://admin.example.com", noDomains, "ADMIN.EXAMPLE.COM:8443", true},
                {"trusted DNS subdomain", "https://admin.example.com", new String[] {"trusted.example"},
                        "child.trusted.example:8443", true},
                {"DNS suffix without dot boundary", "https://admin.example.com", new String[] {"trusted.example"},
                        "nottrusted.example:8443", false},
                {"DNS suffix attack", "https://admin.example.com", new String[] {"trusted.example"},
                        "trusted.example.attacker.test:8443", false},
                {"default IPv4 loopback", "https://admin.example.com", defaults, "127.0.0.1:8443", true},
                {"default localhost", "https://admin.example.com", defaults, "localhost:8443", true},
                {"missing Host", "https://admin.example.com", noDomains, null, false},
                {"empty Host", "https://admin.example.com", noDomains, "", false}
        });
    }

    /**
     * Stores one host-matching scenario.
     *
     * @param description the case label used in assertion messages
     * @param adminUrl the canonical admin URL
     * @param adminDomains the explicitly trusted host list
     * @param host the host to check, optionally including a port
     * @param expected whether the host should be eligible
     */
    public AdminSiteHostTest(final String description, final String adminUrl,
            final String[] adminDomains, final String host, final boolean expected) {
        this.description = description;
        this.adminUrl = adminUrl;
        this.adminDomains = adminDomains;
        this.host = host;
        this.expected = expected;
    }

    /** Provides fixed configuration while leaving host matching and request checks real. */
    @Before
    public void setUp() {
        api = spy(new AdminSiteAPIImpl());
        doReturn(adminUrl).when(api).getAdminSiteUrl();
        doReturn(adminDomains).when(api).getAdminDomains();
        doReturn(true).when(api).isAdminSiteEnabled();
    }

    /** The host-only API must preserve IPv6 addresses and match the configured host. */
    @Test
    public void isAdminSite_matchesHostWithoutCorruptingIpv6() {
        assertEquals(description, expected, api.isAdminSite(host));
    }

    /** The HTTP-request API must apply the same matching and record the resulting eligibility. */
    @Test
    public void isAdminSite_matchesRequestHostWithoutCorruptingIpv6() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("host")).thenReturn(host);

        assertEquals(description, expected, api.isAdminSite(request));
        verify(request).setAttribute(AdminSiteAPI._ADMIN_SITE_HOST_REQUESTED, expected);
    }
}
