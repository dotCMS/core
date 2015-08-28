package com.dotcms.rest.personas;

import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.rest.BaseRestPortlet;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.enterprise.personas.model.Persona;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.config.AuthenticationProvider;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

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