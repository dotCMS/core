package org.apache.velocity.tools;

import org.apache.velocity.tools.view.ToolInfo;

public class MicheleToolInfo implements ToolInfo {

	@Override
	public String getKey() {
		return "michele";
	}

	@Override
	public String getClassname() {
		return MicheleViewTool.class.getName();
	}

	@Override
	public Object getInstance(Object initData) {
		MicheleViewTool viewTool = new MicheleViewTool();
		viewTool.init(initData);
		return viewTool;
	}

};