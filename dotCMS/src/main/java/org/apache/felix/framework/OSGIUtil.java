package org.apache.felix.framework;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.Debouncer;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.ClusterLockManager;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ResourceCollectorUtil;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.FileUtil;
import com.liferay.util.MathUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.util.manifestparser.ParsedHeaderClause;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Jonathan Gamba
 * Date: 9/17/12
 */
public class OSGIUtil {

    private static final String OSGI_EXTRA_CONFIG_FILE_PATH_KEY = "OSGI_EXTRA_CONFIG_FILE_PATH_KEY";
    private static final String OSGI_RESTART_LOCK_KEY = "osgi_restart_lock";
    private static final String OSGI_CHECK_UPLOAD_FOLDER_FREQUENCY = "OSGI_CHECK_UPLOAD_FOLDER_FREQUENCY";
    // by default the upload folder checker is 10 seconds
    private static final int OSGI_CHECK_UPLOAD_FOLDER_FREQUENCY_DEFAULT_VAL = 10;


    //List of jar prefixes of the jars to be included in the osgi-extra.conf file
    public final List<String> portletIDsStopped = Collections.synchronizedList(new ArrayList<>());
    public final List<String> actionletsStopped = Collections.synchronizedList(new ArrayList<>());
    public WorkflowAPIOsgiService workflowOsgiService;
    private static final String WEB_INF_FOLDER = "/WEB-INF";
    private static final String FELIX_BASE_DIR = "felix.base.dir";
    private static final String FELIX_UPLOAD_DIR = "felix.upload.dir";
    private static final String FELIX_FILEINSTALL_DIR = "felix.fileinstall.dir";
    private static final String FELIX_UNDEPLOYED_DIR = "felix.undeployed.dir";
    private static final String FELIX_FRAMEWORK_STORAGE = org.osgi.framework.Constants.FRAMEWORK_STORAGE;
    private static final String AUTO_DEPLOY_DIR_PROPERTY =  AutoProcessor.AUTO_DEPLOY_DIR_PROPERTY;


    private final Debouncer debouncer = new Debouncer();

    // PUBSUB
    private final static  String TOPIC_NAME = OsgiRestartTopic.OSGI_RESTART_TOPIC;
    private final DotPubSubProvider pubsub;
    private final OsgiRestartTopic osgiRestartTopic;

    private Framework felixFramework;

    //// When the strategy to discover jars on the upload folder is not by folder watcher (watchers do not work on docker)
    /// we have a job that runs every 10 seconds (it is configurable) and checks if there is any jars in the upload folder
    /// if they are; runs a process to reload (copy the jars to load folder and so on)
    /// Does not make sense to spend the I/O every 10 seconds reading the folder, so these counters helps to balance
    // hits to that folder. So as soon as the job starts the first read will be in 10 seconds, the next will be in 20 seconds, 30 sec, 40 sec
    // etc, until 100 seconds; here will be reset to 10 seconds again. It helps to balance a bit the I/O time.
    // if some of the reads to the upload folder founds a jar, the counts are reset to the initial values again and the cycle begins one more time.
    private final AtomicInteger uploadFolderReadsCount = new AtomicInteger(0);
    private final AtomicInteger currentJobRestartIterationsCount = new AtomicInteger(0);

    // Indicates the job were already started, so next restart of the OSGI framework won't create a new one job.
    private final AtomicBoolean isStartedOsgiRestartSchedule = new AtomicBoolean(false);
    /**
     * Felix directory list
     */
    private static final String[] FELIX_DIRECTORIES = new String[] {
        FELIX_BASE_DIR, FELIX_UPLOAD_DIR, FELIX_FILEINSTALL_DIR, FELIX_UNDEPLOYED_DIR, AUTO_DEPLOY_DIR_PROPERTY, FELIX_FRAMEWORK_STORAGE
    };

    public static final String BUNDLE_HTTP_BRIDGE_SYMBOLIC_NAME = "org.apache.felix.http.bundle";
    private static final String PROPERTY_OSGI_PACKAGES_EXTRA = "org.osgi.framework.system.packages.extra";
    public String FELIX_EXTRA_PACKAGES_FILE;

    public static OSGIUtil getInstance() {
        return OSGIUtilHolder.instance;
    }

    private static class OSGIUtilHolder{
        private static OSGIUtil instance = new OSGIUtil();
    }

    private OSGIUtil() {

        
        this.pubsub                = DotPubSubProviderLocator.provider.get();
        this.osgiRestartTopic = new OsgiRestartTopic();
        Logger.debug(this.getClass(), "Starting hook with PubSub on OSGI");

        this.pubsub.start();
        this.pubsub.subscribe(this.osgiRestartTopic);
    }

    public List<String> getPortletIDsStopped() {
        return portletIDsStopped;
    }

    public List<String> getActionletsStopped() {
        return actionletsStopped;
    }

    /**
     * Loads the default properties
     *
     * @return Properties
     */
    private Properties defaultProperties() {

        Properties felixProps = new Properties();
        final String felixDirectory = getFelixBaseDirFromConfig();

        Logger.info(this, () -> "Felix base dir: " + felixDirectory);

        final String felixAutoDeployDirectory = Config.getStringProperty(AUTO_DEPLOY_DIR_PROPERTY,  felixDirectory + File.separator + "bundle") ;
        final String felixUploadDirectory     = Config.getStringProperty(FELIX_UPLOAD_DIR, felixDirectory + File.separator + "upload") ;
        final String felixLoadDirectory =       Config.getStringProperty(FELIX_FILEINSTALL_DIR,     felixDirectory + File.separator + "load") ;
        final String felixUndeployDirectory =   Config.getStringProperty(FELIX_UNDEPLOYED_DIR,      felixDirectory + File.separator + "undeployed") ;
        final String felixCacheDirectory =      Config.getStringProperty(FELIX_FRAMEWORK_STORAGE,   felixDirectory + File.separator + "felix-cache") ;

        felixProps.put(FELIX_BASE_DIR, felixDirectory);
        felixProps.put(AUTO_DEPLOY_DIR_PROPERTY, felixAutoDeployDirectory);
        felixProps.put(FELIX_FRAMEWORK_STORAGE, felixCacheDirectory);
        felixProps.put(FELIX_UPLOAD_DIR, felixUploadDirectory);
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

        // Create host activator;
        HostActivator hostActivator = HostActivator.instance();
        felixProps.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, ImmutableList.of(hostActivator));

        return felixProps;
    }

    
    final long delay = Config.getLongProperty("OSGI_UPLOAD_DEBOUNCE_DELAY_MILLIS", 5000);
    /**
     * Overrides the content of the <strong>osgi-extra.conf</strong> file
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void writeOsgiExtras (final String extraPackages, final boolean testDryRun)  {

        if(UtilMethods.isEmpty(extraPackages)) {
            return ;
        }
        
        if("RESET".equals(extraPackages)) {
            new File(OSGIUtil.getInstance().getOsgiExtraConfigPath()).delete();
            debouncer.debounce("restartOsgi", this::restartOsgi, delay, TimeUnit.MILLISECONDS);
            return;
        }

        if (Config.getBooleanProperty("OSGI_TEST_DRY_RUN", true) && testDryRun) {
            this.testDryRun(extraPackages);
        }

        //Override the file with the values we just read
        final String osgiExtraFile = OSGIUtil.getInstance().getOsgiExtraConfigPath();
        final String osgiExtraFileTmp = osgiExtraFile + "_" + UUIDGenerator.shorty();

        try(BufferedWriter writer = new BufferedWriter( new FileWriter( osgiExtraFileTmp   ) )){
            writer.write( extraPackages );
            Logger.info( this, "OSGI Extra Packages Saved");
        }
        catch(Exception e) {
            Logger.error( OSGIUtil.class, e.getMessage(), e );
            throw new DotRuntimeException( e.getMessage(), e );
        }
        new File(osgiExtraFileTmp).renameTo(new File(osgiExtraFile));

        //restart OSGI after delay
        debouncer.debounce("restartOsgi", this::restartOsgi, delay, TimeUnit.MILLISECONDS);
    }

    public String getOsgiExtraConfigPath () {

        final Supplier<String> supplier = () -> APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator + "server" + File.separator + "osgi" + File.separator +  "osgi-extra.conf";
        final String dirPath = Config.getStringProperty(OSGI_EXTRA_CONFIG_FILE_PATH_KEY, supplier.get());
        return Paths.get(dirPath).normalize().toString();
    }

    /**
     * Test if the Osgi Packages are ok to be overriden
     * @param osgiPackages
     */
    public void testDryRun (final String osgiPackages) {

        final List<ParsedHeaderClause> exportClauses =
                invokeParserStandardHeader(osgiPackages);
        for (final ParsedHeaderClause clause : exportClauses) {

            for (final String packageName : clause.m_paths) {
                if (packageName.equals(".")) {

                    Logger.error(this, "Exporing '.' is invalid.");
                    throw new OsgiException("Exporing '.' is invalid.");
                }

                if (packageName.length() == 0) {

                    Logger.error(this, "Exported package names cannot be zero length.\nPackages: " + osgiPackages);
                    throw new OsgiException(
                            "Exported package names cannot be zero length.");
                }
            }

            // Check for "version" and "specification-version" attributes
            // and verify they are the same if both are specified.
            final Object versionAttr = clause.m_attrs.get(Constants.VERSION_ATTRIBUTE);
            final Object packageSpecVersion = clause.m_attrs.get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if ((versionAttr != null) && (packageSpecVersion != null)) {
                // Verify they are equal.
                if (!String.class.cast (versionAttr).trim().equals(String.class.cast(packageSpecVersion).trim())) {
                    throw new OsgiException(
                            "Both version and specification-version are specified, but they are not equal.");
                }
            }

            try {
                if (versionAttr != null) {
                    // check version format
                    Version.parseVersion(versionAttr.toString());
                }

                if (packageSpecVersion != null) {
                    // check version format
                    Version.parseVersion(packageSpecVersion.toString());
                }
            } catch (IllegalArgumentException e) {

                Logger.error(this, e.getMessage() + ".\nPackages: " + osgiPackages);
                throw new OsgiException(e.getMessage(), e);
            }
        }
    }

    private  List<ParsedHeaderClause>  invokeParserStandardHeader (final String osgiPackages) {

        try {
            final Method method = ManifestParser.class.getDeclaredMethod("parseStandardHeader", String.class);
            method.setAccessible(true);
            return (List<ParsedHeaderClause>) method.invoke(null, osgiPackages);
        } catch (Exception e) {
            throw new OsgiException(
                    "Can not access the parseStandardHeader to run the dry run");
        }
    }

    /**
     * Initializes the framework OSGi using the servlet context
     *
     * @return Framework
     */
    public synchronized Framework initializeFramework() {

        if(felixFramework!=null) {
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

        FELIX_EXTRA_PACKAGES_FILE = this.getOsgiExtraConfigPath();

        // Verify the bundles are in the right place
        verifyBundles(felixProps);

        // Set all OSGI Packages
        String extraPackages = getExtraOSGIPackages();


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

            final String fileUploadDirectory = felixProps.getProperty(FELIX_UPLOAD_DIR);
            // before init we have to check if any new bundle has been upload
            final boolean testDryRun = false; // we do not want to do test dry run when starting the osgi
            this.fireReload(new File(fileUploadDirectory), testDryRun);
            // Create an instance and initialize the framework.
            FrameworkFactory factory = getFrameworkFactory();
            felixFramework = factory.newFramework(felixProps);
            felixFramework.init();

            // Use the system bundle context to process the auto-deploy
            // and auto-install/auto-start properties.
            AutoProcessor.process(felixProps, felixFramework.getBundleContext());

            // Start the framework.
            felixFramework.start();

            this.startWatchingUploadFolder(fileUploadDirectory);

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

    private void startWatchingUploadFolder(final String uploadFolder) {
        if(this.isStartedOsgiRestartSchedule.getAndSet(true)) {
            return;
        }

        Logger.debug(this, ()->
                "Using Schedule fixed job to discover changes on the OSGI upload folder: " + uploadFolder);
        // use a schedule thread with shad lock
        final ClusterLockManager<String> lockManager = DotConcurrentFactory.getInstance().getClusterLockManager(OSGI_RESTART_LOCK_KEY);
        final String serverId = APILocator.getServerAPI().readServerId();
        final long delay = Config.getLongProperty(OSGI_CHECK_UPLOAD_FOLDER_FREQUENCY, OSGI_CHECK_UPLOAD_FOLDER_FREQUENCY_DEFAULT_VAL); // check each 10 seconds
        final long initialDelay = MathUtil.sumAndModule(serverId.toCharArray(), delay);
        final File uploadFolderFile = new File(uploadFolder);
        Logger.debug(this, ()-> "Starting the schedule fix job, serverId: " + serverId
                + ", delay: " + delay + ", initialDelay: " + initialDelay);
        DotConcurrentFactory.getScheduledThreadPoolExecutor().scheduleWithFixedDelay(
                ()-> this.checkUploadFolder(uploadFolderFile, lockManager),
                initialDelay, delay, TimeUnit.SECONDS);
    }


    public void checkUploadFolder() {
        final ClusterLockManager<String> lockManager = DotConcurrentFactory.getInstance().getClusterLockManager(OSGI_RESTART_LOCK_KEY);
        checkUploadFolder(new File(OSGIUtil.getInstance().getFelixUploadPath()),lockManager);
    }
    
    
    
    
    public void processExports(final String jarFile) {
        if(!jarFile.equals(StringUtils.sanitizeFileName(jarFile))){
            throw new DotRuntimeException("Invalid bundle name: " + jarFile);
        }
        File jarfile = new File(getFelixDeployPath() + File.separator + jarFile); 
        File newJarfile = new File(getFelixUploadPath() + File.separator + jarFile);     
        
        if(!jarfile.exists() || newJarfile.exists()) {
            throw new DotRuntimeException("Bundle does not exist or is processing: " + jarFile);
        }
        
        jarfile.renameTo(newJarfile);
        
        final ClusterLockManager<String> lockManager = DotConcurrentFactory.getInstance().getClusterLockManager(OSGI_RESTART_LOCK_KEY);
        checkUploadFolder(new File(OSGIUtil.getInstance().getFelixUploadPath()),lockManager);
    }
    
    
    
    

    // this method is called by the schedule to see if jars has been added to the framework
    private void checkUploadFolder(final File uploadFolderFile, final ClusterLockManager<String> lockManager) {

        Logger.debug(this, ()-> "Calling the check upload folder job, uploadFolderFile: " +
                uploadFolderFile + ", currentJobRestartIterationsCount: " + this.currentJobRestartIterationsCount.intValue() +
                ", uploadFolderReadsCount: " + this.uploadFolderReadsCount.intValue());
        // we do not want to read every 10 seconds, so we read at 20, 30, 40, etc

            Logger.debug(this, ()-> "***** Checking the upload folder for jars");
            if (this.anyJarOnUploadFolder(uploadFolderFile)) {

                Logger.debug(this, ()-> "****** Has found jars on upload, folder, acquiring the lock and reloading the OSGI restart *****");

                try {

                    Logger.debug(this, () -> "Trying to lock to start the reload");
                    final boolean testDryRun = true;
                    lockManager.tryClusterLock(() -> this.fireReload(uploadFolderFile, testDryRun));
                    Logger.debug(this, () -> "File Reload Done");
                } catch (Throwable e) {

                    Logger.error(this, "Error try to acquire the lock, uploadFolder: " + uploadFolderFile +
                            ", msg: " + e.getMessage(), e);
                }
            } else {

                Logger.debug(this, ()-> "No jars on upload folder");
                // if not jars we want to wait for the next read of the upload folder
            }
    }

    private boolean anyJarOnUploadFolder(final File uploadFolderFile) {

        Logger.debug(this, ()-> "Check if any jar in the upload folder");
        final String[] pathnames = uploadFolderFile.list(new SuffixFileFilter(".jar"));
        return UtilMethods.isSet(pathnames) && pathnames.length > 0;
    }

    private void fireReload(final File uploadFolderFile, final boolean testDryRun) {

        Logger.info(this, ()-> "Starting the osgi reload on folder: " + uploadFolderFile);

        APILocator.getLocalSystemEventsAPI().asyncNotify(
                new OSGIUploadBundleEvent(Instant.now(), uploadFolderFile));

        final String[] pathnames = uploadFolderFile.list(new SuffixFileFilter(".jar"));

        if (UtilMethods.isSet(pathnames)) {

            final Set<String> osgiUserPackages = new TreeSet<>();
            for (final String pathname : pathnames) {

                Logger.info(this, "OSGI - pathname: " + pathname);
                final File fileJar = new File(uploadFolderFile, pathname);
                Collection<String> packages = Collections.emptyList();
                if (fileJar.exists() && fileJar.canRead()) {

                    packages = ResourceCollectorUtil.getPackages(fileJar);
                }

                osgiUserPackages.addAll(packages);
            }

            processOsgiPackages(uploadFolderFile, pathnames, osgiUserPackages, testDryRun);
        }
    }
    
    private void processOsgiPackages(final File uploadFolderFile,
                                     final String[] pathnames,
                                     final Set<String> osgiUserPackages,
                                     final boolean testDryRun) {
        try {

            final LinkedHashSet<String> exportedPackagesSet = this.getExportedPackagesAsSet();
            
            if (exportedPackagesSet.containsAll(osgiUserPackages)) {
                this.moveNewBundlesToFelixLoadFolder(uploadFolderFile, pathnames);
                return; 
            }
                
            Logger.info(this, "There are a new changes into the exported packages");
            exportedPackagesSet.addAll(osgiUserPackages);
            this.writeExtraPackagesFiles(exportedPackagesSet, testDryRun);

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    LinkedHashSet<String> getExportedPackagesAsSet() {

        return new LinkedHashSet<>(
                StringUtils.splitOnCommasWithQuotes(getExtraOSGIPackages()).stream()
                        .map(org.apache.commons.lang3.StringUtils::normalizeSpace)
                        .collect(Collectors.toSet()));
    }

    // move all on upload folder to load, and restarts osgi.
    private void restartOsgi() {
        Logger.info(this, ()-> "Restarting OSGI");
        final File uploadPath    = new File(this.getFelixUploadPath());
        final String[] pathnames = uploadPath.list(new SuffixFileFilter(".jar"));

        if (UtilMethods.isSet(pathnames)) {

            this.moveNewBundlesToFelixLoadFolder(uploadPath, pathnames);

            this.restartOsgiClusterWide();

            Try.run(()->APILocator.getSystemEventsAPI()
                    .push(SystemEventType.OSGI_FRAMEWORK_RESTART, new Payload(pathnames)))
                    .onFailure(e -> Logger.error(OSGIUtil.this, e.getMessage()));

        }
    }

    /**
     * Restart the current instance and notify the rest of the nodes in the cluster that restart is needed
     */
    public void restartOsgiClusterWide() {
        Logger.info(this, ()-> "Restarting OSGI Cluster Wide");
        this.restartOsgiOnlyLocal();
        Logger.debug(this, ()-> "Sending a PubSub Osgi Restart event");

        final DotPubSubEvent event = new DotPubSubEvent.Builder ()
                .addPayload("sourceNode", APILocator.getServerAPI().readServerId())
                .withTopic(TOPIC_NAME)
                .withType(OsgiRestartTopic.EventType.OGSI_RESTART_REQUEST.name())
                .build();

        this.pubsub.publish(event);
    }

    /**
     * Do the restart only for the current node (locally)
     */
    public void restartOsgiOnlyLocal() {
            Logger.info(this, ()-> "Restarting OSGI Locally");
            //Remove Portlets in the list
            this.portletIDsStopped.stream().forEach(APILocator.getPortletAPI()::deletePortlet);
            Logger.info(this, "Portlets Removed: " + this.portletIDsStopped.toString());

            //Remove Actionlets in the list
            if (null  != this.workflowOsgiService) {
                this.actionletsStopped.stream().forEach(this.workflowOsgiService::removeActionlet);
                Logger.info(this, "Actionlets Removed: " + this.actionletsStopped.toString());
            }

            //Cleanup lists
            this.portletIDsStopped.clear();
            this.actionletsStopped.clear();

            //First we need to stop the framework
            this.stopFramework();

            //Now we need to initialize it
            this.initializeFramework();
    }

    /**
     * Move this bundles from the upload folder to the deploy folder.
     * @param pathnames String array of bundle file names.
     */
    public void moveNewBundlesToFelixLoadFolder(final String[] pathnames) {

        this.moveNewBundlesToFelixLoadFolder(new File(this.getFelixUploadPath()), pathnames);
    }

    private void moveNewBundlesToFelixLoadFolder(final File uploadFolderFile, final String[] pathnames) {

        final File deployDirectory   = new File(this.getFelixDeployPath());
        try {

            if (deployDirectory.exists() && deployDirectory.canWrite()) {

                for (final String pathname : pathnames) {

                    moveBundle(uploadFolderFile, pathnames, deployDirectory, pathname);
                }
                
                sendOSGIBundlesLoadedMessage(pathnames);
            } else {

                Logger.warn(this, "The directory: " + this.getFelixDeployPath()
                        + " does not exists or can not read");
            }
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    private void moveBundle(final File uploadFolderFile,
                            final String[] pathnames,
                            final File deployDirectory,
                            final String pathname) throws IOException {

        final File bundle      = new File(uploadFolderFile, pathname);
        final File bundleDestination = new File(deployDirectory, bundle.getName());
        if (ResourceCollectorUtil.isFragmentJar(bundle)) { // now we delete the bundle if it is a fragment since we already have the exported packages covered

            Files.delete(bundle.toPath());
            Logger.debug(this, "Deleted the fragment bundle: " + bundle);
            return;
        }

        Logger.debug(this, "Moving the bundle: " + bundle + " to " + deployDirectory);

        move(pathnames, deployDirectory, bundle, bundleDestination);
    }

    private void move(final String[] pathnames,
                      final File deployDirectory,
                      final File bundle,
                      final File bundleDestination) throws IOException {

        if (FileUtil.move(bundle, bundleDestination)) {

            Try.run(()->APILocator.getSystemEventsAPI()					    // CLUSTER WIDE
                    .push(SystemEventType.OSGI_BUNDLES_LOADED, new Payload(pathnames)))
                    .onFailure(e -> Logger.error(OSGIUtil.this, e.getMessage()));

            Logger.debug(this, "Moved the bundle: " + bundle + " to " + deployDirectory);
        } else {
            Logger.debug(this, "Could not move the bundle: " + bundle + " to " + deployDirectory);
        }
    }

    private static void sendOSGIBundlesLoadedMessage(final String[] pathnamesIn) {

        final Tuple2<Boolean, String []> result = containsFragments(pathnamesIn);
        if (result._1()) {

            final String successMessage  =  "The packages in the fragment have been added to the exported packages list.";
            SystemMessageEventUtil.getInstance().pushMessage("OSGI_BUNDLES_LOADED",new SystemMessageBuilder().setMessage(successMessage)
                    .setLife(DateUtil.FIVE_SECOND_MILLIS)
                    .setSeverity(MessageSeverity.SUCCESS).create(), null);
        }

        final String[] pathnames = result._2();
        final String messageKey      = pathnames.length > 1? "new-osgi-plugins-installed":"new-osgi-plugin-installed";
        final String successMessage  = Try.of(()->LanguageUtil.get(APILocator.getCompanyAPI()
                .getDefaultCompany().getLocale(), messageKey)).getOrElse(()-> "New OSGi Plugin(s) have been installed");
        SystemMessageEventUtil.getInstance().pushMessage("OSGI_BUNDLES_LOADED",new SystemMessageBuilder().setMessage(successMessage)
                .setLife(DateUtil.FIVE_SECOND_MILLIS)
                .setSeverity(MessageSeverity.SUCCESS).create(), null);
    }


    private static Tuple2<Boolean, String[]> containsFragments(final String[] pathnamesIn) {

        boolean hasFragments = false;
        final List<String> pathnames = new ArrayList<>();
        for (final String pathname : pathnamesIn) {

            if (pathname.contains("fragment")) {
                hasFragments = true;
            } else {
                pathnames.add(pathname);
            }
        }

        return Tuple.of(hasFragments, pathnames.toArray(new String []{}));
    }

    /**
     * Stops the OSGi framework
     */
    public void stopFramework() {

        try {


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
                        return (FrameworkFactory) Class.forName(s).getDeclaredConstructor().newInstance();
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
            if(key==null) {
                continue;
            }
            if (key.startsWith("felix.")) {

                final String value = (UtilMethods.isSet(Config.getStringProperty(key, null))) ? Config.getStringProperty(key)
                                : null;
                String felixKey = key.substring(6);
                properties.put(felixKey, value);
                Logger.info(OSGIUtil.class, () -> "Found property  " + felixKey + "=" + value);

            }
            if (key.startsWith("DOT_FELIX_FELIX")) {
                final String felixKey = key.replace("DOT_FELIX_FELIX", "FELIX").replace("_", ".").toLowerCase();
                String value = (UtilMethods.isSet(Config.getStringProperty(key, null))) ? Config.getStringProperty(key)
                                : null;
                properties.put(felixKey, value);
                Logger.info(OSGIUtil.class, () -> "Found property  " + felixKey + "=" + value);
            }
            if (key.startsWith("DOT_FELIX_OSGI")) {
                final String felixKey = key.replace("DOT_FELIX_OSGI", "OSGI").replace("_", ".").toLowerCase();
                String value = (UtilMethods.isSet(Config.getStringProperty(key, null))) ? Config.getStringProperty(key)
                                : null;
                properties.put(felixKey, value);
                Logger.info(OSGIUtil.class, () -> "Found property  " + felixKey + "=" + value);
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
    public String getExtraOSGIPackages() {

        final File extraPackagesFile = new File(FELIX_EXTRA_PACKAGES_FILE);

        if (!extraPackagesFile.exists() || extraPackagesFile.length()<2) {
            extraPackagesFile.getParentFile().mkdirs();
            this.createNewExtraPackageFile (extraPackagesFile);
        }

        String packageString = readExtraPackagesFiles(extraPackagesFile).toString().replace("\n", "");
        
        // and merge with the one on the external directory
        Collection<String> packages= StringUtils.splitOnCommasWithQuotes(packageString);

        
        
        return packagesToOrderedString(packages);


    }

    private void createNewExtraPackageFile(final File extraPackagesFile)  {


        try (OutputStream out = Files.newOutputStream(extraPackagesFile.toPath());
                        InputStream in = OSGIUtil.class.getResourceAsStream("/osgi/osgi-extra.conf")) {

            org.apache.commons.io.IOUtils.copy(in, out);
        }catch(Exception e) {
            throw new DotRuntimeException(e);
        }
    }



    private String readExtraPackagesFiles(final File extraPackagesFile) {

        
        final StringWriter writer = new StringWriter();
        try (InputStream inputStream = Files.newInputStream(extraPackagesFile.toPath())) {
            writer.append(IOUtils.toString(inputStream));
        }catch(Exception e) {
            throw new DotRuntimeException(e);
        }

        return writer.toString();
    }

    private String packagesToOrderedString(final Collection<String> packages)  {

        //List<String> treeSet= new ArrayList<>(packages);
        TreeSet<String> treeSet= new TreeSet<>(packages);
        treeSet.removeIf(item -> item == null || "".equals(item.trim()));
        
        return String.join(",\n", treeSet);
        

    }

    private void writeExtraPackagesFiles(final Set<String> packages, final boolean testDryRun) {

        this.writeOsgiExtras(packagesToOrderedString(packages), testDryRun);
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
        new File(felixPath).mkdirs();


        return felixPath;
    }


    /**
     * Fetches the Felix Upload path
     *
     * @return String
     */
    public String getFelixUploadPath() {
        return getFelixPath(FELIX_UPLOAD_DIR, "upload");
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
     */
    private void verifyBundles(Properties props) {
        String bundlePath = props.getProperty(AUTO_DEPLOY_DIR_PROPERTY);
        String baseDirectory = getBaseDirectory();

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

    private String getFelixBaseDirFromConfig() {

        String defaultBasePath = Config.CONTEXT.getRealPath(WEB_INF_FOLDER);
        

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
