package com.liferay.portal.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

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

        if (!path.toLowerCase().endsWith("js")
                && !path.toLowerCase().endsWith("css")
                && !path.toLowerCase().endsWith("html")
                && !path.toLowerCase().endsWith("js.map")
                && !path.toLowerCase().endsWith("ttf")
                && !path.toLowerCase().endsWith("woff2")
                && !path.toLowerCase().endsWith("woff")) {

            ServletContext context = servletRequest.getServletContext();
            String indexRealPath = context.getRealPath("/html/ng/index.html");

            try( OutputStream out = servletResponse.getOutputStream();
                 FileInputStream in = new FileInputStream(indexRealPath)){

                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                in.close();
                out.flush();
            }
        }else{
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {

    }
}
