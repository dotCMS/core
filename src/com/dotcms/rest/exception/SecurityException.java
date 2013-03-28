package com.dotcms.rest.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.dotmarketing.util.UtilMethods;

public class SecurityException extends WebApplicationException {

	/**
	 *
	 */
	private static final long serialVersionUID = -4222030277463217807L;

	public SecurityException(String msg, Response.Status status) {
		super(Response.status(status).entity(msg).type("text/plain").build());
	}

}
