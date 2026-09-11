package com.dotcms.enterprise.publishing.sitesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotcms.content.index.domain.DotSearchException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONObject;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link SiteSearchIndexResources} — the SITE_SEARCH_ANALYZER override applied to
 * the bundled site-search mapping, and the default (untouched) behavior when it is unset.
 */
public class SiteSearchIndexResourcesTest extends UnitTestBase {

    @After
    public void cleanUp() {
        Config.setProperty(SiteSearchIndexResources.ANALYZER_PROPERTY, null);
    }

    /** Unset property: the bundled mapping and settings come back byte-for-byte. */
    @Test
    public void default_mapping_and_settings_are_untouched() throws Exception {
        final String settings = SiteSearchIndexResources.settings("es-sitesearch-settings.json");
        assertTrue("bundled settings must declare the default analyzer",
                settings.contains("standard_content"));

        assertEquals("unset property must return the bundled resource verbatim",
                readBundledResource("es-sitesearch-mapping.json"),
                SiteSearchIndexResources.mapping("es-sitesearch-mapping.json"));
    }

    /** Empty/whitespace values behave like unset — the bundled mapping comes back verbatim. */
    @Test
    public void blank_analyzer_values_fall_back_to_default() throws Exception {
        final String bundled = readBundledResource("es-sitesearch-mapping.json");
        for (final String blank : new String[]{"", "   "}) {
            Config.setProperty(SiteSearchIndexResources.ANALYZER_PROPERTY, blank);
            assertEquals("'" + blank + "' must behave like unset",
                    bundled, SiteSearchIndexResources.mapping("es-sitesearch-mapping.json"));
        }
    }

    private static String readBundledResource(final String resource) throws Exception {
        try (final java.io.InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * SITE_SEARCH_ANALYZER set: every text field gets the analyzer, and the ngram subfield's
     * search_analyzer follows it so queries tokenize consistently. Non-text fields stay untouched.
     */
    @Test
    public void analyzer_override_is_applied_to_text_fields() {
        // padded value also exercises the trim (env files commonly carry stray whitespace)
        Config.setProperty(SiteSearchIndexResources.ANALYZER_PROPERTY, " cjk ");

        final JSONObject properties = new JSONObject(
                SiteSearchIndexResources.mapping("os-sitesearch-mapping.json"))
                .getJSONObject("properties");

        for (final String field : new String[]{"content", "content_raw", "title", "description", "author"}) {
            assertEquals("cjk", properties.getJSONObject(field).getString("analyzer"));
        }
        final JSONObject ngram = properties.getJSONObject("content").getJSONObject("fields")
                .getJSONObject("ngram");
        assertEquals("cjk", ngram.getString("search_analyzer"));
        assertEquals("edge-ngram index analyzer must be preserved",
                "partial_content", ngram.getString("analyzer"));
        assertEquals("non-text fields must be untouched",
                "keyword", properties.getJSONObject("host").getString("type"));
    }

    /** A missing bundled resource fails loudly instead of NPE-ing. */
    @Test
    public void missing_resource_fails_loudly() {
        assertThrows(DotSearchException.class,
                () -> SiteSearchIndexResources.mapping("no-such-resource-on-classpath.json"));
    }
}
