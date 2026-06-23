package com.dotmarketing.portlets.contentlet.business.exporter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the {@code SHARED_COMPLETED} dotGenerated path mapping and shared-store publishing
 * in {@link ImageFilterExporter}. These exercise the pure, container-free helpers directly.
 */
class ImageFilterExporterSharedStoreTest {

    /**
     * Given a rendition under the local dotGenerated base,
     * When mapped to the shared store,
     * Then the {@code inode[0]/inode[1]/hashedName} tail is preserved under the shared base.
     */
    @Test
    void toSharedFile_rebasesPreservingShardLayout() {
        final String localBase = "/data/dotsecure/dotGenerated";
        final String sharedBase = "/data/shared/assets/dotGenerated";
        final File local = new File(localBase + "/e/5/dotGenerated_abc123.jpg");

        final File shared = ImageFilterExporter.toSharedFile(local, localBase, sharedBase);

        assertEquals(new File(sharedBase + "/e/5/dotGenerated_abc123.jpg"), shared);
    }

    /**
     * A file outside the local base must not be mapped (e.g. a re-transform of an already-generated
     * file that lives in its source folder rather than under dotGenerated).
     */
    @Test
    void toSharedFile_returnsNullWhenOutsideLocalBase() {
        final File outside = new File("/somewhere/else/image.jpg");
        assertNull(ImageFilterExporter.toSharedFile(outside, "/data/dotsecure/dotGenerated",
                "/data/shared/assets/dotGenerated"));
    }

    /** A null local file maps to null rather than throwing. */
    @Test
    void toSharedFile_nullSafe() {
        assertNull(ImageFilterExporter.toSharedFile(null, "/a", "/b"));
    }

    /**
     * Given a finished rendition and a shared target whose parent directories do not yet exist,
     * When published,
     * Then the directories are created, the bytes match, and no temp file is left behind.
     */
    @Test
    void copyToShared_createsDirsAndCopiesBytes(@TempDir final Path tmp) throws Exception {
        final File local = tmp.resolve("local.jpg").toFile();
        final byte[] payload = "the-finished-rendition-bytes".getBytes();
        Files.write(local.toPath(), payload);

        // target parent dirs intentionally absent
        final File shared = tmp.resolve("shared/e/5/dotGenerated_abc123.jpg").toFile();
        final File sharedTmpDir = tmp.resolve("shared/tmp_upload").toFile();
        assertFalse(shared.getParentFile().exists());

        ImageFilterExporter.copyToShared(local, shared, sharedTmpDir);

        assertTrue(shared.exists());
        assertArrayEquals(payload, Files.readAllBytes(shared.toPath()));
        // staging temp file must not survive
        final File[] strays = shared.getParentFile()
                .listFiles((d, name) -> name.contains(".tmp"));
        assertTrue(strays == null || strays.length == 0, "stray .tmp file left in shared dir");
    }

    /**
     * Publishing the same rendition twice (as two cluster instances would) leaves the shared file
     * intact with the correct bytes — last write wins on identical content.
     */
    @Test
    void copyToShared_idempotentOnRepeat(@TempDir final Path tmp) throws Exception {
        final File local = tmp.resolve("local.jpg").toFile();
        final byte[] payload = "rendition".getBytes();
        Files.write(local.toPath(), payload);
        final File shared = tmp.resolve("shared/a/b/img.jpg").toFile();
        final File sharedTmpDir = tmp.resolve("shared/tmp_upload").toFile();

        ImageFilterExporter.copyToShared(local, shared, sharedTmpDir);
        ImageFilterExporter.copyToShared(local, shared, sharedTmpDir);

        assertTrue(shared.exists());
        assertArrayEquals(payload, Files.readAllBytes(shared.toPath()));
    }
}
