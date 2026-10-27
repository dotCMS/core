package com.dotcms.rest.api.v1.cluster;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.Serializable;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Builds the {@code GET /api/v1/cluster/nodes} entity: every alive server, from the database,
 * plus whatever each one answered to a single cluster status request.
 *
 * @author hassandotcms
 */
public class ClusterNodesHelper {

    private final ServerAPI serverAPI;
    private final Supplier<CacheTransport> transport;
    private final Clock clock;

    /** Uses this server's server API, cache transport and the system clock. */
    public ClusterNodesHelper() {
        this(APILocator.getServerAPI(), () -> CacheLocator.getCacheAdministrator().getTransport(),
                Clock.systemUTC());
    }

    /**
     * Creates the helper with the given collaborators.
     *
     * @param serverAPI reads the alive servers and this server's id
     * @param transport supplies the cache transport that sends the cluster status request
     * @param clock     the clock heartbeat ages are measured against
     */
    ClusterNodesHelper(final ServerAPI serverAPI, final Supplier<CacheTransport> transport, final Clock clock) {
        this.serverAPI = serverAPI;
        this.transport = transport;
        this.clock = clock;
    }

    /**
     * Returns how long a cluster status request waits for replies: {@code CLUSTER_SERVER_THREAD_SLEEP}
     * when set, otherwise one second per alive server with a two-second minimum. Both the old
     * Network tab ({@code GET /api/cluster/getNodesStatus/}) and this endpoint use it, so they
     * always wait the same time.
     *
     * @param aliveServers the number of servers with a recent heartbeat
     * @return the wait window in milliseconds
     */
    public static int statusWaitMillis(final int aliveServers) {
        return Config.getIntProperty("CLUSTER_SERVER_THREAD_SLEEP", Math.max(2000, aliveServers * 1000));
    }

    /**
     * Lists the alive servers in database order (startup time, then server id), each with its
     * identity, whether it answered this call's status request, its status and, when it answered,
     * the typed sections of its report.
     *
     * <p>Exactly one cluster status request is sent, and it waits the same window as the old
     * Network tab: {@code CLUSTER_SERVER_THREAD_SLEEP}, by default one second per server with a
     * two-second minimum. Replies from servers that are not in the alive list are ignored, and a
     * transport that returns no replies at all ({@code null}) means no server answered.</p>
     *
     * @return the servers of the cluster
     * @throws DotRuntimeException if the servers can't be read from the database; the database
     *         error is logged and not passed on, so the response is a 500 that carries no SQL
     */
    public ClusterNodesView nodes() {
        final List<Server> alive;
        try {
            alive = serverAPI.getAliveServers();
        } catch (final DotDataException e) {
            Logger.error(this, "Could not read the cluster servers: " + e.getMessage(), e);
            throw new DotRuntimeException("Could not read the cluster servers");
        }
        final Map<String, Serializable> replies =
                transport.get().validateCacheInCluster(statusWaitMillis(alive.size()));
        final Map<String, Serializable> answered = replies != null ? replies : Map.of();
        final long threshold = ClusterNodeMapper.staleThresholdSeconds();
        final long now = clock.millis();

        final List<ClusterNodeView> nodes = alive.stream()
                .map(server -> node(server, answered.get(server.getServerId()), threshold, now))
                .collect(Collectors.toList());
        return new ClusterNodesView(serverAPI.readServerId(), nodes);
    }

    /**
     * Builds one node: identity from the server's database row, and, when it replied, the typed
     * sections of its report.
     *
     * @param server    the server's database row
     * @param reply     its reply to this call's status request, or {@code null} when it didn't answer
     * @param threshold the heartbeat age, in seconds, after which a heartbeat is stale
     * @param now       the current time in milliseconds
     * @return the node
     */
    @SuppressWarnings("unchecked")
    private static ClusterNodeView node(final Server server, final Serializable reply, final long threshold,
            final long now) {
        final Date heartbeat = server.getLastHeartBeat();
        final long lastContactSeconds = Math.max(0, now - (heartbeat != null ? heartbeat.getTime() : 0)) / 1000;
        final boolean responded = reply instanceof Map;
        final Map<String, Serializable> report = responded ? (Map<String, Serializable>) reply : Map.of();

        return new ClusterNodeView(
                server.getServerId(),
                LicenseUtil.getDisplayServerId(server.getServerId()),
                server.getName(),
                server.getClusterId(),
                server.getIpAddress(),
                server.getKey(),
                lastContactSeconds,
                responded,
                ClusterNodeMapper.status(responded, responded && ClusterNodeMapper.assetsFailed(report),
                        lastContactSeconds, threshold),
                responded ? ClusterNodeMapper.cache(report) : null,
                responded ? ClusterNodeMapper.search(report) : null,
                responded ? ClusterNodeMapper.assets(report) : null);
    }
}
