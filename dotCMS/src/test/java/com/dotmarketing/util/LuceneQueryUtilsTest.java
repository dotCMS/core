package com.dotmarketing.util;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

public class LuceneQueryUtilsTest extends UnitTestBase {

    @Test
    public void testRetrieveAllActions() throws Exception {
        final String retrieveAllActionsQuery = "+contentType:FileAsset +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true";
        final String preparedRetrieveAllActionsQuery = "+contentType:FileAsset +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -contentType:host";
        final String sanitizedQuery = LuceneQueryUtils.sanitizeBulkActionsQuery(retrieveAllActionsQuery);
        Assert.assertEquals(preparedRetrieveAllActionsQuery , sanitizedQuery);
    }

    @Test
    public void testRetrieveAllActionsQueryWithPrefix() throws Exception {
        final String retrieveAllActionsQueryWithPrefix = "query_-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true";
        final String preparedRetrieveAllActionsQueryWithPrefix = "-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -contentType:host";
        final String sanitizedQuery = LuceneQueryUtils.sanitizeBulkActionsQuery(retrieveAllActionsQueryWithPrefix);
        Assert.assertEquals(preparedRetrieveAllActionsQueryWithPrefix , sanitizedQuery);
    }

    @Test
    public void testRetrieveAllActionsQueryWithPrefixAndExcludeInodes() throws Exception {
        final String retrieveAllActionsQueryWithPrefixAndExcludeInodes = "query_-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -(inode:4eb3c99f-e999-44aa-acdc-75371deb8776 inode:2cd36499-beac-4c5a-9f8e-65c28ce62b8f)";
        final String preparedRetrieveAllActionsQueryWithPrefixAndExcludeInodes = "-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -(inode:4eb3c99f-e999-44aa-acdc-75371deb8776 inode:2cd36499-beac-4c5a-9f8e-65c28ce62b8f) -contentType:host";
        final String sanitizedQuery = LuceneQueryUtils.sanitizeBulkActionsQuery(retrieveAllActionsQueryWithPrefixAndExcludeInodes);
        Assert.assertEquals(preparedRetrieveAllActionsQueryWithPrefixAndExcludeInodes , sanitizedQuery);
    }

}
