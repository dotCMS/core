package com.dotcms.content.index;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndexAPI;
import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.ClusterStats;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vendor-neutral router implementation of {@link IndexAPI}.
 *
 * <p>Delegates all index management operations to the appropriate vendor implementation
 * (Elasticsearch or OpenSearch). Currently defaults to {@link ESIndexAPI} for all operations.
 * Routing logic to select the active provider will be added in a future task.</p>
 *
 * <p>This class mirrors the pattern established by
 * {@code com.dotcms.content.elasticsearch.business.ESContentFactoryImpl}
 * for {@code ContentFactoryIndexOperations}.</p>
 *
 * @author Fabrizio Araya
 * @see ESIndexAPI
 * @see OSIndexAPIImpl
 */
public class IndexAPIImpl implements IndexAPI {

    private final ESIndexAPI esImpl;
    private final OSIndexAPIImpl osImpl;

    public IndexAPIImpl() {
        this.esImpl = new ESIndexAPI();
        this.osImpl = new OSIndexAPIImpl();
    }

    /**
     * Package-private constructor for testing.
     */
    IndexAPIImpl(final ESIndexAPI esImpl, final OSIndexAPIImpl osImpl) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
    }

    /**
     * Returns the active {@link IndexAPI} provider.
     * Currently always returns the Elasticsearch implementation.
     * Routing to OpenSearch will be wired here once the OS implementation is complete.
     */
    private IndexAPI getProvider() {
        return esImpl;
    }

    @Override
    public Map<String, IndexStats> getIndicesStats() {
        return getProvider().getIndicesStats();
    }

    @Override
    public Map<String, Integer> flushCaches(List<String> indexNames) {
        return getProvider().flushCaches(indexNames);
    }

    @Override
    public boolean optimize(List<String> indexNames) {
        return getProvider().optimize(indexNames);
    }

    @Override
    public boolean delete(String indexName) {
        return getProvider().delete(indexName);
    }

    @Override
    public boolean deleteMultiple(String... indexNames) {
        return getProvider().deleteMultiple(indexNames);
    }

    @Override
    public void deleteInactiveLiveWorkingIndices(int inactiveLiveWorkingSetsToKeep) {
        getProvider().deleteInactiveLiveWorkingIndices(inactiveLiveWorkingSetsToKeep);
    }

    @Override
    public Set<String> listIndices() {
        return getProvider().listIndices();
    }

    @Override
    public boolean isIndexClosed(String index) {
        return getProvider().isIndexClosed(index);
    }

    @Override
    public boolean indexExists(String indexName) {
        return getProvider().indexExists(indexName);
    }

    @Override
    public void createIndex(String indexName) throws DotStateException, IOException {
        getProvider().createIndex(indexName);
    }

    @Override
    public CreateIndexStatus createIndex(String indexName, int shards)
            throws DotStateException, IOException {
        return getProvider().createIndex(indexName, shards);
    }

    @Override
    public void clearIndex(String indexName) throws DotStateException, IOException, DotDataException {
        getProvider().clearIndex(indexName);
    }

    @Override
    public CreateIndexStatus createIndex(String indexName, String settings, int shards)
            throws IOException {
        return getProvider().createIndex(indexName, settings, shards);
    }

    @Override
    public String getDefaultIndexSettings() {
        return getProvider().getDefaultIndexSettings();
    }

    @Override
    public Map<String, ClusterIndexHealth> getClusterHealth() {
        return getProvider().getClusterHealth();
    }

    @Override
    public void updateReplicas(String indexName, int replicas) throws DotDataException {
        getProvider().updateReplicas(indexName, replicas);
    }

    @Override
    public void createAlias(String indexName, String alias) {
        getProvider().createAlias(indexName, alias);
    }

    @Override
    public Map<String, String> getIndexAlias(List<String> indexNames) {
        return getProvider().getIndexAlias(indexNames);
    }

    @Override
    public Map<String, String> getIndexAlias(String[] indexNames) {
        return getProvider().getIndexAlias(indexNames);
    }

    @Override
    public String getIndexAlias(String indexName) {
        return getProvider().getIndexAlias(indexName);
    }

    @Override
    public Map<String, String> getAliasToIndexMap(List<String> indices) {
        return getProvider().getAliasToIndexMap(indices);
    }

    @Override
    public void closeIndex(String indexName) {
        getProvider().closeIndex(indexName);
    }

    @Override
    public void openIndex(String indexName) {
        getProvider().openIndex(indexName);
    }

    @Override
    public List<String> getIndices(boolean expandToOpenIndices, boolean expandToClosedIndices) {
        return getProvider().getIndices(expandToOpenIndices, expandToClosedIndices);
    }

    @Override
    public List<String> getLiveWorkingIndicesSortedByCreationDateDesc() {
        return getProvider().getLiveWorkingIndicesSortedByCreationDateDesc();
    }

    @Override
    public String removeClusterIdFromName(String name) {
        return getProvider().removeClusterIdFromName(name);
    }

    @Override
    public List<String> getClosedIndexes() {
        return getProvider().getClosedIndexes();
    }

    @Override
    public Status getIndexStatus(String indexName) throws DotDataException {
        return getProvider().getIndexStatus(indexName);
    }

    @Override
    public String getNameWithClusterIDPrefix(String name) {
        return getProvider().getNameWithClusterIDPrefix(name);
    }

    @Override
    public boolean waitUtilIndexReady() {
        return getProvider().waitUtilIndexReady();
    }

    @Override
    public ClusterStats getClusterStats() {
        return getProvider().getClusterStats();
    }
}
