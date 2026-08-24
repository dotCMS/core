package com.dotcms.rest.api.v1.content.bulkrefresh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.impl.BulkRefreshContentletsProcessor;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityBulkRefreshSubmitView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import io.vavr.control.Try;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link BulkRefreshResource} — {@code POST /api/v1/content/_bulkrefresh}.
 * <p>
 * The tests that matter most here are the ones that hold the endpoint to its promise rather than to
 * its shape: that content genuinely absent from the index is findable once the job reports SUCCESS
 * (which only holds if indexing is synchronous), and that every submitted row ends up accounted for in
 * the counters however the run ends.
 */
@EnableWeld
public class BulkRefreshResourceIntegrationTest extends Junit5WeldBaseTest {

    private static User adminUser;
    private static User powerUser;
    private static User plainBackendUser;
    private static Host defaultSite;
    private static Language defaultLanguage;
    private static ContentType contentType;
    private static ContentletIndexAPI indexAPI;
    private static HttpServletResponse response;

    private BulkRefreshResource resource;

    @Inject
    BulkRefreshHelper bulkRefreshHelper;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    /** Static so {@link #stopQueueIfWeStartedIt()} can reach them; only this class writes them. */
    private static boolean queueStartedHere;
    private static JobQueueManagerAPI sharedJobQueueManagerAPI;

    @BeforeAll
    static void setUp() throws Exception {
        IntegrationTestInitService.getInstance().init();

        adminUser = TestUserUtils.getAdminUser();
        defaultSite = APILocator.getHostAPI().findDefaultHost(adminUser, false);
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        contentType = TestDataUtils.getRichTextLikeContentType();
        indexAPI = APILocator.getContentletIndexAPI();
        response = new MockHttpResponse();

        powerUser = TestUserUtils.getUser(getOrCreatePowerUserRole(),
                "bulkrefresh.power@dotcms.com", "Bulk", "Power", "bulkrefreshpower");
        plainBackendUser = TestUserUtils.getBackendUser(defaultSite);
    }

    @BeforeEach
    void prepare() throws Exception {
        resource = new BulkRefreshResource(bulkRefreshHelper);

        // Without this the queue accepts jobs and never runs them: every job stays PENDING and each
        // wait below times out. ContentImportResourceIntegrationTest never noticed because it only
        // asserts job *creation* — 24 tests in under nine seconds — so this suite had no test that
        // actually needed a processor to execute until now.
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
            queueStartedHere = true;
        }
        sharedJobQueueManagerAPI = jobQueueManagerAPI;
    }

    /**
     * Hands the queue back in the state this class found it.
     *
     * Leaving it running would keep it draining jobs for the rest of the JVM, and this class shares a
     * suite with tests that only assert job *creation* — they pass either way, so the difference would
     * surface as confusing behaviour elsewhere rather than as a failure here. Declaration order
     * happening to put this class last is luck, not isolation.
     */
    @AfterAll
    static void stopQueueIfWeStartedIt() {
        if (queueStartedHere && null != sharedJobQueueManagerAPI) {
            Try.run(sharedJobQueueManagerAPI::close)
                    .onFailure(e -> Logger.warn(BulkRefreshResourceIntegrationTest.class,
                            "Unable to stop the job queue after the bulk refresh tests", e));
        }
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: A contentlet is created and then deleted from the index behind dotCMS's back,
     * leaving the database correct and the index wrong — the exact situation this endpoint exists for.
     * A CMS Administrator submits it.
     * <p>
     * Expected result: The job reaches SUCCESS and the contentlet is findable in the index again. This
     * is the test that actually proves indexing is synchronous: under the default DEFER policy the job
     * would report SUCCESS having only written a row to {@code dist_reindex_journal}, and this
     * assertion would fail.
     */
    @Test
    void test_bulkRefresh_makesContentMissingFromTheIndexFindableAgain() throws Exception {
        final Contentlet contentlet = newContentlet();
        indexAPI.removeContentFromIndex(contentlet);
        assertFalse(isInIndex(contentlet.getIdentifier()),
                "Precondition: the contentlet must be absent from the index");

        final String jobId = submit(adminUser, List.of(contentlet.getInode()), false, true);
        final Job job = awaitTerminal(jobId);

        assertEquals(JobState.SUCCESS, job.state());
        assertTrue(isInIndex(contentlet.getIdentifier()),
                "A job reporting SUCCESS must mean the content is actually searchable");
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: Three language versions of one contentlet are submitted by their separate
     * inodes.
     * <p>
     * Expected result: {@code total} is 1 and the single record names all three inodes, while
     * {@code submitted} still reports 3. Reindexing the same identifier three times would be wasted
     * work, but a client that selected three rows needs all three named back so it can settle them.
     */
    @Test
    void test_bulkRefresh_deduplicatesLanguageVersionsButReportsEveryInode() throws Exception {
        final Contentlet english = newContentlet();
        final Language spanish = TestDataUtils.getSpanishLanguage();
        final Contentlet translated = ContentletDataGen.checkout(english);
        translated.setLanguageId(spanish.getId());
        final Contentlet spanishVersion = ContentletDataGen.checkin(translated);

        final List<String> inodes = List.of(english.getInode(), spanishVersion.getInode());
        final BulkRefreshSubmitResponse submitted = submitResponse(adminUser, inodes, false, true);
        assertEquals(2, submitted.submitted(), "submitted is the raw inode count");

        final Job job = awaitTerminal(submitted.jobId());
        final Map<String, Object> metadata = metadata(job);

        assertEquals(1, metadata.get("total"), "Two language rows are one identifier");
        assertEquals(1, metadata.get("successCount"));

        final List<Map<String, Object>> results = itemResults(metadata);
        assertEquals(1, results.size());
        @SuppressWarnings("unchecked")
        final List<String> reported = (List<String>) results.get(0).get("inodes");
        assertTrue(reported.containsAll(inodes),
                "Every submitted inode must be named so the client can mark its rows");
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: A batch containing one real inode and one that never existed.
     * <p>
     * Expected result: The job still reaches SUCCESS, with one success and one failure. A selection can
     * go stale between the click and the submit, and losing the rest of the batch to one dead row would
     * make the endpoint unusable on a busy site.
     */
    @Test
    void test_bulkRefresh_mixedBatchSucceedsAndReportsTheFailure() throws Exception {
        final Contentlet contentlet = newContentlet();
        final String ghostInode = UUIDGenerator.generateUuid();

        final Job job = awaitTerminal(
                submit(adminUser, List.of(contentlet.getInode(), ghostInode), false, true));
        final Map<String, Object> metadata = metadata(job);

        assertEquals(JobState.SUCCESS, job.state(), "One bad row must not fail the job");
        assertEquals(1, metadata.get("successCount"));
        assertEquals(1, metadata.get("failedCount"));

        final Map<String, Object> failure = itemResults(metadata).stream()
                .filter(r -> "FAILED".equals(String.valueOf(r.get("status"))))
                .findFirst().orElseThrow();
        assertTrue(String.valueOf(failure.get("errorMessage")).contains(ghostInode),
                "The failure must name the inode that could not be resolved");
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: However a run ends, the counters are read back.
     * <p>
     * Expected result: {@code success + failed + skipped == total}. Any client rendering per-row state
     * relies on this to know it can stop waiting; a run whose counters do not close leaves rows
     * spinning forever.
     */
    @Test
    void test_bulkRefresh_countersAlwaysSumToTotal() throws Exception {
        final Contentlet first = newContentlet();
        final Contentlet second = newContentlet();

        final Job job = awaitTerminal(submit(adminUser,
                List.of(first.getInode(), second.getInode(), UUIDGenerator.generateUuid()),
                false, true));
        final Map<String, Object> metadata = metadata(job);

        final int total = (int) metadata.get("total");
        final int sum = (int) metadata.get("successCount")
                + (int) metadata.get("failedCount")
                + (int) metadata.get("skippedCount");
        assertEquals(total, sum, "Every item must be accounted for exactly once");
        assertEquals(total, itemResults(metadata).size(), "One record per item when recorded");
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: The same selection is submitted once with {@code includeItemResults} false and
     * once with it true.
     * <p>
     * Expected result: Counters both times; the per-item array only when asked for. It is persisted
     * with the job, so recording a 500-entry array nobody requested is storage spent on nothing.
     */
    @Test
    void test_bulkRefresh_itemResultsAreOptIn() throws Exception {
        final Contentlet contentlet = newContentlet();

        final Map<String, Object> without =
                metadata(awaitTerminal(submit(adminUser, List.of(contentlet.getInode()), false, false)));
        assertEquals(1, without.get("total"), "Counters are always reported");
        assertFalse(without.containsKey("results"),
                "The per-item array must be absent when it was not requested");

        final Map<String, Object> with =
                metadata(awaitTerminal(submit(adminUser, List.of(contentlet.getInode()), false, true)));
        assertEquals(1, itemResults(with).size());
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: A plain backend user with neither the CMS Power User nor the CMS Administrator
     * role submits a selection.
     * <p>
     * Expected result: Rejected. The legacy Refresh button was gated the same way in
     * {@code view_contentlets.jsp}; reindexing is expensive, and the client hiding the action is not
     * authorization.
     */
    @Test
    void test_bulkRefresh_plainBackendUserIsForbidden() throws Exception {
        final Contentlet contentlet = newContentlet();

        assertThrows(DotSecurityException.class, () -> resource.bulkRefresh(
                requestFor(plainBackendUser), response,
                new BulkRefreshForm(List.of(contentlet.getInode()), false, false)));
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: A CMS Power User submits a selection.
     * <p>
     * Expected result: Accepted. Power Users could press the legacy button, so none of them may lose
     * the capability in the move to the Action Center.
     */
    @Test
    void test_bulkRefresh_powerUserIsAllowed() throws Exception {
        final Contentlet contentlet = newContentlet();

        final Job job = awaitTerminal(submit(powerUser, List.of(contentlet.getInode()), false, false));
        assertEquals(JobState.SUCCESS, job.state());
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: More inodes than the configured cap are submitted.
     * <p>
     * Expected result: Rejected outright. Synchronous indexing over an unbounded selection is a
     * self-inflicted full reindex, which is the one thing this endpoint must never become.
     */
    @Test
    void test_bulkRefresh_rejectsSelectionsOverTheCap() throws Exception {
        final int cap = Config.getIntProperty(BulkRefreshHelper.MAX_ITEMS_CONFIG_PROPERTY,
                BulkRefreshHelper.MAX_ITEMS_DEFAULT);
        final List<String> tooMany = new ArrayList<>();
        for (int i = 0; i <= cap; i++) {
            tooMany.add(UUIDGenerator.generateUuid());
        }

        assertThrows(IllegalArgumentException.class, () -> resource.bulkRefresh(
                requestFor(adminUser), response, new BulkRefreshForm(tooMany, false, false)));
    }

    /**
     * Method to test: {@link com.dotcms.rest.api.v1.content.bulkrefresh.BulkRefreshCompletionListener}
     * <p>
     * Given scenario: A reindex is submitted and allowed to finish.
     * <p>
     * Expected result: The submitter gains a notification, which is only possible if the completion
     * listener was actually registered at startup and fired for this job.
     * <p>
     * This is the assertion the suite was missing. Completion is reported by push now, so every other
     * test here can pass while the reporting path is entirely dead — and it was: the listener began life
     * as a CDI bean nothing injected, so it was never constructed, never subscribed, and never told
     * anyone anything, with all 12 tests green.
     */
    @Test
    void test_bulkRefresh_completionNotifiesTheSubmitter() throws Exception {
        final Contentlet contentlet = newContentlet();
        final Long before = APILocator.getNotificationAPI()
                .getNotificationsCount(adminUser.getUserId());

        awaitTerminal(submit(adminUser, List.of(contentlet.getInode()), false, false));

        // The notification is raised off the job-completed event, so it can land slightly after the job
        // itself reaches a terminal state.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> APILocator.getNotificationAPI()
                        .getNotificationsCount(adminUser.getUserId()) > before);
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: A submission is accepted.
     * <p>
     * Expected result: HTTP 202 with the job id. 202 rather than 200 because the work is accepted and
     * not yet done — that distinction is the whole reason this endpoint is job-backed, and a client
     * must not be able to read the response as "reindexed". Completion arrives by push, not by the
     * client asking.
     */
    @Test
    void test_bulkRefresh_respondsAcceptedWithAJobHandle() throws Exception {
        final Contentlet contentlet = newContentlet();

        final Response httpResponse = resource.bulkRefresh(requestFor(adminUser), response,
                new BulkRefreshForm(List.of(contentlet.getInode()), false, false));

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), httpResponse.getStatus());

        final BulkRefreshSubmitResponse entity =
                ((ResponseEntityBulkRefreshSubmitView) httpResponse.getEntity()).getEntity();
        assertNotNull(entity.jobId());
        assertEquals(1, entity.submitted(), "submitted is the raw inode count");
    }

    /**
     * Method to test: {@link BulkRefreshResource#bulkRefresh}
     * <p>
     * Given scenario: The request arrives with no body at all, which Jersey hands over as a null form.
     * <p>
     * Expected result: Rejected as a bad request. Bean validation never runs on a null form, so without
     * an explicit check the first dereference NPEs and the endpoint answers 500 for what is plainly a
     * malformed request — contradicting the 400 its own {@code @ApiResponse} set documents.
     */
    @Test
    void test_bulkRefresh_nullBodyIsRejectedAsBadRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> resource.bulkRefresh(requestFor(adminUser), response, null));
    }

    /**
     * Method to test: {@link BulkRefreshContentletsProcessor#cancel}
     * <p>
     * Given scenario: A larger selection is submitted and cancellation is requested straight away.
     * There is no reindex-specific cancel endpoint — the UI never called one — so this goes through
     * the generic job queue, which is the only way a run gets cancelled in production.
     * <p>
     * Expected result: However the race lands — cancellation reaching the run mid-flight, or the run
     * finishing first — the counters still close over {@code total}, and if the job did end up
     * CANCELED then some items are reported skipped rather than left pending. The assertion is
     * deliberately about the invariant rather than about winning the race, because a test that depends
     * on timing here would be flaky rather than informative.
     */
    @Test
    void test_cancelledRun_leavesNoItemPending() throws Exception {
        final List<String> inodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            inodes.add(newContentlet().getInode());
        }

        final String jobId = submit(adminUser, inodes, false, true);
        try {
            jobQueueManagerAPI.cancelJob(jobId);
        } catch (final IllegalStateException | DoesNotExistException e) {
            // The run may already be terminal by the time cancel lands; that is one of the two
            // legitimate outcomes and the invariant below covers both. Narrowed deliberately: catching
            // everything would let a genuine NullPointerException from cancelJob pass as "the race
            // landed the other way".
            Logger.info(BulkRefreshResourceIntegrationTest.class,
                    "Cancel arrived after the run finished: " + e.getMessage());
        }

        final Job job = awaitTerminal(jobId);
        final Map<String, Object> metadata = metadata(job);
        final int total = (int) metadata.get("total");
        assertEquals(total, (int) metadata.get("successCount")
                        + (int) metadata.get("failedCount")
                        + (int) metadata.get("skippedCount"),
                "A cancelled run must still account for every item");
        assertEquals(total, itemResults(metadata).size(),
                "Skipped items are recorded, not omitted");

        if (JobState.CANCELED == job.state()) {
            assertTrue((int) metadata.get("skippedCount") > 0,
                    "A cancelled run must report what it did not attempt");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * The CMS Power User role, created if this database does not have it.
     *
     * {@code loadRoleByKey} answers null for a missing role rather than throwing, and
     * {@code doesUserHaveRole(user, null)} is then silently false — so passing the lookup straight
     * through produced a "power user" with no such role and a permission check that could only ever
     * fail. Mirrors {@code TestUserUtils.getOrCreateAdminRole}.
     */
    private static Role getOrCreatePowerUserRole() throws DotDataException {
        final Role existing = APILocator.getRoleAPI().loadRoleByKey(Role.CMS_POWER_USER);

        if (null != existing) {
            return existing;
        }

        // Not a dead branch: this database really does lack the role. Before this helper existed the
        // power-user test failed with "must be a CMS Power User or a CMS Administrator" precisely
        // because loadRoleByKey answered null here, so failing loudly instead of creating it would
        // just reinstate that failure. Logged rather than silent, because persisting a role into a
        // shared test database is worth seeing in the output.
        Logger.warn(BulkRefreshResourceIntegrationTest.class, String.format(
                "Role [%s] not present; creating it for the bulk refresh permission tests",
                Role.CMS_POWER_USER));

        return new RoleDataGen().key(Role.CMS_POWER_USER).nextPersisted();
    }

    private Contentlet newContentlet() {
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .languageId(defaultLanguage.getId())
                .host(defaultSite)
                .nextPersisted();
        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> isInIndex(contentlet.getIdentifier()));
        return contentlet;
    }

    private static boolean isInIndex(final String identifier) throws Exception {
        CacheLocator.getESQueryCache().clearCache();
        return !APILocator.getContentletAPI()
                .searchIndex("+identifier:" + identifier, 10, 0, "moddate", adminUser, false)
                .isEmpty();
    }

    private static HttpServletRequest requestFor(final User user) {
        return JobUtil.generateMockRequest(user, defaultSite.getHostname());
    }

    private String submit(final User user, final List<String> inodes,
            final boolean includeDependencies, final boolean includeItemResults) throws Exception {
        return submitResponse(user, inodes, includeDependencies, includeItemResults).jobId();
    }

    private BulkRefreshSubmitResponse submitResponse(final User user, final List<String> inodes,
            final boolean includeDependencies, final boolean includeItemResults) throws Exception {
        final Response httpResponse = resource.bulkRefresh(requestFor(user), response,
                new BulkRefreshForm(inodes, includeDependencies, includeItemResults));
        return ((ResponseEntityBulkRefreshSubmitView) httpResponse.getEntity()).getEntity();
    }

    private Job awaitTerminal(final String jobId) {
        return Awaitility.await().atMost(120, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId), job -> isTerminal(job.state()));
    }

    private static boolean isTerminal(final JobState state) {
        return state == JobState.SUCCESS || state == JobState.CANCELED
                || state == JobState.FAILED_PERMANENTLY
                || state == JobState.ABANDONED_PERMANENTLY;
    }

    private static Map<String, Object> metadata(final Job job) {
        return job.result()
                .orElseThrow(() -> new AssertionError("A terminal job must carry its result"))
                .metadata()
                .orElseThrow(() -> new AssertionError("A terminal job must carry its metadata"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> itemResults(final Map<String, Object> metadata) {
        final Object results = metadata.get("results");
        assertNotNull(results, "The per-item array was requested and must be present");
        return ((List<Object>) results).stream()
                .map(BulkRefreshResourceIntegrationTest::asMap)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(final Object item) {
        if (item instanceof Map) {
            return (Map<String, Object>) item;
        }
        return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(item, Map.class);
    }
}
