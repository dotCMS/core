package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotmarketing.exception.DotDataException;
import java.util.Optional;
import org.junit.Test;

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

        final VersionedIndicesAPI api = mock(VersionedIndicesAPI.class);
        when(api.loadDefaultVersionedIndices()).thenReturn(Optional.of(vi));

        final String result = new OSBulkHelper(api).getIndexName();
        assertEquals("Must return the active working index, not the fallback",
                expectedIndex, result);
    }

    /**
     * Given: versionedIndicesAPI returns a VersionedIndices whose working() is absent.
     * When : getIndexName() is called.
     * Then : the hardcoded fallback "dotcms_content" is returned and a WARN is logged.
     *        Distinguishes "indices row exists but working slot not populated" from
     *        "no indices row at all" — both reach the fallback but via different paths.
     */
    @Test
    public void test_getIndexName_fallback_whenWorkingSlotAbsent() throws DotDataException {
        final VersionedIndices vi = mock(VersionedIndices.class);
        when(vi.working()).thenReturn(Optional.empty());

        final VersionedIndicesAPI api = mock(VersionedIndicesAPI.class);
        when(api.loadDefaultVersionedIndices()).thenReturn(Optional.of(vi));

        final String result = new OSBulkHelper(api).getIndexName();
        assertEquals("Must fall back to default when working slot is absent",
                "dotcms_content", result);
    }

    /**
     * Given: versionedIndicesAPI returns no indices at all (empty Optional).
     * When : getIndexName() is called.
     * Then : the hardcoded fallback "dotcms_content" is returned — the fallback fires only
     *        because no index is configured, not unconditionally.
     */
    @Test
    public void test_getIndexName_fallback_whenNoActiveIndex() throws DotDataException {
        final VersionedIndicesAPI api = mock(VersionedIndicesAPI.class);
        when(api.loadDefaultVersionedIndices()).thenReturn(Optional.empty());

        final String result = new OSBulkHelper(api).getIndexName();
        assertEquals("Must fall back to default when no index is configured",
                "dotcms_content", result);
    }
}
