package com.dotcms.rendering.velocity.services;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PageMode;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Test of {@link ContainerLoader}
 */
public class ContainerLoaderTest {

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ContainerLoader#writeObject}
     * Given Scenario: Using the system container should build the velocity to support the empty contentlets by adding the temp/fake contentlet when editmode is on
     * ExpectedResult: prints the temp cont
     */
    @Test
    public void writeObject_with_empty_contentlets_edit_mode() throws DotDataException, DotSecurityException, IOException {

        final String path = "/EDIT_MODE/SYSTEM_CONTAINER/dotParser_1712178208126.container";
        final String language = "CONTAINER";
        final String id1 = "SYSTEM";
        final String id2 = "dotParser_1712178208126";
        final String cacheKey = "EDIT_MODE-SYSTEM-CONTAINER.container";
        final String variant = "DEFAULT";
        final VelocityType type = VelocityType.CONTAINER;
        final PageMode mode = PageMode.EDIT_MODE;
        final VelocityResourceKey velocityResourceKey =
                new VelocityResourceKey(path, language, id1, id2, variant, type, mode, cacheKey);

        final InputStream inputStream = new ContainerLoader().writeObject(velocityResourceKey);
        final String velocity = IOUtils.toString(inputStream, Charset.defaultCharset());


        Assert.assertTrue("Should contains the token: TEMP_EMPTY_CONTENTLET", velocity.contains("TEMP_EMPTY_CONTENTLET"));
    }

    /**
     * Method to test: {@link ContainerLoader#writeObject}
     * Given Scenario: Using the system container should build the velocity to support the empty contentlets by adding the temp/fake contentlet when editmode is off
     * ExpectedResult: prints the temp cont
     */
    @Test
    public void writeObject_with_empty_contentlets_off_edit_mode() throws DotDataException, DotSecurityException, IOException {

        final String path = "/EDIT_MODE/SYSTEM_CONTAINER/dotParser_1712178208126.container";
        final String language = "CONTAINER";
        final String id1 = "SYSTEM";
        final String id2 = "dotParser_1712178208126";
        final String cacheKey = "EDIT_MODE-SYSTEM-CONTAINER.container";
        final String variant = "DEFAULT";
        final VelocityType type = VelocityType.CONTAINER;
        final PageMode mode = PageMode.LIVE;
        final VelocityResourceKey velocityResourceKey =
                new VelocityResourceKey(path, language, id1, id2, variant, type, mode, cacheKey);

        final InputStream inputStream = new ContainerLoader().writeObject(velocityResourceKey);
        final String velocity = IOUtils.toString(inputStream, Charset.defaultCharset());


        Assert.assertFalse("Should not contains the token: TEMP_EMPTY_CONTENTLET", velocity.contains("TEMP_EMPTY_CONTENTLET"));
    }

}
