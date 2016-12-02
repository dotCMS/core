package com.dotcms.contenttype.model.type;

import java.io.Serializable;

import org.immutables.value.Value;

@Value.Immutable(singleton = true)
public abstract class DbStorageType implements StorageType, Serializable {

	private static final long serialVersionUID = 1L;

}
