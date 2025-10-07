package com.dotcms.csspreproc;

import com.dotmarketing.util.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter intercepts requests for .scss files with the dotsass=true query parameter
 * and forwards them to the CSSPreProcessServlet for SASS compilation.
 * 
 * This is needed because direct .scss requests might be handled by another servlet before
 * reaching the CSSPreProcessServlet, even with the proper servlet mapping.
 */
public class ScssQueryParamFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String uri = httpRequest.getRequestURI();
        String dotsassParam = request.getParameter("dotsass");
        boolean isDotsassParam = "true".equalsIgnoreCase(dotsassParam);
        
        // Only intercept .scss files with dotsass=true parameter
        if (uri.toLowerCase().endsWith(".scss") && isDotsassParam) {
            Logger.debug(this, "ScssQueryParamFilter forwarding request with dotsass=true parameter to CSSPreProcessServlet: " + uri);
            
            // Create a new request attribute to pass the original URI to the CSSPreProcessServlet
            httpRequest.setAttribute("originalScssURI", uri);
            
            // Forward to the CSSPreProcessServlet using the /DOTSASS path
            RequestDispatcher dispatcher = httpRequest.getRequestDispatcher("/DOTSASS");
            dispatcher.forward(httpRequest, httpResponse);
        } else {
            // Continue the filter chain for other requests
            chain.doFilter(request, response);
        }
    }
    
    @Override
    public void destroy() {
        // No cleanup needed
    }
}
