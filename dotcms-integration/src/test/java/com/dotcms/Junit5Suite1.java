package com.dotcms;

import com.dotcms.jobs.business.api.JobProcessorDiscoveryTest;
import com.dotcms.jobs.business.api.JobQueueManagerAPICDITest;
import com.dotcms.jobs.business.api.JobQueueManagerAPIIntegrationTest;
import com.dotcms.jobs.business.processor.impl.ImportContentletsProcessorIntegrationTest;
import com.dotcms.jobs.business.queue.PostgresJobQueueIntegrationTest;
import com.dotcms.rest.api.v1.content.dotimport.ContentImportResourceIntegrationTest;
import com.dotcms.rest.api.v1.job.JobQueueHelperIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        JobQueueManagerAPICDITest.class,
        PostgresJobQueueIntegrationTest.class,
        JobQueueManagerAPIIntegrationTest.class,
        JobQueueHelperIntegrationTest.class,
        ImportContentletsProcessorIntegrationTest.class,
        ContentImportResourceIntegrationTest.class,
        JobProcessorDiscoveryTest.class,
    com.dotmarketing.factories.PublishAssetPermissionIT.class,
    com.dotmarketing.portlets.links.action.PublishLinksActionIT.class,
    com.dotmarketing.portal.struts.PublishWebAssetActionIT.class
})
public class Junit5Suite1 {

}
