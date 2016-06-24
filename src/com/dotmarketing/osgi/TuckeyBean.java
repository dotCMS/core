package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.service.http.HttpContext;
import com.dotcms.repackage.org.tuckey.web.filters.urlrewrite.Rule;

import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.List;

/**
 * Encapsulates Tuckey Config
 * @author jsanca
 */
public class TuckeyBean extends ServletBean {

    private final List<Rule> rules;

    public TuckeyBean(String alias, Servlet servlet,
                      Dictionary initParams, HttpContext httpContext,
                      List<Rule> rules) {

        super(alias, servlet, initParams, httpContext);

        this.rules = rules;
    }

    public List<Rule> getRules() {
        return rules;
    }
} // E:O:F:TuckeyBean.
