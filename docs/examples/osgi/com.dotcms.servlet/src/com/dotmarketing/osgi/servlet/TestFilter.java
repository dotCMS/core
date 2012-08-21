package com.dotmarketing.osgi.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class TestFilter implements Filter {

    private final String name;

    public TestFilter ( String name ) {
        this.name = name;
    }

    public void init ( FilterConfig config ) throws ServletException {

        doLog( "Init with config [" + config + "]" );
    }

    public void doFilter ( ServletRequest req, ServletResponse res, FilterChain chain ) throws IOException, ServletException {

        if ( req instanceof HttpServletRequest ) {
            doLog( "Filter request [" + ((HttpServletRequest) req).getRequestURI() + "]" );
        } else {
            doLog( "Filter request [" + req + "]" );
        }

        chain.doFilter( req, res );
    }

    public void destroy () {
        doLog( "Destroyed filter" );
    }

    private void doLog ( String message ) {
        System.out.println( "## [" + this.name + "] " + message );
    }

}