package com.dotcms.rendering.js;

import org.apache.velocity.tools.view.context.ViewContext;

/**
 * Set the velocity context into the {@link JsViewTool}
 * @author jsanca
 */
public interface JsViewContextAware {

    void setViewContext(final ViewContext viewContext);
}
