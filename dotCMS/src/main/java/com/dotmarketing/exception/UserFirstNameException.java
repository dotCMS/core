package com.dotmarketing.exception;

public class UserFirstNameException extends DotDataException {

	private static final long serialVersionUID = 1L;
	
	public UserFirstNameException(Exception e) {
		super("", e);
	}
}