package com.dotmarketing.osgi.viewtools;

import org.apache.velocity.tools.view.tools.ViewTool;

public class MyViewTool implements ViewTool {

	@Override
	public void init(Object initData) {
	}

	public String getHelloMessage() {
		return "Hello dotCMS World";
	}

	public String getHelloMessage(String name) {
		return "Hello " + name;
	}

}
