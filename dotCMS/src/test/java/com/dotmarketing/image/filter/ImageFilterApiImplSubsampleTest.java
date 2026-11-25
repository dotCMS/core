package com.dotmarketing.image.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.WebKeys;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that {@link ImageFilterApiImpl#intelligentResize(File, int, int, int)} drops a cached subsample
 * that no longer decodes, so the next request regenerates it, and keeps a healthy one when the resize
 * fails for another reason.
 */
class ImageFilterApiImplSubsampleTest {

    /**
     * Given an image larger than the maximum pixel size, so it is first subsampled to a cached file,
     * When the subsample turns out to be damaged and its resize fails,
     * Then the error is raised and the cached subsample is deleted.
     */
    @Test
    void intelligentResize_discardsADamagedSubsample(@TempDir final Path dir) throws Exception {
        final File input = largeGeneratedImage(dir);
        final File[] resized = new File[1];
        final ImageFilterApiImpl api = new ImageFilterApiImpl() {
            @Override
            public BufferedImage resizeImage(final File imageFile, final int width, final int height,
                    final int resampleOption) {
                resized[0] = imageFile;
                // the cached subsample is cut short, as a damaged write would leave it
                final byte[] bytes = io.vavr.control.Try.of(() -> Files.readAllBytes(imageFile.toPath())).get();
                io.vavr.control.Try.run(() -> Files.write(imageFile.toPath(), Arrays.copyOf(bytes, bytes.length / 3)));
                throw new DotRuntimeException("resize failed");
            }
        };

        assertThrows(DotRuntimeException.class, () -> api.intelligentResize(input, 100, 100, 0));

        assertNotNull(resized[0], "the resize must read the subsample");
        assertFalse(resized[0].equals(input), "the resize must read the subsample, not the source");
        assertFalse(resized[0].exists(), "a damaged subsample must not stay cached");
        assertTrue(input.exists(), "the source image is never deleted");
    }

    /**
     * Given a healthy cached subsample,
     * When its resize fails for a reason that is not about the subsample (resampling),
     * Then the error is raised and the subsample is kept, since every size of this image shares it.
     */
    @Test
    void intelligentResize_keepsAHealthySubsampleWhenOnlyTheResizeFails(@TempDir final Path dir) throws Exception {
        final File input = largeGeneratedImage(dir);
        final File[] resized = new File[1];
        final ImageFilterApiImpl api = new ImageFilterApiImpl() {
            @Override
            public BufferedImage resizeImage(final File imageFile, final int width, final int height,
                    final int resampleOption) {
                resized[0] = imageFile;
                throw new DotRuntimeException("resample failed");
            }
        };

        assertThrows(DotRuntimeException.class, () -> api.intelligentResize(input, 100, 100, 0));

        assertNotNull(resized[0]);
        assertTrue(resized[0].exists(), "a healthy subsample must be kept");
    }

    /**
     * Writes an image wider than the maximum pixel size, named like a rendition so its subsample is
     * cached next to it.
     *
     * @param dir the folder to write it in
     * @return the image
     * @throws Exception if it cannot be written
     */
    private static File largeGeneratedImage(final Path dir) throws Exception {
        final File input = dir.resolve(WebKeys.GENERATED_FILE + UUID.randomUUID() + "_big.png").toFile();
        final BufferedImage image = new BufferedImage(5001, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, (x * 7919 + y * 104729) & 0xFFFFFF);
            }
        }
        ImageIO.write(image, "png", input);
        return input;
    }
}
