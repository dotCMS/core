package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

    /**
     * Given scenario: one bundle with only one asset
     * Expected result: should remove the bundle from queue and audit tables
     */
    @Test
    public void test_deleteElementFromPublishQueueTable_OneAsset() throws DotPublisherException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Contentlet contentlet =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(new Date())
                .nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .setSavePublishQueueElements(true).addAssets(List.of(contentlet))
                .nextPersisted();

        insertPublishAuditStatus(Status.FAILED_TO_BUNDLE,bundle.getId());

        PublisherAPIImpl.getInstance().deleteElementsFromPublishQueueTableAndAuditStatus(bundle.getId());

        Assert.assertNull(APILocator.getPublishAuditAPI().getPublishAuditStatus(bundle.getId()));
    }

    /**
     * Given scenario: one bundle with two assets
     * Expected result: should NOT remove the bundle from queue and audit tables
     */
    @Test
    public void test_deleteElementFromPublishQueueTable_TwoAssets() throws DotPublisherException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Contentlet contentlet =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(new Date())
                .nextPersisted();

        final Contentlet contentlet2 =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(new Date())
                .nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .setSavePublishQueueElements(true)
                .addAssets(CollectionsUtils.list(contentlet, contentlet2))
                .nextPersisted();

        insertPublishAuditStatus(Status.FAILED_TO_BUNDLE,bundle.getId());

        PublisherAPIImpl.getInstance().deleteElementFromPublishQueueTableAndAuditStatus(contentlet.getIdentifier());

        Assert.assertNotNull(APILocator.getPublishAuditAPI().getPublishAuditStatus(bundle.getId()));
    }

    /**
     * Given scenario: one bundle with two assets
     * Expected result: should remove the bundle from queue and audit tables
     * NOTE: this is the plural method being tested - not to be confused with the singular one tested above
     */
    @Test
    public void test_deleteElementsFromPublishQueueTable_TwoAssets() throws DotPublisherException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Contentlet contentlet =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(new Date())
                .nextPersisted();

        final Contentlet contentlet2 =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(new Date())
                .nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .setSavePublishQueueElements(true)
                .addAssets(CollectionsUtils.list(contentlet, contentlet2))
                .nextPersisted();

        insertPublishAuditStatus(Status.FAILED_TO_BUNDLE,bundle.getId());

        PublisherAPIImpl.getInstance().deleteElementsFromPublishQueueTableAndAuditStatus(bundle.getId());

        Assert.assertNull(APILocator.getPublishAuditAPI().getPublishAuditStatus(bundle.getId()));
    }

    private void insertPublishAuditStatus(final Status status, final String bundleID) throws DotPublisherException {
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleID);
        publishAuditStatus.setStatusPojo(new PublishAuditHistory());
        publishAuditStatus.setStatus(status);
        APILocator.getPublishAuditAPI().insertPublishAuditStatus(publishAuditStatus);
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

        final Language language = new UniqueLanguageDataGen().nextPersisted();

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

    @Test
    public void test_addExperimentToPushPublishQueue_shouldAddProperly()
            throws DotDataException, DotPublisherException, DotSecurityException {

        final Experiment experiment = createExperiment();

        final Bundle bundle = new BundleDataGen().name("Experiments - " + UUIDGenerator.generateUuid())
                .setSavePublishQueueElements(true).addAssets(List.of(experiment))
                .nextPersisted();

        final List<PublishQueueElement> queue = PublisherAPI.getInstance().getQueueElementsByBundleId(bundle.getId());
        Assert.assertEquals(1, queue.size());
        Assert.assertEquals("experiment", queue.get(0).getType());

    }

//    @Test // TODO remove
    public void test_addExistingExperiment()
            throws DotDataException, DotPublisherException, DotSecurityException {

        final Optional<Experiment> experiment = APILocator.getExperimentsAPI().find("47b9a080-f2ca-4e34-a2be-2549a68602be", APILocator.systemUser());

        final Bundle bundle = new BundleDataGen().name("Exsting Experiment - " + UUIDGenerator.generateUuid())
                .setSavePublishQueueElements(true).addAssets(List.of(experiment.orElseThrow()))
                .nextPersisted();

        final List<PublishQueueElement> queue = PublisherAPI.getInstance().getQueueElementsByBundleId(bundle.getId());
        Assert.assertEquals(1, queue.size());
        Assert.assertEquals("experiment", queue.get(0).getType());

    }

    private static Experiment createExperiment() throws DotDataException, DotSecurityException {
        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        Template template = new Template();
        template.setTitle("a template " + UUIDGenerator.generateUuid());
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template = APILocator.getTemplateAPI().saveTemplate(template, host, APILocator.systemUser(), false);

        final Folder folder = new FolderDataGen().nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).languageId(1)
                .nextPersisted();

        final Experiment experiment = new ExperimentDataGen().page(page).nextPersisted();
        return experiment;
    }

}
