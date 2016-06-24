package com.dotcms.contenttype.model;

import java.io.Serializable;
import java.util.Date;

import org.immutables.value.Value;

@Value.Immutable
public abstract class FieldVariable implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	abstract String id();

	abstract String fieldId();

	abstract String name();

	abstract String key();

	abstract String value();

	abstract String lastModifierId();

	abstract Date lastModDate();

}
