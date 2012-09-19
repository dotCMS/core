package com.dotmarketing.osgi;

import org.apache.felix.http.proxy.DispatcherTracker;
import org.osgi.framework.BundleContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Jonathan Gamba
 * Date: 9/18/12
 */
public class OSGIProxyServlet extends HttpServlet {

    public static DispatcherTracker tracker;
    public static ServletConfig servletConfig;

    @Override
    public void init ( ServletConfig config ) throws ServletException {

        super.init( config );

        try {
            doInit();
        } catch ( ServletException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new ServletException( e );
        }
    }

    private void doInit () throws Exception {

        tracker = new DispatcherTracker( getBundleContext(), null, getServletConfig() );
        tracker.open();

        servletConfig = getServletConfig();
    }

    @Override
    protected void service ( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {

        HttpServlet dispatcher = tracker.getDispatcher();
        if ( dispatcher != null ) {
            dispatcher.service( req, res );
        } else {
            res.sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE );
        }
    }

    @Override
    public void destroy () {

        if ( tracker != null ) {
            tracker.close();
            tracker = null;
        }
        super.destroy();
    }

    private BundleContext getBundleContext () throws ServletException {

        Object context = getServletContext().getAttribute( BundleContext.class.getName() );
        if ( context instanceof BundleContext ) {
            return (BundleContext) context;
        }

        throw new ServletException( "Bundle context attribute [" + BundleContext.class.getName() + "] not set in servlet context" );
    }

}