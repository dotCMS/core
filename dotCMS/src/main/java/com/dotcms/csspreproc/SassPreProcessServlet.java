package com.dotcms.csspreproc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SassPreProcessServlet extends HttpServlet {
    
    private static final long serialVersionUID = -3315180323197314439L;

    private static final String DOTSASS_PREFIX = "/DOTSASS";
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
   
        // forward to the CSSPreProcessorServlet
        req.getRequestDispatcher(DOTSASS_PREFIX + req.getRequestURI()).forward(req, resp);     
    }

}
