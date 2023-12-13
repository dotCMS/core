package com.dotcms.rendering.js;

import javax.servlet.ServletContext;

/**
 * Implement this class to get the context ({@link javax.servlet.ServletContext}) into the class.
 * @author jsanca
 */
public interface JsApplicationContextAware {

    void setContext(final ServletContext context);
}
