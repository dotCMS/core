package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.FormParam;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletRequest;

@Path("/dummy")
public class DummyResource {

    private final WebResource webResource = new WebResource();

    @POST
	@Path("/postauth")
	public String doPostAuthentication(@Context HttpServletRequest request, @FormParam("user") String user, @FormParam("password") String password) {
        webResource.init("user/" + user + "/password/" + password, true, request, true, null);
        return "success";
	}
}
