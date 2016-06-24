package com.dotcms.contenttype.model.type;

import org.immutables.value.Value;

import com.dotcms.contenttype.model.BaseContentType;

@Value.Immutable
public abstract class Persona extends BaseContent{



	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentType baseContentType() {
		return  BaseContentType.PERSONA;
	}


}
