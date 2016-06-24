package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.service.http.HttpContext;

import java.io.Serializable;
import java.util.Dictionary;

/**
 * Spring Servlet bean
 * @author jsanca
 */
public class SpringServletBean implements Serializable {

    private final String alias;
    private final String configLocation;
    private final Dictionary initParams;
    private final HttpContext httpContext;
    private final String excludePath;

    public SpringServletBean(final String alias,
                             final String configLocation,
                             final Dictionary initParams,
                             final HttpContext httpContext,
                             final String excludePath) {
        this.alias = alias;
        this.configLocation = configLocation;
        this.initParams = initParams;
        this.httpContext = httpContext;
        this.excludePath = excludePath;
    }

    public String getAlias() {
        return alias;
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public Dictionary getInitParams() {
        return initParams;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }

    public String getExcludePath() {
        return excludePath;
    }

    @Override
    public String toString() {
        return "SpringServletBean{" +
                "alias='" + alias + '\'' +
                ", configLocation='" + configLocation + '\'' +
                ", initParams=" + initParams +
                ", httpContext=" + httpContext +
                ", excludePath='" + excludePath + '\'' +
                '}';
    }
} // E:O:F:SpringServletBean.
