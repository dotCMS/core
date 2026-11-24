package com.dotmarketing.image.filter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.WebKeys;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link ImageFilter#publishRendition(File, File)}: a rendition is put in place in one step and
 * is never deleted ahead of its regeneration.
 */
class ImageFilterPublishRenditionTest {

    /**
     * Given an existing rendition and a newly written temporary file next to it,
     * When the temporary file is published,
     * Then the rendition holds the new content and the temporary file is gone.
     */
    @Test
    void publishRendition_replacesTheExistingRenditionAndRemovesTheTempFile(@TempDir final Path dir) throws Exception {
        final File result = dir.resolve("dotGenerated_rendition.png").toFile();
        final File temp = dir.resolve("dotGenerated_rendition.png_123.tmp").toFile();
        Files.write(result.toPath(), bytes("old"));
        Files.write(temp.toPath(), bytes("new"));

        ImageFilter.publishRendition(temp, result);

        assertArrayEquals(bytes("new"), Files.readAllBytes(result.toPath()));
        assertFalse(temp.exists(), "the temporary file must be moved, not copied");
    }

    /**
     * Given a temporary file whose target cannot be written (its folder does not exist),
     * When it is published,
     * Then the error is raised and the temporary file is removed, so nothing is left behind.
     */
    @Test
    void publishRendition_failureRaisesAndRemovesTheTempFile(@TempDir final Path dir) throws Exception {
        final File temp = dir.resolve("dotGenerated_rendition.png_123.tmp").toFile();
        Files.write(temp.toPath(), bytes("new"));
        final File unreachable = dir.resolve("missing-folder/dotGenerated_rendition.png").toFile();

        assertThrows(IOException.class, () -> ImageFilter.publishRendition(temp, unreachable));
        assertFalse(temp.exists(), "a failed publish must not leave the temporary file behind");
    }

    /**
     * Given a cached rendition and a forced regeneration ({@code overwrite}) whose input no longer
     * decodes,
     * When the legacy filter runs,
     * Then it fails and the previously cached rendition is still in place: filters no longer delete
     * the rendition before regenerating it.
     */
    @Test
    void failedRegeneration_keepsThePreviousRendition(@TempDir final Path dir) throws Exception {
        // a generated-name input keeps the rendition next to it instead of the dotGenerated folder
        final File input = dir.resolve(WebKeys.GENERATED_FILE + UUID.randomUUID() + "_hero.png").toFile();
        Files.write(input.toPath(), bytes("not an image"));
        final Map<String, String[]> parameters = new HashMap<>();
        parameters.put("filter", new String[]{"Jpeg"});
        parameters.put("jpeg_q", new String[]{"80"});
        parameters.put("overwrite", new String[]{"true"});
        final JpegImageFilter filter = new JpegImageFilter();
        final File rendition = filter.getResultsFile(input, parameters, "jpg");
        Files.write(rendition.toPath(), bytes("previously cached rendition"));

        assertThrows(DotRuntimeException.class, () -> filter.runFilter(input, parameters));

        assertTrue(rendition.exists(), "a failed regeneration must not delete the cached rendition");
        assertArrayEquals(bytes("previously cached rendition"), Files.readAllBytes(rendition.toPath()));
    }

    private static byte[] bytes(final String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
