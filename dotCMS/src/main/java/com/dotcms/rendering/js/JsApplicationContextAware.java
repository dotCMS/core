package com.dotcms.rendering.js;

import javax.servlet.ServletContext;

/**
 * Implement this class to get the response into the {@link javax.servlet.ServletContext}
 * @author jsanca
 */
public interface JsApplicationContextAware {

    void setContext(final ServletContext context);
}
