package com.dotcms.publisher.business;

import com.dotcms.datagen.BundleDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PublisherAPIImplTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: com.dotcms.publisher.business.PublishAuditAPI#insertPublishAuditStatus(com.dotcms.publisher.business.PublishAuditStatus)
     * Given scenario: A bundle in the Status.FAILED_INTEGRITY_CHECK status
     * Expected result: Requesting the bundles ids to process should NOT containt the bundle in the
     * Status.FAILED_INTEGRITY_CHECK status
     */
    @Test
    public void test_getQueueBundleIdsToProcess_ShouldExcludeFailedIntegrityStatus()
            throws DotPublisherException {
        // create bundle with failed_integrity_check status
        final Bundle bundle = new BundleDataGen().nextPersisted();

        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundle.getId());
        publishAuditStatus.setStatusPojo(new PublishAuditHistory());
        publishAuditStatus.setStatus(Status.FAILED_INTEGRITY_CHECK);

        APILocator.getPublishAuditAPI().insertPublishAuditStatus(publishAuditStatus);

        Assert.assertTrue(PublisherAPI.getInstance().getQueueBundleIdsToProcess().stream()
                .noneMatch(bundleMap-> bundleMap.get("bundle_id").equals(bundle.getId())));

    }

}
