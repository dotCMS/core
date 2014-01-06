package com.dotcms.rest;

import javax.servlet.http.HttpServletRequest;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.FormParam;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.POST;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.Path;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.core.Context;

@Path("/dummy")
public class DummyResource extends WebResource {

	@POST
	@Path("/postauth")
	public String doPostAuthentication(@Context HttpServletRequest request, @FormParam("user") String user, @FormParam("password") String password) {
		init("user/"+user+"/password/"+password, true, request, true);
		return "success";
	}
}
