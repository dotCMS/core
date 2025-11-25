package com.dotcms;

import com.dotcms.jobs.business.api.JobQueueManagerAPICDITest;
import com.dotcms.jobs.business.api.JobQueueManagerAPIIntegrationTest;
import com.dotcms.jobs.business.queue.PostgresJobQueueIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        JobQueueManagerAPICDITest.class,
        PostgresJobQueueIntegrationTest.class,
        JobQueueManagerAPIIntegrationTest.class
})
public class Junit5Suite1 {

}
