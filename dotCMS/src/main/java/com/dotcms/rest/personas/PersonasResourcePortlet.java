package com.dotcms.rest.personas;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.portlets.personas.model.Persona;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.BaseRestPortlet;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Personalization")
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