package com.dotcms.velocity;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.VelocityUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * VelocityUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VelocityUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        Mockito.when(Config.CONTEXT.getRealPath("/WEB-INF/velocity"))
            .thenReturn(Config.getStringProperty("VELOCITY_ROOT", "/WEB-INF/velocity"));
    }

    /**
     * Test the velocity path is the default path
     */
    @Test
    public void test01DefaultVelocityPath() throws Exception {
        String velocityPath = VelocityUtil.getVelocityRootPath();

        Assert.assertNotNull(velocityPath);
        assertThat("Path ends with /WEB-INF/velocity", velocityPath.endsWith("/WEB-INF/velocity"));
    }

    /**
     * Test the velocity path is a custom path
     */
    @Test
    public void test02CustomVelocityPath() throws Exception {
        String velocityPath = Config.getStringProperty("VELOCITY_ROOT", "/WEB-INF/velocity");
        velocityPath = velocityPath.replace("/WEB-INF/velocity", "/WEB-INF/customvelocity");
        Config.setProperty("VELOCITY_ROOT", velocityPath);

        String customVelocityPath = VelocityUtil.getVelocityRootPath();

        Assert.assertNotNull(customVelocityPath);
        assertThat("Path ends with /WEB-INF/customvelocity", customVelocityPath.endsWith("/WEB-INF/customvelocity"));

        // restore the default value (used on other tests)
        Config.setProperty("VELOCITY_ROOT", "/WEB-INF/velocity");
    }

}
