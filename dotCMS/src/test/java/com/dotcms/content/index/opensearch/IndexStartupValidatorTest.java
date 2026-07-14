package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.content.index.IndexConfigHelper;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link IndexStartupValidator} endpoint-separation logic.
 *
 * <p>Covers the config-only separation gate ({@link IndexStartupValidator#endpointsAreSeparate()})
 * used at the OS index-creation chokepoint (issue #36419): OS must point to a cluster separate
 * from ES, otherwise the {@code .os} shadow indices must not be created.</p>
 */
public class IndexStartupValidatorTest {

    @After
    public void clearEndpoints() {
        Config.setProperty("ES_ENDPOINTS", null);
        Config.setProperty("OS_ENDPOINTS", null);
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, 0);
    }

    /**
     * Given Scenario: ES and OS are configured with the same address.
     * Expected Result: endpointsAreSeparate() returns false — OS bootstrap must be skipped.
     */
    @Test
    public void test_endpointsAreSeparate_sameAddress_returnsFalse() {
        Config.setProperty("ES_ENDPOINTS", new String[]{"https://localhost:9201"});
        Config.setProperty("OS_ENDPOINTS", new String[]{"https://localhost:9201"});

        assertFalse(IndexStartupValidator.endpointsAreSeparate());
    }

    /**
     * Given Scenario: ES and OS point at different ports on the same host.
     * Expected Result: endpointsAreSeparate() returns true — safe to create OS indices.
     */
    @Test
    public void test_endpointsAreSeparate_differentAddress_returnsTrue() {
        Config.setProperty("ES_ENDPOINTS", new String[]{"https://localhost:9200"});
        Config.setProperty("OS_ENDPOINTS", new String[]{"https://localhost:9201"});

        assertTrue(IndexStartupValidator.endpointsAreSeparate());
    }

    /**
     * Given Scenario: same address, but written with different scheme/formatting.
     * Expected Result: overlap is still detected (normalized to host:port) — returns false.
     */
    @Test
    public void test_endpointsAreSeparate_sameHostPortDifferentScheme_returnsFalse() {
        Config.setProperty("ES_ENDPOINTS", new String[]{"http://localhost:9201"});
        Config.setProperty("OS_ENDPOINTS", new String[]{"https://localhost:9201"});

        assertFalse(IndexStartupValidator.endpointsAreSeparate());
    }

    /**
     * Given Scenario: multi-endpoint ES [a,b] and OS [b,c] share one endpoint.
     * Expected Result: partial overlap is detected — returns false.
     */
    @Test
    public void test_endpointsAreSeparate_multiEndpointPartialOverlap_returnsFalse() {
        Config.setProperty("ES_ENDPOINTS",
                new String[]{"https://es-a:9200", "https://shared:9200"});
        Config.setProperty("OS_ENDPOINTS",
                new String[]{"https://shared:9200", "https://os-c:9200"});

        assertFalse(IndexStartupValidator.endpointsAreSeparate());
    }

    /**
     * Given Scenario: OS config cannot be resolved (blank endpoint trips OSClientConfig validation).
     * Expected Result: endpointsAreSeparate() fails closed — returns false — rather than throwing
     *                  out of the OS bootstrap gate.
     */
    @Test
    public void test_endpointsAreSeparate_unresolvableOsConfig_returnsFalse() {
        Config.setProperty("ES_ENDPOINTS", new String[]{"https://localhost:9200"});
        Config.setProperty("OS_ENDPOINTS", new String[]{""});

        assertFalse(IndexStartupValidator.endpointsAreSeparate());
    }

    /**
     * Given Scenario: Phase 3 (OPENSEARCH_ONLY) with ES and OS resolving to the same address —
     *                 legitimate, since ES is decommissioned and ES_ENDPOINTS is not required.
     * Expected Result: the separation check is skipped — endpointsAreSeparate() returns true.
     */
    @Test
    public void test_endpointsAreSeparate_phase3_checkSkipped_returnsTrue() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, 3);
        Config.setProperty("ES_ENDPOINTS", new String[]{"https://localhost:9201"});
        Config.setProperty("OS_ENDPOINTS", new String[]{"https://localhost:9201"});

        assertTrue(IndexStartupValidator.endpointsAreSeparate());
    }

    /**
     * Given Scenario: ES and OS share the same host:port.
     * Expected Result: assertEndpointsSeparate throws DotRuntimeException naming the overlap.
     */
    @Test
    public void test_assertEndpointsSeparate_overlap_throws() {
        Config.setProperty("ES_ENDPOINTS", new String[]{"https://localhost:9201"});
        Config.setProperty("OS_ENDPOINTS", new String[]{"https://localhost:9201"});

        try {
            IndexStartupValidator.assertEndpointsSeparate(
                    ConfigurableOpenSearchProvider.configFromProperties());
            fail("Expected DotRuntimeException for overlapping ES/OS endpoints");
        } catch (final DotRuntimeException e) {
            assertTrue("message should name the overlap",
                    e.getMessage().contains("point to the same endpoint(s)"));
        }
    }
}
