package com.dotcms.content.index.opensearch;

import static com.dotcms.content.index.IndicesFactory.CLUSTER_PREFIX;

import com.dotcms.content.index.opensearch.OpenSearchDefaultClientProvider;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.Lazy;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;

/**
 * Implementation of OpenSearchIndexAPI for managing OpenSearch indices operations.
 * This is the OpenSearch equivalent of ESIndexAPI for OpenSearch 3.7.
 *
 * @author fabrizio
 */
@ApplicationScoped
@Default
public class OpenSearchIndexAPIImpl implements OpenSearchIndexAPI {

    @Inject
    OpenSearchDefaultClientProvider clientProvider;

    private static final ObjectMapper objectMapper = DotObjectMapperProvider.createDefaultMapper();

    public static final int INDEX_OPERATIONS_TIMEOUT_IN_MS =
            Config.getIntProperty("OPENSEARCH_INDEX_OPERATIONS_TIMEOUT", 15000);

    private final Lazy<String> clusterPrefix;

    OpenSearchIndexAPIImpl(Lazy<String> clusterPrefix) {
        this.clusterPrefix = clusterPrefix;
    }

    public OpenSearchIndexAPIImpl() {
            this(Lazy.of(() -> CLUSTER_PREFIX + ClusterFactory.getClusterId() + "."));
    }

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
    public CreateIndexResponse createIndex(String indexName, int shards) throws DotStateException, IOException {
        return createIndex(indexName, null, shards);
    }

    @Override
    public CreateIndexResponse createIndex(final String indexName, String settings, int shards)
            throws DotStateException, IOException {

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

        final CreateIndexRequest request = CreateIndexRequest.of(builder ->
            builder.index(getNameWithClusterIDPrefix(indexName))
                   .settings(IndexSettings.of(settingsBuilder -> {
                       settingsMap.forEach((key, value) -> {
                           if (value instanceof Integer) {
                               settingsBuilder.numberOfShards((Integer) value);
                           } else if (value instanceof String) {
                               settingsBuilder.autoExpandReplicas((String) value);
                           }
                           // Add more specific mappings as needed
                       });
                       return settingsBuilder;
                   }))
                   .timeout(Time.of(timeBuilder ->
                       timeBuilder.time(String.format("%dms",INDEX_OPERATIONS_TIMEOUT_IN_MS))
                   ))
        );

        try {
            final CreateIndexResponse response = clientProvider.getClient().indices().create(request);
            AdminLogger.log(this.getClass(), "createIndex",
                "Index created: " + indexName + " with shards: " + shards);
            return response;
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error creating index: " + indexName, e);
            throw new DotStateException("Failed to create index: " + indexName, e);
        }
    }

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
            Logger.error(this.getClass(), "Cannot load opensearch-content-settings.json file, using defaults", e);
        }

        // Fallback to basic settings if file not found
        if (settings == null) {
            settings = "{\n" +
                "  \"number_of_shards\": 1,\n" +
                "  \"number_of_replicas\": 0,\n" +
                "  \"auto_expand_replicas\": \"0-all\",\n" +
                "  \"mapping\": {\n" +
                "    \"total_fields\": {\n" +
                "      \"limit\": 10000\n" +
                "    },\n" +
                "    \"nested_fields\": {\n" +
                "      \"limit\": 10000\n" +
                "    }\n" +
                "  },\n" +
                "  \"query\": {\n" +
                "    \"default_field\": \"catchall\"\n" +
                "  }\n" +
                "}";
        }

        return settings;
    }

    @Override
    public Map<String, IndexStats> getIndicesStats() {
        // TODO: Implement indices stats retrieval using OpenSearch client
        // This will require using the cluster stats or indices stats API
        Logger.info(this.getClass(), "getIndicesStats not yet implemented for OpenSearch");
        return new HashMap<>();
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
                           timeBuilder.time(Duration.ofMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS).toString())
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
    public String getNameWithClusterIDPrefix(final String name) {
        return hasClusterPrefix(name) ? name : clusterPrefix.get() + name;
    }

    @Override
    public String removeClusterIdFromName(final String name) {
        if (name == null) return "";
        return name.indexOf(".") > -1
                ? name.substring(name.lastIndexOf(".") + 1, name.length())
                : name;
    }

    /**
     * Checks if the given index name has the cluster prefix
     */
    boolean hasClusterPrefix(final String indexName) {
        return indexName != null && indexName.startsWith(clusterPrefix.get());
    }
}