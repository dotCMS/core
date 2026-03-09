package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.isMigrationComplete;
import static com.dotcms.content.index.IndexConfigHelper.isMigrationNotStarted;
import static com.dotcms.content.index.IndexConfigHelper.isReadEnabled;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.ClusterStats;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotcms.content.model.annotation.IndexRouter;
import com.dotcms.content.model.annotation.IndexRouter.IndexAccess;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vendor-neutral router implementation of {@link IndexAPI}.
 *
 * <p>Delegates every index operation to the appropriate vendor implementation based
 * on the active feature flags:</p>
 * <ul>
 *   <li><strong>Read operations</strong> — routed to {@link OSIndexAPIImpl} when
 *       {@code FEATURE_FLAG_OPEN_SEARCH_READ} is enabled, otherwise to {@link ESIndexAPI}.</li>
 *   <li><strong>Write operations</strong> — dual-write when
 *       {@code FEATURE_FLAG_OPEN_SEARCH_WRITE} is enabled: both {@link OSIndexAPIImpl} and
 *       {@link ESIndexAPI} receive the mutation so both indices stay consistent during
 *       the migration window. ES result is always authoritative.</li>
 * </ul>
 *
 * <p>This class mirrors the pattern established by
 * {@code com.dotcms.content.elasticsearch.business.ESContentFactoryImpl}
 * for {@code ContentFactoryIndexOperations}.</p>
 *
 * @author Fabrizio Araya
 * @see ESIndexAPI
 * @see OSIndexAPIImpl
 * @see IndexConfigHelper
 */
@IndexLibraryIndependent
@IndexRouter(
        access = IndexAccess.READ_WRITE
)
public class IndexAPIImpl implements IndexAPI {

    private final ESIndexAPI esImpl;
    private final OSIndexAPIImpl osImpl;

    public IndexAPIImpl() {
        this(new ESIndexAPI(), CDIUtils.getBeanThrows(OSIndexAPIImpl.class));
    }

    /**
     * Package-private constructor for testing.
     */
    IndexAPIImpl(final ESIndexAPI esImpl, final OSIndexAPIImpl osImpl) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
    }

    /**
     * delegate accessor
     * @return ESIndexAPI
     */
    public ESIndexAPI esImpl() {
        return esImpl;
    }

    /**
     * delegate accessor
     * @return OSIndexAPIImpl
     */
    public OSIndexAPIImpl osImpl() {
        return osImpl;
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Override
    public Map<String, IndexStats> getIndicesStats() {
        if(isMigrationComplete()) {
            return osImpl.getIndicesStats();
        }
        return esImpl.getIndicesStats();
    }

    @Override
    public Map<String, Integer> flushCaches(List<String> indexNames) {
        if(isMigrationNotStarted()){
            return esImpl.flushCaches(indexNames);
        }
        if (isMigrationComplete()) {
            return osImpl.flushCaches(indexNames);
        }
        if(isReadEnabled()) {
            osImpl.flushCaches(indexNames);
        }
        return esImpl.flushCaches(indexNames);
    }

    @Override
    public boolean optimize(List<String> indexNames) {
        if (isMigrationComplete()) {
            return osImpl.optimize(indexNames);
        }
        return esImpl.optimize(indexNames);
    }

    @Override
    public Set<String> listIndices() {
        if (isMigrationComplete()) {
            return osImpl.listIndices();
        }
        return esImpl.listIndices();
    }

    @Override
    public boolean isIndexClosed(String index) {
        if (isMigrationComplete()) {
            return osImpl.isIndexClosed(index);
        }
        return esImpl.isIndexClosed(index);
    }

    @Override
    public boolean indexExists(String indexName) {
        if (isMigrationComplete()) {
            return osImpl.indexExists(indexName);
        }
        return esImpl.indexExists(indexName);
    }

    @Override
    public String getDefaultIndexSettings() {
        if (isMigrationComplete()) {
            return osImpl.getDefaultIndexSettings();
        }
        return esImpl.getDefaultIndexSettings();
    }

    @Override
    public Map<String, ClusterIndexHealth> getClusterHealth() {
        if (isMigrationComplete()) {
            return osImpl.getClusterHealth();
        }
        return esImpl.getClusterHealth();
    }

    @Override
    public Map<String, String> getIndexAlias(List<String> indexNames) {
        if (isMigrationComplete()) {
            return osImpl.getIndexAlias(indexNames);
        }
        return esImpl.getIndexAlias(indexNames);
    }

    @Override
    public Map<String, String> getIndexAlias(String[] indexNames) {
        if (isMigrationComplete()) {
            return osImpl.getIndexAlias(indexNames);
        }
        return esImpl.getIndexAlias(indexNames);
    }

    @Override
    public String getIndexAlias(String indexName) {
        if (isMigrationComplete()) {
            return osImpl.getIndexAlias(indexName);
        }
        return esImpl.getIndexAlias(indexName);
    }

    @Override
    public Map<String, String> getAliasToIndexMap(List<String> indices) {
        if (isMigrationComplete()) {
            return osImpl.getAliasToIndexMap(indices);
        }
        return esImpl.getAliasToIndexMap(indices);
    }

    @Override
    public List<String> getIndices(boolean expandToOpenIndices, boolean expandToClosedIndices) {
        if (isMigrationComplete()) {
            return osImpl.getIndices(expandToOpenIndices, expandToClosedIndices);
        }
        return esImpl.getIndices(expandToOpenIndices, expandToClosedIndices);
    }

    @Override
    public List<String> getLiveWorkingIndicesSortedByCreationDateDesc() {
        if (isMigrationComplete()) {
            return osImpl.getLiveWorkingIndicesSortedByCreationDateDesc();
        }
        return esImpl.getLiveWorkingIndicesSortedByCreationDateDesc();
    }

    @Override
    public List<String> getClosedIndexes() {
        if (isMigrationComplete()) {
            return osImpl.getClosedIndexes();
        }
        return esImpl.getClosedIndexes();
    }

    @Override
    public Status getIndexStatus(String indexName) throws DotDataException {
        if (isMigrationComplete()) {
            return osImpl.getIndexStatus(indexName);
        }
        return esImpl.getIndexStatus(indexName);
    }

    @Override
    public String getNameWithClusterIDPrefix(String name) {
        if (isMigrationComplete()) {
            return osImpl.getNameWithClusterIDPrefix(name);
        }
        return esImpl.getNameWithClusterIDPrefix(name);
    }

    @Override
    public boolean waitUtilIndexReady() {
        if (isMigrationComplete()) {
            return osImpl.waitUtilIndexReady();
        }
        return esImpl.waitUtilIndexReady();
    }

    @Override
    public ClusterStats getClusterStats() {
        if (isMigrationComplete()) {
            return osImpl.getClusterStats();
        }
        return esImpl.getClusterStats();
    }

    // -------------------------------------------------------------------------
    // Write operations — dual-write: OS receives the mutation when enabled,
    // ES always receives it so both indices stay consistent during migration.
    // -------------------------------------------------------------------------

    @Override
    public boolean delete(String indexName) {
        if (isMigrationComplete()) {
            osImpl.delete(indexName);
        }
        return esImpl.delete(indexName);
    }

    @Override
    public boolean deleteMultiple(String... indexNames) {
        if (isMigrationComplete()) {
            osImpl.deleteMultiple(indexNames);
        }
        return esImpl.deleteMultiple(indexNames);
    }

    @Override
    public void deleteInactiveLiveWorkingIndices(int inactiveLiveWorkingSetsToKeep) {
        if (isMigrationComplete()) {
            osImpl.deleteInactiveLiveWorkingIndices(inactiveLiveWorkingSetsToKeep);
        }
        esImpl.deleteInactiveLiveWorkingIndices(inactiveLiveWorkingSetsToKeep);
    }

    @Override
    public void createIndex(String indexName) throws DotStateException, IOException {
        if (isMigrationComplete()) {
            osImpl.createIndex(indexName);
        }
        esImpl.createIndex(indexName);
    }

    @Override
    public CreateIndexStatus createIndex(String indexName, int shards)
            throws DotStateException, IOException {
        if (isMigrationComplete()) {
            osImpl.createIndex(indexName, shards);
        }
        return esImpl.createIndex(indexName, shards);
    }

    @Override
    public void clearIndex(String indexName) throws DotStateException, IOException, DotDataException {
        if (isMigrationComplete()) {
            osImpl.clearIndex(indexName);
        }
        esImpl.clearIndex(indexName);
    }

    @Override
    public CreateIndexStatus createIndex(String indexName, String settings, int shards)
            throws IOException {
        if (isMigrationComplete()) {
            osImpl.createIndex(indexName, settings, shards);
        }
        return esImpl.createIndex(indexName, settings, shards);
    }

    @Override
    public void updateReplicas(String indexName, int replicas) throws DotDataException {
        if (isMigrationComplete()) {
            osImpl.updateReplicas(indexName, replicas);
        }
        esImpl.updateReplicas(indexName, replicas);
    }

    @Override
    public void createAlias(String indexName, String alias) {
        if (isMigrationComplete()) {
            osImpl.createAlias(indexName, alias);
        }
        esImpl.createAlias(indexName, alias);
    }

    @Override
    public void closeIndex(String indexName) {
        if (isMigrationComplete()) {
            osImpl.closeIndex(indexName);
        }
        esImpl.closeIndex(indexName);
    }

    @Override
    public void openIndex(String indexName) {
        if (isMigrationComplete()) {
            osImpl.openIndex(indexName);
        }
        esImpl.openIndex(indexName);
    }
}
