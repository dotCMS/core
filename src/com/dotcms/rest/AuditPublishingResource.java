package com.dotcms.rest;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path ("/auditPublishing")
public class AuditPublishingResource extends WebResource {

    public static String MY_TEMP = "";
    private PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();

    @GET
    @Path ("/get/{bundleId:.*}")
    @Produces (MediaType.TEXT_XML)
    public Response get ( @Context HttpServletRequest request, @PathParam ("params") String params ) {

        InitDataObject initData = init( params, false, request, false );

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

        String bundleId = initData.getParamsMap().get( "bundleId" );
        PublishAuditStatus status;
        try {
            status = auditAPI.getPublishAuditStatus( bundleId );

            if ( status != null ) {
                return responseResource.response( status.getStatusPojo().getSerialized() );
            }
        } catch ( DotPublisherException e ) {
            Logger.warn( this, "error trying to get status for bundle " + bundleId, e );
        }

        return responseResource.responseError( 404 );
    }

}