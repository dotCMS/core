package com.dotcms.rest.api.v1.job;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobListView;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the JobQueueHelper class
 * Helper add functionality to consume JobQueueManagerAPI
 * Here we test those functionalities, methods that simply call the JobQueueManagerAPI are not tested
 */
@EnableWeld
public class JobQueueHelperIntegrationTest  extends com.dotcms.Junit5WeldBaseTest {

    @Inject
    JobQueueHelper jobQueueHelper;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    private static final String TEST_QUEUE_PREFIX = "ownedJobs";

    /**
     * Initializes the integration environment, so the class also runs on its own and not only
     * after another class of its suite did it.
     *
     * @throws Exception if the environment cannot be initialized
     */
    @BeforeAll
    static void setUp() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Deletes the jobs this class created on the queues {@link #newQueue()} registers.
     *
     * @throws DotDataException if the jobs cannot be deleted
     */
    @AfterAll
    static void cleanUp() throws DotDataException {
        final String jobsOfTestQueues = "SELECT id FROM job WHERE queue_name LIKE ?";
        new DotConnect().setSQL("DELETE FROM job_history WHERE job_id IN (" + jobsOfTestQueues + ")")
                .addParam(TEST_QUEUE_PREFIX + "%").loadResult();
        new DotConnect().setSQL("DELETE FROM job_queue WHERE id IN (" + jobsOfTestQueues + ")")
                .addParam(TEST_QUEUE_PREFIX + "%").loadResult();
        new DotConnect().setSQL("DELETE FROM job WHERE queue_name LIKE ?")
                .addParam(TEST_QUEUE_PREFIX + "%").loadResult();
    }

    /**
     * Test with no parameters in the JobParams creating a job
     * Given scenario: create a job with no parameters and valid queue name
     * Expected result: the job is created
     *
     * @throws DotDataException if there's an error creating the job
     */
    @Test
    void testEmptyParams() throws DotDataException, JsonProcessingException {

        jobQueueManagerAPI.registerProcessor("demoQueue", DemoJobProcessor.class);

        final var jobParams = new JobParams();
        final var user = mock(User.class);
        final var request = mock(HttpServletRequest.class);

        when(user.getUserId()).thenReturn("dotcms.org.1");

        final String jobId = jobQueueHelper.createJob(
                "demoQueue", jobParams, user, request
        );

        Assertions.assertNotNull(jobId);
        final Job job = jobQueueHelper.getJob(jobId, user);
        Assertions.assertNotNull(job);
        Assertions.assertEquals(jobId, job.id());
    }

    /**
     * Test with null parameters creating a job
     * Given scenario: create a job with null parameters and valid queue name
     * Expected result: the job is created
     *
     * @throws DotDataException if there's an error creating the job
     */
    @Test
    void testCreateJobWithNoParameters() throws DotDataException {

        jobQueueManagerAPI.registerProcessor("demoQueue", DemoJobProcessor.class);

        final var user = mock(User.class);
        when(user.getUserId()).thenReturn("dotcms.org.1");

        final String jobId = jobQueueHelper.createJob(
                "demoQueue", (Map<String, Object>) null, user, mock(HttpServletRequest.class)
        );

        Assertions.assertNotNull(jobId);
        final Job job = jobQueueHelper.getJob(jobId, user);
        Assertions.assertNotNull(job);
        Assertions.assertEquals(jobId, job.id());
    }

    @Test
    void testWithValidParamsButInvalidQueueName(){
        final JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");

        final var user = mock(User.class);
        when(user.getUserId()).thenReturn("dotcms.org.1");

        assertThrows(DoesNotExistException.class, () -> {
            jobQueueHelper.createJob(
                    "nonExisting", jobParams, user, mock(HttpServletRequest.class)
            );
        });
    }

    public static class DemoJobProcessor implements JobProcessor {

        public DemoJobProcessor() {
            // Do nothing
        }

        @Override
        public void process(Job job) {
            // Do nothing
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return Map.of();
        }
    }

    /**
     * A processor that does nothing and accepts cancellation.
     */
    public static class CancellableDemoJobProcessor extends DemoJobProcessor implements Cancellable {

        @Override
        public void cancel(final Job job) {
            // Do nothing
        }
    }

    /**
     * Test with valid parameters and queue name
     * Given scenario: create a job with valid parameters and queue name
     * Expected result: the job is created and the queue name is added to the list of queue names
     * @throws DotDataException
     * @throws JsonProcessingException
     */
    @Test
    void testWithValidParamsAndQueueName() throws DotDataException, JsonProcessingException {
        jobQueueManagerAPI.registerProcessor("demoQueue", DemoJobProcessor.class);

        final JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");

        final var user = mock(User.class);
        when(user.getUserId()).thenReturn("dotcms.org.1");

        final String jobId = jobQueueHelper.createJob(
                "demoQueue", jobParams, user, mock(HttpServletRequest.class)
        );

        Assertions.assertNotNull(jobId);
        final Job job = jobQueueHelper.getJob(jobId, user);
        Assertions.assertNotNull(job);
        Assertions.assertEquals(jobId, job.id());
        Assertions.assertTrue(jobQueueHelper.getQueueNames().contains("demoQueue"));
    }

    /**
     * Given scenario: create a job with valid parameters and queue name
     * Expected result: the job is created and it is watchable
     * @throws DotDataException
     * @throws JsonProcessingException
     */
    @Test
    void testIsWatchable() throws DotDataException, JsonProcessingException {
        jobQueueManagerAPI.registerProcessor("testQueue", DemoJobProcessor.class);
        final JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");

        final var user = mock(User.class);
        when(user.getUserId()).thenReturn("dotcms.org.1");

        final String jobId = jobQueueHelper.createJob(
                "testQueue", jobParams, user, mock(HttpServletRequest.class)
        );
        Assertions.assertNotNull(jobId);
        final Job job = jobQueueHelper.getJob(jobId, user);
        assertFalse(jobQueueHelper.isNotWatchable(job));
    }


    /**
     * Given scenario: create a job with valid parameters and queue name
     * Expected result: the job is created and the status info is returned
     * @throws DotDataException
     * @throws JsonProcessingException
     */
    @Test
    void testGetStatusInfo() throws DotDataException, JsonProcessingException {
        jobQueueManagerAPI.registerProcessor("testQueue", DemoJobProcessor.class);
        final JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");

        final var user = mock(User.class);
        when(user.getUserId()).thenReturn("dotcms.org.1");

        final String jobId = jobQueueHelper.createJob(
                "testQueue", jobParams, user, mock(HttpServletRequest.class)
        );
        Assertions.assertNotNull(jobId);
        final Job job = jobQueueHelper.getJob(jobId, user);
        final Map<String, Object> info = jobQueueHelper.getJobStatusInfo(job);
        Assertions.assertTrue(info.containsKey("state"));
        Assertions.assertTrue(info.containsKey("progress"));
        Assertions.assertTrue(info.containsKey("startedAt"));
        Assertions.assertTrue(info.containsKey("finishedAt"));
    }

    /**
     * Given scenario: call cancel Job with an invalid job id
     * Expected result: we should get a DoesNotExistException
     */
   @Test
    void testCancelNonExistingJob(){
        assertThrows(DoesNotExistException.class, () -> {
            jobQueueHelper.cancelJob("nonExisting", user("dotcms.org.1", false));
        });
    }

    /**
     * Mock the request
     * @return a mocked HttpServletRequest
     */
    HttpServletRequest mockRequest(){
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("param1")).thenReturn("value1");
        when(request.getParameter("param2")).thenReturn("value2");
        when(request.getParameter("param3")).thenReturn("value3");
        when(request.getParameterNames()).thenReturn(java.util.Collections.enumeration(java.util.Arrays.asList("param1", "param2", "param3")));
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("mockSessionId123");
        when(request.getSession()).thenReturn(session);

        User user = mock(User.class);
        when(user.getUserId()).thenReturn("mockUserId123");
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        return request;
    }

    FormDataContentDisposition mockFormDataContentDisposition(){
        FormDataContentDisposition formDataContentDisposition = mock(FormDataContentDisposition.class);
        when(formDataContentDisposition.getFileName()).thenReturn("test.txt");
        return formDataContentDisposition;
    }

    /**
     * Test file upload
     * Given scenario: upload a file
     * Expected result: the file is uploaded and the request fingerprint and temp file id are added to the map
     * @throws IOException
     */
    @Test
    void TestFileUpload() throws IOException {

        final File tempFile = File.createTempFile("test", "test");
        JobParams jobParams = new JobParams();
        jobParams.setJsonParams("{}");
        jobParams.setParams(Map.of());
        jobParams.setFileInputStream(new FileInputStream(tempFile));
        jobParams.setContentDisposition(mockFormDataContentDisposition());

        final Map <String,Object>map = new HashMap<>();
        jobQueueHelper.handleUploadIfPresent(jobParams,map,mockRequest());
        Assertions.assertTrue(map.containsKey("requestFingerPrint"));
        Assertions.assertTrue(map.containsKey("tempFileId"));
    }

    /**
     * Builds a mocked {@link User} with the given id.
     *
     * @param userId the user's id
     * @param admin  whether the user is a CMS Administrator
     * @return the mocked user
     */
    private static User user(final String userId, final boolean admin) {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(user.isAdmin()).thenReturn(admin);
        return user;
    }

    /**
     * Registers a queue with a unique name, so listings in a test only see that test's jobs. Its
     * jobs do nothing and accept cancellation.
     *
     * @return the queue name
     */
    private String newQueue() {
        final String queueName = TEST_QUEUE_PREFIX + UUIDGenerator.shorty();
        jobQueueManagerAPI.registerProcessor(queueName, CancellableDemoJobProcessor.class);
        return queueName;
    }

    /**
     * Creates a job on the given queue as the given user.
     *
     * @param queueName the queue
     * @param owner     the user creating the job
     * @return the job id
     */
    private String createJobAs(final String queueName, final User owner) throws DotDataException {
        return jobQueueHelper.createJob(queueName, new HashMap<String, Object>(), owner,
                mock(HttpServletRequest.class));
    }

    /**
     * Given scenario: a back-end user creates a job and reads it back
     * Expected result: the job is returned
     */
    @Test
    void getJob_returnsJobToItsCreator() throws DotDataException {
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final String jobId = createJobAs(newQueue(), owner);

        final Job job = jobQueueHelper.getJob(jobId, owner);

        Assertions.assertEquals(jobId, job.id());
    }

    /**
     * Given scenario: a back-end user reads a job another back-end user created
     * Expected result: the job is reported as not found
     */
    @Test
    void getJob_reportsNotFoundToAnotherUser() throws DotDataException {
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final User other = user("other-" + UUIDGenerator.shorty(), false);
        final String jobId = createJobAs(newQueue(), owner);

        assertThrows(DoesNotExistException.class, () -> jobQueueHelper.getJob(jobId, other));
    }

    /**
     * Given scenario: a CMS Administrator reads a job a back-end user created
     * Expected result: the job is returned
     */
    @Test
    void getJob_returnsAnyJobToAdministrator() throws DotDataException {
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final User admin = user("admin-" + UUIDGenerator.shorty(), true);
        final String jobId = createJobAs(newQueue(), owner);

        Assertions.assertEquals(jobId, jobQueueHelper.getJob(jobId, admin).id());
    }

    /**
     * Given scenario: a back-end user cancels a job another back-end user created
     * Expected result: the job is reported as not found and no cancellation is requested
     */
    @Test
    void cancelJob_reportsNotFoundToAnotherUserAndLeavesJobAsIs() throws DotDataException {
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final User other = user("other-" + UUIDGenerator.shorty(), false);
        final String jobId = createJobAs(newQueue(), owner);

        assertThrows(DoesNotExistException.class, () -> jobQueueHelper.cancelJob(jobId, other));

        final JobState state = jobQueueManagerAPI.getJob(jobId).state();
        assertFalse(state == JobState.CANCEL_REQUESTED || state == JobState.CANCELLING
                || state == JobState.CANCELED, "unexpected state " + state);
    }

    /**
     * Given scenario: a back-end user cancels a job they created
     * Expected result: the request is accepted and the job's cancellation is under way
     */
    @Test
    void cancelJob_acceptsRequestFromCreator() throws DotDataException {
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final String jobId = createJobAs(newQueue(), owner);

        Assertions.assertDoesNotThrow(() -> jobQueueHelper.cancelJob(jobId, owner));

        final JobState state = jobQueueManagerAPI.getJob(jobId).state();
        Assertions.assertTrue(state == JobState.CANCEL_REQUESTED || state == JobState.CANCELLING
                || state == JobState.CANCELED, "unexpected state " + state);
    }

    /**
     * Given scenario: an administrator cancels a job a back-end user created
     * Expected result: the request is accepted and the job's cancellation is under way
     */
    @Test
    void cancelJob_acceptsRequestFromAdministrator() throws DotDataException {
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final User admin = user("admin-" + UUIDGenerator.shorty(), true);
        final String jobId = createJobAs(newQueue(), owner);

        Assertions.assertDoesNotThrow(() -> jobQueueHelper.cancelJob(jobId, admin));

        final JobState state = jobQueueManagerAPI.getJob(jobId).state();
        Assertions.assertTrue(state == JobState.CANCEL_REQUESTED || state == JobState.CANCELLING
                || state == JobState.CANCELED, "unexpected state " + state);
    }

    /**
     * Given scenario: a job is created without a recorded creator, as internal jobs may be, and is
     * read and listed by a back-end user and by an administrator
     * Expected result: the back-end user gets "not found" and an empty listing; the administrator
     * reads and lists it
     */
    @Test
    void jobWithoutCreator_isAvailableToAdministratorsOnly() throws DotDataException {
        final String queueName = newQueue();
        final String jobId = jobQueueManagerAPI.createJob(queueName, Map.of("key", "value"));
        final User backendUser = user("other-" + UUIDGenerator.shorty(), false);
        final User admin = user("admin-" + UUIDGenerator.shorty(), true);

        assertThrows(DoesNotExistException.class, () -> jobQueueHelper.getJob(jobId, backendUser));
        Assertions.assertEquals(0,
                jobQueueHelper.getJobs(JobListView.ALL, queueName, backendUser, 1, 50).total());

        Assertions.assertEquals(jobId, jobQueueHelper.getJob(jobId, admin).id());
        Assertions.assertEquals(Set.of(jobId),
                ids(jobQueueHelper.getJobs(JobListView.ALL, queueName, admin, 1, 50)));
    }

    /**
     * Given scenario: a job is requested with a userId parameter naming another user
     * Expected result: the job records the requesting user as its creator, so the named user
     * cannot read it and the requesting user can
     */
    @Test
    void createJob_recordsTheRequestingUserAsCreator() throws DotDataException {
        final String queueName = newQueue();
        final User requester = user("owner-" + UUIDGenerator.shorty(), false);
        final User named = user("other-" + UUIDGenerator.shorty(), false);

        final String jobId = jobQueueHelper.createJob(queueName,
                Map.<String, Object>of("userId", named.getUserId()), requester,
                mock(HttpServletRequest.class));

        Assertions.assertEquals(requester.getUserId(),
                jobQueueManagerAPI.getJob(jobId).parameters().get("userId"));
        Assertions.assertEquals(jobId, jobQueueHelper.getJob(jobId, requester).id());
        assertThrows(DoesNotExistException.class, () -> jobQueueHelper.getJob(jobId, named));
    }

    /**
     * Given scenario: the job to monitor is looked up for its creator and for another back-end user
     * Expected result: the creator gets the job, the other user gets none
     */
    @Test
    void getJobForSSE_returnsJobOnlyToReadersOfIt() throws DotDataException {
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final User other = user("other-" + UUIDGenerator.shorty(), false);
        final String jobId = createJobAs(newQueue(), owner);

        Assertions.assertNotNull(jobQueueHelper.getJobForSSE(jobId, owner));
        Assertions.assertNull(jobQueueHelper.getJobForSSE(jobId, other));
    }

    /**
     * Given scenario: two back-end users each create jobs on the same queue and list them
     * Expected result: each user's listing, per queue and across queues, holds only their own
     * jobs, and the total counts only those
     */
    @Test
    void getJobs_listsOnlyTheUsersOwnJobs() throws DotDataException {
        final String queueName = newQueue();
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final User other = user("other-" + UUIDGenerator.shorty(), false);
        final String ownerJob = createJobAs(queueName, owner);
        final String otherJob1 = createJobAs(queueName, other);
        final String otherJob2 = createJobAs(queueName, other);

        final JobPaginatedResult inQueue =
                jobQueueHelper.getJobs(JobListView.ALL, queueName, other, 1, 50);
        Assertions.assertEquals(Set.of(otherJob1, otherJob2), ids(inQueue));
        Assertions.assertEquals(2, inQueue.total());

        final JobPaginatedResult everywhere =
                jobQueueHelper.getJobs(JobListView.ALL, null, owner, 1, 50);
        Assertions.assertEquals(Set.of(ownerJob), ids(everywhere));
        Assertions.assertEquals(1, everywhere.total());
    }

    /**
     * Given scenario: two back-end users create jobs on the same queue and an administrator lists
     * the queue
     * Expected result: the listing holds every job
     */
    @Test
    void getJobs_listsEveryJobForAdministrator() throws DotDataException {
        final String queueName = newQueue();
        final String ownerJob = createJobAs(queueName, user("owner-" + UUIDGenerator.shorty(), false));
        final String otherJob = createJobAs(queueName, user("other-" + UUIDGenerator.shorty(), false));
        final User admin = user("admin-" + UUIDGenerator.shorty(), true);

        final JobPaginatedResult result =
                jobQueueHelper.getJobs(JobListView.ALL, queueName, admin, 1, 50);

        Assertions.assertEquals(Set.of(ownerJob, otherJob), ids(result));
        Assertions.assertEquals(2, result.total());
    }

    /**
     * Given scenario: a job is created with an uploaded file, then read and listed
     * Expected result: the job keeps the staged file's id and request fingerprint for its
     * processor, and neither value is in what the helper returns
     */
    @Test
    void getJob_omitsStagedFileReferences() throws IOException, DotDataException {
        final String queueName = newQueue();
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final File tempFile = File.createTempFile("test", "test");
        final String jobId;
        try (FileInputStream content = new FileInputStream(tempFile)) {
            final JobParams jobParams = new JobParams();
            jobParams.setJsonParams("{}");
            jobParams.setFileInputStream(content);
            jobParams.setContentDisposition(mockFormDataContentDisposition());
            jobId = jobQueueHelper.createJob(queueName, jobParams, owner, mockRequest());
        } finally {
            tempFile.delete();
        }

        final Map<String, Object> stored = jobQueueManagerAPI.getJob(jobId).parameters();
        Assertions.assertTrue(stored.containsKey("tempFileId"));
        Assertions.assertTrue(stored.containsKey("requestFingerPrint"));

        final Map<String, Object> returned = jobQueueHelper.getJob(jobId, owner).parameters();
        Assertions.assertFalse(returned.containsKey("tempFileId"));
        Assertions.assertFalse(returned.containsKey("requestFingerPrint"));

        final Job listed = jobQueueHelper.getJobs(JobListView.ALL, queueName, owner, 1, 50)
                .jobs().get(0);
        Assertions.assertFalse(listed.parameters().containsKey("tempFileId"));
        Assertions.assertFalse(listed.parameters().containsKey("requestFingerPrint"));
    }

    /**
     * Given scenario: a job is requested with a staged file id or request fingerprint among its
     * parameters
     * Expected result: the request is rejected and no job is created
     */
    @Test
    void createJob_rejectsStagedFileReferencesInParameters() throws DotDataException {
        final String queueName = newQueue();
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);

        for (final String key : List.of("tempFileId", "requestFingerPrint", "requestFingerprint")) {
            final JobParams jobParams = new JobParams();
            jobParams.setJsonParams("{\"" + key + "\":\"value\"}");
            Assertions.assertThrows(BadRequestException.class, () -> jobQueueHelper.createJob(
                    queueName, jobParams, owner, mock(HttpServletRequest.class)), key);
            Assertions.assertThrows(BadRequestException.class, () -> jobQueueHelper.createJob(
                    queueName, Map.<String, Object>of(key, "value"), owner,
                    mock(HttpServletRequest.class)), key);
        }

        Assertions.assertEquals(0, jobQueueHelper.getJobs(JobListView.ALL, queueName, owner, 1, 50)
                .total());
    }

    /**
     * Given scenario: a job carries the parameters a bulk upload sets for its files, and is read
     * and listed
     * Expected result: the job keeps them for its processor; what the helper returns has neither
     * the request fingerprint nor any file's temp id, and still lists every file
     */
    @Test
    void getJob_omitsBulkUploadFileReferences() throws DotDataException {
        final String queueName = newQueue();
        final User owner = user("owner-" + UUIDGenerator.shorty(), false);
        final String jobId = jobQueueManagerAPI.createJob(queueName, Map.of(
                "userId", owner.getUserId(),
                "requestFingerprint", "fingerprint",
                "stagedFiles", List.of(
                        Map.of("fileName", "a.txt", "tempFileId", "temp_a", "sizeBytes", 1),
                        Map.of("fileName", "b.txt", "refusedReason", "TOO_LARGE"))));

        final Map<String, Object> stored = jobQueueManagerAPI.getJob(jobId).parameters();
        Assertions.assertEquals("fingerprint", stored.get("requestFingerprint"));
        Assertions.assertEquals("temp_a",
                ((Map<?, ?>) ((List<?>) stored.get("stagedFiles")).get(0)).get("tempFileId"));

        final Job listed = jobQueueHelper.getJobs(JobListView.ALL, queueName, owner, 1, 50)
                .jobs().get(0);
        for (final Job returned : List.of(jobQueueHelper.getJob(jobId, owner), listed)) {
            Assertions.assertFalse(returned.parameters().containsKey("requestFingerprint"));
            final List<?> files = (List<?>) returned.parameters().get("stagedFiles");
            Assertions.assertEquals(2, files.size());
            Assertions.assertEquals("a.txt", ((Map<?, ?>) files.get(0)).get("fileName"));
            Assertions.assertFalse(((Map<?, ?>) files.get(0)).containsKey("tempFileId"));
        }
    }

    /**
     * Given scenario: a job is read, cancelled and listed with no user
     * Expected result: the job is reported as not found and the listing is empty
     */
    @Test
    void jobAccess_withNoUser_findsNothing() throws DotDataException {
        final String queueName = newQueue();
        final String jobId = createJobAs(queueName, user("owner-" + UUIDGenerator.shorty(), false));

        assertThrows(DoesNotExistException.class, () -> jobQueueManagerAPI.getJob(jobId, null));
        assertThrows(DoesNotExistException.class, () -> jobQueueManagerAPI.cancelJob(jobId, null));
        Assertions.assertEquals(0,
                jobQueueManagerAPI.getJobs(JobListView.ALL, queueName, null, 1, 50).total());
    }

    /**
     * Given scenario: a back-end user's job is requested through every job endpoint by another
     * back-end user and by an administrator
     * Expected result: the other user gets "not found" on read and cancel, does not see the job
     * in any listing and is the user the monitor is opened for; the administrator reads the job
     * and sees it in the listings
     */
    @Test
    void jobQueueResource_answersOnlyForTheCreatorOrAdministrator() throws Exception {
        final String queueName = newQueue();
        final String jobId = createJobAs(queueName, user("owner-" + UUIDGenerator.shorty(), false));
        final User admin = TestUserUtils.getAdminUser();
        final User backendUser = new UserDataGen().roles(TestUserUtils.getBackendRole())
                .nextPersisted();
        final SSEMonitorUtil monitor = mock(SSEMonitorUtil.class);
        final JobQueueResource resource = new JobQueueResource(jobQueueHelper, monitor);
        final HttpServletResponse response = new MockHttpResponse();
        try {
            final String siteName = APILocator.getHostAPI().findDefaultHost(admin, false)
                    .getHostname();
            final HttpServletRequest adminRequest = JobUtil.generateMockRequest(admin, siteName);
            final HttpServletRequest backendRequest =
                    JobUtil.generateMockRequest(backendUser, siteName);

            Assertions.assertEquals(jobId,
                    resource.getJobStatus(adminRequest, response, jobId).getEntity().id());
            Assertions.assertTrue(ids(resource.activeJobs(adminRequest, response, queueName, 1, 50)
                    .getEntity()).contains(jobId));
            Assertions.assertTrue(ids(resource.listJobs(adminRequest, response, 1, 50)
                    .getEntity()).contains(jobId));

            assertThrows(DoesNotExistException.class,
                    () -> resource.getJobStatus(backendRequest, response, jobId));
            assertThrows(DoesNotExistException.class,
                    () -> resource.cancelJob(backendRequest, response, jobId));
            for (final JobPaginatedResult listed : List.of(
                    resource.activeJobs(backendRequest, response, queueName, 1, 500).getEntity(),
                    resource.listJobs(backendRequest, response, 1, 500).getEntity(),
                    resource.activeJobs(backendRequest, response, 1, 500).getEntity(),
                    resource.completedJobs(backendRequest, response, 1, 500).getEntity(),
                    resource.successfulJobs(backendRequest, response, 1, 500).getEntity(),
                    resource.canceledJobs(backendRequest, response, 1, 500).getEntity(),
                    resource.failedJobs(backendRequest, response, 1, 500).getEntity(),
                    resource.abandonedJobs(backendRequest, response, 1, 500).getEntity())) {
                Assertions.assertFalse(ids(listed).contains(jobId), "job listed for another user");
            }

            resource.monitorJob(backendRequest, response, jobId);
            verify(monitor).monitorJob(eq(jobId),
                    argThat(user -> backendUser.getUserId().equals(user.getUserId())));
        } finally {
            UserDataGen.remove(backendUser);
        }
    }

    /**
     * Collects the ids of the jobs in a listing.
     *
     * @param result the listing
     * @return the job ids
     */
    private static Set<String> ids(final JobPaginatedResult result) {
        return result.jobs().stream().map(Job::id).collect(Collectors.toSet());
    }

}
