package com.dotcms.enterprise.publishing.sitesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotcms.content.index.domain.DotSearchException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SiteSearchIndexResources} — the SITE_SEARCH_ANALYZER override applied to
 * the bundled site-search mapping, and the default (untouched) behavior when it is unset.
 */
public class SiteSearchIndexResourcesTest extends UnitTestBase {

    private static final String CONTENT_SOURCE = "index.content.source.enabled";
    private static final String SITESEARCH_SOURCE = "index.sitesearch.source.enabled";
    private static final String[] MAPPINGS = {
            "es-sitesearch-mapping.json", "os-sitesearch-mapping.json"};
    private String originalContentSource;
    private String originalSiteSearchSource;

    /** Saves and clears source overrides so defaults do not depend on another test. */
    @Before
    public void saveSourceSettings() {
        originalContentSource = Config.getStringProperty(CONTENT_SOURCE, null);
        originalSiteSearchSource = Config.getStringProperty(SITESEARCH_SOURCE, null);
        Config.setProperty(CONTENT_SOURCE, null);
        Config.setProperty(SITESEARCH_SOURCE, null);
    }

    /** Removes the analyzer override and restores the source settings changed by these tests. */
    @After
    public void cleanUp() {
        Config.setProperty(SiteSearchIndexResources.ANALYZER_PROPERTY, null);
        Config.setProperty(CONTENT_SOURCE, originalContentSource);
        Config.setProperty(SITESEARCH_SOURCE, originalSiteSearchSource);
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

    /** Site Search keeps source by default even when content indexes explicitly disable it. */
    @Test
    public void siteSearchSourceIsEnabledByDefault() {
        Config.setProperty(CONTENT_SOURCE, false);
        for (final String resource : MAPPINGS) {
            final JSONObject source = new JSONObject(SiteSearchIndexResources.mapping(resource))
                    .optJSONObject("_source");
            assertTrue(resource, source == null || source.optBoolean("enabled", true));
        }
    }

    /** Disabling Site Search source must compose with the existing analyzer override. */
    @Test
    public void disablingSiteSearchSourcePreservesAnalyzerOverride() {
        Config.setProperty(SITESEARCH_SOURCE, false);
        Config.setProperty(CONTENT_SOURCE, true);
        Config.setProperty(SiteSearchIndexResources.ANALYZER_PROPERTY, "cjk");
        for (final String resource : MAPPINGS) {
            final JSONObject mapping = new JSONObject(SiteSearchIndexResources.mapping(resource));
            final JSONObject source = mapping.optJSONObject("_source");
            assertTrue(resource + " must explicitly disable source",
                    source != null && !source.optBoolean("enabled", true));
            assertEquals("cjk", mapping.getJSONObject("properties")
                    .getJSONObject("content").getString("analyzer"));
        }
    }

    /** Mapping creation rereads Site Search's own runtime setting, not the content setting. */
    @Test
    public void siteSearchSourceCanBeChangedAtRuntime() {
        for (final boolean enabled : new boolean[]{false, true, false}) {
            Config.setProperty(SITESEARCH_SOURCE, enabled);
            Config.setProperty(CONTENT_SOURCE, !enabled);
            for (final String resource : MAPPINGS) {
                final JSONObject source = new JSONObject(SiteSearchIndexResources.mapping(resource))
                        .optJSONObject("_source");
                assertEquals(resource, enabled,
                        source == null || source.optBoolean("enabled", true));
            }
        }
    }

    /** A missing bundled resource fails loudly instead of NPE-ing. */
    @Test
    public void missing_resource_fails_loudly() {
        assertThrows(DotSearchException.class,
                () -> SiteSearchIndexResources.mapping("no-such-resource-on-classpath.json"));
    }
}
