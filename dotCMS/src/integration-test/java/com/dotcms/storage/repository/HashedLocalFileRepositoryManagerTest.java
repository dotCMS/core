package com.dotcms.storage.repository;

import org.junit.Assert;
import org.junit.Test;

public class HashedLocalFileRepositoryManagerTest {

    /**
     * Method to test:  {@link HashedLocalFileRepositoryManager#normalizeFileInSubDirectoryAndFile(String)}
     * Given Scenario: Test diff scenarios for the method
     * ExpectedResult: The results perform based on the expected invariant
     *
     */
    @Test
    public void test_normalizeFileInSubDirectoryAndFile() {

        final HashedLocalFileRepositoryManager hashedLocalFileRepositoryManager = new HashedLocalFileRepositoryManager();

        Assert.assertNull(hashedLocalFileRepositoryManager.normalizeFileInSubDirectoryAndFile(null));
        final String shortHash1 = "1";
        Assert.assertEquals(shortHash1, hashedLocalFileRepositoryManager.normalizeFileInSubDirectoryAndFile(shortHash1));
        final String shortHash2 = "12";
        Assert.assertEquals(shortHash2, hashedLocalFileRepositoryManager.normalizeFileInSubDirectoryAndFile(shortHash2));
        final String shortHash3 = "1234";
        Assert.assertEquals(shortHash3, hashedLocalFileRepositoryManager.normalizeFileInSubDirectoryAndFile(shortHash3));
        final String hash = "f813463c714e009df1227f706e290e01";
        Assert.assertEquals("/f813/" + hash , hashedLocalFileRepositoryManager.normalizeFileInSubDirectoryAndFile(hash));
    }
}
