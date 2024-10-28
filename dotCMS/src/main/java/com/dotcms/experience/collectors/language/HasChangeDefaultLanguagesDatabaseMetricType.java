package com.dotcms.experience.collectors.language;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Checks if the default language was changed from English
 */
public class HasChangeDefaultLanguagesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "IS_DEFAULT_LANGUAGE_NOT_ENGLISH";
    }

    @Override
    public String getDescription() {
        return "Has default language been changed from English?";
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
    public String getSqlQuery() {
        return "SELECT language_code <> 'en'  AS value FROM language " +
                "WHERE id IN (SELECT default_language_id FROM company)";
    }
}