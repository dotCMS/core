package com.dotcms;

import com.dotcms.ai.api.OpenAIVisionAPIImplTest;
import com.dotcms.jitsu.validators.AnalyticsValidatorUtilTest;
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
import com.dotcms.telemetry.collectors.theme.TotalSizeOfFilesPerThemeMetricTypeTest;
import com.dotcms.util.TimeMachineUtilTest;
import com.dotmarketing.business.DeterministicIdentifierAPITest;
import com.dotmarketing.business.SecondaryCategoryPermissionTest;
import com.dotmarketing.portlets.rules.RuleAPITest;
import com.dotmarketing.startup.runonce.Task230630CreateRunningIdsExperimentFieldIntegrationTest;
import com.dotmarketing.startup.runonce.Task250604UpdateFolderInodesTest;
import com.dotmarketing.startup.runonce.Task250826AddIndexesToUniqueFieldsTableTest;
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
        Task230630CreateRunningIdsExperimentFieldIntegrationTest.class,
        TotalSizeOfFilesPerThemeMetricTypeTest.class,
        TimeMachineUtilTest.class,
        Task250604UpdateFolderInodesTest.class,
        AnalyticsValidatorUtilTest.class,
        Task250826AddIndexesToUniqueFieldsTableTest.class,
        SecondaryCategoryPermissionTest.class,
        OpenAIVisionAPIImplTest.class
})

public class MainSuite3a {

}
