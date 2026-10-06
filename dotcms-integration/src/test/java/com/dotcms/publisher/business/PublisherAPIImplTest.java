package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
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
import com.liferay.portal.model.User;
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

    /**
     * Given scenario: the SAME asset is queued in two different bundles, and it is deleted from
     *                 one of them by id.
     * Expected result: only that bundle's queue entry is removed. The other bundle still lists the
     *                  asset and will still publish it.
     *
     * This is the scenario from issue #36861. The pre-existing single-argument overload deletes
     * `WHERE asset = ?` with no bundle predicate, so it would clear both.
     */
    @Test
    public void test_deleteElementFromBundle_leavesOtherBundleIntact() throws DotPublisherException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();

        final Contentlet sharedContentlet = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(new Date())
                .nextPersisted();

        final Bundle bundleA = new BundleDataGen()
                .setSavePublishQueueElements(true)
                .addAssets(CollectionsUtils.list(sharedContentlet))
                .nextPersisted();

        final Bundle bundleB = new BundleDataGen()
                .setSavePublishQueueElements(true)
                .addAssets(CollectionsUtils.list(sharedContentlet))
                .nextPersisted();

        PublisherAPIImpl.getInstance()
                .deleteElementFromPublishQueueTableAndAuditStatus(
                        sharedContentlet.getIdentifier(), bundleA.getId());

        Assert.assertTrue("Bundle A should no longer queue the asset",
                PublisherAPIImpl.getInstance().getQueueElementsByBundleId(bundleA.getId()).isEmpty());
        Assert.assertFalse("Bundle B must keep its own queue entry for the same asset",
                PublisherAPIImpl.getInstance().getQueueElementsByBundleId(bundleB.getId()).isEmpty());
    }

    /**
     * Given scenario: a bundle with two assets; one is deleted, then the other.
     * Expected result: the publish-audit status survives the first delete and is removed only when
     *                  the second delete empties the bundle's queue.
     */
    @Test
    public void test_deleteElementFromBundle_auditStatusOnlyWhenEmptied() throws DotPublisherException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();

        final Contentlet first = new ContentletDataGen(contentType.id())
                .languageId(language.getId()).host(host).modeDate(new Date()).nextPersisted();
        final Contentlet second = new ContentletDataGen(contentType.id())
                .languageId(language.getId()).host(host).modeDate(new Date()).nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .setSavePublishQueueElements(true)
                .addAssets(CollectionsUtils.list(first, second))
                .nextPersisted();

        insertPublishAuditStatus(Status.FAILED_TO_BUNDLE, bundle.getId());

        PublisherAPIImpl.getInstance()
                .deleteElementFromPublishQueueTableAndAuditStatus(first.getIdentifier(), bundle.getId());

        Assert.assertNotNull("Audit status must survive while the bundle still has elements",
                APILocator.getPublishAuditAPI().getPublishAuditStatus(bundle.getId()));

        PublisherAPIImpl.getInstance()
                .deleteElementFromPublishQueueTableAndAuditStatus(second.getIdentifier(), bundle.getId());

        Assert.assertNull("Audit status must be removed once the bundle's queue is empty",
                APILocator.getPublishAuditAPI().getPublishAuditStatus(bundle.getId()));
    }

    /**
     * Given scenario: the bundle-scoped delete is called with a null or blank bundle id.
     * Expected result: it fails loudly and deletes nothing.
     *
     * `publishing_queue.bundle_id` is nullable with no foreign key, so a null can genuinely reach
     * this method. Silently dropping the predicate would turn a one-bundle delete into a
     * `WHERE asset = ?` that clears the asset from EVERY bundle - the exact behaviour this change
     * exists to stop.
     */
    @Test
    public void test_deleteElementFromBundle_nullBundleIdDoesNotWiden() throws DotPublisherException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();

        final Contentlet sharedContentlet = new ContentletDataGen(contentType.id())
                .languageId(language.getId()).host(host).modeDate(new Date()).nextPersisted();

        final Bundle bundleA = new BundleDataGen()
                .setSavePublishQueueElements(true)
                .addAssets(CollectionsUtils.list(sharedContentlet))
                .nextPersisted();
        final Bundle bundleB = new BundleDataGen()
                .setSavePublishQueueElements(true)
                .addAssets(CollectionsUtils.list(sharedContentlet))
                .nextPersisted();

        for (final String badBundleId : new String[]{null, "", "   "}) {
            try {
                PublisherAPIImpl.getInstance()
                        .deleteElementFromPublishQueueTableAndAuditStatus(
                                sharedContentlet.getIdentifier(), badBundleId);
                Assert.fail("Expected a failure for bundleId=[" + badBundleId + "]");
            } catch (final IllegalArgumentException | DotPublisherException expected) {
                // expected - the call must be rejected, not silently widened
            }
        }

        Assert.assertFalse("Bundle A must be untouched by a rejected delete",
                PublisherAPIImpl.getInstance().getQueueElementsByBundleId(bundleA.getId()).isEmpty());
        Assert.assertFalse("Bundle B must be untouched by a rejected delete",
                PublisherAPIImpl.getInstance().getQueueElementsByBundleId(bundleB.getId()).isEmpty());
    }

    /**
     * Given scenario: several bundles, resolved for a CMS Admin and for a limited (contributor)
     *                 user.
     * Expected result: the batched {@link PublishQueuePermissionFilter} returns exactly the same
     *                  set of bundle ids the per-item {@code doesUserHavePermission} loop returns.
     *
     * AC-010. The point of this test is <b>parity</b>, not speed: batching the N+1 permission
     * check (DEC-002 / ADR-0020) must not change who can see which bundles. A too-permissive
     * filter exposes other people's bundles and a too-strict one hides your own, and neither
     * throws - so only a comparison against the old behaviour can catch it.
     */
    @Test
    public void test_bundlePermissionFilter_matchesPerItemResult() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();

        final java.util.Map<String, PublishQueueElement> firstElementByBundle =
                new java.util.LinkedHashMap<>();
        final List<Contentlet> seededAssets = new java.util.ArrayList<>();

        for (int i = 0; i < 3; i++) {
            final Contentlet contentlet = new ContentletDataGen(contentType.id())
                    .languageId(language.getId()).host(host).modeDate(new Date()).nextPersisted();
            seededAssets.add(contentlet);

            final Bundle bundle = new BundleDataGen()
                    .setSavePublishQueueElements(true)
                    .addAssets(CollectionsUtils.list(contentlet))
                    .nextPersisted();

            final List<PublishQueueElement> elements =
                    PublisherAPIImpl.getInstance().getQueueElementsByBundleId(bundle.getId());
            Assert.assertFalse("seeded bundle should have a queue element", elements.isEmpty());
            firstElementByBundle.put(bundle.getId(), elements.get(0));
        }

        // A role and user created for this test alone. TestUserUtils.getJoeContributorUser() is
        // deliberately NOT used: it returns the shared, pre-existing joe@dotcms.com whenever one
        // exists, skipping the role grant for this test's fresh host - so joe would hold nothing
        // on any seeded asset and the comparison would be empty-vs-empty.
        final Role limitedRole = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(limitedRole).nextPersisted();

        // The PARTIAL grant that makes this assertion load-bearing: PUBLISH on the FIRST seeded
        // asset only. Saving an individual permission also breaks inheritance from the host and
        // content type, so nothing ambient can widen the result.
        final String firstBundleId = firstElementByBundle.keySet().iterator().next();
        final Contentlet firstAsset = seededAssets.get(0);
        APILocator.getPermissionAPI().save(
                new Permission(firstAsset.getPermissionId(), limitedRole.getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH, true),
                firstAsset, APILocator.systemUser(), false);

        final List<User> users = list(APILocator.systemUser(), limitedUser);

        for (final User user : users) {
            final java.util.Set<String> expected = new java.util.HashSet<>();

            for (final java.util.Map.Entry<String, PublishQueueElement> entry
                    : firstElementByBundle.entrySet()) {
                final PublishQueueElement element = entry.getValue();
                final PermissionableProxy proxy = new PermissionableProxy();
                proxy.setIdentifier(element.getAsset());
                proxy.setType(element.getType());
                proxy.setInode(element.getAsset());

                // The exact call the JSP used to make, including its respectFrontendRoles default.
                if (APILocator.getPermissionAPI().doesUserHavePermission(
                        proxy, PermissionAPI.PERMISSION_PUBLISH, user)) {
                    expected.add(entry.getKey());
                }
            }

            final java.util.Set<String> actual =
                    PublishQueuePermissionFilter.permittedBundleIds(firstElementByBundle, user);

            Assert.assertEquals(
                    "Batched permission filter must match the per-item result for user "
                            + user.getUserId(),
                    expected, actual);
        }

        // Parity alone is not enough: the system user is permitted on everything and both paths
        // short-circuit for it, so an implementation returning "all" or "none" would still satisfy
        // the loop above. These pin the limited user to a STRICT, NON-EMPTY SUBSET - one bundle of
        // three - which neither a `return keySet()` nor a `return emptySet()` stub can produce.
        final java.util.Set<String> limitedResult =
                PublishQueuePermissionFilter.permittedBundleIds(firstElementByBundle, limitedUser);

        Assert.assertEquals("the limited user must see exactly the one bundle it was granted",
                1, limitedResult.size());
        Assert.assertTrue("the granted bundle must be the one whose head asset carries the grant",
                limitedResult.contains(firstBundleId));
    }

    /**
     * Given scenario: a bundle whose first queue element cannot be resolved (a null entry, which is
     *                 what a NULL {@code bundle_id} in publishing_queue produces).
     * Expected result: the bundle is simply absent from the permitted set - no NPE.
     *
     * AC-008. See finding F1: the render-time NPE needs an unresolvable bundle, not the
     * "zero-element bundle" the issue described, which cannot reach that code path at all.
     */
    @Test
    public void test_bundlePermissionFilter_toleratesUnresolvableBundle() throws Exception {
        final java.util.Map<String, PublishQueueElement> withNull = new java.util.LinkedHashMap<>();
        withNull.put("bundle-with-no-resolvable-element", null);

        final java.util.Set<String> permitted =
                PublishQueuePermissionFilter.permittedBundleIds(withNull, APILocator.systemUser());

        Assert.assertTrue("An unresolvable bundle must be absent, not an error",
                permitted.isEmpty());
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
