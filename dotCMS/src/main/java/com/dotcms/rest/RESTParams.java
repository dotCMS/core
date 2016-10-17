package com.dotcms.rest;

public enum RESTParams {
	RENDER ("render"),
	TYPE   ("type"),
	QUERY   ("query"),
	ORDERBY    ("orderby"),
	LIMIT ("limit"),
	OFFSET  ("offset"),
	USER  ("user"),
	PASSWORD ("password"),
	ID ("id"),
	LIVE ("live"),
	LANGUAGE ("language"),
    CALLBACK ("callback"),
	INODE ("inode");

	private final String  value;

	RESTParams(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public String toString() {
		return value;
	}

}
