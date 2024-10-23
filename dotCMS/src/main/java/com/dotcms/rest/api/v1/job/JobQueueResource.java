package com.dotcms.rest.api.v1.job;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.VisibleForTesting;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

@Path("/v1/jobs")
public class JobQueueResource {

    private final WebResource webResource;

    private final JobQueueHelper helper;

    public JobQueueResource() {
        this(new WebResource(), CDIUtils.getBean(JobQueueHelper.class).orElseThrow(()->new IllegalStateException("JobQueueHelper Bean not found")));
    }

    @VisibleForTesting
    public JobQueueResource(WebResource webResource, JobQueueHelper helper) {
        this.webResource = webResource;
        this.helper = helper;
    }

    @POST
    @Path("/{queueName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(
            @Context HttpServletRequest request,
            @PathParam("queueName") String queueName,
            @BeanParam JobParams form) throws JsonProcessingException, DotDataException {
           new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            final String jobId = helper.createJob(queueName, form, request);
            return Response.ok(new ResponseEntityView<>(jobId)).build();
    }

    @GET
    @Path("/queues")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<Set<String>> getQueues(@Context HttpServletRequest request) {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            return new ResponseEntityView<>(helper.getQueueNames());
    }

    @GET
    @Path("/{jobId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<Job> getJobStatus(@Context HttpServletRequest request, @PathParam("jobId") String jobId)
            throws DotDataException {

            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();

            Job job = helper.getJob(jobId);
            return new ResponseEntityView<>(job);
    }

    @POST
    @Path("/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public ResponseEntityView<String> cancelJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId)
            throws DotDataException {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            helper.cancelJob(jobId);
            return new ResponseEntityView<>("Job cancelled successfully");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView <JobPaginatedResult> listJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            final JobPaginatedResult result = helper.getJobs(page, pageSize);
            return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/{queueName}/active")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> activeJobs(@Context HttpServletRequest request, @PathParam("queueName") String queueName,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            final JobPaginatedResult result = helper.getActiveJobs(queueName, page, pageSize);
            return new ResponseEntityView<>(result);
    }

    @GET
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntityView<JobPaginatedResult> failedJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            final JobPaginatedResult result = helper.getFailedJobs(page, pageSize);
            return new ResponseEntityView<>(result);
    }


    @GET
    @Path("/{jobId}/monitor")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput monitorJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();

        Job job = null;
        try {
            job = helper.getJob(jobId);
        } catch (DotDataException | DoesNotExistException e) {
            // ignore
        }

        final EventOutput eventOutput = new EventOutput();

        if (job == null || helper.isNotWatchable(job)) {
            try {
                OutboundEvent event = new OutboundEvent.Builder()
                        .mediaType(MediaType.TEXT_HTML_TYPE)
                        .name("job-not-found")
                        .data(String.class, "404")
                        .build();
                eventOutput.write(event);
                eventOutput.close();
            } catch (IOException e) {
                Logger.error(this, "Error closing SSE connection", e);
            }
        }  else {
            // Callback for watching job updates and sending them to the client
            helper.watchJob(job.id(), watched -> {
                if (!eventOutput.isClosed()) {
                    try {
                        OutboundEvent event = new OutboundEvent.Builder()
                                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                .name("job-update")
                                .data(Map.class, helper.getJobStatusInfo(watched))
                                .build();
                        eventOutput.write(event);
                    } catch (IOException e) {
                        Logger.error(this, "Error writing SSE event", e);
                        throw new DotRuntimeException(e);
                    }
                }
            });
        }
        return eventOutput;
    }
}