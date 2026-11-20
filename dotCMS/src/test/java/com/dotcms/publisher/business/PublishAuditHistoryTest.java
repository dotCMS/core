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
     * Method to test: {@link PublishAuditHistory#hasEndpointWithoutFailure(String, String)}
     * Given scenario: no entry, then an entry for every kind of failed result, then entries that
     * are not failures
     * Expected result: true only when the endpoint has an entry that is not a failure, so a
     * publisher replaces a failed result and keeps any other one (issue #34434)
     */
    @Test
    public void test_hasEndpointWithoutFailure_onlyForEntriesThatAreNotFailures() {
        final PublishAuditHistory history = new PublishAuditHistory();
        assertFalse(history.hasEndpointWithoutFailure("env-1", "ep-1"));

        for (final PublishAuditStatus.Status failed : new PublishAuditStatus.Status[] {
                PublishAuditStatus.Status.FAILED_TO_PUBLISH, PublishAuditStatus.Status.FAILED_TO_SENT,
                PublishAuditStatus.Status.INVALID_TOKEN, PublishAuditStatus.Status.LICENSE_REQUIRED}) {
            history.addOrUpdateEndpoint("env-1", "ep-1", detail(failed.getCode(), "failed"));
            assertFalse(failed.name(), history.hasEndpointWithoutFailure("env-1", "ep-1"));
        }

        for (final PublishAuditStatus.Status kept : new PublishAuditStatus.Status[] {
                PublishAuditStatus.Status.SUCCESS, PublishAuditStatus.Status.BUNDLE_SAVED_SUCCESSFULLY,
                PublishAuditStatus.Status.PUBLISHING_BUNDLE}) {
            history.addOrUpdateEndpoint("env-1", "ep-1", detail(kept.getCode(), "kept"));
            assertTrue(kept.name(), history.hasEndpointWithoutFailure("env-1", "ep-1"));
        }
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
