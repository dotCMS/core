package org.apache.felix.framework;

import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.osgi.framework.launch.Framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * This OSGI framework only encapsulates the system osgi bundles (not the plugin, for them @see @{@link org.apache.felix.framework.OSGIUtil})
 * These bundles are: saml, tika, ...
 * @author jsanca
 */
public class OSGISystem {

    private static final String WEB_INF_FOLDER = "/WEB-INF";
    private static final String FELIX_BASE_DIR = "felix.base.dir";
    private static final String FELIX_FRAMEWORK_STORAGE  = org.osgi.framework.Constants.FRAMEWORK_STORAGE;
    private static final String AUTO_DEPLOY_DIR_PROPERTY =  AutoProcessor.AUTO_DEPLOY_DIR_PROPERTY;
    /**
     * Felix directory list
     */
    private static final String[] FELIX_DIRECTORIES = new String[] {
            FELIX_BASE_DIR, AUTO_DEPLOY_DIR_PROPERTY, FELIX_FRAMEWORK_STORAGE
    };
    private Framework felixFramework;

    public static OSGISystem getInstance() {
        return OSGISystem.OSGIUtilHolder.instance;
    }

    private static class OSGIUtilHolder{
        private static OSGISystem instance = new OSGISystem();
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

        final String felixAutoDeployDirectory = Config.getStringProperty(AUTO_DEPLOY_DIR_PROPERTY,  felixDirectory + File.separator + "bundle") ;
        final String felixCacheDirectory =      Config.getStringProperty(FELIX_FRAMEWORK_STORAGE,   felixDirectory + File.separator + "felix-cache") ;

        felixProps.put(FELIX_BASE_DIR, felixDirectory);
        felixProps.put(AUTO_DEPLOY_DIR_PROPERTY, felixAutoDeployDirectory);
        felixProps.put(FELIX_FRAMEWORK_STORAGE, felixCacheDirectory);

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
        felixProps.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, ImmutableList.of(hostActivator));

        return felixProps;
    }

    /**
     * Initializes the System framework OSGi using the servlet context
     *
     * @return Framework
     */
    public synchronized Framework initializeFramework() {

        if(felixFramework != null) {

            return felixFramework;
        }

        long start = System.currentTimeMillis();

        // load all properties and set base directory
        Properties felixProps = loadConfig();

        // fetch the 'felix.base.dir' property and check if exists. On the props file the prop needs to
        for (final String felixDirectory : FELIX_DIRECTORIES) {

            try {
                Files.createDirectories(new File(felixProps.getProperty(felixDirectory)).toPath());
                Logger.info(this.getClass(),
                        () -> "Building Directory:" + felixProps.getProperty(felixDirectory));
            } catch (IOException e) {
                Logger.error(this, "Error Building Directory:" +
                        felixProps.getProperty(felixDirectory) + ": " + e.getMessage(), e);
            }
        }

        // Verify the bundles are in the right place
        verifyBundles(felixProps);

        try {

            // before init we have to check if any new bundle has been upload
            // Create an instance and initialize the framework.
            FrameworkFactory factory = getFrameworkFactory();
            felixFramework = factory.newFramework(felixProps);
            felixFramework.init();

            // Use the system bundle context to process the auto-deploy
            // and auto-install/auto-start properties.
            AutoProcessor.process(felixProps, felixFramework.getBundleContext());

            // Start the framework.
            felixFramework.start();
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
     * Verify the bundles are in the right place if the default path has been overwritten
     * If bundle path is different to the default one then move all bundles to the new directory and get rid of the default one
     *
     * @param props The properties
     */
    private void verifyBundles(final Properties props) {

        final String bundlePath = props.getProperty(AUTO_DEPLOY_DIR_PROPERTY);
        final String baseDirectory = getBaseDirectory();

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
     * @return String
     */
    public String getBaseDirectory() {

        String baseDirectory = null;

        if (this.isInitialized()) {
            if (this.getConfig().containsKey(FELIX_BASE_DIR)) {
                baseDirectory = (String) this.getConfig().get(FELIX_BASE_DIR);
            }
        }

        if (!UtilMethods.isSet(baseDirectory)) {
            baseDirectory = getFelixBaseDirFromConfig();
        }

        if (!UtilMethods.isSet(baseDirectory)) {
            String errorMessage = "Base directory for the Felix framework is not found. Value is null";
            Logger.error(this, errorMessage);

            throw new RuntimeException(errorMessage);
        }

        return baseDirectory;
    }

    private Framework getFelixFramework() {
        return this.felixFramework;
    }

    public Map<String, Object> getConfig() {
        return ((Felix) getFelixFramework()).getConfig();
    }

    public Boolean isInitialized() {
        return null != felixFramework ;
    }

    /**
     * Loads all the OSGI configured properties
     *
     * @return Properties
     */
    private Properties loadConfig() {
        final Properties properties = defaultProperties();
        final Iterator<String> it = Config.getKeys();
        while (it.hasNext()) {
            final String key = it.next();
            if(key==null) {
                continue;
            }
            if (key.startsWith("system.felix.")) {

                final String felixKey = key.replace("system.felix.", "felix.").toLowerCase().substring(6);
                final String value = (UtilMethods.isSet(Config.getStringProperty(key, null))) ? Config.getStringProperty(key)
                        : null;
                properties.put(felixKey, value);
                Logger.info(OSGIUtil.class, () -> "Found property  " + felixKey + "=" + value);

            }
            if (key.startsWith("DOT_SYSTEM_FELIX_FELIX")) {
                final String felixKey = key.replace("DOT_SYSTEM_FELIX_FELIX", "FELIX").replace("_", ".").toLowerCase();
                String value = (UtilMethods.isSet(Config.getStringProperty(key, null))) ? Config.getStringProperty(key)
                        : null;
                properties.put(felixKey, value);
                Logger.info(OSGIUtil.class, () -> "Found property  " + felixKey + "=" + value);
            }
            if (key.startsWith("DOT_SYSTEM_FELIX_OSGI")) {
                final String felixKey = key.replace("DOT_SYSTEM_FELIX_OSGI", "OSGI").replace("_", ".").toLowerCase();
                String value = (UtilMethods.isSet(Config.getStringProperty(key, null))) ? Config.getStringProperty(key)
                        : null;
                properties.put(felixKey, value);
                Logger.info(OSGIUtil.class, () -> "Found property  " + felixKey + "=" + value);
            }

        }
        return properties;
    }

    private String getFelixBaseDirFromConfig() {

        final String defaultBasePath = Config.CONTEXT.getRealPath(WEB_INF_FOLDER);

        return new File(Config
                .getStringProperty(FELIX_BASE_DIR,
                        defaultBasePath + File.separator + "system-felix"))
                .getAbsolutePath();
    }
} // E:O:F:OSGISystem.
