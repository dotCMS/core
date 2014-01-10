package com.dotcms.rest;

import com.dotcms.repackage.jersey_1_12.javax.ws.rs.GET;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.Path;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.PathParam;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.Produces;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.core.Response;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotmarketing.util.Logger;

@Path("/auditPublishing")
public class AuditPublishingResource extends WebResource {
    public static String MY_TEMP = "";
    private PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();

    @GET
    @Path("/get/{bundleId:.*}")
    @Produces(MediaType.TEXT_XML)
    public Response get(@PathParam("bundleId") String bundleId) {
        PublishAuditStatus status = null;

        try {
            status = auditAPI.getPublishAuditStatus(bundleId);

            if(status != null)
                return Response.ok((String) status.getStatusPojo().getSerialized()).build();
        } catch (DotPublisherException e) {
            Logger.warn(this, "error trying to get status for bundle "+bundleId,e);
        }

        return Response.status(404).build();
    }


}