package com.dotcms.graphql.datafetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class FolderCollectionDataFetcherTest {

    @Test
    void defaultMaxDepth_isFive() {
        assertEquals(5, FolderCollectionDataFetcher.DEFAULT_MAX_DEPTH);
    }

    @Test
    void classCanBeInstantiated() {
        assertNotNull(new FolderCollectionDataFetcher());
    }
}
