package com.dotcms.rest;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Path("/auditPublishing")
@Tag(name = "Publishing")
public class AuditPublishingResource {
    private PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();

    @GET
    @Path("/get/{bundleId:.*}")
    @Produces(MediaType.TEXT_XML)
    public Response get(@PathParam("bundleId") String bundleId) {
        PublishAuditStatus status = null;

        try {
            status = auditAPI.getPublishAuditStatus(bundleId);

            if(status != null)
                return Response.ok( status.getStatusPojo().getSerialized()).build();
        } catch (DotPublisherException e) {
            Logger.warn(this, "error trying to get status for bundle "+bundleId,e);
        }

        return Response.status(404).build();
    }

    @POST
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll( List<String> bundleIds) {
        try {
            final List<PublishAuditStatus> statuses = auditAPI.getPublishAuditStatuses(bundleIds);

            if(statuses != null)
                return Response.ok( statuses.stream().map(status -> status.getStatusPojo().getSerialized() ) ).build();
        } catch (DotPublisherException e) {
            Logger.warn(this, "error trying to get status for bundle "+bundleIds.get(0),e);
        }

        return Response.status(404).build();
    }


}