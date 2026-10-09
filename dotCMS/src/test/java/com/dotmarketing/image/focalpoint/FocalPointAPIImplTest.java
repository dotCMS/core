package com.dotmarketing.image.focalpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.FileMetadataAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks how a focal-point read treats a failed contentlet lookup. A visitor who cannot read the
 * content, or content that does not exist, has no focal point; only a failed lookup with S3 asset
 * storage on is an error.
 */
class FocalPointAPIImplTest {

    private static final String INODE = "abc123";
    private String previousFlag;
    private final ContentletAPI contentlets = mock(ContentletAPI.class);
    private final FocalPointAPIImpl focalPoints = new FocalPointAPIImpl(mock(FileMetadataAPI.class),
            mock(TempFileAPI.class), contentlets, () -> null);

    @BeforeEach
    void rememberFlag() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
    }

    @AfterEach
    void restoreFlag() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @Test
    void anonymousVisitorWithoutBackendReadAccessGetsNoFocalPoint() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, true);
        when(contentlets.find(eq(INODE), any(), eq(false))).thenThrow(new DotSecurityException("not readable"));
        assertTrue(focalPoints.readFocalPoint(INODE, "HeroImage").isEmpty());
    }

    @Test
    void missingContentGetsNoFocalPoint() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, true);
        when(contentlets.find(eq(INODE), any(), eq(false))).thenReturn(null);
        assertTrue(focalPoints.readFocalPoint(INODE, "HeroImage").isEmpty());
    }

    @Test
    void failedLookupIsAnErrorOnlyWhenEnabled() throws Exception {
        final DotDataException failure = new DotDataException("database unavailable");
        when(contentlets.find(eq(INODE), any(), eq(false))).thenThrow(failure);

        Config.setProperty(AssetStorageFeature.FLAG, true);
        final var thrown = assertThrows(DotRuntimeException.class,
                () -> focalPoints.readFocalPoint(INODE, "HeroImage"));
        assertSame(failure, thrown.getCause());

        Config.setProperty(AssetStorageFeature.FLAG, false);
        assertTrue(focalPoints.readFocalPoint(INODE, "HeroImage").isEmpty(), "Flag-off reads keep the legacy result");
    }
}
