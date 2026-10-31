package com.dotcms.rest.api.v1.pagescanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dotmarketing.util.Config;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PageScannerServiceUrl}, the check applied to the Page Scanner service
 * URL read from the {@code dotPageScanner-config} App before any request is sent to it.
 *
 * <p>Accepted: an absolute {@code https} URL with a host and no user info, query or fragment,
 * whose host is not a loopback, link-local, private or otherwise internal address unless
 * {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS} is enabled. The accepted value is returned without
 * one trailing slash. Anything else is refused.</p>
 *
 * <p>{@code 203.0.113.10} is a documentation-range public address, so the accepted cases need
 * no name lookup.</p>
 */
class PageScannerServiceUrlTest {

    private static final String PRIVATE_SUBNETS = "ALLOW_ACCESS_TO_PRIVATE_SUBNETS";

    private String previousPrivateSubnets;

    @BeforeEach
    void saveSetting() {
        previousPrivateSubnets = Config.getStringProperty(PRIVATE_SUBNETS, null);
    }

    @AfterEach
    void restoreSetting() {
        Config.setProperty(PRIVATE_SUBNETS, previousPrivateSubnets);
    }

    // Given an https URL whose host is a public address
    // When it is validated
    // Then the value is returned as the base URL
    @Test
    void validate_httpsPublicAddress_returnsBaseUrl() {
        assertEquals(Optional.of("https://203.0.113.10"),
                PageScannerServiceUrl.validate("https://203.0.113.10"));
    }

    // Given an https URL with a port, a path and a trailing slash
    // When it is validated
    // Then the port and path are kept and the trailing slash is removed
    @Test
    void validate_portPathAndTrailingSlash_keepsPortAndPathWithoutSlash() {
        assertEquals(Optional.of("https://203.0.113.10:8443/scanner"),
                PageScannerServiceUrl.validate("https://203.0.113.10:8443/scanner/"));
    }

    // Given an upper-case HTTPS scheme
    // When it is validated
    // Then it is accepted
    @Test
    void validate_upperCaseScheme_isAccepted() {
        assertEquals(Optional.of("HTTPS://203.0.113.10"),
                PageScannerServiceUrl.validate("HTTPS://203.0.113.10"));
    }

    // Given no value
    // When it is validated
    // Then it is refused without an exception
    @Test
    void validate_null_returnsEmpty() {
        assertEquals(Optional.empty(), PageScannerServiceUrl.validate(null));
    }

    // Given a plain http URL
    // When it is validated
    // Then it is refused
    @Test
    void validate_httpScheme_returnsEmpty() {
        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("http://203.0.113.10"));
    }

    // Given a value without a scheme
    // When it is validated
    // Then it is refused without an exception
    @Test
    void validate_missingScheme_returnsEmpty() {
        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("scanner.test/v1"));
    }

    // Given a value that is not a valid URI
    // When it is validated
    // Then it is refused without an exception
    @Test
    void validate_unparsableValue_returnsEmpty() {
        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https://a b.test"));
    }

    // Given an https URL without a host
    // When it is validated
    // Then it is refused
    @Test
    void validate_missingHost_returnsEmpty() {
        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https:///scanner"));
    }

    // Given an https URL with user info
    // When it is validated
    // Then it is refused
    @Test
    void validate_userInfo_returnsEmpty() {
        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https://user@203.0.113.10"));
    }

    // Given an https URL with a query
    // When it is validated
    // Then it is refused, since a path appended to it would land in the query
    @Test
    void validate_query_returnsEmpty() {
        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https://203.0.113.10?x=1"));
    }

    // Given an https URL with a fragment
    // When it is validated
    // Then it is refused
    @Test
    void validate_fragment_returnsEmpty() {
        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https://203.0.113.10#x"));
    }

    // Given an https URL whose host is a loopback address
    // When it is validated with the default settings
    // Then it is refused
    @Test
    void validate_loopbackAddress_returnsEmpty() {
        Config.setProperty(PRIVATE_SUBNETS, false);

        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https://127.0.0.1"));
    }

    // Given an https URL whose host name resolves to a loopback address
    // When it is validated with the default settings
    // Then it is refused
    @Test
    void validate_hostNameResolvingToLoopback_returnsEmpty() {
        Config.setProperty(PRIVATE_SUBNETS, false);

        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https://localhost"));
    }

    // Given an https URL whose host is the IPv6 loopback address in brackets
    // When it is validated with the default settings
    // Then it is refused
    @Test
    void validate_ipv6LoopbackAddress_returnsEmpty() {
        Config.setProperty(PRIVATE_SUBNETS, false);

        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https://[::1]"));
    }

    // Given an https URL whose host name does not resolve (.invalid is a reserved name)
    // When it is validated with the default settings
    // Then it is refused
    @Test
    void validate_unresolvableHost_returnsEmpty() {
        Config.setProperty(PRIVATE_SUBNETS, false);

        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("https://scanner.invalid"));
    }

    // Given ALLOW_ACCESS_TO_PRIVATE_SUBNETS=true and an https URL on a loopback address
    // When it is validated
    // Then it is accepted
    @Test
    void validate_internalAddressWithPrivateSubnetsAllowed_returnsBaseUrl() {
        Config.setProperty(PRIVATE_SUBNETS, true);

        assertEquals(Optional.of("https://127.0.0.1"),
                PageScannerServiceUrl.validate("https://127.0.0.1"));
    }

    // Given ALLOW_ACCESS_TO_PRIVATE_SUBNETS=true and a plain http URL on a loopback address
    // When it is validated
    // Then it is still refused
    @Test
    void validate_httpWithPrivateSubnetsAllowed_returnsEmpty() {
        Config.setProperty(PRIVATE_SUBNETS, true);

        assertEquals(Optional.empty(), PageScannerServiceUrl.validate("http://127.0.0.1"));
    }
}
