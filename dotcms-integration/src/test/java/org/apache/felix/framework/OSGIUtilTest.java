package org.apache.felix.framework;

import static org.junit.Assert.assertEquals;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import java.io.File;
import java.util.Collection;
import java.util.Set;

import com.dotmarketing.util.ResourceCollectorUtil;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

/**
 * OSGIUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OSGIUtilTest {

    private static final String WEB_INF_FOLDER = "/WEB-INF";
    private static final String FELIX_BASE_DIR = "felix.base.dir";
    private static final String FELIX_FILEINSTALL_DIR = "felix.felix.fileinstall.dir";
    private static final String FELIX_UNDEPLOYED_DIR = "felix.felix.undeployed.dir";

    private static String felixDirectory;

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
}
