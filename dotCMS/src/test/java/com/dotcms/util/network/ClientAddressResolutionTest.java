package com.dotcms.util.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.IntrospectionUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Runs Tomcat's {@link RemoteIpValve} with the settings shipped in the container's
 * {@code server.xml} and {@code setenv.sh}, and checks which address dotCMS code then sees from
 * {@code request.getRemoteAddr()}. Every client-address decision in dotCMS (API token network
 * limits, security and access logs, rules, geolocation) reads that value, so these settings
 * decide whether an {@code X-Forwarded-For} header is believed.
 */
public class ClientAddressResolutionTest {

    private static final String VALVE_CLASS = RemoteIpValve.class.getName();

    private static String shippedRemoteIpHeader;
    private static String shippedInternalProxies;
    private static String shippedTrustedProxies;
    private static String setenvInternalProxies;

    /** Reads the valve attributes from server.xml, resolved as Tomcat does when no variable is set. */
    @BeforeClass
    public static void readShippedConfig() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        final NodeList valves = factory.newDocumentBuilder()
                .parse(containerFile("conf/server.xml")).getElementsByTagName("Valve");
        Element remoteIpValve = null;
        for (int i = 0; i < valves.getLength(); i++) {
            final Element valve = (Element) valves.item(i);
            if (VALVE_CLASS.equals(valve.getAttribute("className"))) {
                remoteIpValve = valve;
            }
        }
        assertTrue("server.xml declares no RemoteIpValve", remoteIpValve != null);
        shippedRemoteIpHeader = resolve(remoteIpValve.getAttribute("remoteIpHeader"));
        shippedInternalProxies = resolve(remoteIpValve.getAttribute("internalProxies"));
        shippedTrustedProxies = resolve(remoteIpValve.getAttribute("trustedProxies"));

        final String setenv = new String(Files.readAllBytes(containerFile("bin/setenv.sh").toPath()),
                StandardCharsets.UTF_8);
        final Matcher matcher = Pattern
                .compile("CMS_REMOTEIP_INTERNALPROXIES:-\"([^\"]*)\"").matcher(setenv);
        assertTrue("setenv.sh sets no CMS_REMOTEIP_INTERNALPROXIES default", matcher.find());
        setenvInternalProxies = matcher.group(1);
    }

    /**
     * Method to test: the shipped RemoteIpValve settings
     * Given scenario: a client connects directly from a public address and sends its own
     * X-Forwarded-For naming another address
     * Expected result: the header is ignored and the connecting address is used
     */
    @Test
    public void testRemoteAddr_WhenPublicPeerSendsForwardedFor_ShouldUseConnectingAddress() throws Exception {
        assertEquals("203.0.113.9", resolvedAddress(shippedValve(), "203.0.113.9", "1.2.3.4"));
    }

    /**
     * Method to test: the shipped RemoteIpValve settings
     * Given scenario: a load balancer on a private address forwards a request and adds the
     * client's address to X-Forwarded-For
     * Expected result: the client address from the header is used
     */
    @Test
    public void testRemoteAddr_WhenTrustedProxyForwards_ShouldUseClientAddressFromHeader() throws Exception {
        assertEquals("198.51.100.7", resolvedAddress(shippedValve(), "10.0.0.1", "198.51.100.7"));
    }

    /**
     * Method to test: the shipped RemoteIpValve settings
     * Given scenario: the client sends its own X-Forwarded-For and a load balancer on a private
     * address appends the real client address
     * Expected result: the client-supplied entry is ignored; the entry the load balancer added wins
     */
    @Test
    public void testRemoteAddr_WhenTrustedProxyAppends_ShouldIgnoreClientSuppliedEntry() throws Exception {
        assertEquals("198.51.100.7",
                resolvedAddress(shippedValve(), "10.0.0.1", "1.2.3.4, 198.51.100.7"));
    }

    /**
     * Method to test: the shipped RemoteIpValve settings
     * Given scenario: the connection comes from a private address that does not rewrite
     * X-Forwarded-For (a Docker bridge gateway, a Kubernetes service that rewrites the source
     * address, or a client on the same private network), and the header names another address
     * Expected result: CURRENT BEHAVIOUR. Every private range is trusted as a proxy by default, so
     * the address named in the header is used. Deployments where clients can reach dotCMS from a
     * private address should narrow CMS_REMOTEIP_INTERNALPROXIES to their own proxies.
     */
    @Test
    public void testRemoteAddr_WhenPrivatePeerDoesNotRewriteHeader_ShouldUseHeaderAddress() throws Exception {
        assertEquals("1.2.3.4", resolvedAddress(shippedValve(), "172.17.0.1", "1.2.3.4"));
    }

    /**
     * Method to test: CMS_REMOTEIP_INTERNALPROXIES narrowed to the deployment's own proxy
     * Given scenario: a private address that is not the configured proxy sends X-Forwarded-For
     * Expected result: the header is ignored and the connecting address is used, while the
     * configured proxy is still honoured
     */
    @Test
    public void testRemoteAddr_WhenInternalProxiesNarrowed_ShouldIgnoreOtherPrivatePeers() throws Exception {
        final RemoteIpValve valve = valve("10\\.0\\.0\\.1", shippedTrustedProxies);
        assertEquals("172.17.0.1", resolvedAddress(valve, "172.17.0.1", "1.2.3.4"));
        assertEquals("198.51.100.7", resolvedAddress(valve, "10.0.0.1", "198.51.100.7"));
    }

    /**
     * Method to test: CMS_REMOTEIP_TRUSTEDPROXIES
     * Given scenario: a CDN on a public address sits in front of the private load balancer;
     * X-Forwarded-For carries the client and then the CDN
     * Expected result: without the setting the CDN address is used; with the CDN listed the
     * client address is used
     */
    @Test
    public void testRemoteAddr_WhenCdnListedAsTrustedProxy_ShouldSkipCdnEntry() throws Exception {
        final String chain = "198.51.100.7, 203.0.113.10";
        assertEquals("203.0.113.10", resolvedAddress(shippedValve(), "10.0.0.1", chain));
        assertEquals("198.51.100.7", resolvedAddress(
                valve(shippedInternalProxies, "203\\.0\\.113\\.10"), "10.0.0.1", chain));
    }

    /**
     * Method to test: the internalProxies default in setenv.sh, which the Docker image exports
     * and which therefore replaces the server.xml default at run time
     * Given scenario: the same peers checked against both defaults
     * Expected result: both defaults trust and refuse the same peers
     */
    @Test
    public void testInternalProxiesDefault_WhenSetenvExportsIt_ShouldMatchServerXml() {
        final Pattern serverXml = Pattern.compile(shippedInternalProxies);
        final Pattern setenv = Pattern.compile(setenvInternalProxies);
        for (final String peer : List.of("10.1.2.3", "127.0.0.1", "192.168.0.1", "172.16.0.1",
                "172.31.255.255", "172.32.0.1", "169.254.1.1", "100.64.0.1", "100.127.255.255",
                "100.128.0.1", "100.63.255.255", "203.0.113.9", "8.8.8.8", "::1", "0:0:0:0:0:0:0:1")) {
            assertEquals(peer, serverXml.matcher(peer).matches(), setenv.matcher(peer).matches());
        }
    }

    private static RemoteIpValve shippedValve() {
        return valve(shippedInternalProxies, shippedTrustedProxies);
    }

    private static RemoteIpValve valve(final String internalProxies, final String trustedProxies) {
        final RemoteIpValve valve = new RemoteIpValve();
        valve.setRemoteIpHeader(shippedRemoteIpHeader);
        valve.setInternalProxies(internalProxies);
        valve.setTrustedProxies(trustedProxies);
        return valve;
    }

    /** Sends a request from {@code peer} with the given X-Forwarded-For and returns the address the application sees. */
    private static String resolvedAddress(final RemoteIpValve valve, final String peer,
            final String forwardedFor) throws Exception {
        final String[] seen = new String[1];
        valve.setNext(new ValveBase() {
            @Override
            public void invoke(final Request request, final Response response) {
                seen[0] = request.getRemoteAddr();
            }
        });
        final Request request = new Request(new Connector());
        request.setCoyoteRequest(new org.apache.coyote.Request());
        request.setRemoteAddr(peer);
        request.setRemoteHost(peer);
        request.getCoyoteRequest().getMimeHeaders().addValue("X-Forwarded-For").setString(forwardedFor);
        valve.invoke(request, null);
        return seen[0];
    }

    /** Resolves {@code ${VAR:-default}} the way Tomcat does when the variable is not set. */
    private static String resolve(final String attribute) {
        return IntrospectionUtils.replaceProperties(attribute, null, null,
                ClientAddressResolutionTest.class.getClassLoader());
    }

    /** Finds a file under the module's tomcat9 container overlay, relative to the compiled test classes. */
    private static File containerFile(final String path) throws Exception {
        final File testClasses = new File(ClientAddressResolutionTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        final File file = new File(testClasses,
                "../../src/main/resources/container/tomcat9/" + path).getCanonicalFile();
        assertTrue(path + " not found at " + file, file.isFile());
        return file;
    }
}
