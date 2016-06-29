package com.dotcms.contenttype.model.type;

import org.immutables.value.Value;

@Value.Immutable
public abstract class FormContentType extends ContentType{



	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentTypes baseType() {
		return  BaseContentTypes.FORM;
	}
	
	@Value.Default
	public boolean multilingualable(){
		return false;
	}

}
