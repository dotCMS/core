package com.dotcms.rest.personas;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.portlets.personas.model.Persona;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.BaseRestPortlet;

@Path("/personas")
public class PersonasResourcePortlet extends BaseRestPortlet{

	
	/*
	 * Returns a JSON of the personas in the given Host
	 * 
	 * 
	*/
	@GET
	@JSONP
	@Path("/sites/{id}")
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Map<String,Persona> list(@Context HttpServletRequest request, @PathParam("id") String siteId){
		
		return null;
	}
	
}