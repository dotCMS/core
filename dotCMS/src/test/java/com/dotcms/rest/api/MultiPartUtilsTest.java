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

public class MultiPartUtilsTest {

    @Test
    public void getBinariesFromMultipart_returnsEmptyListWhenNoFilePart() throws IOException {
        // Jersey's FormDataMultiPart.getFields(name) returns null (not an empty list)
        // when no part with that name exists. The previous implementation NPE'd in the
        // for-each loop; this guards against that and surfaces the missing-file case
        // through downstream validation instead of a 500.
        final FormDataMultiPart multipart = mock(FormDataMultiPart.class);
        when(multipart.getFields("file")).thenReturn(null);

        final List<File> binaries = new MultiPartUtils(mock(FileAssetAPI.class))
                .getBinariesFromMultipart(multipart);

        assertNotNull(binaries);
        assertTrue(binaries.isEmpty());
    }

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
