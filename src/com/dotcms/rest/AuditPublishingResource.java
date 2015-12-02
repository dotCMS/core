package com.dotcms.rest;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.util.Logger;

@Path("/auditPublishing")
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


}