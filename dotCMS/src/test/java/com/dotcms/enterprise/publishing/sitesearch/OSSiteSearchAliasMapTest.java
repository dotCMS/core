package com.dotcms.enterprise.publishing.sitesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Pure unit tests for {@link OSSiteSearchAPI#reverseAliasToIndexMap(Map)} — the reverse + multi-index
 * detection step behind {@link OSSiteSearchAPI#getAliasToIndexMap()}. No live cluster is needed: the
 * helper is a static function of a raw {@code index(.os) -> alias} map (issue #36360).
 */
public class OSSiteSearchAliasMapTest {

    /**
     * Normal 1:1 case: each {@code .os}-tagged index maps to one alias. The result must key by alias
     * and hand back the logical (tag-stripped) index name.
     */
    @Test
    public void reverse_strips_os_and_maps_alias_to_logical_index() {
        final Map<String, String> indexToAlias = new LinkedHashMap<>();
        indexToAlias.put("sitesearch_20260101000000_a.os", "lol");
        indexToAlias.put("sitesearch_20260102000000_b.os", "kek");

        final Map<String, String> result = OSSiteSearchAPI.reverseAliasToIndexMap(indexToAlias);

        assertEquals(2, result.size());
        assertEquals("sitesearch_20260101000000_a", result.get("lol"));
        assertEquals("sitesearch_20260102000000_b", result.get("kek"));
    }

    /**
     * Multi-index alias (two OS indices sharing one alias — a stale/duplicate state). Insertion order
     * puts the OLDER index last, so a naive last-wins would keep the older one; the guard must keep
     * the NEWEST (highest {@code sitesearch_<timestamp>}).
     */
    @Test
    public void multiIndex_alias_keeps_newest() {
        final Map<String, String> indexToAlias = new LinkedHashMap<>();
        indexToAlias.put("sitesearch_20260102000000_new.os", "lol");
        indexToAlias.put("sitesearch_20260101000000_old.os", "lol");

        final Map<String, String> result = OSSiteSearchAPI.reverseAliasToIndexMap(indexToAlias);

        assertEquals(1, result.size());
        assertEquals("newest (highest timestamp) index must win, not last-inserted",
                "sitesearch_20260102000000_new", result.get("lol"));
    }

    /**
     * The multi-index resolution must be deterministic: feeding the same two indices in either
     * iteration order must yield the same winner (never a non-deterministic last-wins).
     */
    @Test
    public void multiIndex_resolution_is_independent_of_iteration_order() {
        final Map<String, String> ascending = new LinkedHashMap<>();
        ascending.put("sitesearch_20260101000000_old.os", "lol");
        ascending.put("sitesearch_20260102000000_new.os", "lol");

        final Map<String, String> descending = new LinkedHashMap<>();
        descending.put("sitesearch_20260102000000_new.os", "lol");
        descending.put("sitesearch_20260101000000_old.os", "lol");

        assertEquals("resolution must not depend on map-iteration order",
                OSSiteSearchAPI.reverseAliasToIndexMap(ascending).get("lol"),
                OSSiteSearchAPI.reverseAliasToIndexMap(descending).get("lol"));
        assertEquals("sitesearch_20260102000000_new",
                OSSiteSearchAPI.reverseAliasToIndexMap(ascending).get("lol"));
    }

    /** An empty input yields an empty map (no NPE, no spurious entries). */
    @Test
    public void empty_input_yields_empty_map() {
        assertTrue(OSSiteSearchAPI.reverseAliasToIndexMap(new LinkedHashMap<>()).isEmpty());
    }
}
