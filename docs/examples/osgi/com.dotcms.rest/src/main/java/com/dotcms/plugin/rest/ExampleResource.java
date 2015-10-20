package com.dotcms.plugin.rest;

import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.core.CacheControl;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.ResponseBuilder;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

@Path("/example")
public class ExampleResource  {

    private final WebResource webResource = new WebResource();

	@PUT
	public Response doPut(@Context HttpServletRequest request, @PathParam("params") String params) throws URISyntaxException {

		InitDataObject auth = webResource.init(true, request, false);
		CacheControl cc = new CacheControl();
		cc.setNoCache(true);
		User user = auth.getUser();
		String username = (user != null) ? user.getFullName() : " unknown ";
		ResponseBuilder builder = Response.ok("{\"result\":\"" + username + " PUT!\"}", "application/json");
		return builder.cacheControl(cc).build();
	}

	@POST
	public Response doPost(@Context HttpServletRequest request, @PathParam("params") String params) throws URISyntaxException {
		InitDataObject auth = webResource.init(true, request, false);
		User user = auth.getUser();
		String username = (user != null) ? user.getFullName() : " unknown ";
		CacheControl cc = new CacheControl();
		cc.setNoCache(true);
		ResponseBuilder builder = Response.ok("{\"result\":\"" + username + " POST!\"}", "application/json");
		return builder.cacheControl(cc).build();
	}

	/**
	 * This is an authenticated rest service.
	 * 
	 * @param request
	 * @param params
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@GET
	@Path("/auth{params:.*}")
	public Response loadJson(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException,
			DotDataException, DotSecurityException {
		// force authentication
		InitDataObject auth = webResource.init(true, request, true);
		User user = auth.getUser();
		String username = (user != null) ? user.getFullName() : " unknown ";
		CacheControl cc = new CacheControl();
		cc.setNoCache(true);
		ResponseBuilder builder = Response.ok("{\"result\":\"/test/" + username + " GET!\"}", "application/json");
		return builder.cacheControl(cc).build();

	}

	@GET
	public Response loadRoot(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException,
			DotDataException, DotSecurityException {
		InitDataObject auth = webResource.init(true, request, false);
		User user = auth.getUser();
		String username = (user != null) ? user.getFullName() : " unknown ";
		CacheControl cc = new CacheControl();
		cc.setNoCache(true);
		ResponseBuilder builder = Response.ok("{\"result\":\"" + username + " GET!\"}", "application/json");
		return builder.cacheControl(cc).build();

	}

}