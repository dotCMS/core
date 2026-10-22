package com.dotcms.publisher.receiver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for {@link BundlePluginAccess#carriesPlugins(File)}.
 */
public class BundlePluginAccessTest {

    private File archive;

    @After
    public void cleanup() {
        FileUtils.deleteQuietly(archive);
    }

    private File archiveWith(final String... entryNames) throws IOException {
        archive = Files.createTempFile("bundle", ".tar.gz").toFile();
        final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(
                new GZIPOutputStream(Files.newOutputStream(archive.toPath())))) {
            for (final String name : entryNames) {
                final TarArchiveEntry entry = new TarArchiveEntry(name);
                entry.setSize(content.length);
                tar.putArchiveEntry(entry);
                tar.write(content);
                tar.closeArchiveEntry();
            }
        }
        return archive;
    }

    /**
     * Method to Test: {@link BundlePluginAccess#carriesPlugins(File)}
     * When: an entry sits in the plugins folder, written plainly, with a leading dot segment, or
     * through a parent segment that still resolves into the plugins folder
     * Should: return true, because extraction places all of them in the plugins folder.
     */
    @Test
    public void test_carriesPlugins_detectsEveryNameThatResolvesIntoThePluginsFolder() throws IOException {
        for (final String name : new String[] {
                "osgiBundles/a.jar",
                "./osgiBundles/a.jar",
                "x/../osgiBundles/a.jar",
                "x/y/../../osgiBundles/a.jar",
                "osgiBundles/sub/../a.jar"}) {
            assertTrue("must detect '" + name + "'", BundlePluginAccess.carriesPlugins(archiveWith("bundle.xml", name)));
            cleanup();
        }
    }

    /**
     * Method to Test: {@link BundlePluginAccess#carriesPlugins(File)}
     * When: no entry resolves into the plugins folder, including a folder whose name only starts
     * like it
     * Should: return false.
     */
    @Test
    public void test_carriesPlugins_ignoresEntriesOutsideThePluginsFolder() throws IOException {
        for (final String name : new String[] {
                "bundle.xml",
                "osgiBundlesExtra/a.jar",
                "other/osgiBundles/a.jar",
                "osgiBundles/../a.jar"}) {
            assertFalse("must not detect '" + name + "'", BundlePluginAccess.carriesPlugins(archiveWith(name)));
            cleanup();
        }
    }

    /**
     * Method to Test: {@link BundlePluginAccess#carriesPlugins(File)}
     * When: the file is not a readable archive
     * Should: return true, so an archive that cannot be listed is treated as carrying plugins and
     * only users with access to the plugins portlet get past the early check.
     */
    @Test
    public void test_carriesPlugins_unreadableArchive_returnsTrue() throws IOException {
        archive = Files.createTempFile("bundle", ".tar.gz").toFile();
        Files.write(archive.toPath(), "not an archive".getBytes(StandardCharsets.UTF_8));
        assertTrue(BundlePluginAccess.carriesPlugins(archive));
    }
}
