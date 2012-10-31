package com.dotcms.rest;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;

@Path("/auditPublishing")
public class AuditPublishingResource extends WebResource {
	public static String MY_TEMP = "";
	private PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();

	@GET
	@Path("/get/{bundleId:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	public PublishAuditHistory get(@PathParam("bundleId") String bundleId) {
		Map<String, Object> status = null;
		
		try {
			status = auditAPI.getPublishAuditStatus(bundleId);
			
			if(status != null)
				return PublishAuditHistory.getObjectFromString((String) status.get("status_pojo"));
		} catch (DotPublisherException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	
}
