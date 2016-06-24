package com.dotcms.contenttype.model;

import org.immutables.value.Value;

@Value.Immutable
public interface SimpleStructureURLMap {

	String inode();

	String urlMapPattern();

}
