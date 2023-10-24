package com.dotcms.storage;

import com.dotcms.IntegrationTestBase;
import com.dotmarketing.business.CacheLocator;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link Chainable404StorageCache}
 * @author jsanca
 */
public class Chainable404StorageCacheTest extends IntegrationTestBase {

    /**
     * Method to test: This test tries the {@link Chainable404StorageCache}
     * Given Scenario: Will add a couple hits to the cache and then test them
     * ExpectedResult: The values added as a 404, should be there and the ones that are not should not
     */
    @Test
    public void Test_Cache() {

        final Chainable404StorageCache cache = CacheLocator.getChainable404StorageCache();
        final String groupName = "mem-test";
        final String path1 = "/path1.txt";
        final String path2 = "/path2.txt";
        final String path3 = "/path3.txt";

        // mark path1 and path2 as a 404, path 3 is not
        cache.put404(groupName, path1);
        cache.put404(groupName, path2);

        assertTrue("The object on group mem-test and path1 should be 404", cache.is404(groupName, path1));
        assertTrue("The object on group mem-test and path2 should be 404", cache.is404(groupName, path2));
        assertFalse("The object on group mem-test and path3 should be NOT 404", cache.is404(groupName, path3));

        // clear the cache
        cache.clearCache();
        assertFalse("The object on group mem-test and path1 should be NOT 404, after clean cache", cache.is404(groupName, path1));
        assertFalse("The object on group mem-test and path2 should be NOT 404, after clean cache", cache.is404(groupName, path2));
        assertFalse("The object on group mem-test and path3 should be NOT 404, after clean cache", cache.is404(groupName, path3));
    }

}
