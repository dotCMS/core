package com.dotcms.rest.api.v1.job;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/v1/jobs")
public class JobQueueResource {

    private final WebResource webResource;
    private final JobQueueManagerAPI jobQueueManagerAPI;

    //@Inject
    MyTestBean myTestBean;

    public JobQueueResource() {
        this(new WebResource(), APILocator.getJobQueueManagerAPI());
    }

    //@Inject
    public JobQueueResource(WebResource webResource, JobQueueManagerAPI jobQueueManagerAPI) {
        this.webResource = webResource;
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createJob(@Context HttpServletRequest request,
            @QueryParam("queueName") String queueName,
            Map<String, Object> jobParameters) {
        try {
            InitDataObject initData = webResource.init(null, true, request, true, null);
            User user = initData.getUser();

            String jobId = jobQueueManagerAPI.createJob(queueName, jobParameters);

            return Response.ok(new ResponseEntityView<>(jobId)).build();
        } catch (Exception e) {
            Logger.error(this, "Error creating job", e);
            return Response.serverError().entity(new ResponseEntityView<>(e.getMessage())).build();
        }
    }

    @GET
    @Path("/{jobId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobStatus(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {
        try {
            InitDataObject initData = webResource.init(null, true, request, true, null);
            User user = initData.getUser();

            Job job = jobQueueManagerAPI.getJob(jobId);
            Map<String, Object> statusInfo = Map.of(
                    "state", job.state(),
                    "progress", job.progress(),
                    "executionNode", job.executionNode().orElse("N/A")
            );

            return Response.ok(new ResponseEntityView<>(statusInfo)).build();
        } catch (Exception e) {
            Logger.error(this, "Error getting job status", e);
            return Response.serverError().entity(new ResponseEntityView<>(e.getMessage())).build();
        }
    }

    @POST
    @Path("/{jobId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {
        try {
            InitDataObject initData = webResource.init(null, true, request, true, null);
            User user = initData.getUser();

            jobQueueManagerAPI.cancelJob(jobId);
            return Response.ok(new ResponseEntityView<>("Job cancelled successfully")).build();
        } catch (Exception e) {
            Logger.error(this, "Error cancelling job", e);
            return Response.serverError().entity(new ResponseEntityView<>(e.getMessage())).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listJobs(@Context HttpServletRequest request,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        try {
            System.out.println(myTestBean.sayHello());
            InitDataObject initData = webResource.init(null, true, request, true, null);
            User user = initData.getUser();

            JobPaginatedResult result = jobQueueManagerAPI.getJobs(page, pageSize);
            return Response.ok(new ResponseEntityView(result)).build();
        } catch (Exception e) {
            Logger.error(this, "Error listing jobs", e);
            return Response.serverError().entity(new ResponseEntityView<>(e.getMessage())).build();
        }
    }

    @GET
    @Path("/{jobId}/monitor")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @NoCache
    public EventOutput monitorJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {
        EventOutput eventOutput = new EventOutput();
        try {
            InitDataObject initData = webResource.init(null, true, request, true, null);
            User user = initData.getUser();

            jobQueueManagerAPI.watchJob(jobId, job -> {
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
