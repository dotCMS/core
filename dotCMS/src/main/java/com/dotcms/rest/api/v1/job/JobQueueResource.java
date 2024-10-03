package com.dotcms.rest.api.v1.job;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    public Response getQueues(@Context HttpServletRequest request) {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            return Response.ok(new ResponseEntityView<>(helper.getQueueNames())).build();

    }

    @GET
    @Path("/{jobId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobStatus(@Context HttpServletRequest request, @PathParam("jobId") String jobId)
            throws DotDataException {

            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();

            Job job = helper.getJob(jobId);
            Map<String, Object> statusInfo = Map.of(
                    "state", job.state(),
                    "progress", job.progress(),
                    "executionNode", job.executionNode().orElse("N/A")
            );

            return Response.ok(new ResponseEntityView<>(statusInfo)).build();

    }

    @POST
    @Path("/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId)
            throws DotDataException {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            helper.cancelJob(jobId);
            return Response.ok(new ResponseEntityView<>("Job cancelled successfully")).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            final JobPaginatedResult result = helper.getJobs(page, pageSize);
            return Response.ok(new ResponseEntityView<>(result)).build();

    }

    @GET
    @Path("/{queueName}/active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activeJobs(@Context HttpServletRequest request, @PathParam("queueName") String queueName,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            final JobPaginatedResult result = helper.getActiveJobs(queueName, page, pageSize);
            return Response.ok(new ResponseEntityView<>(result)).build();

    }

    @GET
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response failedJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
            new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
            final JobPaginatedResult result = helper.getFailedJobs(page, pageSize);
            return Response.ok(new ResponseEntityView<>(result)).build();
    }


    @GET
    @Path("/{jobId}/monitor")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @NoCache
    public EventOutput monitorJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {
        final EventOutput eventOutput = new EventOutput();
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        try {

            helper.watchJob(jobId, job -> {
                try {
                    OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                    eventBuilder.name("job-update");
                    eventBuilder.data(Job.class, job);
                    eventOutput.write(eventBuilder.build());
                } catch (IOException e) {
                    Logger.error(this, "Error writing SSE event", e);
                }
            });

            // Keep the connection open for a reasonable time (e.g., 5 minutes)
            if (!eventOutput.isClosed()) {
                Thread.sleep(TimeUnit.MINUTES.toMillis(5));
            }
        } catch (Exception e) {
            Logger.error(this, "Error monitoring job", e);
            Thread.currentThread().interrupt();
        } finally {
            try {
                eventOutput.close();
            } catch (IOException e) {
                Logger.error(this, "Error closing SSE connection", e);
            }
        }
        return eventOutput;
    }
}