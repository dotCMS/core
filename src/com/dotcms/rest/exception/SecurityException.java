package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.WebApplicationException;
import com.dotcms.repackage.javax.ws.rs.core.Response;


public class SecurityException extends WebApplicationException {

	/**
	 *
	 */
	private static final long serialVersionUID = -4222030277463217807L;

	public SecurityException(String msg) {
		this(msg, Response.Status.FORBIDDEN);
	}

	public SecurityException(String msg, Response.Status status) {
		super(Response.status(status).entity(msg).type("text/plain").build());
	}

}
