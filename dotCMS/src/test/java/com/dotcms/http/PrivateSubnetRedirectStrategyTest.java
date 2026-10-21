package com.dotcms.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.http.CircuitBreakerUrl.PrivateSubnetRedirectStrategy;
import java.net.URI;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpCoreContext;
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

    @Test
    public void redirect_to_same_host_and_port_keeps_all_headers() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", 443),
                "https://8.8.8.8/next");

        assertEquals("https://8.8.8.8/next", redirect.getURI().toString());
        assertEquals("Bearer token", redirect.getFirstHeader("Authorization").getValue());
        assertEquals("session=abc", redirect.getFirstHeader("Cookie").getValue());
        assertEquals("Basic cHJveHk=", redirect.getFirstHeader("Proxy-Authorization").getValue());
        assertEquals("application/json", redirect.getFirstHeader("Accept").getValue());
    }

    @Test
    public void redirect_to_explicit_default_port_is_the_same_origin() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", -1),
                "https://8.8.8.8:443/next");

        assertEquals("Bearer token", redirect.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void redirect_to_another_host_drops_credentials_and_keeps_other_headers() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", 443),
                "https://1.1.1.1/next");

        assertCredentialsRemoved(redirect);
        assertEquals("application/json", redirect.getFirstHeader("Accept").getValue());
    }

    @Test
    public void redirect_to_another_port_on_the_same_host_drops_credentials() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", 443),
                "https://8.8.8.8:8443/next");

        assertCredentialsRemoved(redirect);
    }

    @Test
    public void relative_redirect_resolves_against_the_origin_and_keeps_headers() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", 443), "/next");

        assertEquals("8.8.8.8", redirect.getURI().getHost());
        assertEquals("/next", redirect.getURI().getPath());
        assertEquals("Bearer token", redirect.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void cross_host_redirect_of_a_credentials_only_request_is_refused() {
        final HttpGet request = new HttpGet("https://8.8.8.8/start");
        request.addHeader("Authorization", "Bearer token");

        assertThrows(ProtocolException.class, () -> redirectFrom(
                request, originContext("https", "8.8.8.8", 443), "https://1.1.1.1/next"));
    }

    @Test
    public void host_names_are_compared_ignoring_case() {
        assertTrue(PrivateSubnetRedirectStrategy.sameOrigin(
                new HttpHost("Example.COM", -1, "https"), new HttpHost("example.com", 443, "https")));
        assertFalse(PrivateSubnetRedirectStrategy.sameOrigin(
                new HttpHost("example.com", -1, "https"), new HttpHost("example.org", -1, "https")));
        assertFalse(PrivateSubnetRedirectStrategy.sameOrigin(null, new HttpHost("example.com", -1, "https")));
    }

    private static HttpGet getWithCredentials(final String url) {
        final HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("Cookie", "session=abc");
        request.addHeader("Proxy-Authorization", "Basic cHJveHk=");
        request.addHeader("Accept", "application/json");
        return request;
    }

    private static HttpCoreContext originContext(final String scheme, final String host, final int port) {
        final HttpCoreContext context = HttpCoreContext.create();
        context.setTargetHost(new HttpHost(host, port, scheme));
        return context;
    }

    private static HttpUriRequest redirectFrom(final HttpGet request, final HttpCoreContext context,
                                               final String location) throws ProtocolException {
        final HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, 302, "Found"));
        response.addHeader("Location", location);
        return new PrivateSubnetRedirectStrategy().getRedirect(request, response, context);
    }

    private static void assertCredentialsRemoved(final HttpUriRequest redirect) {
        for (final String name : new String[]{"Authorization", "Cookie", "Proxy-Authorization"}) {
            final Header header = redirect.getFirstHeader(name);
            assertNull(name + " must not follow a redirect to another origin", header);
        }
    }
}
