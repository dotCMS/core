package com.dotcms.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotmarketing.exception.DotDataException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

/**
 * Versioned-object operations are part of the storage contract so callers depend on the interface,
 * not the S3 implementation. A store that cannot provide them must refuse clearly rather than
 * pretend: callers rely on these operations for concurrency safety.
 */
class VersionedObjectStorageDefaultsTest {
    @Test
    void storesWithoutVersionedObjectsRefuseEachOperationWithAClearError() {
        final StoragePersistenceAPI store = mock(StoragePersistenceAPI.class, CALLS_REAL_METHODS);
        final var write = assertThrows(DotDataException.class,
                () -> store.writeObjectIfMatch("group", "path", new HashMap<String, java.io.Serializable>(), null));
        final var read = assertThrows(DotDataException.class,
                () -> store.readObjectSnapshot("group", "path", new JsonReaderDelegate<>(java.util.Map.class)));
        final var list = assertThrows(DotDataException.class,
                () -> store.listObjectSnapshots("group", "prefix/"));
        for (DotDataException refused : java.util.List.of(write, read, list)) {
            assertTrue(refused.getMessage().contains("does not support versioned objects"), refused.getMessage());
        }
    }
}
