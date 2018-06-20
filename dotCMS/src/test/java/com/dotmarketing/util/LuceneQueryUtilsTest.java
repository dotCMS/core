package com.dotmarketing.util;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

public class LuceneQueryUtilsTest extends UnitTestBase {


    private static final String retrieveAllActionsQuery = "+contentType:FileAsset +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true";

    private static final String retrieveAllActionsQueryWithPrefix = "query_-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true";

    private static final String retrieveAllActionsQueryWithPrefixAndExcludeInodes = "query_-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -( inode:4eb3c99f-e999-44aa-acdc-75371deb8776 inode:2cd36499-beac-4c5a-9f8e-65c28ce62b8f)";

    @Test
    public void testRetrieveAllActions() throws Exception {

        final String preparedQuery = LuceneQueryUtils.prepareBulkActionsQuery(
                retrieveAllActionsQuery);
        Assert.assertEquals(retrieveAllActionsQuery + " -contentType:Host" , preparedQuery);

    }

}
