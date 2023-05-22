package org.apache.felix.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import io.vavr.Lazy;

/**
 * This OSGI framework only encapsulates the system osgi bundles (not the plugins, for
 * them @see @{@link org.apache.felix.framework.OSGIUtil}) These bundles are: saml, tika, ...
 * 
 * @author jsanca
 */
public class OSGISystem {


    private static final String WEB_INF_FOLDER = "/WEB-INF";
    private static final String FELIX_BASE_DIR = "felix.base.dir";
    private static final String FELIX_FRAMEWORK_STORAGE = org.osgi.framework.Constants.FRAMEWORK_STORAGE;
    private static final String AUTO_DEPLOY_DIR_PROPERTY = AutoProcessor.AUTO_DEPLOY_DIR_PROPERTY;
    private static final String FELIX_FILEINSTALL_DIR = "felix.fileinstall.dir";
    private static final String FELIX_UNDEPLOYED_DIR = "felix.undeployed.dir";
    private static final String UTF_8 = "utf-8";
    private static final String PROPERTY_OSGI_PACKAGES_EXTRA = "org.osgi.framework.system.packages.extra";
    private final Framework felixFramework;
    private final static Lazy<OSGISystem> instance = Lazy.of(OSGISystem::new);

    public static OSGISystem getInstance() {
        return instance.get();
    }

    private OSGISystem() {
        this.felixFramework = buildFramework();
    }


    public void initializeFramework() {
        if(this.felixFramework==null) {
            throw new RuntimeException("OSGi System Framework not initied");
        }
        Logger.debug(getClass(), ()->"OSGi System Framework OK");
    }



    /**
     * Loads the default properties
     *
     * @return Properties
     */
    private Properties defaultProperties() {

        final Properties felixProps = new Properties();
        final String felixDirectory = getFelixBaseDirFromConfig();

        Logger.info(this, () -> "Felix System base dir: " + felixDirectory);

        final String felixAutoDeployDirectory = Config.getStringProperty("system." + AUTO_DEPLOY_DIR_PROPERTY,
                        felixDirectory + File.separator + "bundle");
        final String felixLoadDirectory =
                        Config.getStringProperty("system." + FELIX_FILEINSTALL_DIR, felixDirectory + File.separator + "load");
        final String felixUndeployDirectory = Config.getStringProperty("system." + FELIX_UNDEPLOYED_DIR,
                        felixDirectory + File.separator + "undeployed");
        final String felixCacheDirectory = Config.getStringProperty("system." + FELIX_FRAMEWORK_STORAGE,
                        felixDirectory + File.separator + "felix-cache");

        felixProps.put(FELIX_BASE_DIR, felixDirectory);
        felixProps.put(AUTO_DEPLOY_DIR_PROPERTY, felixAutoDeployDirectory);
        felixProps.put(FELIX_FRAMEWORK_STORAGE, felixCacheDirectory);
        felixProps.put(FELIX_FILEINSTALL_DIR, felixLoadDirectory);
        felixProps.put(FELIX_UNDEPLOYED_DIR, felixUndeployDirectory);

        felixProps.put("felix.auto.deploy.action", "install,start");
        felixProps.put("felix.fileinstall.start.level", "1");
        felixProps.put("felix.fileinstall.log.level", "3");
        felixProps.put("org.osgi.framework.startlevel.beginning", "2");
        felixProps.put("org.osgi.framework.storage.clean", "onFirstInit");
        felixProps.put("felix.log.level", "3");
        felixProps.put("felix.fileinstall.disableNio2", "true");
        felixProps.put("gosh.args", "--noi");
        felixProps.put("org.ops4j.pax.logging.log4j2.config.file","/Users/stevebolton/git/core-baseline/dotcms-integration/src/main/resources/felix-log4j.xml");

        // Create host activator;
        final HostActivator hostActivator = HostActivator.instance();
        felixProps.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, ImmutableList.of(hostActivator));

        return felixProps;
    }

    /**
     * Initializes the System framework OSGi using the servlet context
     *
     * @return Framework
     */
    private Framework buildFramework() {

        Logger.info(getClass(), "Starting: OSGi System Framework");
        // load all properties and set base directory
        final Properties felixProps = defaultProperties();

        try {


            // Setting the OSGI extra packages property
            felixProps.setProperty(PROPERTY_OSGI_PACKAGES_EXTRA,  getExtraOSGIPackages());

            // before init we have to check if any new bundle has been upload
            // Create an instance and initialize the framework.
            final FrameworkFactory factory = this.getFrameworkFactory();
            final Framework myFelixFrameWork = factory.newFramework(felixProps);
            myFelixFrameWork.init();
            AutoProcessor.process(felixProps, myFelixFrameWork.getBundleContext());

            myFelixFrameWork.start();
            Logger.info(this, () -> "Started : OSGi System Framework");


            return myFelixFrameWork;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }


    }

    /**
     * Returns the packages inside the <strong>osgi-extra.conf</strong> and the osgi-extra-generate.conf
     * files If neither of those files are there, it will generate the osgi-extra-generate.conf based
     * off the classpath for the OSGI configuration property
     * <strong>org.osgi.framework.system.packages.extra</strong>. <br/>
     * <br/>
     * The property <strong>org.osgi.framework.system.packages.extra</strong> is use to set the list of
     * packages the dotCMS context in going to expose to the OSGI context.
     *
     * @return String
     * @throws IOException Any IOException
     */
    private String getExtraOSGIPackages() throws IOException {

        final StringWriter writer = new StringWriter();

        try (InputStream initialStream = OSGIUtil.class.getResourceAsStream("/osgi/system/osgi-extra.conf")) {

            final byte[] buffer = new byte[1024];
            int bytesRead = -1;
            while ((bytesRead = initialStream.read(buffer)) != -1) {
                writer.write(new String(buffer, UTF_8), 0, bytesRead);
            }

            writer.flush();
        }

        // Clean up the properties, it is better to keep it simple and in a standard format
        return writer.toString().replaceAll("\\\n", "").replaceAll("\\\r", "").replaceAll("\\\\", "");
    }

    /**
     * Gets the OSGi framework factory
     *
     * @return FrameworkFactory
     * @throws Exception Any Exception
     */
    private FrameworkFactory getFrameworkFactory() throws Exception {

        final URL url = Main.class.getClassLoader().getResource("META-INF/services/org.osgi.framework.launch.FrameworkFactory");
        if (url != null) {

            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                for (String s = br.readLine(); s != null; s = br.readLine()) {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ((s.length() > 0) && (s.charAt(0) != '#')) {
                        Logger.info(OSGIUtil.class, "Loading Factory " + s);
                        return (FrameworkFactory) Class.forName(s).getDeclaredConstructor().newInstance();
                    }
                }
            }
        }

        throw new Exception("Could not find framework factory.");
    }



    private Framework getFelixFramework() {
        return this.felixFramework;
    }


    public Boolean isInitialized() {
        return true;
    }

    private String getFelixBaseDirFromConfig() {

        final String defaultBasePath = Config.CONTEXT.getRealPath(WEB_INF_FOLDER);

        return new File(Config.getStringProperty("system." +FELIX_BASE_DIR, defaultBasePath + File.separator + "felix-system"))
                        .getAbsolutePath();
    }

    /**
     * Returns an instance of a given service registered through OSGI
     *
     * @param serviceClass Registered service class
     * @param bundleName Bundle name of the Bundle where the service is registered
     * @return Instance of the requested service
     */
    public <T> T getService(final Class<T> serviceClass, final String bundleName) {

        final Bundle foundBundle = findBundle(bundleName);
        if (null == foundBundle) {
            throw new IllegalStateException(String.format("[%s] OSGI bundle NOT FOUND.", bundleName));
        }

        final BundleContext bundleContext = foundBundle.getBundleContext();
        if (null == bundleContext) {
            throw new IllegalStateException(String.format("OSGI bundle context NOT FOUND for bundle [%s]", bundleName));
        }

        // Getting the requested OSGI service reference
        final ServiceReference serviceReference = bundleContext.getServiceReference(serviceClass.getName());
        if (null == serviceReference) {
            throw new IllegalStateException(String.format("[%s] Service Reference NOT FOUND.", serviceClass.getName()));
        }

        final T osgiBundleService;
        try {
            // Service reference instance exposed through OSGI
            osgiBundleService = (T) bundleContext.getService(serviceReference);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Error reading [%s] Service.", serviceClass.getName()), e);
        }

        return osgiBundleService;
    }

    /**
     * Finds a bundle by bundle name
     *
     * @param bundleName Name of the bundle to search for
     */
    public Bundle findBundle(final String bundleName) {

        Bundle foundBundle = null;

        // Get the list of existing bundles
        final Bundle[] bundles = this.getBundles();
        for (final Bundle bundle : bundles) {
            if (bundleName.equalsIgnoreCase(bundle.getSymbolicName())) {
                foundBundle = bundle;
                break;
            }
        }

        return foundBundle;
    }

    public Bundle[] getBundles() {
        return ((Felix) getFelixFramework()).getBundles();
    }
} // E:O:F:OSGISystem.
