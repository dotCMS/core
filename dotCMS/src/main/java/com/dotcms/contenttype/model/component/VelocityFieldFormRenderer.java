package com.dotcms.contenttype.model.component;

import org.apache.velocity.context.Context;

import com.dotmarketing.util.VelocityUtil;

public class VelocityFieldFormRenderer implements FieldFormRenderer{

	private final Context context;
	private final String storedValue;
	public VelocityFieldFormRenderer(Context context,String enteredValue ){
		this.context = context;
		this.storedValue=enteredValue;
	}
	@Override
	public String render(){
		return new VelocityUtil().parseVelocity(storedValue, context);
	}
}
