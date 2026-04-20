package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import java.util.Optional;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link OSBulkHelper#getIndexName()}.
 *
 * <p>Verifies that the method resolves the active OS working index from
 * {@code VersionedIndicesAPI} and only triggers the fallback path when
 * no index is configured — never on every call.</p>
 */
public class OSBulkHelperTest {

    /**
     * Given: versionedIndicesAPI has an active working index.
     * When : getIndexName() is called.
     * Then : the working index name is returned — the fallback is NOT triggered.
     */
    @Test
    public void test_getIndexName_happyPath_returnsWorkingIndex() throws DotDataException {
        final String expectedIndex = "working_20240101_abc123";

        final VersionedIndices vi = mock(VersionedIndices.class);
        when(vi.working()).thenReturn(Optional.of(expectedIndex));

        final VersionedIndicesAPI versionedIndicesAPI = mock(VersionedIndicesAPI.class);
        when(versionedIndicesAPI.loadDefaultVersionedIndices()).thenReturn(Optional.of(vi));

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getVersionedIndicesAPI).thenReturn(versionedIndicesAPI);

            final String result = new OSBulkHelper().getIndexName();
            assertEquals("Must return the active working index, not the fallback",
                    expectedIndex, result);
        }
    }

    /**
     * Given: versionedIndicesAPI returns no indices (empty Optional).
     * When : getIndexName() is called.
     * Then : the hardcoded fallback "dotcms_content" is returned — the fallback fires only
     *        because no index is configured, not unconditionally.
     */
    @Test
    public void test_getIndexName_fallback_whenNoActiveIndex() throws DotDataException {
        final VersionedIndicesAPI versionedIndicesAPI = mock(VersionedIndicesAPI.class);
        when(versionedIndicesAPI.loadDefaultVersionedIndices()).thenReturn(Optional.empty());

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getVersionedIndicesAPI).thenReturn(versionedIndicesAPI);

            final String result = new OSBulkHelper().getIndexName();
            assertEquals("Must fall back to default when no index is configured",
                    "dotcms_content", result);
        }
    }
}
