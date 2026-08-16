package com.dotcms.enterprise.cluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.cluster.bean.Server;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link ClusterFactory#shouldRewire}, the decision that governs whether a
 * heartbeat re-runs the cluster rewire.
 *
 * This is the logic that guarantees a failed cache-transport init keeps being retried (issue
 * #36803). It could not be exercised against a live cluster: {@code ServerHeartbeatJob.execute()}
 * calls {@code LicenseUtil.updateLicenseHeartbeat()} before {@code rewireClusterIfNeeded()}, and
 * that throws when the database is down, so the only way to move the failure counter is for the
 * database to be healthy while {@code setCluster()}/{@code testCluster()} fails -- a window that
 * resisted local reproduction. Hence these tests on the extracted predicate.
 */
public class ClusterFactoryRewireTest {

    private static final long NO_PENDING_FAILURES = 0L;

    private static Server server(final String id) {
        return Server.builder().withServerId(id).withIpAddress("127.0.0." + id.length()).build();
    }

    private static final Server ME = server("me");
    private static final Server PEER = server("peer");

    /**
     * Method to test: {@link ClusterFactory#shouldRewire}
     * Given Scenario: membership is unchanged, this server is in the alive set, and no rewire
     * failure is pending -- the steady state of a healthy cluster on every heartbeat.
     * Expected Result: no rewire. Rewiring here is what produced the churn measured in #36544.
     */
    @Test
    public void steadyStateDoesNotRewire() {
        final List<Server> alive = Arrays.asList(ME, PEER);

        assertFalse(ClusterFactory.shouldRewire(alive, Arrays.asList(ME, PEER), ME,
                NO_PENDING_FAILURES));
    }

    /**
     * Method to test: {@link ClusterFactory#shouldRewire}
     * Given Scenario: a previous rewire failed, so KNOWN_SERVERS was never advanced, and
     * membership has since settled back to exactly that stale set.
     * Expected Result: rewire anyway. This is the regression guard for the retry clause added in
     * #36803 -- the membership comparison alone reports "nothing changed", so without the pending
     * failure check the transport would stay broken forever, the failure counter could never
     * return to zero, and the cache-transport health check would report unhealthy indefinitely.
     */
    @Test
    public void pendingFailureForcesRetryEvenWhenMembershipLooksUnchanged() {
        final List<Server> alive = Arrays.asList(ME, PEER);
        final List<Server> knownServers = Arrays.asList(ME, PEER);

        // sanity: membership genuinely looks unchanged, so only the failure counter can trigger it
        assertFalse("precondition: membership must look identical for this test to mean anything",
                ClusterFactory.shouldRewire(alive, knownServers, ME, NO_PENDING_FAILURES));

        assertTrue(ClusterFactory.shouldRewire(alive, knownServers, ME, 1L));
        assertTrue("still retried after several failures",
                ClusterFactory.shouldRewire(alive, knownServers, ME, 7L));
    }

    /**
     * Method to test: {@link ClusterFactory#shouldRewire}
     * Given Scenario: a node joined since the last successful rewire.
     * Expected Result: rewire, so the transport is wired to the new membership.
     */
    @Test
    public void membershipChangeRewires() {
        assertTrue("a server joined",
                ClusterFactory.shouldRewire(Arrays.asList(ME, PEER), Collections.singletonList(ME),
                        ME, NO_PENDING_FAILURES));

        assertTrue("a server left",
                ClusterFactory.shouldRewire(Collections.singletonList(ME), Arrays.asList(ME, PEER),
                        ME, NO_PENDING_FAILURES));
    }

    /**
     * Method to test: {@link ClusterFactory#shouldRewire}
     * Given Scenario: this server is absent from the alive set, which is how a node that lost its
     * own registration looks.
     * Expected Result: rewire, so it registers itself again.
     */
    @Test
    public void missingSelfRewires() {
        final List<Server> aliveWithoutMe = Collections.singletonList(PEER);

        assertTrue(ClusterFactory.shouldRewire(aliveWithoutMe, aliveWithoutMe, ME,
                NO_PENDING_FAILURES));
    }

    /**
     * Method to test: {@link ClusterFactory#shouldRewire}
     * Given Scenario: the very first heartbeat, when KNOWN_SERVERS is still the empty list it is
     * initialized to and this server is already alive.
     * Expected Result: rewire, so the cluster is wired at least once.
     */
    @Test
    public void firstHeartbeatRewires() {
        assertTrue(ClusterFactory.shouldRewire(Collections.singletonList(ME),
                Collections.emptyList(), ME, NO_PENDING_FAILURES));
    }
}
