package com.dotmarketing.microprofile.config;

import static org.junit.jupiter.api.Assertions.*;

import com.dotmarketing.util.Config;
import java.io.File;

import org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class ResourceFileManagerBeanTest {

    private WeldContainer weld;

    @BeforeAll
    void setUp() {
        weld = new Weld().containerId(RegistrySingletonProvider.STATIC_INSTANCE)
                .initialize();

        Config.initializeConfig();
    }

    @AfterAll
    void cleanup() {
        weld.shutdown();
    }

    @Test
    void initializeCache() {
        ResourceFileManagerBean bean = CDIUtils.getBean(ResourceFileManagerBean.class);
        File resource = bean.getResource("test");
        fail("Not yet implemented");


    }
}
