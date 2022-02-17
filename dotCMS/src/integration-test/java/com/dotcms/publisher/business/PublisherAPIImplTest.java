package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class PublisherAPIImplTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static List<Status> statuses(){
        return CollectionsUtils.list(Status.BUNDLE_SENT_SUCCESSFULLY,
                Status.PUBLISHING_BUNDLE, Status.WAITING_FOR_PUBLISHING,
                Status.FAILED_INTEGRITY_CHECK);
    }

    /**
     * Method to test: com.dotcms.publisher.business.PublishAuditAPI#insertPublishAuditStatus(com.dotcms.publisher.business.PublishAuditStatus)
     * <p>
     * Given scenario: A bundle in any of the following statuses:
     * <li>BUNDLE_SENT_SUCCESSFULLY
     * <li>PUBLISHING_BUNDLE
     * <li>WAITING_FOR_PUBLISHING
     * <li>FAILED_INTEGRITY_CHECK
     * <p>
     * Expected result: Requesting the bundles ids to process should NOT containt the bundle in the
     * given status
     */

    @UseDataProvider("statuses")
    @Test
    public void test_getQueueBundleIdsToProcess_ShouldExcludeFailedIntegrityStatus(final Status statusToTest)
            throws DotPublisherException {
        // create bundle with failed_integrity_check status
        final ContentType contentTypeForContentlet = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentTypeForContentlet).nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();

        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .setSavePublishQueueElements(true)
                .addAssets(list(contentlet, language, contentType))
                .nextPersisted();

        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundle.getId());
        publishAuditStatus.setStatusPojo(new PublishAuditHistory());
        publishAuditStatus.setStatus(statusToTest);

        APILocator.getPublishAuditAPI().insertPublishAuditStatus(publishAuditStatus);

        PublisherAPI.getInstance().publishBundleAssets(bundle.getId(), new Date());

        Assert.assertTrue(PublisherAPI.getInstance().getQueueBundleIdsToProcess().stream()
                .noneMatch(bundleMap-> bundleMap.get("bundle_id").equals(bundle.getId())));

    }

}
