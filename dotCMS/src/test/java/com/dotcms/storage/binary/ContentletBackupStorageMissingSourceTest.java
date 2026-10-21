package com.dotcms.storage.binary;

import com.dotmarketing.exception.DotDataException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Only a binary that storage confirms is gone (no local file and no durable copy) may be left out
 * of a recovery archive; every other failure must still abort the backup.
 */
class ContentletBackupStorageMissingSourceTest {
    private static final File BINARY = new File("a/b/ab12/heroImage/Shared-Mixed.Txt");

    @Test
    void confirmedMissingBinaryIsSkippedWithoutLeavingAnEntryBehind() throws Exception {
        final var binaries = mock(BinaryAssetStorageAPI.class);
        when(binaries.openLocalFile(BINARY)).thenThrow(new NoSuchFileException(BINARY.getPath()));
        final var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            assertFalse(ContentletBackupStorage.archiveBinary(binaries, BINARY, zip, "assets/missing"));
            // The archive must still accept the next entry: nothing was left half-open.
            zip.putNextEntry(new ZipEntry("next"));
            zip.write("still writable".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertEquals(Map.of("next", "still writable"), entries(bytes.toByteArray()));
    }

    @Test
    void availableBinaryIsCopiedIntoItsEntry() throws Exception {
        final var binaries = mock(BinaryAssetStorageAPI.class);
        when(binaries.openLocalFile(BINARY)).thenReturn(
                new ByteArrayInputStream("original bytes".getBytes(StandardCharsets.UTF_8)));
        final var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            assertTrue(ContentletBackupStorage.archiveBinary(binaries, BINARY, zip, "assets/present"));
        }
        assertEquals(Map.of("assets/present", "original bytes"), entries(bytes.toByteArray()));
    }

    @Test
    void otherOpenFailuresStillAbort() throws Exception {
        final var binaries = mock(BinaryAssetStorageAPI.class);
        when(binaries.openLocalFile(BINARY)).thenThrow(new IOException("disk error"));
        try (var zip = new ZipOutputStream(new ByteArrayOutputStream())) {
            final var failure = assertThrows(IOException.class,
                    () -> ContentletBackupStorage.archiveBinary(binaries, BINARY, zip, "assets/broken"));
            assertFalse(failure instanceof NoSuchFileException);
        }
    }

    @Test
    void restoreFailuresStillAbort() throws Exception {
        final var binaries = mock(BinaryAssetStorageAPI.class);
        when(binaries.openLocalFile(BINARY)).thenThrow(new DotDataException("S3 unavailable"));
        try (var zip = new ZipOutputStream(new ByteArrayOutputStream())) {
            assertThrows(DotDataException.class,
                    () -> ContentletBackupStorage.archiveBinary(binaries, BINARY, zip, "assets/unreachable"));
        }
    }

    @Test
    void noSuchFileWhileCopyingAnOpenedBinaryStillAborts() throws Exception {
        // Only the open of the source counts as "confirmed missing". The same exception type from
        // anywhere later, such as a staging file, must not be mistaken for a missing source.
        final var binaries = mock(BinaryAssetStorageAPI.class);
        when(binaries.openLocalFile(BINARY)).thenReturn(new InputStream() {
            @Override public int read() throws IOException { throw new NoSuchFileException("staging"); }
        });
        try (var zip = new ZipOutputStream(new ByteArrayOutputStream())) {
            assertThrows(NoSuchFileException.class,
                    () -> ContentletBackupStorage.archiveBinary(binaries, BINARY, zip, "assets/interrupted"));
        }
    }

    private static Map<String, String> entries(final byte[] archive) throws IOException {
        final Map<String, String> entries = new LinkedHashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
