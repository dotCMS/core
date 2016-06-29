package com.dotcms.contenttype.model.type;

import org.immutables.value.Value;

@Value.Immutable
public abstract class SimpleContentType extends ContentType{



	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentTypes baseType() {
		return  BaseContentTypes.CONTENT;
	}


}
