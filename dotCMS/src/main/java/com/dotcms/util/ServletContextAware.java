package com.dotcms.util;

import javax.servlet.ServletContext;

public interface ServletContextAware {

    /**
     * Set the context
     * @param servletContext {@link javax.servlet.ServletContext}
     */
    void setServletContext(ServletContext servletContext);
}
