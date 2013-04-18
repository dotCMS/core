package com.dotcms.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;

@Path("/auditPublishing")
public class AuditPublishingResource extends WebResource {
	public static String MY_TEMP = "";
	private PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();

	@GET
	@Path("/get/{bundleId:.*}")
	@Produces(MediaType.TEXT_XML)
	public String get(@PathParam("bundleId") String bundleId) {
		PublishAuditStatus status = null;
		
		try {
			status = auditAPI.getPublishAuditStatus(bundleId);
			
			if(status != null)
				return (String) status.getStatusPojo().getSerialized();
		} catch (DotPublisherException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	
}
