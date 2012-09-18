package com.dotmarketing.util;

import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.osgi.OSGIProxyServlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.http.proxy.DispatcherTracker;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

import javax.servlet.ServletContextEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Created by Jonathan Gamba
 * Date: 9/17/12
 */
public class OSGIUtil {

    public static final Long BUNDLE_HTTP_BRIDGE_ID = 6L;

    private static OSGIUtil instance;

    public static OSGIUtil getInstance () {

        if ( instance == null ) {
            instance = new OSGIUtil();
        }
        return instance;
    }

    private OSGIUtil () {
    }

    private static Framework m_fwk;
    private ServletContextEvent servletContextEvent;

    public Framework initializeFramework () {

        if ( servletContextEvent != null ) {
            return initializeFramework( servletContextEvent );
        }

        throw new IllegalArgumentException( "In order to initialize the OSGI framework a ServletContextEvent must be set." );
    }

    public Framework initializeFramework ( ServletContextEvent context ) {

        servletContextEvent = context;

        Properties configProps = loadConfig();

        String felixDir = context.getServletContext().getRealPath( File.separator + "WEB-INF" + File.separator + "felix" );
        Logger.info( this, "Felix dir: " + felixDir );
        String bundleDir = felixDir + File.separator + "bundle";
        String cacheDir = felixDir + File.separator + "felix-cache";
        String autoLoadDir = felixDir + File.separator + "load";

        // we need gosh to not expecting stdin to work
        configProps.setProperty( "gosh.args", "--noi" );

        // (2) Load system properties.
        Main.loadSystemProperties();

        // (4) Copy framework properties from the system properties.
        Main.copySystemProperties( configProps );

        // (5) Use the specified auto-deploy directory over default.
        if ( bundleDir != null ) {
            configProps.setProperty( AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, bundleDir );
        }

        // (6) Use the specified bundle cache directory over default.
        if ( cacheDir != null ) {
            configProps.setProperty( org.osgi.framework.Constants.FRAMEWORK_STORAGE, cacheDir );
        }

        // Create host activator;
        List<BundleActivator> list = new ArrayList<BundleActivator>();
        HostActivator hostActivator = HostActivator.instance();
        hostActivator.setServletContext( context.getServletContext() );
        list.add( hostActivator );
        configProps.put( FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list );

        configProps.put( "felix.fileinstall.dir", autoLoadDir );

        try {
            // (8) Create an instance and initialize the framework.
            FrameworkFactory factory = getFrameworkFactory();
            m_fwk = factory.newFramework( configProps );
            m_fwk.init();

            // (9) Use the system bundle context to process the auto-deploy
            // and auto-install/auto-start properties.
            AutoProcessor.process( configProps, m_fwk.getBundleContext() );

            // (10) Start the framework.
            m_fwk.start();
            Logger.info( this, "osgi felix framework started" );
        } catch ( Exception ex ) {
            Logger.error( this, "Could not create framework: " + ex );
            throw new RuntimeException( ex );
        }

        return m_fwk;
    }

    public void stopFramework () {

        try {

            BundleContext bundleContext = HostActivator.instance().getBundleContext();

            //Closing tracker associated to the HttpServlet
            DispatcherTracker tracker = OSGIProxyServlet.tracker;
            if ( tracker != null ) {
                tracker.close();
                OSGIProxyServlet.tracker = null;
            }

            //Unregistering ToolBox services
            ServiceReference toolBoxService = getBundleContext().getServiceReference( PrimitiveToolboxManager.class.getName() );
            if ( toolBoxService != null ) {
                bundleContext.ungetService( toolBoxService );
            }

            //Unregistering Workflow services
            ServiceReference workflowService = getBundleContext().getServiceReference( WorkflowAPIOsgiService.class.getName() );
            if ( workflowService != null ) {
                bundleContext.ungetService( workflowService );
            }

            // Stop felix
            m_fwk.stop();
            // (11) Wait for framework to stop to exit the VM.
            m_fwk.waitForStop( 0 );

        } catch ( Exception e ) {
            Logger.warn( this, "exception while stopping felix!", e );
        }
    }

    public BundleContext getBundleContext () {
        return m_fwk.getBundleContext();
    }

    private static FrameworkFactory getFrameworkFactory () throws Exception {

        URL url = Main.class.getClassLoader().getResource( "META-INF/services/org.osgi.framework.launch.FrameworkFactory" );
        if ( url != null ) {
            BufferedReader br = new BufferedReader( new InputStreamReader( url.openStream() ) );
            try {
                for ( String s = br.readLine(); s != null; s = br.readLine() ) {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ( (s.length() > 0) && (s.charAt( 0 ) != '#') ) {
                        Logger.info( OSGIUtil.class, "Loading Factory " + s );
                        return (FrameworkFactory) Class.forName( s ).newInstance();
                    }
                }
            } finally {
                if ( br != null ) br.close();
            }
        }

        throw new Exception( "Could not find framework factory." );
    }

    private Properties loadConfig () {

        Properties properties = new Properties();
        Iterator<String> it = Config.getKeys();
        while ( it.hasNext() ) {
            String key = it.next();
            if ( key == null ) continue;
            if ( key.startsWith( "felix." ) ) {
                properties.put( key.substring( 6 ), Config.getStringProperty( key ) );
                Logger.info( this, "Loading property  " + key.substring( 6 ) + "=" + Config.getStringProperty( key ) );
            }
        }
        return properties;
    }

}