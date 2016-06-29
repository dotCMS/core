package com.dotcms.contenttype.model.field;



import org.immutables.value.Value;

@Value.Immutable
public abstract class KeyValueField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public  String type() {
		return  FieldTypes.KEY_VALUE.name();
	}
	
	
}
