package com.dotmarketing.osgi.tuckey;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class HelloWorldServlet extends HttpServlet {

    private static final long serialVersionUID = 42L;

    protected void doGet ( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse ) throws ServletException, IOException {

        httpServletResponse.setContentType( "text/html" );

        ServletOutputStream out = httpServletResponse.getOutputStream();

        out.println( "<html><body>" );
        out.println( "<h1>Hello Word</h1>" );

        if ( httpServletRequest.getParameter( "browser" ) != null ) {
            out.println( "<br><br><h3>from google Chrome</h3>" );
        }

        out.println( "</body></html>" );

        out.close();
    }

}