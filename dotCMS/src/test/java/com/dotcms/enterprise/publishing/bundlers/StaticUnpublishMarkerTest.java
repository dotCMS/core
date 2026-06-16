package com.dotcms.enterprise.publishing.bundlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import java.io.File;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link StaticUnpublishMarker}, the helper that decides when a static un-publish
 * bundle should carry a /live/ path marker and writes it. See issue #35365.
 */
public class StaticUnpublishMarkerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String LIVE_PATH = File.separator + "live" + File.separator
            + "demo.dotcms.com" + File.separator + "1" + File.separator + "about-us"
            + File.separator + "index";

    private PublisherConfig config(final boolean isStatic, final Operation operation) {
        final PublisherConfig config = mock(PublisherConfig.class);
        when(config.isStatic()).thenReturn(isStatic);
        when(config.getOperation()).thenReturn(operation);
        return config;
    }

    private DirectoryBundleOutput output(final PublisherConfig config) {
        return new DirectoryBundleOutput(config, tempFolder.getRoot());
    }

    /**
     * Given a static endpoint bundle whose operation is UNPUBLISH, when the marker is requested,
     * then it is created at the canonical /live/ path and the call reports it handled the case.
     */
    @Test
    public void test_writeMarker_static_unpublish_creates_marker() throws Exception {
        final PublisherConfig config = config(true, Operation.UNPUBLISH);
        final DirectoryBundleOutput output = output(config);

        final boolean handled = StaticUnpublishMarker.writeMarkerIfNeeded(config, output, LIVE_PATH);

        assertTrue("static UNPUBLISH should be handled", handled);
        assertTrue("marker should exist via the bundle output", output.exists(LIVE_PATH));
        assertTrue("marker file should exist on disk", new File(tempFolder.getRoot(), LIVE_PATH).exists());
    }

    /**
     * Site Search and dynamic push bundles are not static (isStatic == false), so no marker is
     * written and the case is not handled.
     */
    @Test
    public void test_writeMarker_non_static_writes_nothing() throws Exception {
        final PublisherConfig config = config(false, Operation.UNPUBLISH);
        final DirectoryBundleOutput output = output(config);

        final boolean handled = StaticUnpublishMarker.writeMarkerIfNeeded(config, output, LIVE_PATH);

        assertFalse("non-static bundles must not be handled here", handled);
        assertFalse("no marker should be written for non-static bundles", output.exists(LIVE_PATH));
    }

    /**
     * A static PUBLISH bundle renders real content, so the marker path must never be written.
     */
    @Test
    public void test_writeMarker_static_publish_writes_nothing() throws Exception {
        final PublisherConfig config = config(true, Operation.PUBLISH);
        final DirectoryBundleOutput output = output(config);

        final boolean handled = StaticUnpublishMarker.writeMarkerIfNeeded(config, output, LIVE_PATH);

        assertFalse("PUBLISH must not be treated as un-publish", handled);
        assertFalse("no marker should be written on PUBLISH", output.exists(LIVE_PATH));
    }

    /**
     * Calling the writer twice for the same path is safe and leaves the marker in place.
     */
    @Test
    public void test_writeMarker_is_idempotent() throws Exception {
        final PublisherConfig config = config(true, Operation.UNPUBLISH);
        final DirectoryBundleOutput output = output(config);

        assertTrue(StaticUnpublishMarker.writeMarkerIfNeeded(config, output, LIVE_PATH));
        assertTrue(StaticUnpublishMarker.writeMarkerIfNeeded(config, output, LIVE_PATH));
        assertTrue(output.exists(LIVE_PATH));
    }

    /**
     * The {@code isStaticUnpublish} predicate is true only for static UNPUBLISH bundles and is
     * null-safe.
     */
    @Test
    public void test_isStaticUnpublish_predicate() {
        assertTrue(StaticUnpublishMarker.isStaticUnpublish(config(true, Operation.UNPUBLISH)));
        assertFalse(StaticUnpublishMarker.isStaticUnpublish(config(true, Operation.PUBLISH)));
        assertFalse(StaticUnpublishMarker.isStaticUnpublish(config(false, Operation.UNPUBLISH)));
        assertFalse(StaticUnpublishMarker.isStaticUnpublish(null));
    }

    /**
     * For a static un-publish, a marker is written under /live/&lt;host&gt;/&lt;lang&gt;/&lt;path&gt;
     * for each requested language.
     */
    @Test
    public void test_writeContentMarkers_writes_one_marker_per_language() throws Exception {
        final PublisherConfig config = config(true, Operation.UNPUBLISH);
        final DirectoryBundleOutput output = output(config);

        StaticUnpublishMarker.writeContentMarkers(config, output, "demo.dotcms.com",
                Arrays.asList("1", "2"), "/about-us/index");

        final String lang1 = File.separator + "live" + File.separator + "demo.dotcms.com"
                + File.separator + "1" + File.separator + "about-us" + File.separator + "index";
        final String lang2 = File.separator + "live" + File.separator + "demo.dotcms.com"
                + File.separator + "2" + File.separator + "about-us" + File.separator + "index";
        assertTrue("language 1 marker should exist", output.exists(lang1));
        assertTrue("language 2 marker should exist", output.exists(lang2));
    }

    /**
     * Non-static bundles (Site Search, dynamic push) get no content markers.
     */
    @Test
    public void test_writeContentMarkers_non_static_writes_nothing() throws Exception {
        final PublisherConfig config = config(false, Operation.UNPUBLISH);
        final DirectoryBundleOutput output = output(config);

        StaticUnpublishMarker.writeContentMarkers(config, output, "demo.dotcms.com",
                Arrays.asList("1"), "/about-us/index");

        final String lang1 = File.separator + "live" + File.separator + "demo.dotcms.com"
                + File.separator + "1" + File.separator + "about-us" + File.separator + "index";
        assertFalse("no marker should be written for non-static bundles", output.exists(lang1));
    }

    /**
     * A blank/null asset path or hostname is ignored without error.
     */
    @Test
    public void test_writeContentMarkers_ignores_blank_inputs() throws Exception {
        final PublisherConfig config = config(true, Operation.UNPUBLISH);
        final DirectoryBundleOutput output = output(config);

        StaticUnpublishMarker.writeContentMarkers(config, output, "demo.dotcms.com",
                Arrays.asList("1"), "   ");
        StaticUnpublishMarker.writeContentMarkers(config, output, null,
                Arrays.asList("1"), "/about-us/index");
        // no exception, and the temp root has no /live/ tree
        assertFalse(new File(tempFolder.getRoot(), "live").exists());
    }

    /**
     * Path traversal in the asset path must not escape the bundle /live/ root: no file is written
     * anywhere outside /live/ and the marker is skipped.
     */
    @Test
    public void test_writeMarker_rejects_path_traversal_in_asset_path() throws Exception {
        final PublisherConfig config = config(true, Operation.UNPUBLISH);
        final DirectoryBundleOutput output = output(config);

        final String evil = File.separator + "live" + File.separator + "demo.dotcms.com"
                + File.separator + "1" + File.separator + ".." + File.separator + ".."
                + File.separator + ".." + File.separator + "etc" + File.separator + "evil";

        final boolean handled = StaticUnpublishMarker.writeMarkerIfNeeded(config, output, evil);

        assertTrue("static UNPUBLISH is still 'handled' even when the marker is skipped", handled);
        // nothing escaped the bundle root
        assertFalse(new File(tempFolder.getRoot(), "etc").exists());
        assertFalse(new File(tempFolder.getRoot().getParentFile(), "etc").exists());
    }

    /**
     * Path traversal in the hostname segment is likewise rejected.
     */
    @Test
    public void test_writeMarker_rejects_path_traversal_in_hostname() throws Exception {
        final PublisherConfig config = config(true, Operation.UNPUBLISH);
        final DirectoryBundleOutput output = output(config);

        StaticUnpublishMarker.writeContentMarkers(config, output, ".." + File.separator + ".."
                + File.separator + "outside", Arrays.asList("1"), "/page/index");

        assertFalse(new File(tempFolder.getRoot().getParentFile(), "outside").exists());
    }
}
