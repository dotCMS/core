package com.dotmarketing.image.filter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * Tests how legacy filters stage, name and publish a rendition: it is written to a private staging
 * file, put in place in one step, and never deleted ahead of its regeneration.
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

    /**
     * Given the same rendition rendered twice,
     * When each render asks for its staging file,
     * Then each gets its own file in the rendition's folder, named with a leading dot (so inode-scoped
     * cleanup never matches it) and not created until the filter writes it.
     */
    @Test
    void stagingFileFor_isUniquePerRenderHiddenAndNotCreated(@TempDir final Path dir) {
        final File result = dir.resolve("dotGenerated_929f6225-7c89-4cdf-9867-0bfb0c57d7b5_resize_abc.png").toFile();

        final File first = ImageFilter.stagingFileFor(result);
        final File second = ImageFilter.stagingFileFor(result);

        assertNotEquals(first, second, "concurrent renders must not share a staging file");
        assertEquals(result.getAbsoluteFile().getParentFile(), first.getParentFile(),
                "staging must stay in the rendition's folder so the final move is a rename");
        assertTrue(first.getName().startsWith("." + result.getName() + "_"),
                "a staging file must not start with dotGenerated_<inode>_, which cleanup deletes");
        assertFalse(first.exists(), "nothing is created before the filter writes");
    }

    /**
     * Given a generated input carrying a legacy numeric inode,
     * When the next filter in the chain names its rendition,
     * Then the numeric inode is kept at the start of the name, so cleanup by inode still finds it.
     */
    @Test
    void getResultsFile_keepsALegacyNumericInodeInTheName(@TempDir final Path dir) throws Exception {
        final File input = dir.resolve(WebKeys.GENERATED_FILE + "12345_resize_abc.png").toFile();
        Files.write(input.toPath(), bytes("input"));
        final Map<String, String[]> parameters = new HashMap<>();
        parameters.put("filter", new String[]{"Jpeg"});
        parameters.put("jpeg_q", new String[]{"80"});

        final File rendition = new JpegImageFilter().getResultsFile(input, parameters, "jpg");

        assertTrue(rendition.getName().startsWith(WebKeys.GENERATED_FILE + "12345_jpeg_"), rendition.getName());
    }

    /**
     * Given binaries stored in the {@code x/y/<inode>/<field>/<file>} layout (UUID and numeric inodes)
     * and files that only look like it,
     * When the version inode is read from the path,
     * Then it is found for the real layout and nothing is returned otherwise.
     */
    @Test
    void versionInodeFromBinaryPath_acceptsUuidAndNumericInodesInTheStorageLayout(@TempDir final Path dir) {
        final String uuid = "929f6225-7c89-4cdf-9867-0bfb0c57d7b5";
        assertEquals(uuid, ImageFilter.versionInodeFromBinaryPath(
                dir.resolve("assets/9/2/" + uuid + "/fileAsset/hero.jpg").toFile()).orElse(null));
        assertEquals("4815162342", ImageFilter.versionInodeFromBinaryPath(
                dir.resolve("assets/4/8/4815162342/fileAsset/hero.jpg").toFile()).orElse(null));
        assertTrue(ImageFilter.versionInodeFromBinaryPath(
                dir.resolve("assets/1/2/4815162342/fileAsset/hero.jpg").toFile()).isEmpty(),
                "the bucket folders must match the inode's first two characters");
        assertTrue(ImageFilter.versionInodeFromBinaryPath(
                dir.resolve("tmp_upload/temp_abc/hero.jpg").toFile()).isEmpty());
    }

    private static byte[] bytes(final String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
