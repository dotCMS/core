package com.dotcms.rendering.velocity;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * VelocityUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VelocityUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
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
        String originalVelocityPath = Config.getStringProperty("VELOCITY_ROOT", "/WEB-INF/velocity");
        String newVelocityPath = "/WEB-INF/customVelocity";
        Config.setProperty("VELOCITY_ROOT", newVelocityPath);

        String customVelocityPath = VelocityUtil.getVelocityRootPath();

        Assert.assertNotNull(customVelocityPath);
        assertThat("Path ends with /WEB-INF/customVelocity", customVelocityPath.endsWith("/WEB-INF/customVelocity"));

        // restore the default value (used on other tests)
        Config.setProperty("VELOCITY_ROOT", originalVelocityPath);
        String velocityRoot = Config.getStringProperty("VELOCITY_ROOT");
        assertThat("Path velocity root has been successfully restored", originalVelocityPath.equals(velocityRoot));
    }

}
