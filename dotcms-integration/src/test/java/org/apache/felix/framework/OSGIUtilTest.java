package org.apache.felix.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.util.Config;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.dotmarketing.util.ResourceCollectorUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * OSGIUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OSGIUtilTest {

    private static final String WEB_INF_FOLDER = "/WEB-INF";
    private static final String FELIX_BASE_DIR = "felix.base.dir";
    private static final String FELIX_FILEINSTALL_DIR = "felix.felix.fileinstall.dir";
    private static final String FELIX_UNDEPLOYED_DIR = "felix.felix.undeployed.dir";

    // Config key OSGIUtil uses to locate osgi-extra.conf; overriding it lets us control the
    // exact set of packages the system bundle exports for a given framework (re)start.
    private static final String OSGI_EXTRA_CONFIG_FILE_PATH_KEY = "OSGI_EXTRA_CONFIG_FILE_PATH_KEY";

    // The package from Freshdesk #37894. It is NOT in the shipped osgi-extra.conf defaults
    // (dotCMS adds it dynamically on plugin upload), so it is absent unless we export it ourselves.
    private static final String TARGET_PACKAGE = "com.dotcms.content.index.domain";

    private static String felixDirectory;

    // Pristine osgi-extra.conf path, captured before any test overrides it, so we can restore it.
    private static String originalOsgiExtraPath;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        Mockito.when(Config.CONTEXT.getRealPath("/WEB-INF/felix"))
                .thenReturn(Config.getStringProperty(FELIX_BASE_DIR, "/WEB-INF/felix"));

        // Initialize OSGI
        initializeOSGIFramework();

        felixDirectory = new File(Config
                .getStringProperty(FELIX_BASE_DIR,
                        Config.CONTEXT.getRealPath(WEB_INF_FOLDER) + File.separator + "felix"))
                .getAbsolutePath();

        originalOsgiExtraPath = OSGIUtil.getInstance().getOsgiExtraConfigPath();
    }

    @AfterClass
    public static void cleanUp() {
        //Stopping the OSGI framework
        OSGIUtil.getInstance().stopFramework();
    }

    /**
     * Initialize Mock OSGI Framework
     */
    private static void initializeOSGIFramework() {
        try {
            OSGIUtil.getInstance().initializeFramework();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to initialize OSGI Framework", ex);
        }
    }

    /**
     * Restart the OSGI Framework
     */
    private static void restartOSGi() {
        //First we need to stop the framework
        OSGIUtil.getInstance().stopFramework();

        //Now we need to initialize it
        initializeOSGIFramework();
    }

    /**
     * Test the felix deploy path is the default path
     */


    @Test
    public void test_validate_felix_custom_base_dir() {

        Assert.assertNotNull(felixDirectory);

        String customFelixBasePath = felixDirectory.replace("/felix", "/customfelix");

        try {
            //Changing the base path and restarting the OSGI framework
            Config.setProperty(FELIX_BASE_DIR, customFelixBasePath);
            restartOSGi();

            //Validate we are using the property we just set
            String deployBasePath = OSGIUtil.getInstance().getBaseDirectory();
            Assert.assertNotNull(deployBasePath);
            assertEquals(deployBasePath, customFelixBasePath);

            //Setting back to the original value the base path and restarting the OSGI framework
            Config.setProperty(FELIX_BASE_DIR, felixDirectory);
            restartOSGi();

            //Validate we are using the property we just set
            deployBasePath = OSGIUtil.getInstance().getBaseDirectory();
            assertEquals(felixDirectory, deployBasePath);
        } finally {
            //Clean up
            removeFolder(customFelixBasePath);
        }
    }

    /**
     * Test the felix deploy path is a custom path
     */
    @Test
    public void test03CustomFelixDeployPath() {

        final String configuredDeployPath = getDeployPathInConfig();
        Assert.assertNotNull(configuredDeployPath);

        String customFelixDeployPath = configuredDeployPath.replace("/felix", "/customfelix");

        try {
            //Changing the deploy path and restarting the OSGI framework
            Config.setProperty(FELIX_FILEINSTALL_DIR, customFelixDeployPath);
            restartOSGi();

            //Validate we are using the property we just set
            String deployFelixPath = OSGIUtil.getInstance().getFelixDeployPath();
            Assert.assertNotNull(deployFelixPath);
            assertEquals(deployFelixPath, customFelixDeployPath);

            //Setting back to the original value the undeploy path and restarting the OSGI framework
            Config.setProperty(FELIX_FILEINSTALL_DIR, configuredDeployPath);
            restartOSGi();

            //Validate we are using the property we just set
            deployFelixPath = OSGIUtil.getInstance().getFelixDeployPath();
            assertEquals(configuredDeployPath, deployFelixPath);
        } finally {
            //Clean up
            removeParentFolder(customFelixDeployPath);
        }
    }

    /**
     * Test the felix undeploy path is a custom path
     */
    @Test
    public void test04CustomFelixUndeployPath() {

        final String configuredUndeployPath = getUndeployPathInConfig();
        Assert.assertNotNull(configuredUndeployPath);

        String customFelixUndeployPath = configuredUndeployPath.replace("/felix", "/customfelix");

        try {
            //Changing the undeploy path and restarting the OSGI framework
            Config.setProperty(FELIX_UNDEPLOYED_DIR, customFelixUndeployPath);
            restartOSGi();

            //Validate we are using the property we just set
            String undeployFelixPath = OSGIUtil.getInstance().getFelixUndeployPath();
            Assert.assertNotNull(undeployFelixPath);
            assertEquals(undeployFelixPath, customFelixUndeployPath);

            //Setting back to the original value the undeploy path and restarting the OSGI framework
            Config.setProperty(FELIX_UNDEPLOYED_DIR, configuredUndeployPath);
            restartOSGi();

            //Validate we are using the property we just set
            undeployFelixPath = OSGIUtil.getInstance().getFelixUndeployPath();
            assertEquals(configuredUndeployPath, undeployFelixPath);
        } finally {
            //Clean up
            removeParentFolder(customFelixUndeployPath);
        }
    }

    /**
     * Method to test: {@link OSGIUtil.getInstance().getExportedPackages()}
     * Given Scenario: Retrieve the current packages exported by the OSGI framework
     * ExpectedResult: The packages returned should not have spaces, neither new lines, etc
     *
     */
    @Test
    public void test_check_get_exported_packages_normalized() {

        final Set<String> exportedPackages = OSGIUtil.getInstance().getExportedPackagesAsSet();
        Assert.assertNotNull(exportedPackages);

        for (final String exportedPackage : exportedPackages) {
            Assert.assertNotNull(exportedPackage);
            Assert.assertFalse(exportedPackage.contains(" "));
            Assert.assertFalse(exportedPackage.contains("\t"));
            Assert.assertFalse(exportedPackage.contains("\r"));
            Assert.assertFalse(exportedPackage.contains("\n"));
        }
    }

    private void removeFolder(String path) {
        try {
            File directory = new File(path);
            FileUtils.deleteDirectory(directory);
        } catch (Exception ex) {
            //Do nothing...
        }
    }

    private void removeParentFolder(String path) {
        try {
            File directory = new File(path);
            FileUtils.deleteDirectory(directory.getParentFile());
        } catch (Exception ex) {
            //Do nothing...
        }
    }

    private String getDeployPathInConfig() {
        return Config
                .getStringProperty(FELIX_FILEINSTALL_DIR, felixDirectory + File.separator + "load");
    }

    private String getUndeployPathInConfig() {
        return Config.getStringProperty(FELIX_UNDEPLOYED_DIR,
                felixDirectory + File.separator + "undeployed");
    }

    /**
     * Method to test: include this in case the method to test does not belong to the class this test-class is testing
     * Given Scenario: Scenario under test
     * ExpectedResult: Expected result that the assertion is checking against
     *
     */
    @Test(expected = OsgiException.class)
    public void test_test_dry_run_wrong_version_format() {

        final String packageString ="com.liferay.portal.model;version=0.1.0#(*%&#(*%&#(*$&#$,com.liferay.portal.util,io.vavr;version=\"0.10\",javax.servlet.http;version=\"3.1,4\"";
        OSGIUtil.getInstance().testDryRun(packageString);
    }

    // ---------------------------------------------------------------------------------------------
    // Freshdesk #37894 — a plugin importing an exported package fails to resolve with
    // "osgi.wiring.package=...;(version>=0.0.0)" even though the package IS exported as version=0.
    // These tests prove the version value is NOT the cause: a bare version=0 export satisfies the
    // (version>=0.0.0) requirement identically to version="0.0.0"; the failure only happens when the
    // export is ABSENT from the running framework at resolve time. Named test_zzz_* so they run last
    // (FixMethodOrder = NAME_ASCENDING) and never disturb the assumptions of the tests above.
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: OSGi resolution of a bundle whose imported package is not exported by the
     * system bundle at resolve time.
     * Given Scenario: The framework is (re)started with an osgi-extra.conf that does NOT export
     * {@link #TARGET_PACKAGE}, then a bundle importing {@code TARGET_PACKAGE;version=0} is started.
     * ExpectedResult: start() throws a BundleException whose message is exactly the customer's error
     * — {@code (&(osgi.wiring.package=com.dotcms.content.index.domain)(version>=0.0.0))} — and the
     * bundle stays INSTALLED (unresolved). This isolates the symptom to "export not live at resolve
     * time", independent of the version string.
     */
    @Test
    public void test_zzz_01_import_fails_when_package_not_exported() throws Exception {

        restartWithExtraPackages(pristineExports()); // TARGET_PACKAGE deliberately absent
        final BundleContext context = HostActivator.instance().getBundleContext();
        assertNotNull(context);

        final byte[] jar = buildBundleImporting(
                "com.dotcms.osgitest.absent", TARGET_PACKAGE + ";version=0");
        final Bundle bundle = context.installBundle(
                "osgitest:absent", new ByteArrayInputStream(jar));
        try {
            bundle.start();
            fail("Expected BundleException: " + TARGET_PACKAGE + " should be unresolvable");
        } catch (final BundleException e) {
            final String msg = e.getMessage();
            assertTrue("Message should name the unresolved package. Was: " + msg,
                    msg.contains(TARGET_PACKAGE));
            assertTrue("Import version=0 should compile to a (version>=0.0.0) requirement. Was: " + msg,
                    msg.contains("version>=0.0.0"));
            assertEquals("Bundle must remain unresolved (INSTALLED)",
                    Bundle.INSTALLED, bundle.getState());
        } finally {
            uninstallQuietly(bundle);
            restoreOsgiExtras();
        }
    }

    /**
     * Method to test: OSGi resolution when the imported package IS exported as a bare {@code version=0}.
     * Given Scenario: The framework is (re)started with an osgi-extra.conf that exports
     * {@code TARGET_PACKAGE;version=0}, then a bundle importing {@code TARGET_PACKAGE;version=0} starts.
     * ExpectedResult: the bundle resolves and reaches ACTIVE — proving a bare version=0 export DOES
     * satisfy (version>=0.0.0). This is the exact scenario support could not reproduce as a failure.
     */
    @Test
    public void test_zzz_02_version0_export_satisfies_requirement() throws Exception {

        restartWithExtraPackages(pristineExports() + ",\n" + TARGET_PACKAGE + ";version=0");
        final BundleContext context = HostActivator.instance().getBundleContext();

        final byte[] jar = buildBundleImporting(
                "com.dotcms.osgitest.v0", TARGET_PACKAGE + ";version=0");
        final Bundle bundle = context.installBundle(
                "osgitest:v0", new ByteArrayInputStream(jar));
        try {
            bundle.start();
            assertEquals("version=0 export should satisfy the import and go ACTIVE",
                    Bundle.ACTIVE, bundle.getState());
        } finally {
            uninstallQuietly(bundle);
            restoreOsgiExtras();
        }
    }

    /**
     * Method to test: OSGi resolution when the imported package is exported as {@code version="0.0.0"}.
     * Given Scenario: Same as the previous test but the export is written as {@code version="0.0.0"}
     * (the customer's manual workaround value).
     * ExpectedResult: the bundle resolves and reaches ACTIVE — behaving IDENTICALLY to the bare
     * version=0 case, proving {@code 0} and {@code 0.0.0} are equivalent and the workaround's real
     * effect is the framework restart it triggers, not the version string.
     */
    @Test
    public void test_zzz_03_version_0_0_0_export_is_equivalent() throws Exception {

        restartWithExtraPackages(pristineExports() + ",\n" + TARGET_PACKAGE + ";version=\"0.0.0\"");
        final BundleContext context = HostActivator.instance().getBundleContext();

        final byte[] jar = buildBundleImporting(
                "com.dotcms.osgitest.v000", TARGET_PACKAGE + ";version=0");
        final Bundle bundle = context.installBundle(
                "osgitest:v000", new ByteArrayInputStream(jar));
        try {
            bundle.start();
            assertEquals("version=\"0.0.0\" export should behave identically to version=0",
                    Bundle.ACTIVE, bundle.getState());
        } finally {
            uninstallQuietly(bundle);
            restoreOsgiExtras();
        }
    }

    /**
     * Method to test: {@link OSGIUtil} upload-processing restart decision ({@code processOsgiPackages}).
     * Given Scenario: a PEER cluster node has already merged {@link #TARGET_PACKAGE} into the shared
     * {@code osgi-extra.conf}, but THIS node's framework was started without it, so its live export
     * snapshot ({@code liveExportedPackages}) still lacks it — the exact #36434 "Mode A" cluster race.
     * ExpectedResult: {@code processOsgiPackages} returns {@code true} (this node must restart) even
     * though the on-disk {@code osgi-extra.conf} already lists the package. The pre-fix code compared
     * the plugin packages against the shared FILE and would have returned {@code false} (skipping the
     * restart, so the package was never published on this node). A control case with the package
     * already present in the live snapshot must return {@code false} (no restart needed).
     */
    @Test
    public void test_zzz_04_restart_decision_uses_live_snapshot_not_shared_file() throws Exception {

        final String targetClause = TARGET_PACKAGE + ";version=0";

        // Clean baseline: TARGET_PACKAGE absent from both the running framework and the conf file.
        restartWithExtraPackages(pristineExports());
        final File uploadDir = Files.createTempDirectory("osgi-upload-37894").toFile();
        uploadDir.deleteOnExit();
        try {
            // Simulate a PEER node's write: the shared osgi-extra.conf now lists TARGET_PACKAGE, but we
            // deliberately do NOT restart, so this node's live snapshot stays stale (without it).
            final String confPath = OSGIUtil.getInstance().getOsgiExtraConfigPath();
            Files.write(new File(confPath).toPath(),
                    (pristineExports() + ",\n" + targetClause).getBytes(StandardCharsets.UTF_8));

            // Precondition: the shared FILE reports the package as exported...
            assertTrue("Precondition: shared osgi-extra.conf should now list the package",
                    OSGIUtil.getInstance().getExportedPackagesAsSet().stream()
                            .anyMatch(p -> p.contains(TARGET_PACKAGE)));
            // ...while the live snapshot (what the framework actually started with) does NOT.
            assertFalse("Precondition: live snapshot must NOT contain the package yet",
                    liveExportedPackages().stream().anyMatch(p -> p.contains(TARGET_PACKAGE)));

            // REGRESSION: live snapshot lacks the package -> this node MUST restart, even though the
            // shared file already has it. Pre-fix (compare-against-file) returned false here => Mode A.
            writeFragmentJar(new File(uploadDir, "peer-race-a.jar"));
            assertTrue("Node must restart when its live framework lacks the package even though a peer "
                            + "already wrote it to the shared osgi-extra.conf (Mode A regression)",
                    invokeProcessOsgiPackages(uploadDir, new String[]{"peer-race-a.jar"},
                            Set.of(targetClause)));

            // CONTROL: once the live snapshot DOES export the package, no restart is needed.
            setLiveExportedPackages(withEntry(liveExportedPackages(), targetClause));
            writeFragmentJar(new File(uploadDir, "peer-race-b.jar"));
            assertFalse("No restart expected when the running framework already exports the package",
                    invokeProcessOsgiPackages(uploadDir, new String[]{"peer-race-b.jar"},
                            Set.of(targetClause)));
        } finally {
            restoreOsgiExtras();
        }
    }

    /** Reflectively reads the private {@code liveExportedPackages} snapshot for assertions. */
    @SuppressWarnings("unchecked")
    private static Set<String> liveExportedPackages() throws Exception {
        final Field field = OSGIUtil.class.getDeclaredField("liveExportedPackages");
        field.setAccessible(true);
        return (Set<String>) field.get(OSGIUtil.getInstance());
    }

    /** Reflectively overwrites the private {@code liveExportedPackages} snapshot (control case). */
    private static void setLiveExportedPackages(final Set<String> value) throws Exception {
        final Field field = OSGIUtil.class.getDeclaredField("liveExportedPackages");
        field.setAccessible(true);
        field.set(OSGIUtil.getInstance(), value);
    }

    private static Set<String> withEntry(final Set<String> base, final String entry) {
        final Set<String> copy = new LinkedHashSet<>(base);
        copy.add(entry);
        return copy;
    }

    /** Invokes the private {@code processOsgiPackages} decision method and returns its needsRestart flag. */
    private static boolean invokeProcessOsgiPackages(final File uploadDir, final String[] pathnames,
            final Set<String> osgiUserPackages) throws Exception {
        final Method method = OSGIUtil.class.getDeclaredMethod(
                "processOsgiPackages", File.class, String[].class, Set.class, boolean.class);
        method.setAccessible(true);
        // testDryRun=false: we assert the restart decision, not package-string validation.
        return (boolean) method.invoke(
                OSGIUtil.getInstance(), uploadDir, pathnames, osgiUserPackages, false);
    }

    /**
     * Writes a minimal OSGi FRAGMENT jar to the given file. A fragment is used so the move step in
     * {@code processOsgiPackages} deletes it (fragments are not moved to the load folder), leaving no
     * unresolved bundle behind after the test.
     */
    private static void writeFragmentJar(final File dest) throws Exception {
        final Manifest manifest = new Manifest();
        final Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Bundle-ManifestVersion", "2");
        attrs.putValue("Bundle-SymbolicName", dest.getName().replace(".jar", ""));
        attrs.putValue("Bundle-Version", "1.0.0");
        // Exact value OSGIUtil/ResourceCollectorUtil.isFragmentJar recognizes, so the move step
        // deletes this jar instead of leaving it (unresolved) in the shared load folder.
        attrs.putValue("Fragment-Host", "system.bundle; extension:=framework");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(dest.toPath()), manifest)) {
            // manifest-only fragment: nothing else to write
        }
    }

    /**
     * Reads the pristine, shipped osgi-extra.conf defaults from the classpath, independent of any
     * override a test may have left in place. {@link #TARGET_PACKAGE} is not present in this list.
     */
    private static String pristineExports() throws Exception {
        try (InputStream in = OSGIUtil.class.getResourceAsStream("/osgi/osgi-extra.conf")) {
            assertNotNull("Default osgi-extra.conf resource must exist", in);
            return IOUtils.toString(in, StandardCharsets.UTF_8).trim();
        }
    }

    /**
     * Writes the given exported-packages content to a temp osgi-extra.conf, points OSGIUtil at it via
     * config, and restarts the framework so the system bundle re-publishes exactly these exports.
     */
    private static void restartWithExtraPackages(final String extraPackages) throws Exception {
        final File confFile = File.createTempFile("osgi-extra-37894", ".conf");
        confFile.deleteOnExit();
        Files.write(confFile.toPath(), extraPackages.getBytes(StandardCharsets.UTF_8));
        Config.setProperty(OSGI_EXTRA_CONFIG_FILE_PATH_KEY, confFile.getAbsolutePath());
        restartOSGi();
    }

    /**
     * Restores the original osgi-extra.conf path and restarts, leaving the framework in its default
     * state for any subsequent test / teardown.
     */
    private static void restoreOsgiExtras() {
        Config.setProperty(OSGI_EXTRA_CONFIG_FILE_PATH_KEY, originalOsgiExtraPath);
        restartOSGi();
    }

    /**
     * Builds an in-memory OSGi bundle jar (manifest only, no classes, no activator) that imports the
     * given package header. Starting such a bundle forces OSGi to resolve the import, so an absent
     * export surfaces as a BundleException.
     */
    private static byte[] buildBundleImporting(final String symbolicName, final String importPackage)
            throws Exception {
        final Manifest manifest = new Manifest();
        final Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Bundle-ManifestVersion", "2");
        attrs.putValue("Bundle-SymbolicName", symbolicName);
        attrs.putValue("Bundle-Name", symbolicName);
        attrs.putValue("Bundle-Version", "1.0.0");
        attrs.putValue("Import-Package", importPackage);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(bos, manifest)) {
            // manifest-only bundle: nothing else to write
        }
        return bos.toByteArray();
    }

    private static void uninstallQuietly(final Bundle bundle) {
        try {
            if (bundle != null && bundle.getState() != Bundle.UNINSTALLED) {
                bundle.uninstall();
            }
        } catch (final Exception e) {
            //Do nothing...
        }
    }
}
