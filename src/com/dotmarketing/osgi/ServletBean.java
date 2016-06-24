package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;
import java.io.Serializable;
import java.util.Dictionary;

/**
 * Encapsulates the logic to add Servlet to OSGI
 * @author jsanca
 */
public class ServletBean implements Serializable {

    private final String alias;
    private final Servlet servlet;
    private final Dictionary initParams;
    private final HttpContext httpContext;


    public ServletBean(final String alias,
                       final Servlet servlet,
                       final Dictionary initParams,
                       final HttpContext httpContext) {

        this.alias = alias;
        this.servlet = servlet;
        this.initParams = initParams;
        this.httpContext = httpContext;
    }

    public String getAlias() {
        return alias;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public Dictionary getInitParams() {
        return initParams;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }
} // E:O:F:ServletBean.
