package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.service.http.HttpContext;

import javax.servlet.Filter;
import java.io.Serializable;
import java.util.Dictionary;

/**
 * Encapsulates the logic to add Filter to OSGI
 *
 * @author jsanca
 */
public class FilterBean implements Serializable {

    private final String pattern;
    private final Filter filter;
    private final Dictionary initParams;
    private final int ranking;
    private final HttpContext httpContext;
    private final String excludePath;

    public FilterBean(String pattern, Filter filter,
                      Dictionary initParams, int ranking,
                      HttpContext httpContext,
                      String removeExcludePath) {

        this.pattern = pattern;
        this.filter = filter;
        this.initParams = initParams;
        this.ranking = ranking;
        this.httpContext = httpContext;
        this.excludePath = removeExcludePath;
    }

    public String getPattern() {
        return pattern;
    }

    public Filter getFilter() {
        return filter;
    }

    public Dictionary getInitParams() {
        return initParams;
    }

    public int getRanking() {
        return ranking;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }

    public String getExcludePath() {
        return excludePath;
    }
} // E:O:F:ServletBean.
