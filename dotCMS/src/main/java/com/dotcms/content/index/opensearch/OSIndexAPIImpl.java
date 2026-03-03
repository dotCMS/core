package com.dotcms.content.index.opensearch;

import static com.dotcms.content.index.IndicesFactory.CLUSTER_PREFIX;
import static com.dotcms.content.index.opensearch.ConfigurableOpenSearchProvider.INDEX_OPERATIONS_TIMEOUT;

import com.dotcms.cluster.ClusterUtils;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.IndexAPI;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.ClusterStats;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotcms.content.index.domain.NodeStats;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;

/**
 * OpenSearch implementation of {@link IndexAPI}.
 *
 * <p>This class implements all index management operations using the OpenSearch Java client.
 * Methods that are not yet fully implemented log an info message and return safe defaults —
 * the {@link com.dotcms.content.index.IndexAPIImpl} router delegates to
 * {@link com.dotcms.content.elasticsearch.business.ESIndexAPI} by default,
 * so these stubs will not be called in production until OS routing is enabled.</p>
 *
 * @author Fabrizio Araya
 */
@ApplicationScoped
@Default
public class OSIndexAPIImpl implements IndexAPI {

    @Inject
    private OpenSearchDefaultClientProvider clientProvider;

    private static final ObjectMapper objectMapper = DotObjectMapperProvider.createDefaultMapper();

    private Lazy<String> clusterPrefix =
            Lazy.of(() -> CLUSTER_PREFIX + ClusterFactory.getClusterId() + ".");

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
    OSIndexAPIImpl(OpenSearchDefaultClientProvider clientProvider, Lazy<String> clusterPrefix) {
        this.clientProvider = clientProvider;
        this.clusterPrefix = clusterPrefix;
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

        shards = shards > 0 ? shards : Config.getIntProperty("opensearch.index.number_of_shards", 1);

        Map<String, Object> settingsMap = (settings == null) ? new HashMap<>() :
            objectMapper.readValue(settings, LinkedHashMap.class);

        settingsMap.put("index.number_of_shards", shards);
        settingsMap.put("index.auto_expand_replicas", "0-all");
        settingsMap.putIfAbsent("index.mapping.total_fields.limit", 10000);
        settingsMap.putIfAbsent("index.mapping.nested_fields.limit", 10000);
        settingsMap.putIfAbsent("index.query.default_field",
            Config.getStringProperty("OPENSEARCH_INDEX_QUERY_DEFAULT_FIELD", "catchall"));

        final int finalShards = shards;
        final CreateIndexRequest request = CreateIndexRequest.of(builder ->
            builder.index(getNameWithClusterIDPrefix(indexName))
                   .settings(IndexSettings.of(settingsBuilder -> {
                       settingsBuilder.numberOfShards(finalShards);
                       settingsBuilder.autoExpandReplicas("0-all");
                       return settingsBuilder;
                   }))
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
                b.index(clusterPrefix.get() + "*")
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
        return listIndices().stream()
                .filter(name -> name.contains("working") || name.contains("live"))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getClosedIndexes() {
        // OpenSearch does not expose closed index state via GetIndexRequest easily.
        // Full implementation requires enumerating state via cat or cluster API.
        Logger.info(this.getClass(), "getClosedIndexes not yet fully implemented for OpenSearch");
        return new ArrayList<>();
    }

    @Override
    public boolean isIndexClosed(String index) {
        return getClosedIndexes().contains(getNameWithClusterIDPrefix(index));
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
        Logger.info(this.getClass(), "getIndicesStats not yet implemented for OpenSearch");
        return new HashMap<>();
    }

    @Override
    public Map<String, ClusterIndexHealth> getClusterHealth() {
        Logger.info(this.getClass(), "getClusterHealth not yet fully implemented for OpenSearch");
        return new HashMap<>();
    }

    @Override
    public ClusterStats getClusterStats() {
        Logger.info(this.getClass(), "getClusterStats not yet fully implemented for OpenSearch");
        return ClusterStats.builder()
                .clusterName("opensearch")
                .build();
    }

    @Override
    public boolean waitUtilIndexReady() {
        ClusterStats stats = null;
        final int attempts = Config.getIntProperty("OS_CONNECTION_ATTEMPTS", 24);
        for (int i = 0; i < attempts; i++) {
            try {
                stats = getClusterStats();
                break;
            } catch (Exception e) {
                Logger.error(this.getClass(),
                    "OpenSearch Connection Attempt #" + (i + 1) + ": " + e.getMessage());
            }
            DateUtil.sleep(Config.getIntProperty("OS_CONNECTION_TIMEOUT", 5) * 1000);
        }
        if (stats == null) {
            Logger.fatal(this.getClass(), "Cannot connect to OpenSearch, giving up.");
            com.dotcms.shutdown.SystemExitManager.immediateExit(1, "OpenSearch connection failed");
        }
        return true;
    }

    // =========================================================================
    // Alias management
    // =========================================================================

    @Override
    public void createAlias(String indexName, String alias) {
        Logger.info(this.getClass(), "createAlias not yet implemented for OpenSearch");
    }

    @Override
    public Map<String, String> getIndexAlias(List<String> indexNames) {
        Logger.info(this.getClass(), "getIndexAlias not yet implemented for OpenSearch");
        return new HashMap<>();
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
    public Map<String, String> getAliasToIndexMap(List<String> indices) {
        Logger.info(this.getClass(), "getAliasToIndexMap not yet implemented for OpenSearch");
        return new HashMap<>();
    }

    // =========================================================================
    // Performance operations
    // =========================================================================

    @Override
    public Map<String, Integer> flushCaches(List<String> indexNames) {
        Logger.info(this.getClass(), "flushCaches not yet implemented for OpenSearch");
        return ImmutableMap.of("failedShards", 0, "successfulShards", 0);
    }

    @Override
    public boolean optimize(List<String> indexNames) {
        Logger.info(this.getClass(), "optimize not yet implemented for OpenSearch");
        return true;
    }

    @Override
    public void updateReplicas(String indexName, int replicas) throws DotDataException {
        Logger.info(this.getClass(), "updateReplicas not yet implemented for OpenSearch");
    }

    @Override
    public void deleteInactiveLiveWorkingIndices(int inactiveLiveWorkingSetsToKeep) {
        Logger.info(this.getClass(),
            "deleteInactiveLiveWorkingIndices not yet implemented for OpenSearch");
    }

    // =========================================================================
    // Settings & utilities
    // =========================================================================

    @Override
    public String getDefaultIndexSettings() {
        String settings = null;
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final URL url = classLoader.getResource("opensearch-content-settings.json");
            if (url != null) {
                settings = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
            }
        } catch (Exception e) {
            Logger.error(this.getClass(),
                "Cannot load opensearch-content-settings.json file, using defaults", e);
        }

        if (settings == null) {
            settings = "{\n" +
                "  \"number_of_shards\": 1,\n" +
                "  \"number_of_replicas\": 0,\n" +
                "  \"auto_expand_replicas\": \"0-all\",\n" +
                "  \"mapping\": {\n" +
                "    \"total_fields\": { \"limit\": 10000 },\n" +
                "    \"nested_fields\": { \"limit\": 10000 }\n" +
                "  },\n" +
                "  \"query\": { \"default_field\": \"catchall\" }\n" +
                "}";
        }
        return settings;
    }

    @Override
    public String getNameWithClusterIDPrefix(final String name) {
        return hasClusterPrefix(name) ? name : clusterPrefix.get() + name;
    }

    @Override
    public String removeClusterIdFromName(final String name) {
        if (name == null) return "";
        return name.contains(".")
                ? name.substring(name.lastIndexOf(".") + 1)
                : name;
    }

    /**
     * Checks if the given index name has the cluster prefix.
     */
    boolean hasClusterPrefix(final String indexName) {
        return indexName != null && indexName.startsWith(clusterPrefix.get());
    }
}
