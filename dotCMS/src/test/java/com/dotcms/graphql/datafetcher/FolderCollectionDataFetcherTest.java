package com.dotcms.graphql.datafetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class FolderCollectionDataFetcherTest {

    @Test
    void defaultMaxDepth_isThree() {
        assertEquals(3, FolderCollectionDataFetcher.DEFAULT_MAX_DEPTH);
    }

    @Test
    void classCanBeInstantiated() {
        assertNotNull(new FolderCollectionDataFetcher());
    }
}
