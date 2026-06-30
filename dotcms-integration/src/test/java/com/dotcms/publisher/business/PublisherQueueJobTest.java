package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link PublisherQueueJob}.
 */
public class PublisherQueueJobTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private boolean isBundleQueued(final String bundleId) throws DotPublisherException {
        return PublisherAPI.getInstance().getQueueBundleIdsToProcess().stream()
                .anyMatch(b -> bundleId.equals(b.get("bundle_id").toString()));
    }

    private Status statusOf(final String bundleId) throws DotPublisherException {
        return APILocator.getPublishAuditAPI().getPublishAuditStatus(bundleId).getStatus();
    }

    /**
     * Method to test: {@link PublisherQueueJob} audit-status update (its private
     * {@code updateAuditStatus(List)} method, invoked here via reflection).
     * <p>
     * Given scenario: A bundle stuck in {@link Status#FAILED_TO_SEND_TO_ALL_GROUPS} whose number of
     * re-publish tries already exceeds {@link PublisherQueueJob#MAX_NUM_TRIES} + 1. This simulates a
     * receiver endpoint that was unreachable (e.g. {@code UnknownHostException}) on every retry, so
     * the bundle could never be confirmed as published.
     * <p>
     * Expected result: The bundle is moved to the terminal {@link Status#FAILED_TO_PUBLISH} status
     * and removed from the {@code publishing_queue}, so it can no longer be retried indefinitely.
     */
    @Test
    public void test_updateAuditStatus_finalizesBundle_whenMaxTriesExceeded() throws Exception {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .setSavePublishQueueElements(true)
                .addAssets(list(contentlet))
                .nextPersisted();

        // Simulate a bundle that has already been retried more than MAX_NUM_TRIES + 1 times against
        // an unreachable endpoint, leaving it stuck in FAILED_TO_SEND_TO_ALL_GROUPS.
        final PublishAuditHistory history = new PublishAuditHistory();
        history.setNumTries(PublisherQueueJob.MAX_NUM_TRIES + 2);

        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundle.getId());
        auditStatus.setStatusPojo(history);
        auditStatus.setStatus(Status.FAILED_TO_SEND_TO_ALL_GROUPS);
        // insertPublishAuditStatus is a no-op when the bundle already has an audit record (e.g.
        // left over from a previous test run with the same name). Explicitly update afterwards so
        // the desired status/numTries are always in place regardless of prior state.
        APILocator.getPublishAuditAPI().insertPublishAuditStatus(auditStatus);
        APILocator.getPublishAuditAPI().updatePublishAuditStatus(
                bundle.getId(), Status.FAILED_TO_SEND_TO_ALL_GROUPS, history);

        // Put the bundle assets into the publishing queue.
        PublisherAPI.getInstance().publishBundleAssets(bundle.getId(), new Date());

        // Sanity check: while in FAILED_TO_SEND_TO_ALL_GROUPS the bundle is still picked up for processing.
        assertTrue("Bundle should initially be in the publishing queue", isBundleQueued(bundle.getId()));

        // Drive the audit-status pass directly. With numTries beyond MAX_NUM_TRIES + 1 it must take
        // the terminal branch without contacting any remote endpoint.
        invokeUpdateAuditStatus();

        // The bundle must now be in a terminal failed state...
        assertEquals("Bundle should reach the terminal FAILED_TO_PUBLISH status",
                Status.FAILED_TO_PUBLISH, statusOf(bundle.getId()));

        // ...and must no longer be retried (removed from the publishing queue).
        assertFalse("Bundle should have been removed from the publishing queue",
                isBundleQueued(bundle.getId()));
    }

    /**
     * Method to test: the full {@link PublisherQueueJob} retry lifecycle for a push that fails
     * because the receiver endpoint is unreachable.
     * <p>
     * Given scenario: A real bundle is pushed (via {@link TestPushPublisher} in its opt-in
     * unreachable-endpoint failure mode) to an endpoint that cannot be reached. The send fails and
     * the bundle lands in {@link Status#FAILED_TO_SEND_TO_ALL_GROUPS}. The audit/retry pass then
     * polls that same unreachable endpoint on every cycle — the call that, before the fix, threw an
     * unhandled {@code ProcessingException} and aborted the whole job.
     * <p>
     * Expected result: Each retry is counted instead of aborting the job, and once the attempts are
     * exhausted the bundle reaches the terminal {@link Status#FAILED_TO_PUBLISH} status and is
     * removed from the {@code publishing_queue} — it no longer loops forever.
     */
    @Test
    public void test_unreachableEndpoint_bundleReachesTerminalState_endToEnd() throws Exception {
        // createEnvironment saves permissions — system user's role is locked, use admin instead.
        final User user = APILocator.getUserAPI().loadByUserByEmail(
                "admin@dotcms.com", APILocator.getUserAPI().getSystemUser(), false);

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .languageId(language.getId()).host(host).nextPersisted();

        final Environment environment = PublisherTestUtil.createEnvironment(user);
        // createEndpoint points at 127.0.0.1:999 — effectively unreachable.
        final PublishingEndPoint endpoint = PublisherTestUtil.createEndpoint(environment);
        final Bundle bundle = PublisherTestUtil.createBundle("bundle-" + System.nanoTime(), user, environment);

        try {
            // Enqueue the asset so the bundle is in publishing_queue with a due publish date.
            PublisherAPI.getInstance().addContentsToPublish(
                    list(contentlet.getIdentifier()), bundle.getId(), new Date(), user);

            // Each send attempt fails because the endpoint is unreachable (simulated by
            // TestPushPublisher). PushPublisher advances numTries on every attempt, exactly as the
            // real send/retry loop does. Push MAX_NUM_TRIES times to exhaust the retry budget.
            final List<PublishQueueElement> assets = PublisherTestUtil.getAssets(bundle, contentlet);
            Config.setProperty(TestPushPublisher.SIMULATE_UNREACHABLE_ENDPOINT, true);
            try {
                for (int i = 0; i < PublisherQueueJob.MAX_NUM_TRIES; i++) {
                    PublisherTestUtil.push(assets, bundle, user);
                }
            } finally {
                Config.setProperty(TestPushPublisher.SIMULATE_UNREACHABLE_ENDPOINT, false);
            }

            // After exhausting the retries the bundle failed to all groups and is still queued.
            assertEquals("Repeated pushes to an unreachable endpoint should fail to all groups",
                    Status.FAILED_TO_SEND_TO_ALL_GROUPS, statusOf(bundle.getId()));
            assertTrue("Bundle should still be in the publishing queue before the audit pass",
                    isBundleQueued(bundle.getId()));

            // One audit pass: polling the unreachable endpoint throws (now handled per-bundle instead
            // of aborting the job), and since the retries are exhausted the bundle is finalized.
            invokeUpdateAuditStatus();

            assertEquals("Bundle should reach the terminal FAILED_TO_PUBLISH status",
                    Status.FAILED_TO_PUBLISH, statusOf(bundle.getId()));
            assertFalse("Bundle should have been removed from the publishing queue",
                    isBundleQueued(bundle.getId()));
        } finally {
            PublisherTestUtil.cleanBundleEndpointEnv(bundle, endpoint, environment);
        }
    }

    /**
     * Method to test: the stale-bundle guard at the top of the send loop in
     * {@link PublisherQueueJob#execute}.
     * <p>
     * Given scenario: A bundle that {@code updateAuditStatus()} finalized and removed from the
     * {@code publishing_queue} in the current tick is still present in the {@code bundles} list —
     * because that list was captured via {@code getQueueBundleIdsToProcess()} <em>before</em>
     * {@code updateAuditStatus()} ran. Without the guard, the send loop would hand the stale entry
     * to {@link com.dotcms.publisher.pusher.PushPublisher#process PushPublisher.process()}, which
     * would increment {@code numTries} and flip the status back to
     * {@link Status#FAILED_TO_SEND_TO_ALL_GROUPS}.
     * <p>
     * Expected result: After the queue rows are deleted (simulating mid-tick finalization),
     * {@link PublisherAPI#getQueueElementsByBundleId} returns an empty result. This is exactly the
     * condition the guard evaluates, and a {@code continue} skips the bundle so it cannot be
     * resurrected.
     */
    @Test
    public void test_staleBundleGuard_skipsDequeuedBundle() throws Exception {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .setSavePublishQueueElements(true)
                .addAssets(list(contentlet))
                .nextPersisted();

        // Put assets into the queue — this is what makes the bundle visible in the stale 'bundles'
        // list that execute() captures before updateAuditStatus() runs.
        PublisherAPI.getInstance().publishBundleAssets(bundle.getId(), new Date());
        assertTrue("Bundle must have queue elements before finalization",
                UtilMethods.isSet(PublisherAPI.getInstance().getQueueElementsByBundleId(bundle.getId())));

        // Simulate mid-tick finalization: finalizeFailedBundle() calls
        // deleteElementsFromPublishQueueTable(), removing the queue rows for this bundle.
        PublisherAPI.getInstance().deleteElementsFromPublishQueueTable(bundle.getId());

        // The guard evaluates getQueueElementsByBundleId() at the top of the send loop. It must
        // now return empty so the guard fires and skips this stale bundle — preventing resurrection.
        assertFalse(
                "After finalization getQueueElementsByBundleId() must return empty: " +
                "this is the condition the stale-bundle guard checks before entering the send loop",
                UtilMethods.isSet(PublisherAPI.getInstance().getQueueElementsByBundleId(bundle.getId())));

        assertFalse("Bundle must no longer appear in the processing queue after finalization",
                isBundleQueued(bundle.getId()));
    }

    /**
     * Invokes the package-private-by-reflection {@code PublisherQueueJob#updateAuditStatus(List)}
     * with an empty in-queue list (the parameter is irrelevant to the branches under test).
     */
    private void invokeUpdateAuditStatus() throws Exception {
        final PublisherQueueJob job = new PublisherQueueJob();
        final Method updateAuditStatus =
                PublisherQueueJob.class.getDeclaredMethod("updateAuditStatus", List.class);
        updateAuditStatus.setAccessible(true);
        updateAuditStatus.invoke(job, Collections.<Map<String, Object>>emptyList());
    }
}
