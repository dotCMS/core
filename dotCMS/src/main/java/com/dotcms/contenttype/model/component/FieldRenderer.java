package com.dotcms.contenttype.model.component;

public interface FieldRenderer {
	default String render(){
		return null;
	}
}
