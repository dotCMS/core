package com.dotmarketing.osgi.viewtools;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class MyToolInfo extends ServletToolInfo {

	@Override
	public String getKey() {
		return "osgitool";
	}

	@Override
	public String getClassname() {
		return MyViewTool.class.getName();
	}

	@Override
	public Object getInstance(Object initData) {
		MyViewTool viewTool = new MyViewTool();
		viewTool.init(initData);
		setScope(ViewContext.APPLICATION);
		return viewTool;
	}
	
};