package com.dotcms.osgi;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.OSGIUtil;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;

import javax.servlet.ServletContextEvent;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * OSGIUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OSGIUtilTest {

    private static String FELIX_BASE_DIR;
    private static final String FELIX_BASE_DIR_KEY = "felix.base.dir";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        FELIX_BASE_DIR = Config.getStringProperty(FELIX_BASE_DIR_KEY, Config.CONTEXT.getRealPath("/WEB-INF") + File.separator + "felix");

        // Initialize OSGI
        initializeOSGIFramework();
    }

    @AfterClass
    public static void restartOSGIAfterFinishing() {
        Config.setProperty(FELIX_BASE_DIR_KEY, FELIX_BASE_DIR);

        // Restores original state of the OSGi framework
        restartOSGi();
    }

    /**
     * Initialize Mock OSGI Framework
     */
    private static void initializeOSGIFramework() {
        try {
            ServletContextEvent context = new ServletContextEvent(Config.CONTEXT);
            OSGIUtil.getInstance().initializeFramework(context);
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
        String deployPath = OSGIUtil.getInstance().getFelixDeployPath();

        Assert.assertNotNull(deployPath);
        assertThat("Path ends with /WEB-INF/felix/load", deployPath.endsWith("/WEB-INF/felix/load"));
    }

    /**
     * Test the felix undeploy path is the default path
     */
    @Test
    public void test02DefaultFelixUndeployPath() throws Exception {
        String undeployPath = OSGIUtil.getInstance().getFelixUndeployPath();

        Assert.assertNotNull(undeployPath);
        assertThat("Path ends with /WEB-INF/felix/undeployed", undeployPath.endsWith("/WEB-INF/felix/undeployed"));
    }

    /**
     * Test the felix deploy path is a custom path
     */
    @Test
    public void test03CustomFelixDeployPath() throws Exception {
        String contextFelixPath = Config.getStringProperty("context.path.felix", "/WEB-INF/felix");
        String customFelixPath = contextFelixPath.replace("/WEB-INF/felix", "/WEB-INF/customfelix");
        Config.setProperty(FELIX_BASE_DIR_KEY, customFelixPath);

        restartOSGi();

        String deployFelixPath = OSGIUtil.getInstance().getFelixDeployPath();

        Assert.assertNotNull(deployFelixPath);
        assertThat("Path ends with /WEB-INF/customfelix/load", deployFelixPath.endsWith("/WEB-INF/customfelix/load"));

        removeFolder(deployFelixPath);
        removeFolder(customFelixPath);

        Config.setProperty(FELIX_BASE_DIR_KEY, FELIX_BASE_DIR);
    }

    /**
     * Test the felix undeploy path is a custom path
     */
    @Test
    public void test04CustomFelixUndeployPath() throws Exception {
        String contextFelixPath = Config.getStringProperty("context.path.felix", "/WEB-INF/felix");
        String customFelixPath = contextFelixPath.replace("/WEB-INF/felix", "/WEB-INF/customfelix");
        Config.setProperty(FELIX_BASE_DIR_KEY, customFelixPath);

        restartOSGi();

        String undeployFelixPath = OSGIUtil.getInstance().getFelixUndeployPath();

        Assert.assertNotNull(undeployFelixPath);
        assertThat("Path ends with /WEB-INF/customfelix/undeployed", undeployFelixPath.endsWith("/WEB-INF/customfelix/undeployed"));

        removeFolder(undeployFelixPath);
        removeFolder(customFelixPath);

        Config.setProperty(FELIX_BASE_DIR_KEY, FELIX_BASE_DIR);
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
            return;
        }
    }
}
