package com.dotcms.telemetry.collectors;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.telemetry.MetricCalculationError;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.telemetry.business.MetricsAPI;
import com.dotcms.telemetry.collectors.ai.TotalEmbeddingsIndexesMetricType;
import com.dotcms.telemetry.collectors.ai.TotalSitesUsingDotaiMetricType;
import com.dotcms.telemetry.collectors.ai.TotalSitesWithAutoIndexContentConfigMetricType;
import com.dotcms.telemetry.collectors.api.ApiMetricAPI;
import com.dotcms.telemetry.collectors.container.TotalFileContainersInLivePageDatabaseMetricType;
import com.dotcms.telemetry.collectors.container.TotalFileContainersInLiveTemplatesDatabaseMetricType;
import com.dotcms.telemetry.collectors.container.TotalFileContainersInWorkingPageDatabaseMetricType;
import com.dotcms.telemetry.collectors.container.TotalStandardContainersInLivePageDatabaseMetricType;
import com.dotcms.telemetry.collectors.container.TotalStandardContainersInLiveTemplatesDatabaseMetricType;
import com.dotcms.telemetry.collectors.container.TotalStandardContainersInWorkingPageDatabaseMetricType;
import com.dotcms.telemetry.collectors.content.LastContentEditedDatabaseMetricType;
import com.dotcms.telemetry.collectors.content.LiveNotDefaultLanguageContentsDatabaseMetricType;
import com.dotcms.telemetry.collectors.content.RecentlyEditedContentDatabaseMetricType;
import com.dotcms.telemetry.collectors.content.TotalContentsDatabaseMetricType;
import com.dotcms.telemetry.collectors.content.WorkingNotDefaultLanguageContentsDatabaseMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfBinaryFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfBlockEditorFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfCategoryFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfCheckboxFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfColumnsFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfConstantFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfDateFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfDateTimeFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfFileFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfHiddenFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfImageFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfJSONFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfKeyValueFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfLineDividersFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfMultiselectFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfPermissionsFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfRadioFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfRelationshipFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfRowsFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfSelectFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfSiteOrFolderFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfTabFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfTagFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfTextAreaFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfTextFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfTimeFieldsMetricType;
import com.dotcms.telemetry.collectors.contenttype.CountOfWYSIWYGFieldsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountExperimentsEditedInThePast30DaysMetricType;
import com.dotcms.telemetry.collectors.experiment.CountExperimentsWithBounceRateGoalMetricType;
import com.dotcms.telemetry.collectors.experiment.CountExperimentsWithExitRateGoalMetricType;
import com.dotcms.telemetry.collectors.experiment.CountExperimentsWithReachPageGoalMetricType;
import com.dotcms.telemetry.collectors.experiment.CountExperimentsWithURLParameterGoalMetricType;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithAllEndedExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithArchivedExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithDraftExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithRunningExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithScheduledExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllArchivedExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllDraftExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllEndedExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllRunningExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllScheduledExperimentsMetricType;
import com.dotcms.telemetry.collectors.experiment.ExperimentFeatureFlagMetricType;
import com.dotcms.telemetry.collectors.language.HasChangeDefaultLanguagesDatabaseMetricType;
import com.dotcms.telemetry.collectors.language.OldStyleLanguagesVarialeMetricType;
import com.dotcms.telemetry.collectors.language.TotalLanguagesDatabaseMetricType;
import com.dotcms.telemetry.collectors.language.TotalLiveLanguagesVariablesDatabaseMetricType;
import com.dotcms.telemetry.collectors.language.TotalUniqueLanguagesDatabaseMetricType;
import com.dotcms.telemetry.collectors.language.TotalWorkingLanguagesVariablesDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.CountOfLiveSitesWithSiteVariablesMetricType;
import com.dotcms.telemetry.collectors.site.CountOfSitesWithIndividualPermissionsMetricType;
import com.dotcms.telemetry.collectors.site.CountOfSitesWithThumbnailsMetricType;
import com.dotcms.telemetry.collectors.site.CountOfWorkingSitesWithSiteVariablesMetricType;
import com.dotcms.telemetry.collectors.site.SitesWithNoDefaultTagStorageDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.SitesWithNoSystemFieldsDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.SitesWithRunDashboardDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.TotalActiveSitesDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.TotalAliasesActiveSitesDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.TotalAliasesAllSitesDatabaseMetricType;
import com.dotcms.telemetry.collectors.site.TotalSitesDatabaseMetricType;
import com.dotcms.telemetry.collectors.sitesearch.CountSiteSearchDocumentMetricType;
import com.dotcms.telemetry.collectors.sitesearch.CountSiteSearchIndicesMetricType;
import com.dotcms.telemetry.collectors.sitesearch.TotalSizeSiteSearchIndicesMetricType;
import com.dotcms.telemetry.collectors.template.TotalAdvancedTemplatesDatabaseMetricType;
import com.dotcms.telemetry.collectors.template.TotalBuilderTemplatesDatabaseMetricType;
import com.dotcms.telemetry.collectors.template.TotalTemplatesDatabaseMetricType;
import com.dotcms.telemetry.collectors.template.TotalTemplatesInLivePagesDatabaseMetricType;
import com.dotcms.telemetry.collectors.template.TotalTemplatesInWorkingPagesDatabaseMetricType;
import com.dotcms.telemetry.collectors.theme.TotalFilesInThemeMetricType;
import com.dotcms.telemetry.collectors.theme.TotalLiveContainerDatabaseMetricType;
import com.dotcms.telemetry.collectors.theme.TotalLiveFilesInThemeMetricType;
import com.dotcms.telemetry.collectors.theme.TotalSizeOfFilesPerThemeMetricType;
import com.dotcms.telemetry.collectors.theme.TotalThemeMetricType;
import com.dotcms.telemetry.collectors.theme.TotalThemeUsedInLiveTemplatesMetricType;
import com.dotcms.telemetry.collectors.theme.TotalThemeUsedInWorkingTemplatesMetricType;
import com.dotcms.telemetry.collectors.theme.TotalWorkingContainerDatabaseMetricType;
import com.dotcms.telemetry.collectors.urlmap.ContentTypesWithUrlMapDatabaseMetricType;
import com.dotcms.telemetry.collectors.urlmap.LiveContentInUrlMapDatabaseMetricType;
import com.dotcms.telemetry.collectors.urlmap.UrlMapPatterWithTwoVariablesDatabaseMetricType;
import com.dotcms.telemetry.collectors.urlmap.WorkingContentInUrlMapDatabaseMetricType;
import com.dotcms.telemetry.collectors.user.ActiveUsersDatabaseMetricType;
import com.dotcms.telemetry.collectors.user.LastLoginDatabaseMetricType;
import com.dotcms.telemetry.collectors.user.LastLoginUserDatabaseMetric;
import com.dotcms.telemetry.collectors.workflow.ActionsDatabaseMetricType;
import com.dotcms.telemetry.collectors.workflow.ContentTypesDatabaseMetricType;
import com.dotcms.telemetry.collectors.workflow.SchemesDatabaseMetricType;
import com.dotcms.telemetry.collectors.workflow.StepsDatabaseMetricType;
import com.dotcms.telemetry.collectors.workflow.SubActionsDatabaseMetricType;
import com.dotcms.telemetry.collectors.workflow.UniqueSubActionsDatabaseMetricType;
import com.dotcms.telemetry.util.MetricCaches;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * This class collects and generates all the different Metrics that will be reported to a User or
 * any other application/client.
 *
 * @author Freddy Rodriguez
 * @since Jan 8th, 2024
 */
public final class MetricStatsCollector {

    public static final ApiMetricAPI apiStatAPI = CDIUtils.getBeanThrows(ApiMetricAPI.class);
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
        metricStatsCollectors.add(new TotalSizeOfFilesPerThemeMetricType());

        metricStatsCollectors.add(new TotalLiveContainerDatabaseMetricType());
        metricStatsCollectors.add(new TotalWorkingContainerDatabaseMetricType());

        if (CDIUtils.getBean(MetricsAPI.class).isPresent()) {
            final MetricsAPI metricsAPI = CDIUtils.getBean(MetricsAPI.class).get();
            metricStatsCollectors.add(new TotalStandardContainersInLivePageDatabaseMetricType(metricsAPI));
            metricStatsCollectors.add(new TotalFileContainersInLivePageDatabaseMetricType(metricsAPI));
            metricStatsCollectors.add(new TotalStandardContainersInWorkingPageDatabaseMetricType(metricsAPI));
            metricStatsCollectors.add(new TotalFileContainersInWorkingPageDatabaseMetricType(metricsAPI));

            metricStatsCollectors.add(new TotalFileContainersInLiveTemplatesDatabaseMetricType(metricsAPI));
            metricStatsCollectors.add(new TotalStandardContainersInLiveTemplatesDatabaseMetricType(metricsAPI));
        } else {
            Logger.debug(MetricStatsCollector.class, () -> "MetricsAPI could not be injected via CDI");
        }

        metricStatsCollectors.add(new CountOfCategoryFieldsMetricType());
        metricStatsCollectors.add(new CountOfConstantFieldsMetricType());
        metricStatsCollectors.add(new CountOfDateFieldsMetricType());
        metricStatsCollectors.add(new CountOfDateTimeFieldsMetricType());
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
        metricStatsCollectors.add(new CountOfHiddenFieldsMetricType());
        metricStatsCollectors.add(new CountOfPermissionsFieldsMetricType());
        metricStatsCollectors.add(new CountOfRelationshipFieldsMetricType());
        metricStatsCollectors.add(new CountOfSiteOrFolderFieldsMetricType());
        metricStatsCollectors.add(new CountOfTagFieldsMetricType());
        metricStatsCollectors.add(new CountOfTextAreaFieldsMetricType());
        metricStatsCollectors.add(new CountOfTextFieldsMetricType());
        metricStatsCollectors.add(new CountOfTimeFieldsMetricType());
        metricStatsCollectors.add(new CountOfWYSIWYGFieldsMetricType());

        metricStatsCollectors.add(new TotalSitesUsingDotaiMetricType());
        metricStatsCollectors.add(new TotalEmbeddingsIndexesMetricType());
        metricStatsCollectors.add(new TotalSitesWithAutoIndexContentConfigMetricType());

        // adding experiments metrics
        metricStatsCollectors.add(new ExperimentFeatureFlagMetricType());
        metricStatsCollectors.add(new CountVariantsInAllScheduledExperimentsMetricType());
        metricStatsCollectors.add(new CountVariantsInAllRunningExperimentsMetricType());
        metricStatsCollectors.add(new CountVariantsInAllEndedExperimentsMetricType());
        metricStatsCollectors.add(new CountVariantsInAllDraftExperimentsMetricType());
        metricStatsCollectors.add(new CountVariantsInAllArchivedExperimentsMetricType());
        metricStatsCollectors.add(new CountPagesWithScheduledExperimentsMetricType());
        metricStatsCollectors.add(new CountPagesWithRunningExperimentsMetricType());
        metricStatsCollectors.add(new CountPagesWithDraftExperimentsMetricType());
        metricStatsCollectors.add(new CountPagesWithArchivedExperimentsMetricType());
        metricStatsCollectors.add(new CountPagesWithAllEndedExperimentsMetricType());
        metricStatsCollectors.add(new CountExperimentsWithURLParameterGoalMetricType());
        metricStatsCollectors.add(new CountExperimentsWithReachPageGoalMetricType());
        metricStatsCollectors.add(new CountExperimentsWithExitRateGoalMetricType());
        metricStatsCollectors.add(new CountExperimentsWithBounceRateGoalMetricType());
        metricStatsCollectors.add(new CountExperimentsEditedInThePast30DaysMetricType());
    }

    public static MetricsSnapshot getStatsAndCleanUp() {
        final MetricsSnapshot stats = getStats();

        apiStatAPI.flushTemporaryTable();
        return stats;
    }

    /**
     * Calculate a MetricSnapshot by iterating through all the MetricType collections.
     *
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public static MetricsSnapshot getStats() {
        return getStats(Set.of());
    }
    /**
     * Calculate a MetricSnapshot by iterating through all the MetricType collections.
     *
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public static MetricsSnapshot getStats(final Set<String> metricNameSet) {

        final Collection<MetricValue> stats = new ArrayList<>();
        final Collection<MetricValue> noNumberStats = new ArrayList<>();
        Collection<MetricCalculationError> errors = new ArrayList<>();

        try {
            openDBConnection();

            for (final MetricType metricType : metricStatsCollectors) {

                // If the metricNameSet is not empty and the metricNameSet does not contain the metricType name, skip it
                if (!metricNameSet.isEmpty() && !metricNameSet.contains(metricType.getName())) {
                    continue;
                }
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
