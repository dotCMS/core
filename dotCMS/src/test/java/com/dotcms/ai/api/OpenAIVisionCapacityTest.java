package com.dotcms.ai.api;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.exporter.ImageGenerationBusyException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.File;
import java.util.Optional;
import org.junit.Test;

public class OpenAIVisionCapacityTest {

    /** An unavailable rendition skips optional enrichment instead of aborting a content save. */
    @Test
    public void skipsEnrichmentWhenBusy() {
        final OpenAIVisionAPIImpl vision = spy(new OpenAIVisionAPIImpl());
        final Contentlet content = mock(Contentlet.class);
        final Field field = mock(Field.class);
        final File image = new File("fixture.png");
        doReturn(Optional.of(image)).when(vision).getFileToProcess(content, field);
        doReturn("test-model").when(vision).getAiVisionModel(null);
        doReturn("1").when(vision).getAiVisionMaxTokens(null);
        final ImageGenerationBusyException busy = mock(ImageGenerationBusyException.class);
        when(busy.getMessage()).thenReturn("Image generation is at capacity");
        doThrow(new DotRuntimeException(busy)).when(vision).base64EncodeImage(image);

        try {
            assertTrue(vision.readImageTagsAndDescription(content, field).isEmpty());
        } catch (DotRuntimeException e) {
            throw new AssertionError("Enrichment propagated the busy error: " + e.getMessage());
        }
    }
}
