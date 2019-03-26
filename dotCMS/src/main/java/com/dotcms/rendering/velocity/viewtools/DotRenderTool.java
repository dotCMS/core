package com.dotcms.rendering.velocity.viewtools;

import com.dotmarketing.util.VelocityUtil;
import org.apache.velocity.tools.view.tools.ViewRenderTool;

public class DotRenderTool extends ViewRenderTool {

  public DotRenderTool() {
    setVelocityEngine(VelocityUtil.getEngine());
  }
}
