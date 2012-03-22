package com.dotmarketing.viewtools;

import org.apache.velocity.tools.view.tools.ViewRenderTool;

import com.dotmarketing.util.VelocityUtil;

public class DotRenderTool extends ViewRenderTool {
	
	public DotRenderTool() {
		setVelocityEngine(VelocityUtil.getEngine());
	}
}