package com.dotcms.osgi;

import static org.hamcrest.MatcherAssert.assertThat;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.apache.felix.framework.OSGIUtil;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

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
        String deployPath = OSGIUtil.getInstance().getFelixDeployPath();

        Assert.assertNotNull(deployPath);
        assertThat("Path ends with /WEB-INF/felix/load " + deployPath, deployPath.endsWith("/WEB-INF/felix/load"));
    }

    /**
     * Test the felix undeploy path is the default path
     */
    @Test
    public void test02DefaultFelixUndeployPath() throws Exception {
        String undeployPath = OSGIUtil.getInstance().getFelixUndeployPath();

        Assert.assertNotNull(undeployPath);
        assertThat("Path ends with /WEB-INF/felix/undeployed " + undeployPath, undeployPath.endsWith("/WEB-INF/felix/undeployed"));
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
        assertThat("Path ends with /WEB-INF/customfelix/load " + deployFelixPath, deployFelixPath.endsWith("/WEB-INF/customfelix/load"));

        Config.setProperty(FELIX_BASE_DIR_KEY, contextFelixPath);

        restartOSGi();

        deployFelixPath = OSGIUtil.getInstance().getFelixDeployPath();
        assertThat("Path ends with /WEB-INF/felix/load " + deployFelixPath, deployFelixPath.endsWith("/WEB-INF/felix/load"));

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
        assertThat("Path ends with /WEB-INF/customfelix/undeployed " + undeployFelixPath, undeployFelixPath.endsWith("/WEB-INF/customfelix/undeployed"));

        Config.setProperty(FELIX_BASE_DIR_KEY, contextFelixPath);

        restartOSGi();

        undeployFelixPath = OSGIUtil.getInstance().getFelixUndeployPath();
        assertThat("Path ends with /WEB-INF/felix/undeployed " + undeployFelixPath, undeployFelixPath.endsWith("/WEB-INF/felix/undeployed"));

        removeFolder(customFelixPath);
    }

    /**
     * Test the base directory exists using the servlet context
     */
//    @Test
    public void test05GetBaseDirectoryFromServletContext() throws Exception {
        String baseDirectory = OSGIUtil.getInstance().getBaseDirectory(Config.CONTEXT);
        assertThat("WEB-INF Base Directory exists", new File(baseDirectory).exists());
    }

    /**
     * Test the base directory exists using the Config.CONTEXT
     */
//    @Test
    public void test06GetBaseDirectoryFromConfigContext() throws Exception {
        String baseDirectory = OSGIUtil.getInstance().getBaseDirectory(null);
        assertThat("WEB-INF Base Directory exists", new File(baseDirectory).exists());
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
            return;
        }
    }
}
