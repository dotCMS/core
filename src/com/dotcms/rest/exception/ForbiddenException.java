package com.dotcms.rest.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ForbiddenException extends WebApplicationException {

	/**
	 *
	 */
	private static final long serialVersionUID = -4222030277463217807L;

	public ForbiddenException(String msg) {
		super(Response.status(Response.Status.FORBIDDEN).entity(msg).type("text/plain").build());
	}

}
