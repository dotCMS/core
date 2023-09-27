package com.dotcms.cli.security;

import com.dotcms.security.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class UtilsTest {

    @Test
    void testSha256toUnixHash_withValidFile_returnsCorrectHash() throws Exception {

        Path path = null;

        try {
            path = Files.createTempFile("test", ".txt");
            Files.write(path, "Hello, world!".getBytes());

            String expectedHash = "315f5bdb76d078c43b8ac0064e4a0164612b1fce77c869345bfc94c75894edd3";
            String actualHash = Utils.Sha256toUnixHash(path);

            Assertions.assertEquals(expectedHash, actualHash);
        } finally {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void testSha256toUnixHash_withValidFile_afterRename_returnsSameHash() throws Exception {

        Path originalPath = null;
        Path renamePath = null;

        try {
            originalPath = Files.createTempFile("test", ".txt");
            Files.write(originalPath, "Hello, world!".getBytes());

            var originalHash = Utils.Sha256toUnixHash(originalPath);
            Assertions.assertNotNull(originalHash);

            renamePath = Files.createTempFile("new-test", ".txt");
            Files.move(originalPath, renamePath, StandardCopyOption.REPLACE_EXISTING);

            var newHash = Utils.Sha256toUnixHash(renamePath);
            Assertions.assertNotNull(newHash);

            Assertions.assertEquals(originalHash, newHash);
        } finally {
            if (originalPath != null) {
                Files.deleteIfExists(originalPath);
            }
            if (renamePath != null) {
                Files.deleteIfExists(renamePath);
            }
        }
    }

    @Test
    void testSha256toUnixHash_withNonExistentFile_throwsIOException() {

        Path path = Path.of("nonexistent.txt");

        try {
            Utils.Sha256toUnixHash(path);
            Assertions.fail("Expected NoSuchFileException to be thrown");
        } catch (Exception e) {
            Assertions.assertTrue(e.getCause() instanceof NoSuchFileException);
        }
    }

    @Test
    void testSha256toUnixHash_withNullPath_throwsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> Utils.Sha256toUnixHash(null));
    }

}
