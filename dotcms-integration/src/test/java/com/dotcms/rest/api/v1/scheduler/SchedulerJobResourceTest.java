package com.dotcms.rest.api.v1.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.web.UpdatePageTemplatePathJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.job.ContentImportThread;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.SchedulerException;

/**
 * Integration tests for {@link SchedulerJobResource}, which saves the jobs people schedule from the
 * Scheduler portlet.
 */
public class SchedulerJobResourceTest {

    /** Fires only at 01:00 on 1 January 2099, so no job runs during the test. */
    private static final String NEVER = "0 0 1 1 1 ? 2099";

    /** A class on the Scheduler form's list that takes no file paths. */
    private static final String LISTED = "com.dotmarketing.quartz.job.BinaryCleanupJob";

    private static SchedulerJobResource resource;
    private static User jobsPortletUser;
    private static User noPortletUser;
    private static User admin;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new SchedulerJobResource();
        jobsPortletUser = backendUser(PortletID.JOBS.toString());
        noPortletUser = backendUser();
        admin = APILocator.getUserAPI().loadUserById("dotcms.org.1");
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a Scheduler portlet user who is not a CMS Administrator, and the content import job,
     * whose parameters are file paths on the server
     * Expected: 403, and no job is created
     */
    @Test
    public void test_createJob_contentImport_byNonAdmin_shouldBeRefused() {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            assertStatus(Response.Status.FORBIDDEN, () -> resource.createJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(), form(jobName, ContentImportThread.class.getName())));
            assertFalse(exists(jobName));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#updateJob}
     * Given: a content import job saved by a CMS Administrator, saved again by a Scheduler portlet user
     * who is not a CMS Administrator
     * Expected: 403, and the job still runs as the administrator
     */
    @Test
    public void test_updateJob_contentImport_byNonAdmin_shouldBeRefused() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            resource.createJob(requestFor(admin), new MockHttpResponse().response(),
                    form(jobName, ContentImportThread.class.getName()));

            assertStatus(Response.Status.FORBIDDEN, () -> resource.updateJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(), jobName, form(jobName, ContentImportThread.class.getName())));
            assertEquals(admin.getUserId(), saved(jobName).getProperties().get(ScheduledTask.SCHEDULED_BY_USER_ID));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a CMS Administrator and the content import job
     * Expected: the job is scheduled and runs as the administrator
     */
    @Test
    public void test_createJob_contentImport_byAdmin_shouldBeScheduled() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            resource.createJob(requestFor(admin), new MockHttpResponse().response(),
                    form(jobName, ContentImportThread.class.getName()));

            final ScheduledTask job = saved(jobName);
            assertEquals(ContentImportThread.class.getName(), job.getJavaClassName());
            assertEquals(admin.getUserId(), job.getProperties().get(ScheduledTask.SCHEDULED_BY_USER_ID));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a JSON body, read with the REST object mapper, whose {@code properties} is null
     * Expected: the body is accepted and the job is scheduled with no parameters other than the
     * user it runs as
     */
    @Test
    public void test_createJob_nullProperties_shouldBeScheduled() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            final SchedulerJobForm form = DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                    .readValue("{\"jobName\":\"" + jobName + "\",\"javaClass\":\"" + LISTED
                            + "\",\"cronExpression\":\"" + NEVER + "\",\"properties\":null}", SchedulerJobForm.class);

            resource.createJob(requestFor(jobsPortletUser), new MockHttpResponse().response(), form);

            assertEquals(Map.of(ScheduledTask.SCHEDULED_BY_USER_ID, jobsPortletUser.getUserId()),
                    saved(jobName).getProperties());
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a Scheduler portlet user who is not a CMS Administrator, and a class outside the list
     * the Scheduler form offers
     * Expected: 403, and no job is created
     */
    @Test
    public void test_createJob_classOutsideList_byNonAdmin_shouldBeRefused() {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            assertStatus(Response.Status.FORBIDDEN, () -> resource.createJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(), form(jobName, UpdatePageTemplatePathJob.class.getName())));
            assertFalse(exists(jobName));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#updateJob}
     * Given: an existing job, saved again by a Scheduler portlet user who is not a CMS Administrator
     * with a class outside the list
     * Expected: 403, and the job keeps its class
     */
    @Test
    public void test_updateJob_classOutsideList_byNonAdmin_shouldBeRefused() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            resource.createJob(requestFor(jobsPortletUser), new MockHttpResponse().response(),
                    form(jobName, LISTED));

            assertStatus(Response.Status.FORBIDDEN, () -> resource.updateJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(), jobName, form(jobName, UpdatePageTemplatePathJob.class.getName())));
            assertEquals(LISTED, saved(jobName).getJavaClassName());
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a CMS Administrator and a class outside the list the Scheduler form offers
     * Expected: the job is scheduled with that class
     */
    @Test
    public void test_createJob_classOutsideList_byAdmin_shouldBeScheduled() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            resource.createJob(requestFor(APILocator.getUserAPI().loadUserById("dotcms.org.1")),
                    new MockHttpResponse().response(), form(jobName, UpdatePageTemplatePathJob.class.getName()));

            assertEquals(UpdatePageTemplatePathJob.class.getName(), saved(jobName).getJavaClassName());
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a Scheduler portlet user who is not a CMS Administrator, a class from the list, and a
     * {@link ScheduledTask#SCHEDULED_BY_USER_ID} parameter naming another user
     * Expected: the job is scheduled, records the user who saved it, and keeps the other parameters
     */
    @Test
    public void test_createJob_listedClass_shouldRecordTheSavingUser() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            final SchedulerJobForm form = SchedulerJobForm.builder()
                    .from(form(jobName, LISTED))
                    .properties(Map.of("structure", "SomeType", ScheduledTask.SCHEDULED_BY_USER_ID, "another-user"))
                    .build();

            final SchedulerJobForm returned = resource.createJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(), form).getEntity();

            final Map<String, Object> properties = saved(jobName).getProperties();
            assertEquals(jobsPortletUser.getUserId(), properties.get(ScheduledTask.SCHEDULED_BY_USER_ID));
            assertEquals("SomeType", properties.get("structure"));
            assertEquals(Map.of("structure", "SomeType"), returned.properties());
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#updateJob}
     * Given: an existing job saved again by another Scheduler portlet user with a new cron expression
     * Expected: the job has the new cron expression and runs as the user who saved it last
     */
    @Test
    public void test_updateJob_listedClass_shouldSaveTheNewSettings() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        final User otherUser = backendUser(PortletID.JOBS.toString());
        try {
            resource.createJob(requestFor(jobsPortletUser), new MockHttpResponse().response(),
                    form(jobName, LISTED));

            resource.updateJob(requestFor(otherUser), new MockHttpResponse().response(), jobName,
                    SchedulerJobForm.builder().from(form(jobName, LISTED))
                            .cronExpression("0 0 2 1 1 ? 2099").build());

            final ScheduledTask job = saved(jobName);
            assertEquals("0 0 2 1 1 ? 2099", ((com.dotmarketing.quartz.CronScheduledTask) job).getCronExpression());
            assertEquals(otherUser.getUserId(), job.getProperties().get(ScheduledTask.SCHEDULED_BY_USER_ID));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a job name that is already taken
     * Expected: 409, and the existing job is left as it was
     */
    @Test
    public void test_createJob_existingName_shouldBeRefused() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            resource.createJob(requestFor(jobsPortletUser), new MockHttpResponse().response(),
                    form(jobName, LISTED));

            assertStatus(Response.Status.CONFLICT, () -> resource.createJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(),
                    SchedulerJobForm.builder().from(form(jobName, LISTED))
                            .cronExpression("0 0 2 1 1 ? 2099").build()));
            assertEquals(NEVER, ((com.dotmarketing.quartz.CronScheduledTask) saved(jobName)).getCronExpression());
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#updateJob}
     * Given: a job name that does not exist
     * Expected: 404, and no job is created
     */
    @Test
    public void test_updateJob_missingJob_shouldBeNotFound() {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            assertStatus(Response.Status.NOT_FOUND, () -> resource.updateJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(), jobName, form(jobName, LISTED)));
            assertFalse(exists(jobName));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: an invalid cron expression, and an end date before the start date
     * Expected: 400 for each, and no job is created
     */
    @Test
    public void test_createJob_invalidSettings_shouldBeRefused() {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        final SchedulerJobForm valid = form(jobName, LISTED);
        try {
            assertStatus(Response.Status.BAD_REQUEST, () -> resource.createJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(),
                    SchedulerJobForm.builder().from(valid).cronExpression("not a cron").build()));
            assertStatus(Response.Status.BAD_REQUEST, () -> resource.createJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(),
                    SchedulerJobForm.builder().from(valid).startDate("2098-02-01 00:00").endDate("2098-01-01 00:00").build()));
            assertFalse(exists(jobName));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a back-end user without the Scheduler portlet
     * Expected: the request is rejected, and no job is created
     */
    @Test
    public void test_createJob_withoutSchedulerPortlet_shouldBeRejected() {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            resource.createJob(requestFor(noPortletUser), new MockHttpResponse().response(),
                    form(jobName, LISTED));
            fail("the request must be rejected");
        } catch (final SecurityException expected) {
            assertFalse(exists(jobName));
        } catch (final Exception e) {
            fail("unexpected " + e);
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#updateJob}
     * Given: an existing job, and new settings whose cron expression only fires in a year that has
     * passed, so Quartz would refuse the trigger
     * Expected: 400, and the job keeps its description and parameters instead of taking the new
     * ones while keeping its old schedule
     */
    @Test
    public void test_updateJob_scheduleThatNeverFires_shouldLeaveJobUnchanged() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            resource.createJob(requestFor(admin), new MockHttpResponse().response(),
                    SchedulerJobForm.copyOf(form(jobName, LISTED)).withJobDescription("before")
                            .withProperties(Map.of("kept", "1")));

            assertStatus(Response.Status.BAD_REQUEST, () -> resource.updateJob(requestFor(admin),
                    new MockHttpResponse().response(), jobName,
                    SchedulerJobForm.copyOf(form(jobName, LISTED)).withJobDescription("after")
                            .withCronExpression("0 0 1 1 1 ? 2020").withProperties(Map.of("new", "2"))));

            final ScheduledTask job = saved(jobName);
            assertEquals("before", job.getJobDescription());
            assertEquals("1", job.getProperties().get("kept"));
            assertFalse(job.getProperties().containsKey("new"));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: start dates that a lenient parser would accept: a day that does not exist, and a
     * valid date followed by extra text
     * Expected: 400 for each, and no job is created
     */
    @Test
    public void test_createJob_invalidDate_shouldBeRefused() {
        for (final String startDate : List.of("2099-02-30 10:00", "2099-01-01 10:00 tomorrow")) {
            final String jobName = "scheduler-job-" + UUID.randomUUID();
            try {
                assertStatus(Response.Status.BAD_REQUEST, () -> resource.createJob(requestFor(admin),
                        new MockHttpResponse().response(),
                        SchedulerJobForm.copyOf(form(jobName, LISTED)).withStartDate(startDate)));
                assertFalse(startDate, exists(jobName));
            } finally {
                remove(jobName);
            }
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: a start time that does not exist in the server's time zone because the clocks move
     * forward at that moment (02:30 on 9 March 2098 in New York), and an hour later the same day
     * Expected: 400 for the skipped time instead of starting the job an hour later, and no job is
     * created; the existing time an hour later is accepted
     */
    @Test
    public void test_createJob_dateInDaylightSavingGap_shouldBeRefused() throws Exception {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        final TimeZone serverZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            assertStatus(Response.Status.BAD_REQUEST, () -> resource.createJob(requestFor(admin),
                    new MockHttpResponse().response(),
                    SchedulerJobForm.copyOf(form(jobName, LISTED)).withStartDate("2098-03-09 02:30")));
            assertFalse(exists(jobName));

            resource.createJob(requestFor(admin), new MockHttpResponse().response(),
                    SchedulerJobForm.copyOf(form(jobName, LISTED)).withStartDate("2098-03-09 03:30"));
            assertTrue(exists(jobName));
        } finally {
            TimeZone.setDefault(serverZone);
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: {@link com.dotmarketing.quartz.DotJob}, which is on the Scheduler form's list but is
     * abstract, so Quartz cannot run it
     * Expected: 400, and no job is created
     */
    @Test
    public void test_createJob_abstractClass_shouldBeRefused() {
        final String jobName = "scheduler-job-" + UUID.randomUUID();
        try {
            assertStatus(Response.Status.BAD_REQUEST, () -> resource.createJob(requestFor(jobsPortletUser),
                    new MockHttpResponse().response(), form(jobName, "com.dotmarketing.quartz.DotJob")));
            assertFalse(exists(jobName));
        } finally {
            remove(jobName);
        }
    }

    /**
     * Method to test: {@link SchedulerJobResource#createJob}
     * Given: an 80-character job name, which fits the job table, while the trigger name made from it
     * ({@code <name>_trigger}) does not fit the trigger table
     * Expected: the save fails, and the job is not stored without its trigger, which would hide it
     * from the Scheduler portlet and block the name
     */
    @Test
    public void test_createJob_triggerNotStored_shouldLeaveNoJob() throws Exception {
        final String jobName = ("scheduler-job-" + UUID.randomUUID() + "-".repeat(80)).substring(0, 80);
        try {
            resource.createJob(requestFor(admin), new MockHttpResponse().response(), form(jobName, LISTED));
            fail("the trigger name is longer than the trigger table allows");
        } catch (final DotRuntimeException expected) {
            assertNull(QuartzUtils.getScheduler().getJobDetail(jobName, SchedulerJobResource.USER_JOB_GROUP));
        } finally {
            remove(jobName);
        }
    }

    private static SchedulerJobForm form(final String jobName, final String javaClass) {
        return SchedulerJobForm.builder()
                .jobName(jobName)
                .javaClass(javaClass)
                .cronExpression(NEVER)
                .build();
    }

    private interface Call {
        Object run() throws Exception;
    }

    private static void assertStatus(final Response.Status status, final Call call) {
        try {
            call.run();
            fail("expected " + status);
        } catch (final WebApplicationException e) {
            assertEquals(status.getStatusCode(), e.getResponse().getStatus());
        } catch (final Exception e) {
            fail("expected " + status + " but got " + e);
        }
    }

    private static boolean exists(final String jobName) {
        return !tasks(jobName).isEmpty();
    }

    private static ScheduledTask saved(final String jobName) {
        final List<ScheduledTask> tasks = tasks(jobName);
        assertTrue("the job " + jobName + " must exist", !tasks.isEmpty());
        return tasks.get(0);
    }

    private static List<ScheduledTask> tasks(final String jobName) {
        try {
            return QuartzUtils.getStandardScheduledTask(jobName, SchedulerJobResource.USER_JOB_GROUP);
        } catch (final SchedulerException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void remove(final String jobName) {
        try {
            QuartzUtils.removeJob(jobName, SchedulerJobResource.USER_JOB_GROUP);
        } catch (final Exception e) {
            // nothing to remove
        }
    }

    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = new MockHeaderRequest(new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request()).request());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        return request;
    }

    private static User backendUser(final String... portletIds) throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User user = new UserDataGen().roles(roleAPI.loadBackEndUserRole()).nextPersisted();
        if (portletIds.length > 0) {
            final Layout layout = new LayoutDataGen().portletIds(portletIds).nextPersisted();
            roleAPI.addLayoutToRole(layout, roleAPI.getUserRole(user));
        }
        return user;
    }
}
