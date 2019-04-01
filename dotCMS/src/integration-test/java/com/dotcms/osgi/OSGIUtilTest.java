package com.dotcms.osgi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.OSGIUtil;
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
                .thenReturn(Config.getStringProperty("felix.base.dir", "/WEB-INF/felix"));

        // Initialize OSGI
        initializeOSGIFramework();

        felixDirectory = new File(Config
                .getStringProperty(FELIX_BASE_DIR,
                        Config.CONTEXT.getRealPath(WEB_INF_FOLDER) + File.separator + "felix"))
                .getAbsolutePath();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        //Stopping the OSGI framework
        OSGIUtil.getInstance().stopFramework();
    }

    /**
     * Initialize Mock OSGI Framework
     */
    private static void initializeOSGIFramework() {
        try {
            OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);
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
    public void test01DefaultFelixDeployPath() throws Exception {

        final String deployPath = OSGIUtil.getInstance().getFelixDeployPath();
        Assert.assertNotNull(deployPath);

        final String configuredDeployPath = getDeployPathInConfig();
        Assert.assertNotNull(configuredDeployPath);
        assertEquals(configuredDeployPath, deployPath);
    }

    /**
     * Test the felix undeploy path is the default path
     */
    @Test
    public void test02DefaultFelixUndeployPath() throws Exception {

        final String undeployPath = OSGIUtil.getInstance().getFelixUndeployPath();
        Assert.assertNotNull(undeployPath);

        final String configuredUndeployPath = getUndeployPathInConfig();
        Assert.assertNotNull(configuredUndeployPath);
        assertEquals(configuredUndeployPath, undeployPath);
    }

    /**
     * Test the felix deploy path is a custom path
     */
    @Test
    public void test03CustomFelixDeployPath() throws Exception {

        String customFelixPath = felixDirectory.replace("/felix", "/customfelix");
        Config.setProperty(FELIX_BASE_DIR, customFelixPath);

        restartOSGi();

        String deployFelixPath = OSGIUtil.getInstance().getFelixDeployPath();
        Assert.assertNotNull(deployFelixPath);

        final String configuredDeployPath = getDeployPathInConfig();
        Assert.assertNotNull(configuredDeployPath);
        assertEquals(configuredDeployPath, deployFelixPath);

        Config.setProperty(FELIX_BASE_DIR, felixDirectory);

        restartOSGi();

        deployFelixPath = OSGIUtil.getInstance().getFelixDeployPath();
        assertEquals(configuredDeployPath, deployFelixPath);

        removeFolder(customFelixPath);
    }

    /**
     * Test the felix undeploy path is a custom path
     */
    @Test
    public void test04CustomFelixUndeployPath() throws Exception {

        String customFelixPath = felixDirectory.replace("/felix", "/customfelix");
        Config.setProperty(FELIX_BASE_DIR, customFelixPath);

        restartOSGi();

        String undeployFelixPath = OSGIUtil.getInstance().getFelixUndeployPath();
        Assert.assertNotNull(undeployFelixPath);

        final String configuredUndeployPath = getUndeployPathInConfig();
        Assert.assertNotNull(configuredUndeployPath);
        assertEquals(configuredUndeployPath, undeployFelixPath);

        Config.setProperty(FELIX_BASE_DIR, felixDirectory);

        restartOSGi();

        undeployFelixPath = OSGIUtil.getInstance().getFelixUndeployPath();
        assertEquals(configuredUndeployPath, undeployFelixPath);

        removeFolder(customFelixPath);
    }

    /**
     * Test the parse base directory from 'felix.base.dir' property
     */
    @Test
    public void test07ParseBaseDirectory() throws Exception {
        String baseDirectory = OSGIUtil.getInstance().parseBaseDirectoryFromConfig();
        assertThat("WEB-INF Path exists", new File(baseDirectory).exists());
    }

    /**
     * Remove created path
     *
     * @param path The path to remove
     */
    private void removeFolder(String path) {
        try {
            File directory = new File(path);
            FileUtils.deleteDirectory(directory);
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

}