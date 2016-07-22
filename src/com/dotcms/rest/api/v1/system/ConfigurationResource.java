package com.dotcms.rest.api.v1.system;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.ApiProvider;

/**
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 22, 2016
 *
 */
@SuppressWarnings("serial")
public class ConfigurationResource implements Serializable {

	private final WebResource webResource;

	public ConfigurationResource() {
		this(new WebResource(new ApiProvider()));
	}

	@VisibleForTesting
	protected ConfigurationResource(WebResource webResource) {
		this.webResource = webResource;
	}

	@GET
	@JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response get(@Context final HttpServletRequest request) {
		return null;
	}

}
