package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.ClusterStats;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotcms.content.index.domain.IndexStats;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * Unit tests for the cluster-prefix {@code default} methods on {@link IndexAPI}:
 * {@link IndexAPI#getNameWithClusterIDPrefix}, {@link IndexAPI#removeClusterIdFromName}
 * and {@link IndexAPI#hasClusterPrefix}, all derived from a single overridable
 * {@link IndexAPI#getClusterPrefix()}.
 *
 * <p>The decisive cases are the ones that broke earlier heuristics: a cluster id
 * that itself contains dots, and a name carrying the {@code .os} suffix — the
 * strip must remove only the exact prefix and leave everything after it intact.
 * Tests exercise the real interface defaults through a minimal stub that overrides
 * only {@code getClusterPrefix()} (no cluster/DB required).</p>
 */
public class IndexAPIPrefixTest {

    private static final String RAW = "working_20260406";

    /** Minimal {@link IndexAPI} that supplies a fixed prefix and inherits every default. */
    private static IndexAPI apiWithPrefix(final String prefix) {
        return new PrefixOnlyIndexAPI(prefix);
    }

    // =========================================================================
    // removeClusterIdFromName — exact-prefix strip
    // =========================================================================

    /**
     * Given Scenario: A name carrying the exact cluster prefix.
     * Expected Result: The prefix is removed, leaving the raw name.
     */
    @Test
    public void test_removeClusterIdFromName_whenPresent_stripsExactPrefix() {
        final IndexAPI api = apiWithPrefix("cluster_08abc3.");

        assertEquals(RAW, api.removeClusterIdFromName("cluster_08abc3." + RAW));
    }

    /**
     * Given Scenario: A name with no cluster prefix.
     * Expected Result: Returned unchanged.
     */
    @Test
    public void test_removeClusterIdFromName_whenAbsent_returnsUnchanged() {
        final IndexAPI api = apiWithPrefix("cluster_08abc3.");

        assertEquals(RAW, api.removeClusterIdFromName(RAW));
    }

    /**
     * Given Scenario: A null name.
     * Expected Result: Empty string (legacy contract).
     */
    @Test
    public void test_removeClusterIdFromName_null_returnsEmptyString() {
        final IndexAPI api = apiWithPrefix("cluster_08abc3.");

        assertEquals("", api.removeClusterIdFromName(null));
    }

    /**
     * Given Scenario: The cluster id itself contains dots (e.g. {@code testing.cluster-names.}).
     * Expected Result: Only the exact prefix is stripped — the segment after it is preserved.
     * <p>This is the regression that defeated the {@code ^cluster_[^.]+\.(.+)$} heuristic,
     * which stopped at the first dot and left {@code cluster-names.<idx>} behind.</p>
     */
    @Test
    public void test_removeClusterIdFromName_dottedClusterId_stripsWholePrefix() {
        final IndexAPI api = apiWithPrefix("testing.cluster-names.");

        assertEquals(RAW, api.removeClusterIdFromName("testing.cluster-names." + RAW));
    }

    /**
     * Given Scenario: A prefixed name that also carries the {@code .os} suffix.
     * Expected Result: The prefix is stripped and the suffix is left untouched.
     */
    @Test
    public void test_removeClusterIdFromName_preservesOsSuffix() {
        final IndexAPI api = apiWithPrefix("cluster_08abc3.");

        assertEquals(RAW + ".os", api.removeClusterIdFromName("cluster_08abc3." + RAW + ".os"));
    }

    // =========================================================================
    // getNameWithClusterIDPrefix — inverse, idempotent
    // =========================================================================

    /**
     * Given Scenario: A raw name is prefixed, then stripped (round trip).
     * Expected Result: The original raw name is recovered; adding the prefix is idempotent.
     */
    @Test
    public void test_getNameWithClusterIDPrefix_isInverseAndIdempotent() {
        final IndexAPI api = apiWithPrefix("testing.cluster-names.");

        final String prefixed = api.getNameWithClusterIDPrefix(RAW);

        assertTrue(api.hasClusterPrefix(prefixed));
        assertEquals("adding the prefix twice must be a no-op",
                prefixed, api.getNameWithClusterIDPrefix(prefixed));
        assertEquals(RAW, api.removeClusterIdFromName(prefixed));
    }

    // =========================================================================
    // hasClusterPrefix
    // =========================================================================

    /**
     * Given Scenario: Prefixed, bare, and null names.
     * Expected Result: Only the prefixed name reports true; null is false.
     */
    @Test
    public void test_hasClusterPrefix_detectsExactPrefixOnly() {
        final IndexAPI api = apiWithPrefix("cluster_08abc3.");

        assertTrue(api.hasClusterPrefix("cluster_08abc3." + RAW));
        assertFalse(api.hasClusterPrefix(RAW));
        assertFalse(api.hasClusterPrefix(null));
    }

    // =========================================================================
    // Minimal stub: overrides only getClusterPrefix(), everything else unsupported
    // =========================================================================

    private static final class PrefixOnlyIndexAPI implements IndexAPI {

        private final String prefix;

        private PrefixOnlyIndexAPI(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String getClusterPrefix() {
            return prefix;
        }

        // ── unused by these tests ────────────────────────────────────────────
        @Override public List<String> getIndices(boolean a, boolean b) { throw new UnsupportedOperationException(); }
        @Override public List<String> getClosedIndexes() { throw new UnsupportedOperationException(); }
        @Override public Map<String, IndexStats> getIndicesStats() { throw new UnsupportedOperationException(); }
        @Override public Map<String, Integer> flushCaches(List<String> n) { throw new UnsupportedOperationException(); }
        @Override public boolean optimize(List<String> n) { throw new UnsupportedOperationException(); }
        @Override public boolean delete(String n) { throw new UnsupportedOperationException(); }
        @Override public boolean deleteMultiple(String... n) { throw new UnsupportedOperationException(); }
        @Override public void deleteInactiveLiveWorkingIndices(int n) { throw new UnsupportedOperationException(); }
        @Override public Set<String> listIndices() { throw new UnsupportedOperationException(); }
        @Override public boolean isIndexClosed(String n) { throw new UnsupportedOperationException(); }
        @Override public boolean indexExists(String n) { throw new UnsupportedOperationException(); }
        @Override public void createIndex(String n) { throw new UnsupportedOperationException(); }
        @Override public CreateIndexStatus createIndex(String n, int s) { throw new UnsupportedOperationException(); }
        @Override public void clearIndex(String n) { throw new UnsupportedOperationException(); }
        @Override public CreateIndexStatus createIndex(String n, String s, int sh) { throw new UnsupportedOperationException(); }
        @Override public String getDefaultIndexSettings() { throw new UnsupportedOperationException(); }
        @Override public Map<String, ClusterIndexHealth> getClusterHealth() { throw new UnsupportedOperationException(); }
        @Override public void updateReplicas(String n, int r) { throw new UnsupportedOperationException(); }
        @Override public void createAlias(String n, String a) { throw new UnsupportedOperationException(); }
        @Override public Map<String, String> getIndexAlias(List<String> n) { throw new UnsupportedOperationException(); }
        @Override public Map<String, String> getIndexAlias(String[] n) { throw new UnsupportedOperationException(); }
        @Override public String getIndexAlias(String n) { throw new UnsupportedOperationException(); }
        @Override public Map<String, String> getAliasToIndexMap(List<String> n) { throw new UnsupportedOperationException(); }
        @Override public void closeIndex(String n) { throw new UnsupportedOperationException(); }
        @Override public void openIndex(String n) { throw new UnsupportedOperationException(); }
        @Override public List<String> getLiveWorkingIndicesSortedByCreationDateDesc() { throw new UnsupportedOperationException(); }
        @Override public Status getIndexStatus(String n) { throw new UnsupportedOperationException(); }
        @Override public boolean waitUtilIndexReady() { throw new UnsupportedOperationException(); }
        @Override public ClusterStats getClusterStats() { throw new UnsupportedOperationException(); }
    }
}
