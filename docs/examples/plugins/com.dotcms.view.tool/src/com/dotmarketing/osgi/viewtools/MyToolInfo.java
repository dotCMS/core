package com.dotmarketing.osgi.viewtools;

import org.apache.velocity.tools.view.ToolInfo;

public class MyToolInfo implements ToolInfo {

	@Override
	public String getKey() {
		return "michele";
	}

	@Override
	public String getClassname() {
		return MyViewTool.class.getName();
	}

	@Override
	public Object getInstance(Object initData) {
		MyViewTool viewTool = new MyViewTool();
		viewTool.init(initData);
		return viewTool;
	}

};