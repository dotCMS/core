package com.dotcms.storage.repository;

import com.dotcms.IntegrationTestBase;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UUIDUtil;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class HashedLocalFileRepositoryManagerTest extends IntegrationTestBase {

    /**
     * Method to test:  {@link HashedLocalFileRepositoryManager#normalizeFileInSubDirectoryAndFile(String)}
     * Given Scenario: Test diff scenarios for the method
     * ExpectedResult: The results perform based on the expected invariant
     *
     */
    @Test
    public void test_normalizeFileInSubDirectoryAndFile() {

        final HashedLocalFileRepositoryManager hashedLocalFileRepositoryManager = new HashedLocalFileRepositoryManager("./");

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

    /**
     * Method to test:  {@link HashedLocalFileRepositoryManager#exists(String)} (String)}   {@link HashedLocalFileRepositoryManager#getOrCreateFile(String)}
     * Given Scenario: Test with non exiting hash, then get or create it, then checks it exists and retrieve again
     * ExpectedResult: The methods should return the right information based on the scenarios
     *
     */
    @Test
    public void test_GetOrCreate_and_Exits_() {

        final String basePath = "./" + UUIDUtil.uuid();

        try {
            final HashedLocalFileRepositoryManager hashedLocalFileRepositoryManager = new HashedLocalFileRepositoryManager(basePath);
            final String hash = "f813463c714e009df1227f706e290e01";
            Assert.assertFalse(hashedLocalFileRepositoryManager.exists(hash));
            final File file = hashedLocalFileRepositoryManager.getOrCreateFile(hash);
            Assert.assertNotNull(file);
            Assert.assertFalse(file.exists());
            Try.run(()->FileUtils.write(file, "2" +
                    "test", StandardCharsets.UTF_8));
            Assert.assertTrue(hashedLocalFileRepositoryManager.exists(hash));
            Assert.assertEquals(hash, file.getName());
            Assert.assertEquals("f813", file.getParentFile().getName());


            final File file2 = hashedLocalFileRepositoryManager.getOrCreateFile(hash);
            Assert.assertNotNull(file2);
            Assert.assertTrue(file2.exists());
            Assert.assertEquals(hash, file.getName());
            Assert.assertEquals("f813", file.getParentFile().getName());

        } finally {

            Try.run(()->FileUtil.deleteDir(basePath));
        }
    }
}
