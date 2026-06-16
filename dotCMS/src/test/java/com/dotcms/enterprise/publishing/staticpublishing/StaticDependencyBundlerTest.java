package com.dotcms.enterprise.publishing.staticpublishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.publisher.business.PublishQueueElement;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

/**
 * Unit tests for {@link StaticDependencyBundler#markerLanguages(PublishQueueElement, Set)}, which
 * decides the languages an un-published asset's static artifacts should be removed for. See issue
 * #35365.
 */
public class StaticDependencyBundlerTest {

    private Set<String> allLanguages() {
        final Set<String> languages = new LinkedHashSet<>();
        languages.add("1");
        languages.add("2");
        return languages;
    }

    private PublishQueueElement assetWithLanguage(final Integer languageId) {
        final PublishQueueElement asset = new PublishQueueElement();
        asset.setLanguageId(languageId);
        return asset;
    }

    /**
     * A language-specific un-publish removes the artifact only for that language.
     */
    @Test
    public void test_specific_language_returns_only_that_language() {
        final Collection<String> languages =
                StaticDependencyBundler.markerLanguages(assetWithLanguage(2), allLanguages());

        assertEquals(1, languages.size());
        assertTrue(languages.contains("2"));
    }

    /**
     * A null language id (language-agnostic un-publish) removes the artifact for all bundle
     * languages.
     */
    @Test
    public void test_null_language_returns_all_languages() {
        final Set<String> all = allLanguages();
        final Collection<String> languages =
                StaticDependencyBundler.markerLanguages(assetWithLanguage(null), all);

        assertEquals(all.size(), languages.size());
        assertTrue(languages.containsAll(all));
    }

    /**
     * A zero/unset language id is treated as language-agnostic and removes all bundle languages.
     */
    @Test
    public void test_zero_language_returns_all_languages() {
        final Set<String> all = allLanguages();
        final Collection<String> languages =
                StaticDependencyBundler.markerLanguages(assetWithLanguage(0), all);

        assertEquals(all.size(), languages.size());
    }
}
