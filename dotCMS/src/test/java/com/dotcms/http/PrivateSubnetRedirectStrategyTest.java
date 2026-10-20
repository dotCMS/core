package com.dotcms.http;

import static org.junit.Assert.assertThrows;

import com.dotcms.http.CircuitBreakerUrl.PrivateSubnetRedirectStrategy;
import java.net.URI;
import org.apache.http.ProtocolException;
import org.junit.Test;

/**
 * Every redirect target followed by {@link CircuitBreakerUrl} must pass the same private-subnet
 * check as the first URL.
 */
public class PrivateSubnetRedirectStrategyTest {

    @Test
    public void redirects_to_internal_addresses_are_refused() {
        for (final String target : new String[]{
                "http://127.0.0.1:8080/", "http://169.254.169.254/latest/meta-data/",
                "https://10.0.0.1/", "http://[::1]/", "http://192.168.1.10/"}) {
            final URI location = URI.create(target);
            assertThrows(target, ProtocolException.class,
                    () -> PrivateSubnetRedirectStrategy.checkRedirectTarget(location, false, location.getScheme()));
        }
    }

    @Test
    public void redirects_to_non_http_schemes_are_refused() {
        assertThrows(ProtocolException.class,
                () -> PrivateSubnetRedirectStrategy.checkRedirectTarget(URI.create("file:///etc/passwd"), true, "https"));
    }

    @Test
    public void internal_redirects_are_allowed_only_when_private_subnets_are_allowed() throws Exception {
        PrivateSubnetRedirectStrategy.checkRedirectTarget(URI.create("http://10.0.0.1/"), true, "http");
    }

    @Test
    public void redirects_to_public_addresses_are_allowed() throws Exception {
        PrivateSubnetRedirectStrategy.checkRedirectTarget(URI.create("https://8.8.8.8/"), false, "https");
    }

    @Test
    public void https_redirects_to_http_are_refused() {
        assertThrows(ProtocolException.class,
                () -> PrivateSubnetRedirectStrategy.checkRedirectTarget(URI.create("http://8.8.8.8/"), false, "https"));
    }

    @Test
    public void http_redirects_to_public_http_are_allowed() throws Exception {
        PrivateSubnetRedirectStrategy.checkRedirectTarget(URI.create("http://8.8.8.8/"), false, "http");
    }
}
