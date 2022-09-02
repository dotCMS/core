package com.dotcms.cli.command;

import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import java.io.IOException;
import javax.inject.Inject;

public abstract class CommandTest {

    private static final String PICOCLI_ANSI = "picocli.ansi";

    /**
     * This here to prevent an issue running test from within quarkus:dev console on which coloring/styling meta chars are not resolved. Therefore, this breaks our tests.
     */
     static void disableAnsi() {
        System.setProperty(PICOCLI_ANSI, Boolean.FALSE.toString());
    }

    /**
     * This removes the flag to prevent issues outside the test execution
     */
    static void enableAnsi() {
        System.clearProperty(PICOCLI_ANSI);
    }

    @Inject
    PicocliCommandLineFactory factory;

    @Inject
    ServiceManager serviceManager;

    ServiceManager resetServiceProfiles() throws IOException {
        return serviceManager.removeAll()
                .persist(ServiceBean.builder().name("default").active(true).build());
    }

}
