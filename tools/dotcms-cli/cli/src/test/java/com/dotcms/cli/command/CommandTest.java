package com.dotcms.cli.command;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import picocli.CommandLine;

public abstract class CommandTest {

    protected static final String PICOCLI_ANSI = "picocli.ansi";

    /**
     * This here to prevent an issue running test from within quarkus:dev console on which coloring/styling meta chars are not resolved. Therefore, this breaks our tests.
     */
    void disableAnsi() {
        System.setProperty(PICOCLI_ANSI, Boolean.FALSE.toString());
    }

    /**
     * This removes the flag to prevent issues outside the test execution
     */
     void enableAnsi() {
        System.clearProperty(PICOCLI_ANSI);
    }

    @Inject
    PicocliCommandLineFactory factory;

    @Inject
    ServiceManager serviceManager;

    @CanIgnoreReturnValue
    protected ServiceManager resetServiceProfiles() throws IOException {
        return serviceManager.removeAll()
                .persist(ServiceBean.builder().name("default").active(true).build());
    }

    protected CommandLine createCommand() {
        final CustomConfiguration customConfiguration = new CustomConfiguration();
        return customConfiguration.customCommandLine(factory);
    }

    @PostConstruct
    public void  postConstruct(){
       disableAnsi();
    }

    @PreDestroy
    public void  preDestroy(){
        enableAnsi();
    }

}
