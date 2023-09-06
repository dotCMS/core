package com.dotmarketing.microprofile.config;

import static org.junit.Assert.assertNotNull;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.io.File;
import javax.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

class PathsBeanTest {

    @BeforeAll
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @org.junit.jupiter.api.Test
    public void generateTokenTest() throws DotSecurityException, DotDataException {

        PathsBean pathsConfig = CDI.current().select(PathsBean.class).get();
        assertNotNull(pathsConfig);

        System.out.println("dynamicContentPath: " + pathsConfig.dynamicContentPath().getAbsolutePath());
        System.out.println("buildOutputPath: " + pathsConfig.buildOutputPath().getAbsolutePath());
        System.out.println("testRuntimeRoot: " + pathsConfig.testRuntimeRoot().getAbsolutePath());

        System.out.println("testRuntimeRoot-prog: " + pathsConfig.testRuntimeRoot().getAbsolutePath());
        Config cp = ConfigProvider.getConfig();

        String result = cp.getValue("dot.test-runtime-root", File.class).getAbsolutePath();
        System.out.println("dot.testRuntimeRoot: " + result);

    }
}
