package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.osgi.OSGIProxyServlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;

import org.apache.commons.io.FileUtils;
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
import javax.websocket.Session;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Jonathan Gamba
 * Date: 9/17/12
 */
public class OSGIUtil {

    private static final String FELIX_BASE_DIR = "felix.base.dir";
    private static final String FELIX_FILEINSTALL_DIR = "felix.fileinstall.dir";
    private static final String FELIX_UNDEPLOYED_DIR = "felix.undeployed.dir";

    public static final String BUNDLE_HTTP_BRIDGE_SYMBOLIC_NAME = "org.apache.felix.http.bundle";
    private static final String PROPERTY_OSGI_PACKAGES_EXTRA = "org.osgi.framework.system.packages.extra";
    private String FELIX_EXTRA_PACKAGES_FILE_GENERATED;
    public String FELIX_EXTRA_PACKAGES_FILE;

    private static OSGIUtil instance;

    public static OSGIUtil getInstance () {

        if ( instance == null ) {
            instance = new OSGIUtil();
        }
        return instance;
    }

    private OSGIUtil () {


    }

    private static Framework felixFramework;
    private ServletContextEvent servletContextEvent;

    public Framework initializeFramework () {

        if ( servletContextEvent != null ) {
            return initializeFramework( servletContextEvent );
        }

        throw new IllegalArgumentException( "In order to initialize the OSGI framework a ServletContextEvent must be set." );
    }

    public Framework initializeFramework ( ServletContextEvent context ) {

        servletContextEvent = context;

        // load all properties and set base directory
        Properties configProps = loadConfig();

        // fetch the 'felix.base.dir' property and check if exists. On the props file the prop needs to be set as felix.felix.base.dir
        String felixDirectory = configProps.getProperty(FELIX_BASE_DIR);
        if (UtilMethods.isSet(felixDirectory)) {
            // verify folder exists and if not create it
            if (!verifyOrCreateFelixFolder(felixDirectory)) {
                // override the property to default
                felixDirectory = context.getServletContext().getRealPath("/WEB-INF/felix");
            }
        } else {
            // override the property to default
            felixDirectory = context.getServletContext().getRealPath("/WEB-INF/felix");
        }

        // Set the base dir property
        configProps.put(FELIX_BASE_DIR, felixDirectory);
        Logger.info(this, "Felix dir: " + felixDirectory);

        // Set all required paths
        FELIX_EXTRA_PACKAGES_FILE = felixDirectory + File.separator + "osgi-extra.conf";
        FELIX_EXTRA_PACKAGES_FILE_GENERATED = felixDirectory + File.separator + "osgi-extra-generated.conf";

        // All of the following paths will always be part of dotCMS core conf (by default)
        String bundleDir = felixDirectory + File.separator + "bundle";
        String cacheDir = felixDirectory + File.separator + "felix-cache";
        String autoLoadDir = felixDirectory + File.separator + "load";
        String undeployedDir = felixDirectory + File.separator + "undeployed";

        // Verify bundle dir is created and set the prop
        verifyOrCreateFelixFolder(bundleDir);
        configProps.setProperty(AutoProcessor.AUTO_DEPLOY_DIR_PROPERTY, bundleDir);

        // Verify cache dir and set the prop
        verifyOrCreateFelixFolder(cacheDir);
        configProps.setProperty(org.osgi.framework.Constants.FRAMEWORK_STORAGE, cacheDir);

        // Verify load dir and set the prop
        verifyOrCreateFelixFolder(autoLoadDir);
        configProps.put(FELIX_FILEINSTALL_DIR, autoLoadDir);

        // Verify undeploy dir and set the prop
        verifyOrCreateFelixFolder(undeployedDir);
        configProps.put(FELIX_UNDEPLOYED_DIR, undeployedDir);

        // Verify the bundles are in the right place
        verifyBundles(bundleDir, context);

        // Create host activator;
        List<BundleActivator> list = new ArrayList<BundleActivator>();
        HostActivator hostActivator = HostActivator.instance();
        hostActivator.setServletContext(context.getServletContext());
        list.add(hostActivator);
        configProps.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);

        // Set all OSGI Packages
        String extraPackages;
        try {
            extraPackages = getExtraOSGIPackages();
        } catch ( IOException e ) {
            Logger.error( this, "Error loading the OSGI framework properties: " + e );
            throw new RuntimeException( e );
        }

        // Setting the OSGI extra packages property
        configProps.setProperty( PROPERTY_OSGI_PACKAGES_EXTRA, extraPackages );

        // we need gosh to not expecting stdin to work
        configProps.setProperty( "gosh.args", "--noi" );

        // Load system properties.
        Main.loadSystemProperties();

        // Copy framework properties from the system properties.
        Main.copySystemProperties( propertiesToMap( configProps ) );

        try {
            // Create an instance and initialize the framework.
            FrameworkFactory factory = getFrameworkFactory();
            felixFramework = factory.newFramework( configProps );
            felixFramework.init();

            // Use the system bundle context to process the auto-deploy
            // and auto-install/auto-start properties.
            AutoProcessor.process( configProps, felixFramework.getBundleContext() );

            // Start the framework.
            felixFramework.start();
            Logger.info( this, "osgi felix framework started" );
        } catch ( Exception ex ) {
            Logger.error( this, "Could not create framework: " + ex );
            throw new RuntimeException( ex );
        }

        return felixFramework;
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

            if (null != felixFramework) {
                //Unregistering ToolBox services
                ServiceReference toolBoxService = getBundleContext().getServiceReference(PrimitiveToolboxManager.class.getName());
                if (toolBoxService != null) {
                    bundleContext.ungetService(toolBoxService);
                }

                //Unregistering Workflow services
                ServiceReference workflowService = getBundleContext().getServiceReference(WorkflowAPIOsgiService.class.getName());
                if (workflowService != null) {
                    bundleContext.ungetService(workflowService);
                }

                // Stop felix
                felixFramework.stop();

                // Wait for framework to stop to exit the VM.
                felixFramework.waitForStop(0);
            }

        } catch ( Exception e ) {
            Logger.warn( this, "exception while stopping felix!", e );
        }
    }

    public BundleContext getBundleContext () {
        return felixFramework.getBundleContext();
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
            for ( final String name : list ) {
            	if(name.startsWith("/")) continue;
            	if(name.contains(":")) continue;

            	if ( File.separator.equals( "/" ) ) {
                    bob.append( name.replace( File.separator, "." ) + "," + "\n" );
                } else {
                    //Zip entries have '/' as separator on all platforms
                    bob.append( (name.replace( File.separator, "." ).replace( "/", "." )) + "," + "\n" );
                }
            }

            bob.append( "org.osgi.framework," +
                    "org.osgi.framework.wiring," +
                    "org.osgi.service.packageadmin," +
                    "org.osgi.framework.startlevel," +
                    "org.osgi.service.startlevel," +
                    "org.osgi.service.url," +
                    "org.osgi.util.tracker," +
                    "org.osgi.service.http," +
                    "javax.inject.Qualifier," +
                    "javax.servlet.resources," +
                    "javax.servlet;javax.servlet.http;version=3.1.0" );

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

    /**
     * Fetches the Felix Path based on the input param property value.
     * If property is not found, then the felix path will be set manually, specified by manual default path param.
     *
     * @param felixDirProperty  Property to be fetched from the bundle context.
     * @param manualDefaultPath Property to set manual path by default, in case the property is not found
     * @return String
     */
    private String getFelixPath(String felixDirProperty, String manualDefaultPath) {
        String felixPath;

        try {
            felixPath = getBundleContext().getProperty(felixDirProperty);
        } catch (Exception ex) {
            Logger.error(this, String.format("Unable to find the felix '%s' folder path from OSGI bundle context. Trying to fetch it from Config.CONTEXT as real path from '/WEB-INF/felix/%s'", manualDefaultPath, manualDefaultPath), ex);

            try {
                felixPath = Config.CONTEXT.getRealPath("/WEB-INF/felix/" + manualDefaultPath);
            } catch (Exception ex2) {
                Logger.error(this, String.format("Unable to find the felix '%s' folder real path from Config.CONTEXT. Setting it manually to '/WEB-INF/felix/%s'", manualDefaultPath, manualDefaultPath), ex2);
                felixPath = "/WEB-INF/felix/" + manualDefaultPath;
            }
        }

        if (felixPath == null) {
            Logger.error(this, String.format("Path '%s' was not successfully set. Setting it manually to '/WEB-INF/felix/%s'", manualDefaultPath, manualDefaultPath));
            felixPath = "/WEB-INF/felix/" + manualDefaultPath;
        }

        createFolder(felixPath);

        return felixPath;
    }

    /**
     * Verifies the folder exists.
     * If it does not exists then tries to create it
     *
     * @param path The path to verify
     * @return boolean true when path exists or it was created successfully
     */
    private boolean verifyOrCreateFelixFolder(String path) {
        return new File(path).exists() || createFolder(path);
    }

    /**
     * Create the path if it does not exist. Required for felix install and undeploy folder
     *
     * @param path The path to create
     * @return boolean
     */
    private boolean createFolder(String path) {
        boolean created = false;
        File directory = new File(path);
        if (!directory.exists()) {
            Logger.debug(this, String.format("Felix directory %s does not exist. Trying to create it...", path));
            created = directory.mkdirs();
            if (!created) {
                Logger.error(this, String.format("Unable to create Felix directory: %s", path));
            }
        }
        return created;
    }

    /**
     * Fetches the Felix Deploy path
     *
     * @return String
     */
    public String getFelixDeployPath() {
        return getFelixPath(FELIX_FILEINSTALL_DIR, "load");
    }

    /**
     * Fetches the Felix Undeploy path
     *
     * @return String
     */
    public String getFelixUndeployPath() {
        return getFelixPath(FELIX_UNDEPLOYED_DIR, "undeployed");
    }

    /**
     * Verify the bundles are in the right place if the default path has been overwritten
     * If bundle path is different to the default one then move all bundles to the new directory and get rid of the default one
     *
     * @param bundlePath The bundle path to move items
     * @param context The servlet context
     */
    private void verifyBundles(String bundlePath, ServletContextEvent context) {
        String baseDirectory = context.getServletContext().getRealPath("/WEB-INF");
        String defaultFelixPath = baseDirectory + File.separator + "felix";
        String defaultBundlePath = defaultFelixPath + File.separator + "bundle";

        if (UtilMethods.isSet(bundlePath)) {
            if (!bundlePath.trim().equals(defaultBundlePath)) {
                File bundleDirectory = new File(bundlePath);
                File defaultBundleDirectory = new File(defaultBundlePath);

                if (defaultBundleDirectory.exists() && bundleDirectory.exists()) {
                    try {
                        // copy all bundles
                        FileUtils.copyDirectory(defaultBundleDirectory, bundleDirectory);

                        // delete felix default folder since we don't need it
                        File defaultFelixDirectory = new File(defaultFelixPath);
                        if (defaultFelixDirectory.exists()) {
                            FileUtils.deleteDirectory(defaultFelixDirectory);
                        }
                    } catch (IOException ioex) {
                        String errorMessage = String.format("There was a problem moving felix bundles from '%s' to '%s'", defaultBundlePath, bundlePath);
                        Logger.error(this, errorMessage);
                        throw new RuntimeException(errorMessage, ioex);
                    }
                }
            }
        }

    }
}