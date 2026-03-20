package com.dotcms.content.index;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phase-aware router implementation of {@link IndexAPI}.
 *
 * <h2>Routing table</h2>
 * <pre>
 * Phase                     | Read provider | Write providers
 * --------------------------|---------------|-----------------
 * 0 — not started           | ES            | [ES]
 * 1 — dual-write, ES reads  | ES            | [ES, OS]
 * 2 — dual-write, OS reads  | OS            | [ES, OS]
 * 3 — OS only               | OS            | [OS]
 * </pre>
 *
 * <h2>Routing delegation</h2>
 * <p>All routing decisions are centralised in {@link PhaseRouter}. Methods in this class
 * are thin wrappers that use one of four delegation patterns:</p>
 * <ul>
 *   <li>{@code router.read(impl -&gt; ...)} — single read provider, unchecked.</li>
 *   <li>{@code router.write(impl -&gt; ...)} — void write fan-out, unchecked.</li>
 *   <li>{@code router.writeBoolean(impl -&gt; ...)} — boolean write fan-out (AND of all results).</li>
 *   <li>{@code router.writeReturning(impl -&gt; ...)} — value-returning write fan-out; returns
 *       the read-provider's result for consistency with subsequent reads.</li>
 *   <li>Checked variants ({@code readChecked}, {@code writeChecked},
 *       {@code writeReturningChecked}) for methods that declare {@code IOException} or
 *       {@code DotDataException}.</li>
 * </ul>
 *
 * <h2>Exceptions to the uniform pattern</h2>
 * <p>Three categories of methods cannot use the uniform delegation pattern and contain
 * inline routing logic instead:</p>
 * <ol>
 *   <li><strong>Aggregating reads</strong> — {@code listIndices}, {@code getClosedIndexes},
 *       {@code getIndices}, {@code getClusterHealth}: in dual-write phases both providers
 *       maintain their own index sets simultaneously; the results must be <em>merged</em>
 *       rather than selecting one provider. These methods inspect
 *       {@code router.writeProviders().size()} to detect the dual-write phase and combine
 *       results from both {@code esImpl} and {@code osImpl} with deduplication.</li>
 *   <li><strong>Flush-and-return</strong> — {@code flushCaches}: must write to all providers
 *       (so caches are consistent) but the return value must come from the read provider
 *       (so it matches what a subsequent read would observe). Uses
 *       {@link PhaseRouter#writeReturning} which calls each provider exactly once.</li>
 *   <li><strong>Checked exceptions</strong> — methods that declare {@code IOException} or
 *       {@code DotDataException} must catch and re-throw because Java's functional
 *       interfaces ({@code Function}, {@code Consumer}) do not permit checked exceptions.
 *       The {@code Checked} variants of the router methods accept
 *       {@link PhaseRouter.ThrowingFunction} / {@link PhaseRouter.ThrowingConsumer}
 *       instead, so the caller only needs a catch-rethrow for the declared type.</li>
 * </ol>
 *
 * @author Fabrizio Araya
 * @see PhaseRouter
 * @see ESIndexAPI
 * @see OSIndexAPIImpl
 */
@IndexLibraryIndependent
@IndexRouter(access = {IndexAccess.READ, IndexAccess.WRITE})
public class IndexManagerAPIImpl implements IndexAPI {

    private final ESIndexAPI esImpl;
    private final OSIndexAPIImpl osImpl;
    private final PhaseRouter<IndexAPI> router;

    public IndexManagerAPIImpl() {
        this(new ESIndexAPI(), CDIUtils.getBeanThrows(OSIndexAPIImpl.class));
    }

    /**
     * Package-private constructor for testing.
     */
    IndexManagerAPIImpl(final ESIndexAPI esImpl, final OSIndexAPIImpl osImpl) {
        this.esImpl  = esImpl;
        this.osImpl  = osImpl;
        this.router  = new PhaseRouter<>(esImpl, osImpl);
    }

    // -------------------------------------------------------------------------
    // Read operations — uniform pattern: router.read(impl -> ...)
    // -------------------------------------------------------------------------

    @Override
    public Map<String, IndexStats> getIndicesStats() {
        return router.read(impl -> impl.getIndicesStats());
    }

    @Override
    public boolean optimize(final List<String> indexNames) {
        return router.read(impl -> impl.optimize(indexNames));
    }

    @Override
    public boolean isIndexClosed(final String index) {
        return router.read(impl -> impl.isIndexClosed(index));
    }

    @Override
    public boolean indexExists(final String indexName) {
        return router.read(impl -> impl.indexExists(indexName));
    }

    @Override
    public String getDefaultIndexSettings() {
        return router.read(impl -> impl.getDefaultIndexSettings());
    }

    @Override
    public Map<String, String> getIndexAlias(final List<String> indexNames) {
        return router.read(impl -> impl.getIndexAlias(indexNames));
    }

    @Override
    public Map<String, String> getIndexAlias(final String[] indexNames) {
        return router.read(impl -> impl.getIndexAlias(indexNames));
    }

    @Override
    public String getIndexAlias(final String indexName) {
        return router.read(impl -> impl.getIndexAlias(indexName));
    }

    @Override
    public Map<String, String> getAliasToIndexMap(final List<String> indices) {
        return router.read(impl -> impl.getAliasToIndexMap(indices));
    }

    @Override
    public List<String> getLiveWorkingIndicesSortedByCreationDateDesc() {
        return router.read(impl -> impl.getLiveWorkingIndicesSortedByCreationDateDesc());
    }

    @Override
    public Status getIndexStatus(final String indexName) throws DotDataException {
        try {
            return router.readChecked(impl -> impl.getIndexStatus(indexName));
        } catch (DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public String getNameWithClusterIDPrefix(final String name) {
        return router.read(impl -> impl.getNameWithClusterIDPrefix(name));
    }

    @Override
    public boolean waitUtilIndexReady() {
        return router.read(impl -> impl.waitUtilIndexReady());
    }

    @Override
    public ClusterStats getClusterStats() {
        return router.read(impl -> impl.getClusterStats());
    }

    // -------------------------------------------------------------------------
    // Aggregating reads — EXCEPTION: dual-write phases need results from BOTH
    // providers merged into a single response.
    //
    // Why: in phases 1 and 2, ES and OS each maintain their own physical index
    // sets simultaneously (different names, different clusters).  Routing the
    // read to just one provider would hide the other provider's indices from
    // admin callers.  Deduplication (via HashSet / seen-set) prevents duplicates
    // if the same logical name ever appears in both providers.
    //
    // Detection: router.writeProviders().size() == 1 → single-provider phase,
    //            delegate to that provider directly.  Size == 2 → dual-write,
    //            merge results from both esImpl and osImpl.
    // -------------------------------------------------------------------------

    /**
     * Returns the union of all index names across every active write provider.
     *
     * <p><strong>Routing exception:</strong> in dual-write phases (1 and 2) both ES and OS
     * maintain their own index sets; results from both are combined so that admin callers
     * can observe all active indices regardless of origin. In single-provider phases the
     * set is returned directly from that provider.</p>
     */
    @Override
    public Set<String> listIndices() {
        final List<IndexAPI> providers = router.writeProviders();
        if (providers.size() == 1) {
            return providers.get(0).listIndices();
        }
        final Set<String> combined = new HashSet<>(esImpl.listIndices());
        combined.addAll(osImpl.listIndices());
        return combined;
    }

    /**
     * Returns the merged cluster health map from every active write provider.
     *
     * <p><strong>Routing exception:</strong> same aggregation rule as {@link #listIndices()}.
     * On key collision in dual-write phases the OS entry wins, since OS is the future primary.</p>
     */
    @Override
    public Map<String, ClusterIndexHealth> getClusterHealth() {
        final List<IndexAPI> providers = router.writeProviders();
        if (providers.size() == 1) {
            return providers.get(0).getClusterHealth();
        }
        final Map<String, ClusterIndexHealth> merged = new HashMap<>(esImpl.getClusterHealth());
        merged.putAll(osImpl.getClusterHealth());
        return merged;
    }

    /**
     * Returns the combined list of open/closed indices from every active write provider.
     *
     * <p><strong>Routing exception:</strong> same aggregation rule as {@link #listIndices()}.
     * Order is preserved (ES first, then OS) with deduplication via a seen-set.</p>
     */
    @Override
    public List<String> getIndices(final boolean expandToOpenIndices,
            final boolean expandToClosedIndices) {
        final List<IndexAPI> providers = router.writeProviders();
        if (providers.size() == 1) {
            return providers.get(0).getIndices(expandToOpenIndices, expandToClosedIndices);
        }
        final Set<String> seen = new HashSet<>();
        final List<String> combined = new ArrayList<>();
        for (final String idx : esImpl.getIndices(expandToOpenIndices, expandToClosedIndices)) {
            if (seen.add(idx)) combined.add(idx);
        }
        for (final String idx : osImpl.getIndices(expandToOpenIndices, expandToClosedIndices)) {
            if (seen.add(idx)) combined.add(idx);
        }
        return combined;
    }

    /**
     * Returns the combined list of closed indices from every active write provider.
     *
     * <p><strong>Routing exception:</strong> same aggregation rule as {@link #listIndices()}.
     * Order is preserved (ES first, then OS) with deduplication via a seen-set.</p>
     */
    @Override
    public List<String> getClosedIndexes() {
        final List<IndexAPI> providers = router.writeProviders();
        if (providers.size() == 1) {
            return providers.get(0).getClosedIndexes();
        }
        final Set<String> seen = new HashSet<>();
        final List<String> combined = new ArrayList<>();
        for (final String idx : esImpl.getClosedIndexes()) {
            if (seen.add(idx)) combined.add(idx);
        }
        for (final String idx : osImpl.getClosedIndexes()) {
            if (seen.add(idx)) combined.add(idx);
        }
        return combined;
    }

    // -------------------------------------------------------------------------
    // Flush-and-return — EXCEPTION: fan out the write to ALL providers but
    // return the READ provider's result.
    //
    // Why: all provider caches must be consistent after a flush (write fan-out),
    // but the integer map returned to the caller should reflect the state of the
    // index that reads are currently served from (read-provider result).
    //
    // PhaseRouter.writeReturning() handles this in one call: it invokes every
    // write provider exactly once and returns the read-provider's value, avoiding
    // the double-call that would occur if we called router.write() then router.read().
    // -------------------------------------------------------------------------

    /**
     * Flushes caches on all active providers and returns the read provider's result.
     *
     * <p><strong>Routing exception:</strong> this is the only method that fans out a write
     * <em>and</em> returns a value. {@link PhaseRouter#writeReturning} ensures each provider
     * is called exactly once: the read-provider's result is returned for consistency with
     * subsequent reads; the other provider's result is discarded.</p>
     */
    @Override
    public Map<String, Integer> flushCaches(final List<String> indexNames) {
        return router.writeReturning(impl -> impl.flushCaches(indexNames));
    }

    // -------------------------------------------------------------------------
    // Write operations — uniform pattern: router.write / writeBoolean / writeReturning
    //
    // Methods that declare checked exceptions (IOException, DotDataException) use
    // the *Checked variants of the router: writeChecked / writeReturningChecked.
    // These accept ThrowingConsumer / ThrowingFunction so that the lambda body may
    // propagate checked exceptions.  The method wrapper catches-and-rethrows the
    // declared type and wraps anything else into it.
    // -------------------------------------------------------------------------

    @Override
    public boolean delete(final String indexName) {
        return router.writeBoolean(impl -> impl.delete(indexName));
    }

    @Override
    public boolean deleteMultiple(final String... indexNames) {
        return router.writeBoolean(impl -> impl.deleteMultiple(indexNames));
    }

    @Override
    public void deleteInactiveLiveWorkingIndices(final int inactiveLiveWorkingSetsToKeep) {
        router.write(impl -> impl.deleteInactiveLiveWorkingIndices(inactiveLiveWorkingSetsToKeep));
    }

    @Override
    public void createIndex(final String indexName) throws DotStateException, IOException {
        try {
            router.writeChecked(impl -> impl.createIndex(indexName));
        } catch (DotStateException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public CreateIndexStatus createIndex(final String indexName, final int shards)
            throws DotStateException, IOException {
        try {
            return router.writeReturningChecked(impl -> impl.createIndex(indexName, shards));
        } catch (DotStateException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void clearIndex(final String indexName)
            throws DotStateException, IOException, DotDataException {
        try {
            router.writeChecked(impl -> impl.clearIndex(indexName));
        } catch (DotStateException | IOException | DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public CreateIndexStatus createIndex(final String indexName, final String settings,
            final int shards) throws IOException {
        try {
            return router.writeReturningChecked(impl -> impl.createIndex(indexName, settings, shards));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void updateReplicas(final String indexName, final int replicas)
            throws DotDataException {
        try {
            router.writeChecked(impl -> impl.updateReplicas(indexName, replicas));
        } catch (DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public void createAlias(final String indexName, final String alias) {
        router.write(impl -> impl.createAlias(indexName, alias));
    }

    @Override
    public void closeIndex(final String indexName) {
        router.write(impl -> impl.closeIndex(indexName));
    }

    @Override
    public void openIndex(final String indexName) {
        router.write(impl -> impl.openIndex(indexName));
    }
}