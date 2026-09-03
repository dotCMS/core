package com.dotcms;

import com.dotcms.ai.api.OpenAIVisionAPIImplTest;
import com.dotcms.ai.util.ContentToStringUtilTest;
import com.dotcms.contenttype.business.StoryBlockValidationTest;
import com.dotcms.contenttype.test.StoryBlockUtilTest;
import com.dotcms.cost.RequestCostReportTest;
import com.dotcms.jitsu.validators.AnalyticsValidatorUtilTest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.publisher.business.PublisherQueueJobTest;
import com.dotcms.rest.api.v1.drive.ContentDriveFieldFilterTest;
import com.dotcms.rest.api.v1.drive.ContentDriveHelperContentletAPIComparisonTest;
import com.dotcms.rest.api.v1.drive.ContentDriveKeywordSearchTest;
import com.dotcms.rest.api.v1.drive.ContentDriveLinksTest;
import com.dotcms.rest.api.v1.drive.ContentDriveWorkflowArchiveStepTest;
import com.dotcms.rest.api.v1.drive.ContentDriveWorkflowFilterTest;
import com.dotcms.rest.api.v1.drive.ContentDriveStatusFilterTest;
import com.dotcms.rest.api.v1.system.cache.CacheResourceIntegrationTest;
import com.dotcms.rest.api.v1.system.role.RoleResourceIntegrationTest;
import com.dotcms.security.apps.AppsAPIImplTest;
import com.dotcms.security.apps.SecretsStoreConcurrentWriteRaceTest;
import com.dotcms.security.apps.SecretsStoreWipeRegressionTest;
import com.dotcms.telemetry.collectors.MetricTimeoutTest;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithAllEndedExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithArchivedExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithDraftExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithRunningExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountPagesWithScheduledExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllArchivedExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllDraftExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllEndedExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllRunningExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.experiment.CountVariantsInAllScheduledExperimentsMetricTypeTest;
import com.dotcms.telemetry.collectors.theme.TotalSizeOfFilesPerThemeMetricTypeTest;
import com.dotcms.util.TimeMachineUtilTest;
import com.dotmarketing.business.DeterministicIdentifierAPITest;
import com.dotmarketing.business.SecondaryCategoryPermissionTest;
import com.dotmarketing.db.InodeExistenceCheckIntegrationTest;
import com.dotmarketing.factories.TreeFactoryTest;
import com.dotmarketing.fixtask.tasks.FixTask00090RecreateMissingFoldersInParentPathTest;
import com.dotmarketing.portlets.contentlet.action.ImportContentletsActionSmokeTest;
import com.dotmarketing.portlets.rules.RuleAPITest;
import com.dotmarketing.startup.runonce.Task230630CreateRunningIdsExperimentFieldIntegrationTest;
import com.dotmarketing.startup.runonce.Task250604UpdateFolderInodesTest;
import com.dotmarketing.startup.runonce.Task250826AddIndexesToUniqueFieldsTableTest;
import com.dotmarketing.startup.runonce.Task251103AddStylePropertiesColumnInMultiTreeTest;
import com.dotmarketing.startup.runonce.Task251212AddVersionColumnIndicesTableTest;
import com.dotmarketing.startup.runonce.Task260206AddUsagePortletToMenuTest;
import com.dotmarketing.startup.runonce.Task260320AddPluginsPortletToMenuTest;
import com.dotmarketing.startup.runonce.Task260407AddBaseTypeColumnToIdentifierTest;
import com.dotmarketing.startup.runonce.Task260505AddPluginsPortletToMenuTest;
import com.dotcms.job.system.event.SystemEventsClusterDeliveryIntegrationTest;
import com.dotcms.job.system.event.SystemEventsCursorIntegrationTest;
import com.dotcms.job.system.event.SystemEventsJobDelegateIntegrationTest;
import com.dotcms.job.system.event.SystemEventsReconciliationIntegrationTest;
import com.dotcms.job.system.event.SystemEventsRetentionIntegrationTest;
import com.dotmarketing.startup.runonce.Task260615AlterClusterIdLengthTest;
import com.dotmarketing.startup.runonce.Task260826CreateSystemEventCursorTableTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(MainBaseSuite.class)
@Suite.SuiteClasses({
    RuleAPITest.class,
        DeterministicIdentifierAPITest.class,
        CountPagesWithAllEndedExperimentsMetricTypeTest.class,
        CountPagesWithArchivedExperimentsMetricTypeTest.class,
        CountPagesWithDraftExperimentsMetricTypeTest.class,
        CountPagesWithRunningExperimentsMetricTypeTest.class,
        CountPagesWithScheduledExperimentsMetricTypeTest.class,
        CountVariantsInAllArchivedExperimentsMetricTypeTest.class,
        CountVariantsInAllDraftExperimentsMetricTypeTest.class,
        CountVariantsInAllEndedExperimentsMetricTypeTest.class,
        CountVariantsInAllRunningExperimentsMetricTypeTest.class,
        CountVariantsInAllScheduledExperimentsMetricTypeTest.class,
        MetricTimeoutTest.class,
        Task230630CreateRunningIdsExperimentFieldIntegrationTest.class,
        TotalSizeOfFilesPerThemeMetricTypeTest.class,
        TimeMachineUtilTest.class,
        Task250604UpdateFolderInodesTest.class,
        FixTask00090RecreateMissingFoldersInParentPathTest.class,
        AnalyticsValidatorUtilTest.class,
        Task250826AddIndexesToUniqueFieldsTableTest.class,
        SecondaryCategoryPermissionTest.class,
        RequestCostReportTest.class,
        OpenAIVisionAPIImplTest.class,
        ContentDriveFieldFilterTest.class,
        ContentDriveHelperContentletAPIComparisonTest.class,
        ContentDriveKeywordSearchTest.class,
        ContentDriveLinksTest.class,
        ContentDriveWorkflowArchiveStepTest.class,
        ContentDriveWorkflowFilterTest.class,
        ContentDriveStatusFilterTest.class,
        AppsAPIImplTest.class,
        com.dotcms.content.elasticsearch.business.ESContentletAPIImplTest.class,
        com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.class,
        com.dotcms.browser.BrowserAPITest.class,
        com.dotcms.rest.api.v1.content.search.strategies.GlobalSearchAttributeStrategyMatchingTest.class,
        com.dotcms.contenttype.test.ContentResourceTest.class,
        com.dotmarketing.portlets.htmlpages.business.render.HTMLPageAssetRenderedAPIImplIntegrationTest.class,
        com.dotcms.contenttype.business.ContentTypeDestroyAPIImplTest.class,
        com.dotcms.rest.api.v1.apps.AppsResourceTest.class,
        Task251103AddStylePropertiesColumnInMultiTreeTest.class,
        StoryBlockValidationTest.class,
        StoryBlockUtilTest.class,
        Task251212AddVersionColumnIndicesTableTest.class,
        Task260206AddUsagePortletToMenuTest.class,
        Task260320AddPluginsPortletToMenuTest.class,
        Task260505AddPluginsPortletToMenuTest.class,
        Task260407AddBaseTypeColumnToIdentifierTest.class,
        Task260615AlterClusterIdLengthTest.class,
        ImportContentletsActionSmokeTest.class,
        TreeFactoryTest.class,
        PublisherQueueJobTest.class,
        ContentToStringUtilTest.class,
        CacheResourceIntegrationTest.class,
        InodeExistenceCheckIntegrationTest.class,
        SecretsStoreWipeRegressionTest.class,
        SecretsStoreConcurrentWriteRaceTest.class,
        RoleResourceIntegrationTest.class,

        // System event delivery in a cluster (issue #36827). The migration runs first so the
        // cursor table exists before anything reads it.
        Task260826CreateSystemEventCursorTableTest.class,
        SystemEventsCursorIntegrationTest.class,
        SystemEventsJobDelegateIntegrationTest.class,
        SystemEventsClusterDeliveryIntegrationTest.class,
        SystemEventsReconciliationIntegrationTest.class,
        SystemEventsRetentionIntegrationTest.class,
})

public class MainSuite3a {

}
