package com.dotcms;

import org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)
public class TestBaseJunit5WeldInitiator {


    @WeldSetup
    public static WeldInitiator weld = WeldInitiator.of(
            WeldInitiator.createWeld()
                    .containerId(RegistrySingletonProvider.STATIC_INSTANCE)
                    .enableDiscovery()
    );

    @AfterAll
    public static void tearDown() {
        if (weld != null && weld.isRunning()) {
            weld.shutdown();
            weld = null;
        }
    }

}
