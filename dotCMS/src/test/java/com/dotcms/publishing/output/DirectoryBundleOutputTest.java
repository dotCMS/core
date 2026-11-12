package com.dotcms.publishing.output;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.publishing.PublisherConfig;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DirectoryBundleOutput#bundleFileExists()}.
 * <p>
 * A directory bundle is created empty when the output is opened, so "the bundle exists" has to mean
 * "the directory holds something", otherwise a first attempt looks like a bundle that was already
 * built and its bundlers are skipped (issue #34434).
 */
public class DirectoryBundleOutputTest {

    private File parent;

    @Before
    public void setUp() throws IOException {
        parent = Files.createTempDirectory("directoryBundleOutputTest").toFile();
    }

    @After
    public void tearDown() {
        deleteRecursively(parent);
    }

    /**
     * Method to test: {@link DirectoryBundleOutput#bundleFileExists()}
     * Given scenario: the bundle directory does not exist
     * Expected result: false
     */
    @Test
    public void test_bundleFileExists_missingDirectory_isFalse() {
        final File missing = new File(parent, "missing");
        final DirectoryBundleOutput output = new DirectoryBundleOutput(
                mock(PublisherConfig.class), missing);

        assertFalse(output.bundleFileExists());
    }

    /**
     * Method to test: {@link DirectoryBundleOutput#bundleFileExists()}
     * Given scenario: the bundle directory exists but is empty, which is what opening the output on a
     * first attempt leaves behind
     * Expected result: false, there is no bundle yet
     */
    @Test
    public void test_bundleFileExists_emptyDirectory_isFalse() {
        final File empty = new File(parent, "empty");
        assertTrue(empty.mkdirs());
        final DirectoryBundleOutput output = new DirectoryBundleOutput(
                mock(PublisherConfig.class), empty);

        assertFalse(output.bundleFileExists());
    }

    /**
     * Method to test: {@link DirectoryBundleOutput#bundleFileExists()}
     * Given scenario: the bundle directory holds a file
     * Expected result: true
     */
    @Test
    public void test_bundleFileExists_directoryWithFile_isTrue() throws IOException {
        final File withFile = new File(parent, "withFile");
        final DirectoryBundleOutput output = new DirectoryBundleOutput(
                mock(PublisherConfig.class), withFile);

        try (OutputStream stream = output.addFile("/ROOT/page.html")) {
            stream.write(1);
        }

        assertTrue(output.bundleFileExists());
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
