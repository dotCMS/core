package com.dotcms.experience.collectors;

import com.dotcms.experience.MetricCalculationError;
import com.dotcms.experience.MetricType;
import com.dotcms.experience.MetricValue;
import com.dotcms.experience.MetricsSnapshot;
import com.dotcms.experience.collectors.api.ApiMetricAPI;
import com.dotcms.experience.collectors.api.ApiMetricTypes;
import com.dotcms.experience.collectors.container.TotalFileContainersInLivePageDatabaseMetricType;
import com.dotcms.experience.collectors.container.TotalFileContainersInLiveTemplatesDatabaseMetricType;
import com.dotcms.experience.collectors.container.TotalFileContainersInWorkingPageDatabaseMetricType;
import com.dotcms.experience.collectors.container.TotalStandardContainersInLivePageDatabaseMetricType;
import com.dotcms.experience.collectors.container.TotalStandardContainersInLiveTemplatesDatabaseMetricType;
import com.dotcms.experience.collectors.container.TotalStandardContainersInWorkingPageDatabaseMetricType;
import com.dotcms.experience.collectors.content.LastContentEditedDatabaseMetricType;
import com.dotcms.experience.collectors.content.LiveNotDefaultLanguageContentsDatabaseMetricType;
import com.dotcms.experience.collectors.content.RecentlyEditedContentDatabaseMetricType;
import com.dotcms.experience.collectors.content.TotalContentsDatabaseMetricType;
import com.dotcms.experience.collectors.content.WorkingNotDefaultLanguageContentsDatabaseMetricType;
import com.dotcms.experience.collectors.contenttype.CountOCategoryFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOConstantFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountODateFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountODateTimeFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOHiddenFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOPermissionsFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountORelationshipFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOSiteOrFolderFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOTagFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOTextAreaFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOTextFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOTimeFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOWYSIWYGFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfBinaryFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfBlockEditorFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfCheckboxFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfColumnsFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfFileFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfImageFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfJSONFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfKeyValueFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfLineDividersFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfMultiselectFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfRadioFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfRowsFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfSelectFieldsMetricType;
import com.dotcms.experience.collectors.contenttype.CountOfTabFieldsMetricType;
import com.dotcms.experience.collectors.language.HasChangeDefaultLanguagesDatabaseMetricType;
import com.dotcms.experience.collectors.language.OldStyleLanguagesVarialeMetricType;
import com.dotcms.experience.collectors.language.TotalLanguagesDatabaseMetricType;
import com.dotcms.experience.collectors.language.TotalLiveLanguagesVariablesDatabaseMetricType;
import com.dotcms.experience.collectors.language.TotalUniqueLanguagesDatabaseMetricType;
import com.dotcms.experience.collectors.language.TotalWorkingLanguagesVariablesDatabaseMetricType;
import com.dotcms.experience.collectors.site.CountOfLiveSitesWithSiteVariablesMetricType;
import com.dotcms.experience.collectors.site.CountOfSitesWithIndividualPermissionsMetricType;
import com.dotcms.experience.collectors.site.CountOfSitesWithThumbnailsMetricType;
import com.dotcms.experience.collectors.site.CountOfWorkingSitesWithSiteVariablesMetricType;
import com.dotcms.experience.collectors.site.SitesWithNoDefaultTagStorageDatabaseMetricType;
import com.dotcms.experience.collectors.site.SitesWithNoSystemFieldsDatabaseMetricType;
import com.dotcms.experience.collectors.site.SitesWithRunDashboardDatabaseMetricType;
import com.dotcms.experience.collectors.site.TotalActiveSitesDatabaseMetricType;
import com.dotcms.experience.collectors.site.TotalAliasesActiveSitesDatabaseMetricType;
import com.dotcms.experience.collectors.site.TotalAliasesAllSitesDatabaseMetricType;
import com.dotcms.experience.collectors.site.TotalSitesDatabaseMetricType;
import com.dotcms.experience.collectors.sitesearch.CountSiteSearchDocumentMetricType;
import com.dotcms.experience.collectors.sitesearch.CountSiteSearchIndicesMetricType;
import com.dotcms.experience.collectors.sitesearch.TotalSizeSiteSearchIndicesMetricType;
import com.dotcms.experience.collectors.template.TotalAdvancedTemplatesDatabaseMetricType;
import com.dotcms.experience.collectors.template.TotalBuilderTemplatesDatabaseMetricType;
import com.dotcms.experience.collectors.template.TotalTemplatesDatabaseMetricType;
import com.dotcms.experience.collectors.template.TotalTemplatesInLivePagesDatabaseMetricType;
import com.dotcms.experience.collectors.template.TotalTemplatesInWorkingPagesDatabaseMetricType;
import com.dotcms.experience.collectors.theme.TotalFilesInThemeMetricType;
import com.dotcms.experience.collectors.theme.TotalLiveContainerDatabaseMetricType;
import com.dotcms.experience.collectors.theme.TotalLiveFilesInThemeMetricType;
import com.dotcms.experience.collectors.theme.TotalThemeMetricType;
import com.dotcms.experience.collectors.theme.TotalThemeUsedInLiveTemplatesMetricType;
import com.dotcms.experience.collectors.theme.TotalThemeUsedInWorkingTemplatesMetricType;
import com.dotcms.experience.collectors.theme.TotalWorkingContainerDatabaseMetricType;
import com.dotcms.experience.collectors.urlmap.ContentTypesWithUrlMapDatabaseMetricType;
import com.dotcms.experience.collectors.urlmap.LiveContentInUrlMapDatabaseMetricType;
import com.dotcms.experience.collectors.urlmap.UrlMapPatterWithTwoVariablesDatabaseMetricType;
import com.dotcms.experience.collectors.urlmap.WorkingContentInUrlMapDatabaseMetricType;
import com.dotcms.experience.collectors.user.ActiveUsersDatabaseMetricType;
import com.dotcms.experience.collectors.user.LastLoginDatabaseMetricType;
import com.dotcms.experience.collectors.user.LastLoginUserDatabaseMetric;
import com.dotcms.experience.collectors.workflow.ActionsDatabaseMetricType;
import com.dotcms.experience.collectors.workflow.ContentTypesDatabaseMetricType;
import com.dotcms.experience.collectors.workflow.SchemesDatabaseMetricType;
import com.dotcms.experience.collectors.workflow.StepsDatabaseMetricType;
import com.dotcms.experience.collectors.workflow.SubActionsDatabaseMetricType;
import com.dotcms.experience.collectors.workflow.UniqueSubActionsDatabaseMetricType;
import com.dotcms.experience.util.MetricCaches;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

/**
 * This class collects and generates all the different Metrics that will be reported to a User or
 * any other application/client.
 *
 * @author Freddy Rodriguez
 * @since Jan 8th, 2024
 */
public final class MetricStatsCollector {

    public static final ApiMetricAPI apiStatAPI = new ApiMetricAPI();
    static final Collection<MetricType> metricStatsCollectors;

    private MetricStatsCollector() {

    }

    static {
        metricStatsCollectors = new HashSet<>();

        metricStatsCollectors.add(new LastContentEditedDatabaseMetricType());
        metricStatsCollectors.add(new RecentlyEditedContentDatabaseMetricType());
        metricStatsCollectors.add(new LastLoginDatabaseMetricType());
        metricStatsCollectors.add(new LastLoginUserDatabaseMetric());

        metricStatsCollectors.add(new SchemesDatabaseMetricType());
        metricStatsCollectors.add(new ContentTypesDatabaseMetricType());
        metricStatsCollectors.add(new StepsDatabaseMetricType());
        metricStatsCollectors.add(new ActionsDatabaseMetricType());
        metricStatsCollectors.add(new SubActionsDatabaseMetricType());
        metricStatsCollectors.add(new UniqueSubActionsDatabaseMetricType());

        metricStatsCollectors.add(new TotalContentsDatabaseMetricType());
        metricStatsCollectors.add(new WorkingNotDefaultLanguageContentsDatabaseMetricType());
        metricStatsCollectors.add(new LiveNotDefaultLanguageContentsDatabaseMetricType());
        metricStatsCollectors.add(new OldStyleLanguagesVarialeMetricType());

        metricStatsCollectors.add(new ActiveUsersDatabaseMetricType());
        metricStatsCollectors.add(new ContentTypesWithUrlMapDatabaseMetricType());
        metricStatsCollectors.add(new LiveContentInUrlMapDatabaseMetricType());
        metricStatsCollectors.add(new UrlMapPatterWithTwoVariablesDatabaseMetricType());
        metricStatsCollectors.add(new WorkingContentInUrlMapDatabaseMetricType());

        metricStatsCollectors.add(new HasChangeDefaultLanguagesDatabaseMetricType());
        metricStatsCollectors.add(new TotalLanguagesDatabaseMetricType());
        metricStatsCollectors.add(new TotalLiveLanguagesVariablesDatabaseMetricType());
        metricStatsCollectors.add(new TotalUniqueLanguagesDatabaseMetricType());
        metricStatsCollectors.add(new TotalWorkingLanguagesVariablesDatabaseMetricType());

        metricStatsCollectors.add(new CountOfSitesWithIndividualPermissionsMetricType());
        metricStatsCollectors.add(new CountOfSitesWithThumbnailsMetricType());
        metricStatsCollectors.add(new CountOfWorkingSitesWithSiteVariablesMetricType());
        metricStatsCollectors.add(new CountOfLiveSitesWithSiteVariablesMetricType());

        metricStatsCollectors.add(new SitesWithNoSystemFieldsDatabaseMetricType());
        metricStatsCollectors.add(new SitesWithNoDefaultTagStorageDatabaseMetricType());
        metricStatsCollectors.add(new SitesWithRunDashboardDatabaseMetricType());
        metricStatsCollectors.add(new TotalAliasesAllSitesDatabaseMetricType());
        metricStatsCollectors.add(new TotalAliasesActiveSitesDatabaseMetricType());
        metricStatsCollectors.add(new TotalSitesDatabaseMetricType());
        metricStatsCollectors.add(new TotalActiveSitesDatabaseMetricType());
        metricStatsCollectors.add(new TotalAdvancedTemplatesDatabaseMetricType());

        metricStatsCollectors.add(new CountSiteSearchDocumentMetricType());
        metricStatsCollectors.add(new CountSiteSearchIndicesMetricType());
        metricStatsCollectors.add(new TotalSizeSiteSearchIndicesMetricType());

        metricStatsCollectors.add(new TotalTemplatesDatabaseMetricType());
        metricStatsCollectors.add(new TotalTemplatesInLivePagesDatabaseMetricType());
        metricStatsCollectors.add(new TotalTemplatesInWorkingPagesDatabaseMetricType());
        metricStatsCollectors.add(new TotalBuilderTemplatesDatabaseMetricType());

        metricStatsCollectors.add(new TotalThemeUsedInLiveTemplatesMetricType());
        metricStatsCollectors.add(new TotalThemeUsedInWorkingTemplatesMetricType());

        metricStatsCollectors.add(new TotalLiveFilesInThemeMetricType());
        metricStatsCollectors.add(new TotalFilesInThemeMetricType());
        metricStatsCollectors.add(new TotalThemeMetricType());

        metricStatsCollectors.add(new TotalLiveContainerDatabaseMetricType());
        metricStatsCollectors.add(new TotalWorkingContainerDatabaseMetricType());

        metricStatsCollectors.add(new TotalStandardContainersInLivePageDatabaseMetricType());
        metricStatsCollectors.add(new TotalFileContainersInLivePageDatabaseMetricType());
        metricStatsCollectors.add(new TotalStandardContainersInWorkingPageDatabaseMetricType());
        metricStatsCollectors.add(new TotalFileContainersInWorkingPageDatabaseMetricType());

        metricStatsCollectors.add(new TotalFileContainersInLiveTemplatesDatabaseMetricType());
        metricStatsCollectors.add(new TotalStandardContainersInLiveTemplatesDatabaseMetricType());

        metricStatsCollectors.add(new CountOCategoryFieldsMetricType());
        metricStatsCollectors.add(new CountOConstantFieldsMetricType());
        metricStatsCollectors.add(new CountODateFieldsMetricType());
        metricStatsCollectors.add(new CountODateTimeFieldsMetricType());
        metricStatsCollectors.add(new CountOfBinaryFieldsMetricType());
        metricStatsCollectors.add(new CountOfBlockEditorFieldsMetricType());
        metricStatsCollectors.add(new CountOfCheckboxFieldsMetricType());
        metricStatsCollectors.add(new CountOfColumnsFieldsMetricType());
        metricStatsCollectors.add(new CountOfFileFieldsMetricType());
        metricStatsCollectors.add(new CountOfImageFieldsMetricType());
        metricStatsCollectors.add(new CountOfJSONFieldsMetricType());
        metricStatsCollectors.add(new CountOfKeyValueFieldsMetricType());
        metricStatsCollectors.add(new CountOfLineDividersFieldsMetricType());
        metricStatsCollectors.add(new CountOfMultiselectFieldsMetricType());
        metricStatsCollectors.add(new CountOfRadioFieldsMetricType());
        metricStatsCollectors.add(new CountOfRowsFieldsMetricType());
        metricStatsCollectors.add(new CountOfSelectFieldsMetricType());
        metricStatsCollectors.add(new CountOfTabFieldsMetricType());
        metricStatsCollectors.add(new CountOHiddenFieldsMetricType());
        metricStatsCollectors.add(new CountOPermissionsFieldsMetricType());
        metricStatsCollectors.add(new CountORelationshipFieldsMetricType());
        metricStatsCollectors.add(new CountOSiteOrFolderFieldsMetricType());
        metricStatsCollectors.add(new CountOTagFieldsMetricType());
        metricStatsCollectors.add(new CountOTextAreaFieldsMetricType());
        metricStatsCollectors.add(new CountOTextFieldsMetricType());
        metricStatsCollectors.add(new CountOTimeFieldsMetricType());
        metricStatsCollectors.add(new CountOWYSIWYGFieldsMetricType());

        metricStatsCollectors.addAll(ApiMetricTypes.INSTANCE.get());
    }

    public static MetricsSnapshot getStatsAndCleanUp() {
        final MetricsSnapshot stats = getStats();

        apiStatAPI.flushTemporalTable();
        return stats;
    }

    /**
     * Calculate a MetricSnapshot by iterating through all the MetricType collections.
     *
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public static MetricsSnapshot getStats() {

        final Collection<MetricValue> stats = new ArrayList<>();
        final Collection<MetricValue> noNumberStats = new ArrayList<>();
        Collection<MetricCalculationError> errors = new ArrayList<>();

        try {
            openDBConnection();

            for (final MetricType metricType : metricStatsCollectors) {
                try {
                    getMetricValue(metricType).ifPresent(metricValue -> {
                        if (metricValue.isNumeric()) {
                            stats.add(metricValue);
                        } else {
                            noNumberStats.add(metricValue);
                        }
                    });
                } catch (final Throwable e) {
                    errors.add(new MetricCalculationError(metricType.getMetric(), e.getMessage()));
                    Logger.debug(MetricStatsCollector.class, () ->
                            "Error while calculating Metric " + metricType.getName() + ": " + e.getMessage());
                }
            }
        } finally {
            DbConnectionFactory.closeSilently();
        }

        MetricCaches.flushAll();

        return new MetricsSnapshot.Builder()
                .stats(stats)
                .notNumericStats(noNumberStats)
                .errors(errors)
                .build();
    }

    private static Optional<MetricValue> getMetricValue(final MetricType metricType) throws DotDataException {
        final Optional<MetricValue> metricStatsOptional = metricType.getStat();

        if (metricStatsOptional.isPresent()) {
            final MetricValue metricValue = metricStatsOptional.get();

            if (metricValue.getValue() instanceof Boolean) {
                return Optional.of(new MetricValue(metricValue.getMetric(),
                        Boolean.getBoolean(metricValue.getValue().toString()) ? 1 : 0));
            }
        }

        return metricStatsOptional;
    }

    private static void openDBConnection() {
        DbConnectionFactory.getConnection();
    }
}
