package com.dotcms.storage;

import com.dotcms.IntegrationTestBase;
import com.dotmarketing.business.CacheLocator;
import org.junit.Assert;
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
     * Given Scenario: Check the basics just to be align with test coverage :)
     * ExpectedResult: Do some implicit asserts
     */
    @Test
    public void Test_Cache_Basics() {

        final Chainable404StorageCache cache = new Chainable404StorageCache();

        Assert.assertEquals(Chainable404StorageCache.NOT_FOUND_404, "CHAINABLE_404_STORAGE_NOT_FOUND");
        Assert.assertEquals(cache.getPrimaryGroup(), "Chainable_404_Storage_group");
        Assert.assertEquals(cache.getGroups(), new String [] {"Chainable_404_Storage_group"});
    }
    /**
     * Method to test: This test tries the {@link Chainable404StorageCache}
     * Given Scenario: Will add a couple hits to the cache and then test them
     * ExpectedResult: The values added as a 404, should be there and the ones that are not should not
     */
    @Test
    public void Test_Cache() {

        final Chainable404StorageCache cache = CacheLocator.getChainable4040StorageCache();
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

        cache.remove(groupName, path1);
        assertFalse("The object on group mem-test and path1 should be NOT 404, after remove cache", cache.is404(groupName, path1));

        // clear the cache
        cache.clearCache();
        assertFalse("The object on group mem-test and path1 should be NOT 404, after clean cache", cache.is404(groupName, path1));
        assertFalse("The object on group mem-test and path2 should be NOT 404, after clean cache", cache.is404(groupName, path2));
        assertFalse("The object on group mem-test and path3 should be NOT 404, after clean cache", cache.is404(groupName, path3));
    }

    /**
     * Method to test: This test tries the {@link Chainable404StorageCache#put404(String, String)}
     * Given Scenario: Test null params put404
     * ExpectedResult: The values added as a 404, should be there and the ones that are not should not
     */
    @Test
    public void Test_Cache_null_put404() {

        final Chainable404StorageCache cache = new Chainable404StorageCache();

        // mark path1 and path2 as a 404, path 3 is not
        try {
            cache.put404(null, null);
        } catch (Exception e) {
            Assert.fail("Should not throw exception put404 null null");
        }
    }

}
