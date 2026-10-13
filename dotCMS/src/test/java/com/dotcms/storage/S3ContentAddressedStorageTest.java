package com.dotcms.storage;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Upload;
import com.dotcms.enterprise.publishing.storage.Storage;
import com.dotmarketing.exception.DotDataException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Store verifies shared bytes with at most one full blob download. */
class S3ContentAddressedStorageTest {
    private static final String BUCKET = "bucket";
    private static final String OWNER = "binary-assets/a/b/file";
    private static final byte[] BYTES = "shared asset bytes".getBytes(StandardCharsets.UTF_8);
    private static final String HASH = DigestUtils.sha256Hex(BYTES);
    private static final String BLOB = S3ContentAddressedStorage.blobKey(HASH);

    @TempDir Path root;
    private Storage storage;
    private File source;

    @BeforeEach
    void setUp() throws Exception {
        source = Files.write(root.resolve("source"), BYTES).toFile();
        storage = mock(Storage.class);
        when(storage.uploadFile(any(PutObjectRequest.class))).thenReturn(mock(Upload.class));
        when(storage.getObject(BUCKET, OWNER)).thenAnswer(call ->
                object("{}".getBytes(StandardCharsets.UTF_8), HASH));
    }

    @Test
    void storingANewBlobDoesNotDownloadIt() throws Exception {
        final var uploaded = new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(call -> { uploaded.set(true); return null; })
                .when(storage).uploadFileIfAbsent(eq(BUCKET), eq(BLOB), any(File.class));
        when(storage.getObject(BUCKET, BLOB)).thenAnswer(call -> uploaded.get() ? object(BYTES, null) : null);

        assertEquals(HASH, new S3ContentAddressedStorage(storage, BUCKET).store(OWNER, source, false));
        verify(storage).uploadFileIfAbsent(eq(BUCKET), eq(BLOB), any(File.class));
        verify(storage, never()).getObject(BUCKET, BLOB);
    }

    @Test
    void storingAnExistingBlobComparesItsBytesOnce() throws Exception {
        when(storage.listFirstObject(BUCKET, BLOB)).thenReturn(summary());
        when(storage.getObject(BUCKET, BLOB)).thenAnswer(call -> object(BYTES, null));

        assertEquals(HASH, new S3ContentAddressedStorage(storage, BUCKET).store(OWNER, source, false));
        verify(storage, times(1)).getObject(BUCKET, BLOB);
        verify(storage, never()).uploadFileIfAbsent(any(), any(), any());
    }

    @Test
    void storingABlobWrittenConcurrentlyByAnotherNodeComparesItsBytesOnce() throws Exception {
        // The blob is absent when listed, then another node's conditional write wins the race.
        final var written = new java.util.concurrent.atomic.AtomicBoolean();
        final var conflict = new AmazonS3Exception("ConditionalRequestConflict");
        conflict.setStatusCode(409);
        doAnswer(call -> { written.set(true); throw conflict; })
                .when(storage).uploadFileIfAbsent(eq(BUCKET), eq(BLOB), any(File.class));
        when(storage.getObject(BUCKET, BLOB)).thenAnswer(call -> written.get() ? object(BYTES, null) : null);

        assertEquals(HASH, new S3ContentAddressedStorage(storage, BUCKET).store(OWNER, source, false));
        verify(storage, times(1)).getObject(BUCKET, BLOB);
    }

    @Test
    void anExistingBlobWithDifferentBytesFailsWithoutPublishingAReference() throws Exception {
        final byte[] other = BYTES.clone();
        other[0] ^= 1;
        when(storage.listFirstObject(BUCKET, BLOB)).thenReturn(summary());
        when(storage.getObject(BUCKET, BLOB)).thenAnswer(call -> object(other, null));

        assertThrows(DotDataException.class,
                () -> new S3ContentAddressedStorage(storage, BUCKET).store(OWNER, source, false));
        verify(storage, never()).uploadFile(any(PutObjectRequest.class));
    }

    private static S3ObjectSummary summary() {
        final var summary = new S3ObjectSummary();
        summary.setKey(BLOB);
        summary.setSize(BYTES.length);
        return summary;
    }

    private static S3Object object(final byte[] bytes, final String hashHeader) {
        final var metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        if (hashHeader != null) {
            metadata.addUserMetadata(S3ContentAddressedStorage.HASH_HEADER, hashHeader);
        }
        final var object = new S3Object();
        object.setObjectMetadata(metadata);
        object.setObjectContent(new ByteArrayInputStream(bytes));
        return object;
    }
}
