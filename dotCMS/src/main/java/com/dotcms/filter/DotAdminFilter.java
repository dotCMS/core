package com.dotcms.filter;

import java.io.IOException;



import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

@WebFilter(filterName = "DotAdminFilter", urlPatterns = {"/dotadmin", "/dotAdmin"})
public class DotAdminFilter implements Filter {

    
    final String NG_PATH="/html/ng";
    final String DOTADMIN_PATH="/dotadmin";
    
    
    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        
        String uri = request.getRequestURI();   
        if(uri.toLowerCase().startsWith(DOTADMIN_PATH)){
            req.getRequestDispatcher(NG_PATH + uri);
            return;
        }
        
        
        chain.doFilter(req, res);
        
        
        

    }

    @Override
    public void init(FilterConfig config) throws ServletException {


    }

}
