package com.dotmarketing.image.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.WebKeys;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that {@link ImageFilterApiImpl#intelligentResize(File, int, int, int)} does not keep a cached
 * subsample that it cannot resize, so the next request regenerates it instead of failing on it again.
 */
class ImageFilterApiImplSubsampleTest {

    /**
     * Given an image larger than the maximum pixel size, so it is first subsampled to a cached file,
     * When resizing that subsample fails,
     * Then the error is raised and the cached subsample is deleted.
     */
    @Test
    void intelligentResize_discardsTheCachedSubsampleWhenItsResizeFails(@TempDir final Path dir) throws Exception {
        // a generated-name input keeps the subsample next to it instead of the dotGenerated folder
        final File input = dir.resolve(WebKeys.GENERATED_FILE + UUID.randomUUID() + "_big.png").toFile();
        ImageIO.write(new BufferedImage(5001, 8, BufferedImage.TYPE_INT_RGB), "png", input);
        final File[] resized = new File[1];
        final ImageFilterApiImpl api = new ImageFilterApiImpl() {
            @Override
            public BufferedImage resizeImage(final File imageFile, final int width, final int height,
                    final int resampleOption) {
                resized[0] = imageFile;
                assertTrue(imageFile.exists(), "the subsample is cached before it is resized");
                throw new DotRuntimeException("resize failed");
            }
        };

        assertThrows(DotRuntimeException.class, () -> api.intelligentResize(input, 100, 100, 0));

        assertTrue(resized[0] != null && !resized[0].equals(input), "the resize must read the subsample");
        assertFalse(resized[0].exists(), "the subsample that failed to resize must not stay cached");
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.allMatch(file -> file.toFile().equals(input)),
                    "the subsample that failed to resize must not stay cached");
        }
        assertTrue(input.exists(), "the source image is never deleted");
    }
}
