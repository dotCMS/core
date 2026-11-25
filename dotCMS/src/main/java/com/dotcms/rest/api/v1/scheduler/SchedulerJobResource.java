package com.dotcms.rest.api.v1.scheduler;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.job.ContentImportThread;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SchedulerJobLocator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;

/**
 * Saves the jobs people schedule from the Scheduler portlet, in its {@code User Job} group. The
 * caller needs the Scheduler portlet. A job class must come from the list the Scheduler form offers
 * ({@link SchedulerJobLocator#getJobClassess()}) and not be one of {@link #ADMIN_ONLY_CLASSES}, unless
 * the caller is a CMS Administrator.
 */
@Path("/v1/scheduler/jobs")
@Tag(name = "Scheduler", description = "Jobs scheduled from the Scheduler portlet")
public class SchedulerJobResource {

    /** The group the Scheduler portlet lists and saves people's jobs in. */
    static final String USER_JOB_GROUP = "User Job";

    /**
     * Classes on the Scheduler form's list whose parameters are file paths on the server, so only CMS
     * Administrators may schedule them. {@link ContentImportThread} reads the files under {@code path},
     * writes result files under {@code logpath}, and moves each imported file aside.
     */
    static final Set<String> ADMIN_ONLY_CLASSES = Set.of(ContentImportThread.class.getName());

    /**
     * The whole value must match, and dates that do not exist (such as February 30) are refused. The
     * year takes exactly four digits: a signed year such as +999999999 does not fit a {@link Date}.
     */
    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendPattern("-MM-dd HH:mm")
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    /** Quartz's job and trigger name columns, in characters. */
    private static final int NAME_LIMIT = 80;

    /** Quartz's cron expression column, in characters. */
    private static final int CRON_EXPRESSION_LIMIT = 80;

    /** Quartz's job and trigger description columns, in characters. */
    private static final int DESCRIPTION_LIMIT = 120;

    private final WebResource webResource = new WebResource();

    /**
     * Schedules a new job in the {@code User Job} group.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param form     the job to schedule
     * @return the saved job
     * @throws DotDataException when the caller's roles cannot be read
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "createSchedulerJob",
            summary = "Schedule a job",
            description = "Schedules a new job in the Scheduler portlet's User Job group. The caller "
                    + "needs the Scheduler portlet. Users who are not CMS Administrators can only "
                    + "schedule a class from the list the Scheduler form offers, and not the content "
                    + "import job (ContentImportThread), whose parameters are file paths on the "
                    + "server. The job runs as the user who saved it.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job scheduled",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySchedulerJobView.class))),
                    @ApiResponse(responseCode = "400", description = "A required value is missing, "
                            + "the job name is longer than 72 characters, the cron expression "
                            + "longer than 80 or the description longer than 120, "
                            + "or the cron expression, dates or class are not valid",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "Not authenticated as a back-end "
                            + "user, or the user lacks the Scheduler portlet",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The caller is not a CMS "
                            + "Administrator and chose a class outside the list, or the content import job",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "409", description = "A job with this name already exists",
                            content = @Content(mediaType = "application/json"))
            })
    public ResponseEntitySchedulerJobView createJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "The job to schedule", required = true,
                    content = @Content(schema = @Schema(implementation = SchedulerJobForm.class)))
            final SchedulerJobForm form) throws DotDataException {

        final User user = init(request, response);
        final String jobName = form == null ? null : form.jobName();
        // Quartz names the trigger <name>_trigger, and both names must fit an 80-character column
        if (!UtilMethods.isSet(jobName) || characters(jobName + "_trigger") > NAME_LIMIT) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidJobName");
        }
        if (exists(jobName)) {
            throw refused(Response.Status.CONFLICT, user, "message.Scheduler.jobAlreadyExists");
        }
        return save(jobName, form, user);
    }

    /**
     * Saves an existing job in the {@code User Job} group with new settings.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param jobName  the name of the job to update
     * @param form     the job's new settings
     * @return the saved job
     * @throws DotDataException when the caller's roles cannot be read
     */
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "updateSchedulerJob",
            summary = "Update a scheduled job",
            description = "Saves an existing job in the Scheduler portlet's User Job group with new "
                    + "settings. The job is named in the jobName query parameter, so names with "
                    + "characters a URL path refuses, such as ';', can be updated. The same checks as "
                    + "scheduling a job apply, and the job then runs as the user who saved it.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job saved",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySchedulerJobView.class))),
                    @ApiResponse(responseCode = "400", description = "A required value is missing, "
                            + "the job name is longer than 72 characters, the cron expression "
                            + "longer than 80 or the description longer than 120, "
                            + "or the cron expression, dates or class are not valid",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "Not authenticated as a back-end "
                            + "user, or the user lacks the Scheduler portlet",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The caller is not a CMS "
                            + "Administrator and chose a class outside the list, or the content import job",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "No job with this name in the User Job group",
                            content = @Content(mediaType = "application/json"))
            })
    public ResponseEntitySchedulerJobView updateJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "The name of the job to update", required = true)
            @QueryParam("jobName") final String jobName,
            @RequestBody(description = "The job's new settings", required = true,
                    content = @Content(schema = @Schema(implementation = SchedulerJobForm.class)))
            final SchedulerJobForm form) throws DotDataException {

        final User user = init(request, response);
        if (!UtilMethods.isSet(jobName) || !exists(jobName)) {
            throw refused(Response.Status.NOT_FOUND, user, "message.Scheduler.jobNotFound");
        }
        // A job record without a trigger may have a name too long for the trigger Quartz creates
        if (characters(jobName + "_trigger") > NAME_LIMIT) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidJobName");
        }
        return save(jobName, form, user);
    }

    private User init(final HttpServletRequest request, final HttpServletResponse response) {
        return new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.JOBS.toString())
                .init()
                .getUser();
    }

    /**
     * Tells whether the User Job group has a job record with this name. A record that lost its
     * trigger still counts, so it can be saved again with PUT and is not overwritten by POST.
     */
    private static boolean exists(final String jobName) {
        try {
            return QuartzUtils.isJobScheduled(jobName, USER_JOB_GROUP);
        } catch (final SchedulerException e) {
            throw new DotRuntimeException("Unable to read the job " + jobName, e);
        }
    }

    /**
     * Checks the job's settings and schedules it under the given name, recording the saving user
     * under {@link ScheduledTask#SCHEDULED_BY_USER_ID} so jobs that act on content run with that
     * user's permissions.
     */
    private static ResponseEntitySchedulerJobView save(final String jobName, final SchedulerJobForm form,
            final User user) throws DotDataException {

        if (form == null || !UtilMethods.isSet(form.javaClass())) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidJavaClass");
        }
        final boolean openToSchedulerUsers = SchedulerJobLocator.getJobClassess().contains(form.javaClass())
                && !ADMIN_ONLY_CLASSES.contains(form.javaClass());
        if (!openToSchedulerUsers && !APILocator.getUserAPI().isCMSAdmin(user)) {
            throw refused(Response.Status.FORBIDDEN, user, "message.Scheduler.javaClassNotAllowed");
        }
        // Longer values do not fit Quartz's columns, and the database would refuse them with a 500
        if (!UtilMethods.isSet(form.cronExpression())
                || characters(form.cronExpression()) > CRON_EXPRESSION_LIMIT) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidCronExpression");
        }
        if (form.jobDescription() != null && characters(form.jobDescription()) > DESCRIPTION_LIMIT) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidJobDescription");
        }
        final Date startDate = parseDate(form.startDate(), user);
        final Date endDate = parseDate(form.endDate(), user);
        if (endDate != null) {
            if (startDate == null) {
                throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.startDateNeeded");
            }
            if (endDate.before(startDate)) {
                throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.endDateBeforeStartDate");
            }
            if (endDate.before(new Date())) {
                throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.endDateBeforeActualDate");
            }
        }

        // The job runs as whoever saved it last; a submitted value is ignored
        final Map<String, String> parameters = form.properties() == null ? new HashMap<>()
                : new HashMap<>(form.properties());
        parameters.remove(ScheduledTask.SCHEDULED_BY_USER_ID);
        final Map<String, Object> properties = new HashMap<>(parameters);
        properties.put(ScheduledTask.SCHEDULED_BY_USER_ID, user.getUserId());

        final CronScheduledTask job = new CronScheduledTask();
        job.setJobName(jobName);
        job.setJobGroup(USER_JOB_GROUP);
        job.setJobDescription(form.jobDescription());
        job.setJavaClassName(form.javaClass());
        job.setProperties(properties);
        job.setStartDate(startDate);
        job.setEndDate(endDate);
        job.setCronExpression(form.cronExpression());

        // Quartz refuses a trigger that never fires; checking first answers 400 instead of 500
        try {
            // Quartz creates the job through a public no-argument constructor, so an abstract class
            // (the Scheduler form's list includes DotJob) or one without that constructor cannot run
            final Class<?> jobClass = Class.forName(form.javaClass());
            if (!Job.class.isAssignableFrom(jobClass) || Modifier.isAbstract(jobClass.getModifiers())
                    || !Modifier.isPublic(jobClass.getModifiers())) {
                throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.javaClassNotFound");
            }
            jobClass.getConstructor();
            if (new CronTrigger(jobName, USER_JOB_GROUP, jobName, USER_JOB_GROUP, startDate, endDate,
                    form.cronExpression()).computeFirstFireTime(null) == null) {
                throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidJobSettings");
            }
            QuartzUtils.scheduleTask(job);
        } catch (final ParseException e) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidCronExpression");
        } catch (final ObjectAlreadyExistsException e) {
            // Another save created the job after this request checked for it
            throw refused(Response.Status.CONFLICT, user, "message.Scheduler.jobAlreadyExists");
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalArgumentException e) {
            // Not on the classpath, no public no-argument constructor, or not a Quartz job
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.javaClassNotFound");
        } catch (final SchedulerException e) {
            Logger.error(SchedulerJobResource.class, "Unable to schedule the job " + jobName, e);
            throw new DotRuntimeException("Unable to schedule the job " + jobName, e);
        }

        return new ResponseEntitySchedulerJobView(SchedulerJobForm.copyOf(form)
                .withJobName(jobName).withProperties(parameters));
    }

    /**
     * Counts characters the way PostgreSQL counts them against a {@code varchar} limit. Java's
     * {@link String#length()} counts a character outside the Basic Multilingual Plane, such as an
     * emoji, as two.
     *
     * @param value the value to measure
     * @return the number of Unicode code points in the value
     */
    private static int characters(final String value) {
        return value.codePointCount(0, value.length());
    }

    /**
     * Reads a date in the {@code yyyy-MM-dd HH:mm} format, in the server's time zone.
     *
     * @return the date, or null when none was given
     * A time that occurs twice when the clocks go back is read as the second occurrence.
     *
     * @throws WebApplicationException 400 when the value is not a whole, existing date in that format,
     *                                  or names a time the clocks skip when daylight saving starts
     */
    private static Date parseDate(final String value, final User user) {
        if (!UtilMethods.isSet(value)) {
            return null;
        }
        final LocalDateTime local;
        try {
            local = LocalDateTime.parse(value, DATE_FORMAT);
        } catch (final DateTimeParseException e) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidDate");
        }
        // A time the clocks skip when daylight saving starts would otherwise move an hour later
        final ZoneId zone = ZoneId.systemDefault();
        if (zone.getRules().getValidOffsets(local).isEmpty()) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidDate");
        }
        // A time that occurs twice when daylight saving ends is read as the second one, as the
        // Scheduler form's earlier date parsing and Quartz's cron trigger both do
        return Date.from(local.atZone(zone).withLaterOffsetAtOverlap().toInstant());
    }

    /**
     * Builds the error response for a refused save, with the message in the caller's language so
     * the Scheduler form can show it as is.
     */
    private static WebApplicationException refused(final Response.Status status, final User user,
            final String messageKey) {
        final String message = Try.of(() -> LanguageUtil.get(user, messageKey)).getOrElse(messageKey);
        return new WebApplicationException(Response.status(status)
                .entity(new ResponseEntityView<>(List.of(new ErrorEntity(messageKey, message))))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
    }
}
