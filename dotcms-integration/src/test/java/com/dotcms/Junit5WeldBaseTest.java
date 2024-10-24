package com.dotcms;

import org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;


public abstract class Junit5WeldBaseTest {

    @WeldSetup
    public static WeldInitiator weldInitiator = WeldInitiator.of(
            WeldInitiator.createWeld()
                    .containerId(RegistrySingletonProvider.STATIC_INSTANCE)
                    .enableDiscovery()
    );

}
