package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.osgi.OSGIProxyServlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import javax.servlet.ServletContextEvent;
import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.http.proxy.DispatcherTracker;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

/**
 * Created by Jonathan Gamba
 * Date: 9/17/12
 */
public class OSGIUtil {

    private static final String FELIX_BASE_DIR = "felix.base.dir";
    private static final String FELIX_FILEINSTALL_DIR = "felix.fileinstall.dir";
    private static final String FELIX_UNDEPLOYED_DIR = "felix.undeployed.dir";
    private static final String FELIX_FRAMEWORK_STORAGE = org.osgi.framework.Constants.FRAMEWORK_STORAGE;
    private static final String AUTO_DEPLOY_DIR_PROPERTY =  AutoProcessor.AUTO_DEPLOY_DIR_PROPERTY;

    /**
     * Felix directory list
     */
    private static final String[] FELIX_DIRECTORIES = new String[] {
        FELIX_BASE_DIR, FELIX_FILEINSTALL_DIR, FELIX_UNDEPLOYED_DIR, AUTO_DEPLOY_DIR_PROPERTY, FELIX_FRAMEWORK_STORAGE
    };

    public static final String BUNDLE_HTTP_BRIDGE_SYMBOLIC_NAME = "org.apache.felix.http.bundle";
    private static final String PROPERTY_OSGI_PACKAGES_EXTRA = "org.osgi.framework.system.packages.extra";
    private String FELIX_EXTRA_PACKAGES_FILE_GENERATED;
    public String FELIX_EXTRA_PACKAGES_FILE;

    private static OSGIUtil instance;

    public static OSGIUtil getInstance() {
        if (instance == null) {
            instance = new OSGIUtil();
        }
        return instance;
    }

    private OSGIUtil () {
    }

    private static Framework felixFramework;
    private ServletContextEvent servletContextEvent;

    /**
     * Initializes the OSGi framework
     *
     * @return Framework
     */
    public Framework initializeFramework() {
        if (servletContextEvent != null) {
            return initializeFramework(servletContextEvent);
        }

        throw new IllegalArgumentException("In order to initialize the OSGI framework a ServletContextEvent must be set.");
    }

    /**
     * Loads the default properties
     *
     * @return Properties
     */
    private Properties defaultProperties() {
        Properties felixProps = new Properties();
        String felixDirectory = new File(Config.getStringProperty(FELIX_BASE_DIR, Config.CONTEXT.getRealPath("/WEB-INF") + File.separator + "felix")).getAbsolutePath();

        Logger.info(this, "Felix base dir: " + felixDirectory);

        felixProps.put(FELIX_BASE_DIR, felixDirectory);
        felixProps.put(AUTO_DEPLOY_DIR_PROPERTY, felixDirectory + File.separator + "bundle");
        felixProps.put(FELIX_FRAMEWORK_STORAGE, felixDirectory + File.separator + "felix-cache");
        felixProps.put(FELIX_FILEINSTALL_DIR, felixDirectory + File.separator + "load");
        felixProps.put(FELIX_UNDEPLOYED_DIR, felixDirectory + File.separator + "undeployed");

        felixProps.put("felix.auto.deploy.action", "install,start");
        felixProps.put("felix.fileinstall.start.level", "1");
        felixProps.put("felix.fileinstall.log.level", "3");
        felixProps.put("org.osgi.framework.startlevel.beginning", "2");
        felixProps.put("org.osgi.framework.storage.clean", "onFirstInit");
        felixProps.put("felix.log.level", "4");
        felixProps.put("felix.fileinstall.disableNio2", "true");
        felixProps.put("gosh.args", "--noi");

        // Create host activator;
        HostActivator hostActivator = HostActivator.instance();
        hostActivator.setServletContext(servletContextEvent.getServletContext());
        felixProps.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, ImmutableList.of(hostActivator));

        return felixProps;
    }

    /**
     * Initializes the framework OSGi using the servlet context
     *
     * @param context The servlet context
     * @return Framework
     */
    public Framework initializeFramework (ServletContextEvent context) {
        servletContextEvent = context;

        if (null == Config.CONTEXT) {
            Config.setMyApp(servletContextEvent.getServletContext());
        }

        // load all properties and set base directory
        Properties felixProps = loadConfig();

        // fetch the 'felix.base.dir' property and check if exists. On the props file the prop needs to
        for (String key : FELIX_DIRECTORIES) {
            if (new File(felixProps.getProperty(key)).mkdirs()) {
                Logger.info(this.getClass(), "Building Directory:" + felixProps.getProperty(key));
            }
        }

        FELIX_EXTRA_PACKAGES_FILE = felixProps.getProperty(FELIX_BASE_DIR) + File.separator + "osgi-extra.conf";
        FELIX_EXTRA_PACKAGES_FILE_GENERATED = felixProps.getProperty(FELIX_BASE_DIR) + File.separator + "osgi-extra-generated.conf";

        // Verify the bundles are in the right place
        verifyBundles(felixProps, context);

        // Set all OSGI Packages
        String extraPackages;
        try {
            extraPackages = getExtraOSGIPackages();
        } catch (IOException e) {
            Logger.error(this, "Error loading the OSGI framework properties: " + e);
            throw new RuntimeException(e);
        }

        // Setting the OSGI extra packages property
        felixProps.setProperty(PROPERTY_OSGI_PACKAGES_EXTRA, extraPackages);

        /*
        // The following is commented out since it is not affecting any OSGI functionality
        // Nevertheless the following code allows the system to include additional Felix and OSGI properties by using a
        // felix.system.properties key (could be a path)

        // Load system properties.
        Main.loadSystemProperties(); // will load any property by using the felix.system.properties value

        // Copy framework properties from the system properties.
        Main.copySystemProperties( propertiesToMap( configProps ) ); // will copy any system property to the config props
        */

        try {
            // Create an instance and initialize the framework.
            FrameworkFactory factory = getFrameworkFactory();
            felixFramework = factory.newFramework(felixProps);
            felixFramework.init();

            // Use the system bundle context to process the auto-deploy
            // and auto-install/auto-start properties.
            AutoProcessor.process(felixProps, felixFramework.getBundleContext());

            // Start the framework.
            felixFramework.start();
            Logger.info(this, "osgi felix framework started");
        } catch (Exception ex) {
            Logger.error(this, "Could not create framework: " + ex);
            throw new RuntimeException(ex);
        }

        return felixFramework;
    }

    /**
     * Stops the OSGi framework
     */
    public void stopFramework() {
        try {
            BundleContext bundleContext = HostActivator.instance().getBundleContext();

            //Closing tracker associated to the HttpServlet
            DispatcherTracker tracker = OSGIProxyServlet.tracker;
            if (tracker != null) {
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
        } catch (Exception e) {
            Logger.warn(this, "exception while stopping felix!", e);
        }
    }

    /**
     * Get bundle context
     *
     * @return BundleContext
     */
    public BundleContext getBundleContext() {
        return felixFramework.getBundleContext();
    }

    /**
     * Gets the OSGi framework factory
     *
     * @return FrameworkFactory
     * @throws Exception Any Exception
     */
    private static FrameworkFactory getFrameworkFactory() throws Exception {
        URL url = Main.class.getClassLoader().getResource("META-INF/services/org.osgi.framework.launch.FrameworkFactory");
        if ( url != null ) {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            try {
                for (String s = br.readLine(); s != null; s = br.readLine()) {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ((s.length() > 0) && (s.charAt( 0 ) != '#')) {
                        Logger.info(OSGIUtil.class, "Loading Factory " + s);
                        return (FrameworkFactory) Class.forName(s).newInstance();
                    }
                }
            } finally {
                if (br != null) br.close();
            }
        }

        throw new Exception("Could not find framework factory.");
    }

    /**
     * Loads all the OSGI configured properties
     *
     * @return Properties
     */
    private Properties loadConfig() {
        Properties properties = defaultProperties();
        Iterator<String> it = Config.getKeys();
        while (it.hasNext()) {
            final String key = it.next();
            if (key != null && key.startsWith("felix.")) {
                if (key.equals(FELIX_BASE_DIR)) {
                    // Allow the property in the file to be felix.base.dir
                    properties.put(key, Config.getStringProperty(key));
                    Logger.info(this, "Found property  " + key + "=" + Config.getStringProperty(key));
                } else {
                    String value = (UtilMethods.isSet(Config.getStringProperty(key, null))) ? Config.getStringProperty(key) : null;
                    String felixKey = key.substring(6);
                    properties.put(felixKey, value);
                    Logger.info(OSGIUtil.class, "Found property  " + felixKey + "=" + value);
                }
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
     * @return String
     * @throws IOException Any IOException
     */
    public String getExtraOSGIPackages() throws IOException {
        String extraPackages;

        File f = new File(FELIX_EXTRA_PACKAGES_FILE);
        if (!f.exists()) {
            StringBuilder bob = new StringBuilder();
            final Collection<String> list = ResourceCollectorUtil.getResources();
            for (final String name : list) {
                if(name.startsWith("/")) continue;
                if(name.contains(":")) continue;

                if (File.separator.equals( "/" )) {
                    bob.append(name.replace(File.separator, ".") + "," + "\n");
                } else {
                    //Zip entries have '/' as separator on all platforms
                    bob.append((name.replace( File.separator, "." ).replace( "/", "." )) + "," + "\n");
                }
            }

            bob.append(
                "org.osgi.framework," +
                    "org.osgi.framework.wiring," +
                    "org.osgi.service.packageadmin," +
                    "org.osgi.framework.startlevel," +
                    "org.osgi.service.startlevel," +
                    "org.osgi.service.url," +
                    "org.osgi.util.tracker," +
                    "org.osgi.service.http," +
                    "javax.inject.Qualifier," +
                    "javax.servlet.resources," +
                    "javax.servlet;javax.servlet.http;version=3.1.0"
            );

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(FELIX_EXTRA_PACKAGES_FILE_GENERATED)), "utf-8"));
                writer.write(bob.toString());
            } catch (IOException ex) {
                Logger.error(this, ex.getMessage(), ex);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (Exception ex) {
                    Logger.error(this, ex.getMessage(), ex);
                }
            }
        }

        //Reading the file with the extra packages
        InputStream inputStream;
        if (f.exists()) {
            inputStream = Files.newInputStream(Paths.get(FELIX_EXTRA_PACKAGES_FILE));
        } else {
            inputStream = Files.newInputStream(Paths.get(FELIX_EXTRA_PACKAGES_FILE_GENERATED));
        }

        try {
            extraPackages = IOUtils.toString(inputStream);
        } finally {
            inputStream.close();
        }

        //Clean up the properties, it is better to keep it simple and in a standard format
        extraPackages = extraPackages.replaceAll("\\\n", "");
        extraPackages = extraPackages.replaceAll("\\\r", "");
        extraPackages = extraPackages.replaceAll("\\\\", "");

        return extraPackages;
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
                felixPath = Config.CONTEXT.getRealPath("/WEB-INF") + File.separator + "felix" + File.separator + manualDefaultPath;
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
     * @param props The properties
     * @param context The servlet context
     */
    private void verifyBundles(Properties props, ServletContextEvent context) {
        String bundlePath = props.getProperty(AUTO_DEPLOY_DIR_PROPERTY);
        String baseDirectory = getBaseDirectory(context);

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

    /**
     * Gets the base directory, fetching it from the real path on the servlet context.
     * If not found, it tries to fetch it from configuration context.
     * If still not found, it fetches it from the 'felix.base.dir' property
     * If value is null an exception is thrown.
     *
     * @param context The servlet context
     * @return String
     */
    public String getBaseDirectory(ServletContextEvent context) {
        String baseDirectory = null;
        if (context != null) {
            baseDirectory = context.getServletContext().getRealPath("/WEB-INF");
        }

        if (!UtilMethods.isSet(baseDirectory)) {
            baseDirectory = Config.CONTEXT.getRealPath("/WEB-INF");

            if (!UtilMethods.isSet(baseDirectory)) {
                baseDirectory = parseBaseDirectoryFromConfig();
            }
        }

        if (!UtilMethods.isSet(baseDirectory)) {
            String errorMessage = "The default WEB-INF base directory is not found. Value is null";
            Logger.error(this, errorMessage);

            throw new RuntimeException(errorMessage);
        }

        return baseDirectory;
    }

    /**
     * Parses the base directory from config
     *
     * @return String
     */
    public String parseBaseDirectoryFromConfig() {
        String baseDirectory = Config.getStringProperty(FELIX_BASE_DIR, "/WEB-INF");
        if (baseDirectory.endsWith("/WEB-INF")) {
            baseDirectory = baseDirectory.substring(0, baseDirectory.indexOf(("/WEB-INF")) + 8);
        }

        return baseDirectory;
    }
}