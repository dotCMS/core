package com.dotmarketing.portlets.contentlet.business.exporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.image.filter.ImageFilterApiImpl;
import com.dotmarketing.image.vips.VipsImageFilterApiImpl;
import com.dotmarketing.image.vips.VipsManager;
import com.dotmarketing.util.Config;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

/**
 * Pins the headline requirement: the libvips image engine is selected purely by the
 * {@code IMAGE_API_USE_LIBVIPS} feature flag, with the legacy engine as the default, and both engines
 * expose the identical filter-key contract.
 */
public class ImageFilterExporterEngineSelectionTest {

    @After
    public void resetFlag() {
        Config.setProperty(VipsManager.USE_LIBVIPS, false);
    }

    @Test
    public void flag_off_selects_legacy_engine() {
        Config.setProperty(VipsManager.USE_LIBVIPS, false);
        assertTrue("flag off must use the pure-JVM engine",
                new ImageFilterExporter().imageFilterAPI() instanceof ImageFilterApiImpl);
    }

    @Test
    public void flag_on_selects_libvips_engine() {
        Assume.assumeTrue("native libvips required", VipsManager.isAvailable());
        Config.setProperty(VipsManager.USE_LIBVIPS, true);
        assertTrue("flag on (with native libvips) must use the libvips engine",
                new ImageFilterExporter().imageFilterAPI() instanceof VipsImageFilterApiImpl);
    }

    @Test
    public void both_engines_resolve_identical_filter_keys() {
        final Map<String, String[]> p = new HashMap<>();
        p.put("filter", new String[] {"resize,crop,jpeg"});
        p.put("resize_w", new String[] {"800"});

        final Map<String, Class<? extends ImageFilter>> legacy =
                ImageFilterAPI.apiInstance.apply().resolveFilters(new HashMap<>(p));
        final Map<String, Class<? extends ImageFilter>> vips =
                new VipsImageFilterApiImpl().resolveFilters(new HashMap<>(p));

        assertEquals("URL filter-key contract must be identical across engines",
                legacy.keySet(), vips.keySet());
    }
}
