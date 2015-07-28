package com.dotcms.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

/**
 * <a href="JSPPortlet.java.html"><b><i>View Source</i></b></a> This is a simple
 * class that extends the RestPortlet that can be re-used in the portlet.xml. To
 * use and reuse this portlet, all you need to do is have a "render" jsp file
 * under a folder with the portlet id, e.g. The "render" jsp path would look
 * like: /WEB-INF/jsp/{portlet-id}/render.jsp If you extend this portlet, you
 * can use Jersey Annotations to produce or consume web services
 * 
 */
@Path("/portlet")
public class JSPPortlet extends BaseRestPortlet {

	@GET
	@Path("/{params:.*}")
	@Produces("text/html")
	public Response layoutGet(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws DotDataException,
			DotSecurityException, ServletException, IOException, DotRuntimeException, PortalException, SystemException {

		return super.getLayout(request, response, params);
	}

	@POST
	@Path("/{params:.*}")
	@Produces("text/html")
	public Response layoutPost(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws DotDataException,
			DotSecurityException, ServletException, IOException, DotRuntimeException, PortalException, SystemException {

		return super.getLayout(request, response, params);
	}

}