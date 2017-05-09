package com.dotcms.osgi;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import java.io.File;

import javax.servlet.ServletContextEvent;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * OSGIUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OSGIUtilTest {

    private static final String FELIX_BASE_DIR_KEY = "felix.base.dir";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        Mockito.when(Config.CONTEXT.getRealPath("/WEB-INF/felix")).thenReturn(Config.getStringProperty("context.path.felix","/WEB-INF/felix"));

        // Initialize OSGI
        initializeOSGIFramework();
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
     * Restart the OSGI Framework. Copies/Restores all files
     *
     * @param  felixBasePath The default felix base path
     * @param newDirectory The new directory to copy the files from
     */
    private static void restartOSGi(String felixBasePath, String newDirectory) {
        try {
            Config.setProperty(FELIX_BASE_DIR_KEY, felixBasePath);

            FileUtils.copyDirectory(new File(newDirectory), new File(felixBasePath));

            restartOSGi();
        } catch (Exception ex) {
            Logger.error(OSGIUtilTest.class, "Error restarting OSGI", ex);
        }
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

        restartOSGi(contextFelixPath, customFelixPath);

        removeFolder(deployFelixPath);
        removeFolder(customFelixPath);
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

        restartOSGi(contextFelixPath, customFelixPath);

        removeFolder(undeployFelixPath);
        removeFolder(customFelixPath);
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
