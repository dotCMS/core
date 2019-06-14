package com.dotcms.rest.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


public class SecurityException extends WebApplicationException {

	/**
	 *
	 */
	private static final long serialVersionUID = -4222030277463217807L;

	public SecurityException(String msg) {
		this(msg, Response.Status.FORBIDDEN);
	}

	public SecurityException(String msg, Response.Status status) {
		super(msg, Response.status(status).entity(msg).type("text/plain").build());
	}

	public SecurityException(String message, Throwable cause, Status status)
			throws IllegalArgumentException {
		super(message, cause, Response.status(status).entity(message).type("text/plain").build());
	}
}
