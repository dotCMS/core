package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONObject;
import java.util.List;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/** Verifies content source storage defaults and per-index runtime configuration. */
public class ContentIndexSourceMappingTest {

    private static final String CONTENT_SOURCE = "index.content.source.enabled";
    private static final String SITESEARCH_SOURCE = "index.sitesearch.source.enabled";

    private String originalContentSource;
    private String originalSiteSearchSource;
    private ESIndexAPI indexAPI;
    private MappingOperationsES mappingOps;
    private ContentletIndexOperationsES operations;

    /** Isolates configuration and supplies an acknowledged, cluster-free index creator. */
    @Before
    public void setUp() throws Exception {
        originalContentSource = Config.getStringProperty(CONTENT_SOURCE, null);
        originalSiteSearchSource = Config.getStringProperty(SITESEARCH_SOURCE, null);
        Config.setProperty(CONTENT_SOURCE, null);
        Config.setProperty(SITESEARCH_SOURCE, null);
        indexAPI = mock(ESIndexAPI.class);
        mappingOps = mock(MappingOperationsES.class);
        when(indexAPI.createIndex(anyString(), anyString(), eq(1)))
                .thenReturn(CreateIndexStatus.builder().acknowledged(true).build());
        when(mappingOps.putMapping(anyList(), anyString())).thenReturn(true);
        operations = new ContentletIndexOperationsES(indexAPI, mappingOps);
    }

    /** Restores the caller's configuration even after an assertion fails. */
    @After
    public void tearDown() {
        Config.setProperty(CONTENT_SOURCE, originalContentSource);
        Config.setProperty(SITESEARCH_SOURCE, originalSiteSearchSource);
    }

    /** A rejected mapping must never be reported as a successful source-disabled index creation. */
    @Test
    public void rejectedMappingFailsIndexCreation() throws Exception {
        when(mappingOps.putMapping(anyList(), anyString())).thenReturn(false);
        assertThrows(IOException.class, () -> operations.createContentIndex("rejected_mapping", 1));
    }

    /** With no override, a new content index does not store source. */
    @Test
    public void contentSourceIsDisabledByDefault() throws Exception {
        assertSourceEnabled("content_default", false);
    }

    /** An explicit opt-in retains source storage. */
    @Test
    public void contentSourceCanBeEnabled() throws Exception {
        Config.setProperty(CONTENT_SOURCE, true);
        assertSourceEnabled("content_enabled", true);
    }

    /** One operations instance must reread Config for each new index, independently of Site Search. */
    @Test
    public void runtimeChangesAffectSubsequentIndexes() throws Exception {
        Config.setProperty(CONTENT_SOURCE, true);
        Config.setProperty(SITESEARCH_SOURCE, false);
        assertSourceEnabled("content_before_change", true);

        Config.setProperty(CONTENT_SOURCE, false);
        Config.setProperty(SITESEARCH_SOURCE, true);
        assertSourceEnabled("content_after_change", false);

        Config.setProperty(CONTENT_SOURCE, true);
        assertSourceEnabled("content_enabled_again", true);
    }

    /**
     * Checks the mapping submitted to the engine rather than a configuration helper's return value.
     *
     * @param indexName the fresh index whose mapping is captured
     * @param expected whether the engine should retain source
     */
    private void assertSourceEnabled(final String indexName, final boolean expected) throws Exception {
        operations.createContentIndex(indexName, 1);
        final ArgumentCaptor<String> mapping = ArgumentCaptor.forClass(String.class);
        verify(mappingOps).putMapping(eq(List.of(indexName)), mapping.capture());
        final JSONObject source = new JSONObject(mapping.getValue()).optJSONObject("_source");
        assertEquals(indexName + ": omitted _source.enabled means storage is enabled",
                expected, source == null || source.optBoolean("enabled", true));
    }
}
