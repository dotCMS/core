package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.StaticPublishingEndPointDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublishClientFactory;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.lang.reflect.Method;
import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientProperties;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

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
    /**
     * Method to test: {@code PushPublishClientFactory.newClient()}, the single place that builds the
     * REST client used by {@link com.dotcms.publisher.pusher.PushPublisher} to upload bundles and by
     * {@link PublisherQueueJob} to poll bundle status on the receivers.
     * <p>
     * Given scenario: {@code PUSH_PUBLISH_CONNECT_TIMEOUT_MS} is set, unset, and set to zero.
     * <p>
     * Expected result: The client carries the configured value as Jersey's
     * {@link ClientProperties#CONNECT_TIMEOUT}; with no property the default is 10000 ms; with zero
     * no bound is applied (Jersey's "wait forever", today's behavior). Before the fix the factory
     * does not exist and the client built by {@code RestClientBuilder} has no connect timeout at all,
     * which is what let one unreachable endpoint block the publisher for minutes per attempt.
     */
    @Test
    public void test_clientFactory_appliesConfiguredConnectTimeout() throws Exception {
        final String key = "PUSH_PUBLISH_CONNECT_TIMEOUT_MS";
        final String original = Config.getStringProperty(key, null);
        try {
            Config.setProperty(key, 2500);
            assertEquals("Configured value must be applied as the Jersey connect timeout",
                    2500, newPublishClient().getConfiguration().getProperty(ClientProperties.CONNECT_TIMEOUT));

            Config.setProperty(key, null);
            assertEquals("Default connect timeout must be 10 seconds",
                    10000, newPublishClient().getConfiguration().getProperty(ClientProperties.CONNECT_TIMEOUT));

            Config.setProperty(key, 0);
            final Object unbounded = newPublishClient().getConfiguration().getProperty(ClientProperties.CONNECT_TIMEOUT);
            assertTrue("Zero must leave the connect wait unbounded (property absent or 0)",
                    unbounded == null || Integer.valueOf(0).equals(unbounded));

            // The shared builder used by the integrity checker, announcements loader and API-token
            // code must stay untouched: the timeout belongs to the push-publish clients only.
            Config.setProperty(key, 2500);
            assertNull("RestClientBuilder.newClient() must not carry the push-publish connect timeout",
                    com.dotcms.rest.RestClientBuilder.newClient().getConfiguration()
                            .getProperty(ClientProperties.CONNECT_TIMEOUT));
        } finally {
            Config.setProperty(key, original);
        }
    }

    private static Client newPublishClient() {
        return PushPublishClientFactory.newClient();
    }

    /**
     * Method to test: the per-bundle fault isolation in {@link PublisherQueueJob#execute}.
     * <p>
     * Given scenario: Two bundles are due in the publishing queue. The first one carries a queue row
     * whose {@code operation} column is NULL, which makes the send loop throw a
     * {@code NullPointerException} while reading it. Before the fix, only
     * {@code DotPublishingException} was caught per bundle: any other throwable escaped to the
     * outer catch, the run ended, and the second bundle was never looked at. On the next tick the
     * broken bundle sorted first again and the run died again.
     * <p>
     * Expected result: The broken bundle is finalized as {@link Status#FAILED_TO_PUBLISH} with the
     * error recorded and its queue rows removed, and the second bundle is still processed in the same
     * run (it gets an audit row; its send to the unreachable endpoint fails on its own merits).
     */
    @Test
    public void test_unexpectedErrorInOneBundle_finalizesItAndContinues() throws Exception {
        final User user = APILocator.getUserAPI().loadByUserByEmail(
                "admin@dotcms.com", APILocator.getUserAPI().getSystemUser(), false);

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();
        final Contentlet contentA = new ContentletDataGen(contentType.id())
                .languageId(language.getId()).host(host).nextPersisted();
        final Contentlet contentB = new ContentletDataGen(contentType.id())
                .languageId(language.getId()).host(host).nextPersisted();

        final Environment environment = PublisherTestUtil.createEnvironment(user);
        // 127.0.0.1:999 — refused immediately, so bundle B fails fast on its own.
        final PublishingEndPoint endpoint = PublisherTestUtil.createEndpoint(environment);
        final Bundle bundleA = PublisherTestUtil.createBundle("broken-" + System.nanoTime(), user, environment);
        final Bundle bundleB = PublisherTestUtil.createBundle("behind-" + System.nanoTime(), user, environment);

        try {
            final long now = System.currentTimeMillis();
            // A is due first, B right after it, so A is processed first in the loop.
            PublisherAPI.getInstance().addContentsToPublish(
                    list(contentA.getIdentifier()), bundleA.getId(), new Date(now - 120_000L), user);
            PublisherAPI.getInstance().addContentsToPublish(
                    list(contentB.getIdentifier()), bundleB.getId(), new Date(now - 60_000L), user);

            // Corrupt A: a NULL operation makes Integer.parseInt(bundle.get("operation").toString())
            // in the send loop throw a NullPointerException.
            new DotConnect()
                    .setSQL("update publishing_queue set operation = null where bundle_id = ?")
                    .addParam(bundleA.getId())
                    .loadResult();

            runJobOnce();

            final PublishAuditStatus auditA = APILocator.getPublishAuditAPI()
                    .getPublishAuditStatus(bundleA.getId());
            assertTrue("The broken bundle must have an audit row after the run", auditA != null);
            assertEquals("The broken bundle must be finalized as FAILED_TO_PUBLISH",
                    Status.FAILED_TO_PUBLISH, auditA.getStatus());
            assertFalse("The broken bundle must be removed from the publishing queue",
                    isBundleQueued(bundleA.getId()));
            assertTrue("The broken bundle must record the error in its endpoint detail",
                    APILocator.getPublishAuditAPI().getPublishAuditStatus(bundleA.getId())
                            .getStatusPojo().getEndpointsMap().values().stream()
                            .flatMap(group -> group.values().stream())
                            .anyMatch(detail -> UtilMethods.isSet(detail.getInfo())));

            final PublishAuditStatus auditB = APILocator.getPublishAuditAPI()
                    .getPublishAuditStatus(bundleB.getId());
            assertTrue("The bundle behind the broken one must still be processed in the same run",
                    auditB != null);
            assertEquals("The bundle behind the broken one fails on its own merits (endpoint refused)",
                    Status.FAILED_TO_SEND_TO_ALL_GROUPS, auditB.getStatus());
        } finally {
            PublisherTestUtil.cleanBundleEndpointEnv(bundleA, endpoint, environment);
            try {
                APILocator.getPublishAuditAPI().deletePublishAuditStatus(bundleB.getId());
                PublisherAPI.getInstance().deleteElementsFromPublishQueueTable(bundleB.getId());
                APILocator.getBundleAPI().deleteBundleAndDependencies(bundleB.getId(), user);
            } catch (Exception e) {
                // best-effort cleanup
            }
        }
    }

    // ---- Per-endpoint status evaluation (issue #34434) ----

    /**
     * Method to test: {@link PublisherQueueJob} audit pass, bundle status for an environment with two
     * static endpoints.
     * <p>
     * Given scenario: one endpoint is SUCCESS and the other FAILED_TO_PUBLISH, in the same environment.
     * <p>
     * Expected result: {@link Status#FAILED_TO_SEND_TO_SOME_GROUPS}. Today the job counts the
     * environment once, so it reads as failed to every group, and an environment whose endpoints
     * mix results can never be told apart from one that failed entirely.
     */
    @Test
    public void test_status_oneSuccessOneFailedEndpoint_isFailedToSendToSome() throws Exception {
        assertStatusForTwoStaticEndpoints(Status.SUCCESS, Status.FAILED_TO_PUBLISH,
                Status.FAILED_TO_SEND_TO_SOME_GROUPS);
    }

    /**
     * Method to test: same as above.
     * <p>
     * Given scenario: one endpoint is BUNDLE_SAVED_SUCCESSFULLY and the other FAILED_TO_PUBLISH.
     * <p>
     * Expected result: {@link Status#FAILED_TO_SEND_TO_SOME_GROUPS}, never
     * {@link Status#BUNDLE_SAVED_SUCCESSFULLY} (AC-003: a failed endpoint is never hidden).
     */
    @Test
    public void test_status_oneSavedOneFailedEndpoint_isNotSaved() throws Exception {
        assertStatusForTwoStaticEndpoints(Status.BUNDLE_SAVED_SUCCESSFULLY, Status.FAILED_TO_PUBLISH,
                Status.FAILED_TO_SEND_TO_SOME_GROUPS);
    }

    /**
     * Method to test: same as above, single-endpoint environments (regression guard, AC-002).
     * <p>
     * Expected result: the final status is the one the job always produced for one endpoint.
     */
    @Test
    public void test_status_singleEndpoint_keepsItsStatus() throws Exception {
        assertStatusForOneStaticEndpoint(Status.SUCCESS, Status.SUCCESS);
        assertStatusForOneStaticEndpoint(Status.FAILED_TO_PUBLISH, Status.FAILED_TO_SEND_TO_ALL_GROUPS);
        assertStatusForOneStaticEndpoint(Status.PUBLISHING_BUNDLE, Status.PUBLISHING_BUNDLE);
        assertStatusForOneStaticEndpoint(Status.BUNDLE_SAVED_SUCCESSFULLY, Status.BUNDLE_SAVED_SUCCESSFULLY);
    }

    /**
     * Method to test: {@link PublisherQueueJob} merging of a polled remote history.
     * <p>
     * Given scenario: the local audit lists a static endpoint (BUNDLE_SAVED_SUCCESSFULLY) and an
     * {@code http} endpoint (BUNDLE_SENT_SUCCESSFULLY); the receiver answers with its own history,
     * keyed by the receiver's address, saying SUCCESS.
     * <p>
     * Expected result: the static endpoint keeps its entry and the {@code http} endpoint's entry
     * becomes SUCCESS; no entry is replaced by the other protocol's.
     */
    @Test
    public void test_merge_remoteHistory_updatesOnlyTheHttpEndpoint() throws Exception {
        final PublishAuditHistory remote = new PublishAuditHistory();
        remote.addOrUpdateEndpoint("127.0.0.1", "127.0.0.1", endpointDetail(Status.SUCCESS, "installed"));

        try (AuditStub stub = new AuditStub(list(remote.getSerialized()))) {
            final User user = TestUserUtils.getAdminUser();
            final Environment environment = PublisherTestUtil.createEnvironment(user, true);
            final PublishingEndPoint http = PublisherTestUtil.createEndpoint(environment);
            http.setPort(String.valueOf(stub.port()));
            APILocator.getPublisherEndPointAPI().updateEndPoint(http);
            final PublishingEndPoint staticEndpoint = new StaticPublishingEndPointDataGen()
                    .environment(environment).protocol(StaticPublisher.PROTOCOL_STATIC).nextPersisted();
            final Bundle bundle = PublisherTestUtil.createBundle("bundle-" + System.nanoTime(), user, environment);
            try {
                seedAudit(bundle, environment, Map.of(
                        staticEndpoint.getId(), Status.BUNDLE_SAVED_SUCCESSFULLY,
                        http.getId(), Status.BUNDLE_SENT_SUCCESSFULLY));

                invokeUpdateAuditStatus();

                final Map<String, EndpointDetail> endpoints = endpointsOf(bundle, environment);
                assertEquals(Status.BUNDLE_SAVED_SUCCESSFULLY.getCode(),
                        endpoints.get(staticEndpoint.getId()).getStatus());
                assertEquals(Status.SUCCESS.getCode(), endpoints.get(http.getId()).getStatus());
                assertFalse("A merged success must not read as a failure: " + statusOf(bundle.getId()),
                        statusOf(bundle.getId()) == Status.FAILED_TO_SEND_TO_ALL_GROUPS
                                || statusOf(bundle.getId()) == Status.FAILED_TO_SEND_TO_SOME_GROUPS);
            } finally {
                cleanUpAudit(bundle);
                APILocator.getPublisherEndPointAPI().deleteEndPointById(staticEndpoint.getId());
                PublisherTestUtil.cleanBundleEndpointEnv(bundle, http, environment);
            }
        }
    }

    /**
     * Method to test: {@link PublisherQueueJob} handling of a remote answer it cannot process.
     * <p>
     * Given scenario: the receiver answers with a history that has no endpoints map, so merging it
     * fails.
     * <p>
     * Expected result: the failure is recorded as FAILED_TO_PUBLISH on the {@code http} endpoint's
     * own id, and no {@code failed-remote-group-<timestamp>} key is left in the audit.
     */
    @Test
    public void test_merge_unprocessableRemoteHistory_failsTheEndpointByName() throws Exception {
        try (AuditStub stub = new AuditStub(list("<com.dotcms.publisher.business.PublishAuditHistory/>"))) {
            final User user = TestUserUtils.getAdminUser();
            final Environment environment = PublisherTestUtil.createEnvironment(user, true);
            final PublishingEndPoint http = PublisherTestUtil.createEndpoint(environment);
            http.setPort(String.valueOf(stub.port()));
            APILocator.getPublisherEndPointAPI().updateEndPoint(http);
            final Bundle bundle = PublisherTestUtil.createBundle("bundle-" + System.nanoTime(), user, environment);
            try {
                seedAudit(bundle, environment, Map.of(http.getId(), Status.BUNDLE_SENT_SUCCESSFULLY));

                invokeUpdateAuditStatus();

                final Map<String, Map<String, EndpointDetail>> all = APILocator.getPublishAuditAPI()
                        .getPublishAuditStatus(bundle.getId()).getStatusPojo().getEndpointsMap();
                assertEquals(Status.FAILED_TO_PUBLISH.getCode(),
                        all.get(environment.getId()).get(http.getId()).getStatus());
                assertTrue("No synthetic failed-remote-group key may be left: " + all.keySet(),
                        all.keySet().stream().noneMatch(key -> key.startsWith("failed-remote-group")));
            } finally {
                cleanUpAudit(bundle);
                PublisherTestUtil.cleanBundleEndpointEnv(bundle, http, environment);
            }
        }
    }

    /**
     * Method to test: {@code PublisherQueueJob#getPublishersForBundle}
     * Given scenario: an environment with an {@code awss3}, a {@code static} and an {@code http}
     * endpoint, created in that order (#34434, US4).
     * Expected result: the publishers come back in a fixed order, push, S3, static, so the logs of
     * one bundle read the same on every server.
     */
    @Test
    public void test_getPublishersForBundle_returnsAFixedOrder() throws Exception {
        final PublisherTestUtil.MixedEndpointEnvironment mixed =
                PublisherTestUtil.createMixedEndpointEnvironment(
                        Collections.singletonList(PushPublisher.class), false);
        PublishingEndPoint s3 = null;
        try {
            // The data generator's persist() reads the endpoint back as a static one, which
            // fails for an S3 endpoint, so it is saved directly
            s3 = new StaticPublishingEndPointDataGen()
                    .environment(mixed.environment)
                    .protocol(AWSS3Publisher.PROTOCOL_AWS_S3)
                    .next();
            APILocator.getPublisherEndPointAPI().saveEndPoint(s3);

            final Method method = PublisherQueueJob.class
                    .getDeclaredMethod("getPublishersForBundle", String.class);
            method.setAccessible(true);
            final java.util.Collection<?> publishers =
                    (java.util.Collection<?>) method.invoke(new PublisherQueueJob(), mixed.bundle.getId());

            assertEquals(list(PushPublisher.class, AWSS3Publisher.class, StaticPublisher.class),
                    new java.util.ArrayList<>(publishers));
        } finally {
            if (s3 != null) {
                APILocator.getPublisherEndPointAPI().deleteEndPointById(s3.getId());
            }
            mixed.cleanUp();
        }
    }

    private void assertStatusForTwoStaticEndpoints(final Status first, final Status second,
            final Status expected) throws Exception {
        final User user = TestUserUtils.getAdminUser();
        final Environment environment = PublisherTestUtil.createEnvironment(user, true);
        final PublishingEndPoint one = new StaticPublishingEndPointDataGen().environment(environment)
                .protocol(StaticPublisher.PROTOCOL_STATIC).nextPersisted();
        final PublishingEndPoint two = new StaticPublishingEndPointDataGen().environment(environment)
                .protocol(StaticPublisher.PROTOCOL_STATIC).nextPersisted();
        final Bundle bundle = PublisherTestUtil.createBundle("bundle-" + System.nanoTime(), user, environment);
        try {
            seedAudit(bundle, environment, Map.of(one.getId(), first, two.getId(), second));

            invokeUpdateAuditStatus();

            assertEquals(expected, statusOf(bundle.getId()));
        } finally {
            cleanUpAudit(bundle);
            APILocator.getPublisherEndPointAPI().deleteEndPointById(one.getId());
            PublisherTestUtil.cleanBundleEndpointEnv(bundle, two, environment);
        }
    }

    private void assertStatusForOneStaticEndpoint(final Status endpointStatus, final Status expected)
            throws Exception {
        final User user = TestUserUtils.getAdminUser();
        final Environment environment = PublisherTestUtil.createEnvironment(user, true);
        final PublishingEndPoint endpoint = new StaticPublishingEndPointDataGen().environment(environment)
                .protocol(StaticPublisher.PROTOCOL_STATIC).nextPersisted();
        final Bundle bundle = PublisherTestUtil.createBundle("bundle-" + System.nanoTime(), user, environment);
        try {
            seedAudit(bundle, environment, Map.of(endpoint.getId(), endpointStatus));

            invokeUpdateAuditStatus();

            assertEquals("Endpoint status " + endpointStatus, expected, statusOf(bundle.getId()));
        } finally {
            cleanUpAudit(bundle);
            PublisherTestUtil.cleanBundleEndpointEnv(bundle, endpoint, environment);
        }
    }

    /** Inserts an audit row for the bundle, with one entry per endpoint and one earlier attempt. */
    private void seedAudit(final Bundle bundle, final Environment environment,
            final Map<String, Status> endpointStatuses) throws Exception {
        final PublishAuditStatus audit = new PublishAuditStatus(bundle.getId());
        final PublishAuditHistory history = new PublishAuditHistory();
        history.setNumTries(1);
        endpointStatuses.forEach((endpointId, status) -> history.addOrUpdateEndpoint(
                environment.getId(), endpointId, endpointDetail(status, "seeded")));
        audit.setStatusPojo(history);
        APILocator.getPublishAuditAPI().insertPublishAuditStatus(audit);
        APILocator.getPublishAuditAPI().updatePublishAuditStatus(bundle.getId(),
                Status.BUNDLE_SENT_SUCCESSFULLY, history);
    }

    private static EndpointDetail endpointDetail(final Status status, final String info) {
        final EndpointDetail detail = new EndpointDetail();
        detail.setStatus(status.getCode());
        detail.setInfo(info);
        return detail;
    }

    private Map<String, EndpointDetail> endpointsOf(final Bundle bundle, final Environment environment)
            throws Exception {
        return APILocator.getPublishAuditAPI().getPublishAuditStatus(bundle.getId()).getStatusPojo()
                .getEndpointsMap().get(environment.getId());
    }

    private void cleanUpAudit(final Bundle bundle) {
        try {
            APILocator.getPublishAuditAPI().deletePublishAuditStatus(bundle.getId());
        } catch (Exception e) {
            // best-effort cleanup
        }
    }

    /**
     * Local HTTP server that plays the receiver's {@code /api/auditPublishing/getAll}: it answers with
     * the serialized histories it was given. Closing it stops the server.
     */
    private static final class AuditStub implements AutoCloseable {

        private final com.sun.net.httpserver.HttpServer server;

        AuditStub(final List<String> serializedHistories) throws Exception {
            server = com.sun.net.httpserver.HttpServer.create(
                    new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            final byte[] body = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsBytes(serializedHistories);
            server.createContext("/api/auditPublishing/getAll", exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (java.io.OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
        }

        int port() {
            return server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    /**
     * Runs one {@link PublisherQueueJob#execute} cycle with a mocked Quartz context, the way the
     * cron would: fire time now, previous fire time one minute ago, delivery strategy ALL_ENDPOINTS.
     */
    private void runJobOnce() throws Exception {
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put("deliveryStrategy", DeliveryStrategy.ALL_ENDPOINTS);
        final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getFireTime()).thenReturn(new Date());
        Mockito.when(context.getPreviousFireTime()).thenReturn(new Date(System.currentTimeMillis() - 60_000L));
        Mockito.when(context.getMergedJobDataMap()).thenReturn(dataMap);
        new PublisherQueueJob().execute(context);
    }

    private void invokeUpdateAuditStatus() throws Exception {
        final PublisherQueueJob job = new PublisherQueueJob();
        final Method updateAuditStatus =
                PublisherQueueJob.class.getDeclaredMethod("updateAuditStatus", List.class);
        updateAuditStatus.setAccessible(true);
        updateAuditStatus.invoke(job, Collections.<Map<String, Object>>emptyList());
    }
}
