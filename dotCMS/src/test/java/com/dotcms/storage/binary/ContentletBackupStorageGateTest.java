package com.dotcms.storage.binary;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIInterceptor;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentletBackupStorageGateTest {
    @Test
    void disabledBackupDoesNotInitializeStorageAndAllVersionDeletionRetainsLegacyNoOp() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var locator = mockStatic(APILocator.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, false);
            final var remote = mock(StoragePersistenceAPI.class);
            final var backups = new ContentletBackupStorage(remote);
            assertThrows(DotDataException.class, () -> backups.store(new Contentlet()));
            assertThrows(DotDataException.class, () -> backups.list("identifier"));
            final var delegate = mock(ContentletAPI.class);
            locator.when(APILocator::getContentletAPIImpl).thenReturn(delegate);
            new ContentletAPIInterceptor().deleteAllVersionsandBackup(List.of(), null, false);
            verifyNoInteractions(remote, delegate);
        } finally { Config.setProperty(AssetStorageFeature.FLAG, previous); }
    }

    @Test
    void enabledAllVersionDeletionHonorsVetoAndPropagatesBackupFailure() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var locator = mockStatic(APILocator.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            final var delegate = mock(ContentletAPI.class);
            locator.when(APILocator::getContentletAPIImpl).thenReturn(delegate);
            final var interceptor = new ContentletAPIInterceptor();
            final var hook = mock(ContentletAPIPreHook.class);
            interceptor.addPreHook(hook);
            final List<Contentlet> contents = List.of(new Contentlet());
            assertThrows(DotRuntimeException.class, () -> interceptor.deleteAllVersionsandBackup(contents, null, false));
            verifyNoInteractions(delegate);
            when(hook.delete(contents, null, false, true)).thenReturn(true);
            final var failure = new DotDataException("backup unavailable");
            doThrow(failure).when(delegate).deleteAllVersionsandBackup(contents, null, false);
            assertSame(failure, assertThrows(DotDataException.class,
                    () -> interceptor.deleteAllVersionsandBackup(contents, null, false)));
        } finally { Config.setProperty(AssetStorageFeature.FLAG, previous); }
    }
}
