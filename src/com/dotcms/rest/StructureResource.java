package com.dotcms.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Path("/structure")
public class StructureResource extends WebResource {

	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.TEXT_PLAIN)
	public String get(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path) {
		return "Hello World!!!";
	}
}
