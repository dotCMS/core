package com.dotcms;

import com.dotcms.jobs.business.api.JobProcessorDiscoveryTest;
import com.dotcms.jobs.business.api.JobQueueManagerAPICDITest;
import com.dotcms.jobs.business.api.JobQueueManagerAPIIntegrationTest;
import com.dotcms.jobs.business.processor.impl.ImportContentletsProcessorIntegrationTest;
import com.dotcms.jobs.business.queue.PostgresJobQueueIntegrationTest;
import com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshResourceIntegrationTest;
import com.dotcms.rest.api.v1.content.dotimport.ContentImportResourceIntegrationTest;
import com.dotcms.rest.api.v1.job.JobQueueHelperIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Integration tests written against JUnit 5.
 * <p>
 * The split between this suite and {@code MainSuite1a}/{@code 1b}/{@code 2a}/{@code 2b}/{@code 3a} is
 * by test framework, not by subject area. Those run {@code @RunWith(MainBaseSuite.class)} with JUnit 4's
 * {@code @SuiteClasses}, so a {@code org.junit.jupiter} test listed there is silently skipped — it never
 * runs, and nothing reports that it did not. A JUnit 5 test therefore belongs here regardless of what it
 * covers, and a JUnit 4 test belongs in a MainSuite.
 * <p>
 * Failsafe picks up both families ({@code **}{@code /MainSuite*.java} and
 * {@code **}{@code /Junit5Suite*.java}, dotcms-integration/pom.xml), and each becomes its own CI job.
 */
@Suite
@SelectClasses({
        JobQueueManagerAPICDITest.class,
        PostgresJobQueueIntegrationTest.class,
        JobQueueManagerAPIIntegrationTest.class,
        JobQueueHelperIntegrationTest.class,
        ImportContentletsProcessorIntegrationTest.class,
        ContentImportResourceIntegrationTest.class,
        BulkRefreshResourceIntegrationTest.class,
        JobProcessorDiscoveryTest.class
})
public class Junit5Suite1 {

}
