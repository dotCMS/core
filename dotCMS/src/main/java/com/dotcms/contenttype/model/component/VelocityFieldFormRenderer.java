package com.dotcms.contenttype.model.component;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import org.apache.velocity.context.Context;

public class VelocityFieldFormRenderer implements FieldFormRenderer {

  private final Context context;
  private final String storedValue;

  public VelocityFieldFormRenderer(Context context, String enteredValue) {
    this.context = context;
    this.storedValue = enteredValue;
  }

  @Override
  public String render() {
    return VelocityUtil.getInstance().parseVelocity(storedValue, context);
  }
}
