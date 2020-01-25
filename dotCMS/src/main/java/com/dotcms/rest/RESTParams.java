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
	INODE ("inode"),
	RESPECT_FRONT_END_ROLES ("respectFrontendRoles"),
    //keeps the identifier of the related content to filter by (useful when retrieving parents given a child
    RELATED ("related"),
	DEPTH ("depth"),
	ALL_CATEGORIES_INFO ("allCategoriesInfo");

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
