package com.dotmarketing.portlets.contentlet.business.exporter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.image.filter.ResizeImageFilter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the {@code SHARED_COMPLETED} dotGenerated path mapping and shared-store publishing
 * in {@link ImageFilterExporter}. These exercise the pure, container-free helpers directly.
 */
class ImageFilterExporterSharedStoreTest {

    private static final String DOTGENERATED_DEFAULT_PATH = "DOTGENERATED_DEFAULT_PATH";

    /**
     * Given a rendition under the local dotGenerated base,
     * When mapped to the shared store,
     * Then the {@code inode[0]/inode[1]/hashedName} tail is preserved under the shared base.
     */
    @Test
    void toSharedFile_rebasesPreservingShardLayout() {
        final String localBase = "/data/dotsecure/dotGenerated";
        final String sharedBase = "/data/shared/assets/dotGenerated";
        final File local = new File(localBase + "/e/5/dotGenerated_abc123.jpg");

        final File shared = ImageFilterExporter.toSharedFile(local, localBase, sharedBase);

        assertEquals(new File(sharedBase + "/e/5/dotGenerated_abc123.jpg"), shared);
    }

    /**
     * A file outside the local base must not be mapped (e.g. a re-transform of an already-generated
     * file that lives in its source folder rather than under dotGenerated).
     */
    @Test
    void toSharedFile_returnsNullWhenOutsideLocalBase() {
        final File outside = new File("/somewhere/else/image.jpg");
        assertNull(ImageFilterExporter.toSharedFile(outside, "/data/dotsecure/dotGenerated",
                "/data/shared/assets/dotGenerated"));
    }

    /** A null local file maps to null rather than throwing. */
    @Test
    void toSharedFile_nullSafe() {
        assertNull(ImageFilterExporter.toSharedFile(null, "/a", "/b"));
    }

    /**
     * Given a finished rendition and a shared target whose parent directories do not yet exist,
     * When published,
     * Then the directories are created, the bytes match, and no temp file is left behind.
     */
    @Test
    void copyToShared_createsDirsAndCopiesBytes(@TempDir final Path tmp) throws Exception {
        final File local = tmp.resolve("local.jpg").toFile();
        final byte[] payload = "the-finished-rendition-bytes".getBytes();
        Files.write(local.toPath(), payload);

        // target parent dirs intentionally absent
        final File shared = tmp.resolve("shared/e/5/dotGenerated_abc123.jpg").toFile();
        final File sharedTmpDir = tmp.resolve("shared/tmp_upload").toFile();
        assertFalse(shared.getParentFile().exists());

        ImageFilterExporter.copyToShared(local, shared, sharedTmpDir);

        assertTrue(shared.exists());
        assertArrayEquals(payload, Files.readAllBytes(shared.toPath()));
        // staging temp file must not survive — neither in the rendition dir nor the temp dir
        assertNoStrayTmp(shared.getParentFile());
        assertNoStrayTmp(sharedTmpDir);
    }

    /**
     * Given a shared target that already holds stale bytes (an older rendition, or a partial from a
     * crashed instance),
     * When the rendition is published again,
     * Then the stale content is replaced with the fresh bytes — the real cross-instance / pre-restart
     * case the design targets, not merely re-publishing identical bytes.
     */
    @Test
    void copyToShared_overwritesStaleSharedContent(@TempDir final Path tmp) throws Exception {
        final File local = tmp.resolve("local.jpg").toFile();
        final byte[] fresh = "the-fresh-rendition".getBytes();
        Files.write(local.toPath(), fresh);

        final File shared = tmp.resolve("shared/a/b/img.jpg").toFile();
        final File sharedTmpDir = tmp.resolve("shared/tmp_upload").toFile();
        Files.createDirectories(shared.getParentFile().toPath());
        Files.write(shared.toPath(), "STALE-different-bytes-and-length".getBytes());

        ImageFilterExporter.copyToShared(local, shared, sharedTmpDir);

        assertTrue(shared.exists());
        assertArrayEquals(fresh, Files.readAllBytes(shared.toPath()));
        assertNoStrayTmp(sharedTmpDir);
    }

    /**
     * The feature gate reads {@link com.dotmarketing.util.Config} live, so it flips with the property
     * without a restart.
     */
    @Test
    void isDotGeneratedSharedCompleted_readsConfigLive() {
        final String original = Config.getStringProperty("DOTGENERATED_DEFAULT_PATH", "LOCAL");
        try {
            Config.setProperty("DOTGENERATED_DEFAULT_PATH", "SHARED_COMPLETED");
            assertTrue(ConfigUtils.isDotGeneratedSharedCompleted());

            Config.setProperty("DOTGENERATED_DEFAULT_PATH", "LOCAL");
            assertFalse(ConfigUtils.isDotGeneratedSharedCompleted());
        } finally {
            Config.setProperty("DOTGENERATED_DEFAULT_PATH", original);
        }
    }

    /**
     * Given the shipped default {@code DOTGENERATED_DEFAULT_PATH=shared} (not a recognized mode),
     * When the feature gate is read,
     * Then it behaves as {@code SHARED_COMPLETED} and generation stays under the local dotsecure path.
     */
    @Test
    void isDotGeneratedSharedCompleted_shippedDefaultSharedIsTreatedAsSharedCompleted() {
        final String original = Config.getStringProperty(DOTGENERATED_DEFAULT_PATH, "LOCAL");
        try {
            Config.setProperty(DOTGENERATED_DEFAULT_PATH, "shared");

            assertTrue(ConfigUtils.isDotGeneratedSharedCompleted());
            assertLocalGenerationPath();
        } finally {
            Config.setProperty(DOTGENERATED_DEFAULT_PATH, original);
        }
    }

    /** Recognized values are matched case-insensitively and keep their meaning. */
    @Test
    void isDotGeneratedSharedCompleted_recognizedValuesAreCaseInsensitive() {
        final String original = Config.getStringProperty(DOTGENERATED_DEFAULT_PATH, "LOCAL");
        try {
            Config.setProperty(DOTGENERATED_DEFAULT_PATH, "local");
            assertFalse(ConfigUtils.isDotGeneratedSharedCompleted());
            assertLocalGenerationPath();

            Config.setProperty(DOTGENERATED_DEFAULT_PATH, "shared_completed");
            assertTrue(ConfigUtils.isDotGeneratedSharedCompleted());
            assertLocalGenerationPath();
        } finally {
            Config.setProperty(DOTGENERATED_DEFAULT_PATH, original);
        }
    }

    /**
     * Given a made-up {@code DOTGENERATED_DEFAULT_PATH} value,
     * When the feature gate is read several times,
     * Then it behaves as {@code SHARED_COMPLETED} and a single WARN naming the value is logged.
     */
    @Test
    void isDotGeneratedSharedCompleted_unknownValueWarnsOnceAndIsTreatedAsSharedCompleted() {
        final String original = Config.getStringProperty(DOTGENERATED_DEFAULT_PATH, "LOCAL");
        final String unknownMode = "bogus-" + UUID.randomUUID();
        final Logger configUtilsLogger = (Logger) LogManager.getLogger(ConfigUtils.class);
        final Level previousLevel = configUtilsLogger.getLevel();
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        configUtilsLogger.addAppender(appender);
        configUtilsLogger.setLevel(Level.WARN);
        try {
            Config.setProperty(DOTGENERATED_DEFAULT_PATH, unknownMode);

            assertTrue(ConfigUtils.isDotGeneratedSharedCompleted());
            assertTrue(ConfigUtils.isDotGeneratedSharedCompleted());
            assertLocalGenerationPath();

            final List<String> warnings = appender.messages(Level.WARN);
            assertEquals(1, warnings.size(), () -> "warnings: " + warnings);
            assertTrue(warnings.get(0).contains(unknownMode));
            assertTrue(warnings.get(0).contains(ConfigUtils.SHARED_COMPLETED));
        } finally {
            configUtilsLogger.removeAppender(appender);
            configUtilsLogger.setLevel(previousLevel);
            appender.stop();
            Config.setProperty(DOTGENERATED_DEFAULT_PATH, original);
        }
    }

    /**
     * Given a filter chain whose first stage reads the original binary (stored under its version
     * inode) while the request names the content by identifier, and any configured mode,
     * When the result files are resolved for each stage,
     * Then every stage is written under the local dotGenerated path (never the shared store) and
     * every stage's name embeds the version inode, so the inode-scoped cleanup can find it.
     */
    @Test
    void getResultsFile_intermediatesAreLocalAndNamedByVersionInode(@TempDir final Path tmp) throws Exception {
        final String original = Config.getStringProperty(DOTGENERATED_DEFAULT_PATH, "LOCAL");
        final String inode = UUID.randomUUID().toString();
        final String identifier = UUID.randomUUID().toString();
        final File binary = tmp.resolve("assets/" + inode.charAt(0) + "/" + inode.charAt(1) + "/"
                + inode + "/fileAsset/image.jpg").toFile();
        final Map<String, String[]> parameters = new HashMap<>();
        parameters.put("filter", new String[]{"Resize", "Resize"});
        parameters.put("resize_w", new String[]{"100"});
        parameters.put("fieldVarName", new String[]{"fileAsset"});
        parameters.put("assetInodeOrIdentifier", new String[]{identifier});
        final File bucket = new File(ConfigUtils.getDotGeneratedPath() + File.separator
                + inode.charAt(0) + File.separator + inode.charAt(1));
        final boolean bucketExisted = bucket.exists();
        try {
            for (final String mode : new String[]{"shared", "LOCAL", "SHARED_COMPLETED", "bogus"}) {
                Config.setProperty(DOTGENERATED_DEFAULT_PATH, mode);

                final File firstStage = new ResizeImageFilter().getResultsFile(binary, parameters);
                final File secondStage = new ResizeImageFilter().getResultsFile(firstStage, parameters);

                for (final File stage : new File[]{firstStage, secondStage}) {
                    assertTrue(stage.getName().startsWith("dotGenerated_" + inode + "_"),
                            () -> mode + ": unexpected name " + stage.getName());
                    assertFalse(stage.getName().contains(identifier));
                    assertTrue(stage.getCanonicalPath().startsWith(
                                    new File(ConfigUtils.getDotGeneratedPath()).getCanonicalPath()),
                            () -> mode + ": not under the local dotGenerated path " + stage);
                }
            }
        } finally {
            Config.setProperty(DOTGENERATED_DEFAULT_PATH, original);
            if (!bucketExisted) {
                bucket.delete();
                bucket.getParentFile().delete();
            }
        }
    }

    /** Whatever the mode, generation happens under the instance-local dynamic content path. */
    private static void assertLocalGenerationPath() {
        assertTrue(ConfigUtils.getDotGeneratedPath().startsWith(ConfigUtils.getDynamicContentPath()),
                () -> "dotGenerated path not local: " + ConfigUtils.getDotGeneratedPath());
    }

    private static void assertNoStrayTmp(final File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        final File[] strays = dir.listFiles((d, name) -> name.contains(".tmp"));
        assertTrue(strays == null || strays.length == 0, "stray .tmp file left in " + dir);
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<Level> levels = new ArrayList<>();
        private final List<String> texts = new ArrayList<>();

        private CapturingAppender() {
            super("ImageFilterExporterSharedStoreTest-appender", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public synchronized void append(final LogEvent event) {
            levels.add(event.getLevel());
            texts.add(event.getMessage().getFormattedMessage());
        }

        private synchronized List<String> messages(final Level level) {
            final List<String> result = new ArrayList<>();
            for (int i = 0; i < levels.size(); i++) {
                if (levels.get(i) == level) {
                    result.add(texts.get(i));
                }
            }
            return result;
        }
    }
}
