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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.quartz.SchedulerException;

/**
 * Saves the jobs people schedule from the Scheduler portlet, in its {@code User Job} group. The
 * caller needs the Scheduler portlet. A job class must come from the list the Scheduler form offers
 * ({@link SchedulerJobLocator#getJobClassess()}) unless the caller is a CMS Administrator.
 */
@Path("/v1/scheduler/jobs")
@Tag(name = "Scheduler", description = "Jobs scheduled from the Scheduler portlet")
public class SchedulerJobResource {

    /** The group the Scheduler portlet lists and saves people's jobs in. */
    static final String USER_JOB_GROUP = "User Job";

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

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
                    + "schedule a class from the list the Scheduler form offers. The job runs as "
                    + "the user who saved it.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job scheduled",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySchedulerJobView.class))),
                    @ApiResponse(responseCode = "400", description = "A required value is missing, "
                            + "or the cron expression, dates or class are not valid",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "Not authenticated as a back-end "
                            + "user, or the user lacks the Scheduler portlet",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The caller is not a CMS "
                            + "Administrator and chose a class outside the list",
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
        if (!UtilMethods.isSet(jobName)) {
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
    @Path("/{jobName}")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "updateSchedulerJob",
            summary = "Update a scheduled job",
            description = "Saves an existing job in the Scheduler portlet's User Job group with new "
                    + "settings. The same checks as scheduling a job apply, and the job then runs "
                    + "as the user who saved it.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job saved",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntitySchedulerJobView.class))),
                    @ApiResponse(responseCode = "400", description = "A required value is missing, "
                            + "or the cron expression, dates or class are not valid",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "Not authenticated as a back-end "
                            + "user, or the user lacks the Scheduler portlet",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "403", description = "The caller is not a CMS "
                            + "Administrator and chose a class outside the list",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "No job with this name in the User Job group",
                            content = @Content(mediaType = "application/json"))
            })
    public ResponseEntitySchedulerJobView updateJob(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "The name of the job to update", required = true)
            @PathParam("jobName") final String jobName,
            @RequestBody(description = "The job's new settings", required = true,
                    content = @Content(schema = @Schema(implementation = SchedulerJobForm.class)))
            final SchedulerJobForm form) throws DotDataException {

        final User user = init(request, response);
        if (!exists(jobName)) {
            throw refused(Response.Status.NOT_FOUND, user, "message.Scheduler.jobNotFound");
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

    private static boolean exists(final String jobName) {
        try {
            return !QuartzUtils.getStandardScheduledTask(jobName, USER_JOB_GROUP).isEmpty();
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
        if (!SchedulerJobLocator.getJobClassess().contains(form.javaClass())
                && !APILocator.getUserAPI().isCMSAdmin(user)) {
            throw refused(Response.Status.FORBIDDEN, user, "message.Scheduler.javaClassNotAllowed");
        }
        if (!UtilMethods.isSet(form.cronExpression())) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidCronExpression");
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
        final Map<String, String> parameters = new HashMap<>(form.properties());
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

        try {
            QuartzUtils.scheduleTask(job);
        } catch (final ParseException e) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidCronExpression");
        } catch (final ClassNotFoundException | IllegalArgumentException e) {
            // Not on the classpath, or not a Quartz job
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.javaClassNotFound");
        } catch (final SchedulerException e) {
            Logger.error(SchedulerJobResource.class, "Unable to schedule the job " + jobName, e);
            throw new DotRuntimeException("Unable to schedule the job " + jobName, e);
        }

        return new ResponseEntitySchedulerJobView(SchedulerJobForm.copyOf(form)
                .withJobName(jobName).withProperties(parameters));
    }

    private static Date parseDate(final String value, final User user) {
        if (!UtilMethods.isSet(value)) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(value);
        } catch (final ParseException e) {
            throw refused(Response.Status.BAD_REQUEST, user, "message.Scheduler.invalidDate");
        }
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
