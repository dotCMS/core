package com.dotmarketing.osgi;

import com.dotmarketing.util.WebKeys;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.http.proxy.DispatcherTracker;
import org.osgi.framework.BundleContext;

/**
 * Created by Jonathan Gamba
 * Date: 9/18/12
 */
public class OSGIProxyServlet extends HttpServlet {

    public static DispatcherTracker tracker;
    public static ServletConfig servletConfig;
    public volatile static BundleContext bundleContext;
    private static AtomicBoolean isInitialized = new AtomicBoolean(Boolean.FALSE);

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

        setServletConfig(getServletConfig());

        if (System.getProperty(WebKeys.OSGI_ENABLED) != null) {

            setBundleContext(getBundleContext());

            setTracker(new DispatcherTracker(bundleContext, null, servletConfig));
            tracker.open();

            setIsInitialized(Boolean.TRUE);
        }
    }

    @Override
    protected void service ( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {

    	if(System.getProperty(WebKeys.OSGI_ENABLED)==null){
    		return;
    	}

        if (!isInitialized.get()) {
            try {
                doInit();
            } catch (Exception e) {
                throw new ServletException("Error initializing OSGIProxyServlet", e);
            }
        }

        HttpServlet dispatcher = tracker.getDispatcher();
        if ( dispatcher != null ) {
            dispatcher.service( req, res );
        } else {
            res.sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE );
        }
    }

    @Override
    public void destroy () {
        if (System.getProperty(WebKeys.OSGI_ENABLED) == null) {
            return;
        }
        if ( tracker != null ) {
            tracker.close();
            setTracker(null);
        }
        super.destroy();
    }

    private BundleContext getBundleContext () throws ServletException {
        if (System.getProperty(WebKeys.OSGI_ENABLED) == null) {
            return null;
        }
        Object context = getServletContext().getAttribute( BundleContext.class.getName() );
        if ( context instanceof BundleContext ) {
            return (BundleContext) context;
        }

        throw new ServletException( "Bundle context attribute [" + BundleContext.class.getName() + "] not set in servlet context" );
    }

    private static void setIsInitialized(Boolean initialized) {
        isInitialized.set(initialized);
    }

    private static void setServletConfig(ServletConfig config) {
        servletConfig = config;
    }

    private static void setBundleContext(BundleContext context) {
        bundleContext = context;
    }

    private static void setTracker(DispatcherTracker dispatcherTracker) {
        tracker = dispatcherTracker;
    }

}