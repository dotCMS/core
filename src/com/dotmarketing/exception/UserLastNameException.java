package com.dotmarketing.exception;

public class UserLastNameException extends DotDataException {

	private static final long serialVersionUID = 1L;

	public UserLastNameException(Exception e) {
		super("", e);
	}

}