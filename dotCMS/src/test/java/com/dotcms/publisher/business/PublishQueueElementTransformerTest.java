package com.dotcms.publisher.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link PublishQueueElementTransformer}.
 *
 * @author Jose Castro
 * @since Jul 2, 2026
 */
public class PublishQueueElementTransformerTest {

    private MockedStatic<APILocator> apiLocatorMock;
    private LanguageAPI languageAPI;

    @Before
    public void setUp() {
        languageAPI = Mockito.mock(LanguageAPI.class);
        apiLocatorMock = mockStatic(APILocator.class);
        apiLocatorMock.when(APILocator::getLanguageAPI).thenReturn(languageAPI);
    }

    @After
    public void tearDown() {
        apiLocatorMock.close();
    }

    /**
     * Regression test for the Push Publishing queue view crashing on language-only locales.
     * <p>
     * {@code getMapForLanguage} used to build a {@code java.util.Map.of(...)} directly from
     * {@code language.getCountryCode()}, which is legitimately null for a language-only locale
     * (e.g. {@code es}, no country). {@code Map.of()} throws {@code NullPointerException} on any
     * null value, the same failure mode as Guava's {@code ImmutableMap.Builder}.
     */
    @Test
    public void testGetMap_whenLanguageIsLanguageOnly_doesNotThrowAndDefaultsCountryToBlank() {
        final Language language = new Language(1L, "es", null, "Spanish", null);
        Mockito.when(languageAPI.getLanguage("1")).thenReturn(language);

        final PublishQueueElementTransformer transformer = new PublishQueueElementTransformer();
        final Map<String, Object> result = transformer.getMap("1", PusheableAsset.LANGUAGE.getType());

        assertEquals("", result.get(PublishQueueElementTransformer.COUNTRY_CODE_KEY));
        assertEquals("es", result.get(PublishQueueElementTransformer.LANGUAGE_CODE_KEY));
        assertEquals("Spanish()", result.get(PublishQueueElementTransformer.TITLE_KEY));
    }

    /**
     * Non-regression check: a locale with both language and country must keep mapping the
     * country code through unchanged.
     */
    @Test
    public void testGetMap_whenLanguageHasCountry_mapsCountryCode() {
        final Language language = new Language(2L, "es", "CR", "Spanish", "Costa Rica");
        Mockito.when(languageAPI.getLanguage("2")).thenReturn(language);

        final PublishQueueElementTransformer transformer = new PublishQueueElementTransformer();
        final Map<String, Object> result = transformer.getMap("2", PusheableAsset.LANGUAGE.getType());

        assertEquals("CR", result.get(PublishQueueElementTransformer.COUNTRY_CODE_KEY));
        assertEquals("Spanish(CR)", result.get(PublishQueueElementTransformer.TITLE_KEY));
    }
}
