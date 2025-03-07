package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import com.dotmarketing.business.DeterministicIdentifierAPITest;
import com.dotmarketing.startup.runonce.Task230630CreateRunningIdsExperimentFieldIntegrationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(MainBaseSuite.class)
@Suite.SuiteClasses({
    DeterministicIdentifierAPITest.class,
    Task230630CreateRunningIdsExperimentFieldIntegrationTest.class,
})
public class MainSuite3a {
}
