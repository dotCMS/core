package com.dotcms.telemetry.collectors.language;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Collects the count of old style Language Variables
 */

public class OldStyleLanguagesVarialeMetricType implements MetricType {

    @Override
    public String getName() {
        return "OLD_STYLE_LANGUAGES_VARIABLE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of old-style Language variables";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.LANGUAGES;
    }

    @Override
    public Optional<Object> getValue() {

        final LanguageAPI languageAPI = APILocator.getLanguageAPI();

        final Set<String> languagesCodes = languageAPI.getLanguages().stream()
                .map(Language::getLanguageCode)
                .collect(Collectors.toSet());

        final Set<String> oldStyleLanguageKey = languagesCodes.stream().flatMap(languagesCode -> languageAPI.getLanguageKeys(languagesCode).stream())
                .map(LanguageKey::getKey)
                .collect(Collectors.toSet());

        return Optional.of(oldStyleLanguageKey.size());
    }
}
