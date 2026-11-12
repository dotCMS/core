package com.dotcms.publisher.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link PublishAuditHistory}: the per-endpoint lookup used to decide whether a
 * publisher must record an endpoint's result, and the serialized form that must not change
 * (issue #34434).
 */
public class PublishAuditHistoryTest {

    private static EndpointDetail detail(final int status, final String info) {
        final EndpointDetail detail = new EndpointDetail();
        detail.setStatus(status);
        detail.setInfo(info);
        return detail;
    }

    /**
     * Method to test: {@link PublishAuditHistory#hasEndpoint(String, String)}
     * Given scenario: the history has no entry for the environment
     * Expected result: false
     */
    @Test
    public void test_hasEndpoint_absentGroup_isFalse() {
        assertFalse(new PublishAuditHistory().hasEndpoint("env-1", "ep-1"));
    }

    /**
     * Method to test: {@link PublishAuditHistory#hasEndpoint(String, String)}
     * Given scenario: the environment is present but holds only another endpoint
     * Expected result: false for the missing endpoint, true for the recorded one
     */
    @Test
    public void test_hasEndpoint_groupWithoutThatEndpoint_isFalse() {
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("env-1", "ep-static", detail(14, "saved"));

        assertFalse(history.hasEndpoint("env-1", "ep-http"));
        assertTrue(history.hasEndpoint("env-1", "ep-static"));
    }

    /**
     * Method to test: {@link PublishAuditHistory#hasEndpoint(String, String)}
     * Given scenario: the same endpoint id under a different environment
     * Expected result: false, the lookup is per environment
     */
    @Test
    public void test_hasEndpoint_sameEndpointOtherGroup_isFalse() {
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("env-1", "ep-1", detail(14, "saved"));

        assertFalse(history.hasEndpoint("env-2", "ep-1"));
    }

    /**
     * Method to test: {@link PublishAuditHistory#getSerialized()} and
     * {@link PublishAuditHistory#getObjectFromString(String)}
     * Given scenario: a history with two endpoints is serialized and read back
     * Expected result: the same environments, endpoints, statuses and info (the stored shape is
     * unchanged by the fix)
     */
    @Test
    public void test_serializedRoundTrip_keepsEndpoints() {
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("env-1", "ep-static", detail(14, "saved"));
        history.addOrUpdateEndpoint("env-1", "ep-http", detail(8, "failed"));

        final PublishAuditHistory copy =
                PublishAuditHistory.getObjectFromString(history.getSerialized());

        assertEquals(1, copy.getEndpointsMap().size());
        assertEquals(2, copy.getEndpointsMap().get("env-1").size());
        assertEquals(14, copy.getEndpointsMap().get("env-1").get("ep-static").getStatus());
        assertEquals("failed", copy.getEndpointsMap().get("env-1").get("ep-http").getInfo());
    }
}
