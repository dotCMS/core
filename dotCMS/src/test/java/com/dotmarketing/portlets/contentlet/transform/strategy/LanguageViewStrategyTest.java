package com.dotmarketing.portlets.contentlet.transform.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.APIProvider;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link LanguageViewStrategy}, covering how it maps a {@link Language} into a
 * view Map — including the language-only locale (no country code) regression from
 * <a href="https://github.com/dotCMS/core/issues/36106">Issue #36106</a>.
 *
 * @author Jose Castro
 * @since Jul 2nd, 2026
 */
public class LanguageViewStrategyTest {

    /**
     * Regression test for <a href="https://github.com/dotCMS/core/issues/36106">Issue #36106</a>.
     * <p>
     * dotCMS allows a {@code Language} to be defined with only a language code and no country
     * (e.g. {@code es}), leaving {@code country}/{@code countryCode} {@code null}. {@code mapLanguage}
     * builds its result with a Guava {@code ImmutableMap.Builder}, which throws
     * {@code NullPointerException("null value in entry: country=null")} when a null value is put.
     * Language-only locales must map to an empty string instead of null.
     */
    @Test
    public void testMapLanguage_whenCountryIsNull_doesNotThrowAndDefaultsToBlank() {
        final Language language = new Language(1L, "es", null, "Spanish", null);

        final Map<String, Object> result = LanguageViewStrategy.mapLanguage(language, false);

        assertEquals("", result.get("country"));
        assertEquals("", result.get("countryCode"));
        assertEquals("es", result.get("isoCode"));
    }

    /**
     * Same regression as above, but through the {@code wrapAsMap=true} path, which is what
     * {@link LanguageViewStrategy#transform} actually uses in production.
     */
    @Test
    public void testMapLanguage_whenWrapAsMapAndCountryIsNull_wrapsBlankCountryUnderLanguageMapKey() {
        final Language language = new Language(2L, "fr", null, "French", null);

        final Map<String, Object> result = LanguageViewStrategy.mapLanguage(language, true);

        assertTrue(result.containsKey("languageMap"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> inner = (Map<String, Object>) result.get("languageMap");
        assertEquals("", inner.get("country"));
        assertEquals("", inner.get("countryCode"));
        assertEquals("fr", inner.get("isoCode"));
        assertEquals(2L, inner.get("id"));
    }

    /**
     * Non-regression check: a locale with both language and country must keep mapping those
     * values through unchanged (no fallback to blank).
     */
    @Test
    public void testMapLanguage_whenCountryIsSet_mapsCountryAndIsoCode() {
        final Language language = new Language(3L, "es", "CR", "Spanish", "Costa Rica");

        final Map<String, Object> result = LanguageViewStrategy.mapLanguage(language, false);

        assertEquals("Costa Rica", result.get("country"));
        assertEquals("CR", result.get("countryCode"));
        assertEquals("es-cr", result.get("isoCode"));
    }

    /**
     * Exercises the actual {@code transform} entry point used by {@code DefaultTransformStrategy}
     * and {@code HistoryViewStrategy} callers, confirming a language-only locale doesn't throw
     * when resolved through {@link APIProvider#languageAPI}.
     */
    @Test
    public void testTransform_whenCountryIsNull_doesNotThrowAndPutsBlankCountryIntoMap()
            throws Exception {
        final Language language = new Language(4L, "es", null, "Spanish", null);
        final LanguageAPI languageAPI = Mockito.mock(LanguageAPI.class);
        Mockito.when(languageAPI.getLanguage(4L)).thenReturn(language);

        final APIProvider toolBox = Mockito.mock(APIProvider.class);
        final Field languageApiField = APIProvider.class.getDeclaredField("languageAPI");
        languageApiField.setAccessible(true);
        languageApiField.set(toolBox, languageAPI);

        final LanguageViewStrategy strategy = new LanguageViewStrategy(toolBox);

        final Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getLanguageId()).thenReturn(4L);

        final Map<String, Object> outputMap = new HashMap<>();
        strategy.transform(contentlet, outputMap, Collections.emptySet(), null);

        @SuppressWarnings("unchecked")
        final Map<String, Object> languageMap = (Map<String, Object>) outputMap.get("languageMap");
        assertEquals("", languageMap.get("country"));
        assertEquals("", languageMap.get("countryCode"));
        assertEquals("es", languageMap.get("isoCode"));
    }
}
