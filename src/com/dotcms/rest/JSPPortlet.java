/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dotcms.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

/**
 * <a href="JSPPortlet.java.html"><b><i>View Source</i></b></a>
 * This is a simple class that extends the RestPortlet that can be re-used
 * in the portlet.xml.  To use and reuse this portlet, all you need to do is
 * have a "render" jsp file under a folder with the portlet id, e.g.
 * The "render" jsp path would look like: /WEB-INF/jsp/{portlet-id}/render.jsp
 * If you extend this portlet, you can use Jersey Annotations to produce or consume 
 * web services 
 *
 */
@Path("/portlet")
public class JSPPortlet extends BaseRestPortlet {

	@GET
	@Path("/{params:.*}")
	@Produces("text/html")
	public Response getLayout(@Context HttpServletRequest request, @PathParam("params") String params)
			throws DotDataException, DotSecurityException, ServletException, IOException, DotRuntimeException, PortalException, SystemException {

		return super.getLayout(request, params);

	}
}