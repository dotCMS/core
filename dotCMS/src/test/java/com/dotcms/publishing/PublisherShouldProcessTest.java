package com.dotcms.publishing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Container-free coverage of the incremental-run date check in {@link Publisher#shouldProcess}.
 *
 * <p>Bundlers stamp a file with the asset's version timestamp truncated to the whole second, while
 * the start date of an incremental run is the previous run's fire time, which keeps its
 * milliseconds. The check must therefore compare at one-second precision, or an asset changed in
 * the same second as the previous run (after it fired) is skipped by every later run.</p>
 */
public class PublisherShouldProcessTest {

    private static final String BUNDLE_ID = "bundleid";
    private static final String BUNDLE_NAME = "bundlename";
    private static final long LANGUAGE_ID = 1L;
    private static final Instant START = Instant.parse("2026-10-28T12:27:26.025Z");

    private File bundleRoot;
    private PublisherConfig config;
    private Publisher publisher;

    @Before
    public void setUp() throws IOException {
        bundleRoot = Files.createTempDirectory("publisher-should-process").toFile();
        config = mock(PublisherConfig.class);
        when(config.getId()).thenReturn(BUNDLE_ID);
        when(config.getName()).thenReturn(BUNDLE_NAME);
        when(config.getLanguage()).thenReturn(LANGUAGE_ID);
        when(config.getStartDate()).thenReturn(Date.from(START));

        publisher = new Publisher() {
            @Override
            public PublisherConfig init(final PublisherConfig config) {
                return config;
            }

            @Override
            public PublisherConfig process(final PublishStatus status) {
                return config;
            }

            @Override
            public List<Class> getBundlers() {
                return List.of();
            }
        };
        publisher.config = config;
    }

    @After
    public void tearDown() {
        deleteRecursively(bundleRoot);
    }

    /**
     * Given a file stamped at the whole second in which the previous run fired (26.000, previous
     * run fired at 26.025), then it is processed: the asset may have changed after the run fired.
     */
    @Test
    public void shouldProcess_FileStampedInTheStartSecond_IsProcessed() throws Exception {
        assertTrue(publisher.shouldProcess(bundleFile("page-a", START.toEpochMilli() / 1000 * 1000)));
    }

    /**
     * Given a file stamped at a whole second before the one in which the previous run fired
     * (25.000), then it is not processed: it predates the previous run.
     */
    @Test
    public void shouldProcess_FileStampedBeforeTheStartSecond_IsNotProcessed() throws Exception {
        assertFalse(publisher.shouldProcess(
                bundleFile("page-b", START.toEpochMilli() / 1000 * 1000 - 1000)));
    }

    /**
     * Given a file stamped after the start date, then it is processed.
     */
    @Test
    public void shouldProcess_FileStampedAfterTheStartDate_IsProcessed() throws Exception {
        assertTrue(publisher.shouldProcess(bundleFile("page-c", START.toEpochMilli() + 5000)));
    }

    /**
     * Given an incremental run without a start date (a full run), then every file is processed.
     */
    @Test
    public void shouldProcess_WithoutStartDate_ProcessesAnyFile() throws Exception {
        when(config.getStartDate()).thenReturn(null);

        assertTrue(publisher.shouldProcess(bundleFile("page-d", 1000L)));
    }

    /** Creates {@code <root>/<id>/<name>/live/<host>/<language>/<name>} stamped as given. */
    private File bundleFile(final String name, final long lastModified) throws IOException {
        final File dir = new File(bundleRoot, String.join(File.separator,
                BUNDLE_ID, BUNDLE_NAME, "live", "demo.dotcms.com", Long.toString(LANGUAGE_ID)));
        Files.createDirectories(dir.toPath());
        final File file = new File(dir, name);
        Files.createFile(file.toPath());
        file.setLastModified(lastModified);
        return file;
    }

    private static void deleteRecursively(final File file) {
        final File[] children = file.listFiles();
        if (children != null) {
            for (final File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
