package org.apache.felix.framework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.http.proxy.DispatcherTracker;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.osgi.OSGIProxyServlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ResourceCollectorUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;

/**
 * Created by Jonathan Gamba
 * Date: 9/17/12
 */
public class OSGIUtil {

    //List of jar prefixes of the jars to be included in the osgi-extra-generated.conf file
    private List<String> dotCMSJarPrefixes = ImmutableList.of("dotcms", "ee-");
    public final List<String> portletIDsStopped = Collections.synchronizedList(new ArrayList<>());
    public final List<String> actionletsStopped = Collections.synchronizedList(new ArrayList<>());
    public WorkflowAPIOsgiService workflowOsgiService;
    private static final String WEB_INF_FOLDER = "/WEB-INF";
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


    public static OSGIUtil getInstance() {
        return OSGIUtilHolder.instance;
    }

    private static class OSGIUtilHolder{
        private static OSGIUtil instance = new OSGIUtil();
    }
    
    private OSGIUtil () {
        this.servletContext = Config.CONTEXT;
    }

    private Framework felixFramework;
    final private ServletContext servletContext;

    /**
     * Initializes the OSGi framework
     *
     * @return Framework
     */
    public Framework initializeFramework() {

        return initializeFramework(servletContext);


    }

    /**
     * Loads the default properties
     *
     * @return Properties
     */
    private Properties defaultProperties() {

        Properties felixProps = new Properties();
        final String felixDirectory = getFelixBaseDirFromConfig(null);

        Logger.info(this, () -> "Felix base dir: " + felixDirectory);

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
        felixProps.put("felix.log.level", "3");
        felixProps.put("felix.fileinstall.disableNio2", "true");
        felixProps.put("gosh.args", "--noi");

        // Create host activator;
        HostActivator hostActivator = HostActivator.instance();
        hostActivator.setServletContext(servletContext);
        felixProps.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, ImmutableList.of(hostActivator));

        return felixProps;
    }

    /**
     * Initializes the framework OSGi using the servlet context
     *
     * @param context The servlet context
     * @return Framework
     */
    public synchronized Framework initializeFramework(ServletContext context) {

        if(felixFramework!=null) {
            return felixFramework;
        }
        long start = System.currentTimeMillis();

        if (null == Config.CONTEXT) {
            Config.setMyApp(servletContext);
        }

        // load all properties and set base directory
        Properties felixProps = loadConfig();

        // fetch the 'felix.base.dir' property and check if exists. On the props file the prop needs to
        for (String key : FELIX_DIRECTORIES) {
            if (new File(felixProps.getProperty(key)).mkdirs()) {
                Logger.info(this.getClass(),
                        () -> "Building Directory:" + felixProps.getProperty(key));
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
            
            startProxyServlet() ;
            
            Logger.info(this, () -> "osgi felix framework started");
        } catch (Exception ex) {
            felixFramework=null;
            Logger.error(this, "Could not create framework: " + ex);
            throw new RuntimeException(ex);
        }

        System.setProperty(WebKeys.OSGI_ENABLED, "true");
        System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_OSGI,
                String.valueOf(System.currentTimeMillis() - start));

        return felixFramework;
    }

    private boolean startProxyServlet() {
        if (Config.getBooleanProperty("felix.felix.enable.osgi.proxyservlet", false)) {
            if (OSGIProxyServlet.bundleContext == null) {

                final Object bundleContext = servletContext.getAttribute(BundleContext.class.getName());
                if (bundleContext instanceof BundleContext) {

                    OSGIProxyServlet.bundleContext = (BundleContext) bundleContext;

                    try {
                        OSGIProxyServlet.tracker = new DispatcherTracker(OSGIProxyServlet.bundleContext, null,
                                        OSGIProxyServlet.servletConfig);
                        OSGIProxyServlet.tracker.open();
                    } catch (Exception e) {
                        Logger.error(OSGIUtil.class, "Error loading HttpService.", e);
                        return false;
                    }


                }
            }
        }
        return true;

    }
    
    
    
    /**
     * Stops the OSGi framework
     */
    public void stopFramework() {

        try {
            //Closing tracker associated to the HttpServlet
            DispatcherTracker tracker = OSGIProxyServlet.tracker;
            if (null != tracker) {
                tracker.close();
                OSGIProxyServlet.tracker = null;
            }

            if (null != felixFramework) {

                BundleContext bundleContext = HostActivator.instance().getBundleContext();
                final BundleContext frameworkBundleContext = felixFramework.getBundleContext();

                if (null != bundleContext && null != frameworkBundleContext) {
                    //Unregistering ToolBox services
                    final ServiceReference toolBoxService = frameworkBundleContext
                            .getServiceReference(PrimitiveToolboxManager.class.getName());
                    if (toolBoxService != null) {
                        bundleContext.ungetService(toolBoxService);
                    }

                    //Unregistering Workflow services
                    final ServiceReference workflowService = frameworkBundleContext
                            .getServiceReference(WorkflowAPIOsgiService.class.getName());
                    if (workflowService != null) {
                        bundleContext.ungetService(workflowService);
                    }
                } else {
                    Logger.warn(this,
                            () -> "Unable to unregistering services while stopping felix");
                }
            }
        } catch (Exception e) {
            Logger.warn(this, "Error unregistering services while stopping felix", e);
        }

        try {
            if (null != felixFramework) {
                // Stop felix
                felixFramework.stop();
                // Wait for framework to stop to exit the VM.
                felixFramework.waitForStop(0);
            }
        } catch (Exception e) {
            Logger.warn(this, "Error while stopping felix!", e);
        }finally {
            felixFramework=null;
        }
    }

    public Boolean isInitialized() {
        return null != felixFramework ;
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
                    Logger.info(this,
                            () -> "Found property  " + key + "=" + Config.getStringProperty(key));
                } else {
                    String value = (UtilMethods.isSet(Config.getStringProperty(key, null))) ? Config.getStringProperty(key) : null;
                    String felixKey = key.substring(6);
                    properties.put(felixKey, value);
                    Logger.info(OSGIUtil.class, () -> "Found property  " + felixKey + "=" + value);
                }
            }
        }
        return properties;
    }

    /**
     * Returns the packages inside the <strong>osgi-extra.conf</strong> and the
     * osgi-extra-generate.conf files If neither of those files are there, it will generate the
     * osgi-extra-generate.conf based off the classpath for the OSGI configuration property
     * <strong>org.osgi.framework.system.packages.extra</strong>. <br/><br/> The property
     * <strong>org.osgi.framework.system.packages.extra</strong> is use to set the list of packages
     * the dotCMS context in going to expose to the OSGI context.
     *
     * @return String
     * @throws IOException Any IOException
     */
    public String getExtraOSGIPackages() throws IOException {

        final File extraPackagesFile = new File(FELIX_EXTRA_PACKAGES_FILE);
        final File extraPackagesGeneratedFile = new File(FELIX_EXTRA_PACKAGES_FILE_GENERATED);

        // if neither exist, we generate a FELIX_EXTRA_PACKAGES_FILE_GENERATED
        if (!(extraPackagesFile.exists() || extraPackagesGeneratedFile.exists())) {

            final StringBuilder bob = new StringBuilder();
            final Collection<String> list = ResourceCollectorUtil.getResources(dotCMSJarPrefixes);
            for (final String name : list) {
                if (name.charAt(0) == '/' || name.contains(":")) {
                    continue;
                }
                if ("/".equals(File.separator)) {
                    bob.append(name.replace(File.separator, ".")).append(",").append("\n");
                } else {
                    // Zip entries have '/' as separator on all platforms
                    bob.append(name.replace(File.separator, ".").replace("/", ".")).append(",")
                            .append("\n");
                }
            }

            bob.append("org.osgi.framework,\n")
                    .append("org.osgi.framework.wiring,\n")
                    .append("org.osgi.service.packageadmin,\n")
                    .append("org.osgi.framework.startlevel,\n")
                    .append("org.osgi.service.startlevel,\n")
                    .append("org.osgi.service.url,\n")
                    .append("org.osgi.util.tracker,\n")
                    .append("javax.inject.Qualifier,\n")
                    .append("javax.servlet.resources,\n")
                    .append("javax.servlet;javax.servlet.http;version=3.1.0\n"
                    );

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(Paths.get(FELIX_EXTRA_PACKAGES_FILE_GENERATED)),
                    "utf-8"))) {
                writer.write(bob.toString());
            }
        }

        StringWriter stringWriter;
        if (extraPackagesFile.exists()) {
            stringWriter = readExtraPackagesFiles(extraPackagesFile);
        } else {
            stringWriter = readExtraPackagesFiles(extraPackagesGeneratedFile);
        }

        //Clean up the properties, it is better to keep it simple and in a standard format
        return stringWriter.toString().replaceAll("\\\n", "").
                replaceAll("\\\r", "").replaceAll("\\\\", "");
    }

    private StringWriter readExtraPackagesFiles(final File extraPackagesFile)
            throws IOException {

        final StringWriter writer = new StringWriter();
        try (InputStream inputStream = Files.newInputStream(extraPackagesFile.toPath())) {
            writer.append(IOUtils.toString(inputStream));
        }

        return writer;
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
        String felixPath = null;

        try {
            if (this.getConfig().containsKey(felixDirProperty)) {
                felixPath = (String) this.getConfig().get(felixDirProperty);
            }
        } catch (Exception ex) {
            Logger.error(this, String.format(
                    "Unable to find the felix '%s' folder path from OSGI bundle context. Trying to fetch it from Config.CONTEXT as real path from '/WEB-INF/felix/%s'",
                    manualDefaultPath, manualDefaultPath), ex);

            try {
                felixPath = Config.CONTEXT.getRealPath(WEB_INF_FOLDER) + File.separator + "felix"
                        + File.separator + manualDefaultPath;
            } catch (Exception ex2) {
                Logger.error(this, String.format(
                        "Unable to find the felix '%s' folder real path from Config.CONTEXT. Setting it manually to '/WEB-INF/felix/%s'",
                        manualDefaultPath, manualDefaultPath), ex2);
                felixPath = "/WEB-INF/felix/" + manualDefaultPath;
            }
        }

        if (felixPath == null) {
            Logger.error(this, String.format(
                    "Path '%s' was not successfully set. Setting it manually to '/WEB-INF/felix/%s'",
                    manualDefaultPath, manualDefaultPath));
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
            Logger.debug(this,
                    () -> String.format("Felix directory %s does not exist. Trying to create it...",
                            path));
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
    private void verifyBundles(Properties props, ServletContext context) {
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
    public String getBaseDirectory(ServletContext context) {

        String baseDirectory = null;

        if (this.isInitialized()) {
            if (this.getConfig().containsKey(FELIX_BASE_DIR)) {
                baseDirectory = (String) this.getConfig().get(FELIX_BASE_DIR);
            }
        }

        if (!UtilMethods.isSet(baseDirectory)) {
            baseDirectory = getFelixBaseDirFromConfig(context);
        }

        if (!UtilMethods.isSet(baseDirectory)) {
            String errorMessage = "Base directory for the Felix framework is not found. Value is null";
            Logger.error(this, errorMessage);

            throw new RuntimeException(errorMessage);
        }

        return baseDirectory;
    }

    private String getFelixBaseDirFromConfig(ServletContext context) {

        String defaultBasePath;

        if (context != null) {
            defaultBasePath = context.getRealPath(WEB_INF_FOLDER);
        } else {
            defaultBasePath = Config.CONTEXT.getRealPath(WEB_INF_FOLDER);
        }

        return new File(Config
                .getStringProperty(FELIX_BASE_DIR,
                        defaultBasePath + File.separator + "felix"))
                .getAbsolutePath();
    }

    /**
     * Finds a bundle by bundle name
     *
     * @param bundleName Name of the bundle to search for
     */
    public Bundle findBundle(final String bundleName) {

        Bundle foundBundle = null;

        //Get the list of existing bundles
        Bundle[] bundles = this.getBundles();
        for (Bundle bundle : bundles) {
            if (bundleName.equalsIgnoreCase(bundle.getSymbolicName())) {
                foundBundle = bundle;
                break;
            }
        }

        return foundBundle;
    }

    /**
     * Returns an instance of a given service registered through OSGI
     *
     * @param serviceClass Registered service class
     * @param bundleName Bundle name of the Bundle where the service is registered
     * @return Instance of the requested service
     */
    public <T> T getService(final Class<T> serviceClass, final String bundleName) {

        Bundle foundBundle = findBundle(bundleName);
        if (null == foundBundle) {
            throw new IllegalStateException(
                    String.format("[%s] OSGI bundle NOT FOUND.", bundleName));
        }

        BundleContext bundleContext = foundBundle.getBundleContext();
        if (null == bundleContext) {
            throw new IllegalStateException(
                    String.format("OSGI bundle context NOT FOUND for bundle [%s]", bundleName));
        }

        //Getting the requested OSGI service reference
        ServiceReference serviceReference = bundleContext
                .getServiceReference(serviceClass.getName());
        if (null == serviceReference) {
            throw new IllegalStateException(String.format(
                    "[%s] Service Reference NOT FOUND.",
                    serviceClass.getName()));
        }

        T osgiBundleService;
        try {
            //Service reference instance exposed through OSGI
            osgiBundleService = (T) bundleContext.getService(serviceReference);
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Error reading [%s] Service.", serviceClass.getName()), e);
        }

        return osgiBundleService;
    }

    private Framework getFelixFramework() {
        return this.felixFramework;
    }

    public Map<String, Object> getConfig() {
        return ((Felix) getFelixFramework()).getConfig();
    }

    public Bundle[] getBundles() {
        return ((Felix) getFelixFramework()).getBundles();
    }

    public Bundle getBundle(long id) {
        return ((Felix) getFelixFramework()).getBundle(id);
    }

    public Bundle getBundle(String location) {
        return ((Felix) getFelixFramework()).getBundle(location);
    }

    public Bundle getBundle(Class clazz) {
        return ((Felix) getFelixFramework()).getBundle(clazz);
    }

    public Bundle getBundle() {
        return ((Felix) getFelixFramework()).getBundle();
    }

}