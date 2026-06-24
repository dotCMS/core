package com.dotcms.content.index.opensearch;

import com.dotcms.cluster.ClusterUtils;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.IndexType;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.VersionedIndices;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;
import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.ClusterStats;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.AdminLogger;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.json.stream.JsonParser;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.indices.ClearCacheRequest;
import org.opensearch.client.opensearch.indices.ClearCacheResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.ForcemergeRequest;
import org.opensearch.client.opensearch.indices.ForcemergeResponse;
import org.opensearch.client.opensearch.indices.GetAliasRequest;
import org.opensearch.client.opensearch.indices.GetAliasResponse;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.UpdateAliasesRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;

/**
 * OpenSearch implementation of {@link IndexAPI}.
 *
 * <p>This class implements all {@link IndexAPI} operations using the OpenSearch Java client:
 * index lifecycle (create/delete/open/close/clear), statistics and health, the performance
 * operations ({@code flushCaches}, {@code optimize}, {@code updateReplicas}), alias management
 * ({@code createAlias}, {@code getIndexAlias}, {@code getAliasToIndexMap}) and inactive-set
 * cleanup ({@code deleteInactiveLiveWorkingIndices}). The {@link com.dotcms.content.index.IndexAPIImpl}
 * router decides when these are reached on OpenSearch as the migration phase advances.</p>
 *
 * <p>Error-handling contract: write operations propagate provider failures (so the router can
 * apply shadow fire-and-forget vs. primary-propagate semantics per phase), while read operations
 * log and return safe defaults — consistent with the rest of this class.</p>
 *
 * @author Fabrizio Araya
 */
@ApplicationScoped
@Default
public class OSIndexAPIImpl implements IndexAPI {

    /** OS analog of {@code ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS}. */
    public static final String INDEX_OPERATIONS_TIMEOUT = Lazy.of(
            () -> IndexConfigHelper.getString(OSIndexProperty.INDEX_OPERATIONS_TIMEOUT, "15s")).get();

    @Inject
    private OSClientProvider clientProvider;

    /**
     * No-arg constructor required by CDI for proxy creation.
     * The {@code clientProvider} dependency is injected via field injection after construction.
     */
    public OSIndexAPIImpl() {
        // CDI uses this constructor; clientProvider is injected via @Inject field
    }

    /**
     * Package-private constructor for testing.
     */
    OSIndexAPIImpl(OSClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    // =========================================================================
    // Index existence & lifecycle
    // =========================================================================

    @Override
    public boolean indexExists(String indexName) {
        if (indexName == null) {
            return false;
        }
        try {
            final ExistsRequest request = ExistsRequest.of(builder ->
                builder.index(getNameWithClusterIDPrefix(indexName))
            );
            return clientProvider.getClient().indices().exists(request).value();
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error checking if index exists: " + indexName, e);
            return false;
        }
    }

    @Override
    public void createIndex(String indexName) throws DotStateException, IOException {
        createIndex(indexName, null, 0);
    }

    @Override
    public CreateIndexStatus createIndex(String indexName, int shards)
            throws DotStateException, IOException {
        return createIndex(indexName, null, shards);
    }

    @Override
    public CreateIndexStatus createIndex(final String indexName, String settings, int shards)
            throws IOException {

        AdminLogger.log(this.getClass(), "createIndex",
            "Trying to create index: " + indexName + " with shards: " + shards);

        shards = shards > 0 ? shards : IndexConfigHelper.getInt(OSIndexProperty.INDEX_NUMBER_OF_SHARDS, 1);
        if (shards > 1) {
            Logger.warn(this.getClass(), "Number of OS shards : " + shards
                + ". Important to note that the more shards you enable, the slower the index reads."
                + "  dotCMS recommends using only 1 shard per replica. ");
        }

        final String autoExpandReplicas = IndexConfigHelper.getString(
            OSIndexProperty.INDEX_AUTO_EXPAND_REPLICAS, "0-1");
        final int finalShards = shards;

        // Parse the full settings JSON (including the nested "analysis" block with custom
        // analyzers) via IndexSettings._DESERIALIZER + the transport's JsonpMapper.
        // IndexSettings.Builder does NOT implement PlainDeserializable, so withJson() is
        // unavailable; instead we deserialize a full IndexSettings object, then call
        // toBuilder() to apply dynamic overrides (shards, replicas) on top.
        // Flat "index.*" keys in the JSON (e.g. "index.mapping.total_fields.limit") are
        // captured in IndexSettings.customSettings and forwarded to OpenSearch unchanged.
        final IndexSettings indexSettings;
        if (settings != null && !settings.isEmpty()) {
            try {
                // Use the shared client transport's JsonpMapper directly. Do NOT close it:
                // _transport() returns the singleton client's own transport, whose lifecycle
                // is owned by the client provider. Closing it here shuts down the shared
                // connection pool for the entire JVM, breaking every subsequent OS operation.
                final JsonpMapper mapper = clientProvider.getClient()._transport().jsonpMapper();
                try (JsonParser parser = mapper.jsonProvider().createParser(
                        new java.io.StringReader(settings))) {
                    indexSettings = IndexSettings._DESERIALIZER.deserialize(parser, mapper)
                            .toBuilder()
                            .numberOfShards(finalShards)
                            .autoExpandReplicas(autoExpandReplicas)
                            .build();
                }
            } catch (Exception e) {
                Logger.error(this.getClass(),
                    "Failed to parse settings JSON for index: " + indexName
                        + " — index will be created with defaults only", e);
                throw new DotStateException("Failed to parse index settings for: " + indexName, e);
            }
        } else {
            indexSettings = new IndexSettings.Builder()
                    .numberOfShards(finalShards)
                    .autoExpandReplicas(autoExpandReplicas)
                    .build();
        }

        final CreateIndexRequest request = CreateIndexRequest.of(builder ->
                builder.index(getNameWithClusterIDPrefix(indexName))
                        .settings(indexSettings)
                        .timeout(Time.of(timeBuilder ->
                                timeBuilder.time(INDEX_OPERATIONS_TIMEOUT)
                        ))
        );

        try {
            final CreateIndexResponse response = clientProvider.getClient().indices().create(request);
            AdminLogger.log(this.getClass(), "createIndex",
                "Index created: " + indexName + " with shards: " + shards);
            return CreateIndexStatus.from(response);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error creating index: " + indexName, e);
            throw new DotStateException("Failed to create index: " + indexName, e);
        }
    }

    @Override
    public void clearIndex(final String indexName)
            throws DotStateException, IOException {
        if (indexName == null || !indexExists(indexName)) {
            throw new DotStateException("Index " + indexName + " does not exist");
        }
        AdminLogger.log(this.getClass(), "clearIndex", "Trying to clear index: " + indexName);

        delete(indexName);

        final CreateIndexStatus res = createIndex(indexName, getDefaultIndexSettings(), 0);
        try {
            int w = 0;
            while (!res.acknowledged() && ++w < 100) {
                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
            Logger.warn(this, ex.getMessage(), ex);
        }
        AdminLogger.log(this.getClass(), "clearIndex", "Index: " + indexName + " cleared");
    }

    @Override
    public boolean delete(String indexName) {
        if (indexName == null) {
            Logger.error(this.getClass(), "Failed to delete a null OpenSearch index");
            return true;
        }
        try {
            final DeleteIndexRequest request = DeleteIndexRequest.of(builder ->
                builder.index(getNameWithClusterIDPrefix(indexName))
                       .timeout(Time.of(timeBuilder ->
                           timeBuilder.time(INDEX_OPERATIONS_TIMEOUT)
                       ))
            );
            final var response = clientProvider.getClient().indices().delete(request);
            return response.acknowledged();
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error deleting index: " + indexName, e);
            throw new RuntimeException("Failed to delete index: " + indexName, e);
        }
    }

    @Override
    public boolean deleteMultiple(String... indexNames) {
        if (indexNames == null || indexNames.length == 0) {
            return true;
        }
        boolean allAcknowledged = true;
        for (String indexName : indexNames) {
            try {
                if (!delete(indexName)) {
                    allAcknowledged = false;
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error deleting index: " + indexName, e);
                allAcknowledged = false;
            }
        }
        return allAcknowledged;
    }

    @Override
    public void closeIndex(String indexName) {
        try {
            clientProvider.getClient().indices().close(b ->
                b.index(getNameWithClusterIDPrefix(indexName))
                 .timeout(Time.of(t -> t.time(INDEX_OPERATIONS_TIMEOUT)))
            );
            AdminLogger.log(this.getClass(), "closeIndex", "Index closed: " + indexName);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error closing index: " + indexName, e);
            throw new RuntimeException("Failed to close index: " + indexName, e);
        }
    }

    @Override
    public void openIndex(String indexName) {
        try {
            clientProvider.getClient().indices().open(b ->
                b.index(getNameWithClusterIDPrefix(indexName))
                 .timeout(Time.of(t -> t.time(INDEX_OPERATIONS_TIMEOUT)))
            );
            AdminLogger.log(this.getClass(), "openIndex", "Index opened: " + indexName);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error opening index: " + indexName, e);
            throw new RuntimeException("Failed to open index: " + indexName, e);
        }
    }

    @Override
    public Set<String> listIndices() {
        try {
            final GetIndexRequest request = GetIndexRequest.of(b ->
                b.index(getClusterPrefix() + "*")
            );
            final GetIndexResponse response = clientProvider.getClient().indices().get(request);
            return response.result().keySet().stream()
                    .map(this::removeClusterIdFromName)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error listing indices", e);
            return new HashSet<>();
        }
    }

    @Override
    public List<String> getIndices(boolean expandToOpenIndices, boolean expandToClosedIndices) {
        // Basic implementation: list open indices always, closed if requested
        final List<String> result = new ArrayList<>();
        if (expandToOpenIndices) {
            result.addAll(listIndices());
        }
        if (expandToClosedIndices) {
            result.addAll(getClosedIndexes());
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<String> getLiveWorkingIndicesSortedByCreationDateDesc() {
        // Only OS-tagged live/working indices: this provider owns the tagged index set, never the
        // legacy (untagged) one. The tag filter is a no-op in production (every OS index is tagged)
        // but is essential in single-cluster test profiles where ES and OS share one cluster — it
        // keeps OS lifecycle ops (e.g. deleteInactiveLiveWorkingIndices) from ever touching the
        // legacy ES indices that live alongside under the same cluster prefix.
        return listIndices().stream()
                .filter(IndexTag.OS::isTagged)
                .filter(name -> IndexType.WORKING.is(name) || IndexType.LIVE.is(name))
                .sorted(Comparator.comparing(OSIndexAPIImpl::getIndexTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Extracts the {@code _YYYYMMDDHHMMSS} timestamp embedded in an index name, so live/working
     * sets can be ordered chronologically. The {@code .os} tag is stripped first (locally, only to
     * parse the value out) because the timestamp parser cannot consume a trailing tag.
     */
    private static String getIndexTimestamp(final String indexName) {
        final String base = IndexTag.strip(indexName);
        return Try.of(() -> base.substring(base.lastIndexOf('_') + 1)).getOrNull();
    }

    @Override
    public List<String> getClosedIndexes() {
        try {
            final GetIndexRequest request = GetIndexRequest.of(b ->
                b.index(getClusterPrefix() + "*")
                 .expandWildcards(ExpandWildcard.Closed)
                 .allowNoIndices(true)
            );
            final GetIndexResponse response = clientProvider.getClient().indices().get(request);
            return response.result().keySet().stream()
                    .filter(this::hasClusterPrefix)
                    .map(this::removeClusterIdFromName)
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(),
                    "Could not retrieve closed OpenSearch indices: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isIndexClosed(String index) {
        return getClosedIndexes().contains(index);
    }

    @Override
    public Status getIndexStatus(String indexName) throws DotDataException {
        final ContentletIndexAPI contentletIndexAPI = APILocator.getContentletIndexAPI();
        final List<String> currentIndex =
                Try.of(contentletIndexAPI::getCurrentIndex).getOrElse(Collections.emptyList());
        final List<String> newIndex =
                Try.of(contentletIndexAPI::getNewIndex).getOrElse(Collections.emptyList());
        if (currentIndex.contains(indexName)) {
            return Status.ACTIVE;
        }
        if (newIndex.contains(indexName)) {
            return Status.PROCESSING;
        }
        return Status.INACTIVE;
    }

    // =========================================================================
    // Statistics & monitoring
    // =========================================================================

    @Override
    public Map<String, com.dotcms.content.index.domain.IndexStats> getIndicesStats() {
        try {
            final org.opensearch.client.opensearch.indices.IndicesStatsResponse response =
                    clientProvider.getClient().indices()
                            .stats(r -> r.index(getClusterPrefix() + "*")
                                        .expandWildcards(ExpandWildcard.Open));
            final Map<String, com.dotcms.content.index.domain.IndexStats> result = new HashMap<>();
            for (final Map.Entry<String, org.opensearch.client.opensearch.indices.stats.IndicesStats> entry
                    : response.indices().entrySet()) {
                final String fullName = entry.getKey();
                if (!hasClusterPrefix(fullName)) {
                    continue;
                }
                final String name = removeClusterIdFromName(fullName);
                final org.opensearch.client.opensearch.indices.stats.IndexStats primaries =
                        entry.getValue().primaries();
                final long count    = primaries.docs()  != null ? primaries.docs().count()          : 0L;
                final long rawBytes = primaries.store() != null ? primaries.store().sizeInBytes()    : 0L;
                result.put(name, com.dotcms.content.index.domain.IndexStats.builder()
                        .indexName(name)
                        .documentCount(count)
                        .sizeRaw(rawBytes)
                        .size(toHumanReadableSize(rawBytes))
                        .build());
            }
            return result;
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error fetching OS indices stats: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, ClusterIndexHealth> getClusterHealth() {
        try {
            final org.opensearch.client.opensearch.cluster.HealthResponse response =
                    clientProvider.getClient().cluster()
                            .health(r -> r.index(getClusterPrefix() + "*")
                                         .level(org.opensearch.client.opensearch.cluster.health.ClusterHealthLevel.Indices));
            final Map<String, ClusterIndexHealth> result = new HashMap<>();
            for (final Map.Entry<String, org.opensearch.client.opensearch.cluster.health.IndexHealthStats> entry
                    : response.indices().entrySet()) {
                final String fullName = entry.getKey();
                if (!hasClusterPrefix(fullName)) {
                    continue;
                }
                final String name = removeClusterIdFromName(fullName);
                final org.opensearch.client.opensearch.cluster.health.IndexHealthStats health = entry.getValue();
                result.put(name, ClusterIndexHealth.builder()
                        .numberOfShards(health.numberOfShards())
                        .numberOfReplicas(health.numberOfReplicas())
                        .status(health.status() != null ? health.status().name() : "n/a")
                        .build());
            }
            return result;
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error fetching OS cluster health: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private static String toHumanReadableSize(final long bytes) {
        if (bytes < 1_024L)         return bytes + "b";
        if (bytes < 1_048_576L)     return String.format("%.1fkb", bytes / 1_024.0);
        if (bytes < 1_073_741_824L) return String.format("%.1fmb", bytes / 1_048_576.0);
        return                             String.format("%.1fgb", bytes / 1_073_741_824.0);
    }

    @Override
    public ClusterStats getClusterStats() {
        try {
            final org.opensearch.client.opensearch.nodes.NodesStatsResponse response =
                    clientProvider.getClient().nodes().stats();

            final String clusterName = response.clusterName();
            final List<com.dotcms.content.index.domain.NodeStats> nodeStatsList = new ArrayList<>();

            response.nodes().forEach((nodeId, nodeStats) -> {
                final org.opensearch.client.opensearch.nodes.stats.NodeIndicesStats indices = nodeStats.indices();
                final long docCount = indices != null && indices.docs() != null
                        ? indices.docs().count() : 0L;
                final long sizeRaw = indices != null && indices.store() != null
                        ? indices.store().sizeInBytes() : 0L;

                final List<org.opensearch.client.opensearch._types.NodeRole> roles = nodeStats.roles();
                final boolean isMaster = roles != null &&
                        roles.contains(org.opensearch.client.opensearch._types.NodeRole.ClusterManager);

                nodeStatsList.add(com.dotcms.content.index.domain.NodeStats.builder()
                        .name(nodeStats.name())
                        .transportAddress(nodeStats.transportAddress())
                        .host(nodeStats.host())
                        .master(isMaster)
                        .docCount(docCount)
                        .sizeRaw(sizeRaw)
                        .size(toHumanReadableSize(sizeRaw))
                        .build());
            });

            return ClusterStats.builder()
                    .clusterName(clusterName)
                    .nodeStats(nodeStatsList)
                    .build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error fetching OS cluster stats: " + e.getMessage(), e);
            return ClusterStats.builder()
                    .clusterName("opensearch")
                    .nodeStats(Collections.emptyList())
                    .build();
        }
    }

    /**
     * Waits for the OpenSearch cluster to become reachable, retrying up to
     * {@code OS_CONNECTION_ATTEMPTS} times (falling back to {@code ES_CONNECTION_ATTEMPTS},
     * default 24) with a {@code OS_CONNECTION_RETRY_SLEEP_SECONDS} (default 5s) pause between
     * attempts. Used as the OpenSearch startup connection gate (issue #36244).
     *
     * <h2>Active probe — not the swallowing {@code getClusterStats()}</h2>
     * <p>Connectivity is verified with {@code client.info()}, which round-trips to the cluster
     * and <strong>propagates</strong> any transport / TLS / auth failure. This is deliberate:
     * {@link #getClusterStats()} catches every exception and returns an empty result, so a retry
     * loop built on it can never observe a failure — the gate would always pass and the real
     * error would only surface much later, deep inside {@code createContentIndex} (the opaque
     * late crash this gate exists to prevent).</p>
     *
     * <h2>Phase-aware outcome on exhaustion</h2>
     * <ul>
     *   <li><strong>Phase 3 (OS only)</strong> — OS is the primary store and ES is decommissioned,
     *       so there is no safe fallback: log a FATAL actionable message and abort the JVM via
     *       {@link com.dotcms.shutdown.SystemExitManager#immediateExit(int, String)} (same as ES
     *       does today).</li>
     *   <li><strong>Phase 1 / 2 (shadow)</strong> — OS is not yet primary; ES still holds the
     *       authoritative state. Instead of killing the server, halt the migration
     *       ({@link IndexConfigHelper#haltMigration()} resets the phase to
     *       {@code PHASE_0_MIGRATION_NOT_STARTED}) so dotCMS falls back to ES-only, log an ERROR
     *       explaining the fallback, and return {@code false}.</li>
     * </ul>
     *
     * @return {@code true} when OS is reachable; {@code false} when OS was unreachable in a
     *         shadow phase and the migration was halted (ES-only fallback). In Phase 3 this method
     *         never returns {@code false} — it aborts the JVM instead.
     */
    @Override
    public boolean waitUtilIndexReady() {
        final int attempts = IndexConfigHelper.getInt(OSIndexProperty.CONNECTION_ATTEMPTS, 24);
        final long sleepMs =
                IndexConfigHelper.getInt(OSIndexProperty.CONNECTION_RETRY_SLEEP_SECONDS, 5) * 1000L;
        Exception lastError = null;
        for (int i = 0; i < attempts; i++) {
            try {
                // Active probe: info() round-trips to the cluster and throws on any failure.
                clientProvider.getClient().info();
                return true;
            } catch (Exception e) {
                lastError = e;
                Logger.error(this.getClass(), "OpenSearch Connection Attempt #" + (i + 1)
                        + " of " + attempts + ": " + e.getMessage());
            }
            DateUtil.sleep(sleepMs);
        }
        return handleConnectionExhausted(attempts, lastError);
    }

    /**
     * Phase-aware handler invoked once the OS connection retries are exhausted.
     *
     * @param attempts  the number of attempts that were made (for the log message)
     * @param lastError the last connection error observed, or {@code null}
     * @return {@code false} after halting the migration in a shadow phase; never returns in Phase 3
     *         (the JVM is terminated).
     */
    private boolean handleConnectionExhausted(final int attempts, final Exception lastError) {
        final MigrationPhase phase = MigrationPhase.current();
        final String cause = lastError != null ? lastError.getMessage() : "unknown";
        final String detail = "OpenSearch is not reachable after " + attempts + " attempt(s)."
                + " phase=" + phase.name()
                + ", endpoints=" + resolveEndpointsForLogging()
                + ", cause=" + cause;

        if (phase.isMigrationComplete()) {
            // Phase 3: OS is primary and ES is decommissioned — no fallback is possible.
            Logger.fatal(this.getClass(), detail
                    + " — OS is the primary store in " + phase.name() + "; cannot fall back to ES."
                    + " Verify OS_ENDPOINTS, OS_PROTOCOL/OS_TLS_ENABLED (scheme must match the"
                    + " server), and credentials, then restart dotCMS.");
            com.dotcms.shutdown.SystemExitManager.immediateExit(1,
                    "OpenSearch connection failed in PHASE_3_OPENSEARCH_ONLY");
            return false; // unreachable — immediateExit terminates the JVM
        }

        // Phase 1 / 2 (shadow): ES still holds the authoritative state. Fall back to ES-only
        // instead of killing the server.
        Logger.error(this.getClass(), detail
                + " — OS is a shadow store in " + phase.name() + "; falling back to ES-only"
                + " (resetting FEATURE_FLAG_OPEN_SEARCH_PHASE to 0 via haltMigration)."
                + " Fix OS connectivity and re-enable the migration phase when ready.");
        IndexConfigHelper.haltMigration();
        return false;
    }

    /**
     * Resolves the configured OpenSearch endpoints for an actionable log message, mirroring
     * {@code ConfigurableOpenSearchProvider} resolution: the explicit {@code OS_ENDPOINTS} array
     * when set, otherwise a single {@code protocol://host:port} synthesised from the OS connection
     * properties (with ES fallback).
     */
    private static String resolveEndpointsForLogging() {
        final String[] endpoints = Config.getStringArrayProperty("OS_ENDPOINTS", null);
        if (endpoints != null && endpoints.length > 0) {
            return Arrays.toString(endpoints);
        }
        final String protocol = IndexConfigHelper.getString(OSIndexProperty.PROTOCOL, "https");
        final String hostname = IndexConfigHelper.getString(OSIndexProperty.HOSTNAME, "localhost");
        final int    port     = IndexConfigHelper.getInt(OSIndexProperty.PORT, 9200);
        return protocol + "://" + hostname + ":" + port;
    }


    // =========================================================================
    // Alias management
    // =========================================================================

    @Override
    public void createAlias(final String indexName, final String alias) {
        try {
            // Only create the alias when it is not already pointing at an index — mirrors ES.
            if (getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices()).get(alias) == null) {
                clientProvider.getClient().indices().updateAliases(UpdateAliasesRequest.of(r -> r
                        .actions(a -> a.add(add -> add
                                .index(getNameWithClusterIDPrefix(indexName))
                                .alias(getNameWithClusterIDPrefix(alias))))
                        .timeout(t -> t.time(INDEX_OPERATIONS_TIMEOUT))));
                AdminLogger.log(this.getClass(), "createAlias",
                        "Alias '" + alias + "' created for index: " + indexName);
            }
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Error creating alias '" + alias + "' for OpenSearch index: " + indexName, e);
            throw new RuntimeException(
                    "Failed to create alias '" + alias + "' for index: " + indexName, e);
        }
    }

    @Override
    public Map<String, String> getIndexAlias(final List<String> indexNames) {
        final Map<String, String> aliases = new HashMap<>();
        if (indexNames == null || indexNames.isEmpty()) {
            return aliases;
        }
        try {
            final List<String> physicalNames = indexNames.stream()
                    .map(this::getNameWithClusterIDPrefix)
                    .collect(Collectors.toList());
            final GetAliasResponse response = clientProvider.getClient().indices()
                    .getAlias(GetAliasRequest.of(b -> b.index(physicalNames)));
            // result(): index name -> IndexAliases; IndexAliases.aliases() is keyed by alias name.
            response.result().forEach((indexName, indexAliases) -> {
                final Map<String, ?> aliasMap = indexAliases.aliases();
                if (UtilMethods.isSet(aliasMap)) {
                    final String aliasName = aliasMap.keySet().iterator().next();
                    aliases.put(removeClusterIdFromName(indexName),
                            removeClusterIdFromName(aliasName));
                }
            });
        } catch (Exception e) {
            // Consistent with the other OS read methods (listIndices, getClusterHealth, …):
            // log and return what was resolved rather than propagating provider errors.
            Logger.warnAndDebug(this.getClass(),
                    "Could not retrieve OpenSearch index aliases for " + indexNames
                        + ": " + e.getMessage(), e);
        }
        return aliases;
    }

    @Override
    public Map<String, String> getIndexAlias(String[] indexNames) {
        return getIndexAlias(Arrays.asList(indexNames));
    }

    @Override
    public String getIndexAlias(String indexName) {
        final Map<String, String> aliasMap = getIndexAlias(List.of(indexName));
        return aliasMap.get(indexName);
    }

    @Override
    public Map<String, String> getAliasToIndexMap(final List<String> indices) {
        final Map<String, String> reverse = new HashMap<>();
        getIndexAlias(indices).forEach((index, alias) -> reverse.put(alias, index));
        return reverse;
    }

    // =========================================================================
    // Performance operations
    // =========================================================================

    @Override
    public Map<String, Integer> flushCaches(final List<String> indexNames) {
        Logger.warn(this.getClass(), "Flushing OpenSearch index caches:" + indexNames);
        if (indexNames == null || indexNames.isEmpty()) {
            return ImmutableMap.of("failedShards", 0, "successfulShards", 0);
        }
        try {
            final List<String> physicalNames = indexNames.stream()
                    .map(this::getNameWithClusterIDPrefix)
                    .collect(Collectors.toList());
            final ClearCacheResponse response = clientProvider.getClient().indices()
                    .clearCache(ClearCacheRequest.of(b -> b.index(physicalNames)));
            final Map<String, Integer> map = ImmutableMap.of(
                    "failedShards", response.shards().failed(),
                    "successfulShards", response.shards().successful());
            Logger.warn(this.getClass(), "Flushed OpenSearch index caches:" + map);
            return map;
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error flushing OpenSearch index caches: " + indexNames, e);
            throw new RuntimeException("Failed to flush OpenSearch index caches: " + indexNames, e);
        }
    }

    @Override
    public boolean optimize(final List<String> indexNames) {
        try {
            final List<String> physicalNames = indexNames.stream()
                    .map(this::getNameWithClusterIDPrefix)
                    .collect(Collectors.toList());
            final ForcemergeResponse response = clientProvider.getClient().indices()
                    .forcemerge(ForcemergeRequest.of(b -> b.index(physicalNames)));
            Logger.info(this.getClass(),
                "Optimizing " + indexNames + " :" + response.shards().successful()
                    + "/" + response.shards().total() + " shards optimized");
            return true;
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error optimizing OpenSearch indices: " + indexNames, e);
            throw new RuntimeException("Failed to optimize OpenSearch indices: " + indexNames, e);
        }
    }

    @Override
    public void updateReplicas(final String indexName, final int replicas) throws DotDataException {
        if (!ClusterUtils.isReplicasSet()
                || !StringUtils.isNumeric(
                        IndexConfigHelper.getString(OSIndexProperty.INDEX_REPLICAS, null))) {
            AdminLogger.log(this.getClass(), "updateReplicas",
                    "Replicas can only be updated when an Enterprise License is used and "
                        + "OS_INDEX_REPLICAS (or ES_INDEX_REPLICAS) is set to a specific value.");
            throw new DotDataException(
                    "Replicas can only be updated when an Enterprise License is used and "
                        + "OS_INDEX_REPLICAS (or ES_INDEX_REPLICAS) is set to a specific value.");
        }

        AdminLogger.log(this.getClass(), "updateReplicas",
                "Trying to update replicas to index: " + indexName);

        // Unlike ES, we do not read the current replica count via getClusterHealth() to skip a
        // no-op update: OS getClusterHealth() keys are logical names (cluster prefix stripped,
        // .os tag preserved), so an ES-style prefixed lookup would miss and silently no-op.
        // Setting unconditionally after the gate above is simpler and always correct.
        try {
            clientProvider.getClient().indices().putSettings(
                    PutIndicesSettingsRequest.of(b -> b
                            .index(getNameWithClusterIDPrefix(indexName))
                            .settings(s -> s.numberOfReplicas(replicas))));
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Error updating replicas for OpenSearch index: " + indexName, e);
            throw new DotDataException("Failed to update replicas for index: " + indexName, e);
        }

        AdminLogger.log(this.getClass(), "updateReplicas",
                "Replicas updated to index: " + indexName);
    }

    @Override
    public void deleteInactiveLiveWorkingIndices(final int inactiveLiveWorkingSetsToKeep) {
        // List of live/working indices ordered by embedded timestamp, newest first.
        final List<String> indices = getLiveWorkingIndicesSortedByCreationDateDesc();

        // Never delete the currently-active OS live/working set.
        removeActiveLiveAndWorkingFromList(indices);

        int kept = 0;
        String lastTimestamp = "";
        final List<String> indicesToRemove = new ArrayList<>(indices);

        for (final String index : indices) {
            if (kept == inactiveLiveWorkingSetsToKeep) {
                break;
            }

            final String indexTimestamp = getIndexTimestamp(index);

            deleteLiveWorkingSetFromList(indicesToRemove, index, indexTimestamp);

            if (!Objects.equals(indexTimestamp, lastTimestamp)) {
                kept++;
            }

            lastTimestamp = indexTimestamp;
        }

        if (!indicesToRemove.isEmpty()) {
            deleteMultiple(indicesToRemove.toArray(new String[0]));
            Logger.info(this, "The following OpenSearch indices were deleted: "
                    + String.join(",", indicesToRemove));
        }
    }

    /**
     * Removes {@code index} from {@code indicesToRemove}, along with the sibling index that shares
     * its timestamp (the other half of the live/working set) when it is next in the ordered list.
     */
    private void deleteLiveWorkingSetFromList(final List<String> indicesToRemove,
            final String index, final String indexTimestamp) {
        indicesToRemove.remove(index);
        final String nextIndex = Try.of(() -> indicesToRemove.get(0)).getOrNull();
        if (UtilMethods.isSet(nextIndex)
                && Objects.equals(indexTimestamp, getIndexTimestamp(nextIndex))) {
            indicesToRemove.remove(0);
        }
    }

    /**
     * Drops the currently-active OS live and working indices from the candidate list so they are
     * never deleted. The active set is resolved from {@link VersionedIndices} (the OS store) — its
     * names are canonical physical form, so the cluster prefix is stripped to match the logical
     * names produced by {@link #getLiveWorkingIndicesSortedByCreationDateDesc()}; the {@code .os}
     * tag is preserved on both sides.
     */
    private void removeActiveLiveAndWorkingFromList(final List<String> indices) {
        final Optional<VersionedIndices> info = Try.of(() ->
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices())
                .getOrElse(Optional.empty());
        info.ifPresent(versioned -> {
            versioned.live().ifPresent(name -> indices.remove(removeClusterIdFromName(name)));
            versioned.working().ifPresent(name -> indices.remove(removeClusterIdFromName(name)));
        });
    }

    // =========================================================================
    // Settings & utilities
    // =========================================================================

    @Override
    public String getDefaultIndexSettings() {
        String settings = null;
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final URL url = classLoader.getResource("os-content-settings.json");
            if (url != null) {
                settings = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
            }
        } catch (Exception e) {
            Logger.error(this.getClass(),
                "Cannot load os-content-settings.json file, using defaults", e);
        }

        if (settings == null) {
            settings = "{\n" +
                "  \"number_of_shards\": 1,\n" +
                "  \"number_of_replicas\": 0,\n" +
                "  \"auto_expand_replicas\": \"0-1\",\n" +
                "  \"mapping\": {\n" +
                "    \"total_fields\": { \"limit\": 10000 },\n" +
                "    \"nested_fields\": { \"limit\": 10000 }\n" +
                "  },\n" +
                "  \"query\": { \"default_field\": \"catchall\" }\n" +
                "}";
        }
        return settings;
    }

}
