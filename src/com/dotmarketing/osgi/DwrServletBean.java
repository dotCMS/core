package com.dotmarketing.osgi;

import com.dotcms.repackage.org.directwebremoting.servlet.DwrServlet;
import com.dotcms.repackage.org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;
import java.util.Dictionary;

/**
 * Encapsulates DwrServletBean
 * @author jsanca
 */
public class DwrServletBean extends ServletBean {

    private final String excludePath;

    public DwrServletBean(final String alias,
                          final DwrServlet servlet,
                          final Dictionary initParams,
                          final HttpContext httpContext,
                          final String excludePath) {

        super(alias, servlet, initParams, httpContext);
        this.excludePath = excludePath;
    }

    public String getExcludePath() {
        return excludePath;
    }
}
