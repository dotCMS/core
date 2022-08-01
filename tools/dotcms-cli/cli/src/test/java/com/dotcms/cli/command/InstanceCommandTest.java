package com.dotcms.cli.command;

import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
public class InstanceCommandTest {

    @Inject
    PicocliCommandLineFactory factory;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
    }

    /**
     * Scenario: If we do not pass the required params
     * Expected: Instance by def
     */
    @Test
    @Order(1)
    public void Test_Command_Instance_No_Params_Expect_Usage_Print()  {
        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setErr(out);
            final int status = commandLine.execute(InstanceCommand.NAME);
            Assertions.assertEquals(ExitCode.USAGE, status);
            final String output = writer.toString();
            //Assertions.assertTrue(output.contains("Missing required options: '--user', '--password'"));
            //Assertions.assertTrue(output.contains("Once a profile is selected. Use this command to open a session Expects a user"));
        }
    }

}
