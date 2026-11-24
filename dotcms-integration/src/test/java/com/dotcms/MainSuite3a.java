package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
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
import com.dotcms.telemetry.collectors.experiment.ExperimentFeatureFlagMetricTypeTest;
import com.dotcms.telemetry.collectors.theme.TotalSizeOfFilesPerThemeMetricTypeTest;
import com.dotcms.util.TimeMachineUtilTest;
import com.dotmarketing.business.DeterministicIdentifierAPITest;
import com.dotmarketing.factories.TreeFactoryTest;
import com.dotmarketing.portlets.contentlet.action.ImportContentletsActionSmokeTest;
import com.dotmarketing.startup.runonce.Task230630CreateRunningIdsExperimentFieldIntegrationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(MainBaseSuite.class)
@Suite.SuiteClasses({
        DeterministicIdentifierAPITest.class,
        com.dotcms.rest.api.v1.container.ContainerResourceCallerPermissionTest.class,
        ExperimentFeatureFlagMetricTypeTest.class,
        com.dotcms.variant.business.web.CurrentVariantWebInterceptorTest.class,
        com.dotcms.graphql.datafetcher.ContentletDataFetcherTest.class,
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
        Task230630CreateRunningIdsExperimentFieldIntegrationTest.class,
        TotalSizeOfFilesPerThemeMetricTypeTest.class,
        TimeMachineUtilTest.class,
        ImportContentletsActionSmokeTest.class,
        TreeFactoryTest.class,
    com.dotmarketing.portlets.user.ajax.UserAjaxHasUserRolesIntegrationTest.class,
    com.dotcms.rest.api.v1.user.UserResourcePasswordIntegrationTest.class,
        com.dotmarketing.webdav.DotWebdavTempPathContainmentTest.class,
    com.dotcms.rendering.js.FileJavascriptReaderTest.class,
    com.dotcms.rest.api.v1.container.ContainerResourceContentReadAccessTest.class,
        com.dotmarketing.portlets.contentlet.action.ContentImportAccessTest.class,
        com.dotmarketing.portlets.workflows.action.WorkflowTaskAccessTest.class,
        com.dotmarketing.portlets.contentlet.action.ImportContentletsActionTest.class,
        com.dotmarketing.portlets.workflows.action.EditWorkflowTaskActionTest.class,
        com.dotmarketing.portal.struts.DotCustomLoginPostActionTest.class,
        com.dotmarketing.portlets.htmlpageasset.business.render.page.PageViewSerializerRelatedContentTest.class
})
public class MainSuite3a {
}
