package com.liferay.portal.servlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.util.Config;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * If the request is a Angular routing request then it response with a 304 Http code.
 */
public class AngularRoutingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        String path = ((HttpServletRequest) servletRequest).getServletPath();
        String angularRoutingUrlRex = Config.getStringProperty("ANGULAR_ROUTING_URL_REX", "^\\/html\\/ng\\/(public|dotCMS)\\/.*");

        if (path.matches(angularRoutingUrlRex)) {

            ServletContext context = servletRequest.getServletContext();
            String indexURL = Config.getStringProperty("ANGULAR_INDEX_URL", "/html/ng/index.html");
            String indexRealPath = context.getRealPath(indexURL);

            IOUtils.copy(new FileInputStream(indexRealPath), servletResponse.getOutputStream());
        }else{
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {

    }
}
