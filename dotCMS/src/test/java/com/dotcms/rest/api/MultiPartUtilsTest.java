package com.dotcms.rest.api;

import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MultiPartUtils}, specifically guarding the
 * <code>getBinariesFromMultipart</code> code path against a Jersey quirk where
 * <code>FormDataMultiPart.getFields(name)</code> returns <code>null</code>
 * (not an empty list) when no part with that name exists.
 */
public class MultiPartUtilsTest {

    /**
     * Given scenario: A multipart request with no "file" form part is received
     * (e.g. the caller forgot to attach a file but sent a valid "contentlet" JSON).
     * Expected result: getBinariesFromMultipart returns an empty list — not a
     * NullPointerException. Downstream validation can then report the missing
     * required field cleanly instead of the server emitting a 500.
     */
    @Test
    public void getBinariesFromMultipart_returnsEmptyListWhenNoFilePart() throws IOException {
        final FormDataMultiPart multipart = mock(FormDataMultiPart.class);
        when(multipart.getFields("file")).thenReturn(null);

        final List<File> binaries = new MultiPartUtils(mock(FileAssetAPI.class))
                .getBinariesFromMultipart(multipart);

        assertNotNull(binaries);
        assertTrue(binaries.isEmpty());
    }

    /**
     * Given scenario: A multipart request includes a "file" form key but the
     * list of parts under that key is empty.
     * Expected result: getBinariesFromMultipart returns an empty list and does
     * not throw. Ensures the null-guard does not regress the previously-working
     * empty-list path.
     */
    @Test
    public void getBinariesFromMultipart_returnsEmptyListWhenFilePartListIsEmpty() throws IOException {
        final FormDataMultiPart multipart = mock(FormDataMultiPart.class);
        when(multipart.getFields("file")).thenReturn(Collections.<FormDataBodyPart>emptyList());

        final List<File> binaries = new MultiPartUtils(mock(FileAssetAPI.class))
                .getBinariesFromMultipart(multipart);

        assertNotNull(binaries);
        assertTrue(binaries.isEmpty());
    }
}
