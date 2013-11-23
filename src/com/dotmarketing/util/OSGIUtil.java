package com.dotmarketing.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContextEvent;

import org.apache.commons.io.IOUtils;
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

import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.osgi.OSGIProxyServlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;

/**
 * Created by Jonathan Gamba
 * Date: 9/17/12
 */
public class OSGIUtil {

    private static final String FELIX_FILEINSTALL_DIR = "felix.fileinstall.dir";
    public static final String BUNDLE_HTTP_BRIDGE_SYMBOLIC_NAME = "org.apache.felix.http.bundle";
    private static final String PROPERTY_OSGI_PACKAGES_EXTRA = "org.osgi.framework.system.packages.extra";
    public String FELIX_EXTRA_PACKAGES_FILE;
    public String FELIX_EXTRA_PACKAGES_FILE_GENERATED;

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

        String felixDirectory = context.getServletContext().getRealPath( File.separator + "WEB-INF" + File.separator + "felix" );
        FELIX_EXTRA_PACKAGES_FILE = felixDirectory + File.separator + "osgi-extra.conf";
        FELIX_EXTRA_PACKAGES_FILE_GENERATED = felixDirectory + File.separator + "osgi-extra-generated.conf";

        Logger.info( this, "Felix dir: " + felixDirectory );
        String bundleDir = felixDirectory + File.separator + "bundle";
        String cacheDir = felixDirectory + File.separator + "felix-cache";
        String autoLoadDir = felixDirectory + File.separator + "load";

        Properties configProps;
        String extraPackages;
        try {
            configProps = loadConfig();
            extraPackages = getExtraOSGIPackages();
        } catch ( IOException e ) {
            Logger.error( this, "Error loading the OSGI framework properties: " + e );
            throw new RuntimeException( e );
        }

        //Setting the OSGI extra packages property
        configProps.setProperty( PROPERTY_OSGI_PACKAGES_EXTRA, extraPackages );
        // we need gosh to not expecting stdin to work
        configProps.setProperty( "gosh.args", "--noi" );

        // (2) Load system properties.
        Main.loadSystemProperties();

        // (4) Copy framework properties from the system properties.
        Main.copySystemProperties( propertiesToMap( configProps ) );

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
        
	String felixFileInstallDirPath = configProps.getProperty(FELIX_FILEINSTALL_DIR);
	if (felixFileInstallDirPath == null || !new File(felixFileInstallDirPath).isDirectory()) {
	    configProps.put(FELIX_FILEINSTALL_DIR, autoLoadDir);
	}

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

    /**
     * Loads all the OSGI configured properties
     *
     * @return
     */
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

    /**
     * Returns the packages inside the <strong>osgi-extra.conf</strong> file, those packages are the value
     * for the OSGI configuration property <strong>org.osgi.framework.system.packages.extra</strong>.
     * <br/><br/>
     * The property <strong>org.osgi.framework.system.packages.extra</strong> is use to set the list of packages the
     * dotCMS context in going to expose to the OSGI context.
     *
     * @return
     * @throws IOException
     */
    public String getExtraOSGIPackages () throws IOException {

        String extraPackages;
        
        File f = new File(FELIX_EXTRA_PACKAGES_FILE);
        if(!f.exists()){
        	StringBuilder bob = new StringBuilder();
        	final Collection<String> list = ResourceCollectorUtil.getResources();
        	for(final String name : list){
        		bob.append(name.replace(File.separator, ".") + "," + "\n");
        	}
        	bob.append("org.osgi.framework," +
        			"org.osgi.service.packageadmin," +
        			"org.osgi.service.startlevel," +
        			"org.osgi.service.url," +
        			"org.osgi.util.tracker," +
        			"org.osgi.service.http," +
        			"javax.inject.Qualifier," +
        			"javax.servlet.resources," +
        			"javax.servlet;javax.servlet.http");
        	
        	BufferedWriter writer = null;
        	try {
        	    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream( FELIX_EXTRA_PACKAGES_FILE_GENERATED ), "utf-8"));
        	    writer.write(bob.toString());
        	} catch (IOException ex) {
        		Logger.error(this, ex.getMessage(), ex);
        	} finally {
        	   try {writer.close();} catch (Exception ex) {Logger.error(this, ex.getMessage(), ex);}
        	}
        }
        
        //Reading the file with the extra packages
        FileInputStream inputStream = null;
        if(f.exists()){
        	inputStream = new FileInputStream( FELIX_EXTRA_PACKAGES_FILE );
        }else{
        	inputStream = new FileInputStream( FELIX_EXTRA_PACKAGES_FILE_GENERATED );
        }
        try {
            extraPackages = IOUtils.toString( inputStream );
        } finally {
            inputStream.close();
        }

        //Clean up the properties, it is better to keep it simple and in a standard format
        extraPackages = extraPackages.replaceAll( "\\\n", "" );
        extraPackages = extraPackages.replaceAll( "\\\r", "" );
        extraPackages = extraPackages.replaceAll( "\\\\", "" );

        return extraPackages;
    }

    /**
     * Transform a given Properties object into a Map
     *
     * @param props
     * @return
     */
    private Map<String, String> propertiesToMap ( Properties props ) {

        HashMap<String, String> propertiesMap = new HashMap<String, String>();

        Enumeration<Object> e = props.keys();
        while ( e.hasMoreElements() ) {
            String s = (String) e.nextElement();
            propertiesMap.put( s, props.getProperty( s ) );
        }

        return propertiesMap;
    }

}